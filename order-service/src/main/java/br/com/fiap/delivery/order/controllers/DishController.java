package br.com.fiap.delivery.order.controllers;

import br.com.fiap.delivery.order.dtos.response.DishResponse;
import br.com.fiap.delivery.order.services.DishService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dishes")
public class DishController {

    private final DishService service;

    public DishController(DishService service) {
        this.service = service;
    }

    @GetMapping
    public List<DishResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public DishResponse findById(@PathVariable("id") Long id) {
        return service.findById(id);
    }
}
