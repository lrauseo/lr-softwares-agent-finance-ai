package com.lrsoftwares.finance_ai_agent.service.rag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.lrsoftwares.finance_ai_agent.dto.rag.CreateKnowledgeChunkRequest;
import com.lrsoftwares.finance_ai_agent.entity.rag.KnowledgeChunk;
import com.lrsoftwares.finance_ai_agent.exception.BusinessException;
import com.lrsoftwares.finance_ai_agent.exception.ResourceNotFoundException;
import com.lrsoftwares.finance_ai_agent.repository.rag.KnowledgeChunkRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KnowledgeIngestionService {

    private static final String DEFAULT_LANGUAGE = "pt-BR";
    private static final String DEFAULT_AUDIENCE = "BEGINNER";

    private final KnowledgeChunkRepository repository;

    @Value("${app.knowledge.docs-path:docs/rag}")
    private String docsPath;

    public KnowledgeChunk ingest(CreateKnowledgeChunkRequest request) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setSource(request.source());
        chunk.setTheme(request.theme());
        chunk.setAudience(request.audience());
        chunk.setLanguage(request.language());
        chunk.setContent(request.content());
        return repository.save(chunk);
    }

    public List<KnowledgeChunk> ingestDocuments(List<MultipartFile> files, String theme, String audience, String language) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException("Informe ao menos um arquivo para ingestao");
        }

        List<KnowledgeChunk> chunks = new ArrayList<>();
        for (MultipartFile file : files) {
            String normalizedSource = validateMultipartSource(file);
            if (file.isEmpty()) {
                throw new BusinessException("Arquivo vazio nao pode ser ingerido: " + normalizedSource);
            }

            String content = readMultipartContent(file, normalizedSource);
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setSource(normalizedSource);
            chunk.setTheme(resolveTheme(theme, normalizedSource));
            chunk.setAudience(resolveAudience(audience));
            chunk.setLanguage(resolveLanguage(language));
            chunk.setContent(content);
            chunks.add(repository.save(chunk));
        }
        return chunks;
    }

    public List<KnowledgeChunk> ingestDocumentsFromRepository(
            List<String> sources,
            String theme,
            String audience,
            String language) {
        if (sources == null || sources.isEmpty()) {
            throw new BusinessException("Informe ao menos um documento para ingestao");
        }

        List<KnowledgeChunk> chunks = new ArrayList<>();
        for (String source : sources) {
            String normalizedSource = validateSource(source);
            Path filePath = Path.of(docsPath, normalizedSource);
            if (!Files.exists(filePath)) {
                throw new ResourceNotFoundException("Documento nao encontrado: " + normalizedSource);
            }

            String content = readContent(filePath, normalizedSource);
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setSource(normalizedSource);
            chunk.setTheme(resolveTheme(theme, normalizedSource));
            chunk.setAudience(resolveAudience(audience));
            chunk.setLanguage(resolveLanguage(language));
            chunk.setContent(content);
            chunks.add(repository.save(chunk));
        }
        return chunks;
    }

    private String validateSource(String source) {
        if (source == null || source.isBlank()) {
            throw new BusinessException("A lista de documentos possui item vazio");
        }
        if (!source.endsWith(".md")) {
            throw new BusinessException("Somente arquivos .md sao permitidos: " + source);
        }
        if (source.contains("..") || source.contains("/") || source.contains("\\")) {
            throw new BusinessException("Nome de documento invalido: " + source);
        }
        return source;
    }

    private String validateMultipartSource(MultipartFile file) {
        if (file == null) {
            throw new BusinessException("A lista de arquivos possui item nulo");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("Arquivo enviado sem nome");
        }

        String fileName = Path.of(originalFilename).getFileName().toString();
        return validateSource(fileName);
    }

    private String readContent(Path filePath, String source) {
        try {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new BusinessException("Nao foi possivel ler o documento: " + source);
        }
    }

    private String readMultipartContent(MultipartFile file, String source) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new BusinessException("Nao foi possivel ler o documento: " + source);
        }
    }

    private String resolveTheme(String requestTheme, String source) {
        if (requestTheme != null && !requestTheme.isBlank()) {
            return requestTheme;
        }
        String baseName = source.replace(".md", "");
        return baseName.replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private String resolveAudience(String audience) {
        if (audience == null || audience.isBlank()) {
            return DEFAULT_AUDIENCE;
        }
        return audience;
    }

    private String resolveLanguage(String language) {
        if (language == null || language.isBlank()) {
            return DEFAULT_LANGUAGE;
        }
        return language;
    }
}
