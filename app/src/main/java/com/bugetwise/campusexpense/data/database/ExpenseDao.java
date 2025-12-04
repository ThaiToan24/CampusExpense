package com.bugetwise.campusexpense.data.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.bugetwise.campusexpense.data.model.Expense;
import java.util.List;

@Dao
public interface ExpenseDao {

    @Insert
    long insert(Expense expense);

    @Update
    void update(Expense expense);

    @Delete
    void delete(Expense expense);

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC, createdAt DESC")
    List<Expense> getAllExpensesByUser(int userId);

    @Query("SELECT * FROM expenses WHERE userId = :userId AND categoryId = :categoryId ORDER BY date DESC, createdAt DESC")
    List<Expense> getExpensesByCategoryAndUser(int userId, int categoryId);

    @Query("SELECT * FROM expenses WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC, createdAt DESC")
    List<Expense> getExpensesByDateRange(int userId, long startDate, long endDate);

    @Query("SELECT * FROM expenses WHERE userId = :userId AND categoryId = :categoryId AND date >= :startDate AND date <= :endDate ORDER BY date DESC, createdAt DESC")
    List<Expense> getExpensesByCategoryAndDateRange(int userId, int categoryId, long startDate, long endDate);

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND date >= :startDate AND date <= :endDate")
    Double getTotalExpensesByDateRange(int userId, long startDate, long endDate);

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND categoryId = :categoryId AND date >= :startDate AND date <= :endDate")
    Double getTotalExpensesByCategoryAndDateRange(int userId, int categoryId, long startDate, long endDate);

    @Query("SELECT * FROM expenses WHERE id = :id")
    Expense getExpenseById(int id);

    @Query("SELECT COUNT(*) FROM expenses WHERE userId = :userId AND date >= :startDate AND date <= :endDate")
    int getExpenseCountByDateRange(int userId, long startDate, long endDate);
}
