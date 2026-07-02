package dev.onion.aicoding.providers;

import dev.onion.aicoding.ai.AIProvider;
import dev.onion.aicoding.ai.AIRequest;
import dev.onion.aicoding.ai.AIResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class ChatGPTProvider implements AIProvider {
    private static final URI RESPONSES_API =
            URI.create("https://api.openai.com/v1/responses");
    private static final Pattern TOTAL_TOKENS =
            Pattern.compile("\"total_tokens\"\\s*:\\s*(\\d+)");
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private final Supplier<String> apiKeySupplier;
    private final IntSupplier timeoutSecondsSupplier;

    public ChatGPTProvider(Supplier<String> apiKeySupplier,
                           IntSupplier timeoutSecondsSupplier) {
        this.apiKeySupplier = apiKeySupplier;
        this.timeoutSecondsSupplier = timeoutSecondsSupplier;
    }

    public String getName() { return "ChatGPT"; }
    public boolean isAvailable() {
        String key = apiKeySupplier.get();
        return key != null && !key.isBlank();
    }

    public AIResponse send(AIRequest request) {
        Instant started = Instant.now();
        if (!isAvailable()) {
            return response("OpenAI request failed: OPENAI_API_KEY is not set.",
                    started, OptionalLong.empty());
        }
        try {
            String body = """
                    {"model":"gpt-4.1-mini","instructions":"%s","input":"%s"}
                    """.formatted(escapeJson(request.systemPrompt()),
                    escapeJson(request.userPrompt()));
            HttpRequest httpRequest = HttpRequest.newBuilder(RESPONSES_API)
                    .timeout(Duration.ofSeconds(timeoutSecondsSupplier.getAsInt()))
                    .header("Authorization", "Bearer " + apiKeySupplier.get())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                String apiMessage = extractJsonString(httpResponse.body(), "message");
                return response("OpenAI request failed (HTTP " + httpResponse.statusCode()
                        + "): " + (apiMessage.isBlank() ? "No error details returned." : apiMessage),
                        started, OptionalLong.empty());
            }
            String text = extractOutputText(httpResponse.body());
            if (text.isBlank()) {
                text = "OpenAI returned no text response.";
            }
            return response(text, started, extractTokenUsage(httpResponse.body()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return response("OpenAI request was interrupted.", started, OptionalLong.empty());
        } catch (Exception e) {
            return response("OpenAI request failed: " + readableMessage(e),
                    started, OptionalLong.empty());
        }
    }

    private AIResponse response(String text, Instant started, OptionalLong tokens) {
        return new AIResponse(text, Duration.between(started, Instant.now()),
                getName(), tokens);
    }

    private String extractOutputText(String json) {
        int position = 0;
        StringBuilder output = new StringBuilder();
        while ((position = json.indexOf("\"type\"", position)) >= 0) {
            int typeValue = json.indexOf("\"output_text\"", position);
            if (typeValue < 0 || typeValue - position > 80) {
                position += 6;
                continue;
            }
            int textKey = json.indexOf("\"text\"", typeValue);
            if (textKey < 0) {
                break;
            }
            String text = extractStringValue(json, textKey);
            if (!text.isBlank()) {
                if (!output.isEmpty()) {
                    output.append('\n');
                }
                output.append(text);
            }
            position = textKey + 6;
        }
        return output.toString();
    }

    private String extractJsonString(String json, String key) {
        int keyPosition = json.indexOf("\"" + key + "\"");
        return keyPosition < 0 ? "" : extractStringValue(json, keyPosition);
    }

    private String extractStringValue(String json, int keyPosition) {
        int colon = json.indexOf(':', keyPosition);
        int quote = colon < 0 ? -1 : json.indexOf('"', colon);
        if (quote < 0) {
            return "";
        }
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = quote + 1; i < json.length(); i++) {
            char character = json.charAt(i);
            if (escaped) {
                value.append(switch (character) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case '"', '\\', '/' -> character;
                    default -> character;
                });
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else if (character == '"') {
                return value.toString();
            } else {
                value.append(character);
            }
        }
        return "";
    }

    private OptionalLong extractTokenUsage(String json) {
        Matcher matcher = TOTAL_TOKENS.matcher(json);
        return matcher.find() ? OptionalLong.of(Long.parseLong(matcher.group(1)))
                : OptionalLong.empty();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String readableMessage(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
