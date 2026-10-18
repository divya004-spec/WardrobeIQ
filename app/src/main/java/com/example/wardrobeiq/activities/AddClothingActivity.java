package com.example.wardrobeiq.activities;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import com.example.wardrobeiq.utils.ClothingImageAnalyzer;

import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Intent;
import android.provider.MediaStore;
import android.content.Context;
import android.content.SharedPreferences;
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
import com.example.wardrobeiq.models.ClothingAnalysisResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.example.wardrobeiq.network.ClothingImageUploader;
public class AddClothingActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 200;
    private String savedImagePath = "";
    private ClothingImageAnalyzer imageAnalyzer;
    private int analysisRequestId = 0;
    private boolean mlKitLooksLikeClothing = true;
    ImageView imgClothingPreview;

    Button btnTakePhoto;
    Button btnChooseGallery;
    Button btnRemovePhoto;
    Button btnSaveClothing;

    Spinner spinnerCategory;
    Spinner spinnerColor;
    Spinner spinnerPattern;
    Spinner spinnerOccasion;
    Spinner spinnerSeason;
    Spinner spinnerWarmth;
    Spinner spinnerFormality;

    ClothingDao clothingDao;

    ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    int userId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_clothing);


        imageAnalyzer =
                new ClothingImageAnalyzer();

        // -----------------------------
        // Find Views
        // -----------------------------

        imgClothingPreview =
                findViewById(R.id.imgClothingPreview);

        btnTakePhoto =
                findViewById(R.id.btnTakePhoto);

        btnChooseGallery =
                findViewById(R.id.btnChooseGallery);

        btnRemovePhoto =
                findViewById(R.id.btnRemovePhoto);

        btnSaveClothing =
                findViewById(R.id.btnSaveClothing);


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


        // -----------------------------
        // Get Logged-in User
        // -----------------------------

        SharedPreferences preferences =
                getSharedPreferences(
                        "WardrobeIQSession",
                        Context.MODE_PRIVATE
                );

        userId =
                preferences.getInt("userId", -1);


        // -----------------------------
        // Database
        // -----------------------------

        AppDatabase database =
                AppDatabase.getInstance(this);

        clothingDao =
                database.clothingDao();


        // -----------------------------
        // Setup Dropdowns
        // -----------------------------

        setupSpinners();


        // -----------------------------
        // Camera Button
        // -----------------------------

        btnTakePhoto.setOnClickListener(v -> {

            Intent cameraIntent =
                    new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            try {

                startActivityForResult(
                        cameraIntent,
                        CAMERA_REQUEST_CODE
                );

            } catch (Exception e) {

                e.printStackTrace();

                Toast.makeText(
                        AddClothingActivity.this,
                        "Camera app is not available",
                        Toast.LENGTH_LONG
                ).show();
            }
        });


        // -----------------------------
        // Gallery Button
        // -----------------------------

        btnChooseGallery.setOnClickListener(v -> {

            Intent galleryIntent =
                    new Intent(Intent.ACTION_PICK);

            galleryIntent.setType("image/*");

            startActivityForResult(
                    galleryIntent,
                    GALLERY_REQUEST_CODE
            );
        });

        // -----------------------------
        // Opened from Home: start camera / gallery straight away
        // -----------------------------

        final String autoLaunch = getIntent().getStringExtra("autoLaunch");

        if (autoLaunch != null) {

            getWindow().getDecorView().post(() -> {

                if ("camera".equals(autoLaunch)) {
                    btnTakePhoto.performClick();
                } else if ("gallery".equals(autoLaunch)) {
                    btnChooseGallery.performClick();
                }
            });
        }

        btnRemovePhoto.setOnClickListener(v -> {

            clearPhoto();

            Toast.makeText(
                    AddClothingActivity.this,
                    "Photo removed",
                    Toast.LENGTH_SHORT
            ).show();
        });


        // -----------------------------
        // Save Button
        // -----------------------------

        btnSaveClothing.setOnClickListener(v -> {

            saveClothing();

        });
    }


    // =====================================================
    // IMAGE ANALYSIS (ML Kit hint + backend AI)
    // =====================================================

    private void analyzeClothingImage(Bitmap bitmap) {

        // Every new photo gets a new id, so an old, slow response
        // can never overwrite the form for a newer photo.
        final int requestId = ++analysisRequestId;

        setAnalyzing(true);

        imageAnalyzer.analyze(
                bitmap,
                new ClothingImageAnalyzer.AnalysisCallback() {

                    @Override
                    public void onWardrobeItemDetected(
                            java.util.List<com.google.mlkit.vision.label.ImageLabel> labels) {

                        runOnUiThread(() -> {
                            mlKitLooksLikeClothing = true;
                            sendToBackend(requestId);
                        });
                    }

                    @Override
                    public void onNotWardrobeItem() {

                        // ML Kit's generic labels are only a hint.
                        // The backend AI makes the final decision.
                        runOnUiThread(() -> {
                            mlKitLooksLikeClothing = false;
                            sendToBackend(requestId);
                        });
                    }

                    @Override
                    public void onError(Exception exception) {

                        runOnUiThread(() -> {
                            mlKitLooksLikeClothing = true;
                            sendToBackend(requestId);
                        });
                    }
                }
        );
    }

    private void sendToBackend(int requestId) {

        if (requestId != analysisRequestId) {
            return; // user picked another photo meanwhile
        }

        ClothingImageUploader.upload(
                AddClothingActivity.this,
                savedImagePath,
                new ClothingImageUploader.UploadCallback() {

                    @Override
                    public void onResult(ClothingAnalysisResult result) {

                        if (requestId != analysisRequestId || isFinishing()) {
                            return;
                        }

                        setAnalyzing(false);

                        if (!result.isWardrobeItem()) {
                            showNotWardrobeDialog();
                            return;
                        }

                        fillFormFromResult(result);

                        Toast.makeText(
                                AddClothingActivity.this,
                                "Details filled automatically. "
                                        + "Please review them, then tap Save.",
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    @Override
                    public void onError(String message) {

                        if (requestId != analysisRequestId || isFinishing()) {
                            return;
                        }

                        setAnalyzing(false);

                        if (!mlKitLooksLikeClothing) {
                            showNotWardrobeDialog();
                            return;
                        }

                        Toast.makeText(
                                AddClothingActivity.this,
                                message + "\nYou can fill the details manually.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    // Disables Save while the AI is working.
    private void setAnalyzing(boolean analyzing) {

        btnSaveClothing.setEnabled(!analyzing);

        btnSaveClothing.setText(
                analyzing ? "Analyzing photo..." : "SAVE CLOTHING"
        );
    }

    // Puts the AI values into the dropdowns. The user can still change them.
    private void fillFormFromResult(ClothingAnalysisResult result) {

        selectByText(spinnerCategory, result.getCategory());
        selectByText(spinnerColor, result.getColor());
        selectByText(spinnerPattern, result.getPattern());
        selectByText(spinnerOccasion, result.getOccasion());
        selectByText(spinnerSeason, result.getSeason());

        // Warmth/Formality items are "1 - ...", "2 - ..." so index == value.
        selectByLevel(spinnerWarmth, result.getWarmth());
        selectByLevel(spinnerFormality, result.getFormality());
    }

    private void selectByText(Spinner spinner, String value) {

        if (value == null || value.trim().isEmpty()) {
            return;
        }

        for (int i = 0; i < spinner.getAdapter().getCount(); i++) {

            String item = String.valueOf(spinner.getAdapter().getItem(i));

            if (item.equalsIgnoreCase(value.trim())) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void selectByLevel(Spinner spinner, int level) {

        if (level >= 1 && level < spinner.getAdapter().getCount()) {
            spinner.setSelection(level);
        }
    }

    private void showNotWardrobeDialog() {

        new AlertDialog.Builder(this)
                .setTitle("Not a wardrobe item")
                .setMessage(
                        "This photo does not appear to contain " +
                                "a clothing or fashion item.\n\n" +
                                "Please upload a photo of clothes, shoes, " +
                                "sandals, accessories, or another wardrobe item."
                )
                .setPositiveButton(
                        "OK",
                        (dialog, which) -> clearPhoto()
                )
                .setOnCancelListener(dialog -> clearPhoto())
                .show();
    }

    private void clearPhoto() {

        analysisRequestId++;
        savedImagePath = "";

        imgClothingPreview.setImageResource(
                android.R.drawable.ic_menu_gallery
        );

        setAnalyzing(false);
    }

    // =====================================================
    // SAVE CLOTHING
    // =====================================================

    private void saveClothing() {

        // Check user session

        if (userId == -1) {

            Toast.makeText(
                    this,
                    "User session not found",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Check if a photo was taken

        if (savedImagePath.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please take a photo first",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Get selected values

        String category =
                spinnerCategory.getSelectedItem().toString();

        String color =
                spinnerColor.getSelectedItem().toString();

        String pattern =
                spinnerPattern.getSelectedItem().toString();

        String occasion =
                spinnerOccasion.getSelectedItem().toString();

        String season =
                spinnerSeason.getSelectedItem().toString();

        String warmthText =
                spinnerWarmth.getSelectedItem().toString();

        String formalityText =
                spinnerFormality.getSelectedItem().toString();


        // -----------------------------
        // Validation
        // -----------------------------

        if (category.equals("Select Category")) {

            Toast.makeText(
                    this,
                    "Please select a category",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (color.equals("Select Color")) {

            Toast.makeText(
                    this,
                    "Please select a color",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (pattern.equals("Select Pattern")) {

            Toast.makeText(
                    this,
                    "Please select a pattern",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (occasion.equals("Select Occasion")) {

            Toast.makeText(
                    this,
                    "Please select an occasion",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (season.equals("Select Season")) {

            Toast.makeText(
                    this,
                    "Please select a season",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (warmthText.equals("Select Warmth")) {

            Toast.makeText(
                    this,
                    "Please select warmth level",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (formalityText.equals("Select Formality")) {

            Toast.makeText(
                    this,
                    "Please select formality",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // -----------------------------
        // Convert numbers
        // -----------------------------

        int warmth =
                Integer.parseInt(
                        warmthText.substring(0, 1)
                );

        int formality =
                Integer.parseInt(
                        formalityText.substring(0, 1)
                );


        // -----------------------------
        // Create Clothing Object
        // -----------------------------

        Clothing clothing =
                new Clothing(
                        userId,

                        // Image path
                        savedImagePath,

                        category,

                        color,

                        pattern,

                        occasion,

                        season,

                        warmth,

                        formality,

                        // Initial wear count
                        0,

                        // Never worn
                        ""
                );


        // -----------------------------
        // Insert into Room
        // -----------------------------

        executorService.execute(() -> {

            long clothingId =
                    clothingDao.insertClothing(clothing);


            runOnUiThread(() -> {

                if (clothingId > 0) {

                    Toast.makeText(
                            AddClothingActivity.this,
                            "Clothing added successfully!",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();

                } else {

                    Toast.makeText(
                            AddClothingActivity.this,
                            "Failed to add clothing",
                            Toast.LENGTH_SHORT
                    ).show();

                }

            });

        });
    }


    // =====================================================
    // SPINNERS
    // =====================================================

    private void setupSpinners() {

        setupSpinner(
                spinnerCategory,
                new String[]{
                        "Select Category",
                        "Top",
                        "Bottom",
                        "Shoes",
                        "Layer",
                        "Dress",
                        "Accessory"
                }
        );


        setupSpinner(
                spinnerColor,
                new String[]{
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
                }
        );


        setupSpinner(
                spinnerPattern,
                new String[]{
                        "Select Pattern",
                        "Solid",
                        "Striped",
                        "Checked",
                        "Printed",
                        "Floral",
                        "Graphic",
                        "Polka Dot"
                }
        );


        setupSpinner(
                spinnerOccasion,
                new String[]{
                        "Select Occasion",
                        "Casual",
                        "College",
                        "Work",
                        "Party",
                        "Formal",
                        "Sports"
                }
        );


        setupSpinner(
                spinnerSeason,
                new String[]{
                        "Select Season",
                        "Summer",
                        "Winter",
                        "Monsoon",
                        "All Season"
                }
        );


        setupSpinner(
                spinnerWarmth,
                new String[]{
                        "Select Warmth",
                        "1 - Very Light",
                        "2 - Light",
                        "3 - Medium",
                        "4 - Warm",
                        "5 - Very Warm"
                }
        );


        setupSpinner(
                spinnerFormality,
                new String[]{
                        "Select Formality",
                        "1 - Very Casual",
                        "2 - Casual",
                        "3 - Smart Casual",
                        "4 - Formal",
                        "5 - Very Formal"
                }
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


    // =====================================================
    // CLEANUP
    // =====================================================

    @Override
    protected void onDestroy() {

        super.onDestroy();

        executorService.shutdown();
    }


    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        // CAMERA PHOTO
        if (requestCode == CAMERA_REQUEST_CODE) {

            Bitmap photo = data.getExtras() != null
                    ? (Bitmap) data.getExtras().get("data")
                    : null;

            if (photo == null) {
                Toast.makeText(
                        this,
                        "Could not capture photo",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            imgClothingPreview.setImageBitmap(photo);

            // Save FIRST, then analyze.
            savedImagePath = saveImageToInternalStorage(photo);

            if (savedImagePath.isEmpty()) {
                Toast.makeText(
                        this,
                        "Could not save photo",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            analyzeClothingImage(photo);
        }

        // GALLERY PHOTO
        else if (requestCode == GALLERY_REQUEST_CODE) {

            Uri selectedImageUri = data.getData();

            if (selectedImageUri == null) {
                Toast.makeText(
                        this,
                        "No image selected",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            try {
                Bitmap bitmap;

                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    android.graphics.ImageDecoder.Source source =
                            android.graphics.ImageDecoder.createSource(
                                    getContentResolver(),
                                    selectedImageUri
                            );

                    bitmap = android.graphics.ImageDecoder.decodeBitmap(source);

                } else {
                    bitmap = android.provider.MediaStore.Images.Media.getBitmap(
                            getContentResolver(),
                            selectedImageUri
                    );
                }

                imgClothingPreview.setImageBitmap(bitmap);

                // Save FIRST, then analyze.
                savedImagePath = saveImageToInternalStorage(bitmap);

                if (savedImagePath.isEmpty()) {
                    Toast.makeText(
                            this,
                            "Could not save gallery photo",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                analyzeClothingImage(bitmap);

            } catch (Exception e) {
                e.printStackTrace();

                Toast.makeText(
                        this,
                        "Could not load selected image",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }



// =====================================================
// SAVE IMAGE TO INTERNAL STORAGE
// =====================================================

    private String saveImageToInternalStorage(Bitmap bitmap) {

        String fileName =
                "clothing_" +
                        System.currentTimeMillis() +
                        ".jpg";


        // Create clothing_images folder
        File directory =
                new File(
                        getFilesDir(),
                        "clothing_images"
                );


        if (!directory.exists()) {

            directory.mkdirs();
        }


        // Create image file
        File imageFile =
                new File(
                        directory,
                        fileName
                );


        // Shrink big photos (max 1280 px) so uploads are fast and
        // stay under the server's 10 MB limit.
        final int maxSize = 1280;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        if (Math.max(w, h) > maxSize) {
            float scale = (float) maxSize / Math.max(w, h);
            bitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    Math.round(w * scale),
                    Math.round(h * scale),
                    true
            );
        }

        try {

            FileOutputStream outputStream =
                    new FileOutputStream(imageFile);


            // Compress bitmap as JPEG
            bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    90,
                    outputStream
            );


            outputStream.flush();
            outputStream.close();


            // Return complete file path
            return imageFile.getAbsolutePath();


        } catch (IOException e) {

            e.printStackTrace();

            return "";
        }
    }
}