package io.d4tzz.ecommerce.catalog.domain.option;

@FunctionalInterface
public interface OptionValueUsageChecker {
    boolean isReferenced(ValueId id);
}
