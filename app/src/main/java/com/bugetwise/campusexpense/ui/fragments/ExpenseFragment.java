package com.bugetwise.campusexpense.ui.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bugetwise.campusexpense.R;
import com.bugetwise.campusexpense.data.database.AppDatabase;
import com.bugetwise.campusexpense.data.database.BudgetDao;
import com.bugetwise.campusexpense.data.database.CategoryDao;
import com.bugetwise.campusexpense.data.database.ExpenseDao;
import com.bugetwise.campusexpense.data.model.Budget;
import com.bugetwise.campusexpense.data.model.Category;
import com.bugetwise.campusexpense.data.model.Expense;
import com.bugetwise.campusexpense.ui.expense.CategoryExpenseAdapter;
import com.bugetwise.campusexpense.ui.expense.ExpenseRecyclerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExpenseFragment extends Fragment {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private TabLayout tabLayout;
    private Spinner monthSpinner;
    private Spinner categoryFilterSpinner;
    private TextView totalExpenseText;
    private TextView expenseCountText;
    private TextView emptyView;

    private ExpenseDao expenseDao;
    private CategoryDao categoryDao;
    private BudgetDao budgetDao;
    private SharedPreferences sharedPreferences;
    private int currentUserId;

    private CategoryExpenseAdapter categoryAdapter;
    private ExpenseRecyclerAdapter expenseAdapter;

    private List<Category> categoryList;
    private List<CategoryExpenseAdapter.CategoryExpenseItem> categoryExpenseList;
    private List<Expense> expenseList;

    private int currentMonth;
    private int currentYear;
    private int selectedCategoryId = -1;
    private int currentTab = 0;

    // Executor để chạy query Room trên background thread, tránh block main thread
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expense, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        fabAdd = view.findViewById(R.id.fabAdd);
        tabLayout = view.findViewById(R.id.tabLayout);
        monthSpinner = view.findViewById(R.id.monthSpinner);
        categoryFilterSpinner = view.findViewById(R.id.categoryFilterSpinner);
        totalExpenseText = view.findViewById(R.id.totalExpenseText);
        expenseCountText = view.findViewById(R.id.expenseCountText);
        emptyView = view.findViewById(R.id.emptyView);

        sharedPreferences = requireContext().getSharedPreferences("UserSession", 0);
        currentUserId = sharedPreferences.getInt("userId", -1);

        AppDatabase database = AppDatabase.getInstance(requireContext());
        expenseDao = database.expenseDao();
        categoryDao = database.categoryDao();
        budgetDao = database.budgetDao();

        categoryList = new ArrayList<>();
        categoryExpenseList = new ArrayList<>();
        expenseList = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        currentMonth = calendar.get(Calendar.MONTH);
        currentYear = calendar.get(Calendar.YEAR);

        setupTabs();
        setupSpinners();
        setupRecyclerView();

        fabAdd.setOnClickListener(v -> showAddDialog());

        refreshData();

        return view;
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_by_category));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_by_date));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                refreshData();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSpinners() {
        Calendar calendar = Calendar.getInstance();
        List<String> months = new ArrayList<>();
        int currentMonthIndex = calendar.get(Calendar.MONTH);
        int currentYearValue = calendar.get(Calendar.YEAR);

        for (int i = -6; i <= 6; i++) {
            calendar.set(currentYearValue, currentMonthIndex + i, 1);
            months.add(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.getTime()));
        }

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);
        monthSpinner.setSelection(6);
        monthSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                calendar.set(currentYearValue, currentMonthIndex + (position - 6), 1);
                currentMonth = calendar.get(Calendar.MONTH);
                currentYear = calendar.get(Calendar.YEAR);
                refreshData();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Load categories và set adapter spinner trên background thread
        dbExecutor.execute(() -> {
            List<String> categoryNames = new ArrayList<>();
            categoryNames.add(getString(R.string.all_categories));

            List<Category> dbCategories = categoryDao.getAll();

            requireActivity().runOnUiThread(() -> {
                categoryList.clear();
                categoryList.addAll(dbCategories);

                for (Category cat : categoryList) {
                    categoryNames.add(cat.getName());
                }

                ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, categoryNames);
                categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categoryFilterSpinner.setAdapter(categoryAdapter);
                categoryFilterSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                        if (position == 0) {
                            selectedCategoryId = -1;
                        } else {
                            selectedCategoryId = categoryList.get(position - 1).getId();
                        }
                        refreshData();
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });
            });
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        categoryAdapter = new CategoryExpenseAdapter(categoryExpenseList, (categoryId, categoryName) -> {
            showCategoryExpensesDialog(categoryId, categoryName);
        });
        categoryAdapter.setContext(requireContext());

        expenseAdapter = new ExpenseRecyclerAdapter(expenseList, categoryList,
                expense -> showEditDialog(expense),
                expense -> showDeleteDialog(expense));
        expenseAdapter.setContext(requireContext());
    }

    private void refreshData() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(currentYear, currentMonth, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startDate = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        long endDate = calendar.getTimeInMillis();

        if (currentTab == 0) {
            refreshCategoryData(startDate, endDate);
        } else {
            refreshDateData(startDate, endDate);
        }
    }

    private void refreshCategoryData(long startDate, long endDate) {
        dbExecutor.execute(() -> {
            List<Category> allCategories = categoryDao.getAll();

            List<CategoryExpenseAdapter.CategoryExpenseItem> newCategoryExpenseList = new ArrayList<>();

            List<Category> categories = new ArrayList<>();
            if (selectedCategoryId == -1) {
                categories.addAll(allCategories);
            } else {
                for (Category cat : allCategories) {
                    if (cat.getId() == selectedCategoryId) {
                        categories.add(cat);
                        break;
                    }
                }
            }

            for (Category category : categories) {
                List<Expense> expenses = expenseDao.getExpensesByCategoryAndDateRange(currentUserId, category.getId(), startDate, endDate);

                Double total = expenseDao.getTotalExpensesByCategoryAndDateRange(currentUserId, category.getId(), startDate, endDate);
                double totalExpense = total != null ? total : 0.0;

                if (totalExpense > 0 || selectedCategoryId != -1) {
                    Budget budget = budgetDao.getBudgetByCategoryAndUser(currentUserId, category.getId());
                    newCategoryExpenseList.add(new CategoryExpenseAdapter.CategoryExpenseItem(
                            category.getId(),
                            category.getName(),
                            totalExpense,
                            expenses.size(),
                            budget
                    ));
                }
            }

            Double totalForStats = selectedCategoryId == -1 ?
                    expenseDao.getTotalExpensesByDateRange(currentUserId, startDate, endDate) :
                    expenseDao.getTotalExpensesByCategoryAndDateRange(currentUserId, selectedCategoryId, startDate, endDate);

            requireActivity().runOnUiThread(() -> {
                categoryList.clear();
                categoryList.addAll(allCategories);

                categoryExpenseList.clear();
                categoryExpenseList.addAll(newCategoryExpenseList);

                recyclerView.setAdapter(categoryAdapter);
                categoryAdapter.notifyDataSetChanged();

                updateStatisticsWithTotal(startDate, endDate, totalForStats, /*useExpenseListSize*/ false);
                updateEmptyView();
            });
        });
    }

    private void refreshDateData(long startDate, long endDate) {
        dbExecutor.execute(() -> {
            List<Expense> newExpenseList;
            if (selectedCategoryId == -1) {
                newExpenseList = expenseDao.getExpensesByDateRange(currentUserId, startDate, endDate);
            } else {
                newExpenseList = expenseDao.getExpensesByCategoryAndDateRange(currentUserId, selectedCategoryId, startDate, endDate);
            }

            List<Category> allCategories = categoryDao.getAll();

            Double totalForStats = selectedCategoryId == -1 ?
                    expenseDao.getTotalExpensesByDateRange(currentUserId, startDate, endDate) :
                    expenseDao.getTotalExpensesByCategoryAndDateRange(currentUserId, selectedCategoryId, startDate, endDate);

            requireActivity().runOnUiThread(() -> {
                expenseList.clear();
                expenseList.addAll(newExpenseList);

                categoryList.clear();
                categoryList.addAll(allCategories);
                expenseAdapter.setCategoryList(categoryList);

                recyclerView.setAdapter(expenseAdapter);
                expenseAdapter.updateExpenses(expenseList);

                updateStatisticsWithTotal(startDate, endDate, totalForStats, /*useExpenseListSize*/ true);
                updateEmptyView();
            });
        });
    }

    private void updateStatistics(long startDate, long endDate) {
        // Giữ lại để tương thích cũ, nhưng chuyển sang dùng hàm mới có sẵn total nếu cần
        updateStatisticsWithTotal(startDate, endDate, null, true);
    }

    private void updateStatisticsWithTotal(long startDate, long endDate, @Nullable Double precomputedTotal, boolean useExpenseListSize) {
        Double total = precomputedTotal;
        if (total == null) {
            total = selectedCategoryId == -1 ?
                    expenseDao.getTotalExpensesByDateRange(currentUserId, startDate, endDate) :
                    expenseDao.getTotalExpensesByCategoryAndDateRange(currentUserId, selectedCategoryId, startDate, endDate);
        }

        double totalExpense = total != null ? total : 0.0;
        int count = useExpenseListSize ? expenseList.size() : categoryExpenseList.size();

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        totalExpenseText.setText(currencyFormat.format(totalExpense));
        expenseCountText.setText(String.valueOf(count));
    }

    private void updateEmptyView() {
        boolean isEmpty = (currentTab == 0 && categoryExpenseList.isEmpty()) ||
                (currentTab == 1 && expenseList.isEmpty());

        if (isEmpty) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddDialog() {
        dbExecutor.execute(() -> {
            List<Category> allCategories = categoryDao.getAll();

            requireActivity().runOnUiThread(() -> {
                categoryList.clear();
                categoryList.addAll(allCategories);

                if (categoryList.isEmpty()) {
                    Toast.makeText(requireContext(), "Please add categories first", Toast.LENGTH_SHORT).show();
                    return;
                }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_expense, null);

        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        TextInputEditText amountInput = dialogView.findViewById(R.id.amountInput);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.descriptionInput);
        Button dateButton = dialogView.findViewById(R.id.dateButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

                List<String> categoryNames = new ArrayList<>();
                for (Category cat : categoryList) {
                    categoryNames.add(cat.getName());
                }

                ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, categoryNames);
                categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categorySpinner.setAdapter(categoryAdapter);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dateButton.setText(getString(R.string.select_date));
        long[] selectedDate = {calendar.getTimeInMillis()};

        dateButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        selectedDate[0] = calendar.getTimeInMillis();
                        dateButton.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

                builder.setView(dialogView);
                AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            int categoryPosition = categorySpinner.getSelectedItemPosition();
            String amountStr = amountInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();

            if (TextUtils.isEmpty(amountStr)) {
                Toast.makeText(requireContext(), "Please enter amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(requireContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            Category selectedCategory = categoryList.get(categoryPosition);
            Expense expense = new Expense(currentUserId, selectedCategory.getId(), amount, description, selectedDate[0]);

            dbExecutor.execute(() -> {
                expenseDao.insert(expense);
                requireActivity().runOnUiThread(() -> {
                    refreshData();
                    dialog.dismiss();
                    Toast.makeText(requireContext(), R.string.expense_added, Toast.LENGTH_SHORT).show();
                });
            });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
            });
        });
    }

    private void showEditDialog(Expense expense) {
        dbExecutor.execute(() -> {
            List<Category> allCategories = categoryDao.getAll();

            requireActivity().runOnUiThread(() -> {
                categoryList.clear();
                categoryList.addAll(allCategories);

                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_expense, null);

        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        TextInputEditText amountInput = dialogView.findViewById(R.id.amountInput);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.descriptionInput);
        Button dateButton = dialogView.findViewById(R.id.dateButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        List<String> categoryNames = new ArrayList<>();
        for (Category cat : categoryList) {
            categoryNames.add(cat.getName());
        }

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        int categoryIndex = -1;
        for (int i = 0; i < categoryList.size(); i++) {
            if (categoryList.get(i).getId() == expense.getCategoryId()) {
                categoryIndex = i;
                break;
            }
        }
        if (categoryIndex >= 0) {
            categorySpinner.setSelection(categoryIndex);
        }
        categorySpinner.setEnabled(false);

        amountInput.setText(String.valueOf(expense.getAmount()));
        descriptionInput.setText(expense.getDescription());

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(expense.getDate());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dateButton.setText(dateFormat.format(calendar.getTime()));
        long[] selectedDate = {expense.getDate()};

        dateButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        selectedDate[0] = calendar.getTimeInMillis();
                        dateButton.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

                builder.setView(dialogView);
                AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String amountStr = amountInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();

            if (TextUtils.isEmpty(amountStr)) {
                Toast.makeText(requireContext(), "Please enter amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(requireContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            expense.setAmount(amount);
            expense.setDescription(description);
            expense.setDate(selectedDate[0]);

            dbExecutor.execute(() -> {
                expenseDao.update(expense);
                requireActivity().runOnUiThread(() -> {
                    refreshData();
                    dialog.dismiss();
                    Toast.makeText(requireContext(), R.string.expense_updated, Toast.LENGTH_SHORT).show();
                });
            });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
            });
        });
    }

    private void showDeleteDialog(Expense expense) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_expense)
                .setMessage(R.string.confirm_delete_expense)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    dbExecutor.execute(() -> {
                        expenseDao.delete(expense);
                        requireActivity().runOnUiThread(() -> {
                            refreshData();
                            Toast.makeText(requireContext(), R.string.expense_deleted, Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showCategoryExpensesDialog(int categoryId, String categoryName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.expense_title, categoryName));

        dbExecutor.execute(() -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(currentYear, currentMonth, 1, 0, 0, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long startDate = calendar.getTimeInMillis();

            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.MILLISECOND, -1);
            long endDate = calendar.getTimeInMillis();

            List<Expense> expenses = expenseDao.getExpensesByCategoryAndDateRange(currentUserId, categoryId, startDate, endDate);

            requireActivity().runOnUiThread(() -> {
                if (expenses.isEmpty()) {
                    builder.setMessage(R.string.no_transactions);
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.show();
                    return;
                }

                StringBuilder message = new StringBuilder();
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

                for (Expense expense : expenses) {
                    message.append(dateFormat.format(new Date(expense.getDate())));
                    message.append(" - ");
                    message.append(currencyFormat.format(expense.getAmount()));
                    if (expense.getDescription() != null && !expense.getDescription().trim().isEmpty()) {
                        message.append("\n");
                        message.append(expense.getDescription());
                    }
                    message.append("\n\n");
                }

                builder.setMessage(message.toString());
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
            });
        });
    }
}

