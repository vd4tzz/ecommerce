package io.d4tzz.ecommerce.inventory.query;

import java.time.Instant;

public record WarehouseView(
    Long id,
    String code,
    String name,
    String province,
    String district,
    String ward,
    String detailAddress,
    String status,
    Instant createdAt,
    Instant updatedAt
) {}
