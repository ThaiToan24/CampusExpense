package com.bugetwise.campusexpense.ui.fragments;

import static android.app.ProgressDialog.show;

import android.app.AlertDialog;
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
import com.bugetwise.campusexpense.data.model.Budget;
import com.bugetwise.campusexpense.data.model.Category;
import com.bugetwise.campusexpense.ui.budget.BudgetRecyclerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BudgetFragment extends Fragment {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private BudgetRecyclerAdapter adapter;
    private List<Budget> budgetsList;
    private List<Category> categoryList;
    private List<String> categoryNames;
    private BudgetDao budgetDao;
    private CategoryDao categoryDao;
    private TextView emtyView;
    private SharedPreferences sharedPreferences;
    private int currentUserId;

    // Executor để chạy query Room trên background thread
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        fabAdd = view.findViewById(R.id.fabAdd);
        emtyView = view.findViewById(R.id.emptyView);
        sharedPreferences = requireContext().getSharedPreferences("UserSession", 0);
        // Dùng cùng key "userId" như LoginActivity / ExpenseFragment để đồng bộ user
        currentUserId = sharedPreferences.getInt("userId", -1);
        AppDatabase database = AppDatabase.getInstance(requireContext());
        budgetDao = database.budgetDao();
        categoryDao = database.categoryDao();
        budgetsList = new ArrayList<>();
        categoryList = new ArrayList<>();
        categoryNames = new ArrayList<>();
        adapter = new BudgetRecyclerAdapter(
                budgetsList,
                categoryNames,
                this::showEditDialog,
                this::showDeleteDialog
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        fabAdd.setOnClickListener(v -> showAddDialog());
        refreshList();
        return view;
    }

    private void refreshList() {
        dbExecutor.execute(() -> {
            List<Budget> dbBudgets = budgetDao.getAllBudgetsByUser(currentUserId);
            List<Category> dbCategories = new ArrayList<>();
            List<String> newCategoryNames = new ArrayList<>();

            for (Budget budget : dbBudgets) {
                Category category = categoryDao.getById(budget.getCategoryId());
                dbCategories.add(category);
                newCategoryNames.add(category != null ? category.getName() : "Unknown");
            }

            requireActivity().runOnUiThread(() -> {
                budgetsList.clear();
                budgetsList.addAll(dbBudgets);

                categoryList.clear();
                categoryList.addAll(dbCategories);

                categoryNames.clear();
                categoryNames.addAll(newCategoryNames);

                adapter.notifyDataSetChanged();
                // Nếu có dữ liệu thì hiển thị RecyclerView, nếu không thì hiện emptyView
                if (budgetsList.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emtyView.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emtyView.setVisibility(View.GONE);
                }
            });
        });
    }

    private void showDeleteDialog(Budget budget) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_budget)
                .setMessage(R.string.confirm_delete_budget)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    dbExecutor.execute(() -> {
                        budgetDao.delete(budget);
                        requireActivity().runOnUiThread(() -> {
                            refreshList();
                            Toast.makeText(requireContext(),(R.string.budget_deleted_successfully), Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
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
                View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_budget, null);
                Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
                TextInputEditText amountInput = dialogView.findViewById(R.id.amountInput);
                Spinner periodSpinner = dialogView.findViewById(R.id.periodSpinner);
                Button saveButton = dialogView.findViewById(R.id.saveButton);
                Button cancelButton = dialogView.findViewById(R.id.cancelButton);

                List<String> categoryNameList = new ArrayList<>();
                for (Category cat : categoryList) {
                    categoryNameList.add(cat.getName());
                }

                ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, categoryNameList);
                categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categorySpinner.setAdapter(categoryAdapter);

                String[] periods = {"Monthly", "Weekly"};
                ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, periods);
                periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                periodSpinner.setAdapter(periodAdapter);
                builder.setView(dialogView);
                AlertDialog dialog = builder.create();
                saveButton.setOnClickListener(v -> {
                    int categoryPosition = categorySpinner.getSelectedItemPosition();
                    String amountStr = amountInput.getText().toString().trim();
                    int periodPosition = periodSpinner.getSelectedItemPosition();


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
                    String period = periods[periodPosition];

                    dbExecutor.execute(() -> {
                        Budget existingBudget = budgetDao.getBudgetByCategoryAndUser(currentUserId, selectedCategory.getId());
                        if (existingBudget != null) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "Budget for this category already exists", Toast.LENGTH_SHORT).show()
                            );
                            return;
                        }

                        Budget budget = new Budget(currentUserId, selectedCategory.getId(), amount, period);
                        budgetDao.insert(budget);
                        requireActivity().runOnUiThread(() -> {
                            refreshList();
                            dialog.dismiss();
                            Toast.makeText(requireContext(), "Budget added successfully", Toast.LENGTH_SHORT).show();
                        });
                    });
                });
                cancelButton.setOnClickListener(v -> dialog.dismiss());
                dialog.show();
            });
        });
    }

    private void showEditDialog(Budget budget) {
        dbExecutor.execute(() -> {
            List<Category> allCategories = categoryDao.getAll();
            Category budgetCategory = categoryDao.getById(budget.getCategoryId());

            requireActivity().runOnUiThread(() -> {
                categoryList.clear();
                categoryList.addAll(allCategories);

                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_budget, null);

        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        TextInputEditText amountInput = dialogView.findViewById(R.id.amountInput);
        Spinner periodSpinner = dialogView.findViewById(R.id.periodSpinner);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        List<String> categoryNameList = new ArrayList<>();
        for (Category cat : categoryList) {
            categoryNameList.add(cat.getName());
        }

                ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, categoryNameList);
                categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categorySpinner.setAdapter(categoryAdapter);

                if (budgetCategory != null) {
                    int categoryIndex = categoryList.indexOf(budgetCategory);
                    if (categoryIndex >= 0) {
                        categorySpinner.setSelection(categoryIndex);
                    }
                }
                categorySpinner.setEnabled(false);

        amountInput.setText(String.valueOf(budget.getAmount()));

        String[] periods = {"Monthly", "Weekly"};
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, periods);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(periodAdapter);

        int periodIndex = -1;
        for (int i = 0; i < periods.length; i++) {
            if (periods[i].equals(budget.getPeriod())) {
                periodIndex = i;
                break;
            }
        }
        if (periodIndex >= 0) {
            periodSpinner.setSelection(periodIndex);
        }

                builder.setView(dialogView);
                AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String amountStr = amountInput.getText().toString().trim();
            int periodPosition = periodSpinner.getSelectedItemPosition();

            if (TextUtils.isEmpty(amountStr)) {
                Toast.makeText(requireContext(), "Please enter amount", Toast.LENGTH_SHORT).show();
                return;
            }

            if (periodPosition < 0 || periodPosition >= periods.length) {
                Toast.makeText(requireContext(), "Please select period", Toast.LENGTH_SHORT).show();
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

            budget.setAmount(amount);
            budget.setPeriod(periods[periodPosition]);

            dbExecutor.execute(() -> {
                budgetDao.update(budget);
                requireActivity().runOnUiThread(() -> {
                    refreshList();
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Budget updated successfully", Toast.LENGTH_SHORT).show();
                });
            });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
            });
        });
    }

}