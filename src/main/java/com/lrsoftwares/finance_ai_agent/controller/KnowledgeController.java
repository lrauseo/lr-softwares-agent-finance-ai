package com.lrsoftwares.finance_ai_agent.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.lrsoftwares.finance_ai_agent.dto.rag.CreateKnowledgeChunkRequest;
import com.lrsoftwares.finance_ai_agent.dto.rag.IngestKnowledgeDocumentsResponse;
import com.lrsoftwares.finance_ai_agent.dto.rag.KnowledgeChunkResponse;
import com.lrsoftwares.finance_ai_agent.dto.rag.KnowledgeSearchResponse;
import com.lrsoftwares.finance_ai_agent.entity.rag.KnowledgeChunk;
import com.lrsoftwares.finance_ai_agent.service.rag.KnowledgeIngestionService;
import com.lrsoftwares.finance_ai_agent.service.rag.KnowledgeRetrievalService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeIngestionService ingestionService;
    private final KnowledgeRetrievalService retrievalService;

    @PostMapping("/chunks")
    @ResponseStatus(HttpStatus.CREATED)
    public KnowledgeChunkResponse ingestChunk(@RequestBody @Valid CreateKnowledgeChunkRequest request) {
        KnowledgeChunk chunk = ingestionService.ingest(request);
        return toResponse(chunk);
    }

    @PostMapping(value = "/chunks/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public IngestKnowledgeDocumentsResponse ingestDocuments(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) String theme,
            @RequestParam(required = false) String audience,
            @RequestParam(required = false) String language) {
        List<KnowledgeChunkResponse> chunks = ingestionService.ingestDocuments(files, theme, audience, language)
                .stream()
                .map(this::toResponse)
                .toList();

        return new IngestKnowledgeDocumentsResponse(chunks.size(), chunks);
    }

    @GetMapping("/search")
    public List<KnowledgeSearchResponse> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "pt-BR") String language,
            @RequestParam(required = false) List<String> sources) {
        return retrievalService.search(query, language, sources);               
    }

    private KnowledgeChunkResponse toResponse(KnowledgeChunk chunk) {
        return new KnowledgeChunkResponse(
                chunk.getId(),
                chunk.getSource(),
                chunk.getTheme(),
                chunk.getAudience(),
                chunk.getLanguage(),
                chunk.getContent(),
                chunk.getCreatedAt());
    }
}
