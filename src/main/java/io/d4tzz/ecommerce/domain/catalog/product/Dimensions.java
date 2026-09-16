package io.d4tzz.ecommerce.domain.catalog.product;

import java.util.Objects;

public class Dimensions {

    private final int length;
    private final int width;
    private final int height;

    public Dimensions(int length, int width, int height) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than zero: " + length);
        }
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be greater than zero: " + width);
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be greater than zero: " + height);
        }
        this.length = length;
        this.width = width;
        this.height = height;
    }

    public static Dimensions of(int length, int width, int height) {
        return new Dimensions(length, width, height);
    }

    public int length() {
        return length;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int getVolume() {
        return length * width * height;
    }

    public double getVolumetricWeightKg(int divisor) {
        if (divisor <= 0) {
            throw new IllegalArgumentException("Divisor must be greater than zero: " + divisor);
        }
        return (double) getVolume() / divisor;
    }

    public double getVolumetricWeightKg() {
        return getVolumetricWeightKg(5000);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Dimensions that = (Dimensions) o;
        return length == that.length && width == that.width && height == that.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(length, width, height);
    }

    @Override
    public String toString() {
        return length + "x" + width + "x" + height + " cm";
    }
}
