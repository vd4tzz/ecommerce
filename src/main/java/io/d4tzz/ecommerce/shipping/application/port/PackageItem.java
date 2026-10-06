package io.d4tzz.ecommerce.shipping.application.port;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;

/** One product line in a package: the size and weight of a single unit, and how many units. */
public record PackageItem(int weightGrams, int lengthCm, int widthCm, int heightCm, int quantity) {
    public PackageItem {
        if (weightGrams <= 0 || lengthCm <= 0 || widthCm <= 0 || heightCm <= 0) {
            throw new InvalidArgumentException("Weight and sizes must be greater than zero");
        }
        if (quantity <= 0) {
            throw new InvalidArgumentException("Quantity must be greater than zero");
        }
    }
}
