package com.bugetwise.campusexpense.ui.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bugetwise.campusexpense.R;
import com.bugetwise.campusexpense.data.database.AppDatabase;
import com.bugetwise.campusexpense.data.database.BudgetDao;
import com.bugetwise.campusexpense.data.database.ExpenseDao;
import com.bugetwise.campusexpense.data.database.CategoryDao;
import com.bugetwise.campusexpense.data.model.Budget;
import com.bugetwise.campusexpense.data.model.Category;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    // Model cho phần phân bổ chi tiêu
    private static class DistributionItem {
        final String categoryName;
        final double amount;
        final double percent;

        DistributionItem(String categoryName, double amount, double percent) {
            this.categoryName = categoryName;
            this.amount = amount;
            this.percent = percent;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Lấy reference tới các view trong fragment_home.xml
        TextView textWelcomeUser = view.findViewById(R.id.text_welcome_user);

        TextView textTotalExpense = view.findViewById(R.id.text_total_expense);
        TextView textExpenseDescription = view.findViewById(R.id.text_expense_description);

        TextView textBudgetCategoryName = view.findViewById(R.id.text_budget_category_name);
        TextView textBudgetLimit = view.findViewById(R.id.text_budget_limit);
        TextView textBudgetSpent = view.findViewById(R.id.text_budget_spent);
        ProgressBar progressBarBudget = view.findViewById(R.id.progress_bar_budget);

        TextView textStatsTransactionsCount = view.findViewById(R.id.text_stats_transactions_count);
        TextView textStatsAvgPerDay = view.findViewById(R.id.text_stats_avg_per_day);
        TextView textStatsBudgetCount = view.findViewById(R.id.text_stats_budget_count);

        TextView textSummaryTotalBudget = view.findViewById(R.id.text_summary_total_budget);
        TextView textSummarySpent = view.findViewById(R.id.text_summary_spent);
        TextView textSummaryRemaining = view.findViewById(R.id.text_summary_remaining);

        LinearLayout distributionContainer = view.findViewById(R.id.layout_expense_distribution_container);

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("UserSession", 0);
        int userId = sharedPreferences.getInt("userId", -1);
        String username = sharedPreferences.getString("username", "");

        if (!username.isEmpty()) {
            textWelcomeUser.setText("Welcome, " + username);
        } else {
            textWelcomeUser.setText("Welcome");
        }

        if (userId == -1) {
            // Không có user -> set về 0 hết
            textTotalExpense.setText(formatCurrency(0));
            textExpenseDescription.setText("");
            textBudgetCategoryName.setText("No budget");
            textBudgetLimit.setText(formatCurrency(0));
            textBudgetSpent.setText(formatCurrency(0));
            progressBarBudget.setMax(100);
            progressBarBudget.setProgress(0);
            textStatsTransactionsCount.setText("0");
            textStatsAvgPerDay.setText(formatCurrency(0));
            textStatsBudgetCount.setText("0");
            textSummaryTotalBudget.setText(formatCurrency(0));
            textSummarySpent.setText(formatCurrency(0));
            textSummaryRemaining.setText(formatCurrency(0));
            return;
        }

        AppDatabase database = AppDatabase.getInstance(requireContext());
        ExpenseDao expenseDao = database.expenseDao();
        BudgetDao budgetDao = database.budgetDao();
        CategoryDao categoryDao = database.categoryDao();

        // Tính khoảng thời gian trong tháng hiện tại
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfMonth = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        long endOfMonth = calendar.getTimeInMillis();

        dbExecutor.execute(() -> {
            // Tổng chi tiêu tháng này
            Double totalExpenseValue = expenseDao.getTotalExpensesByDateRange(userId, startOfMonth, endOfMonth);
            if (totalExpenseValue == null) totalExpenseValue = 0.0;

            // Số giao dịch tháng này
            int transactionCount = expenseDao.getExpenseCountByDateRange(userId, startOfMonth, endOfMonth);

            // Danh sách budget của user
            List<Budget> budgets = budgetDao.getAllBudgetsByUser(userId);
            int budgetCount = budgets != null ? budgets.size() : 0;

            double totalBudgetAmount = 0.0;
            for (Budget b : budgets) {
                totalBudgetAmount += b.getAmount();
            }

            // Card Budget: dùng tổng tất cả budget thay vì chỉ 1 cái
            String budgetCardTitle = budgetCount > 0 ? "All Budgets" : "No budget";
            double cardBudgetLimit = totalBudgetAmount;
            double cardBudgetSpent = totalExpenseValue; // đã chi trong tháng so với tổng budget

            // Tính Avg/Day: tổng chi / số ngày đã trôi qua trong tháng (tính đến hôm nay)
            Calendar todayCal = Calendar.getInstance();
            int dayOfMonth = todayCal.get(Calendar.DAY_OF_MONTH);
            double avgPerDay = dayOfMonth > 0 ? (totalExpenseValue / dayOfMonth) : 0.0;

            double remainingTotal = totalBudgetAmount - totalExpenseValue;

            // Tính phân bổ chi tiêu theo category trong tháng hiện tại
            List<Category> allCategories = categoryDao.getAll();
            List<DistributionItem> distributionItems = new java.util.ArrayList<>();
            double totalForDistribution = 0.0;

            for (Category cat : allCategories) {
                Double spentForCat = expenseDao.getTotalExpensesByCategoryAndDateRange(
                        userId,
                        cat.getId(),
                        startOfMonth,
                        endOfMonth
                );
                double amountForCat = spentForCat != null ? spentForCat : 0.0;
                if (amountForCat > 0) {
                    totalForDistribution += amountForCat;
                    distributionItems.add(new DistributionItem(cat.getName(), amountForCat, 0));
                }
            }

            if (totalForDistribution > 0) {
                for (int i = 0; i < distributionItems.size(); i++) {
                    DistributionItem old = distributionItems.get(i);
                    double percent = (old.amount / totalForDistribution) * 100.0;
                    distributionItems.set(i, new DistributionItem(old.categoryName, old.amount, percent));
                }
            }

            // Các biến final / effectively final để dùng trong lambda
            final double totalExpenseFinal = totalExpenseValue;
            final int transactionCountFinal = transactionCount;
            final int budgetCountFinal = budgetCount;
            final double totalBudgetAmountFinal = totalBudgetAmount;
            final String budgetCardTitleFinal = budgetCardTitle;
            final double cardBudgetLimitFinal = cardBudgetLimit;
            final double cardBudgetSpentFinal = cardBudgetSpent;
            final double avgPerDayFinal = avgPerDay;
            final double remainingTotalFinal = remainingTotal;
            final long finalStartOfMonth = startOfMonth;
            final List<DistributionItem> distributionItemsFinal = new java.util.ArrayList<>(distributionItems);

            requireActivity().runOnUiThread(() -> {
                // Total expense
                textTotalExpense.setText(formatCurrency(totalExpenseFinal));

                SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                String monthLabel = monthFormat.format(new Date(finalStartOfMonth));
                textExpenseDescription.setText(monthLabel);

                // Budget card (tổng tất cả budget)
                textBudgetCategoryName.setText(budgetCardTitleFinal);
                textBudgetLimit.setText("Budget: " + formatCurrency(cardBudgetLimitFinal));
                textBudgetSpent.setText(formatCurrency(cardBudgetSpentFinal));

                progressBarBudget.setMax(100);
                int progress = 0;
                if (cardBudgetLimitFinal > 0) {
                    progress = (int) Math.min(100,
                            Math.round((cardBudgetSpentFinal / cardBudgetLimitFinal) * 100));
                }
                progressBarBudget.setProgress(progress);

                // Quick stats
                textStatsTransactionsCount.setText(String.valueOf(transactionCountFinal));
                textStatsAvgPerDay.setText(formatCurrency(avgPerDayFinal));
                textStatsBudgetCount.setText(String.valueOf(budgetCountFinal));

                // Monthly summary
                textSummaryTotalBudget.setText(formatCurrency(totalBudgetAmountFinal));
                textSummarySpent.setText(formatCurrency(totalExpenseFinal));
                textSummaryRemaining.setText(formatCurrency(remainingTotalFinal));

                // Expense Distribution
                distributionContainer.removeAllViews();
                LayoutInflater inflater = LayoutInflater.from(requireContext());

                if (distributionItemsFinal.isEmpty()) {
                    TextView noData = new TextView(requireContext());
                    noData.setText("No expenses this month");
                    distributionContainer.addView(noData);
                } else {
                    for (DistributionItem item : distributionItemsFinal) {
                        View row = inflater.inflate(R.layout.item_expense_distribution, distributionContainer, false);
                        TextView catNameView = row.findViewById(R.id.text_dist_category_name);
                        TextView amountView = row.findViewById(R.id.text_dist_amount);
                        ProgressBar distProgress = row.findViewById(R.id.progress_bar_distribution);

                        catNameView.setText(item.categoryName);
                        String percentText = String.format(Locale.getDefault(), "%.1f%%", item.percent);
                        amountView.setText(formatCurrency(item.amount) + "   " + percentText);

                        distProgress.setMax(100);
                        distProgress.setProgress((int) Math.round(item.percent));

                        distributionContainer.addView(row);
                    }
                }
            });
        });
    }

    private String formatCurrency(double value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
        return format.format(value);
    }
}
