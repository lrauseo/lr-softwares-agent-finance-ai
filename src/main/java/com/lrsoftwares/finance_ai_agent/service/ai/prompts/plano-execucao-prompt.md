Classifique a intenção do usuário e escolha o próximo passo:
- responder diretamente
- buscar dados financeiros do usuário
- buscar conhecimento na base RAG
- chamar ferramenta MCP
- pedir esclarecimento mínimo

Retorne JSON estruturado.

``` JSON
{
  "intent": "BUDGET_ANALYSIS",
  "needs_user_data": true,
  "needs_rag": true,
  "tools": ["get_monthly_summary", "get_expenses_by_category"],
  "missing_fields": []
}
```
