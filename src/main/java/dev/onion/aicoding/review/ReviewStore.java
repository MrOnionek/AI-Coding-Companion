package dev.onion.aicoding.review;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.stream.Stream;

public class ReviewStore {

    public void save(Path projectPath, ReviewRecord review) throws IOException {
        Path directory = reviewDirectory(projectPath);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(review.id() + ".json"), toJson(review),
                StandardCharsets.UTF_8);
    }

    public ReviewHistory load(Path projectPath) throws IOException {
        Path directory = reviewDirectory(projectPath);
        if (!Files.isDirectory(directory)) {
            return ReviewHistory.empty();
        }
        List<ReviewRecord> records = new ArrayList<>();
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                try {
                    records.add(fromJson(Files.readString(file, StandardCharsets.UTF_8)));
                } catch (RuntimeException ignored) {
                    // One damaged review must not hide the remaining history.
                }
            }
        }
        return new ReviewHistory(records);
    }

    private Path reviewDirectory(Path projectPath) {
        return projectPath.resolve(".ai-coding-companion").resolve("reviews");
    }

    private String toJson(ReviewRecord review) {
        String changedFiles = review.changedFiles().stream()
                .map(file -> "\"" + escape(file) + "\"")
                .collect(java.util.stream.Collectors.joining(","));
        String tokens = review.tokenUsage().isPresent()
                ? Long.toString(review.tokenUsage().getAsLong()) : "null";
        return """
                {"id":"%s","timestamp":"%s","changedFiles":[%s],"gitDiffHash":"%s","projectAnalysisSnapshot":"%s","projectMemorySnapshot":"%s","provider":"%s","elapsedTimeMillis":%d,"tokenUsage":%s,"reviewText":"%s","suggestedCodexPrompt":"%s"}\
                """.formatted(escape(review.id()), review.timestamp(), changedFiles,
                escape(review.gitDiffHash()), escape(review.projectAnalysisSnapshot()),
                escape(review.projectMemorySnapshot()), escape(review.provider()),
                review.elapsedTimeMillis(), tokens, escape(review.reviewText()),
                escape(review.suggestedCodexPrompt()));
    }

    private ReviewRecord fromJson(String json) {
        String tokenText = scalar(json, "tokenUsage");
        return new ReviewRecord(
                string(json, "id"), Instant.parse(string(json, "timestamp")),
                stringArray(json, "changedFiles"), string(json, "gitDiffHash"),
                string(json, "projectAnalysisSnapshot"),
                string(json, "projectMemorySnapshot"), string(json, "provider"),
                Long.parseLong(scalar(json, "elapsedTimeMillis")),
                tokenText.equals("null") ? OptionalLong.empty()
                        : OptionalLong.of(Long.parseLong(tokenText)),
                string(json, "reviewText"), string(json, "suggestedCodexPrompt"));
    }

    private String string(String json, String key) {
        int keyStart = json.indexOf("\"" + key + "\"");
        int colon = json.indexOf(':', keyStart);
        int quote = json.indexOf('"', colon);
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int index = quote + 1; index < json.length(); index++) {
            char character = json.charAt(index);
            if (escaped) {
                value.append(switch (character) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
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
        throw new IllegalArgumentException("Invalid JSON string: " + key);
    }

    private String scalar(String json, String key) {
        int keyStart = json.indexOf("\"" + key + "\"");
        int start = json.indexOf(':', keyStart) + 1;
        int end = start;
        while (end < json.length() && ",}".indexOf(json.charAt(end)) < 0) {
            end++;
        }
        return json.substring(start, end).trim();
    }

    private List<String> stringArray(String json, String key) {
        int keyStart = json.indexOf("\"" + key + "\"");
        int start = json.indexOf('[', keyStart);
        int end = json.indexOf(']', start);
        List<String> values = new ArrayList<>();
        int position = start + 1;
        while (position < end) {
            int quote = json.indexOf('"', position);
            if (quote < 0 || quote >= end) {
                break;
            }
            values.add(string("{\"value\":" + json.substring(quote, end) + "}", "value"));
            boolean escaped = false;
            int closing = quote + 1;
            for (; closing < end; closing++) {
                char character = json.charAt(closing);
                if (character == '"' && !escaped) {
                    break;
                }
                escaped = character == '\\' && !escaped;
                if (character != '\\') {
                    escaped = false;
                }
            }
            position = closing + 1;
        }
        return values;
    }

    private String escape(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\")
                .replace("\"", "\\\"").replace("\n", "\\n")
                .replace("\r", "\\r").replace("\t", "\\t");
    }
}
