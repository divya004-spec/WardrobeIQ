package com.example.wardrobeiq.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.example.wardrobeiq.database.AppDatabase;
import com.example.wardrobeiq.database.ClothingDao;
import com.example.wardrobeiq.models.Clothing;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shared UI helpers used by the Home tab and the Match screen,
 * so outfit cards look the same everywhere.
 */
public class OutfitViews {

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface OnWorn {
        void onWorn();
    }

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static Bitmap loadScaledBitmap(String path, int targetSize) {

        if (path == null || path.isEmpty() || !new File(path).exists()) {
            return null;
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);

        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= targetSize
                && bounds.outHeight / (sample * 2) >= targetSize) {
            sample *= 2;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;

        return BitmapFactory.decodeFile(path, options);
    }

    public static View buildItemView(Context context, Clothing item) {

        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        boxParams.setMargins(0, 0, dp(context, 10), 0);
        box.setLayoutParams(boxParams);

        ImageView image = new ImageView(context);
        image.setLayoutParams(new LinearLayout.LayoutParams(
                dp(context, 96), dp(context, 120)));
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);

        Bitmap bitmap = loadScaledBitmap(item.getImagePath(), 300);

        if (bitmap != null) {
            image.setImageBitmap(bitmap);
        } else {
            image.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        TextView label = new TextView(context);
        label.setText(item.getCategory() + "\n" + item.getColor());
        label.setTextSize(12);
        label.setTextColor(0xFF444444);
        label.setGravity(Gravity.CENTER);

        box.addView(image);
        box.addView(label);

        return box;
    }

    /** A card with the outfit photos, the match %, the reasons and a wear button. */
    public static View buildOutfitCard(
            final Context context,
            String heading,
            final OutfitRecommender.Outfit outfit,
            String wearButtonText,
            final OnWorn onWorn) {

        CardView card = new CardView(context);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, dp(context, 8), 0, dp(context, 8));
        card.setLayoutParams(cardParams);
        card.setRadius(dp(context, 12));
        card.setCardElevation(dp(context, 3));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(context, 12), dp(context, 12),
                dp(context, 12), dp(context, 12));
        card.addView(content);

        int percent = (int) Math.max(0, Math.min(100, Math.round(outfit.score)));

        TextView title = new TextView(context);
        title.setText(heading + "  \u2022  " + percent + "% match");
        title.setTextSize(18);
        title.setTextColor(0xFF222222);
        title.setTypeface(null, Typeface.BOLD);
        content.addView(title);

        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(context, 10), 0, dp(context, 6));
        scroll.addView(row);
        content.addView(scroll);

        for (Clothing item : outfit.items) {
            row.addView(buildItemView(context, item));
        }

        StringBuilder reasons = new StringBuilder();
        for (String reason : outfit.reasons) {
            reasons.append("\u2022 ").append(reason).append("\n");
        }

        TextView why = new TextView(context);
        why.setText(reasons.toString().trim());
        why.setTextSize(13);
        why.setTextColor(0xFF555555);
        why.setPadding(0, dp(context, 4), 0, dp(context, 8));
        content.addView(why);

        final Button wear = new Button(context);
        wear.setText(wearButtonText);
        wear.setOnClickListener(v -> markWorn(context, outfit, wear, onWorn));
        content.addView(wear);

        return card;
    }

    /** Adds +1 wearCount and today's date to every item of the outfit. */
    public static void markWorn(
            final Context context,
            final OutfitRecommender.Outfit outfit,
            final Button button,
            final OnWorn onWorn) {

        button.setEnabled(false);

        final String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        final ClothingDao dao = AppDatabase.getInstance(context).clothingDao();

        IO.execute(() -> {

            for (Clothing item : outfit.items) {

                Clothing fresh = dao.getClothingById(item.getId());

                if (fresh == null) {
                    continue;
                }

                fresh.setWearCount(fresh.getWearCount() + 1);
                fresh.setLastWorn(today);
                dao.updateClothing(fresh);
            }

            MAIN.post(() -> {
                button.setText("MARKED AS WORN TODAY");
                Toast.makeText(context,
                        "Saved. Future ideas will favour other items.",
                        Toast.LENGTH_SHORT).show();
                if (onWorn != null) {
                    onWorn.onWorn();
                }
            });
        });
    }
}
