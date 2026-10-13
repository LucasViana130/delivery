package br.com.fiap.delivery.review.controllers;

import br.com.fiap.delivery.review.dtos.response.RankingResponse;
import br.com.fiap.delivery.review.services.RankingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reviews")
public class RankingController {

    private final RankingService service;

    public RankingController(RankingService service) {
        this.service = service;
    }

    @GetMapping("/ranking")
    public List<RankingResponse> ranking() {
        return service.ranking();
    }
}
