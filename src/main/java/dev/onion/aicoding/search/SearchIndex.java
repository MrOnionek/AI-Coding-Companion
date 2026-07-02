package dev.onion.aicoding.search;

import java.util.List;

public record SearchIndex(List<SearchResult> entries) {

    public SearchIndex {
        entries = List.copyOf(entries);
    }

    public static SearchIndex empty() {
        return new SearchIndex(List.of());
    }
}
