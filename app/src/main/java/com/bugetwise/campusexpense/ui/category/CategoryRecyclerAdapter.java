package com.bugetwise.campusexpense.ui.category;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bugetwise.campusexpense.R;
import com.bugetwise.campusexpense.data.model.Category;

import java.util.List;

public class CategoryRecyclerAdapter extends RecyclerView.Adapter<CategoryRecyclerAdapter.ViewHolder> {
    private List<Category> categoriesList;
    private onEditClickListener editClick;
    private onDeleteClickListener deleteClick;

    public interface onEditClickListener {
        void onEditClick(Category category);
    }

    public interface onDeleteClickListener {
        void onDeleteClick(Category category);
    }

    public CategoryRecyclerAdapter(List<Category> categories, onEditClickListener editClickListener, onDeleteClickListener deleteClickListener) {
        this.categoriesList = categories;
        this.editClick = editClickListener;
        this.deleteClick = deleteClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categoriesList.get(position);
        holder.nameText.setText(category.getName());
        holder.editButton.setOnClickListener(v -> editClick.onEditClick(category));
        holder.deleteButton.setOnClickListener(v -> deleteClick.onDeleteClick(category));
    }
    @Override
    public int getItemCount() {
        return categoriesList.size();
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        ImageButton editButton;
        ImageButton deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.nameText);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}