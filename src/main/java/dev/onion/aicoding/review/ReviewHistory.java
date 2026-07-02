package dev.onion.aicoding.review;

import java.util.Comparator;
import java.util.List;

public record ReviewHistory(List<ReviewRecord> reviews) {

    public ReviewHistory {
        reviews = reviews.stream()
                .sorted(Comparator.comparing(ReviewRecord::timestamp).reversed())
                .toList();
    }

    public static ReviewHistory empty() {
        return new ReviewHistory(List.of());
    }
}
