package br.com.fiap.delivery.order.services;

import br.com.fiap.delivery.order.clients.PaymentClient;
import br.com.fiap.delivery.order.dtos.request.OrderRequest;
import br.com.fiap.delivery.order.dtos.response.OrderResponse;
import br.com.fiap.delivery.order.exceptions.OutOfStockException;
import br.com.fiap.delivery.order.exceptions.PaymentUnavailableException;
import br.com.fiap.delivery.order.exceptions.ResourceNotFoundException;
import br.com.fiap.delivery.order.models.CustomerOrder;
import br.com.fiap.delivery.order.models.Dish;
import br.com.fiap.delivery.order.models.OrderStatus;
import br.com.fiap.delivery.order.repositories.CustomerOrderRepository;
import br.com.fiap.delivery.order.repositories.DishRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final DishRepository dishRepository;
    private final CustomerOrderRepository orderRepository;
    private final PaymentClient paymentClient;

    public OrderService(DishRepository dishRepository,
                        CustomerOrderRepository orderRepository,
                        PaymentClient paymentClient) {
        this.dishRepository = dishRepository;
        this.orderRepository = orderRepository;
        this.paymentClient = paymentClient;
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        Dish dish = dishRepository.findByIdForUpdate(request.dishId())
                .orElseThrow(() -> new ResourceNotFoundException("Dish not found"));

        int quantity = request.quantity();
        if (dish.getStock() < quantity) {
            throw new OutOfStockException("Dish out of stock");
        }

        dish.setStock(dish.getStock() - quantity);
        BigDecimal totalPrice = dish.getPrice().multiply(BigDecimal.valueOf(quantity));

        try {
            paymentClient.charge(totalPrice);
        } catch (RuntimeException ex) {
            throw new PaymentUnavailableException("Payment service unavailable", ex);
        }

        CustomerOrder order = orderRepository.save(new CustomerOrder(
                dish.getId(),
                quantity,
                totalPrice,
                OrderStatus.CONFIRMED,
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));

        log.info("Order {} confirmed: dish {} x{} = {}", order.getId(), dish.getId(), quantity, totalPrice);
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return orderRepository.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }
}
