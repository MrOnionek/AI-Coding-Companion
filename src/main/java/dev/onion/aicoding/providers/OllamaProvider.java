package dev.onion.aicoding.providers;

import dev.onion.aicoding.ai.AIProvider;
import dev.onion.aicoding.ai.AIRequest;
import dev.onion.aicoding.ai.AIResponse;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;

public class OllamaProvider implements AIProvider {

    private static final String PROVIDER_NAME = "Ollama";
    private static final String BASE_URL = "http://localhost:11434/api";
    private static final String MODEL = "qwen2.5-coder:7b";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public AIResponse send(AIRequest request) {
        Instant started = Instant.now();

        try {
            String json = buildRequestJson(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/chat"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            Duration elapsed = Duration.between(started, Instant.now());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new AIResponse(
                        "Ollama request failed. HTTP " + response.statusCode()
                                + "\n\n" + response.body(),
                        elapsed,
                        getName(),
                        OptionalLong.empty()
                );
            }

            String content = extractMessageContent(response.body());

            return new AIResponse(
                    content,
                    elapsed,
                    getName(),
                    OptionalLong.empty()
            );

        } catch (ConnectException e) {
            return new AIResponse(
                    "Ollama is not running. Start Ollama and try again.",
                    Duration.between(started, Instant.now()),
                    getName(),
                    OptionalLong.empty()
            );
        } catch (IOException e) {
            return new AIResponse(
                    "Ollama request failed: " + e.getMessage(),
                    Duration.between(started, Instant.now()),
                    getName(),
                    OptionalLong.empty()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new AIResponse(
                    "Ollama request was interrupted.",
                    Duration.between(started, Instant.now()),
                    getName(),
                    OptionalLong.empty()
            );
        } catch (Exception e) {
            return new AIResponse(
                    "Ollama response parsing failed: " + e.getMessage(),
                    Duration.between(started, Instant.now()),
                    getName(),
                    OptionalLong.empty()
            );
        }
    }

    private String buildRequestJson(AIRequest request) {
        String userPrompt = """
                Project summary:
                %s

                Git diff:
                %s

                User request:
                %s
                """.formatted(
                request.projectSummary(),
                request.gitDiff(),
                request.userPrompt()
        );

        return """
                {
                  "model": "%s",
                  "stream": false,
                  "messages": [
                    {
                      "role": "system",
                      "content": "%s"
                    },
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ]
                }
                """.formatted(
                escapeJson(MODEL),
                escapeJson(request.systemPrompt()),
                escapeJson(userPrompt)
        );
    }

    private String extractMessageContent(String json) {
        String marker = "\"content\":\"";
        int start = json.indexOf(marker);

        if (start < 0) {
            return "Ollama response did not contain message.content:\n\n" + json;
        }

        start += marker.length();
        StringBuilder result = new StringBuilder();
        boolean escaped = false;

        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                switch (c) {
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    default -> result.append(c);
                }
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                break;
            }

            result.append(c);
        }

        return result.toString();
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}