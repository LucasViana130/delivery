package br.com.fiap.delivery.order.config;

import br.com.fiap.delivery.order.models.Dish;
import br.com.fiap.delivery.order.repositories.DishRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final DishRepository dishRepository;

    public DataLoader(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    @Override
    public void run(String... args) {
        if (dishRepository.count() > 0) {
            return;
        }

        dishRepository.saveAll(List.of(
                new Dish("House Burger",
                        "Brioche bun, beef patty and cheddar. Flash promotion: only 10 units!",
                        new BigDecimal("39.90"), 10),
                new Dish("Veggie Burger",
                        "Vegan plant-based patty, lettuce, tomato and vegan mayo",
                        new BigDecimal("34.90"), 100),
                new Dish("Margherita Pizza",
                        "Tomato sauce, mozzarella and fresh basil (vegetarian)",
                        new BigDecimal("44.90"), 80),
                new Dish("Caesar Salad",
                        "Romaine lettuce, grilled chicken, croutons and parmesan",
                        new BigDecimal("29.90"), 60),
                new Dish("Spaghetti Carbonara",
                        "Spaghetti, egg yolk, pecorino cheese and crispy bacon",
                        new BigDecimal("38.50"), 60),
                new Dish("Chocolate Brownie",
                        "Warm chocolate brownie with vanilla ice cream (vegetarian)",
                        new BigDecimal("18.00"), 120)
        ));

        log.info("Menu seeded with {} dishes", dishRepository.count());
    }
}
