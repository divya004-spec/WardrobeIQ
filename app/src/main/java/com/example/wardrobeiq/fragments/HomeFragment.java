package com.example.wardrobeiq.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.wardrobeiq.R;
import com.example.wardrobeiq.activities.AddClothingActivity;
import com.example.wardrobeiq.activities.MatchActivity;
import com.example.wardrobeiq.database.AppDatabase;
import com.example.wardrobeiq.database.ClothingDao;
import com.example.wardrobeiq.models.Clothing;
import com.example.wardrobeiq.utils.OutfitRecommender;
import com.example.wardrobeiq.utils.OutfitViews;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private TextView txtGreeting;
    private TextView txtSeasonInfo;
    private TextView txtTotalItems;
    private TextView txtTotalWears;
    private TextView txtNeverWorn;
    private TextView txtTodayStatus;
    private LinearLayout layoutTodayOutfit;
    private Button btnRefreshOutfit;

    private ClothingDao clothingDao;
    private int userId;
    private String userName;

    // Which of the ranked ideas is shown now (the "Another idea" button cycles).
    private int ideaIndex = 0;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public HomeFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        txtGreeting = view.findViewById(R.id.txtGreeting);
        txtSeasonInfo = view.findViewById(R.id.txtSeasonInfo);
        txtTotalItems = view.findViewById(R.id.txtStatItems);
        txtTotalWears = view.findViewById(R.id.txtStatWears);
        txtNeverWorn = view.findViewById(R.id.txtStatNever);
        txtTodayStatus = view.findViewById(R.id.txtTodayStatus);
        layoutTodayOutfit = view.findViewById(R.id.layoutTodayOutfit);
        btnRefreshOutfit = view.findViewById(R.id.btnRefreshOutfit);

        Button btnScan = view.findViewById(R.id.btnScan);
        Button btnImport = view.findViewById(R.id.btnImport);
        Button btnMatch = view.findViewById(R.id.btnMatch);

        SharedPreferences prefs = requireContext().getSharedPreferences(
                "WardrobeIQSession", Context.MODE_PRIVATE);

        userId = prefs.getInt("userId", -1);
        userName = prefs.getString("userName", "");

        clothingDao = AppDatabase.getInstance(requireContext()).clothingDao();

        txtGreeting.setText(buildGreeting());
        txtSeasonInfo.setText("Season now: " + currentSeason()
                + "  \u2022  Outfits are picked for this season");

        // SCAN: open Add Clothing and start the camera straight away
        btnScan.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddClothingActivity.class);
            intent.putExtra("autoLaunch", "camera");
            startActivity(intent);
        });

        // IMPORT: open Add Clothing and start the gallery straight away
        btnImport.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddClothingActivity.class);
            intent.putExtra("autoLaunch", "gallery");
            startActivity(intent);
        });

        // MATCH: outfit ideas from the whole wardrobe
        btnMatch.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MatchActivity.class)));

        btnRefreshOutfit.setOnClickListener(v -> {
            ideaIndex++;
            loadHome();
        });
    }

    // Refresh numbers and today's outfit whenever the tab is shown again.
    @Override
    public void onResume() {
        super.onResume();
        ideaIndex = 0;
        loadHome();
    }

    private void loadHome() {

        if (userId == -1 || clothingDao == null) {
            txtTodayStatus.setText("Please log in again.");
            return;
        }

        final String season = currentSeason();
        final int wantedIndex = ideaIndex;

        executor.execute(() -> {

            List<Clothing> wardrobe = clothingDao.getClothingByUser(userId);

            int totalWears = 0;
            int never = 0;

            for (Clothing c : wardrobe) {
                totalWears += Math.max(0, c.getWearCount());
                if (c.getWearCount() <= 0) {
                    never++;
                }
            }

            final int finalWears = totalWears;
            final int finalNever = never;
            final int total = wardrobe.size();

            List<OutfitRecommender.Outfit> ideas = OutfitRecommender.recommend(
                    wardrobe, OutfitRecommender.ANY, season, 5);

            final String missing = ideas.isEmpty()
                    ? OutfitRecommender.explainMissing(wardrobe) : "";

            final OutfitRecommender.Outfit pick = ideas.isEmpty()
                    ? null : ideas.get(wantedIndex % ideas.size());

            final int ideaCount = ideas.size();

            if (getActivity() == null) {
                return;
            }

            getActivity().runOnUiThread(() -> {

                if (!isAdded()) {
                    return;
                }

                txtTotalItems.setText(String.valueOf(total));
                txtTotalWears.setText(String.valueOf(finalWears));
                txtNeverWorn.setText(String.valueOf(finalNever));

                layoutTodayOutfit.removeAllViews();

                if (pick == null) {
                    txtTodayStatus.setText(total == 0
                            ? "Your wardrobe is empty. Tap SCAN NEW CLOTHING to add your first item."
                            : missing);
                    btnRefreshOutfit.setVisibility(View.GONE);
                    return;
                }

                txtTodayStatus.setText("Picked from your wardrobe for today");
                btnRefreshOutfit.setVisibility(ideaCount > 1 ? View.VISIBLE : View.GONE);

                layoutTodayOutfit.addView(OutfitViews.buildOutfitCard(
                        requireContext(),
                        "Today's outfit",
                        pick,
                        "WEAR THIS",
                        () -> loadStatsOnly()));
            });
        });
    }

    // After WEAR THIS, update only the numbers (keep the card so the button stays "marked").
    private void loadStatsOnly() {

        if (clothingDao == null || userId == -1) {
            return;
        }

        executor.execute(() -> {

            List<Clothing> wardrobe = clothingDao.getClothingByUser(userId);

            int wears = 0;
            int never = 0;

            for (Clothing c : wardrobe) {
                wears += Math.max(0, c.getWearCount());
                if (c.getWearCount() <= 0) {
                    never++;
                }
            }

            final int w = wears;
            final int n = never;

            if (getActivity() == null) {
                return;
            }

            getActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }
                txtTotalWears.setText(String.valueOf(w));
                txtNeverWorn.setText(String.valueOf(n));
            });
        });
    }

    private String buildGreeting() {

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        String part;
        if (hour < 12) {
            part = "Good Morning";
        } else if (hour < 17) {
            part = "Good Afternoon";
        } else {
            part = "Good Evening";
        }

        if (userName != null && !userName.trim().isEmpty()) {
            String first = userName.trim().split("\\s+")[0];
            return part + ", " + first + " \uD83D\uDC4B";
        }

        return part + " \uD83D\uDC4B";
    }

    // India-style seasons, matching the Season spinner used in the app.
    private String currentSeason() {

        int month = Calendar.getInstance().get(Calendar.MONTH); // 0 = Jan

        if (month >= 2 && month <= 5) {
            return "Summer";   // Mar - Jun
        }

        if (month >= 6 && month <= 8) {
            return "Monsoon";  // Jul - Sep
        }

        if (month == 10 || month == 11 || month == 0 || month == 1) {
            return "Winter";   // Nov - Feb
        }

        return OutfitRecommender.ANY; // October: transition month
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
