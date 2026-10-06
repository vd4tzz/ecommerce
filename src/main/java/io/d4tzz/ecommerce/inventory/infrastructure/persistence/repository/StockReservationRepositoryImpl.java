package io.d4tzz.ecommerce.inventory.infrastructure.persistence.repository;

import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import io.d4tzz.ecommerce.inventory.domain.ReservationLine;
import io.d4tzz.ecommerce.inventory.domain.StockReservation;
import io.d4tzz.ecommerce.inventory.domain.StockReservationId;
import io.d4tzz.ecommerce.inventory.domain.StockReservationRepository;
import io.d4tzz.ecommerce.inventory.domain.WarehouseId;
import io.d4tzz.ecommerce.inventory.infrastructure.persistence.entity.JpaReservationLine;
import io.d4tzz.ecommerce.inventory.infrastructure.persistence.entity.JpaStockReservation;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class StockReservationRepositoryImpl implements StockReservationRepository {

    private final SpringStockReservationRepository springRepo;

    public StockReservationRepositoryImpl(SpringStockReservationRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public boolean exists(Long orderId, WarehouseId warehouseId) {
        return springRepo.existsByOrderIdAndWarehouseId(orderId, warehouseId.value());
    }


    @Override
    public List<StockReservation> findAllForUpdate(Long orderId) {
        return springRepo.findAllForUpdate(orderId).stream().map(this::toDomain).toList();
    }

    @Override
    public StockReservation save(StockReservation reservation) {
        JpaStockReservation jpa;
        if (reservation.getId() == null) {
            jpa = new JpaStockReservation();
            jpa.setOrderId(reservation.getOrderId());
            jpa.setWarehouseId(reservation.getWarehouseId().value());
            jpa.setCreatedAt(reservation.getCreatedAt());
            // Lines never change after creation
            jpa.setLines(
                reservation
                    .getLines()
                    .stream()
                    .map(line -> new JpaReservationLine(line.variantId().value(), line.quantity()))
                    .collect(Collectors.toCollection(ArrayList::new))
            );
        } else {
            jpa = springRepo.findById(reservation.getId().value()).orElseThrow();
        }
        jpa.setStatus(reservation.getStatus());
        jpa.setUpdatedAt(reservation.getUpdatedAt());

        return toDomain(springRepo.save(jpa));
    }

    private StockReservation toDomain(JpaStockReservation jpa) {
        List<ReservationLine> lines = jpa
            .getLines()
            .stream()
            .map(line -> new ReservationLine(VariantId.of(line.getVariantId()), line.getQuantity()))
            .toList();

        return StockReservation.restore(
            StockReservationId.of(jpa.getId()),
            jpa.getOrderId(),
            WarehouseId.of(jpa.getWarehouseId()),
            lines,
            jpa.getStatus(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt()
        );
    }
}
