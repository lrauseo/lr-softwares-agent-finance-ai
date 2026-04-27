package com.lrsoftwares.finance_ai_agent.service.analysis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.dto.CategoryTotalResponse;
import com.lrsoftwares.finance_ai_agent.dto.FinancialAlertResponse;
import com.lrsoftwares.finance_ai_agent.dto.FinancialDiagnosisResponse;
import com.lrsoftwares.finance_ai_agent.dto.MonthlySummaryResponse;
import com.lrsoftwares.finance_ai_agent.entity.AlertSeverity;
import com.lrsoftwares.finance_ai_agent.service.SummaryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinancialAnalysisServiceImpl implements FinancialAnalysisService {

	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final BigDecimal LOW_BALANCE_RATE = new BigDecimal("0.10");
	private static final BigDecimal HIGH_CATEGORY_RATE = new BigDecimal("0.35");
	private static final BigDecimal VERY_HIGH_CATEGORY_RATE = new BigDecimal("0.50");

	private final SummaryService summaryService;

	@Override
	public FinancialDiagnosisResponse analyzeMonthly(UUID userId, YearMonth month) {
		MonthlySummaryResponse summary = summaryService.getSummaryMonthlyByUserIdAndDate(
				Objects.requireNonNull(month));

		List<FinancialAlertResponse> alerts = new ArrayList<>();
		List<String> highlights = new ArrayList<>();
		List<String> recommendations = new ArrayList<>();

		BigDecimal totalIncome = summary.totalIncome();
		BigDecimal totalExpense = summary.totalExpense();
		BigDecimal balance = summary.balance();

		analyzeBalance(alerts, recommendations, totalIncome, totalExpense, balance);
		analyzeCategories(alerts, highlights, recommendations, summary.categories(), totalIncome, totalExpense);

		highlights.add(buildMainHighlight(totalIncome, totalExpense, balance));

		alerts.sort(Comparator.comparing(FinancialAlertResponse::severity).reversed());

		return new FinancialDiagnosisResponse(
				userId,
				month,
				summary,
				alerts,
				highlights,
				recommendations);
	}

	private void analyzeBalance(List<FinancialAlertResponse> alerts,
			List<String> recommendations,
			BigDecimal totalIncome,
			BigDecimal totalExpense,
			BigDecimal balance) {

		if (isZero(totalIncome) && totalExpense.compareTo(ZERO) > 0) {
			alerts.add(new FinancialAlertResponse(
					AlertSeverity.CRITICAL,
					"NO_INCOME_WITH_EXPENSES",
					"Despesas sem receita registrada",
					"Você possui despesas no mês, mas nenhuma receita foi registrada.",
					totalExpense));
			recommendations.add("Cadastre suas receitas do mês para que a análise fique correta.");
			return;
		}

		if (totalExpense.compareTo(totalIncome) > 0) {
			alerts.add(new FinancialAlertResponse(
					AlertSeverity.CRITICAL,
					"EXPENSES_GREATER_THAN_INCOME",
					"Despesas maiores que receitas",
					"Suas despesas ultrapassaram suas receitas neste mês.",
					totalExpense.subtract(totalIncome)));
			recommendations.add("Revise despesas variáveis e recorrentes imediatamente.");
			return;
		}

		if (totalIncome.compareTo(ZERO) > 0) {
			BigDecimal balanceRate = balance.divide(totalIncome, 4, RoundingMode.HALF_UP);

			if (balanceRate.compareTo(LOW_BALANCE_RATE) < 0) {
				alerts.add(new FinancialAlertResponse(
						AlertSeverity.WARNING,
						"LOW_BALANCE_RATE",
						"Saldo mensal apertado",
						"Seu saldo final ficou abaixo de 10% da receita do mês.",
						balance));
				recommendations.add("Tente reservar pelo menos 10% da receita mensal antes de gastos variáveis.");
			}
		}
	}

	private void analyzeCategories(List<FinancialAlertResponse> alerts,
			List<String> highlights,
			List<String> recommendations,
			List<CategoryTotalResponse> categories,
			BigDecimal totalIncome,
			BigDecimal totalExpense) {

		if (categories == null || categories.isEmpty()) {
			highlights.add("Nenhuma despesa categorizada encontrada no mês.");
			return;
		}

		CategoryTotalResponse topCategory = categories.stream()
				.max(Comparator.comparing(CategoryTotalResponse::total))
				.orElse(null);

		if (topCategory == null) {
			return;
		}

		highlights.add("Maior categoria de despesa: " + topCategory.category() + " com total de R$ "
				+ topCategory.total() + ".");

		if (totalIncome.compareTo(ZERO) > 0) {
			BigDecimal categoryIncomeRate = topCategory.total().divide(totalIncome, 4, RoundingMode.HALF_UP);

			if (categoryIncomeRate.compareTo(VERY_HIGH_CATEGORY_RATE) >= 0) {
				alerts.add(new FinancialAlertResponse(
						AlertSeverity.CRITICAL,
						"CATEGORY_OVER_50_PERCENT_INCOME",
						"Categoria consumindo mais de 50% da receita",
						"A categoria " + topCategory.category() + " consumiu mais de 50% da sua receita mensal.",
						topCategory.total()));
				recommendations.add("Investigue a categoria " + topCategory.category()
						+ " e veja se há gastos que podem ser reduzidos.");
			} else if (categoryIncomeRate.compareTo(HIGH_CATEGORY_RATE) >= 0) {
				alerts.add(new FinancialAlertResponse(
						AlertSeverity.WARNING,
						"CATEGORY_OVER_35_PERCENT_INCOME",
						"Categoria consumindo parte alta da receita",
						"A categoria " + topCategory.category() + " consumiu mais de 35% da sua receita mensal.",
						topCategory.total()));
				recommendations.add("Defina um limite mensal para " + topCategory.category() + ".");
			}
		}

		if (totalExpense.compareTo(ZERO) > 0) {
			BigDecimal categoryExpenseRate = topCategory.total().divide(totalExpense, 4, RoundingMode.HALF_UP);

			if (categoryExpenseRate.compareTo(new BigDecimal("0.40")) >= 0) {
				alerts.add(new FinancialAlertResponse(
						AlertSeverity.INFO,
						"CATEGORY_CONCENTRATION",
						"Despesa concentrada em uma categoria",
						"A categoria " + topCategory.category()
								+ " representa uma parte relevante das despesas do mês.",
						topCategory.total()));
			}
		}
	}

	private String buildMainHighlight(BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal balance) {
		if (totalIncome.compareTo(ZERO) == 0 && totalExpense.compareTo(ZERO) == 0) {
			return "Não há receitas ou despesas registradas neste mês.";
		}

		if (balance.compareTo(ZERO) < 0) {
			return "O mês fechou negativo em R$ " + balance.abs() + ".";
		}

		if (balance.compareTo(ZERO) == 0) {
			return "O mês fechou exatamente no zero a zero. Financeiramente emocionante, como assistir tinta secar.";
		}

		return "O mês fechou positivo em R$ " + balance + ".";
	}

	private boolean isZero(BigDecimal value) {
		return value == null || value.compareTo(ZERO) == 0;
	}
}
