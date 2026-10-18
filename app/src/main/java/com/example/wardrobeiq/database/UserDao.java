package com.example.wardrobeiq.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.wardrobeiq.models.User;

@Dao
public interface UserDao {

    @Insert
    long insertUser(User user);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);
}