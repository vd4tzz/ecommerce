package io.d4tzz.ecommerce.cart.infrastructure.persistence.repository;

import io.d4tzz.ecommerce.cart.infrastructure.persistence.entity.JpaCart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringCartRepository extends JpaRepository<JpaCart, Long> {

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.userId = :userId")
    Optional<JpaCart> findByUserIdWithItems(@Param("userId") Long userId);
}
