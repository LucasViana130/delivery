package br.com.fiap.delivery.review.dtos.response;

import br.com.fiap.delivery.review.models.ReviewSummary;

public record RankingResponse(Long dishId, String dishName, double average, long count) {

    public static RankingResponse from(ReviewSummary summary) {
        double average = (double) summary.getTotalRating() / summary.getReviewCount();
        return new RankingResponse(
                summary.getDishId(),
                summary.getDishName(),
                Math.round(average * 100.0) / 100.0,
                summary.getReviewCount());
    }
}
