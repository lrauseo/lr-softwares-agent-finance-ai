package com.lrsoftwares.finance_ai_agent.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lrsoftwares.finance_ai_agent.dto.rag.KnowledgeSearchResponse;
import com.lrsoftwares.finance_ai_agent.entity.rag.KnowledgeChunk;
import com.lrsoftwares.finance_ai_agent.service.rag.KnowledgeIngestionService;
import com.lrsoftwares.finance_ai_agent.service.rag.KnowledgeRetrievalService;

@WebMvcTest(KnowledgeController.class)
class KnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KnowledgeIngestionService ingestionService;

    @MockitoBean
    private KnowledgeRetrievalService retrievalService;

    @Test
    void shouldIngestSingleChunk() throws Exception {
        KnowledgeChunk chunk = buildChunk(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "reserva-emergencia.md",
                "RESERVA_EMERGENCIA");

        when(ingestionService.ingest(any())).thenReturn(chunk);

        mockMvc.perform(post("/api/knowledge/chunks")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("""
                                {
                                  "source": "reserva-emergencia.md",
                                  "theme": "RESERVA_EMERGENCIA",
                                  "audience": "BEGINNER",
                                  "language": "pt-BR",
                                  "content": "Conteudo de teste"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .andExpect(jsonPath("$.source").value("reserva-emergencia.md"));

        verify(ingestionService).ingest(any());
    }

    @Test
    void shouldIngestDocumentsList() throws Exception {
        KnowledgeChunk first = buildChunk(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "reserva-emergencia.md",
                "RESERVA_EMERGENCIA");
        KnowledgeChunk second = buildChunk(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                "cartao-credito.md",
                "CARTAO_CREDITO");

        when(ingestionService.ingestDocuments(any(), eq("FINANCAS"), eq("BEGINNER"), eq("pt-BR")))
                .thenReturn(List.of(first, second));

        MockMultipartFile firstFile = new MockMultipartFile(
                "files",
                "reserva-emergencia.md",
                "text/markdown",
                "Conteudo da reserva".getBytes());
        MockMultipartFile secondFile = new MockMultipartFile(
                "files",
                "cartao-credito.md",
                "text/markdown",
                "Conteudo do cartao".getBytes());

        mockMvc.perform(multipart("/api/knowledge/chunks/documents")
                        .file(firstFile)
                        .file(secondFile)
                        .param("theme", "FINANCAS")
                        .param("audience", "BEGINNER")
                        .param("language", "pt-BR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ingestedCount").value(2))
                .andExpect(jsonPath("$.chunks.length()").value(2));

        verify(ingestionService).ingestDocuments(any(), eq("FINANCAS"), eq("BEGINNER"), eq("pt-BR"));
    }

    @Test
    void shouldSearchKnowledgeChunks() throws Exception {
        KnowledgeSearchResponse result = new KnowledgeSearchResponse(
                "Conteudo de teste",
                "reserva-emergencia.md",
                "RESERVA_EMERGENCIA",
                "BEGINNER",
                "pt-BR",
                java.util.Map.of());

        when(retrievalService.search("reserva", "pt-BR", List.of("reserva-emergencia.md")))
                .thenReturn(List.of(result));

        mockMvc.perform(get("/api/knowledge/search")
                        .param("query", "reserva")
                        .param("language", "pt-BR")
                        .param("sources", "reserva-emergencia.md"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].theme").value("RESERVA_EMERGENCIA"));

        verify(retrievalService).search("reserva", "pt-BR", List.of("reserva-emergencia.md"));
    }

    private KnowledgeChunk buildChunk(UUID id, String source, String theme) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(id);
        chunk.setSource(source);
        chunk.setTheme(theme);
        chunk.setAudience("BEGINNER");
        chunk.setLanguage("pt-BR");
        chunk.setContent("Conteudo de teste");
        chunk.setCreatedAt(LocalDateTime.of(2026, 4, 24, 10, 0));
        return chunk;
    }
}
