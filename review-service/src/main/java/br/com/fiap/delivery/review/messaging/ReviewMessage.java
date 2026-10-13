package br.com.fiap.delivery.review.messaging;

public record ReviewMessage(Long dishId, String dishName, int rating, String comment) {
}
