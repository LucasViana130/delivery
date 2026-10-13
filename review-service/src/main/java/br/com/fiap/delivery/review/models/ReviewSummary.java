package br.com.fiap.delivery.review.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "review_summaries")
public class ReviewSummary {

    @Id
    private Long dishId;

    @Column(nullable = false)
    private String dishName;

    @Column(nullable = false)
    private long totalRating;

    @Column(nullable = false)
    private long reviewCount;

    protected ReviewSummary() {
        // required by JPA
    }

    public ReviewSummary(Long dishId, String dishName) {
        this.dishId = dishId;
        this.dishName = dishName;
    }

    public void add(String latestDishName, long ratingSum, long count) {
        this.dishName = latestDishName;
        this.totalRating += ratingSum;
        this.reviewCount += count;
    }

    public Long getDishId() {
        return dishId;
    }

    public String getDishName() {
        return dishName;
    }

    public long getTotalRating() {
        return totalRating;
    }

    public long getReviewCount() {
        return reviewCount;
    }
}
