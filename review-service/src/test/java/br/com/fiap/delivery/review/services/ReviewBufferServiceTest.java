package br.com.fiap.delivery.review.services;

import br.com.fiap.delivery.review.messaging.ReviewMessage;
import br.com.fiap.delivery.review.models.ReviewSummary;
import br.com.fiap.delivery.review.repositories.ReviewSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewBufferServiceTest {

    private ReviewSummaryRepository repository;
    private ReviewBufferService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ReviewSummaryRepository.class);
        when(repository.findById(any())).thenReturn(Optional.empty());
        service = new ReviewBufferService(repository);
    }

    @Test
    void accumulatesInMemoryWithoutTouchingTheDatabase() {
        service.accept(new ReviewMessage(1L, "House Burger", 5, "Great"));
        service.accept(new ReviewMessage(1L, "House Burger", 4, null));
        service.accept(new ReviewMessage(2L, "Veggie Burger", 3, "Ok"));

        assertThat(service.pendingDishes()).isEqualTo(2);
        Mockito.verifyNoInteractions(repository);
    }

    @Test
    void flushWritesOneAggregatePerDishAndClearsTheBuffer() {
        service.accept(new ReviewMessage(1L, "House Burger", 5, "Great"));
        service.accept(new ReviewMessage(1L, "House Burger", 4, null));
        service.accept(new ReviewMessage(1L, "House Burger", 3, null));

        service.flush();

        ArgumentCaptor<ReviewSummary> captor = ArgumentCaptor.forClass(ReviewSummary.class);
        verify(repository, times(1)).save(captor.capture());
        ReviewSummary saved = captor.getValue();
        assertThat(saved.getDishId()).isEqualTo(1L);
        assertThat(saved.getTotalRating()).isEqualTo(12);
        assertThat(saved.getReviewCount()).isEqualTo(3);
        assertThat(service.pendingDishes()).isZero();

        service.flush(); // empty buffer: no extra write
        verify(repository, times(1)).save(any());
    }

    @Test
    void flushAddsToAnExistingSummary() {
        ReviewSummary existing = new ReviewSummary(1L, "House Burger");
        existing.add("House Burger", 20, 4);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        service.accept(new ReviewMessage(1L, "House Burger", 5, null));
        service.flush();

        assertThat(existing.getTotalRating()).isEqualTo(25);
        assertThat(existing.getReviewCount()).isEqualTo(5);
    }

    @Test
    void failedFlushReturnsTheDataToTheBuffer() {
        service.accept(new ReviewMessage(1L, "House Burger", 5, null));
        when(repository.save(any())).thenThrow(new IllegalStateException("database down"));

        assertThatThrownBy(() -> service.flush()).isInstanceOf(IllegalStateException.class);
        assertThat(service.pendingDishes()).isEqualTo(1);

        Mockito.reset(repository);
        when(repository.findById(any())).thenReturn(Optional.empty());
        service.flush();

        ArgumentCaptor<ReviewSummary> captor = ArgumentCaptor.forClass(ReviewSummary.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getReviewCount()).isEqualTo(1);
    }

    @Test
    void doesNotLoseReviewsWhenThreadsAcceptWhileFlushing() throws Exception {
        int threads = 8;
        int perThread = 5000;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        long[] persisted = {0};
        when(repository.save(any())).thenAnswer(invocation -> {
            ReviewSummary summary = invocation.getArgument(0);
            synchronized (persisted) {
                persisted[0] += summary.getReviewCount();
            }
            return summary;
        });

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                for (int i = 0; i < perThread; i++) {
                    service.accept(new ReviewMessage(1L, "House Burger", 5, null));
                    if (i % 500 == 0) {
                        service.flush();
                    }
                }
            });
        }
        executor.shutdown();
        assertThat(executor.awaitTermination(60, TimeUnit.SECONDS)).isTrue();
        service.flush();

        // every summary object is brand new (findById is empty), so counts add up across flushes
        assertThat(persisted[0]).isEqualTo((long) threads * perThread);
    }
}
