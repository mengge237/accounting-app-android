package com.smxy.myapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smxy.myapplication.R;
import com.smxy.myapplication.model.Recipe;
import com.smxy.myapplication.utils.ErrorHandler;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private final Context context;
    private List<Recipe> recipes;
    private final OnRecipeClickListener listener;
    private int selectedPosition = -1;

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
        void onFavoriteClick(Recipe recipe, int position);
        void onDeleteClick(Recipe recipe, int position);
    }

    public RecipeAdapter(Context context, List<Recipe> recipes, OnRecipeClickListener listener) {
        this.context = context;
        this.recipes = recipes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view, listener, context);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        if (recipes != null && position < recipes.size()) {
            Recipe recipe = recipes.get(position);
            if (recipe != null) {
                holder.bind(recipe, position);
            }
        }

        if (holder.card != null) {
            holder.card.setClickable(true);
            holder.card.setFocusable(true);
            if (context != null) {
                holder.card.setForeground(ContextCompat.getDrawable(context, R.drawable.card_ripple));
            }

            if (selectedPosition == position) {
                holder.card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.selected_recipe_bg));
            } else {
                holder.card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white));
            }
        }

        if (holder.card != null) {
            holder.card.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION && recipes != null && adapterPosition < recipes.size()) {
                    int previousSelected = selectedPosition;
                    selectedPosition = adapterPosition;

                    if (previousSelected != -1) {
                        notifyItemChanged(previousSelected);
                    }
                    notifyItemChanged(selectedPosition);

                    if (listener != null) {
                        listener.onRecipeClick(recipes.get(adapterPosition));
                    }

                    v.animate()
                            .scaleX(0.98f)
                            .scaleY(0.98f)
                            .setDuration(100)
                            .withEndAction(() -> {
                                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                            })
                            .start();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return recipes != null ? recipes.size() : 0;
    }

    public void updateRecipes(List<Recipe> newRecipes) {
        this.recipes = newRecipes;
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    public void updateRecipe(int position, Recipe recipe) {
        if (position >= 0 && recipes != null && position < recipes.size()) {
            recipes.set(position, recipe);
            notifyItemChanged(position);
        }
    }

    public void removeRecipe(int position) {
        if (position >= 0 && recipes != null && position < recipes.size()) {
            recipes.remove(position);
            if (selectedPosition == position) {
                selectedPosition = -1;
            } else if (selectedPosition > position) {
                selectedPosition--;
            }
            notifyItemRemoved(position);
        }
    }

    public Recipe getSelectedRecipe() {
        if (selectedPosition >= 0 && recipes != null && selectedPosition < recipes.size()) {
            return recipes.get(selectedPosition);
        }
        return null;
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public int getPositionByRecipeId(int recipeId) {
        if (recipes != null) {
            for (int i = 0; i < recipes.size(); i++) {
                Recipe r = recipes.get(i);
                if (r != null && r.getId() == recipeId) {
                    return i;
                }
            }
        }
        return -1;
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        private final CardView card;
        private final TextView tvTitle;
        private final TextView tvDescription;
        private final TextView tvTime;
        private final TextView tvDifficulty;
        private final ImageView ivFavorite;
        private final ImageView ivDelete;
        private final OnRecipeClickListener listener;
        private final Context context;

        RecipeViewHolder(@NonNull View itemView, OnRecipeClickListener listener, Context context) {
            super(itemView);
            this.listener = listener;
            this.context = context;
            card = itemView.findViewById(R.id.recipe_card);
            tvTitle = itemView.findViewById(R.id.recipe_title);
            tvDescription = itemView.findViewById(R.id.recipe_description);
            tvTime = itemView.findViewById(R.id.recipe_time);
            tvDifficulty = itemView.findViewById(R.id.recipe_difficulty);
            ivFavorite = itemView.findViewById(R.id.iv_favorite);
            ivDelete = itemView.findViewById(R.id.iv_delete);
        }

        void bind(Recipe recipe, int position) {
            if (recipe == null) return;

            if (tvTitle != null) {
                tvTitle.setText(ErrorHandler.getSafeString(recipe.getTitle(), "无标题"));
            }

            String desc = recipe.getDescription();
            if (desc != null && desc.length() > 80) {
                desc = desc.substring(0, 77) + "...";
            }
            if (tvDescription != null) {
                tvDescription.setText(desc != null ? desc : "暂无描述");
            }

            if (tvTime != null && context != null) {
                String timeText = context.getString(R.string.recipe_time_minutes, recipe.getCookTime());
                tvTime.setText(timeText);
            }

            String difficulty = recipe.getDifficulty();
            if (tvDifficulty != null) {
                if ("easy".equals(difficulty)) {
                    tvDifficulty.setText(R.string.recipe_difficulty_easy);
                } else if ("medium".equals(difficulty)) {
                    tvDifficulty.setText(R.string.recipe_difficulty_medium);
                } else if ("hard".equals(difficulty)) {
                    tvDifficulty.setText(R.string.recipe_difficulty_hard);
                } else {
                    tvDifficulty.setText(ErrorHandler.getSafeString(difficulty, "中等"));
                }
            }

            if (ivFavorite != null) {
                ivFavorite.setImageResource(recipe.isFavorited()
                        ? android.R.drawable.btn_star_big_on
                        : android.R.drawable.btn_star_big_off);
            }

            if (ivDelete != null) {
                ivDelete.setVisibility(recipe.isDefault() ? View.GONE : View.VISIBLE);
            }

            if (ivFavorite != null) {
                ivFavorite.setOnClickListener(v -> {
                    int adapterPosition = getAdapterPosition();
                    if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
                        listener.onFavoriteClick(recipe, adapterPosition);
                    }
                });
            }

            if (ivDelete != null) {
                ivDelete.setOnClickListener(v -> {
                    int adapterPosition = getAdapterPosition();
                    if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
                        listener.onDeleteClick(recipe, adapterPosition);
                    }
                });
            }
        }
    }
}