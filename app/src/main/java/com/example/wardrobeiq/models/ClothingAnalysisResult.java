package com.example.wardrobeiq.models;

public class ClothingAnalysisResult {

    private boolean wardrobeItem;

    private String category;
    private String color;
    private String pattern;
    private String occasion;
    private String season;

    private int warmth;
    private int formality;

    private double confidence;

    private String message;

    public ClothingAnalysisResult() {
    }

    public boolean isWardrobeItem() {
        return wardrobeItem;
    }

    public void setWardrobeItem(boolean wardrobeItem) {
        this.wardrobeItem = wardrobeItem;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getOccasion() {
        return occasion;
    }

    public void setOccasion(String occasion) {
        this.occasion = occasion;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public int getWarmth() {
        return warmth;
    }

    public void setWarmth(int warmth) {
        this.warmth = warmth;
    }

    public int getFormality() {
        return formality;
    }

    public void setFormality(int formality) {
        this.formality = formality;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}