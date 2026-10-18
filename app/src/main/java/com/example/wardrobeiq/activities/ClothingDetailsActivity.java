package com.example.wardrobeiq.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.wardrobeiq.R;
import com.example.wardrobeiq.database.AppDatabase;
import com.example.wardrobeiq.database.ClothingDao;
import com.example.wardrobeiq.models.Clothing;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClothingDetailsActivity extends AppCompatActivity {

    private ImageView imgClothing;

    private TextView txtCategory;
    private TextView txtColor;
    private TextView txtPattern;
    private TextView txtOccasion;
    private TextView txtSeason;
    private TextView txtWarmth;
    private TextView txtFormality;
    private TextView txtWearCount;
    private TextView txtLastWorn;

    private Button btnMatch;
    private Button btnEdit;
    private Button btnDelete;

    private Clothing selectedClothing;

    private ClothingDao clothingDao;

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_clothing_details);

        // Find views
        imgClothing = findViewById(R.id.imgClothing);

        txtCategory = findViewById(R.id.txtCategory);
        txtColor = findViewById(R.id.txtColor);
        txtPattern = findViewById(R.id.txtPattern);
        txtOccasion = findViewById(R.id.txtOccasion);
        txtSeason = findViewById(R.id.txtSeason);
        txtWarmth = findViewById(R.id.txtWarmth);
        txtFormality = findViewById(R.id.txtFormality);
        txtWearCount = findViewById(R.id.txtWearCount);
        txtLastWorn = findViewById(R.id.txtLastWorn);

        btnMatch = findViewById(R.id.btnMatch);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

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

        // Get database
        AppDatabase database =
                AppDatabase.getInstance(this);

        clothingDao =
                database.clothingDao();

        // Load clothing
        loadClothing(clothingId);

        // Match button
        btnMatch.setOnClickListener(v -> {

            if (selectedClothing == null) {

                Toast.makeText(
                        this,
                        "Clothing item not found",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            android.content.Intent matchIntent =
                    new android.content.Intent(
                            ClothingDetailsActivity.this,
                            MatchActivity.class
                    );

            matchIntent.putExtra(
                    "clothingId",
                    selectedClothing.getId()
            );

            startActivity(matchIntent);
        });

        // Edit button
        btnEdit.setOnClickListener(v -> {

            if (selectedClothing == null) {

                Toast.makeText(
                        this,
                        "Clothing item not found",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            android.content.Intent intent =
                    new android.content.Intent(
                            ClothingDetailsActivity.this,
                            EditClothingActivity.class
                    );

            intent.putExtra(
                    "clothingId",
                    selectedClothing.getId()
            );

            startActivity(intent);
        });


        // Delete button
        btnDelete.setOnClickListener(v -> {

            new androidx.appcompat.app.AlertDialog.Builder(
                    ClothingDetailsActivity.this
            )
                    .setTitle("Delete Clothing")
                    .setMessage(
                            "Are you sure you want to delete this clothing item?"
                    )
                    .setNegativeButton(
                            "CANCEL",
                            null
                    )
                    .setPositiveButton(
                            "DELETE",
                            (dialog, which) -> deleteClothing()
                    )
                    .show();
        });
    }

    private void loadClothing(int clothingId) {

        executorService.execute(() -> {

            Clothing clothing =
                    clothingDao.getClothingById(
                            clothingId
                    );

            runOnUiThread(() -> {

                if (clothing == null) {

                    Toast.makeText(
                            ClothingDetailsActivity.this,
                            "Clothing item not found",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                    return;
                }

                selectedClothing = clothing;

                displayClothing(clothing);
            });
        });
    }

    private void displayClothing(Clothing clothing) {

        // Category
        txtCategory.setText(
                clothing.getCategory()
        );

        // Other attributes
        txtColor.setText(
                "Color: " + clothing.getColor()
        );

        txtPattern.setText(
                "Pattern: " + clothing.getPattern()
        );

        txtOccasion.setText(
                "Occasion: " + clothing.getOccasion()
        );

        txtSeason.setText(
                "Season: " + clothing.getSeason()
        );

        txtWarmth.setText(
                "Warmth: " + clothing.getWarmth()
        );

        txtFormality.setText(
                "Formality: " + clothing.getFormality()
        );

        txtWearCount.setText(
                "Times worn: "
                        + clothing.getWearCount()
        );

        String lastWorn =
                clothing.getLastWorn();

        if (lastWorn == null
                || lastWorn.isEmpty()) {

            txtLastWorn.setText(
                    "Last worn: Never"
            );

        } else {

            txtLastWorn.setText(
                    "Last worn: " + lastWorn
            );
        }

        // Load image
        String imagePath =
                clothing.getImagePath();

        if (imagePath != null
                && !imagePath.isEmpty()) {

            Bitmap bitmap =
                    BitmapFactory.decodeFile(
                            imagePath
                    );

            if (bitmap != null) {

                imgClothing.setImageBitmap(
                        bitmap
                );

            } else {

                imgClothing.setImageResource(
                        android.R.drawable.ic_menu_gallery
                );
            }

        } else {

            imgClothing.setImageResource(
                    android.R.drawable.ic_menu_gallery
            );
        }
    }

    private void deleteClothing() {

        if (selectedClothing == null) {

            Toast.makeText(
                    this,
                    "Clothing item not found",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        executorService.execute(() -> {

            clothingDao.deleteClothing(
                    selectedClothing
            );

            runOnUiThread(() -> {

                Toast.makeText(
                        ClothingDetailsActivity.this,
                        "Clothing item deleted",
                        Toast.LENGTH_SHORT
                ).show();

                finish();
            });
        });
    }

    // ✅ ADD onResume() HERE

    @Override
    protected void onResume() {

        super.onResume();

        int clothingId =
                getIntent().getIntExtra(
                        "clothingId",
                        -1
                );

        if (clothingId != -1
                && clothingDao != null) {

            loadClothing(clothingId);
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        executorService.shutdown();
    }


}