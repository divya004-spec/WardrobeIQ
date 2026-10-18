package com.example.wardrobeiq.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.wardrobeiq.R;
import com.example.wardrobeiq.database.AppDatabase;
import com.example.wardrobeiq.database.ClothingDao;
import com.example.wardrobeiq.models.Clothing;
import com.example.wardrobeiq.utils.OutfitRecommender;
import com.example.wardrobeiq.utils.OutfitViews;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Two modes:
 *  - with extra "clothingId": "MATCH THIS ITEM" -> outfits built around that item
 *  - without it: "MATCH MY WARDROBE" -> best outfits from the whole wardrobe
 */
public class MatchActivity extends AppCompatActivity {

    private static final String[] OCCASIONS = {
            "Any", "Casual", "College", "Work", "Party", "Formal", "Sports"
    };

    private static final String[] SEASONS = {
            "Any", "Summer", "Winter", "Monsoon"
    };

    private Spinner spinnerOccasion;
    private Spinner spinnerSeason;
    private Button btnFind;
    private TextView txtStatus;
    private LinearLayout layoutResults;
    private LinearLayout layoutAnchor;

    private ClothingDao clothingDao;
    private int userId;
    private int clothingId = -1;
    private Clothing anchor;

    private boolean spinnersReady = false;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_match);

        TextView txtTitle = findViewById(R.id.txtMatchTitle);
        TextView txtSubtitle = findViewById(R.id.txtMatchSubtitle);
        spinnerOccasion = findViewById(R.id.spinnerMatchOccasion);
        spinnerSeason = findViewById(R.id.spinnerMatchSeason);
        btnFind = findViewById(R.id.btnFindMatches);
        txtStatus = findViewById(R.id.txtMatchStatus);
        layoutResults = findViewById(R.id.layoutMatchResults);
        layoutAnchor = findViewById(R.id.layoutAnchor);

        setupSpinner(spinnerOccasion, OCCASIONS);
        setupSpinner(spinnerSeason, SEASONS);

        SharedPreferences prefs =
                getSharedPreferences("WardrobeIQSession", Context.MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);

        clothingId = getIntent().getIntExtra("clothingId", -1);

        clothingDao = AppDatabase.getInstance(this).clothingDao();

        if (userId == -1) {
            Toast.makeText(this, "User session not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (clothingId != -1) {
            txtTitle.setText("Match This Item");
            txtSubtitle.setText("Outfits built around this piece.");
        }

        btnFind.setOnClickListener(v -> findMatches());

        AdapterView.OnItemSelectedListener autoRun =
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        if (spinnersReady) {
                            findMatches();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                };

        spinnerOccasion.setOnItemSelectedListener(autoRun);
        spinnerSeason.setOnItemSelectedListener(autoRun);

        // Load the chosen item first (if any), then show matches straight away.
        executor.execute(() -> {

            if (clothingId != -1) {
                anchor = clothingDao.getClothingById(clothingId);
            }

            runOnUiThread(() -> {

                if (isFinishing()) {
                    return;
                }

                if (clothingId != -1 && anchor == null) {
                    Toast.makeText(this, "Clothing item not found",
                            Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                if (anchor != null) {
                    layoutAnchor.removeAllViews();
                    layoutAnchor.addView(OutfitViews.buildItemView(this, anchor));
                }

                spinnersReady = true;
                findMatches();
            });
        });
    }

    private void setupSpinner(Spinner spinner, String[] values) {

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, values);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void findMatches() {

        final String occasion = spinnerOccasion.getSelectedItem().toString();
        final String season = spinnerSeason.getSelectedItem().toString();

        btnFind.setEnabled(false);
        txtStatus.setText("Finding matches...");
        layoutResults.removeAllViews();

        executor.execute(() -> {

            List<Clothing> wardrobe = clothingDao.getClothingByUser(userId);

            List<OutfitRecommender.Outfit> outfits;

            if (anchor != null) {
                outfits = OutfitRecommender.matchItem(
                        anchor, wardrobe, occasion, season, 5);
            } else {
                outfits = OutfitRecommender.recommend(
                        wardrobe, occasion, season, 5);
            }

            final String missing = outfits.isEmpty() ? buildMissingMessage(wardrobe) : "";

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                btnFind.setEnabled(true);

                if (outfits.isEmpty()) {
                    txtStatus.setText(missing);
                    return;
                }

                txtStatus.setText(outfits.size() + " match"
                        + (outfits.size() > 1 ? "es" : "")
                        + " for " + occasion + " / " + season);

                int number = 1;

                for (OutfitRecommender.Outfit outfit : outfits) {
                    layoutResults.addView(OutfitViews.buildOutfitCard(
                            this,
                            "Match " + number,
                            outfit,
                            "I'M WEARING THIS",
                            null));
                    number++;
                }
            });
        });
    }

    private String buildMissingMessage(List<Clothing> wardrobe) {

        if (anchor == null) {
            return OutfitRecommender.explainMissing(wardrobe);
        }

        return "Nothing in your wardrobe goes with this " + anchor.getCategory()
                + " yet. Add more pieces (for example a "
                + suggestPartner(anchor.getCategory()) + ") and try again.";
    }

    private String suggestPartner(String category) {

        if ("Top".equalsIgnoreCase(category)) {
            return "Bottom";
        }

        if ("Bottom".equalsIgnoreCase(category)) {
            return "Top";
        }

        if ("Shoes".equalsIgnoreCase(category)) {
            return "Top and a Bottom, or a Dress";
        }

        return "Top, Bottom or Shoes";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
