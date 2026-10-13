package br.com.fiap.delivery.order.clients;

import br.com.fiap.delivery.order.dtos.request.PaymentRequest;
import br.com.fiap.delivery.order.dtos.response.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);

    private static final String PAYMENT_URL = "http://PAYMENT-SERVICE/payments";

    private final RestTemplate restTemplate;

    public PaymentClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(
            includes = {RestClientException.class, IllegalStateException.class},
            maxRetries = 3,
            delay = 100,
            multiplier = 2.0,
            jitter = 50,
            maxDelay = 1000)
    public PaymentResponse charge(BigDecimal amount) {
        try {
            PaymentResponse response = restTemplate.postForObject(
                    PAYMENT_URL, new PaymentRequest(amount), PaymentResponse.class);
            if (response == null) {
                throw new IllegalStateException("Empty answer from payment-service");
            }
            log.info("Payment {} by payment-service instance {}", response.status(), response.instance());
            return response;
        } catch (RestClientException | IllegalStateException ex) {
            log.warn("Payment attempt failed: {}", ex.getMessage());
            throw ex;
        }
    }
}
