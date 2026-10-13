package br.com.fiap.delivery.order.dtos.response;

import br.com.fiap.delivery.order.models.CustomerOrder;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/** createdAt is serialized explicitly as an ISO-8601 local date-time, e.g. 2026-10-08T20:15:00. */
public record OrderResponse(
        Long id,
        Long dishId,
        int quantity,
        BigDecimal totalPrice,
        String status,
        String createdAt
) {

    public static OrderResponse from(CustomerOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getDishId(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus().name(),
                order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}
