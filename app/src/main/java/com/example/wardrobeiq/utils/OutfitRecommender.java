package com.example.wardrobeiq.utils;

import com.example.wardrobeiq.models.Clothing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Rule-based outfit recommender. No AI and no Android classes are used,
 * so it works offline and is easy to test.
 *
 * Outfit shapes (shoes are optional: they are added whenever the user has some):
 *   Top + Bottom  (+ Shoes) (+ Layer in cool/wet weather) (+ Accessory)
 *   Dress         (+ Shoes) (+ Layer in cool/wet weather) (+ Accessory)
 */
public class OutfitRecommender {

    public static final String ANY = "Any";

    // -------------------------------------------------------------
    // Result class
    // -------------------------------------------------------------

    public static class Outfit {

        public final List<Clothing> items = new ArrayList<>();
        public final List<String> reasons = new ArrayList<>();
        public double score;
        public boolean relaxed;
    }

    // -------------------------------------------------------------
    // Rules (edit these to tune the recommendations)
    // -------------------------------------------------------------

    private static final Set<String> NEUTRALS = new HashSet<>(
            Arrays.asList("black", "white", "grey", "beige", "brown"));

    // Colour pairs that usually clash when both are non-neutral.
    private static final String[][] CLASHES = {
            {"red", "green"},
            {"red", "pink"},
            {"red", "orange"},
            {"pink", "orange"},
            {"purple", "green"},
            {"yellow", "pink"},
            {"orange", "purple"}
    };

    private static final Map<String, Integer> TARGET_FORMALITY = new HashMap<>();
    private static final Map<String, Set<String>> COMPATIBLE_OCCASIONS = new HashMap<>();
    private static final Map<String, Integer> TARGET_WARMTH = new HashMap<>();

    static {
        TARGET_FORMALITY.put("casual", 2);
        TARGET_FORMALITY.put("college", 2);
        TARGET_FORMALITY.put("work", 4);
        TARGET_FORMALITY.put("party", 3);
        TARGET_FORMALITY.put("formal", 5);
        TARGET_FORMALITY.put("sports", 1);

        COMPATIBLE_OCCASIONS.put("casual", setOf("college", "party"));
        COMPATIBLE_OCCASIONS.put("college", setOf("casual"));
        COMPATIBLE_OCCASIONS.put("work", setOf("formal"));
        COMPATIBLE_OCCASIONS.put("party", setOf("formal", "casual"));
        COMPATIBLE_OCCASIONS.put("formal", setOf("work", "party"));
        COMPATIBLE_OCCASIONS.put("sports", new HashSet<String>());

        TARGET_WARMTH.put("summer", 1);
        TARGET_WARMTH.put("monsoon", 2);
        TARGET_WARMTH.put("winter", 5);
    }

