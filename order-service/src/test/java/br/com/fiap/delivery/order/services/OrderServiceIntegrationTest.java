package br.com.fiap.delivery.order.services;

import br.com.fiap.delivery.order.clients.PaymentClient;
import br.com.fiap.delivery.order.dtos.request.OrderRequest;
import br.com.fiap.delivery.order.dtos.response.PaymentResponse;
import br.com.fiap.delivery.order.exceptions.OutOfStockException;
import br.com.fiap.delivery.order.exceptions.PaymentUnavailableException;
import br.com.fiap.delivery.order.exceptions.ResourceNotFoundException;
import br.com.fiap.delivery.order.repositories.CustomerOrderRepository;
import br.com.fiap.delivery.order.repositories.DishRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Real H2 database and real transactions; only the payment call is replaced by a mock. */
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private CustomerOrderRepository orderRepository;

    @MockitoBean
    private PaymentClient paymentClient;

    /** Promotion dish (id 1) starts with 10 units: 50 simultaneous orders must confirm exactly 10. */
    @Test
    void fiftyConcurrentOrdersConfirmExactlyTenAndStockEndsAtZero() throws Exception {
        when(paymentClient.charge(any())).thenReturn(new PaymentResponse("APPROVED", 8081));
        int requests = 50;
        long ordersBefore = orderRepository.count();

        AtomicInteger confirmed = new AtomicInteger();
        AtomicInteger outOfStock = new AtomicInteger();
        CountDownLatch startSignal = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(requests);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            Callable<Void> task = () -> {
                startSignal.await();
                try {
                    orderService.createOrder(new OrderRequest(1L, 1));
                    confirmed.incrementAndGet();
                } catch (OutOfStockException ex) {
                    outOfStock.incrementAndGet();
                }
                return null;
            };
            futures.add(executor.submit(task));
        }

        startSignal.countDown();
        for (Future<?> future : futures) {
            future.get(60, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(confirmed.get()).isEqualTo(10);
        assertThat(outOfStock.get()).isEqualTo(40);
        assertThat(dishRepository.findById(1L).orElseThrow().getStock()).isZero();
        assertThat(orderRepository.count() - ordersBefore).isEqualTo(10);
    }

    @Test
    void paymentFailureRollsBackAndKeepsTheStockIntact() {
        when(paymentClient.charge(any()))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
        int stockBefore = dishRepository.findById(2L).orElseThrow().getStock();
        long ordersBefore = orderRepository.count();

        assertThatThrownBy(() -> orderService.createOrder(new OrderRequest(2L, 3)))
                .isInstanceOf(PaymentUnavailableException.class);

        assertThat(dishRepository.findById(2L).orElseThrow().getStock()).isEqualTo(stockBefore);
        assertThat(orderRepository.count()).isEqualTo(ordersBefore);
    }

    @Test
    void unknownDishIsNotFound() {
        assertThatThrownBy(() -> orderService.createOrder(new OrderRequest(9999L, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
