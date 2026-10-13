package br.com.fiap.delivery.order.messaging;

public record ReviewMessage(Long dishId, String dishName, int rating, String comment) {
}
