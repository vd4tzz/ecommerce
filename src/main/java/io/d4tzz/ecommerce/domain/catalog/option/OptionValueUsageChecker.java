package io.d4tzz.ecommerce.domain.catalog.option;

@FunctionalInterface
public interface OptionValueUsageChecker {
    boolean isReferenced(ValueId id);
}
