package com.example.wardrobeiq;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.wardrobeiq.fragments.AnalyticsFragment;
import com.example.wardrobeiq.fragments.HistoryFragment;
import com.example.wardrobeiq.fragments.HomeFragment;
import com.example.wardrobeiq.fragments.OutfitFragment;
import com.example.wardrobeiq.fragments.WardrobeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        bottomNavigation =
                findViewById(R.id.bottomNavigation);

        // Open Home by default
        if (savedInstanceState == null) {

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.fragmentContainer,
                            new HomeFragment()
                    )
                    .commit();
        }

        bottomNavigation.setOnItemSelectedListener(item -> {

            Fragment selectedFragment = null;

            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {

                selectedFragment = new HomeFragment();

            } else if (itemId == R.id.nav_wardrobe) {

                selectedFragment = new WardrobeFragment();

            } else if (itemId == R.id.nav_outfit) {

                selectedFragment = new OutfitFragment();

            } else if (itemId == R.id.nav_history) {

                selectedFragment = new HistoryFragment();

            } else if (itemId == R.id.nav_analytics) {

                selectedFragment = new AnalyticsFragment();
            }

            if (selectedFragment != null) {

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragmentContainer,
                                selectedFragment
                        )
                        .commit();

                return true;
            }

            return false;
        });
    }
}