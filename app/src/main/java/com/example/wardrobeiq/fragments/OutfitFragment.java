package com.example.wardrobeiq.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.wardrobeiq.R;
import com.example.wardrobeiq.database.AppDatabase;
import com.example.wardrobeiq.database.ClothingDao;
import com.example.wardrobeiq.models.Clothing;
import com.example.wardrobeiq.utils.OutfitRecommender;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OutfitFragment extends Fragment {

    private static final String[] OCCASIONS = {
            "Any", "Casual", "College", "Work", "Party", "Formal", "Sports"
    };

    private static final String[] SEASONS = {
            "Any", "Summer", "Winter", "Monsoon"
    };

    private EditText edtRequest;
    private Spinner spinnerOccasion;
    private Spinner spinnerSeason;
    private Button btnSuggest;
    private Button btnPickOutfit;
    private Button btnSuggestShoes;
    private TextView txtStatus;
    private TextView txtPickedHint;
    private LinearLayout layoutResults;
    private LinearLayout layoutPickedItems;

    private ClothingDao clothingDao;
    private int userId;

    // Clothes the user picked for "I already have an outfit".
    private final List<Clothing> pickedItems = new ArrayList<>();

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    public OutfitFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_outfit,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        edtRequest = view.findViewById(R.id.edtRequest);
        spinnerOccasion = view.findViewById(R.id.spinnerOutfitOccasion);
        spinnerSeason = view.findViewById(R.id.spinnerOutfitSeason);
        btnSuggest = view.findViewById(R.id.btnSuggestOutfits);
        btnPickOutfit = view.findViewById(R.id.btnPickOutfit);
        btnSuggestShoes = view.findViewById(R.id.btnSuggestShoes);
        txtStatus = view.findViewById(R.id.txtOutfitStatus);
        txtPickedHint = view.findViewById(R.id.txtPickedHint);
        layoutResults = view.findViewById(R.id.layoutOutfitResults);
        layoutPickedItems = view.findViewById(R.id.layoutPickedItems);

        setupSpinner(spinnerOccasion, OCCASIONS);
        setupSpinner(spinnerSeason, SEASONS);

        SharedPreferences preferences =
                requireContext().getSharedPreferences(
                        "WardrobeIQSession",
                        Context.MODE_PRIVATE
                );

        userId = preferences.getInt("userId", -1);

        clothingDao =
                AppDatabase.getInstance(requireContext()).clothingDao();

        btnSuggest.setOnClickListener(v -> suggestOutfits());
        btnPickOutfit.setOnClickListener(v -> loadAndShowPicker());
        btnSuggestShoes.setOnClickListener(v -> suggestShoesForPicked());

        refreshPickedRow();
    }

    private void setupSpinner(Spinner spinner, String[] values) {

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        values
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(adapter);
    }

    // Reads the text box and the two spinners. Words in the text box win.
    // Returns {occasion, season}.
    private String[] readFilters() {

        String text = edtRequest.getText().toString();

        String occasion = spinnerOccasion.getSelectedItem().toString();
        String season = spinnerSeason.getSelectedItem().toString();

        String textOccasion = OutfitRecommender.detectOccasion(text);
        String textSeason = OutfitRecommender.detectSeason(text);

        if (!OutfitRecommender.ANY.equals(textOccasion)) {
            occasion = textOccasion;
            selectInSpinner(spinnerOccasion, occasion);
        }

        if (!OutfitRecommender.ANY.equals(textSeason)) {
            season = textSeason;
            selectInSpinner(spinnerSeason, season);
        }

        return new String[]{occasion, season};
    }

    // =====================================================
    // 1. SUGGEST FULL OUTFITS
    // =====================================================

    private void suggestOutfits() {

        if (userId == -1) {
            Toast.makeText(
                    requireContext(),
                    "User session not found",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String[] filters = readFilters();
        final String finalOccasion = filters[0];
        final String finalSeason = filters[1];

        btnSuggest.setEnabled(false);
        txtStatus.setText("Finding outfits...");
        layoutResults.removeAllViews();

        executor.execute(() -> {

            List<Clothing> wardrobe =
                    clothingDao.getClothingByUser(userId);

            List<OutfitRecommender.Outfit> outfits =
                    OutfitRecommender.recommend(
                            wardrobe,
                            finalOccasion,
                            finalSeason,
                            5
                    );

            String missing =
                    outfits.isEmpty()
                            ? OutfitRecommender.explainMissing(wardrobe)
                            : "";

            if (getActivity() == null) {
                return;
            }

            getActivity().runOnUiThread(() -> {

                if (!isAdded()) {
                    return;
                }

                btnSuggest.setEnabled(true);

                if (outfits.isEmpty()) {
                    txtStatus.setText(missing);
                    return;
                }

                txtStatus.setText(
                        "Top " + outfits.size() + " outfit idea"
                                + (outfits.size() > 1 ? "s" : "")
                                + " for " + finalOccasion
                                + " / " + finalSeason
                );

                int number = 1;

                for (OutfitRecommender.Outfit outfit : outfits) {
                    layoutResults.addView(
                            buildOutfitCard("Outfit " + number, outfit)
                    );
                    number++;
                }
            });
        });
    }

    // =====================================================
    // 2. PICK CLOTHES I AM ALREADY WEARING
    // =====================================================

    private void loadAndShowPicker() {

        if (userId == -1) {
            Toast.makeText(
                    requireContext(),
                    "User session not found",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        executor.execute(() -> {

            List<Clothing> all = clothingDao.getClothingByUser(userId);

            // Shoes are what we are going to suggest, so they are not listed.
            List<Clothing> choices = new ArrayList<>();

            for (Clothing c : all) {
                if (!"Shoes".equalsIgnoreCase(c.getCategory())) {
                    choices.add(c);
                }
            }

            if (getActivity() == null) {
                return;
            }

            getActivity().runOnUiThread(() -> {

                if (!isAdded()) {
                    return;
                }

                if (choices.isEmpty()) {
                    Toast.makeText(
                            requireContext(),
                            "Add some clothes to your wardrobe first.",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                showPickerDialog(choices);
            });
        });
    }

    private void showPickerDialog(final List<Clothing> choices) {

        Context context = requireContext();

        // Remember what was already picked.
        final Set<Integer> selectedIds = new HashSet<>();
        for (Clothing c : pickedItems) {
            selectedIds.add(c.getId());
        }

        int columns = 3;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int cellWidth = (screenWidth - dp(64)) / columns;

        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(columns);
        grid.setPadding(dp(8), dp(8), dp(8), dp(8));

        for (final Clothing item : choices) {

            final LinearLayout cell = new LinearLayout(context);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(dp(4), dp(4), dp(4), dp(4));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = cellWidth;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.setMargins(dp(2), dp(2), dp(2), dp(2));
            cell.setLayoutParams(params);

            ImageView image = new ImageView(context);
            image.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            cellWidth - dp(8),
                            (int) ((cellWidth - dp(8)) * 1.2f)
                    )
            );
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);

            Bitmap bitmap = loadScaledBitmap(item.getImagePath(), 300);

            if (bitmap != null) {
                image.setImageBitmap(bitmap);
            } else {
                image.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            final TextView label = new TextView(context);
            label.setTextSize(12);
            label.setGravity(Gravity.CENTER);
            label.setTextColor(0xFF333333);

            cell.addView(image);
            cell.addView(label);

            updateCellLook(cell, label, item, selectedIds.contains(item.getId()));

            cell.setOnClickListener(v -> {

                boolean nowSelected;

                if (selectedIds.contains(item.getId())) {
                    selectedIds.remove(item.getId());
                    nowSelected = false;
                } else {
                    selectedIds.add(item.getId());
                    nowSelected = true;
                }

                updateCellLook(cell, label, item, nowSelected);
            });

            grid.addView(cell);
        }

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(grid);

        new AlertDialog.Builder(context)
                .setTitle("Tap the clothes you are wearing")
                .setView(scrollView)
                .setPositiveButton("Done", (dialog, which) -> {

                    pickedItems.clear();

                    for (Clothing c : choices) {
                        if (selectedIds.contains(c.getId())) {
                            pickedItems.add(c);
                        }
                    }

                    refreshPickedRow();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateCellLook(
            LinearLayout cell,
            TextView label,
            Clothing item,
            boolean selected) {

        cell.setBackgroundColor(selected ? 0xFFD9CFF5 : 0x00000000);

        label.setText(
                (selected ? "\u2713 " : "")
                        + item.getCategory() + "\n" + item.getColor()
        );
    }

    private void refreshPickedRow() {

        layoutPickedItems.removeAllViews();

        if (pickedItems.isEmpty()) {
            txtPickedHint.setText("No clothes picked yet.");
            return;
        }

        txtPickedHint.setText(
                "Picked " + pickedItems.size() + " item"
                        + (pickedItems.size() > 1 ? "s" : "") + ":"
        );

        for (Clothing item : pickedItems) {
            layoutPickedItems.addView(buildItemView(item));
        }
    }

    // =====================================================
    // 3. SUGGEST SHOES FOR THE PICKED CLOTHES
    // =====================================================

    private void suggestShoesForPicked() {

        if (pickedItems.isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    "Pick your clothes first.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        boolean hasMainPiece = false;

        for (Clothing c : pickedItems) {
            String category = c.getCategory();
            if ("Top".equalsIgnoreCase(category)
                    || "Bottom".equalsIgnoreCase(category)
                    || "Dress".equalsIgnoreCase(category)) {
                hasMainPiece = true;
            }
        }

        if (!hasMainPiece) {
            Toast.makeText(
                    requireContext(),
                    "Pick at least a Top, a Bottom or a Dress.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String[] filters = readFilters();
        final String finalOccasion = filters[0];
        final String finalSeason = filters[1];

        final List<Clothing> picked = new ArrayList<>(pickedItems);

        btnSuggestShoes.setEnabled(false);
        txtStatus.setText("Finding shoes...");
        layoutResults.removeAllViews();

        executor.execute(() -> {

            List<Clothing> wardrobe =
                    clothingDao.getClothingByUser(userId);

            List<OutfitRecommender.Outfit> options =
                    OutfitRecommender.suggestShoes(
                            picked,
                            wardrobe,
                            finalOccasion,
                            finalSeason,
                            5
                    );

            String advice =
                    OutfitRecommender.shoeAdvice(
                            picked,
                            finalOccasion,
                            finalSeason
                    );

            if (getActivity() == null) {
                return;
            }

            getActivity().runOnUiThread(() -> {

                if (!isAdded()) {
                    return;
                }

                btnSuggestShoes.setEnabled(true);

                if (options.isEmpty()) {

                    txtStatus.setText(
                            "You do not have any shoes saved yet. "
                                    + "Add some in Wardrobe > Add Clothing "
                                    + "and I will match them.\n\n"
                                    + "Shoe tip: " + advice
                    );

                    return;
                }

                txtStatus.setText(
                        "Best shoes for your outfit:\n\n"
                                + "Shoe tip: " + advice
                );

                int number = 1;

                for (OutfitRecommender.Outfit option : options) {
                    layoutResults.addView(
                            buildOutfitCard("Shoe option " + number, option)
                    );
                    number++;
                }
            });
        });
    }

    private void selectInSpinner(Spinner spinner, String value) {

        for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
            if (spinner.getAdapter().getItem(i).toString()
                    .equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    // =====================================================
    // CARD VIEWS (built in code, so no extra layout files)
    // =====================================================

    private View buildOutfitCard(
            String heading,
            OutfitRecommender.Outfit outfit) {

        Context context = requireContext();

        CardView card = new CardView(context);
        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        cardParams.setMargins(0, dp(8), 0, dp(8));
        card.setLayoutParams(cardParams);
        card.setRadius(dp(12));
        card.setCardElevation(dp(3));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.addView(content);

        int percent = (int) Math.max(0, Math.min(100, Math.round(outfit.score)));

        TextView title = new TextView(context);
        title.setText(heading + "  \u2022  " + percent + "% match");
        title.setTextSize(18);
        title.setTextColor(0xFF222222);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(title);

        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, dp(6));
        scroll.addView(row);
        content.addView(scroll);

        for (Clothing item : outfit.items) {
            row.addView(buildItemView(item));
        }

        StringBuilder reasons = new StringBuilder();
        for (String reason : outfit.reasons) {
            reasons.append("\u2022 ").append(reason).append("\n");
        }

        TextView why = new TextView(context);
        why.setText(reasons.toString().trim());
        why.setTextSize(13);
        why.setTextColor(0xFF555555);
        why.setPadding(0, dp(4), 0, dp(8));
        content.addView(why);

        final Button wear = new Button(context);
        wear.setText("I'M WEARING THIS");
        wear.setOnClickListener(v -> markWorn(outfit, wear));
        content.addView(wear);

        return card;
    }

    private View buildItemView(Clothing item) {

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
        image.setLayoutParams(new LinearLayout.LayoutParams(dp(96), dp(120)));
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

    // =====================================================
    // WEAR TRACKING (updates wearCount / lastWorn in Room)
    // =====================================================

    private void markWorn(OutfitRecommender.Outfit outfit, Button button) {

        button.setEnabled(false);

        final String today =
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(new Date());

        executor.execute(() -> {

            for (Clothing item : outfit.items) {

                // Re-read from the database so counts never go stale.
                Clothing fresh = clothingDao.getClothingById(item.getId());

                if (fresh == null) {
                    continue;
                }

                fresh.setWearCount(fresh.getWearCount() + 1);
                fresh.setLastWorn(today);
                clothingDao.updateClothing(fresh);
            }

            if (getActivity() == null) {
                return;
            }

            getActivity().runOnUiThread(() -> {

                if (!isAdded()) {
                    return;
                }

                button.setText("MARKED AS WORN TODAY");

                Toast.makeText(
                        requireContext(),
                        "Saved. Future ideas will favour other items.",
                        Toast.LENGTH_SHORT
                ).show();
            });
        });
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