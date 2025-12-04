package com.bugetwise.campusexpense.data.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.bugetwise.campusexpense.data.model.Budget;

import java.util.List;

@Dao
public interface BudgetDao {
    @Insert
    long insert(Budget budget);

    @Update
    void update(Budget budget);

    @Delete
    void delete(Budget budget);

    @Query("SELECT * FROM budgets WHERE userId = :userId ORDER BY createdAt DESC")
    List<Budget> getAllBudgetsByUser(int userId);

    @Query("SELECT * FROM  budgets WHERE userId = :userId AND categoryId = :categoryId ORDER BY createdAt DESC")
    Budget  getBudgetByCategoryAndUser(int userId, int categoryId);

    @Query("SELECT * FROM budgets WHERE id = :Id")
    Budget getBudgetById(int Id);

}
