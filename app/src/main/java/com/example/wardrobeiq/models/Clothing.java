package com.example.wardrobeiq.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "clothing")
public class Clothing {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int userId;

    private String imagePath;

    private String category;

    private String color;

    private String pattern;

    private String occasion;

    private String season;

    private int warmth;

    private int formality;

    private int wearCount;

    private String lastWorn;

    public Clothing(
            int userId,
            String imagePath,
            String category,
            String color,
            String pattern,
            String occasion,
            String season,
            int warmth,
            int formality,
            int wearCount,
            String lastWorn) {

        this.userId = userId;
        this.imagePath = imagePath;
        this.category = category;
        this.color = color;
        this.pattern = pattern;
        this.occasion = occasion;
        this.season = season;
        this.warmth = warmth;
        this.formality = formality;
        this.wearCount = wearCount;
        this.lastWorn = lastWorn;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getCategory() {
        return category;
    }

    public String getColor() {
        return color;
    }

    public String getPattern() {
        return pattern;
    }

    public String getOccasion() {
        return occasion;
    }

    public String getSeason() {
        return season;
    }

    public int getWarmth() {
        return warmth;
    }

    public int getFormality() {
        return formality;
    }

    public int getWearCount() {
        return wearCount;
    }

    public String getLastWorn() {
        return lastWorn;
    }

    public void setWearCount(int wearCount) {
        this.wearCount = wearCount;
    }

    public void setLastWorn(String lastWorn) {
        this.lastWorn = lastWorn;
    }
}