package com.bugetwise.campusexpense.ui.expense;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bugetwise.campusexpense.R;
import com.bugetwise.campusexpense.data.model.Expense;
import com.bugetwise.campusexpense.R;
import com.bugetwise.campusexpense.data.model.Category;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<ExpenseItem> expenseList;
    private List<Category> categoryList;
    private OnExpenseClickListener onExpenseClickListener;
    private OnExpenseLongClickListener onExpenseLongClickListener;

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }

    public interface OnExpenseLongClickListener {
        void onExpenseLongClick(Expense expense);
    }

    public static class ExpenseItem {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_EXPENSE = 1;

        public int type;
        public String headerText;
        public Expense expense;

        public ExpenseItem(String headerText) {
            this.type = TYPE_HEADER;
            this.headerText = headerText;
        }

        public ExpenseItem(Expense expense) {
            this.type = TYPE_EXPENSE;
            this.expense = expense;
        }
    }

    private android.content.Context context;

    public ExpenseRecyclerAdapter(List<Expense> expenseList,
                                  List<Category> categoryList,
                                  OnExpenseClickListener onExpenseClickListener,
                                  OnExpenseLongClickListener onExpenseLongClickListener) {
        this.onExpenseClickListener = onExpenseClickListener;
        this.onExpenseLongClickListener = onExpenseLongClickListener;
        this.categoryList = categoryList != null ? categoryList : new ArrayList<>();
        this.expenseList = new ArrayList<>();
    }

    public void setContext(android.content.Context context) {
        this.context = context;
    }

    private List<ExpenseItem> groupExpensesByDate(List<Expense> expenses) {
        List<ExpenseItem> items = new ArrayList<>();
        if (expenses.isEmpty()) {
            return items;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat todayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        String currentDateHeader = null;
        String today = todayFormat.format(new Date());

        for (Expense expense : expenses) {
            String expenseDate = todayFormat.format(new Date(expense.getDate()));
            String dateHeader;

            if (expenseDate.equals(today)) {
                dateHeader = context != null ? context.getString(R.string.today) : "Today";
            } else {
                dateHeader = dateFormat.format(new Date(expense.getDate()));
            }

            if (!dateHeader.equals(currentDateHeader)) {
                items.add(new ExpenseItem(dateHeader));
                currentDateHeader = dateHeader;
            }

            items.add(new ExpenseItem(expense));
        }

        return items;
    }

    @Override
    public int getItemViewType(int position) {
        return expenseList.get(position).type == ExpenseItem.TYPE_HEADER ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_expense_section_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_expense_date, parent, false);
            return new ExpenseViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ExpenseItem item = expenseList.get(position);

        if (item.type == ExpenseItem.TYPE_HEADER) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.headerText.setText(item.headerText);
        } else {
            ExpenseViewHolder expenseHolder = (ExpenseViewHolder) holder;
            Expense expense = item.expense;
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            String categoryName = getCategoryName(expense.getCategoryId());
            expenseHolder.categoryNameText.setText(categoryName);
            expenseHolder.amountText.setText(currencyFormat.format(expense.getAmount()));
            expenseHolder.timeText.setText(timeFormat.format(new Date(expense.getDate())));

            if (expense.getDescription() != null && !expense.getDescription().trim().isEmpty()) {
                expenseHolder.descriptionText.setText(expense.getDescription());
                expenseHolder.descriptionText.setVisibility(View.VISIBLE);
            } else {
                expenseHolder.descriptionText.setVisibility(View.GONE);
            }

            expenseHolder.itemView.setOnClickListener(v -> {
                if (onExpenseClickListener != null) {
                    onExpenseClickListener.onExpenseClick(expense);
                }
            });

            expenseHolder.itemView.setOnLongClickListener(v -> {
                if (onExpenseLongClickListener != null) {
                    onExpenseLongClickListener.onExpenseLongClick(expense);
                }
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public void updateExpenses(List<Expense> expenses) {
        this.expenseList = groupExpensesByDate(expenses);
        notifyDataSetChanged();
    }

    public void setCategoryList(List<Category> categoryList) {
        this.categoryList = categoryList != null ? categoryList : new ArrayList<>();
        notifyDataSetChanged();
    }

    private String getCategoryName(int categoryId) {
        for (Category category : categoryList) {
            if (category.getId() == categoryId) {
                return category.getName();
            }
        }
        return "Unknown";
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerText;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerText = (TextView) itemView;
        }
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        View categoryColorView;
        TextView categoryNameText;
        TextView descriptionText;
        TextView amountText;
        TextView timeText;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryColorView = itemView.findViewById(R.id.categoryColorView);
            categoryNameText = itemView.findViewById(R.id.categoryNameText);
            descriptionText = itemView.findViewById(R.id.descriptionText);
            amountText = itemView.findViewById(R.id.amountText);
            timeText = itemView.findViewById(R.id.timeText);
        }
    }
}

