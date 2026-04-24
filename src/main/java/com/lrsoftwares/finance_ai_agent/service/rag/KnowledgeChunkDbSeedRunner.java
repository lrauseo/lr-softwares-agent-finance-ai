package com.lrsoftwares.finance_ai_agent.service.rag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.lrsoftwares.finance_ai_agent.entity.rag.KnowledgeChunk;
import com.lrsoftwares.finance_ai_agent.repository.rag.KnowledgeChunkRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KnowledgeChunkDbSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeChunkDbSeedRunner.class);

    private final KnowledgeChunkRepository repository;

    @Value("${app.knowledge.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.knowledge.docs-path:docs/rag}")
    private String docsPath;

    @Value("${app.knowledge.seed.language:pt-BR}")
    private String defaultLanguage;

    @Value("${app.knowledge.seed.audience:BEGINNER}")
    private String defaultAudience;

    @Value("${app.knowledge.seed.overwrite:true}")
    private boolean overwrite;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Knowledge DB seed desabilitado.");
            return;
        }

        Path directory = Path.of(docsPath);
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            log.warn("Diretorio de seed nao encontrado: {}", directory.toAbsolutePath());
            return;
        }

        List<Path> markdownFiles;
        try (var stream = Files.list(directory)) {
            markdownFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md"))
                    .sorted()
                    .toList();
        } catch (IOException ex) {
            log.error("Falha ao listar arquivos para seed em {}", directory.toAbsolutePath(), ex);
            return;
        }

        int inserted = 0;
        int skipped = 0;

        for (Path file : markdownFiles) {
            String source = file.getFileName().toString();

            if (!overwrite && repository.existsBySource(source)) {
                skipped++;
                continue;
            }

            String content;
            try {
                content = Files.readString(file, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                log.warn("Falha ao ler arquivo de seed: {}", source, ex);
                continue;
            }

            if (content.isBlank()) {
                log.warn("Arquivo de seed vazio ignorado: {}", source);
                skipped++;
                continue;
            }

            if (overwrite) {
                repository.deleteBySource(source);
            }

            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setSource(source);
            chunk.setTheme(resolveTheme(source));
            chunk.setAudience(defaultAudience);
            chunk.setLanguage(defaultLanguage);
            chunk.setContent(content);
            repository.save(chunk);
            inserted++;
        }

        log.info("Seed de knowledge finalizado. Inseridos: {} | Ignorados: {} | Diretorio: {}",
                inserted,
                skipped,
                directory.toAbsolutePath());
    }

    private String resolveTheme(String source) {
        String baseName = source.endsWith(".md") ? source.substring(0, source.length() - 3) : source;
        return baseName.replace('-', '_').toUpperCase(Locale.ROOT);
    }
}
