package com.bugetwise.campusexpense.ui.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bugetwise.campusexpense.R;
import com.bugetwise.campusexpense.data.database.AppDatabase;
import com.bugetwise.campusexpense.data.database.CategoryDao;
import com.bugetwise.campusexpense.data.model.Category;
import com.bugetwise.campusexpense.ui.category.CategoryRecyclerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryFragment extends Fragment {
    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private CategoryRecyclerAdapter adapter;
    private List<Category> categoryList;
    private CategoryDao categoryDao;
    private TextView emptyView;

    // Executor để chạy query Room trên background thread
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_category, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        fabAdd = view.findViewById(R.id.fabAdd);
        emptyView = view.findViewById(R.id.emptyView);
        AppDatabase database = AppDatabase.getInstance(requireContext());
        categoryDao = database.categoryDao();
        categoryList = new ArrayList<>();

        adapter = new CategoryRecyclerAdapter(
                categoryList,
                this::showEditDialog,
                this::showDeleteDialog  );
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showAddDialog());

        refreshList();
        return view;

    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_category, null);
        EditText nameInput = view.findViewById(R.id.nameInput);
        Button saveButton = view.findViewById(R.id.saveButton);
        Button cancelButton = view.findViewById(R.id.cancelButton);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        saveButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(requireContext(), getString(R.string.error_empty_category_name), Toast.LENGTH_SHORT).show();
                return;
            }
            Category category = new Category(name);
            dbExecutor.execute(() -> {
                categoryDao.insert(category);
                requireActivity().runOnUiThread(() -> {
                    refreshList();
                    dialog.dismiss();
                    Toast.makeText(requireContext(), getString(R.string.category_added), Toast.LENGTH_SHORT).show();
                });
            });
        });
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void refreshList() {
        dbExecutor.execute(() -> {
            List<Category> allCategories = categoryDao.getAll();
            requireActivity().runOnUiThread(() -> {
                categoryList.clear();
                categoryList.addAll(allCategories);
                adapter.notifyDataSetChanged();
                if (categoryList.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void showDeleteDialog(Category category) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_category))
                .setMessage(getString(R.string.confirm_delete_category))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    dbExecutor.execute(() -> {
                        categoryDao.delete(category);
                        requireActivity().runOnUiThread(() -> {
                            refreshList();
                            Toast.makeText(requireContext(), getString(R.string.category_deleted), Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showEditDialog(Category category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_category, null);
        EditText nameInput = view.findViewById(R.id.nameInput);
        Button saveButton = view.findViewById(R.id.saveButton);
        Button cancelButton = view.findViewById(R.id.cancelButton);
        nameInput.setText(category.getName());
        builder.setView(view);
        AlertDialog dialog = builder.create();
        saveButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(requireContext(), getString(R.string.error_empty_category_name), Toast.LENGTH_SHORT).show();
                return;
            }
            category.setName(name);
            dbExecutor.execute(() -> {
                categoryDao.update(category);
                requireActivity().runOnUiThread(() -> {
                    refreshList();
                    dialog.dismiss();
                    Toast.makeText(requireContext(), getString(R.string.category_updated), Toast.LENGTH_SHORT).show();
                });
            });
        });
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

}


