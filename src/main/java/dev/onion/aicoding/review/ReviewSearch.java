package dev.onion.aicoding.review;

public class ReviewSearch {

    public ReviewHistory search(ReviewHistory history, String query, String provider) {
        String normalizedQuery = query == null ? "" : query.toLowerCase();
        String normalizedProvider = provider == null ? "" : provider;
        return new ReviewHistory(history.reviews().stream()
                .filter(review -> normalizedProvider.isBlank()
                        || normalizedProvider.equals("All providers")
                        || review.provider().equals(normalizedProvider))
                .filter(review -> normalizedQuery.isBlank()
                        || searchableText(review).toLowerCase().contains(normalizedQuery))
                .toList());
    }

    private String searchableText(ReviewRecord review) {
        return review.reviewText() + "\n" + review.suggestedCodexPrompt()
                + "\n" + review.projectAnalysisSnapshot() + "\n"
                + String.join("\n", review.changedFiles());
    }
}
