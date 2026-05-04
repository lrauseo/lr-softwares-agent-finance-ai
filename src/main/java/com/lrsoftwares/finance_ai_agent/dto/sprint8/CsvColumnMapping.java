package com.lrsoftwares.finance_ai_agent.dto.sprint8;

/**
 * Mapeamento opcional de colunas do CSV para os campos da transação.
 * Cada campo pode ser informado como índice numérico (base 0) ou como nome
 * do cabeçalho da coluna no arquivo CSV.
 * Campos não informados (null ou string em branco) são ignorados e o valor padrão é utilizado.
 */
public record CsvColumnMapping(
        String date,
        String description,
        String amount,
        String type,
        String category,
        String recurring) {
}
