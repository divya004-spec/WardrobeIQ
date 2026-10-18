package com.example.wardrobeiq.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wardrobeiq.R;
import com.example.wardrobeiq.activities.AddClothingActivity;
import com.example.wardrobeiq.adapters.ClothingAdapter;
import com.example.wardrobeiq.database.AppDatabase;
import com.example.wardrobeiq.database.ClothingDao;
import com.example.wardrobeiq.models.Clothing;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WardrobeFragment extends Fragment {

    private RecyclerView recyclerWardrobe;
    private TextView txtEmpty;
    private Button btnAddClothing;

    private ClothingAdapter clothingAdapter;

    private ClothingDao clothingDao;

    private ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    private int userId;

    public WardrobeFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_wardrobe,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        recyclerWardrobe =
                view.findViewById(
                        R.id.recyclerWardrobe
                );

        txtEmpty =
                view.findViewById(
                        R.id.txtEmpty
                );

        btnAddClothing =
                view.findViewById(
                        R.id.btnAddClothing
                );

        SharedPreferences preferences =
                requireContext().getSharedPreferences(
                        "WardrobeIQSession",
                        android.content.Context.MODE_PRIVATE
                );

        userId =
                preferences.getInt(
                        "userId",
                        -1
                );

        recyclerWardrobe.setLayoutManager(
                new GridLayoutManager(
                        requireContext(),
                        2
                )
        );

        clothingAdapter =
                new ClothingAdapter(
                        new ArrayList<>()
                );

        recyclerWardrobe.setAdapter(
                clothingAdapter
        );

        AppDatabase database =
                AppDatabase.getInstance(
                        requireContext()
                );

        clothingDao =
                database.clothingDao();

        btnAddClothing.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            requireContext(),
                            AddClothingActivity.class
                    );

            startActivity(intent);
        });

        loadClothing();
    }

    private void loadClothing() {

        if (userId == -1) {
            return;
        }

        executorService.execute(() -> {

            List<Clothing> clothingList =
                    clothingDao.getClothingByUser(
                            userId
                    );

            requireActivity().runOnUiThread(() -> {

                clothingAdapter.updateList(
                        clothingList
                );

                if (clothingList.isEmpty()) {

                    txtEmpty.setVisibility(
                            View.VISIBLE
                    );

                } else {

                    txtEmpty.setVisibility(
                            View.GONE
                    );
                }
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (clothingDao != null) {
            loadClothing();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Keep executor alive while fragment exists.
    }
}