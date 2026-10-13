package br.com.fiap.delivery.review.repositories;

import br.com.fiap.delivery.review.models.ReviewSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewSummaryRepository extends JpaRepository<ReviewSummary, Long> {
}
