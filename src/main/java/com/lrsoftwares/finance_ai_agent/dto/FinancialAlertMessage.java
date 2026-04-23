package com.lrsoftwares.finance_ai_agent.dto;

public enum FinancialAlertMessage {
    EXPENSES_EXCEED_INCOME("Despesas maiores que receitas no mes analisado."),
    TIGHT_BALANCE("Saldo do mes esta muito apertado em relacao a renda."),
    HIGH_CATEGORY_CONSUMPTION("Categoria '%s' esta consumindo %.0f%% da renda mensal."),
    STRONG_CATEGORY_GROWTH("Categoria '%s' cresceu %.0f%% em relacao ao mes anterior."),
    HEAVY_RECURRING_SUBSCRIPTIONS("Assinaturas recorrentes estao consumindo %.0f%% da renda mensal.");

    private final String template;

    FinancialAlertMessage(String template) {
        this.template = template;
    }

    public String format(Object... args) {
        return String.format(template, args);
    }
}
