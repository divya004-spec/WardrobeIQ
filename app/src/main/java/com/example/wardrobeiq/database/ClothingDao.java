package com.example.wardrobeiq.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.wardrobeiq.models.Clothing;

import java.util.List;

@Dao
public interface ClothingDao {

    @Insert
    long insertClothing(Clothing clothing);

    @Update
    void updateClothing(Clothing clothing);

    @Delete
    void deleteClothing(Clothing clothing);

    @Query("SELECT * FROM clothing WHERE userId = :userId ORDER BY id DESC")
    List<Clothing> getClothingByUser(int userId);

    @Query("SELECT * FROM clothing WHERE id = :clothingId LIMIT 1")
    Clothing getClothingById(int clothingId);

    @Query("SELECT * FROM clothing WHERE userId = :userId AND category = :category")
    List<Clothing> getClothingByCategory(
            int userId,
            String category
    );

    @Query("SELECT * FROM clothing WHERE userId = :userId AND color = :color")
    List<Clothing> getClothingByColor(
            int userId,
            String color
    );

    @Query("DELETE FROM clothing WHERE userId = :userId")
    void deleteAllUserClothing(int userId);
}