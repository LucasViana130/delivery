package br.com.fiap.delivery.payment.services;

import br.com.fiap.delivery.payment.dtos.request.PaymentRequest;
import br.com.fiap.delivery.payment.dtos.response.PaymentResponse;
import br.com.fiap.delivery.payment.exceptions.PaymentFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final Random random = new Random();
    private final double failureRate;
    private final int port;

    public PaymentService(@Value("${payment.failure-rate:0.5}") double failureRate,
                          @Value("${server.port}") int port) {
        this.failureRate = failureRate;
        this.port = port;
    }

    public PaymentResponse process(PaymentRequest request) {
        if (random.nextDouble() < failureRate) {
            log.warn("[payment-service:{}] simulated failure for amount {}", port, request.amount());
            throw new PaymentFailureException("Simulated payment failure on instance " + port);
        }
        log.info("[payment-service:{}] payment APPROVED for amount {}", port, request.amount());
        return new PaymentResponse("APPROVED", port);
    }
}
