package br.com.fiap.delivery.payment.services;

import br.com.fiap.delivery.payment.dtos.request.PaymentRequest;
import br.com.fiap.delivery.payment.dtos.response.PaymentResponse;
import br.com.fiap.delivery.payment.exceptions.PaymentFailureException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceTest {

    private static final PaymentRequest REQUEST = new PaymentRequest(new BigDecimal("79.80"));

    @Test
    void alwaysApprovesWhenFailureRateIsZero() {
        PaymentService service = new PaymentService(0.0, 8081);

        for (int i = 0; i < 100; i++) {
            PaymentResponse response = service.process(REQUEST);
            assertThat(response.status()).isEqualTo("APPROVED");
            assertThat(response.instance()).isEqualTo(8081);
        }
    }

    @Test
    void alwaysFailsWhenFailureRateIsOne() {
        PaymentService service = new PaymentService(1.0, 8082);

        assertThatThrownBy(() -> service.process(REQUEST)).isInstanceOf(PaymentFailureException.class);
    }

    @Test
    void failsRoughlyHalfOfTheTimeByDefault() {
        PaymentService service = new PaymentService(0.5, 8081);
        int failures = 0;
        int attempts = 4000;

        for (int i = 0; i < attempts; i++) {
            try {
                service.process(REQUEST);
            } catch (PaymentFailureException ex) {
                failures++;
            }
        }

        assertThat(failures).isBetween((int) (attempts * 0.4), (int) (attempts * 0.6));
    }
}
