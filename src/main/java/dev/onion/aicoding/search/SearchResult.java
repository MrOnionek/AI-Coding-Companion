package dev.onion.aicoding.search;

import java.nio.file.Path;

public record SearchResult(
        String name,
        Kind kind,
        Path file,
        int declarationLine,
        int score) {

    public enum Kind {
        CLASS,
        INTERFACE,
        RECORD,
        ENUM,
        METHOD,
        PACKAGE,
        FILE
    }

    public SearchResult withScore(int newScore) {
        return new SearchResult(name, kind, file, declarationLine, newScore);
    }
}
