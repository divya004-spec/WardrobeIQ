package com.example.wardrobeiq.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.wardrobeiq.R;
import com.example.wardrobeiq.database.AppDatabase;
import com.example.wardrobeiq.database.ClothingDao;
import com.example.wardrobeiq.models.Clothing;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditClothingActivity extends AppCompatActivity {

    private ImageView imgClothingPreview;

    private Spinner spinnerCategory;
    private Spinner spinnerColor;
    private Spinner spinnerPattern;
    private Spinner spinnerOccasion;
    private Spinner spinnerSeason;
    private Spinner spinnerWarmth;
    private Spinner spinnerFormality;

    private Button btnSaveChanges;

    private ClothingDao clothingDao;

    private Clothing clothing;

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    private final String[] categories = {
            "Select Category",
            "Top",
            "Bottom",
            "Shoes",
            "Layer",
            "Dress",
            "Accessory"
    };

    private final String[] colors = {
            "Select Color",
            "Black",
            "White",
            "Blue",
            "Grey",
            "Brown",
            "Beige",
            "Red",
            "Green",
            "Yellow",
            "Pink",
            "Purple",
            "Orange"
    };

    private final String[] patterns = {
            "Select Pattern",
            "Solid",
            "Striped",
            "Checked",
            "Printed",
            "Floral",
            "Graphic",
            "Polka Dot"
    };

    private final String[] occasions = {
            "Select Occasion",
            "Casual",
            "College",
            "Work",
            "Party",
            "Formal",
            "Sports"
    };

    private final String[] seasons = {
            "Select Season",
            "Summer",
            "Winter",
            "Monsoon",
            "All Season"
    };

    private final String[] warmthValues = {
            "Select Warmth",
            "1 - Very Light",
            "2 - Light",
            "3 - Medium",
            "4 - Warm",
            "5 - Very Warm"
    };

    private final String[] formalityValues = {
            "Select Formality",
            "1 - Very Casual",
            "2 - Casual",
            "3 - Smart Casual",
            "4 - Formal",
            "5 - Very Formal"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_clothing);

        imgClothingPreview =
                findViewById(R.id.imgClothingPreview);

        spinnerCategory =
                findViewById(R.id.spinnerCategory);

        spinnerColor =
                findViewById(R.id.spinnerColor);

        spinnerPattern =
                findViewById(R.id.spinnerPattern);

        spinnerOccasion =
                findViewById(R.id.spinnerOccasion);

        spinnerSeason =
                findViewById(R.id.spinnerSeason);

        spinnerWarmth =
                findViewById(R.id.spinnerWarmth);

        spinnerFormality =
                findViewById(R.id.spinnerFormality);

        btnSaveChanges =
                findViewById(R.id.btnSaveChanges);

        // Setup database
        AppDatabase database =
                AppDatabase.getInstance(this);

        clothingDao =
                database.clothingDao();

        // Setup spinners
        setupSpinner(
                spinnerCategory,
                categories
        );

        setupSpinner(
                spinnerColor,
                colors
        );

        setupSpinner(
                spinnerPattern,
                patterns
        );

        setupSpinner(
                spinnerOccasion,
                occasions
        );

        setupSpinner(
                spinnerSeason,
                seasons
        );

        setupSpinner(
                spinnerWarmth,
                warmthValues
        );

        setupSpinner(
                spinnerFormality,
                formalityValues
        );

        // Get clothing ID
        int clothingId =
                getIntent().getIntExtra(
                        "clothingId",
                        -1
                );

        if (clothingId == -1) {

            Toast.makeText(
                    this,
                    "Clothing item not found",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        // Load existing item
        loadClothing(clothingId);

        // Save changes
        btnSaveChanges.setOnClickListener(
                v -> saveChanges()
        );
    }

    private void setupSpinner(
            Spinner spinner,
            String[] values) {

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        values
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(adapter);
    }

    private void loadClothing(int clothingId) {

        executorService.execute(() -> {

            Clothing result =
                    clothingDao.getClothingById(
                            clothingId
                    );

            runOnUiThread(() -> {

                if (result == null) {

                    Toast.makeText(
                            EditClothingActivity.this,
                            "Clothing item not found",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                    return;
                }

                clothing = result;

                // Display image
                String imagePath =
                        clothing.getImagePath();

                if (imagePath != null
                        && !imagePath.isEmpty()) {

                    Bitmap bitmap =
                            BitmapFactory.decodeFile(
                                    imagePath
                            );

                    if (bitmap != null) {

                        imgClothingPreview
                                .setImageBitmap(bitmap);

                    }
                }

                // Select existing values
                setSpinnerValue(
                        spinnerCategory,
                        categories,
                        clothing.getCategory()
                );

                setSpinnerValue(
                        spinnerColor,
                        colors,
                        clothing.getColor()
                );

                setSpinnerValue(
                        spinnerPattern,
                        patterns,
                        clothing.getPattern()
                );

                setSpinnerValue(
                        spinnerOccasion,
                        occasions,
                        clothing.getOccasion()
                );

                setSpinnerValue(
                        spinnerSeason,
                        seasons,
                        clothing.getSeason()
                );

                setSpinnerValue(
                        spinnerWarmth,
                        warmthValues,
                        clothing.getWarmth()
                                + " - "
                                + getWarmthText(
                                clothing.getWarmth()
                        )
                );

                setSpinnerValue(
                        spinnerFormality,
                        formalityValues,
                        clothing.getFormality()
                                + " - "
                                + getFormalityText(
                                clothing.getFormality()
                        )
                );
            });
        });
    }

    private void setSpinnerValue(
            Spinner spinner,
            String[] values,
            String value) {

        for (int i = 0; i < values.length; i++) {

            if (values[i].equalsIgnoreCase(value)) {

                spinner.setSelection(i);
                return;
            }
        }
    }

    private String getWarmthText(int value) {

        switch (value) {

            case 1:
                return "Very Light";

            case 2:
                return "Light";

            case 3:
                return "Medium";

            case 4:
                return "Warm";

            case 5:
                return "Very Warm";

            default:
                return "";
        }
    }

    private String getFormalityText(int value) {

        switch (value) {

            case 1:
                return "Very Casual";

            case 2:
                return "Casual";

            case 3:
                return "Smart Casual";

            case 4:
                return "Formal";

            case 5:
                return "Very Formal";

            default:
                return "";
        }
    }

    private void saveChanges() {

        if (clothing == null) {
            return;
        }

        String category =
                spinnerCategory
                        .getSelectedItem()
                        .toString();

        String color =
                spinnerColor
                        .getSelectedItem()
                        .toString();

        String pattern =
                spinnerPattern
                        .getSelectedItem()
                        .toString();

        String occasion =
                spinnerOccasion
                        .getSelectedItem()
                        .toString();

        String season =
                spinnerSeason
                        .getSelectedItem()
                        .toString();

        String warmthValue =
                spinnerWarmth
                        .getSelectedItem()
                        .toString();

        String formalityValue =
                spinnerFormality
                        .getSelectedItem()
                        .toString();

        // Validate selections
        if (category.equals("Select Category")
                || color.equals("Select Color")
                || pattern.equals("Select Pattern")
                || occasion.equals("Select Occasion")
                || season.equals("Select Season")
                || warmthValue.equals("Select Warmth")
                || formalityValue.equals("Select Formality")) {

            Toast.makeText(
                    this,
                    "Please select all fields",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Extract numeric values
        int warmth =
                Integer.parseInt(
                        warmthValue.substring(0, 1)
                );

        int formality =
                Integer.parseInt(
                        formalityValue.substring(0, 1)
                );

        // Update object
        clothing =
                updateClothingObject(
                        clothing,
                        category,
                        color,
                        pattern,
                        occasion,
                        season,
                        warmth,
                        formality
                );

        // Update Room
        executorService.execute(() -> {

            clothingDao.updateClothing(
                    clothing
            );

            runOnUiThread(() -> {

                Toast.makeText(
                        EditClothingActivity.this,
                        "Changes saved successfully",
                        Toast.LENGTH_SHORT
                ).show();

                finish();
            });
        });
    }

    private Clothing updateClothingObject(
            Clothing oldClothing,
            String category,
            String color,
            String pattern,
            String occasion,
            String season,
            int warmth,
            int formality) {

        Clothing updated =
                new Clothing(
                        oldClothing.getUserId(),
                        oldClothing.getImagePath(),
                        category,
                        color,
                        pattern,
                        occasion,
                        season,
                        warmth,
                        formality,
                        oldClothing.getWearCount(),
                        oldClothing.getLastWorn()
                );

        updated.setId(
                oldClothing.getId()
        );

        return updated;
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        executorService.shutdown();
    }
}