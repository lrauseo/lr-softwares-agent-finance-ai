package com.lrsoftwares.finance_ai_agent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.CsvColumnMapping;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.ImportTransactionsResponse;
import com.lrsoftwares.finance_ai_agent.service.sprint8.TransactionImportService;

import lombok.RequiredArgsConstructor;

@Tag(name = "Importação", description = "Importação de transações a partir de arquivos CSV e OFX")
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final TransactionImportService transactionImportService;
    private final ObjectMapper objectMapper;

    @Operation(
        summary = "Importar transações de um arquivo CSV",
        description = """
            Importa transações financeiras a partir de um arquivo CSV (separador `;` ou `,`).

            **Formato padrão das colunas (base 0):**
            | Índice | Coluna        | Obrigatória | Valores aceitos                          |
            |--------|---------------|-------------|------------------------------------------|
            | 0      | date          | Sim         | ISO 8601: `2024-01-31`                   |
            | 1      | description   | Sim         | Texto livre                              |
            | 2      | amount        | Sim         | Valor decimal positivo ou negativo       |
            | 3      | type          | Não         | `INCOME` ou `EXPENSE`                    |
            | 4      | category      | Não         | Nome da categoria cadastrada             |
            | 5      | recurring     | Não         | `true` ou `false`                        |

            **Exemplo de arquivo CSV no formato padrão:**
            ```
            date;description;amount;type;category;recurring
            2024-01-31;Salario;5000.00;INCOME;Salario;false
            2024-02-01;Supermercado;-250.50;EXPENSE;Alimentacao;false
            ```

            O arquivo pode omitir o cabeçalho quando o mapeamento for por índices numéricos.
            Arquivos exportados do Excel com BOM (UTF-8 BOM) são suportados.

            Use o parâmetro opcional `columnMapping` para mapear colunas fora da ordem padrão,
            informando o índice (base 0) ou o nome do cabeçalho de cada campo.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Importação processada (pode conter warnings de registros ignorados)"),
            @ApiResponse(responseCode = "400", description = "JSON de columnMapping inválido ou malformado",
                content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
        }
    )
    @PostMapping(path = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportTransactionsResponse importCsv(
            @Parameter(description = "Arquivo CSV a ser importado", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(
                description = """
                    Mapeamento opcional de colunas em formato JSON. Cada campo aceita o índice numérico (base 0)
                    ou o nome do cabeçalho correspondente no CSV.
                    Campos omitidos usam os índices padrão (date=0, description=1, amount=2, type=3, category=4, recurring=5).
                    """,
                examples = {
                    @ExampleObject(name = "Por índice",
                        summary = "Colunas fora da ordem padrão mapeadas por índice",
                        value = """
                            {"date":"2","description":"3","amount":"0","type":"1","category":"4","recurring":"5"}
                            """),
                    @ExampleObject(name = "Por nome de cabeçalho",
                        summary = "Colunas mapeadas pelo nome do cabeçalho no CSV",
                        value = """
                            {"date":"dt","description":"descricao","amount":"valor","type":"tipo","category":"categoria","recurring":"recorrente"}
                            """)
                }
            )
            @RequestParam(value = "columnMapping", required = false) String columnMappingJson) {
        CsvColumnMapping mapping = null;
        if (columnMappingJson != null && !columnMappingJson.isBlank()) {
            try {
                mapping = objectMapper.readValue(columnMappingJson, CsvColumnMapping.class);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("columnMapping inválido: " + e.getOriginalMessage(), e);
            }
        }
        return transactionImportService.importCsv(file, mapping);
    }

    @Operation(summary = "Importar transações de um arquivo OFX")
    @PostMapping(path = "/ofx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportTransactionsResponse importOfx(
            @Parameter(description = "Arquivo OFX a ser importado", required = true)
            @RequestParam("file") MultipartFile file) {
        return transactionImportService.importOfx(file);
    }
}

