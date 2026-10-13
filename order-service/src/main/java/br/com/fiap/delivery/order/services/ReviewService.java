package br.com.fiap.delivery.order.services;

import br.com.fiap.delivery.order.config.RabbitConfig;
import br.com.fiap.delivery.order.dtos.request.ReviewRequest;
import br.com.fiap.delivery.order.exceptions.ResourceNotFoundException;
import br.com.fiap.delivery.order.messaging.ReviewMessage;
import br.com.fiap.delivery.order.models.Dish;
import br.com.fiap.delivery.order.repositories.DishRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final DishRepository dishRepository;
    private final RabbitTemplate rabbitTemplate;

    public ReviewService(DishRepository dishRepository, RabbitTemplate rabbitTemplate) {
        this.dishRepository = dishRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(ReviewRequest request) {
        Dish dish = dishRepository.findById(request.dishId())
                .orElseThrow(() -> new ResourceNotFoundException("Dish not found"));

        ReviewMessage message = new ReviewMessage(dish.getId(), dish.getName(), request.rating(), request.comment());
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, message);

        log.info("Review published: dish {} rating {}", dish.getId(), request.rating());
    }
}
