package com.lrsoftwares.finance_ai_agent.controller;

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

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final TransactionImportService transactionImportService;
    private final ObjectMapper objectMapper;

    @PostMapping(path = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportTransactionsResponse importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "columnMapping", required = false) String columnMappingJson) {
        CsvColumnMapping mapping = null;
        if (columnMappingJson != null && !columnMappingJson.isBlank()) {
            try {
                mapping = objectMapper.readValue(columnMappingJson, CsvColumnMapping.class);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("columnMapping invalido: " + e.getOriginalMessage(), e);
            }
        }
        return transactionImportService.importCsv(file, mapping);
    }

    @PostMapping(path = "/ofx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportTransactionsResponse importOfx(@RequestParam("file") MultipartFile file) {
        return transactionImportService.importOfx(file);
    }
}

