package com.smxy.myapplication.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smxy.myapplication.R;
import com.smxy.myapplication.adapter.RecipeAdapter;
import com.smxy.myapplication.adapter.StepAdapter;
import com.smxy.myapplication.model.Cuisine;
import com.smxy.myapplication.model.Recipe;
import com.smxy.myapplication.network.ApiClient;
import com.smxy.myapplication.network.ApiResponse;
import com.smxy.myapplication.network.ApiService;
import com.smxy.myapplication.network.CuisineResponse;
import com.smxy.myapplication.network.RecipesResponse;
import com.smxy.myapplication.utils.ErrorHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyRecipesFragment extends Fragment {

    private static final String TAG = "MyRecipesFragment";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private SwipeRefreshLayout swipeRefresh;
    private EditText etSearch;
    private Button btnSearch;
    private FloatingActionButton fabAdd;

    private RecipeAdapter adapter;
    private final List<Recipe> allRecipes = new ArrayList<>();
    private final List<Recipe> displayRecipes = new ArrayList<>();
    private ApiService apiService;
    private String authToken;
    private String searchKeyword = "";

    private List<Cuisine> cuisines = new ArrayList<>();

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (!ErrorHandler.isFragmentValid(this)) return;
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    String selectedImagePath = getRealPathFromURI(imageUri);
                    ErrorHandler.showToast(getContext(), getString(R.string.image_selected) + ": " + selectedImagePath);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_recipes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!ErrorHandler.isFragmentValid(this)) return;

        recyclerView = view.findViewById(R.id.recipe_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmpty = view.findViewById(R.id.tv_empty);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        etSearch = view.findViewById(R.id.et_search);
        btnSearch = view.findViewById(R.id.btn_search);
        fabAdd = view.findViewById(R.id.fab_add_recipe);

        setupRecyclerView();
        setupListeners();
        setupSwipeRefresh();
        setupSearch();
        loadToken();
        fetchCuisines();
    }

    private void setupSearch() {
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                searchKeyword = etSearch != null ? etSearch.getText().toString().trim() : "";
                filterRecipes();
            });
        }

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 0) {
                        searchKeyword = "";
                        filterRecipes();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void filterRecipes() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        List<Recipe> filteredRecipes = new ArrayList<>();

        for (Recipe recipe : allRecipes) {
            if (recipe == null) continue;

            if (!searchKeyword.isEmpty()) {
                String title = recipe.getTitle() != null ? recipe.getTitle().toLowerCase() : "";
                String description = recipe.getDescription() != null ? recipe.getDescription().toLowerCase() : "";
                if (!title.contains(searchKeyword.toLowerCase()) &&
                        !description.contains(searchKeyword.toLowerCase())) {
                    continue;
                }
            }
            filteredRecipes.add(recipe);
        }

        displayRecipes.clear();
        displayRecipes.addAll(filteredRecipes);
        if (adapter != null) {
            adapter.updateRecipes(displayRecipes);
        }

        updateEmptyView();
    }

    private void updateEmptyView() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        if (displayRecipes.isEmpty()) {
            if (tvEmpty != null) {
                tvEmpty.setVisibility(View.VISIBLE);
                String msg = !searchKeyword.isEmpty() ?
                        getString(R.string.recipe_search_no_result, searchKeyword) :
                        getString(R.string.my_recipes_empty);
                tvEmpty.setText(msg);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }
        } else {
            if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setupRecyclerView() {
        if (recyclerView == null) return;

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecipeAdapter(requireContext(), displayRecipes, new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                if (recipe != null) showRecipeDetail(recipe);
            }

            @Override
            public void onFavoriteClick(Recipe recipe, int position) {
                if (recipe != null) toggleFavorite(recipe, position);
            }

            @Override
            public void onDeleteClick(Recipe recipe, int position) {
                if (recipe != null && !recipe.isDefault()) {
                    confirmDelete(recipe, position);
                } else if (recipe != null && recipe.isDefault()) {
                    ErrorHandler.showToast(getContext(), R.string.recipe_cannot_delete_default);
                }
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> showAddRecipeDialog());
        }
    }

    private void setupSwipeRefresh() {
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::fetchMyRecipes);
        }
    }

    private void loadToken() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        SharedPreferences userPrefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE);
        String token = userPrefs.getString("token", "");
        if (token.isEmpty()) {
            SharedPreferences authPrefs = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE);
            token = authPrefs.getString("token", "");
        }
        authToken = "Bearer " + token;
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    private void fetchCuisines() {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (apiService == null) return;
        if (!ErrorHandler.isTokenValid(authToken)) return;

        Call<CuisineResponse> call = apiService.getCuisines(authToken);
        call.enqueue(new ErrorHandler.SafeCallback<CuisineResponse>(this) {
            @Override
            protected void onSuccess(CuisineResponse data) {
                if (data.getCuisines() != null) {
                    cuisines = data.getCuisines();
                }
                fetchMyRecipes();
            }

            @Override
            protected void onError(String message) {
                fetchMyRecipes();
            }
        });
    }

    private void fetchMyRecipes() {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (apiService == null) return;
        if (!ErrorHandler.isTokenValid(authToken)) {
            showEmptyState(getString(R.string.login_first));
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        Call<RecipesResponse> call = apiService.getMyRecipes(authToken);
        call.enqueue(new ErrorHandler.SafeCallback<RecipesResponse>(this) {
            @Override
            protected void onSuccess(RecipesResponse data) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                allRecipes.clear();
                List<Recipe> recipes = data.getRecipes();
                if (recipes != null) {
                    allRecipes.addAll(recipes);
                    Log.d(TAG, "获取到 " + allRecipes.size() + " 个我的菜谱");
                }
                filterRecipes();
            }

            @Override
            protected void onError(String message) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                showEmptyState(getString(R.string.recipe_load_failed));
            }
        });
    }

    private void showEmptyState(String message) {
        if (!ErrorHandler.isFragmentValid(this)) return;

        if (tvEmpty != null) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(message);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void showRecipeDetail(Recipe recipe) {
        if (!ErrorHandler.isFragmentValid(this) || recipe == null) return;

        StringBuilder content = new StringBuilder();

        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
            content.append("📝 ").append(recipe.getDescription()).append("\n\n");
        }

        content.append("⏱️ ").append(getString(R.string.cook_time)).append(": ")
                .append(recipe.getCookTime()).append(getString(R.string.minutes)).append("\n");
        content.append("📊 ").append(getString(R.string.difficulty)).append(": ");

        String difficulty = recipe.getDifficulty();
        if ("easy".equals(difficulty)) {
            content.append(getString(R.string.recipe_difficulty_easy));
        } else if ("medium".equals(difficulty)) {
            content.append(getString(R.string.recipe_difficulty_medium));
        } else if ("hard".equals(difficulty)) {
            content.append(getString(R.string.recipe_difficulty_hard));
        } else {
            content.append(ErrorHandler.getSafeString(difficulty, ""));
        }
        content.append("\n\n");

        content.append("【🥬 ").append(getString(R.string.ingredients)).append("】\n");
        List<String> ingredients = recipe.getIngredients();
        if (ingredients != null && !ingredients.isEmpty()) {
            for (String ingredient : ingredients) {
                content.append("• ").append(ingredient).append("\n");
            }
        } else {
            content.append(getString(R.string.recipe_no_ingredients)).append("\n");
        }

        content.append("\n【👨‍🍳 ").append(getString(R.string.steps)).append("】\n");
        List<String> steps = recipe.getSteps();
        if (steps != null && !steps.isEmpty()) {
            for (int i = 0; i < steps.size(); i++) {
                content.append(i + 1).append(". ").append(steps.get(i)).append("\n");
            }
        } else {
            content.append(getString(R.string.recipe_no_steps)).append("\n");
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(ErrorHandler.getSafeString(recipe.getTitle(), "菜谱详情"))
                .setMessage(content.toString())
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private void toggleFavorite(Recipe recipe, int position) {
        if (!ErrorHandler.isFragmentValid(this) || recipe == null) return;
        if (apiService == null) return;

        if (recipe.isFavoriteRequesting()) {
            ErrorHandler.showToast(getContext(), R.string.operation_in_progress);
            return;
        }
        recipe.setFavoriteRequesting(true);

        Call<ApiResponse> call = recipe.isFavorited()
                ? apiService.unfavoriteRecipe(authToken, recipe.getId())
                : apiService.favoriteRecipe(authToken, recipe.getId());

        call.enqueue(new ErrorHandler.SafeCallback<ApiResponse>(this) {
            @Override
            protected void onSuccess(ApiResponse data) {
                recipe.setFavoriteRequesting(false);
                boolean newState = !recipe.isFavorited();
                recipe.setFavorited(newState);
                if (adapter != null) {
                    adapter.updateRecipe(position, recipe);
                }

                for (Recipe r : allRecipes) {
                    if (r != null && r.getId() == recipe.getId()) {
                        r.setFavorited(newState);
                        break;
                    }
                }

                ErrorHandler.showToast(getContext(), newState ? R.string.recipe_favorite_success : R.string.recipe_unfavorite_success);
            }

            @Override
            protected void onError(String message) {
                recipe.setFavoriteRequesting(false);
                ErrorHandler.showToast(getContext(), message);
            }
        });
    }

    private void confirmDelete(Recipe recipe, int position) {
        if (!ErrorHandler.isFragmentValid(this) || recipe == null) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.recipe_delete_title)
                .setMessage(getString(R.string.recipe_confirm_delete,
                        ErrorHandler.getSafeString(recipe.getTitle(), "这个菜谱")))
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteRecipe(recipe, position))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteRecipe(Recipe recipe, int position) {
        if (!ErrorHandler.isFragmentValid(this) || recipe == null) return;
        if (apiService == null) return;

        Call<ApiResponse> call = apiService.deleteRecipe(authToken, recipe.getId());
        call.enqueue(new ErrorHandler.SafeCallback<ApiResponse>(this) {
            @Override
            protected void onSuccess(ApiResponse data) {
                displayRecipes.remove(position);
                allRecipes.remove(recipe);
                if (adapter != null) {
                    adapter.removeRecipe(position);
                }
                ErrorHandler.showToast(getContext(), R.string.recipe_delete_success);
                updateEmptyView();
            }

            @Override
            protected void onError(String message) {
                ErrorHandler.showToast(getContext(), R.string.recipe_delete_failed);
            }
        });
    }

    private void showAddRecipeDialog() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_recipe_optimized, null);

        TextInputEditText etName = dialogView.findViewById(R.id.et_recipe_name);
        AutoCompleteTextView cuisineInput = dialogView.findViewById(R.id.actv_cuisine);
        TextInputEditText etCookTime = dialogView.findViewById(R.id.et_cook_time);
        AutoCompleteTextView difficultyInput = dialogView.findViewById(R.id.actv_difficulty);
        AutoCompleteTextView ingredientInput = dialogView.findViewById(R.id.actv_ingredient_input);
        Button btnAddIngredient = dialogView.findViewById(R.id.btn_add_ingredient);
        ChipGroup chipGroupIngredients = dialogView.findViewById(R.id.chip_group_ingredients);
        RecyclerView rvSteps = dialogView.findViewById(R.id.rv_steps);
        Button btnAddStep = dialogView.findViewById(R.id.btn_add_step);
        Button btnSelectImage = dialogView.findViewById(R.id.btn_select_image);

        List<String> cuisineNames = new ArrayList<>();
        if (cuisines != null) {
            for (Cuisine c : cuisines) {
                if (c != null && c.getName() != null) {
                    cuisineNames.add(c.getName());
                }
            }
        }

        ArrayAdapter<String> cuisineAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, cuisineNames);
        cuisineInput.setAdapter(cuisineAdapter);

        String[] difficultyLevels = {getString(R.string.recipe_difficulty_easy),
                getString(R.string.recipe_difficulty_medium),
                getString(R.string.recipe_difficulty_hard)};
        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, difficultyLevels);
        difficultyInput.setAdapter(difficultyAdapter);

        List<String> ingredientsList = new ArrayList<>();
        List<String> stepsList = new ArrayList<>();
        StepAdapter stepAdapter = new StepAdapter(stepsList);
        rvSteps.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSteps.setAdapter(stepAdapter);

        String[] commonIngredients = getResources().getStringArray(R.array.common_ingredients);
        ArrayAdapter<String> ingredientSuggestionAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, commonIngredients);
        ingredientInput.setAdapter(ingredientSuggestionAdapter);
        ingredientInput.setThreshold(1);

        if (btnAddIngredient != null) {
            btnAddIngredient.setOnClickListener(v -> {
                String ingredient = ingredientInput.getText().toString().trim();
                if (!ingredient.isEmpty()) {
                    ingredientsList.add(ingredient);
                    addIngredientChip(chipGroupIngredients, ingredient, ingredientsList);
                    ingredientInput.setText("");
                } else {
                    ErrorHandler.showToast(getContext(), R.string.enter_ingredient);
                }
            });
        }

        if (btnAddStep != null) {
            btnAddStep.setOnClickListener(v -> showAddStepDialog(stepsList, stepAdapter));
        }

        if (btnSelectImage != null) {
            btnSelectImage.setOnClickListener(v -> selectImage());
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.recipe_add_title)
                .setView(dialogView)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    if (name.isEmpty()) {
                        ErrorHandler.showToast(getContext(), R.string.recipe_name_hint);
                        return;
                    }

                    if (ingredientsList.isEmpty()) {
                        ErrorHandler.showToast(getContext(), R.string.add_ingredients_hint);
                        return;
                    }

                    if (stepsList.isEmpty()) {
                        ErrorHandler.showToast(getContext(), R.string.add_steps_hint);
                        return;
                    }

                    String cookTimeStr = etCookTime.getText() != null ? etCookTime.getText().toString().trim() : "";
                    int cookTime = cookTimeStr.isEmpty() ? 0 : Integer.parseInt(cookTimeStr);

                    String difficulty = difficultyInput.getText().toString();
                    String difficultyEn = "medium";
                    if (getString(R.string.recipe_difficulty_easy).equals(difficulty)) difficultyEn = "easy";
                    else if (getString(R.string.recipe_difficulty_hard).equals(difficulty)) difficultyEn = "hard";

                    String cuisineName = cuisineInput.getText().toString();
                    int cuisineId = 0;
                    if (cuisines != null) {
                        for (Cuisine c : cuisines) {
                            if (c != null && c.getName() != null && c.getName().equals(cuisineName)) {
                                cuisineId = c.getId();
                                break;
                            }
                        }
                    }

                    addRecipe(name, getString(R.string.default_recipe_desc, name),
                            ingredientsList, stepsList, cookTime, difficultyEn, cuisineId);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void addIngredientChip(ChipGroup chipGroup, String ingredient, List<String> ingredientsList) {
        if (!ErrorHandler.isFragmentValid(this) || chipGroup == null) return;

        Chip chip = new Chip(getContext());
        chip.setText(ingredient);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            chipGroup.removeView(chip);
            ingredientsList.remove(ingredient);
        });
        chipGroup.addView(chip);
    }

    private void showAddStepDialog(List<String> stepsList, StepAdapter adapter) {
        if (!ErrorHandler.isFragmentValid(this)) return;

        EditText input = new EditText(getContext());
        input.setHint(R.string.step_input_hint);
        input.setPadding(50, 20, 50, 20);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_step)
                .setView(input)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    String step = input.getText().toString().trim();
                    if (!step.isEmpty()) {
                        stepsList.add(step);
                        if (adapter != null) {
                            adapter.notifyItemInserted(stepsList.size() - 1);
                        }
                    } else {
                        ErrorHandler.showToast(getContext(), R.string.enter_step);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void addRecipe(String name, String description, List<String> ingredients,
                           List<String> steps, int cookTime, String difficulty, int cuisineId) {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (apiService == null) return;

        Map<String, Object> recipeData = new HashMap<>();
        recipeData.put("name", name);
        recipeData.put("description", description);
        recipeData.put("ingredients", ingredients);
        recipeData.put("steps", steps);
        recipeData.put("cook_time", cookTime);
        recipeData.put("difficulty", difficulty);
        if (cuisineId > 0) {
            recipeData.put("cuisine_id", cuisineId);
        }

        Call<ApiResponse> call = apiService.addRecipe(authToken, recipeData);
        call.enqueue(new ErrorHandler.SafeCallback<ApiResponse>(this) {
            @Override
            protected void onSuccess(ApiResponse data) {
                ErrorHandler.showToast(getContext(), R.string.recipe_add_success);
                fetchMyRecipes();
            }

            @Override
            protected void onError(String message) {
                ErrorHandler.showToast(getContext(), getString(R.string.recipe_add_failed));
            }
        });
    }

    private void selectImage() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private String getRealPathFromURI(Uri uri) {
        if (uri == null || !ErrorHandler.isFragmentValid(this)) return "";

        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            try {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(columnIndex);
            } catch (Exception e) {
                Log.e(TAG, "获取图片路径失败", e);
            } finally {
                cursor.close();
            }
        }
        return uri.getPath();
    }
}