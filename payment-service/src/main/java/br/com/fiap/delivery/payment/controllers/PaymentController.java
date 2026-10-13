package br.com.fiap.delivery.payment.controllers;

import br.com.fiap.delivery.payment.dtos.request.PaymentRequest;
import br.com.fiap.delivery.payment.dtos.response.PaymentResponse;
import br.com.fiap.delivery.payment.services.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    public PaymentResponse pay(@Valid @RequestBody PaymentRequest request) {
        return service.process(request);
    }
}
