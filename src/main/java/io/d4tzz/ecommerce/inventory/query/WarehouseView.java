package io.d4tzz.ecommerce.inventory.query;

import java.time.Instant;

/** A warehouse. {@code status} is ACTIVE or INACTIVE. */
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
