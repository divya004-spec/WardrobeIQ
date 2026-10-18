package com.example.wardrobeiq.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.wardrobeiq.R;
import com.example.wardrobeiq.database.AppDatabase;
import com.example.wardrobeiq.database.ClothingDao;
import com.example.wardrobeiq.models.Clothing;
import com.example.wardrobeiq.utils.WardrobeStats;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalyticsFragment extends Fragment {

    private TextView txtEmpty;
    private LinearLayout layoutAnalytics;

    private ClothingDao clothingDao;
    private int userId;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    public AnalyticsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_analytics,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        txtEmpty = view.findViewById(R.id.txtAnalyticsEmpty);
        layoutAnalytics = view.findViewById(R.id.layoutAnalytics);

        SharedPreferences preferences =
                requireContext().getSharedPreferences(
                        "WardrobeIQSession",
                        Context.MODE_PRIVATE
                );

        userId = preferences.getInt("userId", -1);

        clothingDao =
                AppDatabase.getInstance(requireContext()).clothingDao();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStats();
    }

    private void loadStats() {

        if (userId == -1 || clothingDao == null) {
            return;
        }

        executor.execute(() -> {

            List<Clothing> all = clothingDao.getClothingByUser(userId);
            WardrobeStats stats = WardrobeStats.compute(all);

            if (getActivity() == null) {
                return;
            }

            getActivity().runOnUiThread(() -> {

                if (!isAdded()) {
                    return;
                }

                layoutAnalytics.removeAllViews();

                if (stats.totalItems == 0) {
                    txtEmpty.setVisibility(View.VISIBLE);
                    return;
                }

                txtEmpty.setVisibility(View.GONE);
                showStats(stats);
            });
        });
    }

    // =====================================================
    // BUILD THE SCREEN
    // =====================================================

    private void showStats(WardrobeStats stats) {

        Context context = requireContext();

        // 1. Overview boxes
        LinearLayout overview = new LinearLayout(context);
        overview.setOrientation(LinearLayout.HORIZONTAL);
        overview.addView(statBox(String.valueOf(stats.totalItems), "Items"));
        overview.addView(statBox(String.valueOf(stats.totalWears), "Total wears"));
        overview.addView(statBox(String.valueOf(stats.neverWornCount), "Never worn"));
        layoutAnalytics.addView(overview);

        // 2. Categories
        layoutAnalytics.addView(sectionTitle("Items by category"));

        int maxCategory = 1;
        for (int value : stats.categoryCounts.values()) {
            maxCategory = Math.max(maxCategory, value);
        }

        for (Map.Entry<String, Integer> e : stats.categoryCounts.entrySet()) {
            layoutAnalytics.addView(
                    barRow(e.getKey(), e.getValue(), maxCategory, 0xFF6750A4));
        }

        // 3. Colours (top 6)
        layoutAnalytics.addView(sectionTitle("Colours in your wardrobe"));

        int maxColor = 1;
        for (int value : stats.colorCounts.values()) {
            maxColor = Math.max(maxColor, value);
        }

        int shown = 0;
        for (Map.Entry<String, Integer> e : stats.colorCounts.entrySet()) {

            if (shown >= 6) {
                break;
            }

            layoutAnalytics.addView(
                    barRow(e.getKey(), e.getValue(), maxColor, colorFor(e.getKey())));
            shown++;
        }

        // 4. Most worn
        if (!stats.mostWorn.isEmpty()) {

            layoutAnalytics.addView(sectionTitle("Most worn"));

            LinearLayout row = thumbnailRow();

            for (Clothing item : stats.mostWorn) {
                row.addView(thumb(item,
                        item.getWearCount() + (item.getWearCount() == 1 ? " time" : " times")));
            }

            layoutAnalytics.addView(wrapScroll(row));
        }

        // 5. Never worn
        if (!stats.neverWorn.isEmpty()) {

            layoutAnalytics.addView(sectionTitle("Not worn yet \u2013 try these!"));

            LinearLayout row = thumbnailRow();

            int count = 0;
            for (Clothing item : stats.neverWorn) {

                if (count >= 8) {
                    break;
                }

                row.addView(thumb(item, "Never worn"));
                count++;
            }

            layoutAnalytics.addView(wrapScroll(row));
        }

        // 6. Insights
        if (!stats.insights.isEmpty()) {

            layoutAnalytics.addView(sectionTitle("Insights"));

            StringBuilder text = new StringBuilder();

            for (String insight : stats.insights) {
                text.append("\u2022 ").append(insight).append("\n\n");
            }

            TextView insights = new TextView(context);
            insights.setText(text.toString().trim());
            insights.setTextSize(14);
            insights.setTextColor(0xFF444444);
            layoutAnalytics.addView(insights);
        }
    }

    private View statBox(String value, String label) {

        Context context = requireContext();

        CardView card = new CardView(context);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        card.setLayoutParams(params);
        card.setRadius(dp(12));
        card.setCardElevation(dp(2));

        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(8), dp(14), dp(8), dp(14));
        card.addView(box);

        TextView number = new TextView(context);
        number.setText(value);
        number.setTextSize(26);
        number.setTextColor(0xFF6750A4);
        number.setTypeface(null, android.graphics.Typeface.BOLD);
        number.setGravity(Gravity.CENTER);

        TextView caption = new TextView(context);
        caption.setText(label);
        caption.setTextSize(12);
        caption.setTextColor(0xFF666666);
        caption.setGravity(Gravity.CENTER);

        box.addView(number);
        box.addView(caption);

        return card;
    }

    private View sectionTitle(String text) {

        TextView title = new TextView(requireContext());
        title.setText(text);
        title.setTextSize(18);
        title.setTextColor(0xFF222222);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, dp(24), 0, dp(8));

        return title;
    }

    private View barRow(String label, int value, int max, int color) {

        Context context = requireContext();

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView name = new TextView(context);
        name.setLayoutParams(new LinearLayout.LayoutParams(dp(90),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        name.setText(label);
        name.setTextSize(14);
        name.setTextColor(0xFF333333);

        ProgressBar bar = new ProgressBar(context, null,
                android.R.attr.progressBarStyleHorizontal);
        bar.setLayoutParams(new LinearLayout.LayoutParams(0, dp(12), 1f));
        bar.setMax(max);
        bar.setProgress(value);
        bar.setProgressTintList(ColorStateList.valueOf(color));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(0xFFE6E1EA));

        TextView count = new TextView(context);
        count.setLayoutParams(new LinearLayout.LayoutParams(dp(36),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        count.setGravity(Gravity.END);
        count.setText(String.valueOf(value));
        count.setTextSize(14);
        count.setTextColor(0xFF333333);

        row.addView(name);
        row.addView(bar);
        row.addView(count);

        return row;
    }

    private LinearLayout thumbnailRow() {

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);

        return row;
    }

    private View wrapScroll(LinearLayout row) {

        HorizontalScrollView scroll = new HorizontalScrollView(requireContext());
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.addView(row);

        return scroll;
    }

    private View thumb(Clothing item, String caption) {

        Context context = requireContext();

        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout.LayoutParams boxParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        boxParams.setMargins(0, 0, dp(10), 0);
        box.setLayoutParams(boxParams);

        ImageView image = new ImageView(context);
        image.setLayoutParams(new LinearLayout.LayoutParams(dp(88), dp(110)));
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);

        Bitmap bitmap = loadScaledBitmap(item.getImagePath(), 300);

        if (bitmap != null) {
            image.setImageBitmap(bitmap);
        } else {
            image.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        TextView label = new TextView(context);
        label.setText(item.getCategory() + " " + item.getColor() + "\n" + caption);
        label.setTextSize(12);
        label.setTextColor(0xFF444444);
        label.setGravity(Gravity.CENTER);

        box.addView(image);
        box.addView(label);

        return box;
    }

    // Bar colour that matches the clothing colour name.
    private int colorFor(String name) {

        switch (name.toLowerCase(Locale.ROOT)) {
            case "black":
                return 0xFF212121;
            case "white":
                return 0xFFBDBDBD;
            case "blue":
                return 0xFF1E88E5;
            case "grey":
                return 0xFF757575;
            case "brown":
                return 0xFF795548;
            case "beige":
                return 0xFFD7C4A3;
            case "red":
                return 0xFFE53935;
            case "green":
                return 0xFF43A047;
            case "yellow":
                return 0xFFFDD835;
            case "pink":
                return 0xFFEC407A;
            case "purple":
                return 0xFF8E24AA;
            case "orange":
                return 0xFFFB8C00;
            default:
                return 0xFF6750A4;
        }
    }

    private Bitmap loadScaledBitmap(String path, int targetSize) {

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

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}