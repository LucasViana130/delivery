package br.com.fiap.delivery.order.repositories;

import br.com.fiap.delivery.order.models.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
}
