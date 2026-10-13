package br.com.fiap.delivery.order.clients;

import br.com.fiap.delivery.order.dtos.response.PaymentResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifies the @Retryable policy through a real Spring proxy (no Boot, no Eureka, no network). */
class PaymentClientRetryTest {

    @Configuration
    @EnableResilientMethods
    static class TestConfig {

        @Bean
        RestTemplate restTemplate() {
            return Mockito.mock(RestTemplate.class);
        }

        @Bean
        PaymentClient paymentClient(RestTemplate restTemplate) {
            return new PaymentClient(restTemplate);
        }
    }

    @Test
    void retriesAfterFailuresAndEventuallySucceeds() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            RestTemplate restTemplate = context.getBean(RestTemplate.class);
            when(restTemplate.postForObject(anyString(), any(), eq(PaymentResponse.class)))
                    .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                    .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                    .thenReturn(new PaymentResponse("APPROVED", 8082));

            PaymentResponse response = context.getBean(PaymentClient.class).charge(new BigDecimal("79.80"));

            assertThat(response.status()).isEqualTo("APPROVED");
            assertThat(response.instance()).isEqualTo(8082);
            verify(restTemplate, times(3)).postForObject(anyString(), any(), eq(PaymentResponse.class));
        }
    }

    @Test
    void givesUpAfterFourAttemptsAndPropagatesTheFailure() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            RestTemplate restTemplate = context.getBean(RestTemplate.class);
            when(restTemplate.postForObject(anyString(), any(), eq(PaymentResponse.class)))
                    .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

            PaymentClient client = context.getBean(PaymentClient.class);

            assertThatThrownBy(() -> client.charge(new BigDecimal("10.00")))
                    .isInstanceOf(HttpServerErrorException.class);
            // 1 initial attempt + 3 retries
            verify(restTemplate, times(4)).postForObject(anyString(), any(), eq(PaymentResponse.class));
        }
    }
}
