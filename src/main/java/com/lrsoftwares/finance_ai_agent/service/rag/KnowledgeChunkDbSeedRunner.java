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

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KnowledgeChunkDbSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeChunkDbSeedRunner.class);

    private final KnowledgeIngestionService ingestionService;

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

    @Value("${app.knowledge.seed.delay-ms:4500}")
    private long delayBetweenFilesMs;

    @Override
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

        log.info("Iniciando seed de knowledge. Arquivos encontrados: {} | Delay entre arquivos: {}ms",
                markdownFiles.size(), delayBetweenFilesMs);

        int inserted = 0;
        int skipped = 0;

        for (Path file : markdownFiles) {
            String source = file.getFileName().toString();

            try {
                if (isBlankFile(file, source)) {
                    skipped++;
                    continue;
                }

                int previousCount = ingestionService.ingestDocumentsFromRepository(
                        List.of(source),
                        resolveTheme(source),
                        defaultAudience,
                        defaultLanguage,
                        overwrite)
                        .size();

                inserted += previousCount;
            } catch (RuntimeException ex) {
                log.warn("Falha ao ingerir arquivo de seed: {}", source, ex);
            }

            if (delayBetweenFilesMs > 0) {
                try {
                    Thread.sleep(delayBetweenFilesMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Seed interrompido durante espera entre arquivos.");
                    break;
                }
            }
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

    private boolean isBlankFile(Path file, String source) {
        String content;
        try {
            content = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.warn("Falha ao ler arquivo de seed: {}", source, ex);
            return true;
        }

        if (content.isBlank()) {
            log.warn("Arquivo de seed vazio ignorado: {}", source);
            return true;
        }

        return false;
    }
}
