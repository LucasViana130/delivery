package br.com.fiap.delivery.order.services;

import br.com.fiap.delivery.order.dtos.response.DishResponse;
import br.com.fiap.delivery.order.exceptions.ResourceNotFoundException;
import br.com.fiap.delivery.order.repositories.DishRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DishService {

    private final DishRepository dishRepository;

    public DishService(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    @Transactional(readOnly = true)
    public List<DishResponse> findAll() {
        return dishRepository.findAll(Sort.by("id")).stream()
                .map(DishResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DishResponse findById(Long id) {
        return dishRepository.findById(id)
                .map(DishResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Dish not found"));
    }
}
