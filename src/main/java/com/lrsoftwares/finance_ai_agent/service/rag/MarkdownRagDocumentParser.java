package com.lrsoftwares.finance_ai_agent.service.rag;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MarkdownRagDocumentParser {

	private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile(
			"^---\\s*\\R(.*?)\\R---\\s*\\R",
			Pattern.DOTALL);

	private static final Pattern CHUNK_PATTERN = Pattern.compile(
			"##\\s+(chunk_[\\w-]+)\\s*\\R---\\s*\\R(.*?)\\R---\\s*\\R(.*?)(?=\\R##\\s+chunk_|\\z)",
			Pattern.DOTALL);

	public List<Document> parse(Resource resource) {
		try {
			String filename = Objects.requireNonNullElse(resource.getFilename(), "unknown.md");
			String markdown = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

			Map<String, Object> documentMetadata = extractDocumentMetadata(markdown);
			String contentWithoutFrontMatter = removeFrontMatter(markdown);

			List<Document> documents = new ArrayList<>();

			Matcher matcher = CHUNK_PATTERN.matcher(contentWithoutFrontMatter);

			while (matcher.find()) {
				String chunkName = matcher.group(1).trim();
				String chunkMetadataRaw = matcher.group(2).trim();
				String chunkContent = cleanContent(matcher.group(3));

				if (chunkContent.isBlank()) {
					continue;
				}

				Map<String, Object> metadata = new LinkedHashMap<>();
				metadata.putAll(documentMetadata);
				metadata.putAll(parseYamlLikeMetadata(chunkMetadataRaw));

				metadata.putIfAbsent("source", filename);
				metadata.put("filename", filename);
				metadata.put("chunk_name", chunkName);

				validateRequiredMetadata(metadata, filename, chunkName);

				documents.add(new Document(chunkContent, metadata));
			}

			return documents;

		} catch (IOException e) {
			throw new IllegalStateException("Erro ao ler arquivo markdown RAG: " + resource.getFilename(), e);
		}
	}

	private Map<String, Object> extractDocumentMetadata(String markdown) {
		Matcher matcher = FRONT_MATTER_PATTERN.matcher(markdown);

		if (!matcher.find()) {
			return new LinkedHashMap<>();
		}

		return parseYamlLikeMetadata(matcher.group(1));
	}

	private String removeFrontMatter(String markdown) {
		return FRONT_MATTER_PATTERN.matcher(markdown).replaceFirst("");
	}

	private Map<String, Object> parseYamlLikeMetadata(String raw) {
		Map<String, Object> metadata = new LinkedHashMap<>();

		String[] lines = raw.split("\\R");

		for (String line : lines) {
			String trimmed = line.trim();

			if (trimmed.isBlank() || trimmed.startsWith("#")) {
				continue;
			}

			int separatorIndex = trimmed.indexOf(":");

			if (separatorIndex <= 0) {
				continue;
			}

			String key = trimmed.substring(0, separatorIndex).trim();
			String value = trimmed.substring(separatorIndex + 1).trim();

			metadata.put(key, parseValue(value));
		}

		return metadata;
	}

	private Object parseValue(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}

		if (value.startsWith("[") && value.endsWith("]")) {
			String inside = value.substring(1, value.length() - 1).trim();

			if (inside.isBlank()) {
				return List.of();
			}

			return Arrays.stream(inside.split(","))
					.map(String::trim)
					.map(item -> item.replace("\"", "").replace("'", ""))
					.filter(item -> !item.isBlank())
					.toList();
		}

		return value.replace("\"", "").replace("'", "");
	}

	private String cleanContent(String content) {
		return content
				.replaceAll("(?m)^\\s*---\\s*$", "")
				.replaceAll("\\R{3,}", "\n\n")
				.trim();
	}

	private void validateRequiredMetadata(Map<String, Object> metadata, String filename, String chunkName) {
		List<String> required = List.of("theme", "audience", "language", "source", "topic", "id");

		List<String> missing = required.stream()
				.filter(key -> !metadata.containsKey(key) || metadata.get(key) == null
						|| metadata.get(key).toString().isBlank())
				.toList();

		if (!missing.isEmpty()) {
			throw new IllegalArgumentException(
					"Metadata obrigatória ausente no arquivo %s, chunk %s: %s"
							.formatted(filename, chunkName, missing));
		}
	}
}