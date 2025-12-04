package com.bugetwise.campusexpense.data.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.bugetwise.campusexpense.data.model.User;

@Dao
public interface UserDao {
    @Insert
    long insert(User user);
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User getUserByUsername(String username);

    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    User login(String username, String password);

    @Query("SELECT COUNT (*) FROM users WHERE username = :username")
    int checkUsernameExists(String username);
}
