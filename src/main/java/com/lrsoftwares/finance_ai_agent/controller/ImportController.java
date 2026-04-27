package com.lrsoftwares.finance_ai_agent.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.lrsoftwares.finance_ai_agent.dto.sprint8.ImportTransactionsResponse;
import com.lrsoftwares.finance_ai_agent.service.sprint8.TransactionImportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final TransactionImportService transactionImportService;

    @PostMapping(path = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportTransactionsResponse importCsv(@RequestParam("file") MultipartFile file) {
        return transactionImportService.importCsv(file);
    }

    @PostMapping(path = "/ofx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportTransactionsResponse importOfx(@RequestParam("file") MultipartFile file) {
        return transactionImportService.importOfx(file);
    }
}
