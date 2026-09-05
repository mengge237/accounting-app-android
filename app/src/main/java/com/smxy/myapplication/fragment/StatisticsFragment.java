package com.smxy.myapplication.fragment;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.smxy.myapplication.R;
import com.smxy.myapplication.adapter.CategoryDetailAdapter;
import com.smxy.myapplication.model.AccountType;
import com.smxy.myapplication.model.LedgerBook;
import com.smxy.myapplication.model.Record;
import com.smxy.myapplication.network.ApiService;
import com.smxy.myapplication.network.CategoryStatisticsResponse;
import com.smxy.myapplication.network.LedgerResponse;
import com.smxy.myapplication.network.RecordsResponse;
import com.smxy.myapplication.network.RetrofitClient;
import com.smxy.myapplication.network.StatisticsResponse;
import com.smxy.myapplication.network.TypesResponse;
import com.smxy.myapplication.utils.ErrorHandler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatisticsFragment extends Fragment {

    private static final String PREFS_NAME = "user_data";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_CURRENT_LEDGER_ID = "current_ledger_id";
    private static final String KEY_CURRENT_LEDGER_NAME = "current_ledger_name";

    private ApiService apiService;
    private String authToken;
    private List<AccountType> expenseTypes = new ArrayList<>();
    private List<Record> allRecords = new ArrayList<>();

    // 账本相关
    private TextView tvCurrentLedger;
    private MaterialCardView ledgerSelectorCard;
    private int currentLedgerId = 1;
    private String currentLedgerName = "默认账本";
    private List<LedgerBook> ledgerList = new ArrayList<>();

    // UI组件
    private TextView tvStartDate, tvEndDate;
    private Button btnDateRange;
    private Button btnThisMonth, btnLastMonth, btnThisYear;
    private ChipGroup chipGroupChart;
    private LinearLayout layoutTabMode;
    private LinearLayout layoutCircleMode;
    private FrameLayout fragmentContainerBar;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private PieChart pieChartCircle;
    private RecyclerView recyclerCategory;
    private TextView tvTotalIncome, tvTotalExpense, tvBalance;

    private Calendar startCalendar, endCalendar;
    private SimpleDateFormat dateFormat;
    private int currentChartType = 0; // 0=饼图, 1=收支条形图, 2=分类条形图

    private double totalIncome = 0;
    private double totalExpense = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        initViews(view);
        initApiService();
        loadToken();
        loadCurrentLedger();

        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

        initDateRange();
        setupDatePicker();
        setupLedgerSelector();

        loadLedgers();
        loadRecordsFirst();

        return view;
    }

    private void loadCurrentLedger() {
        Context context = getContext();
        if (ErrorHandler.isContextValid(context)) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            currentLedgerId = prefs.getInt(KEY_CURRENT_LEDGER_ID, 1);
            currentLedgerName = prefs.getString(KEY_CURRENT_LEDGER_NAME, "默认账本");
        }
    }

    private void saveCurrentLedger(int ledgerId, String ledgerName) {
        Context context = getContext();
        if (ErrorHandler.isContextValid(context)) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putInt(KEY_CURRENT_LEDGER_ID, ledgerId)
                    .putString(KEY_CURRENT_LEDGER_NAME, ledgerName)
                    .apply();
        }
        currentLedgerId = ledgerId;
        currentLedgerName = ledgerName;
        updateLedgerDisplay();
        refreshData();
    }

    private void updateLedgerDisplay() {
        if (tvCurrentLedger != null) {
            tvCurrentLedger.setText(currentLedgerName);
        }
    }

    private void loadLedgers() {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (apiService == null) return;
        if (!ErrorHandler.isTokenValid(authToken)) {
            return;
        }

        String authHeader = "Bearer " + authToken;
        Call<LedgerResponse> call = apiService.getLedgers(authHeader);
        call.enqueue(new ErrorHandler.SafeCallback<LedgerResponse>(this) {
            @Override
            protected void onSuccess(LedgerResponse data) {
                if (data.getLedgers() != null) {
                    ledgerList.clear();
                    ledgerList.addAll(data.getLedgers());
                }
                updateLedgerDisplay();
            }

            @Override
            protected void onError(String message) {
                Log.w("StatisticsFragment", "加载账本列表失败: " + message);
            }
        });
    }

    private void showLedgerSelector() {
        if (ledgerList.isEmpty()) {
            ErrorHandler.showToast(getContext(), "暂无其他账本");
            loadLedgers();
            return;
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_ledger_selector, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_ledgers);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        LedgerSelectorAdapter adapter = new LedgerSelectorAdapter(ledgerList, currentLedgerId, ledger -> {
            if (ledger.getId() != currentLedgerId) {
                saveCurrentLedger(ledger.getId(), ledger.getName());
                ErrorHandler.showToast(getContext(), "已切换到: " + ledger.getName());
            }
        });
        recyclerView.setAdapter(adapter);

        builder.setView(dialogView)
                .setNegativeButton("取消", null)
                .show();
    }

    private void setupLedgerSelector() {
        if (ledgerSelectorCard != null) {
            ledgerSelectorCard.setOnClickListener(v -> showLedgerSelector());
        }
        if (tvCurrentLedger != null) {
            tvCurrentLedger.setOnClickListener(v -> showLedgerSelector());
        }
        updateLedgerDisplay();
    }

    private void initViews(View view) {
        // 账本选择器
        tvCurrentLedger = view.findViewById(R.id.tv_current_ledger);
        ledgerSelectorCard = view.findViewById(R.id.ledger_selector_card);

        tvStartDate = view.findViewById(R.id.tv_start_date);
        tvEndDate = view.findViewById(R.id.tv_end_date);
        btnDateRange = view.findViewById(R.id.btn_date_range);
        btnThisMonth = view.findViewById(R.id.btn_this_month);
        btnLastMonth = view.findViewById(R.id.btn_last_month);
        btnThisYear = view.findViewById(R.id.btn_this_year);
        chipGroupChart = view.findViewById(R.id.chip_group_chart);
        layoutTabMode = view.findViewById(R.id.layout_tab_mode);
        layoutCircleMode = view.findViewById(R.id.layout_circle_mode);
        fragmentContainerBar = view.findViewById(R.id.fragment_container_bar);
        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);
        pieChartCircle = view.findViewById(R.id.pie_chart_circle);
        recyclerCategory = view.findViewById(R.id.recycler_category);
        tvTotalIncome = view.findViewById(R.id.tv_total_income);

        tvTotalExpense = view.findViewById(R.id.tv_total_expense);
        tvBalance = view.findViewById(R.id.tv_balance);

        if (layoutTabMode != null) layoutTabMode.setVisibility(View.VISIBLE);
        if (layoutCircleMode != null) layoutCircleMode.setVisibility(View.GONE);

        setupChartTypeSelector(view);

        setupCollapsibleCards(view);
    }

    private void setupChartTypeSelector(View view) {
        if (chipGroupChart != null) {
            chipGroupChart.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chip_pie_chart) {
                    currentChartType = 0;
                    showPieChart();
                } else if (checkedId == R.id.chip_bar_income_expense) {
                    currentChartType = 1;
                    showIncomeExpenseBar();
                } else if (checkedId == R.id.chip_bar_category) {
                    currentChartType = 2;
                    showCategoryBar();
                }
            });
        }
    }

    private void showPieChart() {
        if (layoutTabMode != null) layoutTabMode.setVisibility(View.VISIBLE);
        if (layoutCircleMode != null) layoutCircleMode.setVisibility(View.GONE);
        if (fragmentContainerBar != null) fragmentContainerBar.setVisibility(View.GONE);
        showTabMode();
    }

    private void showIncomeExpenseBar() {
        if (layoutTabMode != null) layoutTabMode.setVisibility(View.GONE);
        if (layoutCircleMode != null) layoutCircleMode.setVisibility(View.GONE);
        if (fragmentContainerBar != null) fragmentContainerBar.setVisibility(View.VISIBLE);

        getChildFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_bar, IncomeExpenseBarFragment.newInstance(totalIncome, totalExpense))
                .commit();
    }

    private void showCategoryBar() {
        if (layoutTabMode != null) layoutTabMode.setVisibility(View.GONE);
        if (layoutCircleMode != null) layoutCircleMode.setVisibility(View.GONE);
        if (fragmentContainerBar != null) fragmentContainerBar.setVisibility(View.VISIBLE);

        if (expenseTypes == null) expenseTypes = new ArrayList<>();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_bar, CategoryBarFragment.newInstance(expenseTypes))
                .commit();
    }

    private void setupCollapsibleCards(View view) {
        View layoutDateHeader = view.findViewById(R.id.layout_date_header);
        View layoutDateContent = view.findViewById(R.id.layout_date_content);
        TextView tvDateArrow = view.findViewById(R.id.tv_date_arrow);

        View layoutStatsHeader = view.findViewById(R.id.layout_stats_header);
        View layoutStatsContent = view.findViewById(R.id.layout_stats_content);
        TextView tvStatsArrow = view.findViewById(R.id.tv_stats_arrow);

        final boolean[] dateExpanded = {true};
        final boolean[] statsExpanded = {true};

        if (layoutDateHeader != null && layoutDateContent != null && tvDateArrow != null) {
            layoutDateHeader.setOnClickListener(v -> {
                if (dateExpanded[0]) {
                    layoutDateContent.setVisibility(View.GONE);
                    tvDateArrow.setText(R.string.arrow_right);
                    dateExpanded[0] = false;
                } else {
                    layoutDateContent.setVisibility(View.VISIBLE);
                    tvDateArrow.setText(R.string.arrow_down);
                    dateExpanded[0] = true;
                }
            });
        }

        if (layoutStatsHeader != null && layoutStatsContent != null && tvStatsArrow != null) {
            layoutStatsHeader.setOnClickListener(v -> {
                if (statsExpanded[0]) {
                    layoutStatsContent.setVisibility(View.GONE);
                    tvStatsArrow.setText(R.string.arrow_right);
                    statsExpanded[0] = false;
                } else {
                    layoutStatsContent.setVisibility(View.VISIBLE);
                    tvStatsArrow.setText(R.string.arrow_down);
                    statsExpanded[0] = true;
                }
            });
        }
    }

    private void initApiService() {
        apiService = RetrofitClient.getApiService();
    }

    private void loadToken() {
        Context context = getContext();
        if (ErrorHandler.isContextValid(context)) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            authToken = prefs.getString(KEY_TOKEN, "");
        }
    }

    private void initDateRange() {
        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();
        startCalendar.setTimeInMillis(System.currentTimeMillis());
        endCalendar.setTimeInMillis(System.currentTimeMillis());
        updateDateDisplay();
    }

    private void updateDateDisplay() {
        if (tvStartDate != null && tvEndDate != null && dateFormat != null && startCalendar != null && endCalendar != null) {
            tvStartDate.setText(dateFormat.format(startCalendar.getTime()));
            tvEndDate.setText(dateFormat.format(endCalendar.getTime()));
        }
    }

    private void setupDatePicker() {
        if (btnDateRange != null) {
            btnDateRange.setOnClickListener(v -> showDateRangePicker());
        }

        if (btnThisMonth != null) {
            btnThisMonth.setOnClickListener(v -> {
                setThisMonth();
                refreshData();
            });
        }

        if (btnLastMonth != null) {
            btnLastMonth.setOnClickListener(v -> {
                setLastMonth();
                refreshData();
            });
        }

        if (btnThisYear != null) {
            btnThisYear.setOnClickListener(v -> {
                setThisYear();
                refreshData();
            });
        }
    }

    private void showDateRangePicker() {
        if (!ErrorHandler.isContextValid(getContext())) return;

        DatePickerDialog startDialog = new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> {
                    startCalendar.set(year, month, dayOfMonth);
                    if (startCalendar.after(endCalendar)) {
                        endCalendar.set(year, month, dayOfMonth);
                    }
                    updateDateDisplay();

                    DatePickerDialog endDialog = new DatePickerDialog(getContext(),
                            (view2, year2, month2, dayOfMonth2) -> {
                                Calendar temp = Calendar.getInstance();
                                temp.set(year2, month2, dayOfMonth2);
                                if (temp.after(startCalendar)) {
                                    endCalendar.set(year2, month2, dayOfMonth2);
                                } else {
                                    Toast.makeText(getContext(), R.string.date_end_before_start, Toast.LENGTH_SHORT).show();
                                }
                                updateDateDisplay();
                                refreshData();
                            },
                            endCalendar.get(Calendar.YEAR),
                            endCalendar.get(Calendar.MONTH),
                            endCalendar.get(Calendar.DAY_OF_MONTH));
                    endDialog.show();
                },
                startCalendar.get(Calendar.YEAR),
                startCalendar.get(Calendar.MONTH),
                startCalendar.get(Calendar.DAY_OF_MONTH));
        startDialog.show();
    }

    private void setThisMonth() {
        Calendar now = Calendar.getInstance();
        startCalendar.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 1);
        endCalendar.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.getActualMaximum(Calendar.DAY_OF_MONTH));
        updateDateDisplay();
    }

    private void setLastMonth() {
        Calendar temp = Calendar.getInstance();
        temp.setTimeInMillis(startCalendar.getTimeInMillis());
        temp.add(Calendar.MONTH, -1);
        startCalendar.set(temp.get(Calendar.YEAR), temp.get(Calendar.MONTH), 1);
        endCalendar.set(temp.get(Calendar.YEAR), temp.get(Calendar.MONTH), temp.getActualMaximum(Calendar.DAY_OF_MONTH));
        updateDateDisplay();
    }

    private void setThisYear() {
        Calendar now = Calendar.getInstance();
        startCalendar.set(now.get(Calendar.YEAR), 0, 1);
        endCalendar.set(now.get(Calendar.YEAR), 11, 31);
        updateDateDisplay();
    }

    private void loadRecordsFirst() {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (apiService == null) return;
        if (!ErrorHandler.isTokenValid(authToken)) {
            refreshData();
            return;
        }

        String authHeader = "Bearer " + authToken;
        Call<RecordsResponse> call = apiService.getRecords(authHeader, currentLedgerId);
        call.enqueue(new ErrorHandler.SafeCallback<RecordsResponse>(this) {
            @Override
            protected void onSuccess(RecordsResponse data) {
                allRecords = data.getRecords();
                if (allRecords != null && !allRecords.isEmpty()) {
                    long minDate = Long.MAX_VALUE;
                    long maxDate = Long.MIN_VALUE;

                    for (Record record : allRecords) {
                        if (record == null) continue;
                        long timestamp = record.getTimestamp();
                        if (timestamp < minDate) minDate = timestamp;
                        if (timestamp > maxDate) maxDate = timestamp;
                    }

                    if (minDate != Long.MAX_VALUE && startCalendar != null && endCalendar != null) {
                        startCalendar.setTimeInMillis(minDate);
                        endCalendar.setTimeInMillis(maxDate);
                        updateDateDisplay();
                    }
                }
                refreshData();
            }

            @Override
            protected void onError(String message) {
                refreshData();
            }
        });
    }

    public void refreshData() {
        loadStatisticsData();
    }

    private void loadStatisticsData() {
        if (!ErrorHandler.isFragmentValid(this)) return;
        loadStatistics();
        loadCategoryStatistics();
        loadAccountTypes();
    }

    private void loadStatistics() {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (apiService == null) return;
        if (!ErrorHandler.isTokenValid(authToken)) return;
        if (startCalendar == null || endCalendar == null || dateFormat == null) return;

        String startDate = dateFormat.format(startCalendar.getTime());
        String endDate = dateFormat.format(endCalendar.getTime());
        String authHeader = "Bearer " + authToken;

        Call<StatisticsResponse> call = apiService.getStatistics(authHeader, startDate, endDate, currentLedgerId);
        call.enqueue(new ErrorHandler.SafeCallback<StatisticsResponse>(this) {
            @Override
            protected void onSuccess(StatisticsResponse data) {
                StatisticsResponse.StatisticsData stats = data.getStatistics();
                if (stats != null) {
                    totalIncome = stats.getTotal_income();
                    totalExpense = stats.getTotal_expense();
                    updateStatisticsDisplay();
                }
            }

            @Override
            protected void onError(String message) {
                Log.e("StatisticsFragment", "加载统计失败: " + message);
            }
        });
    }

    private void loadCategoryStatistics() {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (apiService == null) return;
        if (!ErrorHandler.isTokenValid(authToken)) return;
        if (startCalendar == null || endCalendar == null || dateFormat == null) return;

        String startDate = dateFormat.format(startCalendar.getTime());
        String endDate = dateFormat.format(endCalendar.getTime());
        String authHeader = "Bearer " + authToken;

        Call<CategoryStatisticsResponse> expenseCall = apiService.getCategoryStatistics(authHeader, startDate, endDate, "expense", currentLedgerId);
        expenseCall.enqueue(new ErrorHandler.SafeCallback<CategoryStatisticsResponse>(this) {
            @Override
            protected void onSuccess(CategoryStatisticsResponse data) {
                expenseTypes = data.getCategories();
                if (expenseTypes == null) expenseTypes = new ArrayList<>();

                refreshCurrentChart();
            }

            @Override
            protected void onError(String message) {
                Log.e("StatisticsFragment", "加载分类统计失败: " + message);
            }
        });
    }

    private void loadAccountTypes() {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (apiService == null) return;
        if (!ErrorHandler.isTokenValid(authToken)) return;

        String authHeader = "Bearer " + authToken;
        Call<TypesResponse> call = apiService.getAccountTypes(authHeader);
        call.enqueue(new ErrorHandler.SafeCallback<TypesResponse>(this) {
            @Override
            protected void onSuccess(TypesResponse data) {
                // 成功，不需要额外处理
            }

            @Override
            protected void onError(String message) {
                Log.e("StatisticsFragment", "加载账户类型失败: " + message);
            }
        });
    }

    private void updateStatisticsDisplay() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        double balance = totalIncome - totalExpense;

        if (tvTotalIncome != null) {
            tvTotalIncome.setText(String.format(Locale.CHINA, "¥%.2f", totalIncome));
        }
        if (tvTotalExpense != null) {
            tvTotalExpense.setText(String.format(Locale.CHINA, "¥%.2f", totalExpense));
        }
        if (tvBalance != null) {
            tvBalance.setText(String.format(Locale.CHINA, "¥%.2f", balance));
            tvBalance.setTextColor(balance >= 0 ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
        }
    }

    private void refreshCurrentChart() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        switch (currentChartType) {
            case 0:
                showTabMode();
                break;
            case 1:
                showIncomeExpenseBar();
                break;
            case 2:
                showCategoryBar();
                break;
        }
    }

    private void showTabMode() {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (expenseTypes == null || viewPager == null || tabLayout == null) return;

        List<Fragment> fragments = new ArrayList<>();

        if (currentChartType == 1) {
            fragments.add(IncomeExpenseBarFragment.newInstance(totalIncome, totalExpense));
        } else {
            fragments.add(IncomeExpenseChartFragment.newInstance(totalIncome, totalExpense));
            fragments.add(CategoryChartFragment.newInstance(expenseTypes));
        }

        StatisticsPagerAdapter adapter = new StatisticsPagerAdapter(this, fragments);
        viewPager.setAdapter(adapter);

        if (tabLayout != null && currentChartType != 1) {
            new TabLayoutMediator(tabLayout, viewPager,
                    (tab, position) -> {
                        switch (position) {
                            case 0:
                                tab.setText(R.string.income_expense_stats);
                                break;
                            case 1:
                                tab.setText(R.string.category_stats);
                                break;
                        }
                    }).attach();
            tabLayout.setVisibility(View.VISIBLE);
        } else if (tabLayout != null && currentChartType == 1) {
            tabLayout.setVisibility(View.GONE);
        }
    }

    private void showCircleMode() {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (expenseTypes == null || expenseTypes.isEmpty()) return;

        updateCircleChart();

        CategoryDetailAdapter adapter = new CategoryDetailAdapter(expenseTypes);
        if (recyclerCategory != null) {
            recyclerCategory.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerCategory.setAdapter(adapter);
        }
    }

    private void updateCircleChart() {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (pieChartCircle == null) return;

        if (expenseTypes == null || expenseTypes.isEmpty()) {
            pieChartCircle.clear();
            pieChartCircle.setNoDataText(getString(R.string.no_expense_data));
            pieChartCircle.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        int[] customColors = {
                Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4"),
                Color.parseColor("#45B7D1"), Color.parseColor("#96CEB4"),
                Color.parseColor("#FFEAA7"), Color.parseColor("#DDA0DD"),
                Color.parseColor("#98D8C8"), Color.parseColor("#F7DC6F"),
                Color.parseColor("#BB8FCE"), Color.parseColor("#85C1E2")
        };

        int colorIndex = 0;
        double totalExpenseAmount = 0;

        for (AccountType type : expenseTypes) {
            if (type != null && type.getTotalAmount() > 0) {
                entries.add(new PieEntry((float) type.getTotalAmount(), type.getTypeName()));
                colors.add(customColors[colorIndex % customColors.length]);
                colorIndex++;
                totalExpenseAmount += type.getTotalAmount();
            }
        }

        if (entries.isEmpty()) {
            pieChartCircle.clear();
            pieChartCircle.setNoDataText(getString(R.string.no_expense_data));
            pieChartCircle.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new PercentFormatter(pieChartCircle));

        PieData pieData = new PieData(dataSet);
        pieChartCircle.setData(pieData);
        pieChartCircle.setUsePercentValues(true);
        pieChartCircle.getDescription().setEnabled(false);
        pieChartCircle.setCenterText(String.format(Locale.CHINA, "%s\n¥%.2f", getString(R.string.total_expense_title), totalExpenseAmount));
        pieChartCircle.setCenterTextSize(14f);
        pieChartCircle.setHoleRadius(40f);
        pieChartCircle.setTransparentCircleRadius(45f);
        pieChartCircle.setHighlightPerTapEnabled(true);
        pieChartCircle.animateY(1000);
        pieChartCircle.invalidate();
    }

    // 账本选择适配器
    static class LedgerSelectorAdapter extends RecyclerView.Adapter<LedgerSelectorAdapter.ViewHolder> {
        private final List<LedgerBook> ledgers;
        private final int currentLedgerId;
        private final OnLedgerSelectedListener listener;

        interface OnLedgerSelectedListener {
            void onSelected(LedgerBook ledger);
        }

        LedgerSelectorAdapter(List<LedgerBook> ledgers, int currentLedgerId, OnLedgerSelectedListener listener) {
            this.ledgers = ledgers;
            this.currentLedgerId = currentLedgerId;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ledger_selector, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LedgerBook ledger = ledgers.get(position);
            holder.tvIcon.setText(ledger.getIcon());
            holder.tvName.setText(ledger.getName());

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            try {
                int color = Color.parseColor(ledger.getColor());
                drawable.setColor(color);
            } catch (Exception e) {
                drawable.setColor(Color.parseColor("#4CAF50"));
            }
            holder.iconContainer.setBackground(drawable);

            if (ledger.getId() == currentLedgerId) {
                holder.tvCurrent.setVisibility(View.VISIBLE);
                holder.tvCurrent.setText("✓ 当前");
            } else {
                holder.tvCurrent.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null && ledger.getId() != currentLedgerId) {
                    listener.onSelected(ledger);
                }
            });
        }

        @Override
        public int getItemCount() {
            return ledgers.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvIcon, tvName, tvCurrent;
            View iconContainer;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvIcon = itemView.findViewById(R.id.tv_ledger_icon);
                tvName = itemView.findViewById(R.id.tv_ledger_name);
                tvCurrent = itemView.findViewById(R.id.tv_current_ledger);
                iconContainer = itemView.findViewById(R.id.ledger_icon_container);
            }
        }
    }

    // ========== 内部Fragment类 ==========
    public static class IncomeExpenseChartFragment extends Fragment {
        private double totalIncome;
        private double totalExpense;

        public static IncomeExpenseChartFragment newInstance(double totalIncome, double totalExpense) {
            IncomeExpenseChartFragment fragment = new IncomeExpenseChartFragment();
            Bundle args = new Bundle();
            args.putDouble("totalIncome", totalIncome);
            args.putDouble("totalExpense", totalExpense);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null) {
                totalIncome = getArguments().getDouble("totalIncome", 0);
                totalExpense = getArguments().getDouble("totalExpense", 0);
            }
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_chart, container, false);
            PieChart pieChart = view.findViewById(R.id.pie_chart);
            TextView tvHint = view.findViewById(R.id.tv_hint);

            if (totalIncome == 0 && totalExpense == 0) {
                if (tvHint != null) {
                    tvHint.setVisibility(View.VISIBLE);
                    tvHint.setText(R.string.no_data_text);
                }
                if (pieChart != null) pieChart.setVisibility(View.GONE);
                return view;
            }

            if (tvHint != null) tvHint.setVisibility(View.GONE);
            if (pieChart != null) pieChart.setVisibility(View.VISIBLE);

            List<PieEntry> entries = new ArrayList<>();
            if (totalIncome > 0) {
                entries.add(new PieEntry((float) totalIncome, getString(R.string.income_text) + " ¥" + String.format(Locale.CHINA, "%.2f", totalIncome)));
            }
            if (totalExpense > 0) {
                entries.add(new PieEntry((float) totalExpense, getString(R.string.expense_text) + " ¥" + String.format(Locale.CHINA, "%.2f", totalExpense)));
            }

            PieDataSet dataSet = new PieDataSet(entries, "");
            int[] colors = {Color.parseColor("#4CAF50"), Color.parseColor("#F44336")};
            dataSet.setColors(colors);
            dataSet.setValueTextSize(14f);
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setValueFormatter(new PercentFormatter(pieChart));

            PieData pieData = new PieData(dataSet);
            if (pieChart != null) {
                pieChart.setData(pieData);
                pieChart.setUsePercentValues(true);
                pieChart.getDescription().setEnabled(false);
                pieChart.setCenterText(String.format(Locale.CHINA, "%s\n¥%.2f", getString(R.string.total_text), totalIncome + totalExpense));
                pieChart.setCenterTextSize(14f);
                pieChart.animateY(1000);
                pieChart.invalidate();
            }

            return view;
        }
    }

    public static class CategoryChartFragment extends Fragment {
        private List<AccountType> expenseTypes;

        public static CategoryChartFragment newInstance(List<AccountType> expenseTypes) {
            CategoryChartFragment fragment = new CategoryChartFragment();
            Bundle args = new Bundle();
            args.putSerializable("expenseTypes", expenseTypes != null ? new ArrayList<>(expenseTypes) : new ArrayList<>());
            fragment.setArguments(args);
            return fragment;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null) {
                expenseTypes = (List<AccountType>) getArguments().getSerializable("expenseTypes");
                if (expenseTypes == null) expenseTypes = new ArrayList<>();
            }
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_chart, container, false);
            PieChart pieChart = view.findViewById(R.id.pie_chart);
            TextView tvHint = view.findViewById(R.id.tv_hint);

            List<AccountType> typesWithAmount = new ArrayList<>();
            double totalExpense = 0;

            for (AccountType type : expenseTypes) {
                if (type != null && type.getTotalAmount() > 0) {
                    typesWithAmount.add(type);
                    totalExpense += type.getTotalAmount();
                }
            }

            if (typesWithAmount.isEmpty()) {
                if (tvHint != null) {
                    tvHint.setVisibility(View.VISIBLE);
                    tvHint.setText(R.string.no_expense_data);
                }
                if (pieChart != null) pieChart.setVisibility(View.GONE);
                return view;
            }

            if (tvHint != null) tvHint.setVisibility(View.GONE);
            if (pieChart != null) pieChart.setVisibility(View.VISIBLE);

            List<PieEntry> entries = new ArrayList<>();
            int[] colors = {
                    Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4"),
                    Color.parseColor("#45B7D1"), Color.parseColor("#96CEB4"),
                    Color.parseColor("#FFEAA7"), Color.parseColor("#DDA0DD")
            };

            for (int i = 0; i < typesWithAmount.size() && i < colors.length; i++) {
                AccountType type = typesWithAmount.get(i);
                entries.add(new PieEntry((float) type.getTotalAmount(),
                        type.getTypeName() + "\n¥" + String.format(Locale.CHINA, "%.2f", type.getTotalAmount())));
            }

            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColors(colors);
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setValueFormatter(new PercentFormatter(pieChart));

            PieData pieData = new PieData(dataSet);
            if (pieChart != null) {
                pieChart.setData(pieData);
                pieChart.setUsePercentValues(true);
                pieChart.getDescription().setEnabled(false);
                pieChart.setCenterText(String.format(Locale.CHINA, "%s¥%.2f", getString(R.string.expense_text), totalExpense));
                pieChart.setCenterTextSize(14f);
                pieChart.setHoleRadius(40f);
                pieChart.setTransparentCircleRadius(45f);
                pieChart.animateY(1000);
                pieChart.invalidate();
            }

            return view;
        }
    }

    public static class StatisticsPagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        private final List<Fragment> fragments;

        public StatisticsPagerAdapter(@NonNull Fragment fragment, List<Fragment> fragments) {
            super(fragment);
            this.fragments = fragments;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (fragments != null && position < fragments.size()) {
                return fragments.get(position);
            }
            return new Fragment();
        }

        @Override
        public int getItemCount() {
            return fragments != null ? fragments.size() : 0;
        }
    }
}