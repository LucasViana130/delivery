package br.com.fiap.delivery.order.dtos.request;

import java.math.BigDecimal;

public record PaymentRequest(BigDecimal amount) {
}
