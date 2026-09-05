package com.smxy.myapplication.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputEditText;
import com.smxy.myapplication.R;
import com.smxy.myapplication.model.SelfSelectedItem;
import com.smxy.myapplication.utils.ErrorHandler;

import java.util.Objects;

/**
 * 添加自选弹窗
 */
public class AddSelfSelectedDialog extends Dialog {

    private TextInputEditText etCode;
    private TextInputEditText etName;
    private Spinner spinnerType;
    private TextInputEditText etBuyPrice;
    private TextInputEditText etCurrentPrice;
    private TextInputEditText etRemark;
    private Button btnConfirm;
    private Button btnCancel;

    private OnConfirmListener onConfirmListener;
    private String selectedType = "股票";

    public interface OnConfirmListener {
        void onConfirm(SelfSelectedItem item);
    }

    public AddSelfSelectedDialog(@NonNull Context context) {
        super(context);
    }

    public void setOnConfirmListener(OnConfirmListener listener) {
        this.onConfirmListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_add_self_selected);
        setTitle("添加自选");

        initViews();
        setupSpinner();
        setupListeners();
    }

    private void initViews() {
        etCode = findViewById(R.id.et_code);
        etName = findViewById(R.id.et_name);
        spinnerType = findViewById(R.id.spinner_type);
        etBuyPrice = findViewById(R.id.et_buy_price);
        etCurrentPrice = findViewById(R.id.et_current_price);
        etRemark = findViewById(R.id.et_remark);
        btnConfirm = findViewById(R.id.btn_confirm);
        btnCancel = findViewById(R.id.btn_cancel);
    }

    private void setupSpinner() {
        String[] types = {"股票", "基金", "债券", "期货", "外汇"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(Objects.requireNonNull(getContext()),
                android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedType = types[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedType = "股票";
            }
        });
    }

    private void setupListeners() {
        btnConfirm.setOnClickListener(v -> {
            if (validateInput()) {
                SelfSelectedItem item = new SelfSelectedItem();

                String code = getEditTextValue(etCode);
                String name = getEditTextValue(etName);

                item.setCode(code);
                item.setName(name);
                item.setType(selectedType);

                String buyPriceStr = getEditTextValue(etBuyPrice);
                if (!TextUtils.isEmpty(buyPriceStr)) {
                    try {
                        item.setBuyPrice(Double.parseDouble(buyPriceStr));
                    } catch (NumberFormatException e) {
                        item.setBuyPrice(0);
                    }
                }

                String currentPriceStr = getEditTextValue(etCurrentPrice);
                if (!TextUtils.isEmpty(currentPriceStr)) {
                    try {
                        item.setCurrentPrice(Double.parseDouble(currentPriceStr));
                    } catch (NumberFormatException e) {
                        item.setCurrentPrice(0);
                    }
                }

                String remark = getEditTextValue(etRemark);
                if (!TextUtils.isEmpty(remark)) {
                    item.setRemark(remark);
                }

                if (onConfirmListener != null) {
                    onConfirmListener.onConfirm(item);
                }
                dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }

    /**
     * 安全获取 EditText 的值，永远不返回 null
     */
    private String getEditTextValue(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    private boolean validateInput() {
        Context context = getContext();

        String code = getEditTextValue(etCode);
        if (TextUtils.isEmpty(code)) {
            ErrorHandler.showToast(context, "请输入代码");
            return false;
        }

        String name = getEditTextValue(etName);
        if (TextUtils.isEmpty(name)) {
            ErrorHandler.showToast(context, "请输入名称");
            return false;
        }
        return true;
    }
}