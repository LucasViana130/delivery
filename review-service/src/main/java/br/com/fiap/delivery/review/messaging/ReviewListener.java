package br.com.fiap.delivery.review.messaging;

import br.com.fiap.delivery.review.config.RabbitConfig;
import br.com.fiap.delivery.review.services.ReviewBufferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReviewListener {

    private static final Logger log = LoggerFactory.getLogger(ReviewListener.class);

    private final ReviewBufferService bufferService;

    public ReviewListener(ReviewBufferService bufferService) {
        this.bufferService = bufferService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onReview(ReviewMessage message) {
        if (message.dishId() == null || message.rating() < 1 || message.rating() > 5) {
            log.warn("Discarding invalid review message: {}", message);
            return;
        }
        bufferService.accept(message);
    }
}
