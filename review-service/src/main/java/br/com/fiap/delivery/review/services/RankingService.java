package br.com.fiap.delivery.review.services;

import br.com.fiap.delivery.review.dtos.response.RankingResponse;
import br.com.fiap.delivery.review.repositories.ReviewSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class RankingService {

    private final ReviewSummaryRepository repository;

    public RankingService(ReviewSummaryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<RankingResponse> ranking() {
        Comparator<RankingResponse> byAverage = Comparator.comparingDouble(RankingResponse::average).reversed();
        Comparator<RankingResponse> byCount = Comparator.comparingLong(RankingResponse::count).reversed();

        return repository.findAll().stream()
                .filter(summary -> summary.getReviewCount() > 0)
                .map(RankingResponse::from)
                .sorted(byAverage.thenComparing(byCount))
                .toList();
    }
}
