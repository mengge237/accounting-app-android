package com.smxy.myapplication.fragment;

import android.annotation.SuppressLint;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
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
import com.smxy.myapplication.manager.LayoutManager;
import com.smxy.myapplication.manager.TextToSpeechHelper;
import com.smxy.myapplication.model.Cuisine;
import com.smxy.myapplication.model.Recipe;
import com.smxy.myapplication.network.ApiClient;
import com.smxy.myapplication.network.ApiResponse;
import com.smxy.myapplication.network.ApiService;
import com.smxy.myapplication.network.CuisineResponse;
import com.smxy.myapplication.network.FavoritesResponse;
import com.smxy.myapplication.network.RecipesResponse;
import com.smxy.myapplication.utils.ErrorHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecipeFragment extends Fragment {

    private static final String TAG = "RecipeFragment";

    // UI组件
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private TextView tabAll;
    private TextView tabFavorite;
    private FloatingActionButton fabAdd;
    private SwipeRefreshLayout swipeRefresh;
    private ChipGroup chipGroupCuisine;
    private EditText etSearch;
    private Button btnSearch;

    // 语音播报按钮（添加到搜索栏右侧）
    private ImageButton btnSpeakRecipe;

    // 数据相关
    private RecipeAdapter adapter;
    private final List<Recipe> allRecipes = new ArrayList<>();
    private final List<Recipe> displayRecipes = new ArrayList<>();
    private List<Cuisine> cuisines = new ArrayList<>();
    private ApiService apiService;
    private String authToken;

    // 筛选条件
    private boolean showOnlyFavorites = false;
    private boolean hideDefaultRecipes = false;
    private int currentCuisineId = 0;
    private String searchKeyword = "";

    private LayoutManager layoutManager;
    private String selectedImagePath = "";

    // 语音播报
    private TextToSpeechHelper ttsHelper;
    private boolean isSpeaking = false;

    // 图片选择器
    private ImageView ivRecipePreview;
    private TextView tvImagePath;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (getActivity() != null && result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        selectedImagePath = getRealPathFromURI(imageUri);
                        if (ivRecipePreview != null) {
                            ivRecipePreview.setImageURI(imageUri);
                            ivRecipePreview.setVisibility(View.VISIBLE);
                        }
                        if (tvImagePath != null) {
                            tvImagePath.setText(selectedImagePath);
                            tvImagePath.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipe, container, false);

        // 初始化视图
        initViews(view);

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recipe_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmpty = view.findViewById(R.id.tv_empty);
        tabAll = view.findViewById(R.id.tab_all);
        tabFavorite = view.findViewById(R.id.tab_favorite);
        fabAdd = view.findViewById(R.id.fab_add_recipe);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        chipGroupCuisine = view.findViewById(R.id.chip_group_cuisine);
        etSearch = view.findViewById(R.id.et_search);
        btnSearch = view.findViewById(R.id.btn_search);

        // 动态添加语音播报按钮到搜索栏（因为原布局没有这个按钮）
        addSpeakButtonToSearchBar(view);
    }

    /**
     * 动态添加语音播报按钮到搜索栏
     */
    private void addSpeakButtonToSearchBar(View view) {
        // 找到搜索栏的 CardView 和内部的 LinearLayout
        androidx.cardview.widget.CardView searchCard = view.findViewById(R.id.et_search).getParent().getParent() instanceof androidx.cardview.widget.CardView ?
                (androidx.cardview.widget.CardView) view.findViewById(R.id.et_search).getParent().getParent() : null;

        if (searchCard != null) {
            LinearLayout searchLayout = searchCard.findViewById(android.R.id.content);
            if (searchLayout != null && searchLayout instanceof LinearLayout) {
                // 创建语音播报按钮
                btnSpeakRecipe = new ImageButton(getContext());
                btnSpeakRecipe.setImageResource(android.R.drawable.ic_lock_silent_mode);
                btnSpeakRecipe.setBackgroundResource(android.R.color.transparent);
                btnSpeakRecipe.setPadding(12, 12, 12, 12);

                // 设置按钮参数
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = android.view.Gravity.CENTER_VERTICAL;
                btnSpeakRecipe.setLayoutParams(params);
                btnSpeakRecipe.setContentDescription("语音播报菜谱");

                // 添加按钮到搜索栏
                searchLayout.addView(btnSpeakRecipe);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupListeners();
        setupSwipeRefresh();
        setupSearch();
        loadToken();

        layoutManager = new LayoutManager(requireContext());
        loadHideDefaultSetting();

        updateTabStyle(true);

        // 初始化语音播报
        initTextToSpeech();

        // 设置语音播报按钮点击事件
        if (btnSpeakRecipe != null) {
            btnSpeakRecipe.setOnClickListener(v -> {
                if (isSpeaking) {
                    stopSpeaking();
                } else {
                    speakCurrentSelectedRecipe();
                }
            });
        }

        fetchCuisinesAndRecipes();
    }

    /**
     * 初始化文字转语音
     */
    private void initTextToSpeech() {
        if (!ErrorHandler.isContextValid(getContext())) return;

        ttsHelper = TextToSpeechHelper.getInstance(requireContext());
        if (ttsHelper != null) {
            ttsHelper.setOnSpeakListener(new TextToSpeechHelper.OnSpeakListener() {
                @Override
                public void onStart(String utteranceId) {
                    isSpeaking = true;
                    if (btnSpeakRecipe != null && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            btnSpeakRecipe.setImageResource(android.R.drawable.ic_media_pause);
                            btnSpeakRecipe.setAlpha(0.7f);
                        });
                    }
                }

                @Override
                public void onDone(String utteranceId) {
                    isSpeaking = false;
                    if (btnSpeakRecipe != null && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            btnSpeakRecipe.setImageResource(android.R.drawable.ic_lock_silent_mode);
                            btnSpeakRecipe.setAlpha(1.0f);
                        });
                    }
                }

                @Override
                public void onError(String utteranceId) {
                    isSpeaking = false;
                    if (btnSpeakRecipe != null && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            btnSpeakRecipe.setImageResource(android.R.drawable.ic_lock_silent_mode);
                            btnSpeakRecipe.setAlpha(1.0f);
                        });
                    }
                    Toast.makeText(getContext(), "语音播报出错", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * 播报当前选中的菜谱
     */
    private void speakCurrentSelectedRecipe() {
        Recipe selectedRecipe = adapter.getSelectedRecipe();
        if (selectedRecipe == null && !displayRecipes.isEmpty()) {
            selectedRecipe = displayRecipes.get(0);
        }

        if (selectedRecipe != null) {
            speakRecipeContent(selectedRecipe);
        } else {
            Toast.makeText(getContext(), "没有可播报的菜谱，请先选择一个菜谱", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 播报菜谱内容
     */
    private void speakRecipeContent(Recipe recipe) {
        if (recipe == null) {
            Toast.makeText(getContext(), "菜谱信息为空", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ttsHelper == null || !ttsHelper.isInitialized()) {
            Toast.makeText(getContext(), "语音引擎未就绪，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }

        // 构建播报内容
        StringBuilder content = new StringBuilder();

        // 菜谱名称
        content.append("菜谱名称：").append(recipe.getTitle()).append("。");

        // 描述
        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
            content.append("简介：").append(recipe.getDescription()).append("。");
        }

        // 烹饪时间
        content.append("烹饪时间：").append(recipe.getCookTime()).append("分钟。");

        // 难度
        content.append("难度：");
        String difficulty = recipe.getDifficulty();
        if ("easy".equals(difficulty)) {
            content.append("简单");
        } else if ("medium".equals(difficulty)) {
            content.append("中等");
        } else if ("hard".equals(difficulty)) {
            content.append("困难");
        } else {
            content.append(difficulty);
        }
        content.append("。");

        // 食材
        content.append("所需食材：");
        List<String> ingredients = recipe.getIngredients();
        if (ingredients != null && !ingredients.isEmpty()) {
            for (int i = 0; i < ingredients.size(); i++) {
                content.append(ingredients.get(i));
                if (i < ingredients.size() - 1) {
                    content.append("、");
                }
            }
        } else {
            content.append("暂无食材信息");
        }
        content.append("。");

        // 制作步骤
        content.append("制作步骤：");
        List<String> steps = recipe.getSteps();
        if (steps != null && !steps.isEmpty()) {
            for (int i = 0; i < steps.size(); i++) {
                content.append(i + 1).append("、").append(steps.get(i));
                if (i < steps.size() - 1) {
                    content.append("；");
                }
            }
        } else {
            content.append("暂无步骤信息");
        }

        // 开始播报
        ttsHelper.stop();
        ttsHelper.speak(content.toString(), "recipe_" + recipe.getId());

        Toast.makeText(getContext(), "🎤 开始播报：" + recipe.getTitle(), Toast.LENGTH_SHORT).show();
    }

    /**
     * 停止语音播报
     */
    private void stopSpeaking() {
        if (ttsHelper != null) {
            ttsHelper.stop();
            isSpeaking = false;
            if (btnSpeakRecipe != null) {
                btnSpeakRecipe.setImageResource(android.R.drawable.ic_lock_silent_mode);
                btnSpeakRecipe.setAlpha(1.0f);
            }
            Toast.makeText(getContext(), "已停止播报", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 带语音播报的菜谱详情对话框
     */
    private void showRecipeDetailWithSpeak(Recipe recipe) {
        if (recipe == null) {
            Toast.makeText(getContext(), "菜谱信息为空", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder content = new StringBuilder();

        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
            content.append("📝 ").append(recipe.getDescription()).append("\n\n");
        }

        content.append("⏱️ 烹饪时间: ").append(recipe.getCookTime()).append("分钟\n");
        content.append("📊 难度: ");
        String difficulty = recipe.getDifficulty();
        if ("easy".equals(difficulty)) {
            content.append("简单");
        } else if ("medium".equals(difficulty)) {
            content.append("中等");
        } else if ("hard".equals(difficulty)) {
            content.append("困难");
        } else {
            content.append(difficulty);
        }
        content.append("\n\n");

        content.append("【🥬 食材】\n");
        List<String> ingredients = recipe.getIngredients();
        if (ingredients != null && !ingredients.isEmpty()) {
            for (String ingredient : ingredients) {
                content.append("• ").append(ingredient).append("\n");
            }
        } else {
            content.append("暂无食材信息\n");
        }

        content.append("\n【👨‍🍳 制作步骤】\n");
        List<String> steps = recipe.getSteps();
        if (steps != null && !steps.isEmpty()) {
            for (int i = 0; i < steps.size(); i++) {
                content.append(i + 1).append(". ").append(steps.get(i)).append("\n");
            }
        } else {
            content.append("暂无步骤信息\n");
        }

        try {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(recipe.getTitle())
                    .setMessage(content.toString())
                    .setPositiveButton("关闭", null)
                    .setNegativeButton("🔊 语音播报", (dialog, which) -> {
                        speakRecipeContent(recipe);
                    })
                    .setNeutralButton("❤️ 收藏", (dialog, which) -> {
                        int position = adapter.getPositionByRecipeId(recipe.getId());
                        if (position >= 0) {
                            toggleFavorite(recipe, position);
                        }
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "显示菜谱详情失败: " + e.getMessage());
            Toast.makeText(getContext(), "显示详情失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 通过菜谱名称播报（供外部调用）
     */
    public void speakRecipeByName(String recipeName) {
        if (recipeName == null || recipeName.isEmpty()) {
            Toast.makeText(getContext(), "请输入菜谱名称", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Recipe recipe : allRecipes) {
            if (recipe.getTitle().toLowerCase().contains(recipeName.toLowerCase())) {
                speakRecipeContent(recipe);
                return;
            }
        }

        Toast.makeText(getContext(), "未找到菜谱: " + recipeName, Toast.LENGTH_SHORT).show();
    }

    /**
     * 播报菜谱列表摘要
     */
    public void speakRecipeListSummary() {
        if (displayRecipes == null || displayRecipes.isEmpty()) {
            Toast.makeText(getContext(), "当前没有菜谱", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ttsHelper == null || !ttsHelper.isInitialized()) {
            Toast.makeText(getContext(), "语音引擎未就绪", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder content = new StringBuilder();
        if (showOnlyFavorites) {
            content.append("您收藏了").append(displayRecipes.size()).append("个菜谱。");
        } else {
            content.append("当前共有").append(displayRecipes.size()).append("个菜谱。");
        }

        int count = Math.min(displayRecipes.size(), 5);
        for (int i = 0; i < count; i++) {
            Recipe r = displayRecipes.get(i);
            content.append("第").append(i + 1).append("个，").append(r.getTitle()).append("。");
        }

        if (displayRecipes.size() > 5) {
            content.append("还有").append(displayRecipes.size() - 5).append("个菜谱。");
        }

        ttsHelper.speak(content.toString(), "recipe_list_summary");
    }

    private void setupSearch() {
        btnSearch.setOnClickListener(v -> {
            searchKeyword = etSearch.getText().toString().trim();
            filterRecipes();
        });

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

    private void setupCuisineCategories() {
        chipGroupCuisine.removeAllViews();

        chipGroupCuisine.setSingleSelection(true);
        chipGroupCuisine.setSelectionRequired(false);

        Chip recommendChip = createChip("🔥 推荐", 0);
        chipGroupCuisine.addView(recommendChip);

        for (Cuisine cuisine : cuisines) {
            if (cuisine.getId() == 1 && "全部".equals(cuisine.getName())) {
                continue;
            }
            Chip chip = createChip(cuisine.getName(), cuisine.getId());
            chipGroupCuisine.addView(chip);
        }

        Chip otherChip = createChip("📦 其他", -1);
        chipGroupCuisine.addView(otherChip);

        if (chipGroupCuisine.getChildCount() > 0) {
            ((Chip) chipGroupCuisine.getChildAt(0)).setChecked(true);
        }

        Log.d(TAG, "菜系列表已加载，共 " + cuisines.size() + " 个菜系");
    }

    @SuppressLint("ResourceType")
    private Chip createChip(String text, int cuisineId) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setFocusable(true);

        int chipColor = getCuisineColor(cuisineId);

        android.content.res.ColorStateList backgroundColor = new android.content.res.ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        chipColor,
                        ContextCompat.getColor(requireContext(), R.color.chip_unchecked_bg)
                }
        );

        chip.setChipBackgroundColor(backgroundColor);
        chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(chipColor));
        chip.setChipStrokeWidth(1.5f);

        chip.setTextColor(new android.content.res.ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        android.graphics.Color.WHITE,
                        chipColor
                }
        ));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        int marginPx = (int) (8 * getResources().getDisplayMetrics().density);
        lp.setMarginEnd(marginPx);
        chip.setLayoutParams(lp);

        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentCuisineId = cuisineId;
                Log.d(TAG, "选择菜系: " + text + ", ID: " + cuisineId);
                filterRecipes();
                Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
            }
        });

        return chip;
    }

    private int getCuisineColor(int cuisineId) {
        switch (cuisineId) {
            case 0: return ContextCompat.getColor(requireContext(), R.color.cuisine_all);
            case 1: return ContextCompat.getColor(requireContext(), R.color.cuisine_sichuan);
            case 2: return ContextCompat.getColor(requireContext(), R.color.cuisine_cantonese);
            case 3: return ContextCompat.getColor(requireContext(), R.color.cuisine_shandong);
            case 4: return ContextCompat.getColor(requireContext(), R.color.cuisine_jiangsu);
            case 5: return ContextCompat.getColor(requireContext(), R.color.cuisine_zhejiang);
            case 6: return ContextCompat.getColor(requireContext(), R.color.cuisine_fujian);
            case 7: return ContextCompat.getColor(requireContext(), R.color.cuisine_hunan);
            case 8: return ContextCompat.getColor(requireContext(), R.color.cuisine_anhui);
            case 9: return ContextCompat.getColor(requireContext(), R.color.cuisine_western);
            case 10: return ContextCompat.getColor(requireContext(), R.color.cuisine_japanese);
            case 11: return ContextCompat.getColor(requireContext(), R.color.cuisine_korean);
            case -1:
            default:
                return ContextCompat.getColor(requireContext(), R.color.cuisine_other);
        }
    }

    private void filterRecipes() {
        List<Recipe> filteredRecipes = new ArrayList<>();

        Log.d(TAG, "开始筛选 - 当前菜系ID: " + currentCuisineId + ", 总菜谱数: " + allRecipes.size());

        for (Recipe recipe : allRecipes) {
            if (hideDefaultRecipes && recipe.isDefault()) {
                continue;
            }

            if (currentCuisineId != 0) {
                int recipeCuisineId = recipe.getCuisineId();

                if (currentCuisineId == -1) {
                    if (recipeCuisineId > 0) {
                        continue;
                    }
                } else if (recipeCuisineId != currentCuisineId) {
                    continue;
                }
            }

            if (!searchKeyword.isEmpty()) {
                String title = recipe.getTitle() != null ? recipe.getTitle().toLowerCase() : "";
                String desc = recipe.getDescription() != null ? recipe.getDescription().toLowerCase() : "";
                if (!title.contains(searchKeyword.toLowerCase()) &&
                        !desc.contains(searchKeyword.toLowerCase())) {
                    continue;
                }
            }

            filteredRecipes.add(recipe);
        }

        if (showOnlyFavorites) {
            List<Recipe> favoriteRecipes = new ArrayList<>();
            for (Recipe recipe : filteredRecipes) {
                if (recipe.isFavorited()) {
                    favoriteRecipes.add(recipe);
                }
            }
            filteredRecipes = favoriteRecipes;
        }

        displayRecipes.clear();
        displayRecipes.addAll(filteredRecipes);
        adapter.updateRecipes(displayRecipes);

        if (displayRecipes.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            String msg;
            if (showOnlyFavorites) {
                msg = "暂无收藏菜谱\n点击❤️收藏喜欢的菜谱";
            } else if (!searchKeyword.isEmpty()) {
                msg = "未找到 \"" + searchKeyword + "\" 的相关菜谱";
            } else if (currentCuisineId == -1) {
                msg = "暂无其他菜谱\n敬请期待";
            } else if (currentCuisineId != 0) {
                String cuisineName = getCuisineNameById(currentCuisineId);
                msg = "暂无" + cuisineName + "菜谱\n敬请期待";
            } else {
                msg = hideDefaultRecipes ? "暂无自定义菜谱" : "暂无菜谱\n点击+添加新菜谱";
            }
            tvEmpty.setText(msg);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private String getCuisineNameById(int cuisineId) {
        if (cuisineId == -1) return "其他";
        for (Cuisine cuisine : cuisines) {
            if (cuisine.getId() == cuisineId) {
                return cuisine.getName();
            }
        }
        return "";
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecipeAdapter(requireContext(), displayRecipes, new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                showRecipeDetailWithSpeak(recipe);
            }

            @Override
            public void onFavoriteClick(Recipe recipe, int position) {
                toggleFavorite(recipe, position);
            }

            @Override
            public void onDeleteClick(Recipe recipe, int position) {
                if (!recipe.isDefault()) {
                    confirmDelete(recipe, position);
                } else {
                    Toast.makeText(getContext(), R.string.recipe_cannot_delete_default, Toast.LENGTH_SHORT).show();
                }
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        tabAll.setOnClickListener(v -> {
            showOnlyFavorites = false;
            updateTabStyle(true);
            filterRecipes();
        });

        tabFavorite.setOnClickListener(v -> {
            showOnlyFavorites = true;
            updateTabStyle(false);
            filterRecipes();
        });

        fabAdd.setOnClickListener(v -> showAddRecipeDialog());
    }

    private void updateTabStyle(boolean isAllSelected) {
        if (isAllSelected) {
            tabAll.setBackgroundResource(R.drawable.tab_selected);
            tabAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            tabFavorite.setBackgroundResource(R.drawable.tab_unselected);
            tabFavorite.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        } else {
            tabFavorite.setBackgroundResource(R.drawable.tab_selected);
            tabFavorite.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            tabAll.setBackgroundResource(R.drawable.tab_unselected);
            tabAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> fetchCuisinesAndRecipes());
    }

    private void loadToken() {
        SharedPreferences userPrefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE);
        String token = userPrefs.getString("token", "");
        if (token.isEmpty()) {
            SharedPreferences authPrefs = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE);
            token = authPrefs.getString("token", "");
        }
        authToken = "Bearer " + token;
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    private void loadHideDefaultSetting() {
        hideDefaultRecipes = layoutManager.isHideDefaultRecipes();
    }

    private void fetchCuisinesAndRecipes() {
        progressBar.setVisibility(View.VISIBLE);

        Call<CuisineResponse> cuisineCall = apiService.getCuisines(authToken);
        cuisineCall.enqueue(new Callback<CuisineResponse>() {
            @Override
            public void onResponse(@NonNull Call<CuisineResponse> call, @NonNull Response<CuisineResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    cuisines = response.body().getCuisines();
                    Log.d(TAG, "获取到菜系: " + cuisines.size() + " 个");
                    setupCuisineCategories();
                    fetchRecipes();
                } else {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(R.string.recipe_load_failed);
                }
            }

            @Override
            public void onFailure(@NonNull Call<CuisineResponse> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText(getString(R.string.recipe_network_error) + ": " + t.getMessage());
            }
        });
    }

    private void fetchRecipes() {
        Call<RecipesResponse> call = apiService.getRecipes(authToken);
        call.enqueue(new Callback<RecipesResponse>() {
            @Override
            public void onResponse(@NonNull Call<RecipesResponse> call, @NonNull Response<RecipesResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    allRecipes.clear();
                    List<Recipe> recipes = response.body().getRecipes();

                    for (Recipe recipe : recipes) {
                        Log.d(TAG, "菜谱: " + recipe.getTitle() + ", 菜系ID: " + recipe.getCuisineId());
                    }

                    allRecipes.addAll(recipes);
                    Log.d(TAG, "获取到 " + allRecipes.size() + " 个菜谱");
                    syncFavoriteStatus();
                } else {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(R.string.recipe_load_failed);
                }
            }

            @Override
            public void onFailure(@NonNull Call<RecipesResponse> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText(getString(R.string.recipe_network_error) + ": " + t.getMessage());
            }
        });
    }

    private void syncFavoriteStatus() {
        Call<FavoritesResponse> call = apiService.getFavoriteRecipes(authToken);
        call.enqueue(new Callback<FavoritesResponse>() {
            @Override
            public void onResponse(@NonNull Call<FavoritesResponse> call, @NonNull Response<FavoritesResponse> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Recipe> favorites = response.body().getFavorites();
                    Map<Integer, Boolean> favoriteIdMap = new HashMap<>();
                    if (favorites != null) {
                        for (Recipe fav : favorites) {
                            favoriteIdMap.put(fav.getId(), true);
                        }
                    }

                    for (Recipe recipe : allRecipes) {
                        recipe.setFavorited(favoriteIdMap.containsKey(recipe.getId()));
                    }

                    filterRecipes();
                } else {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(R.string.recipe_load_failed);
                    filterRecipes();
                }
            }

            @Override
            public void onFailure(@NonNull Call<FavoritesResponse> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText(getString(R.string.recipe_network_error) + ": " + t.getMessage());
                filterRecipes();
            }
        });
    }

    private void showRecipeDetail(Recipe recipe) {
        showRecipeDetailWithSpeak(recipe);
    }

    private void toggleFavorite(Recipe recipe, int position) {
        if (recipe.isFavoriteRequesting()) {
            Toast.makeText(getContext(), R.string.operation_in_progress, Toast.LENGTH_SHORT).show();
            return;
        }
        recipe.setFavoriteRequesting(true);

        boolean currentState = recipe.isFavorited();

        Call<ApiResponse> call = currentState
                ? apiService.unfavoriteRecipe(authToken, recipe.getId())
                : apiService.favoriteRecipe(authToken, recipe.getId());

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                recipe.setFavoriteRequesting(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    boolean newState = !currentState;
                    recipe.setFavorited(newState);

                    if (position >= 0 && position < displayRecipes.size()) {
                        displayRecipes.get(position).setFavorited(newState);
                    }

                    for (Recipe r : allRecipes) {
                        if (r.getId() == recipe.getId()) {
                            r.setFavorited(newState);
                            break;
                        }
                    }

                    adapter.updateRecipe(position, recipe);

                    Toast.makeText(getContext(), newState ? "已收藏" : "已取消收藏", Toast.LENGTH_SHORT).show();

                    if (showOnlyFavorites && !newState) {
                        filterRecipes();
                    }
                } else {
                    String errorMsg = response.body() != null ? response.body().getMessage() : getString(R.string.recipe_favorite_failed);
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                recipe.setFavoriteRequesting(false);
                Toast.makeText(getContext(), getString(R.string.recipe_network_error) + ": " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDelete(Recipe recipe, int position) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.recipe_delete_title)
                .setMessage(getString(R.string.recipe_confirm_delete, recipe.getTitle()))
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteRecipe(recipe, position))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteRecipe(Recipe recipe, int position) {
        Call<ApiResponse> call = apiService.deleteRecipe(authToken, recipe.getId());
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    displayRecipes.remove(position);
                    allRecipes.remove(recipe);
                    adapter.removeRecipe(position);
                    Toast.makeText(getContext(), R.string.recipe_delete_success, Toast.LENGTH_SHORT).show();
                    if (displayRecipes.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText(showOnlyFavorites ? R.string.recipe_empty_favorites : R.string.recipe_empty_list);
                        recyclerView.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(getContext(), R.string.recipe_delete_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), getString(R.string.recipe_network_error) + ": " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void showAddRecipeDialogPublic() {
        showAddRecipeDialog();
    }

    private void showAddRecipeDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_recipe_optimized, null);

        TextInputEditText etName = dialogView.findViewById(R.id.et_recipe_name);
        AutoCompleteTextView actvCuisine = dialogView.findViewById(R.id.actv_cuisine);
        TextInputEditText etCookTime = dialogView.findViewById(R.id.et_cook_time);
        AutoCompleteTextView actvDifficulty = dialogView.findViewById(R.id.actv_difficulty);
        AutoCompleteTextView actvIngredientInput = dialogView.findViewById(R.id.actv_ingredient_input);
        Button btnAddIngredient = dialogView.findViewById(R.id.btn_add_ingredient);
        ChipGroup chipGroupIngredients = dialogView.findViewById(R.id.chip_group_ingredients);
        RecyclerView rvSteps = dialogView.findViewById(R.id.rv_steps);
        Button btnAddStep = dialogView.findViewById(R.id.btn_add_step);
        Button btnSelectImage = dialogView.findViewById(R.id.btn_select_image);
        ivRecipePreview = dialogView.findViewById(R.id.iv_recipe_preview);
        tvImagePath = dialogView.findViewById(R.id.tv_image_path);

        List<String> cuisineNames = new ArrayList<>();
        for (Cuisine c : cuisines) {
            cuisineNames.add(c.getName());
        }
        ArrayAdapter<String> cuisineAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, cuisineNames);
        actvCuisine.setAdapter(cuisineAdapter);

        String[] difficultyLevels = {"简单", "中等", "困难"};
        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, difficultyLevels);
        actvDifficulty.setAdapter(difficultyAdapter);

        List<String> ingredientsList = new ArrayList<>();
        List<String> stepsList = new ArrayList<>();
        StepAdapter stepAdapter = new StepAdapter(stepsList);
        rvSteps.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSteps.setAdapter(stepAdapter);

        String[] commonIngredients = getResources().getStringArray(R.array.common_ingredients);
        ArrayAdapter<String> ingredientSuggestionAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, commonIngredients);
        actvIngredientInput.setAdapter(ingredientSuggestionAdapter);
        actvIngredientInput.setThreshold(1);

        btnAddIngredient.setOnClickListener(v -> {
            String ingredient = actvIngredientInput.getText().toString().trim();
            if (!ingredient.isEmpty()) {
                ingredientsList.add(ingredient);
                addIngredientChip(chipGroupIngredients, ingredient, ingredientsList);
                actvIngredientInput.setText("");
            }
        });

        btnAddStep.setOnClickListener(v -> showAddStepDialog(stepsList, stepAdapter));

        btnSelectImage.setOnClickListener(v -> selectImage());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.recipe_add_title)
                .setView(dialogView)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), R.string.recipe_name_hint, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String cookTime = etCookTime.getText() != null ? etCookTime.getText().toString().trim() : "";
                    String difficulty = actvDifficulty.getText().toString();

                    String difficultyEn = "medium";
                    if ("简单".equals(difficulty)) difficultyEn = "easy";
                    else if ("困难".equals(difficulty)) difficultyEn = "hard";

                    addRecipeOptimized(name, ingredientsList, stepsList,
                            cookTime.isEmpty() ? 0 : Integer.parseInt(cookTime),
                            difficultyEn);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void addIngredientChip(ChipGroup chipGroup, String ingredient, List<String> ingredientsList) {
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
        EditText input = new EditText(getContext());
        input.setHint("输入步骤，如：将土豆切块");
        input.setPadding(50, 20, 50, 20);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("添加步骤")
                .setView(input)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    String step = input.getText().toString().trim();
                    if (!step.isEmpty()) {
                        stepsList.add(step);
                        adapter.notifyItemInserted(stepsList.size() - 1);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void addRecipeOptimized(String name, List<String> ingredients,
                                    List<String> steps, int cookTime, String difficulty) {
        Map<String, Object> recipeData = new HashMap<>();
        recipeData.put("name", name);
        recipeData.put("description", "美味的" + name);
        recipeData.put("ingredients", ingredients);
        recipeData.put("steps", steps);
        recipeData.put("cook_time", cookTime);
        recipeData.put("difficulty", difficulty);

        Call<ApiResponse> call = apiService.addRecipe(authToken, recipeData);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(getContext(), R.string.recipe_add_success, Toast.LENGTH_SHORT).show();
                    fetchCuisinesAndRecipes();
                } else {
                    Toast.makeText(getContext(), R.string.recipe_add_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), getString(R.string.recipe_network_error) + ": " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private String getRealPathFromURI(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(columnIndex);
            cursor.close();
            return path;
        }
        return uri.getPath();
    }

    public void searchRecipe(String keyword) {
        etSearch.setText(keyword);
        searchKeyword = keyword;
        filterRecipes();
    }

    public void favoriteCurrentRecipe() {
        Recipe selectedRecipe = adapter.getSelectedRecipe();
        if (selectedRecipe == null) {
            Toast.makeText(getContext(), "请先点击选择一个菜谱", Toast.LENGTH_SHORT).show();
            return;
        }
        int position = adapter.getSelectedPosition();
        if (position >= 0 && position < displayRecipes.size()) {
            toggleFavorite(selectedRecipe, position);
        }
    }

    public void showRecipeStepsByName(String recipeName) {
        for (Recipe recipe : allRecipes) {
            if (recipe.getTitle().toLowerCase().contains(recipeName.toLowerCase())) {
                if (hideDefaultRecipes && recipe.isDefault()) {
                    Toast.makeText(getContext(), R.string.system_default_recipe, Toast.LENGTH_SHORT).show();
                    return;
                }
                showRecipeDetailWithSpeak(recipe);
                return;
            }
        }
        Toast.makeText(getContext(), "未找到菜谱: " + recipeName, Toast.LENGTH_SHORT).show();
    }

    public void toggleHideDefaultRecipes() {
        hideDefaultRecipes = !hideDefaultRecipes;
        filterRecipes();
        Toast.makeText(getContext(), hideDefaultRecipes ? "已隐藏预设菜谱" : "已显示预设菜谱", Toast.LENGTH_SHORT).show();
    }

    public boolean isHideDefaultRecipes() {
        return hideDefaultRecipes;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ttsHelper != null) {
            ttsHelper.stop();
        }
    }
}