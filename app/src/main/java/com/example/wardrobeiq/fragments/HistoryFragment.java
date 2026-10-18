package com.example.wardrobeiq.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.wardrobeiq.R;
import com.example.wardrobeiq.activities.ClothingDetailsActivity;
import com.example.wardrobeiq.database.AppDatabase;
import com.example.wardrobeiq.database.ClothingDao;
import com.example.wardrobeiq.models.Clothing;
import com.example.wardrobeiq.utils.WardrobeStats;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryFragment extends Fragment {

    private TextView txtEmpty;
    private LinearLayout layoutHistory;

    private ClothingDao clothingDao;
    private int userId;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    public HistoryFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_history,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        txtEmpty = view.findViewById(R.id.txtHistoryEmpty);
        layoutHistory = view.findViewById(R.id.layoutHistory);

        SharedPreferences preferences =
                requireContext().getSharedPreferences(
                        "WardrobeIQSession",
                        Context.MODE_PRIVATE
                );

        userId = preferences.getInt("userId", -1);

        clothingDao =
                AppDatabase.getInstance(requireContext()).clothingDao();
    }

    // Reload every time the screen is shown (e.g. after coming back from details).
    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
    }

    private void loadHistory() {

        if (userId == -1 || clothingDao == null) {
            return;
        }

        executor.execute(() -> {

            List<Clothing> all = clothingDao.getClothingByUser(userId);

            LinkedHashMap<String, List<Clothing>> groups =
                    WardrobeStats.groupByLastWorn(all);

            if (getActivity() == null) {
                return;
            }

            getActivity().runOnUiThread(() -> {

                if (!isAdded()) {
                    return;
                }

                layoutHistory.removeAllViews();

                if (groups.isEmpty()) {
                    txtEmpty.setVisibility(View.VISIBLE);
                    return;
                }

                txtEmpty.setVisibility(View.GONE);

                for (Map.Entry<String, List<Clothing>> group : groups.entrySet()) {

                    layoutHistory.addView(buildDateHeader(group.getKey()));

                    for (Clothing item : group.getValue()) {
                        layoutHistory.addView(buildRow(item));
                    }
                }
            });
        });
    }

    // =====================================================
    // VIEWS
    // =====================================================

    private View buildDateHeader(String rawDate) {

        TextView header = new TextView(requireContext());
        header.setText(friendlyDate(rawDate));
        header.setTextSize(18);
        header.setTextColor(0xFF222222);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(0, dp(16), 0, dp(4));

        return header;
    }

    private View buildRow(final Clothing item) {

        Context context = requireContext();

        CardView card = new CardView(context);
        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        cardParams.setMargins(0, dp(4), 0, dp(4));
        card.setLayoutParams(cardParams);
        card.setRadius(dp(10));
        card.setCardElevation(dp(2));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(8), dp(8), dp(8));
        card.addView(row);

        ImageView image = new ImageView(context);
        image.setLayoutParams(new LinearLayout.LayoutParams(dp(64), dp(80)));
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);

        Bitmap bitmap = loadScaledBitmap(item.getImagePath(), 200);

        if (bitmap != null) {
            image.setImageBitmap(bitmap);
        } else {
            image.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        row.addView(image);

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(12), 0, 0, 0);

        TextView title = new TextView(context);
        title.setText(item.getCategory() + "  \u2022  " + item.getColor());
        title.setTextSize(16);
        title.setTextColor(0xFF222222);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView sub = new TextView(context);
        int count = item.getWearCount();
        sub.setText("Worn " + count + (count == 1 ? " time" : " times") + " in total");
        sub.setTextSize(13);
        sub.setTextColor(0xFF666666);

        texts.addView(title);
        texts.addView(sub);
        row.addView(texts);

        card.setOnClickListener(v -> {

            Intent intent =
                    new Intent(requireContext(), ClothingDetailsActivity.class);

            intent.putExtra("clothingId", item.getId());

            startActivity(intent);
        });

        return card;
    }

    private String friendlyDate(String raw) {

        try {

            SimpleDateFormat parser =
                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            Date date = parser.parse(raw);

            String today = parser.format(new Date());

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            String yesterday = parser.format(calendar.getTime());

            if (raw.equals(today)) {
                return "Today";
            }

            if (raw.equals(yesterday)) {
                return "Yesterday";
            }

            return new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
                    .format(date);

        } catch (Exception e) {
            return raw;
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