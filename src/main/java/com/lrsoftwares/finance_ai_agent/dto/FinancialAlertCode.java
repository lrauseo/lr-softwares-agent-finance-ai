package com.lrsoftwares.finance_ai_agent.dto;

public enum FinancialAlertCode {
    EXPENSES_EXCEED_INCOME("FA-001"),
    TIGHT_BALANCE("FA-002"),
    HIGH_CATEGORY_CONSUMPTION("FA-003"),
    STRONG_CATEGORY_GROWTH("FA-004"),
    HEAVY_RECURRING_SUBSCRIPTIONS("FA-005");

    private final String code;

    FinancialAlertCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
