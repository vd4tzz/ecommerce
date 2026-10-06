package io.d4tzz.ecommerce.inventory.infrastructure.persistence.repository;

import io.d4tzz.ecommerce.inventory.infrastructure.persistence.entity.JpaWarehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringWarehouseRepository extends JpaRepository<JpaWarehouse, Long> {
    boolean existsByCode(String code);
}
