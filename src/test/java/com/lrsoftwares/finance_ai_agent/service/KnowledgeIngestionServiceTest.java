package com.lrsoftwares.finance_ai_agent.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import com.lrsoftwares.finance_ai_agent.entity.rag.KnowledgeChunk;
import com.lrsoftwares.finance_ai_agent.repository.rag.KnowledgeChunkRepository;
import com.lrsoftwares.finance_ai_agent.service.rag.KnowledgeIngestionService;

@ExtendWith(MockitoExtension.class)
class KnowledgeIngestionServiceTest {

	@Mock
	private KnowledgeChunkRepository repository;

	@Mock
	private VectorStore vectorStore;

	@InjectMocks
	private KnowledgeIngestionService service;

	@Test
	void shouldOverwriteRepositoryDocumentInDatabaseAndVectorStore(@TempDir Path tempDir) throws Exception {
		Path docsPath = tempDir.resolve("docs");
		Files.createDirectories(docsPath);
		Files.writeString(docsPath.resolve("reserva-emergencia.md"), "Conteudo para ingestao");
		ReflectionTestUtils.setField(service, "docsPath", docsPath.toString());

		KnowledgeChunk savedChunk = new KnowledgeChunk();
		savedChunk.setSource("reserva-emergencia.md");
		savedChunk.setTheme("RESERVA_EMERGENCIA");
		savedChunk.setAudience("BEGINNER");
		savedChunk.setLanguage("pt-BR");
		savedChunk.setContent("Conteudo para ingestao");

		when(repository.save(any(KnowledgeChunk.class))).thenReturn(savedChunk);

		List<KnowledgeChunk> chunks = service.ingestDocumentsFromRepository(
				List.of("reserva-emergencia.md"),
				null,
				null,
				null,
				true);

		verify(vectorStore).delete("source == 'reserva-emergencia.md'");
		verify(repository).deleteBySource("reserva-emergencia.md");
		verify(vectorStore, times(1)).add(anyList());
		verify(repository).save(any(KnowledgeChunk.class));
		org.junit.jupiter.api.Assertions.assertEquals(1, chunks.size());
		org.junit.jupiter.api.Assertions.assertEquals("reserva-emergencia.md", chunks.get(0).getSource());
		org.junit.jupiter.api.Assertions.assertEquals("RESERVA_EMERGENCIA", chunks.get(0).getTheme());
		verify(repository).save(any(KnowledgeChunk.class));
	}
}