package com.smxy.myapplication.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.smxy.myapplication.R;
import com.smxy.myapplication.model.AccountType;

import java.util.ArrayList;
import java.util.List;

public class CategoryBarFragment extends Fragment {
    private List<AccountType> expenseTypes;

    public static CategoryBarFragment newInstance(List<AccountType> expenseTypes) {
        CategoryBarFragment fragment = new CategoryBarFragment();
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
        View view = inflater.inflate(R.layout.fragment_category_bar_chart, container, false);
        HorizontalBarChart barChart = view.findViewById(R.id.horizontal_bar_chart);
        TextView tvHint = view.findViewById(R.id.tv_hint);

        if (barChart == null) return view;

        List<AccountType> typesWithAmount = new ArrayList<>();
        for (AccountType type : expenseTypes) {
            if (type != null && type.getTotalAmount() > 0) {
                typesWithAmount.add(type);
            }
        }

        if (typesWithAmount.isEmpty()) {
            if (tvHint != null) {
                tvHint.setVisibility(View.VISIBLE);
            }
            barChart.setVisibility(View.GONE);
            return view;
        }

        if (tvHint != null) tvHint.setVisibility(View.GONE);
        barChart.setVisibility(View.VISIBLE);

        int[] colors = {
                Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4"),
                Color.parseColor("#45B7D1"), Color.parseColor("#96CEB4"),
                Color.parseColor("#FFEAA7"), Color.parseColor("#DDA0DD"),
                Color.parseColor("#98D8C8"), Color.parseColor("#F7DC6F"),
                Color.parseColor("#BB8FCE"), Color.parseColor("#85C1E2")
        };

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < typesWithAmount.size(); i++) {
            AccountType type = typesWithAmount.get(i);
            entries.add(new BarEntry(i, (float) type.getTotalAmount()));
            labels.add(type.getTypeName());
        }

        BarDataSet dataSet = new BarDataSet(entries, getString(R.string.expense_detail_title));
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getBarLabel(BarEntry barEntry) {
                return String.format("¥%.2f", barEntry.getY());
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);

        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);
        barChart.setDrawValueAboveBar(true);
        barChart.setDrawBarShadow(false);

        YAxis yAxis = barChart.getAxisLeft();
        yAxis.setDrawGridLines(false);
        yAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        yAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("¥%.0f", value);
            }
        });

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        barChart.getAxisRight().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();

        return view;
    }
}