    private static Set<String> setOf(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    // -------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------

    public static List<Outfit> recommend(
            List<Clothing> wardrobe,
            String occasion,
            String season,
            int maxResults) {

        if (wardrobe == null || wardrobe.isEmpty()) {
            return new ArrayList<>();
        }

        // First try strict matching. If nothing can be built, relax the filters.
        List<Outfit> result = build(wardrobe, occasion, season, maxResults, false);

        if (result.isEmpty()) {
            result = build(wardrobe, occasion, season, maxResults, true);
        }

        return result;
    }

    // -------------------------------------------------------------
    // "I already have an outfit" -> suggest shoes
    // -------------------------------------------------------------

    /**
     * Ranks the user's shoes against pieces they already picked.
     * Returns an empty list if the user owns no shoes.
     */
    public static List<Outfit> suggestShoes(
            List<Clothing> picked,
            List<Clothing> wardrobe,
            String occasion,
            String season,
            int maxResults) {

        List<Outfit> results = new ArrayList<>();

        if (picked == null || picked.isEmpty() || wardrobe == null) {
            return results;
        }

        List<Clothing> shoes = new ArrayList<>();
        for (Clothing c : wardrobe) {
            if (sameText(c.getCategory(), "Shoes")) {
                shoes.add(c);
            }
        }

        if (shoes.isEmpty()) {
            return results;
        }

        // If the user did not choose an occasion/season, learn it from the picked pieces.
        String occ = isAny(occasion) ? mostCommonOccasion(picked) : occasion;
        String sea = isAny(season) ? mostCommonSeason(picked) : season;

        List<Clothing> candidates = new ArrayList<>();
        for (Clothing shoe : shoes) {
            if (occasionOk(shoe, occ) && seasonOk(shoe, sea)) {
                candidates.add(shoe);
            }
        }

        boolean relaxed = false;
        if (candidates.isEmpty()) {
            candidates = shoes;
            relaxed = true;
        }

        for (Clothing shoe : candidates) {

            List<Clothing> items = new ArrayList<>(picked);
            items.add(shoe);

            Outfit outfit = evaluate(items, occ, sea, relaxed);
            outfit.reasons.add(0, "Goes with the outfit you picked");
            results.add(outfit);
        }

        Collections.sort(results, new Comparator<Outfit>() {
            @Override
            public int compare(Outfit a, Outfit b) {
                return Double.compare(b.score, a.score);
            }
        });

        if (results.size() > maxResults) {
            return new ArrayList<>(results.subList(0, maxResults));
        }

        return results;
    }

    /**
     * Plain-language shoe advice for the picked pieces. Useful when the user has
     * no shoes saved, and as a tip alongside the ranked shoes.
     */
    public static String shoeAdvice(
            List<Clothing> picked,
            String occasion,
            String season) {

        if (picked == null || picked.isEmpty()) {
            return "";
        }

        String occ = isAny(occasion) ? mostCommonOccasion(picked) : occasion;
        String sea = isAny(season) ? mostCommonSeason(picked) : season;

        double formalitySum = 0;
        int formalityCount = 0;
        boolean loud = false;

        for (Clothing c : picked) {

            if (!sameText(c.getCategory(), "Accessory")) {
                formalitySum += c.getFormality();
                formalityCount++;
            }

            String color = lower(c.getColor());
            if (!color.isEmpty() && !NEUTRALS.contains(color)) {
                loud = true;
            }
        }

        double formality = formalityCount == 0 ? 2 : formalitySum / formalityCount;

        StringBuilder advice = new StringBuilder();

        if (sameText(occ, "Sports")) {
            advice.append("Sports shoes or running sneakers.");
        } else if (formality >= 3.5) {
            advice.append("Formal shoes such as oxfords, loafers or heels in Black or Brown.");
        } else if (formality >= 2.5) {
            advice.append("Smart-casual shoes such as loafers, flats or clean sneakers in Black, Brown or White.");
        } else {
            advice.append("Casual sneakers or sandals in White or Black.");
        }

        if (loud) {
            advice.append(" Keep the shoes neutral (White, Black or Brown) so the outfit colours stand out.");
        } else {
            advice.append(" Your outfit is neutral, so you can add a pop of colour with the shoes.");
        }

        if (sameText(sea, "Monsoon")) {
            advice.append(" For rain, choose water-resistant shoes or sandals.");
        } else if (sameText(sea, "Winter")) {
            advice.append(" For cold weather, choose closed shoes or boots.");
        } else if (sameText(sea, "Summer")) {
            advice.append(" For hot weather, choose light, breathable shoes.");
        }

        return advice.toString();
    }

    private static String mostCommonOccasion(List<Clothing> items) {

        Map<String, Integer> counts = new HashMap<>();
        String best = ANY;
        int bestCount = 0;

        for (Clothing c : items) {

            String value = c.getOccasion();

            if (value == null || value.trim().isEmpty()) {
                continue;
            }

            Integer old = counts.get(value);
            int now = old == null ? 1 : old + 1;
            counts.put(value, now);

            if (now > bestCount) {
                bestCount = now;
                best = value;
            }
        }

        return best;
    }

    // "All Season" items do not say anything about the weather, so they are ignored.
    private static String mostCommonSeason(List<Clothing> items) {

        Map<String, Integer> counts = new HashMap<>();
        String best = ANY;
        int bestCount = 0;

        for (Clothing c : items) {

            String value = c.getSeason();

            if (value == null || value.trim().isEmpty()
                    || value.equalsIgnoreCase("All Season")) {
                continue;
            }

            Integer old = counts.get(value);
            int now = old == null ? 1 : old + 1;
            counts.put(value, now);

            if (now > bestCount) {
                bestCount = now;
                best = value;
            }
        }

        return best;
    }

    /** Explains what is missing when no outfit can be built. */
    public static String explainMissing(List<Clothing> wardrobe) {

        if (wardrobe == null || wardrobe.isEmpty()) {
            return "Your wardrobe is empty. Add some clothes first.";
        }

        int tops = count(wardrobe, "Top");
        int bottoms = count(wardrobe, "Bottom");
        int dresses = count(wardrobe, "Dress");

        if (dresses == 0) {
            if (tops == 0 && bottoms == 0) {
                return "To build outfits, please add a Top and a Bottom, or a Dress.";
            }
            if (tops == 0) {
                return "You have a Bottom but no Top. Please add a Top, or a Dress.";
            }
            if (bottoms == 0) {
                return "You have a Top but no Bottom. Please add a Bottom, or a Dress.";
            }
        }

        return "No outfit could be built from your current items.";
    }

    /** Looks for an occasion word in free text. Returns ANY if none found. */
    public static String detectOccasion(String text) {

        String t = lower(text);

        if (containsAny(t, "gym", "sport", "workout", "running", "jog", "exercise")) {
            return "Sports";
        }
        if (containsAny(t, "wedding", "formal", "interview", "ceremony", "function")) {
            return "Formal";
        }
        if (containsAny(t, "party", "date", "dinner", "club", "night out", "birthday")) {
            return "Party";
        }
        if (containsAny(t, "office", "work", "meeting", "presentation")) {
            return "Work";
        }
        if (containsAny(t, "college", "class", "campus", "university", "school")) {
            return "College";
        }
        if (containsAny(t, "casual", "hangout", "shopping", "movie", "friends", "walk")) {
            return "Casual";
        }

        return ANY;
    }

    /** Looks for a season/weather word in free text. Returns ANY if none found. */
    public static String detectSeason(String text) {

        String t = lower(text);

        if (containsAny(t, "winter", "cold", "chilly", "freezing")) {
            return "Winter";
        }
        if (containsAny(t, "rain", "monsoon", "wet", "drizzle")) {
            return "Monsoon";
        }
        if (containsAny(t, "summer", "hot", "warm", "sunny", "humid")) {
            return "Summer";
        }

        return ANY;
    }

    // -------------------------------------------------------------
    // Building outfits
    // -------------------------------------------------------------

    private static List<Outfit> build(
            List<Clothing> wardrobe,
            String occasion,
            String season,
            int maxResults,
            boolean relaxed) {

        List<Clothing> tops = pool(wardrobe, "Top", occasion, season, relaxed);
        List<Clothing> bottoms = pool(wardrobe, "Bottom", occasion, season, relaxed);
        List<Clothing> shoes = pool(wardrobe, "Shoes", occasion, season, relaxed);

        // Shoes are optional, but if the user owns some, always use them:
        // if none pass the strict filters, fall back to the best shoes available.
        boolean ownsShoes = count(wardrobe, "Shoes") > 0;

        if (shoes.isEmpty() && ownsShoes) {
            shoes = pool(wardrobe, "Shoes", occasion, season, true);
        }
        List<Clothing> dresses = pool(wardrobe, "Dress", occasion, season, relaxed);
        List<Clothing> layers = pool(wardrobe, "Layer", occasion, season, relaxed);
        List<Clothing> accessories = pool(wardrobe, "Accessory", occasion, season, relaxed);

        boolean wantsLayer = isCool(season);
        boolean skipAccessory = "sports".equals(lower(occasion));

        List<Outfit> all = new ArrayList<>();

        // Top + Bottom (+ Shoes)
        for (Clothing top : tops) {
            for (Clothing bottom : bottoms) {

                if (shoes.isEmpty()) {
                    all.add(finish(
                            Arrays.asList(top, bottom),
                            layers, accessories,
                            wantsLayer, skipAccessory,
                            occasion, season, relaxed));
                    continue;
                }

                for (Clothing shoe : shoes) {
                    all.add(finish(
                            Arrays.asList(top, bottom, shoe),
                            layers, accessories,
                            wantsLayer, skipAccessory,
                            occasion, season, relaxed));
                }
            }
        }

        // Dress (+ Shoes)
        for (Clothing dress : dresses) {

            if (shoes.isEmpty()) {
                all.add(finish(
                        Arrays.asList(dress),
                        layers, accessories,
                        wantsLayer, skipAccessory,
                        occasion, season, relaxed));
                continue;
            }

            for (Clothing shoe : shoes) {
                all.add(finish(
                        Arrays.asList(dress, shoe),
                        layers, accessories,
                        wantsLayer, skipAccessory,
                        occasion, season, relaxed));
            }
        }

        Collections.sort(all, new Comparator<Outfit>() {
            @Override
            public int compare(Outfit a, Outfit b) {
                return Double.compare(b.score, a.score);
            }
        });

        List<Outfit> picked = pickDiverse(all, maxResults);

        if (!ownsShoes) {
            for (Outfit outfit : picked) {
                outfit.reasons.add(
                        "No shoes in your wardrobe yet. Add some to complete the look");
            }
        }

        return picked;
    }

    /** Adds the best layer / accessory to a core outfit and scores it. */
    private static Outfit finish(
            List<Clothing> core,
            List<Clothing> layers,
            List<Clothing> accessories,
            boolean wantsLayer,
            boolean skipAccessory,
            String occasion,
            String season,
            boolean relaxed) {

        List<Clothing> items = new ArrayList<>(core);
        Outfit best = evaluate(items, occasion, season, relaxed);

        if (wantsLayer && !layers.isEmpty()) {
            best = bestWith(items, layers, occasion, season, relaxed, best, true);
            items = best.items;
        }

        if (!skipAccessory && !accessories.isEmpty()) {
            best = bestWith(new ArrayList<>(best.items), accessories,
                    occasion, season, relaxed, best, false);
        }

        return best;
    }

    private static Outfit bestWith(
            List<Clothing> baseItems,
            List<Clothing> candidates,
            String occasion,
            String season,
            boolean relaxed,
            Outfit current,
            boolean mustAdd) {

        Outfit best = mustAdd ? null : current;

        for (Clothing candidate : candidates) {

            List<Clothing> items = new ArrayList<>(baseItems);
            items.add(candidate);

            Outfit trial = evaluate(items, occasion, season, relaxed);

            if (best == null || trial.score > best.score) {
                best = trial;
            }
        }

        return best == null ? current : best;
    }

    // Keep the best few items per category so the number of combinations stays small.
    private static List<Clothing> pool(
            List<Clothing> wardrobe,
            String category,
            final String occasion,
            final String season,
            boolean relaxed) {

        List<Clothing> pool = new ArrayList<>();

        for (Clothing c : wardrobe) {

            if (!sameText(c.getCategory(), category)) {
                continue;
            }

            if (!relaxed) {
                if (!occasionOk(c, occasion) || !seasonOk(c, season)) {
                    continue;
                }
            }

            pool.add(c);
        }

        Collections.sort(pool, new Comparator<Clothing>() {
            @Override
            public int compare(Clothing a, Clothing b) {
                return Double.compare(
                        itemScore(b, occasion, season),
                        itemScore(a, occasion, season));
            }
        });

        if (pool.size() > 6) {
            return new ArrayList<>(pool.subList(0, 6));
        }

        return pool;
    }

    // Avoid showing 5 almost identical outfits.
    private static List<Outfit> pickDiverse(List<Outfit> sorted, int maxResults) {

        List<Outfit> picked = new ArrayList<>();
        Map<Integer, Integer> usage = new HashMap<>();

        for (Outfit outfit : sorted) {

            if (picked.size() >= maxResults) {
                break;
            }

            boolean tooSimilar = false;

            for (Clothing item : outfit.items) {
                Integer used = usage.get(item.getId());
                if (used != null && used >= 2 && isMainPiece(item)) {
                    tooSimilar = true;
                    break;
                }
            }

            for (Outfit other : picked) {
                if (sameMainPieces(outfit, other)) {
                    tooSimilar = true;
                    break;
                }
            }

            if (tooSimilar) {
                continue;
            }

            picked.add(outfit);

            for (Clothing item : outfit.items) {
                Integer used = usage.get(item.getId());
                usage.put(item.getId(), used == null ? 1 : used + 1);
            }
        }

        // If diversity rules removed too many, top up with the best remaining.
        for (Outfit outfit : sorted) {
            if (picked.size() >= Math.min(maxResults, 3)) {
                break;
            }
            if (!picked.contains(outfit)) {
                picked.add(outfit);
            }
        }

        Collections.sort(picked, new Comparator<Outfit>() {
            @Override
            public int compare(Outfit a, Outfit b) {
                return Double.compare(b.score, a.score);
            }
        });

        return picked;
    }

    // Two outfits are "the same idea" if they use the same Top/Bottom/Dress.
    private static boolean sameMainPieces(Outfit a, Outfit b) {

        Set<Integer> first = mainIds(a);
        Set<Integer> second = mainIds(b);

        return first.equals(second);
    }

    private static Set<Integer> mainIds(Outfit outfit) {

        Set<Integer> ids = new HashSet<>();

        for (Clothing c : outfit.items) {
            if (sameText(c.getCategory(), "Top")
                    || sameText(c.getCategory(), "Bottom")
                    || sameText(c.getCategory(), "Dress")) {
                ids.add(c.getId());
            }
        }

        return ids;
    }

    private static boolean isMainPiece(Clothing c) {
        return sameText(c.getCategory(), "Top")
                || sameText(c.getCategory(), "Bottom")
                || sameText(c.getCategory(), "Dress");
    }

    // -------------------------------------------------------------
    // Scoring
    // -------------------------------------------------------------

    private static boolean occasionOk(Clothing c, String occasion) {

        if (isAny(occasion)) {
            return true;
        }

        return occasionScore(c, occasion) > 0;
    }

    private static boolean seasonOk(Clothing c, String season) {

        if (isAny(season)) {
            return true;
        }

        String itemSeason = lower(c.getSeason());

        return itemSeason.equals(lower(season)) || itemSeason.equals("all season");
    }

    private static int occasionScore(Clothing c, String occasion) {

        if (isAny(occasion)) {
            return 15;
        }

        String wanted = lower(occasion);
        String have = lower(c.getOccasion());

        if (have.equals(wanted)) {
            return 30;
        }

        Set<String> compatible = COMPATIBLE_OCCASIONS.get(wanted);

        if (compatible != null && compatible.contains(have)) {
            return 18;
        }

        return 0;
    }

    /** Score of one item on its own (about 0 to 80). */
    private static double itemScore(Clothing c, String occasion, String season) {

        double score = occasionScore(c, occasion);

        // Season
        if (isAny(season)) {
            score += 8;
        } else {
            String itemSeason = lower(c.getSeason());
            if (itemSeason.equals(lower(season))) {
                score += 15;
            } else if (itemSeason.equals("all season")) {
                score += 12;
            }
        }

        // Formality closeness to the occasion
        Integer target = TARGET_FORMALITY.get(lower(occasion));
        if (target != null) {
            score += Math.max(0, 15 - 5 * Math.abs(c.getFormality() - target));
        } else {
            score += 8;
        }

        // Warmth closeness to the season
        Integer warmthTarget = TARGET_WARMTH.get(lower(season));
        if (warmthTarget != null) {
            score += Math.max(0, 10 - 3 * Math.abs(c.getWarmth() - warmthTarget));
        } else {
            score += 5;
        }

        // Freshness: items worn less often score higher
        score += 10 - Math.min(c.getWearCount(), 10);

        return score;
    }

    /** Scores a full outfit and records why it was chosen. */
    private static Outfit evaluate(
            List<Clothing> items,
            String occasion,
            String season,
            boolean relaxed) {

        Outfit outfit = new Outfit();
        outfit.items.addAll(items);
        outfit.relaxed = relaxed;

        // 1. Average item score
        double sum = 0;
        for (Clothing c : items) {
            sum += itemScore(c, occasion, season);
        }
        double score = sum / items.size();

        // 2. Occasion and season reasons
        if (!isAny(occasion)) {
            boolean allExact = true;
            boolean allSuit = true;
            for (Clothing c : items) {
                int os = occasionScore(c, occasion);
                if (os < 30) {
                    allExact = false;
                }
                if (os <= 0) {
                    allSuit = false;
                }
            }
            if (allExact) {
                outfit.reasons.add("Every piece is marked for " + occasion);
            } else if (allSuit) {
                outfit.reasons.add("Pieces suit a " + occasion + " look");
            } else {
                outfit.reasons.add("Closest available pieces for " + occasion);
            }
        }

        if (!isAny(season)) {
            outfit.reasons.add("Chosen for " + season + " weather");
        }

        // 3. Colour harmony (core pieces only: ignore shoes/accessory browns etc.)
        List<String> loudColors = new ArrayList<>();
        for (Clothing c : items) {
            String color = lower(c.getColor());
            if (!color.isEmpty() && !NEUTRALS.contains(color)
                    && !loudColors.contains(color)) {
                loudColors.add(color);
            }
        }

        if (loudColors.isEmpty()) {
            score += 15;
            outfit.reasons.add("Clean neutral colours that go together");
        } else if (loudColors.size() == 1) {
            score += 15;
            outfit.reasons.add("One colour accent (" + cap(loudColors.get(0))
                    + ") balanced with neutrals");
        } else if (loudColors.size() == 2) {
            if (clashes(loudColors.get(0), loudColors.get(1))) {
                score -= 8;
                outfit.reasons.add("Colours may clash ("
                        + cap(loudColors.get(0)) + " + " + cap(loudColors.get(1)) + ")");
            } else {
                score += 5;
                outfit.reasons.add("Two colours that work together ("
                        + cap(loudColors.get(0)) + " + " + cap(loudColors.get(1)) + ")");
            }
        } else {
            score -= 12;
            outfit.reasons.add("Many bright colours at once");
        }

        // 4. Patterns: avoid two busy pieces
        int patterned = 0;
        for (Clothing c : items) {
            String pattern = lower(c.getPattern());
            if (!pattern.isEmpty() && !pattern.equals("solid")) {
                patterned++;
            }
        }

        if (patterned == 0) {
            score += 10;
        } else if (patterned == 1) {
            score += 8;
            outfit.reasons.add("One patterned piece keeps it balanced");
        } else {
            score -= 10;
            outfit.reasons.add("Two or more patterns may look busy");
        }

        // 5. Formality should be consistent across the outfit
        int min = 99;
        int max = -99;
        for (Clothing c : items) {
            if (sameText(c.getCategory(), "Accessory")) {
                continue;
            }
            min = Math.min(min, c.getFormality());
            max = Math.max(max, c.getFormality());
        }

        int spread = max - min;

        if (spread <= 1) {
            score += 10;
            outfit.reasons.add("Consistent formality level");
        } else if (spread >= 3) {
            score -= 10;
            outfit.reasons.add("Mix of very casual and very formal pieces");
        }

        // 6. Completeness bonus for layers and accessories
        for (Clothing c : items) {
            if (sameText(c.getCategory(), "Layer")) {
                score += 4;
                outfit.reasons.add("Layer added for comfort");
            }
            if (sameText(c.getCategory(), "Accessory")) {
                score += 3;
            }
        }

        // 7. Freshness reason
        int totalWears = 0;
        for (Clothing c : items) {
            totalWears += c.getWearCount();
        }
        if (totalWears <= items.size()) {
            outfit.reasons.add("Rarely worn pieces");
        }

        if (relaxed) {
            outfit.reasons.add("Limited matching items, so filters were relaxed");
        }

        outfit.score = score;
        return outfit;
    }

    // -------------------------------------------------------------
    // Small helpers
    // -------------------------------------------------------------

    private static boolean clashes(String a, String b) {
        for (String[] pair : CLASHES) {
            if ((pair[0].equals(a) && pair[1].equals(b))
                    || (pair[0].equals(b) && pair[1].equals(a))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCool(String season) {
        String s = lower(season);
        return s.equals("winter") || s.equals("monsoon");
    }

    private static boolean isAny(String value) {
        return value == null || value.trim().isEmpty()
                || value.equalsIgnoreCase(ANY);
    }

    private static boolean sameText(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private static int count(List<Clothing> wardrobe, String category) {
        int n = 0;
        if (wardrobe != null) {
            for (Clothing c : wardrobe) {
                if (sameText(c.getCategory(), category)) {
                    n++;
                }
            }
        }
        return n;
    }

    private static String lower(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    // Matches whole words (or word starts for longer words), so "brunch"
    // does not match "run" and "photo" does not match "hot".
    private static boolean containsAny(String text, String... words) {

        String[] tokens = text.split("[^a-z]+");

        for (String w : words) {

            if (w.contains(" ")) {
                if (text.contains(w)) {
                    return true;
                }
                continue;
            }

            for (String token : tokens) {
                if (token.equals(w)
                        || (w.length() >= 4 && token.startsWith(w))) {
                    return true;
                }
            }
        }

        return false;
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(i == parts.size() - 1 ? " and " : ", ");
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    // -------------------------------------------------------------
    // "Match this item" -> outfits built around ONE chosen item
    // -------------------------------------------------------------

    public static List<Outfit> matchItem(
            Clothing anchor,
            List<Clothing> wardrobe,
            String occasion,
            String season,
            int maxResults) {

        List<Outfit> results = new ArrayList<>();

        if (anchor == null || wardrobe == null) {
            return results;
        }

        // Learn occasion / season from the item when the user did not choose.
        List<Clothing> single = new ArrayList<>();
        single.add(anchor);
        String occ = isAny(occasion) ? mostCommonOccasion(single) : occasion;
        String sea = isAny(season) ? mostCommonSeason(single) : season;

        // Never combine the anchor with itself.
        List<Clothing> others = new ArrayList<>();
        for (Clothing c : wardrobe) {
            if (c.getId() != anchor.getId()) {
                others.add(c);
            }
        }

        for (int pass = 0; pass < 2 && results.isEmpty(); pass++) {

            boolean relaxed = pass == 1;

            List<Clothing> tops = pool(others, "Top", occ, sea, relaxed);
            List<Clothing> bottoms = pool(others, "Bottom", occ, sea, relaxed);
            List<Clothing> shoes = pool(others, "Shoes", occ, sea, true);
            List<Clothing> dresses = pool(others, "Dress", occ, sea, relaxed);
            List<Clothing> layers = pool(others, "Layer", occ, sea, relaxed);
            List<Clothing> accessories = pool(others, "Accessory", occ, sea, relaxed);

            boolean wantsLayer = isCool(sea);
            boolean skipAccessory = "sports".equals(lower(occ));

            String cat = anchor.getCategory();
            List<List<Clothing>> cores = new ArrayList<>();

            if (sameText(cat, "Top")) {
                for (Clothing b : bottoms) {
                    cores.addAll(withShoes(Arrays.asList(anchor, b), shoes));
                }
            } else if (sameText(cat, "Bottom")) {
                for (Clothing t : tops) {
                    cores.addAll(withShoes(Arrays.asList(t, anchor), shoes));
                }
            } else if (sameText(cat, "Dress")) {
                cores.addAll(withShoes(Arrays.asList(anchor), shoes));
            } else if (sameText(cat, "Shoes")) {
                for (Clothing t : tops) {
                    for (Clothing b : bottoms) {
                        cores.add(Arrays.asList(t, b, anchor));
                    }
                }
                for (Clothing d : dresses) {
                    cores.add(Arrays.asList(d, anchor));
                }
            } else {
                // Layer or Accessory: build a base outfit and add the anchor
                for (Clothing t : tops) {
                    for (Clothing b : bottoms) {
                        cores.addAll(withShoes(Arrays.asList(t, b, anchor), shoes));
                    }
                }
                for (Clothing d : dresses) {
                    cores.addAll(withShoes(Arrays.asList(d, anchor), shoes));
                }
            }

            boolean anchorIsLayer = sameText(cat, "Layer");
            boolean anchorIsAccessory = sameText(cat, "Accessory");

            for (List<Clothing> core : cores) {
                results.add(finish(
                        core,
                        anchorIsLayer ? new ArrayList<Clothing>() : layers,
                        anchorIsAccessory ? new ArrayList<Clothing>() : accessories,
                        wantsLayer && !anchorIsLayer,
                        skipAccessory,
                        occ, sea, relaxed));
            }
        }

        for (Outfit o : results) {
            o.reasons.add(0, "Built around your " + lower(anchor.getColor())
                    + " " + lower(anchor.getCategory()));
        }

        Collections.sort(results, new Comparator<Outfit>() {
            @Override
            public int compare(Outfit a, Outfit b) {
                return Double.compare(b.score, a.score);
            }
        });

        return pickDiverse(results, maxResults);
    }

    private static List<List<Clothing>> withShoes(
            List<Clothing> core, List<Clothing> shoes) {

        List<List<Clothing>> out = new ArrayList<>();

        if (shoes.isEmpty()) {
            out.add(new ArrayList<>(core));
            return out;
        }

        for (Clothing shoe : shoes) {
            List<Clothing> items = new ArrayList<>(core);
            items.add(shoe);
            out.add(items);
        }

        return out;
    }

}
