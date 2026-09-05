package com.smxy.myapplication.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.smxy.myapplication.R;
import com.smxy.myapplication.fragment.BalanceFragment;
import com.smxy.myapplication.fragment.LedgerManageFragment;
import com.smxy.myapplication.fragment.MyRecipesFragment;
import com.smxy.myapplication.fragment.NoteListFragment;
import com.smxy.myapplication.fragment.RecipeFragment;
import com.smxy.myapplication.fragment.ScheduleListFragment;
import com.smxy.myapplication.fragment.SelfSelectedFragment;
import com.smxy.myapplication.fragment.SettingsFragment;
import com.smxy.myapplication.activity.ComponentManageActivity;
import com.smxy.myapplication.fragment.StatisticsFragment;
import com.smxy.myapplication.fragment.RecordsFragment;
import com.smxy.myapplication.fragment.VoiceInputFragment;
import com.smxy.myapplication.fragment.MusicPlayerFragment;
import com.smxy.myapplication.manager.SettingsManager;
import com.smxy.myapplication.manager.VoiceCommandParser;
import com.smxy.myapplication.manager.VoiceFloatManager;
import com.smxy.myapplication.utils.ErrorHandler;

public class MainContainerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainContainerActivity";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private TextView navUserName;
    private FloatingActionButton fabVoice;
    private Toolbar toolbar;
    private ActionBarDrawerToggle toggle;
    private SettingsManager settingsManager;
    private VoiceFloatManager voiceFloatManager;

    public static final int NAV_SELF_SELECTED = R.id.nav_self_selected;
    public static final int NAV_MUSIC = R.id.nav_music;

    private final ActivityResultLauncher<Intent> overlayPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(this)) {
                        showVoiceFloatBall();
                        ErrorHandler.showToast(this, "悬浮窗权限已获取");
                    } else {
                        ErrorHandler.showToast(this, "需要悬浮窗权限才能使用语音助手功能");
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        settingsManager = new SettingsManager(this);

        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
        String username = getIntent().getStringExtra("username");
        if (username == null) {
            username = prefs.getString("last_username", "用户");
        }

        initViews();
        setupToolbar();
        setupNavigationView(username);
        setupBottomNavigation();
        setupVoiceButton();
        checkFloatBallStatus();

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            boolean canGoBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(canGoBack);
            }
            if (toggle != null) {
                toggle.setDrawerIndicatorEnabled(!canGoBack);
            }
        });

        if (savedInstanceState == null) {
            loadFragment(new BalanceFragment(), false);
        }
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fabVoice = findViewById(R.id.fab_voice);
        toolbar = findViewById(R.id.toolbar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);

        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.open_drawer, R.string.close_drawer);
        if (drawerLayout != null) {
            drawerLayout.addDrawerListener(toggle);
        }
        toggle.syncState();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return true;
        }
        return super.onSupportNavigateUp();
    }

    private void setupNavigationView(String username) {
        if (navigationView != null && navigationView.getHeaderView(0) != null) {
            navUserName = navigationView.getHeaderView(0).findViewById(R.id.nav_user_name);
            if (navUserName != null) {
                navUserName.setText(String.format(getString(R.string.welcome_user),
                        ErrorHandler.getSafeString(username, "用户")));
            }
        }
        refreshNavigationMenu();
    }

    public void refreshNavigationMenu() {
        if (navigationView == null || settingsManager == null) return;

        refreshNavigationModeUI();

        Menu menu = navigationView.getMenu();
        menu.clear();

        if (settingsManager.showAccounting()) {
            menu.add(1, R.id.nav_accounting, 1, getString(R.string.module_accounting));
        }

        if (settingsManager.showLedgerManage()) {
            menu.add(1, R.id.nav_ledger_manage, 2, getString(R.string.module_ledger_manage));
        }

        if (settingsManager.showStatistics()) {
            menu.add(1, R.id.nav_statistics, 3, getString(R.string.module_statistics));
        }
        if (settingsManager.showRecords()) {
            menu.add(1, R.id.nav_records, 4, getString(R.string.module_records));
        }
        if (settingsManager.showNotes()) {
            menu.add(1, R.id.nav_notes, 5, getString(R.string.module_notes));
        }

        if (settingsManager.showVoice()) {
            menu.add(1, R.id.nav_voice, 7, getString(R.string.module_voice));
        }
        if (settingsManager.showRecipe()) {
            menu.add(1, R.id.nav_community, 8, getString(R.string.module_recipe));
        }
        if (settingsManager.showMyRecipes()) {
            menu.add(1, R.id.nav_my_recipes, 9, getString(R.string.module_my_recipes));
        }
        if (settingsManager.showSchedule()) {
            menu.add(1, R.id.nav_schedule, 10, getString(R.string.module_schedule));
        }
        if (settingsManager.showMusic()) {
            menu.add(1, R.id.nav_music, 11, getString(R.string.module_music));
        }
        if (settingsManager.showStock()) {
            menu.add(1, R.id.nav_self_selected, 12, getString(R.string.module_stock));
        }

        menu.add(2, R.id.nav_settings, 13, getString(R.string.module_settings));

        if (settingsManager.showComponentSettings()) {
            menu.add(3, R.id.nav_component_settings, 14, getString(R.string.module_component_settings));
        }
        if (settingsManager.showExport()) {
            menu.add(3, R.id.nav_export, 16, getString(R.string.module_export));
        }

        menu.add(4, R.id.nav_about, 17, getString(R.string.module_about));
        menu.add(4, R.id.nav_logout, 18, getString(R.string.module_logout));

        navigationView.setNavigationItemSelectedListener(this);
    }

    public void refreshNavigationModeUI() {
        if (settingsManager == null) return;

        boolean sidebarEnabled = settingsManager.isSidebarEnabled();
        boolean bottomNavEnabled = settingsManager.isBottomNavEnabled();

        if (navigationView != null) {
            navigationView.setVisibility(sidebarEnabled ? View.VISIBLE : View.GONE);
        }

        if (toggle != null) {
            toggle.setDrawerIndicatorEnabled(sidebarEnabled);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(sidebarEnabled);
            getSupportActionBar().setHomeButtonEnabled(sidebarEnabled);
        }

        View bottomNavScroll = findViewById(R.id.bottom_nav_scroll);
        if (bottomNavScroll != null) {
            bottomNavScroll.setVisibility(bottomNavEnabled ? View.VISIBLE : View.GONE);
        }
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(bottomNavEnabled ? View.VISIBLE : View.GONE);
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) return;

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }

            int id = item.getItemId();
            if (id == R.id.navigation_accounting) {
                loadFragment(new BalanceFragment(), false);
                setTitle(getString(R.string.title_accounting));
            } else if (id == R.id.navigation_statistics) {
                loadFragment(new StatisticsFragment(), false);
                setTitle(getString(R.string.title_statistics));
            } else if (id == R.id.navigation_community) {
                loadFragment(new RecipeFragment(), false);
                setTitle(getString(R.string.title_community));
            } else if (id == R.id.navigation_schedule) {
                loadFragment(new ScheduleListFragment(), false);
                setTitle(getString(R.string.title_schedule));
            } else if (id == R.id.navigation_profile) {
                loadFragment(new SettingsFragment(), false);
                setTitle(getString(R.string.title_settings));
            }

            if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        });
    }

    private void setupVoiceButton() {
        boolean showVoiceBtn = getSharedPreferences("voice_settings", MODE_PRIVATE)
                .getBoolean("show_main_voice_btn", true);
        if (fabVoice != null) {
            fabVoice.setVisibility(showVoiceBtn ? View.VISIBLE : View.GONE);
            fabVoice.setOnClickListener(v -> {
                loadFragment(new VoiceInputFragment(), true);
                setTitle(getString(R.string.title_voice));
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
            });
        }
    }

    private void checkFloatBallStatus() {
        boolean showFloatBall = getSharedPreferences("voice_settings", MODE_PRIVATE)
                .getBoolean("show_float_ball", false);
        if (showFloatBall) {
            if (Settings.canDrawOverlays(this)) {
                showVoiceFloatBall();
            } else {
                requestOverlayPermission();
            }
        }
    }

    public void loadHomeFragment() {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        loadFragment(new BalanceFragment(), false);
        setTitle(getString(R.string.title_accounting));
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void requestOverlayPermission() {
        ErrorHandler.showToast(this, "请授予悬浮窗权限以使用语音助手");
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        overlayPermissionLauncher.launch(intent);
    }

    public void showVoiceFloatBall() {
        if (voiceFloatManager == null) {
            voiceFloatManager = new VoiceFloatManager(this);
            voiceFloatManager.setOnVoiceRecordListener(new VoiceFloatManager.OnVoiceRecordListener() {
                @Override
                public void onRecordStart() {
                    Log.d(TAG, "开始录音");
                }

                @Override
                public void onRecordEnd(String recognizedText) {
                    parseAndFillForm(recognizedText);
                }

                @Override
                public void onRecordError(String error) {
                    if ("open_voice_fragment".equals(error)) {
                        loadFragment(new VoiceInputFragment(), true);
                        setTitle(getString(R.string.title_voice));
                    } else {
                        ErrorHandler.showToast(MainContainerActivity.this, "识别失败: " + error);
                    }
                }
            });
        }
        voiceFloatManager.show();
    }

    public void hideVoiceFloatBall() {
        if (voiceFloatManager != null) {
            voiceFloatManager.hide();
            voiceFloatManager = null;
        }
    }

    private void parseAndFillForm(String recognizedText) {
        VoiceCommandParser.ParseResult result = VoiceCommandParser.parse(recognizedText);
        boolean hasContent = result.amount > 0 || (result.note != null && !result.note.isEmpty());

        if (hasContent) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (fragment instanceof BalanceFragment) {
                BalanceFragment balanceFragment = (BalanceFragment) fragment;
                if (result.amount > 0) {
                    balanceFragment.fillForm(result.amount, result.category, result.type, result.note);
                } else {
                    balanceFragment.fillNoteOnly(result.note);
                }
                ErrorHandler.showToast(this, "已添加备注: " + result.note);
            } else {
                loadFragment(new BalanceFragment(), false);
                setTitle(getString(R.string.title_accounting));
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Fragment newFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (newFragment instanceof BalanceFragment) {
                        BalanceFragment balanceFragment = (BalanceFragment) newFragment;
                        if (result.amount > 0) {
                            balanceFragment.fillForm(result.amount, result.category, result.type, result.note);
                        } else {
                            balanceFragment.fillNoteOnly(result.note);
                        }
                    }
                }, 500);
            }
        } else {
            ErrorHandler.showToast(this, "未能识别到有效信息");
        }
    }

    public void searchRecipe(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            ErrorHandler.showToast(this, getString(R.string.hint_search_keyword));
            return;
        }
        navigateToFragmentAndExecute(RecipeFragment.class, getString(R.string.title_community),
                fragment -> ((RecipeFragment) fragment).searchRecipe(keyword));
    }

    public void favoriteCurrentRecipe() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof RecipeFragment) {
            ((RecipeFragment) fragment).favoriteCurrentRecipe();
        } else {
            ErrorHandler.showToast(this, getString(R.string.hint_enter_recipe_page));
        }
    }

    public void openAddRecipe() {
        navigateToFragmentAndExecute(RecipeFragment.class, getString(R.string.title_community),
                fragment -> ((RecipeFragment) fragment).showAddRecipeDialogPublic());
    }

    public void showRecipeSteps(String recipeName) {
        if (recipeName == null || recipeName.isEmpty()) {
            ErrorHandler.showToast(this, getString(R.string.hint_enter_recipe_name));
            return;
        }
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof RecipeFragment) {
            ((RecipeFragment) fragment).showRecipeStepsByName(recipeName);
        } else {
            ErrorHandler.showToast(this, getString(R.string.hint_enter_recipe_page));
        }
    }

    private void navigateToFragmentAndExecute(Class<? extends Fragment> fragmentClass,
                                              String title,
                                              FragmentAction action) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragmentClass.isInstance(fragment)) {
            action.execute(fragment);
        } else {
            try {
                Fragment newFragment = fragmentClass.newInstance();
                loadFragment(newFragment, true);
                setTitle(title);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (fragmentClass.isInstance(currentFragment)) {
                        action.execute(currentFragment);
                    }
                }, 500);
            } catch (Exception e) {
                ErrorHandler.showToast(this, "加载页面失败");
            }
        }
    }

    private interface FragmentAction {
        void execute(Fragment fragment);
    }

    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        if (fragment == null) return;

        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        if (id == R.id.nav_accounting) {
            loadFragment(new BalanceFragment(), false);
            setTitle(getString(R.string.title_accounting));
        } else if (id == R.id.nav_ledger_manage) {
            loadFragment(new LedgerManageFragment(), false);
            setTitle(getString(R.string.title_ledger_manage));
        } else if (id == R.id.nav_statistics) {
            loadFragment(new StatisticsFragment(), false);
            setTitle(getString(R.string.title_statistics));
        } else if (id == R.id.nav_records) {
            loadFragment(new RecordsFragment(), false);
            setTitle(getString(R.string.title_records));
        } else if (id == R.id.nav_notes) {
            loadFragment(new NoteListFragment(), false);
            setTitle(getString(R.string.title_notes));
        } else if (id == R.id.nav_self_selected) {
            loadFragment(new SelfSelectedFragment(), false);
            setTitle(getString(R.string.title_self_selected));
        } else if (id == R.id.nav_community) {
            loadFragment(new RecipeFragment(), false);
            setTitle(getString(R.string.title_community));
        } else if (id == R.id.nav_my_recipes) {
            loadFragment(new MyRecipesFragment(), false);
            setTitle(getString(R.string.title_my_recipes));
        } else if (id == R.id.nav_schedule) {
            loadFragment(new ScheduleListFragment(), false);
            setTitle(getString(R.string.title_schedule));
        } else if (id == R.id.nav_music) {
            loadFragment(new MusicPlayerFragment(), false);
            setTitle(getString(R.string.module_music));
        } else if (id == R.id.nav_voice) {
            loadFragment(new VoiceInputFragment(), false);
            setTitle(getString(R.string.title_voice));
        } else if (id == R.id.nav_settings) {
            loadFragment(new SettingsFragment(), false);
            setTitle(getString(R.string.title_settings));
        } else if (id == R.id.nav_component_settings) {
            startActivity(new Intent(this, ComponentManageActivity.class));
        } else if (id == R.id.nav_export) {
            handleExport();
        } else if (id == R.id.nav_about) {
            showAboutDialog();
        } else if (id == R.id.nav_logout) {
            logout();
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    private void handleExport() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof RecordsFragment) {
            ((RecordsFragment) fragment).exportData();
        } else {
            loadFragment(new RecordsFragment(), false);
            setTitle("记录列表");
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Fragment newFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (newFragment instanceof RecordsFragment) {
                    ((RecordsFragment) newFragment).exportData();
                }
            }, 500);
        }
    }

    private void showAboutDialog() {
        if (!ErrorHandler.isContextValid(this)) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_about, null);
        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.confirm, null)
                .show();
    }

    private void logout() {
        getSharedPreferences("user_data", MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences("auth", MODE_PRIVATE).edit().clear().apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void refreshVoiceButtonVisibility() {
        boolean showVoiceBtn = getSharedPreferences("voice_settings", MODE_PRIVATE)
                .getBoolean("show_main_voice_btn", true);
        if (fabVoice != null) {
            fabVoice.setVisibility(showVoiceBtn ? View.VISIBLE : View.GONE);
        }
        refreshVoiceFloatBall();
    }

    public void refreshVoiceFloatBall() {
        boolean showFloatBall = getSharedPreferences("voice_settings", MODE_PRIVATE)
                .getBoolean("show_float_ball", false);
        if (showFloatBall) {
            if (Settings.canDrawOverlays(this)) {
                showVoiceFloatBall();
            } else {
                hideVoiceFloatBall();
            }
        } else {
            hideVoiceFloatBall();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceFloatManager != null) {
            voiceFloatManager.hide();
            voiceFloatManager = null;
        }
    }
}