package br.com.fiap.delivery.order.controllers;

import br.com.fiap.delivery.order.dtos.request.ReviewRequest;
import br.com.fiap.delivery.order.services.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> publish(@Valid @RequestBody ReviewRequest request) {
        service.publish(request);
        return ResponseEntity.accepted().build();
    }
}
