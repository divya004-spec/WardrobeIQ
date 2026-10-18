package com.example.wardrobeiq.utils;

import com.example.wardrobeiq.models.Clothing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple statistics about a user's wardrobe. Plain Java (no Android classes),
 * so it is easy to test.
 */
public class WardrobeStats {

    private static final String[] MAIN_CATEGORIES = {
            "Top", "Bottom", "Dress", "Shoes", "Layer", "Accessory"
    };

    public final int totalItems;
    public final int totalWears;
    public final int neverWornCount;

    // Sorted by count (highest first)
    public final LinkedHashMap<String, Integer> categoryCounts;
    public final LinkedHashMap<String, Integer> colorCounts;

    public final List<Clothing> mostWorn = new ArrayList<>();   // up to 5, wearCount > 0
    public final List<Clothing> neverWorn = new ArrayList<>();  // all items never worn
    public final List<String> insights = new ArrayList<>();

    private WardrobeStats(List<Clothing> items) {

        totalItems = items.size();

        int wears = 0;
        Map<String, Integer> categories = new HashMap<>();
        Map<String, Integer> colors = new HashMap<>();

        for (String category : MAIN_CATEGORIES) {
            categories.put(category, 0);
        }

        for (Clothing c : items) {

            wears += Math.max(0, c.getWearCount());

            increment(categories, normalise(c.getCategory()));
            increment(colors, normalise(c.getColor()));

            if (c.getWearCount() <= 0) {
                neverWorn.add(c);
            }
        }

        totalWears = wears;
        neverWornCount = neverWorn.size();
        categoryCounts = sortByCount(categories);
        colorCounts = sortByCount(colors);

        List<Clothing> worn = new ArrayList<>();
        for (Clothing c : items) {
            if (c.getWearCount() > 0) {
                worn.add(c);
            }
        }

        Collections.sort(worn, new Comparator<Clothing>() {
            @Override
            public int compare(Clothing a, Clothing b) {
                return b.getWearCount() - a.getWearCount();
            }
        });

        mostWorn.addAll(worn.size() > 5 ? worn.subList(0, 5) : worn);

        buildInsights(items);
    }

    public static WardrobeStats compute(List<Clothing> items) {
        return new WardrobeStats(items == null ? new ArrayList<Clothing>() : items);
    }

    /**
     * Groups items by the date they were last worn (yyyy-MM-dd), newest first.
     * Items that were never worn are left out.
     */
    public static LinkedHashMap<String, List<Clothing>> groupByLastWorn(List<Clothing> items) {

        List<Clothing> worn = new ArrayList<>();

        if (items != null) {
            for (Clothing c : items) {
                String date = c.getLastWorn();
                if (date != null && !date.trim().isEmpty()) {
                    worn.add(c);
                }
            }
        }

        // yyyy-MM-dd sorts correctly as plain text
        Collections.sort(worn, new Comparator<Clothing>() {
            @Override
            public int compare(Clothing a, Clothing b) {
                return b.getLastWorn().compareTo(a.getLastWorn());
            }
        });

        LinkedHashMap<String, List<Clothing>> groups = new LinkedHashMap<>();

        for (Clothing c : worn) {

            List<Clothing> list = groups.get(c.getLastWorn());

            if (list == null) {
                list = new ArrayList<>();
                groups.put(c.getLastWorn(), list);
            }

            list.add(c);
        }

        return groups;
    }

    // -------------------------------------------------------------

    private void buildInsights(List<Clothing> items) {

        if (totalItems == 0) {
            return;
        }

        if (neverWornCount > 0) {
            int percent = Math.round(100f * neverWornCount / totalItems);
            insights.add("You have not worn " + neverWornCount + " of your "
                    + totalItems + " items yet (" + percent + "%).");
        } else {
            insights.add("You have worn every item in your wardrobe at least once.");
        }

        if (!mostWorn.isEmpty()) {
            Clothing top = mostWorn.get(0);
            insights.add("Your most worn item is the " + normalise(top.getColor())
                    + " " + normalise(top.getCategory()) + " (" + top.getWearCount()
                    + (top.getWearCount() == 1 ? " time)." : " times)."));
        }

        int tops = categoryCounts.containsKey("Top") ? categoryCounts.get("Top") : 0;
        int bottoms = categoryCounts.containsKey("Bottom") ? categoryCounts.get("Bottom") : 0;
        int dresses = categoryCounts.containsKey("Dress") ? categoryCounts.get("Dress") : 0;
        int shoes = categoryCounts.containsKey("Shoes") ? categoryCounts.get("Shoes") : 0;

        if (shoes == 0) {
            insights.add("You have no shoes saved. Add some so outfits can be completed.");
        }

        if (tops > 0 && bottoms == 0 && dresses == 0) {
            insights.add("You have tops but no bottoms, so full outfits cannot be built yet.");
        }

        if (bottoms > 0 && tops == 0 && dresses == 0) {
            insights.add("You have bottoms but no tops, so full outfits cannot be built yet.");
        }

        if (totalItems >= 4 && !colorCounts.isEmpty()) {

            Map.Entry<String, Integer> first = colorCounts.entrySet().iterator().next();

            if (first.getValue() * 2 > totalItems) {
                insights.add("More than half of your wardrobe is " + first.getKey()
                        + ". Adding another colour gives you more outfit combinations.");
            }
        }
    }

    private static void increment(Map<String, Integer> map, String key) {
        Integer old = map.get(key);
        map.put(key, old == null ? 1 : old + 1);
    }

    private static String normalise(String value) {

        if (value == null || value.trim().isEmpty()) {
            return "Unknown";
        }

        String v = value.trim();

        return Character.toUpperCase(v.charAt(0)) + v.substring(1);
    }

    private static LinkedHashMap<String, Integer> sortByCount(Map<String, Integer> map) {

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(map.entrySet());

        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                int byCount = b.getValue() - a.getValue();
                return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
            }
        });

        LinkedHashMap<String, Integer> sorted = new LinkedHashMap<>();

        for (Map.Entry<String, Integer> e : entries) {
            sorted.put(e.getKey(), e.getValue());
        }

        return sorted;
    }
}