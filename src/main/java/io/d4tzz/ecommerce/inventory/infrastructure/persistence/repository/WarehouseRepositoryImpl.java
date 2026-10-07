package io.d4tzz.ecommerce.inventory.infrastructure.persistence.repository;

import io.d4tzz.ecommerce.common.Address;
import io.d4tzz.ecommerce.inventory.domain.Warehouse;
import io.d4tzz.ecommerce.inventory.domain.WarehouseCode;
import io.d4tzz.ecommerce.inventory.domain.WarehouseId;
import io.d4tzz.ecommerce.inventory.domain.WarehouseRepository;
import io.d4tzz.ecommerce.inventory.infrastructure.persistence.entity.JpaWarehouse;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class WarehouseRepositoryImpl implements WarehouseRepository {

    private final SpringWarehouseRepository springRepo;

    public WarehouseRepositoryImpl(SpringWarehouseRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<Warehouse> find(WarehouseId id) {
        return springRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public boolean existsByCode(WarehouseCode code) {
        return springRepo.existsByCode(code.value());
    }

    @Override
    public Warehouse save(Warehouse warehouse) {
        JpaWarehouse jpa;
        if (warehouse.getId() == null) {
            jpa = new JpaWarehouse();
            jpa.setCode(warehouse.getCode().value());
            jpa.setCreatedAt(warehouse.getCreatedAt());
        } else {
            jpa = springRepo.findById(warehouse.getId().value()).orElseThrow();
        }

        Address address = warehouse.getAddress();
        jpa.setName(warehouse.getName());
        jpa.setProvince(address.province());
        jpa.setDistrict(address.district());
        jpa.setWard(address.ward());
        jpa.setAddressDetail(address.detailAddress());
        jpa.setStatus(warehouse.getStatus());
        jpa.setUpdatedAt(warehouse.getUpdatedAt());

        return toDomain(springRepo.save(jpa));
    }

    private Warehouse toDomain(JpaWarehouse jpa) {
        return Warehouse.restore(
            WarehouseId.of(jpa.getId()),
            WarehouseCode.of(jpa.getCode()),
            jpa.getName(),
            Address.of(jpa.getProvince(), jpa.getDistrict(), jpa.getWard(), jpa.getAddressDetail()),
            jpa.getStatus(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt()
        );
    }
}
