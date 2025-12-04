package com.bugetwise.campusexpense.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budgets")
public class Budget {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int userId;
    private int categoryId;
    private double amount;
    private String period;
    private long createdAt;;

    public Budget (){}
    public Budget (int userId, int categoryId, double amount, String period) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.period = period;
        this.createdAt = System.currentTimeMillis();

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

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }
}
