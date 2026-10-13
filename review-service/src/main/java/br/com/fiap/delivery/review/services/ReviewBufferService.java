package br.com.fiap.delivery.review.services;

import br.com.fiap.delivery.review.messaging.ReviewMessage;
import br.com.fiap.delivery.review.models.ReviewSummary;
import br.com.fiap.delivery.review.repositories.ReviewSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReviewBufferService {

    private static final Logger log = LoggerFactory.getLogger(ReviewBufferService.class);

    private record Accumulator(String dishName, long ratingSum, long count) {

        Accumulator plus(Accumulator other) {
            return new Accumulator(other.dishName, ratingSum + other.ratingSum, count + other.count);
        }
    }

    private final ConcurrentHashMap<Long, Accumulator> buffer = new ConcurrentHashMap<>();
    private final ReviewSummaryRepository repository;

    public ReviewBufferService(ReviewSummaryRepository repository) {
        this.repository = repository;
    }

    public void accept(ReviewMessage message) {
        buffer.merge(message.dishId(),
                new Accumulator(message.dishName(), message.rating(), 1),
                Accumulator::plus);
    }

    public int pendingDishes() {
        return buffer.size();
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void flush() {
        if (buffer.isEmpty()) {
            return;
        }

        Map<Long, Accumulator> snapshot = new HashMap<>();
        for (Long dishId : List.copyOf(buffer.keySet())) {
            Accumulator accumulator = buffer.remove(dishId);
            if (accumulator != null) {
                snapshot.put(dishId, accumulator);
            }
        }

        try {
            snapshot.forEach((dishId, accumulator) -> {
                ReviewSummary summary = repository.findById(dishId)
                        .orElseGet(() -> new ReviewSummary(dishId, accumulator.dishName()));
                summary.add(accumulator.dishName(), accumulator.ratingSum(), accumulator.count());
                repository.save(summary);
            });
            log.info("Flushed {} dish aggregate(s) to the database", snapshot.size());
        } catch (RuntimeException ex) {
            snapshot.forEach((dishId, accumulator) -> buffer.merge(dishId, accumulator, Accumulator::plus));
            log.error("Flush failed, {} aggregate(s) returned to the buffer: {}", snapshot.size(), ex.getMessage());
            throw ex;
        }
    }
}
