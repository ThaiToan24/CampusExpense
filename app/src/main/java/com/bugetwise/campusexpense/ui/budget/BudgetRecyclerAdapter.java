package com.bugetwise.campusexpense.ui.budget;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bugetwise.campusexpense.R;
import com.bugetwise.campusexpense.data.model.Budget;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class BudgetRecyclerAdapter extends RecyclerView.Adapter<BudgetRecyclerAdapter.ViewHolder> {
    private List<Budget> budgetsList;
    private List<String> categoryNames;
    private onEditClickListener onEditClickListener;
    private onDeleteClickListener onDeleteClickListener;

    public interface onEditClickListener {
        void onEditClick(Budget budget);
    }
    public interface onDeleteClickListener {
        void onDeleteClick(Budget budget);
    }

    @Override
    public int getItemCount() {
        return budgetsList.size();
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryNameText;
        TextView periodText;
        TextView amountText;
        ProgressBar progressBar;
        TextView progressText;
        ImageButton editButton;
        ImageButton deleteButton;
        ViewHolder(View itemView) {
            super(itemView);
            categoryNameText = itemView.findViewById(R.id.categoryNameText);
            periodText = itemView.findViewById(R.id.periodText);
            amountText = itemView.findViewById(R.id.amountText);
            progressBar = itemView.findViewById(R.id.progressBar);
            progressText = itemView.findViewById(R.id.progressText);

            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    public BudgetRecyclerAdapter(List<Budget> budgets, List<String> categoryNames, onEditClickListener onEditClickListener, onDeleteClickListener onDeleteClickListener) {
        this.budgetsList = budgets;
        this.categoryNames = categoryNames;
        this.onEditClickListener = onEditClickListener;
        this.onDeleteClickListener = onDeleteClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Budget budget = budgetsList.get(position);
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        String categoryName = (position < categoryNames.size()) ? categoryNames.get(position) : "Unknown Category";
        holder.categoryNameText.setText(categoryName);
        holder.amountText.setText(String.valueOf(budget.getAmount()));
        holder.periodText.setText(budget.getPeriod());
        double spent = 0.0;
        double percentage = budget.getAmount() > 0 ? spent / budget.getAmount() * 100 : 0;
        int progress = (int) Math.min(Math.max(percentage, 0), 100);
        holder.progressBar.setProgress(progress);
        holder.periodText.setText(budget.getPeriod());
        holder.progressText.setText(String.format(Locale.getDefault(), "%.0f%% used", percentage));
        holder.editButton.setOnClickListener(v -> onEditClickListener.onEditClick(budget));
        holder.deleteButton.setOnClickListener(v -> onDeleteClickListener.onDeleteClick(budget));
    }
}
