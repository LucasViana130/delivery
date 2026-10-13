package br.com.fiap.delivery.order.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantRequest(
        @NotBlank(message = "question is required")
        @Size(max = 1000, message = "question must have at most 1000 characters")
        String question
) {
}
