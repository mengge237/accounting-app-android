package com.smxy.myapplication.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.smxy.myapplication.R;

import java.util.ArrayList;
import java.util.List;

public class IncomeExpenseBarFragment extends Fragment {
    private double totalIncome;
    private double totalExpense;

    public static IncomeExpenseBarFragment newInstance(double totalIncome, double totalExpense) {
        IncomeExpenseBarFragment fragment = new IncomeExpenseBarFragment();
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
        View view = inflater.inflate(R.layout.fragment_bar_chart, container, false);
        BarChart barChart = view.findViewById(R.id.bar_chart);

        if (barChart == null) return view;

        if (totalIncome == 0 && totalExpense == 0) {
            barChart.setNoDataText(getString(R.string.no_data_text));
            barChart.invalidate();
            return view;
        }

        List<BarEntry> incomeEntries = new ArrayList<>();
        List<BarEntry> expenseEntries = new ArrayList<>();

        incomeEntries.add(new BarEntry(0f, (float) totalIncome));
        expenseEntries.add(new BarEntry(1f, (float) totalExpense));

        BarDataSet incomeSet = new BarDataSet(incomeEntries, getString(R.string.income_text));
        incomeSet.setColor(Color.parseColor("#4CAF50"));
        incomeSet.setValueTextSize(14f);
        incomeSet.setValueTextColor(Color.parseColor("#4CAF50"));

        BarDataSet expenseSet = new BarDataSet(expenseEntries, getString(R.string.expense_text));
        expenseSet.setColor(Color.parseColor("#F44336"));
        expenseSet.setValueTextSize(14f);
        expenseSet.setValueTextColor(Color.parseColor("#F44336"));

        BarData barData = new BarData(incomeSet, expenseSet);
        barData.setBarWidth(0.6f);
        barData.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getBarLabel(BarEntry barEntry) {
                return String.format("¥%.2f", barEntry.getY());
            }
        });

        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        String[] labels = {getString(R.string.income_text), getString(R.string.expense_text)};
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();

        return view;
    }
}