package io.d4tzz.ecommerce.domain.catalog.product;

import java.util.Objects;

public class Weight implements Comparable<Weight> {

    private final int grams;

    public Weight(int grams) {
        if (grams <= 0) {
            throw new IllegalArgumentException(
                "Weight must be greater than zero: " + grams
            );
        }

        this.grams = grams;
    }

    public static Weight ofGrams(int grams) {
        return new Weight(grams);
    }

    public static Weight ofKilograms(double kg) {
        if (kg <= 0) {
            throw new IllegalArgumentException(
                "Weight must be greater than zero: " + kg
            );
        }
        return new Weight((int) Math.round(kg * 1000));
    }

    public int grams() {
        return grams;
    }

    public double toKilograms() {
        return (double) grams / 1000.0;
    }

    @Override
    public int compareTo(Weight other) {
        Objects.requireNonNull(other, "Weight cannot be null");
        return Integer.compare(this.grams, other.grams);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Weight weight = (Weight) o;
        return grams == weight.grams;
    }

    @Override
    public int hashCode() {
        return Objects.hash(grams);
    }

    @Override
    public String toString() {
        return grams + "g";
    }
}
