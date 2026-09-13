package io.d4tzz.ecommerce.common;

import java.math.BigDecimal;
import java.util.Objects;

public class Money implements Comparable<Money> {

    private final BigDecimal amount;

    public Money(BigDecimal amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        this.amount = amount;
    }

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public static Money of(long amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public BigDecimal amount() {
        return amount;
    }

    public Money plus(Money other) {
        Objects.requireNonNull(other, "Money to add cannot be null");
        return Money.of(this.amount.add(other.amount));
    }

    public Money minus(Money other) {
        Objects.requireNonNull(other, "Money to subtract cannot be null");
        if (this.amount.compareTo(other.amount) < 0) {
            throw new IllegalArgumentException(
                "Cannot subtract " + other.amount + " from " + this.amount + " (result would be negative)"
            );
        }
        return Money.of(this.amount.subtract(other.amount));
    }

    public Money multiply(int factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("Factor cannot be negative: " + factor);
        }
        return Money.of(this.amount.multiply(BigDecimal.valueOf(factor)));
    }

    public Money multiply(long factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("Factor cannot be negative: " + factor);
        }
        return Money.of(this.amount.multiply(BigDecimal.valueOf(factor)));
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public int compareTo(Money other) {
        Objects.requireNonNull(other, "Money cannot be null");
        return this.amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return amount.toString();
    }
}
