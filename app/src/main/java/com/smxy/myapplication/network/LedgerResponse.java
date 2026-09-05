package com.smxy.myapplication.network;

import com.smxy.myapplication.model.LedgerBook;
import java.util.List;

public class LedgerResponse {
    private boolean success;
    private List<LedgerBook> ledgers;
    private LedgerBook ledger;
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<LedgerBook> getLedgers() {
        return ledgers;
    }

    public void setLedgers(List<LedgerBook> ledgers) {
        this.ledgers = ledgers;
    }

    public LedgerBook getLedger() {
        return ledger;
    }

    public void setLedger(LedgerBook ledger) {
        this.ledger = ledger;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}