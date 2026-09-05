package com.smxy.myapplication.fragment;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.smxy.myapplication.R;
import com.smxy.myapplication.adapter.ScheduleAdapter;
import com.smxy.myapplication.model.Schedule;
import com.smxy.myapplication.network.ApiClient;
import com.smxy.myapplication.network.ApiService;
import com.smxy.myapplication.network.ScheduleResponse;
import com.smxy.myapplication.manager.SettingsManager;
import com.smxy.myapplication.utils.ErrorHandler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScheduleListFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvDate;
    private TextView tvEmpty;
    private ImageButton btnPrev;
    private ImageButton btnNext;
    private Button btnToday;
    private Button btnTomorrow;
    private Button btnThisWeek;
    private Button btnNextWeek;
    private Button btnThisMonth;
    private Button btnShowAll;
    private FloatingActionButton fabAdd;
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private String searchKeyword = "";
    private ScheduleAdapter adapter;
    private final List<Schedule> scheduleList = new ArrayList<>();
    private final List<Schedule> allSchedules = new ArrayList<>();
    private ApiService apiService;
    private String authToken;
    private Calendar currentCalendar = Calendar.getInstance();
    private boolean showAllMode = false;
    private String currentViewType = "day";
    private ProgressDialog progressDialog;
    private SettingsManager settingsManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule_list, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        tvDate = view.findViewById(R.id.tv_date);
        tvEmpty = view.findViewById(R.id.tv_empty);
        fabAdd = view.findViewById(R.id.fab_add);
        btnPrev = view.findViewById(R.id.btn_prev_day);
        btnNext = view.findViewById(R.id.btn_next_day);
        btnToday = view.findViewById(R.id.btn_today);
        btnTomorrow = view.findViewById(R.id.btn_tomorrow);
        btnThisWeek = view.findViewById(R.id.btn_this_week);
        btnNextWeek = view.findViewById(R.id.btn_next_week);
        btnThisMonth = view.findViewById(R.id.btn_this_month);
        btnShowAll = view.findViewById(R.id.btn_show_all);
        etSearch = view.findViewById(R.id.et_search);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);

        if (ErrorHandler.isContextValid(getContext())) {
            settingsManager = new SettingsManager(requireContext());
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!ErrorHandler.isFragmentValid(this)) return;

        setupRecyclerView();
        setupListeners();
        loadToken();
        loadAllSchedules();
        updateDateDisplay();
    }

    private void setupRecyclerView() {
        if (recyclerView == null) return;

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ScheduleAdapter(scheduleList, this::showScheduleDetail);
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                if (!showAllMode && !currentViewType.equals("week") && !currentViewType.equals("month")) {
                    navigatePrev();
                    filterSchedulesByDate();
                }
            });
        }

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                if (!showAllMode && !currentViewType.equals("week") && !currentViewType.equals("month")) {
                    navigateNext();
                    filterSchedulesByDate();
                }
            });
        }

        if (btnToday != null) {
            btnToday.setOnClickListener(v -> {
                if (!showAllMode) {
                    currentViewType = "day";
                    currentCalendar = Calendar.getInstance();
                    updateDateDisplay();
                    filterSchedulesByDate();
                }
            });
        }

        if (btnTomorrow != null) {
            btnTomorrow.setOnClickListener(v -> {
                if (!showAllMode) {
                    currentViewType = "day";
                    currentCalendar = Calendar.getInstance();
                    currentCalendar.add(Calendar.DAY_OF_MONTH, 1);
                    updateDateDisplay();
                    filterSchedulesByDate();
                }
            });
        }

        if (btnThisWeek != null) {
            btnThisWeek.setOnClickListener(v -> {
                if (!showAllMode) {
                    currentViewType = "week";
                    currentCalendar = Calendar.getInstance();
                    int dayOfWeek = currentCalendar.get(Calendar.DAY_OF_WEEK);
                    int daysToMonday = dayOfWeek - Calendar.MONDAY;
                    if (daysToMonday < 0) daysToMonday += 7;
                    currentCalendar.add(Calendar.DAY_OF_MONTH, -daysToMonday);
                    updateDateDisplay();
                    loadWeekSchedules();
                }
            });
        }

        if (btnNextWeek != null) {
            btnNextWeek.setOnClickListener(v -> {
                if (!showAllMode) {
                    currentViewType = "week";
                    currentCalendar = Calendar.getInstance();
                    int dayOfWeek = currentCalendar.get(Calendar.DAY_OF_WEEK);
                    int daysToMonday = dayOfWeek - Calendar.MONDAY;
                    if (daysToMonday < 0) daysToMonday += 7;
                    currentCalendar.add(Calendar.DAY_OF_MONTH, -daysToMonday);
                    currentCalendar.add(Calendar.DAY_OF_MONTH, 7);
                    updateDateDisplay();
                    loadWeekSchedules();
                }
            });
        }

        if (btnThisMonth != null) {
            btnThisMonth.setOnClickListener(v -> {
                if (!showAllMode) {
                    currentViewType = "month";
                    currentCalendar = Calendar.getInstance();
                    currentCalendar.set(Calendar.DAY_OF_MONTH, 1);
                    updateDateDisplay();
                    loadMonthSchedules();
                }
            });
        }

        if (btnShowAll != null) {
            btnShowAll.setOnClickListener(v -> {
                showAllMode = !showAllMode;
                if (showAllMode) {
                    currentViewType = "all";
                    btnShowAll.setText("按日期");
                    if (tvDate != null) tvDate.setText("全部日程");
                    scheduleList.clear();
                    scheduleList.addAll(allSchedules);
                    if (adapter != null) adapter.updateSchedules(scheduleList);
                    updateEmptyView();
                } else {
                    currentViewType = "day";
                    btnShowAll.setText("全部");
                    updateDateDisplay();
                    filterSchedulesByDate();
                }
            });
        }

        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> showAddScheduleDialog());
        }

        if (tvDate != null) {
            tvDate.setOnClickListener(v -> {
                if (!showAllMode && !currentViewType.equals("week") && !currentViewType.equals("month")) {
                    showDatePicker();
                }
            });
        }

        setupSearch();
    }

    private void setupSearch() {
        if (etSearch == null) return;

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchKeyword = s.toString().trim().toLowerCase();
                if (btnClearSearch != null) {
                    btnClearSearch.setVisibility(searchKeyword.isEmpty() ? View.GONE : View.VISIBLE);
                }
                applySearchFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (etSearch != null) etSearch.setText("");
                searchKeyword = "";
                btnClearSearch.setVisibility(View.GONE);
                applySearchFilter();
            });
        }
    }

    private void applySearchFilter() {
        if (searchKeyword.isEmpty()) {
            refreshDisplayByCurrentMode();
            return;
        }

        List<Schedule> filtered = new ArrayList<>();
        for (Schedule schedule : allSchedules) {
            if (matchesKeyword(schedule, searchKeyword)) {
                filtered.add(schedule);
            }
        }

        scheduleList.clear();
        scheduleList.addAll(filtered);
        if (adapter != null) adapter.updateSchedules(scheduleList);
        updateEmptyView();
    }

    private boolean matchesKeyword(Schedule schedule, String keyword) {
        String title = schedule.getTitle();
        String content = schedule.getContent();
        String note = schedule.getNote();

        boolean titleMatch = title != null && title.toLowerCase().contains(keyword);
        boolean contentMatch = content != null && content.toLowerCase().contains(keyword);
        boolean noteMatch = note != null && note.toLowerCase().contains(keyword);

        return titleMatch || contentMatch || noteMatch;
    }

    private void navigatePrev() {
        currentCalendar.add(Calendar.DAY_OF_MONTH, -1);
        updateDateDisplay();
    }

    private void navigateNext() {
        currentCalendar.add(Calendar.DAY_OF_MONTH, 1);
        updateDateDisplay();
    }

    private void updateDateDisplay() {
        if (tvDate == null) return;

        SimpleDateFormat sdf;
        if (currentViewType.equals("week")) {
            Calendar endCalendar = (Calendar) currentCalendar.clone();
            endCalendar.add(Calendar.DAY_OF_MONTH, 6);
            sdf = new SimpleDateFormat("MM月dd日", Locale.CHINA);
            String startDate = sdf.format(currentCalendar.getTime());
            String endDate = sdf.format(endCalendar.getTime());
            tvDate.setText(startDate + " - " + endDate);
        } else if (currentViewType.equals("month")) {
            sdf = new SimpleDateFormat("yyyy年MM月", Locale.CHINA);
            tvDate.setText(sdf.format(currentCalendar.getTime()));
        } else {
            sdf = new SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINA);
            tvDate.setText(sdf.format(currentCalendar.getTime()));
        }
    }

    private void showDatePicker() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    currentCalendar.set(year, month, dayOfMonth);
                    currentViewType = "day";
                    updateDateDisplay();
                    filterSchedulesByDate();
                },
                currentCalendar.get(Calendar.YEAR),
                currentCalendar.get(Calendar.MONTH),
                currentCalendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
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

    private void showProgress() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage(getString(R.string.loading));
            progressDialog.setCancelable(false);
        }
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void loadAllSchedules() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        if (!ErrorHandler.isTokenValid(authToken)) {
            if (tvEmpty != null) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText(R.string.login_first);
            }
            return;
        }

        showProgress();

        Map<String, String> params = new HashMap<>();
        params.put("start_date", "2020-01-01");
        params.put("end_date", "2030-12-31");

        Call<ScheduleResponse> call = apiService.getSchedules(authToken, params);
        call.enqueue(new ErrorHandler.SafeCallback<ScheduleResponse>(this) {
            @Override
            protected void onSuccess(ScheduleResponse data) {
                hideProgress();

                List<Schedule> schedules = data.getSchedules();
                if (schedules != null && !schedules.isEmpty()) {
                    for (Schedule s : schedules) {
                        String rawDate = s.getScheduledDate();
                        if (rawDate != null && rawDate.contains("T")) {
                            String formattedDate = rawDate.substring(0, 10);
                            s.setScheduledDate(formattedDate);
                        }
                    }
                    allSchedules.clear();
                    allSchedules.addAll(schedules);
                } else {
                    allSchedules.clear();
                }

                refreshDisplayByCurrentMode();

                if (allSchedules.isEmpty() && tvEmpty != null) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(getString(R.string.empty_all_schedules));
                }
            }

            @Override
            protected void onError(String message) {
                hideProgress();
                if (tvEmpty != null) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(message);
                }
            }
        });
    }

    private void refreshDisplayByCurrentMode() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        if (showAllMode || currentViewType.equals("all")) {
            scheduleList.clear();
            scheduleList.addAll(allSchedules);
            if (adapter != null) adapter.updateSchedules(scheduleList);
            updateEmptyView();
        } else if (currentViewType.equals("week")) {
            loadWeekSchedules();
        } else if (currentViewType.equals("month")) {
            loadMonthSchedules();
        } else {
            filterSchedulesByDate();
        }
    }

    private void filterSchedulesByDate() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        if (allSchedules.isEmpty()) {
            scheduleList.clear();
            if (adapter != null) adapter.updateSchedules(scheduleList);
            updateEmptyView();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        String selectedDate = sdf.format(currentCalendar.getTime());

        List<Schedule> filtered = new ArrayList<>();
        for (Schedule schedule : allSchedules) {
            String scheduleDate = schedule.getScheduledDate();
            if (scheduleDate != null && selectedDate.equals(scheduleDate)) {
                filtered.add(schedule);
            }
        }

        scheduleList.clear();
        scheduleList.addAll(filtered);
        if (adapter != null) adapter.updateSchedules(scheduleList);
        updateEmptyView();

        if (filtered.isEmpty() && !allSchedules.isEmpty() && !showAllMode && ErrorHandler.isContextValid(getContext())) {
            Toast.makeText(getContext(), "今天没有日程，点击「全部」查看所有日程", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadWeekSchedules() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        if (allSchedules.isEmpty()) {
            scheduleList.clear();
            if (adapter != null) adapter.updateSchedules(scheduleList);
            updateEmptyView();
            return;
        }

        Calendar endCal = (Calendar) currentCalendar.clone();
        endCal.add(Calendar.DAY_OF_MONTH, 6);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        String startDate = sdf.format(currentCalendar.getTime());
        String endDate = sdf.format(endCal.getTime());

        List<Schedule> filtered = new ArrayList<>();
        for (Schedule schedule : allSchedules) {
            String scheduleDate = schedule.getScheduledDate();
            if (scheduleDate != null && scheduleDate.compareTo(startDate) >= 0 && scheduleDate.compareTo(endDate) <= 0) {
                filtered.add(schedule);
            }
        }

        scheduleList.clear();
        scheduleList.addAll(filtered);
        if (adapter != null) adapter.updateSchedules(scheduleList);
        updateEmptyView();
    }

    private void loadMonthSchedules() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        if (allSchedules.isEmpty()) {
            scheduleList.clear();
            if (adapter != null) adapter.updateSchedules(scheduleList);
            updateEmptyView();
            return;
        }

        Calendar startCal = (Calendar) currentCalendar.clone();
        startCal.set(Calendar.DAY_OF_MONTH, 1);
        Calendar endCal = (Calendar) currentCalendar.clone();
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        String startDate = sdf.format(startCal.getTime());
        String endDate = sdf.format(endCal.getTime());

        List<Schedule> filtered = new ArrayList<>();
        for (Schedule schedule : allSchedules) {
            String scheduleDate = schedule.getScheduledDate();
            if (scheduleDate != null && scheduleDate.compareTo(startDate) >= 0 && scheduleDate.compareTo(endDate) <= 0) {
                filtered.add(schedule);
            }
        }

        scheduleList.clear();
        scheduleList.addAll(filtered);
        if (adapter != null) adapter.updateSchedules(scheduleList);
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        if (tvEmpty == null) return;

        if (scheduleList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            if (!searchKeyword.isEmpty()) {
                tvEmpty.setText(R.string.search_no_result);
            } else if (showAllMode) {
                tvEmpty.setText(R.string.empty_all_schedules);
            } else {
                tvEmpty.setText(R.string.schedule_empty);
            }
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void showAddScheduleDialog() {
        if (ErrorHandler.isContextValid(getContext())) {
            Toast.makeText(getContext(), R.string.edit_developing, Toast.LENGTH_SHORT).show();
        }
    }

    private void showScheduleDetail(Schedule schedule) {
        if (!ErrorHandler.isFragmentValid(this) || schedule == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_schedule_detail, null);

        TextView tvTitle = view.findViewById(R.id.tv_detail_title);
        TextView tvContent = view.findViewById(R.id.tv_detail_content);
        TextView tvDateTime = view.findViewById(R.id.tv_detail_datetime);
        TextView tvPriority = view.findViewById(R.id.tv_detail_priority);
        TextView tvNote = view.findViewById(R.id.tv_detail_note);
        Button btnEdit = view.findViewById(R.id.btn_edit);
        Button btnDelete = view.findViewById(R.id.btn_delete);
        Button btnComplete = view.findViewById(R.id.btn_complete);

        if (tvTitle != null) tvTitle.setText(ErrorHandler.getSafeString(schedule.getTitle(), "无标题"));

        String contentText = schedule.getContent() != null && !schedule.getContent().isEmpty()
                ? schedule.getContent() : getString(R.string.no_content_text);
        if (tvContent != null) tvContent.setText(contentText);

        String dateTimeText = schedule.getScheduledDate();
        if (schedule.getScheduledTime() != null && !schedule.getScheduledTime().isEmpty()) {
            dateTimeText = schedule.getScheduledDate() + " " + schedule.getScheduledTime();
        }
        if (tvDateTime != null) tvDateTime.setText(dateTimeText);

        int priority = schedule.getPriority();
        String priorityText;
        if (priority == 1) priorityText = getString(R.string.low_priority);
        else if (priority == 2) priorityText = getString(R.string.medium_priority);
        else if (priority == 3) priorityText = getString(R.string.high_priority);
        else priorityText = getString(R.string.medium_priority);
        if (tvPriority != null) tvPriority.setText(priorityText);

        String noteText = schedule.getNote() != null && !schedule.getNote().isEmpty()
                ? schedule.getNote() : getString(R.string.no_note_text);
        if (tvNote != null) tvNote.setText(noteText);

        if (btnComplete != null) {
            if (schedule.isCompleted()) {
                btnComplete.setText("↺ 取消完成");
            } else {
                btnComplete.setText("✓ 标记完成");
            }
            btnComplete.setEnabled(true);
        }

        AlertDialog dialog = builder.setView(view).create();

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                dialog.dismiss();
                Toast.makeText(getContext(), R.string.edit_developing, Toast.LENGTH_SHORT).show();
            });
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                dialog.dismiss();
                showDeleteConfirmDialog(schedule);
            });
        }

        if (btnComplete != null) {
            btnComplete.setOnClickListener(v -> {
                dialog.dismiss();
                toggleScheduleStatus(schedule);
            });
        }

        dialog.show();
    }

    private void toggleScheduleStatus(Schedule schedule) {
        if (!ErrorHandler.isFragmentValid(this) || schedule == null) return;
        if (apiService == null) return;

        if (schedule.isCompleted() && settingsManager != null && !settingsManager.isCheatModeEnabled()) {
            Toast.makeText(getContext(), R.string.cheat_mode_required, Toast.LENGTH_LONG).show();
            return;
        }

        showProgress();

        int newStatus = schedule.isCompleted() ? 0 : 1;
        String statusText = schedule.isCompleted() ? "未完成" : "已完成";

        Map<String, Object> status = new HashMap<>();
        status.put("is_completed", newStatus);

        Call<ScheduleResponse> call = apiService.updateScheduleStatus(authToken, schedule.getId(), status);
        call.enqueue(new ErrorHandler.SafeCallback<ScheduleResponse>(this) {
            @Override
            protected void onSuccess(ScheduleResponse data) {
                hideProgress();
                Toast.makeText(getContext(), "已标记为" + statusText, Toast.LENGTH_SHORT).show();
                loadAllSchedules();
            }

            @Override
            protected void onError(String message) {
                hideProgress();
                Toast.makeText(getContext(), R.string.operation_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmDialog(Schedule schedule) {
        if (!ErrorHandler.isFragmentValid(this) || schedule == null) return;

        String message = getString(R.string.delete_confirm_message,
                ErrorHandler.getSafeString(schedule.getTitle(), "这个日程"));
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_confirm_title)
                .setMessage(message)
                .setPositiveButton(R.string.confirm, (dialog, which) -> deleteSchedule(schedule.getId()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteSchedule(int scheduleId) {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (apiService == null) return;

        showProgress();

        Call<ScheduleResponse> call = apiService.deleteSchedule(authToken, scheduleId);
        call.enqueue(new ErrorHandler.SafeCallback<ScheduleResponse>(this) {
            @Override
            protected void onSuccess(ScheduleResponse data) {
                hideProgress();
                Toast.makeText(getContext(), R.string.delete_success, Toast.LENGTH_SHORT).show();
                loadAllSchedules();
            }

            @Override
            protected void onError(String message) {
                hideProgress();
                Toast.makeText(getContext(), R.string.delete_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
}