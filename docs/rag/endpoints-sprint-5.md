---
title: Endpoints uteis para Sprint 5
source: Documentacao Interna
year: 2026
theme: endpoints
audience: geral
language: pt-BR
tags: [endpoints, sprint, educacao_financeira]
---

## chunk_1
---
id: endpoints_sprint_5_001
topic: ingerir_documento_manualmente
---

`POST /api/knowledge/chunks`

Payload de exemplo:

```json
{
  "source": "reserva-emergencia.md",
  "theme": "RESERVA_EMERGENCIA",
  "audience": "BEGINNER",
  "language": "pt-BR",
  "content": "Reserva de emergencia e uma quantia separada para cobrir imprevistos..."
}
```

:contentReference[oaicite:0]{index=0}

---

## chunk_2
---
id: endpoints_sprint_5_002
topic: buscar_chunks
---

`GET /api/knowledge/search?query=reserva de emergencia`

Com filtro opcional por arquivos fonte:

`GET /api/knowledge/search?query=reserva&sources=reserva-emergencia.md&language=pt-BR`

:contentReference[oaicite:1]{index=1}

---

## chunk_3
---
id: endpoints_sprint_5_003
topic: ingerir_lista_de_documentos_md
---

`POST /api/knowledge/chunks/documents`

Formato: `multipart/form-data`

Campos:

- `files`: lista de arquivos `.md` externos (List<MultipartFile>)
- `theme` (opcional)
- `audience` (opcional, default `BEGINNER`)
- `language` (opcional, default `pt-BR`)

Exemplo curl:

```bash
curl -X POST "http://localhost:8080/api/knowledge/chunks/documents" \
  -F "files=@docs/rag/reserva-emergencia.md" \
  -F "files=@docs/rag/cartao-credito.md" \
  -F "theme=FINANCAS" \
  -F "audience=BEGINNER" \
  -F "language=pt-BR"
```

Observacoes:

- Os arquivos sao enviados externamente no request.
- `theme` e opcional nesse endpoint; se nao for informado, e derivado do nome do arquivo.
- Somente nomes de arquivos `.md` sao aceitos.

:contentReference[oaicite:2]{index=2}

---

## chunk_4
---
id: endpoints_sprint_5_004
topic: fluxo_rapido_recomendado
---

1. Criar ou atualizar arquivos em `docs/rag`.
2. Enviar conteudo por `POST /api/knowledge/chunks` (manual) ou `POST /api/knowledge/chunks/documents` (lote por lista).
3. Validar retorno com `GET /api/knowledge/search`.

:contentReference[oaicite:3]{index=3}

---
