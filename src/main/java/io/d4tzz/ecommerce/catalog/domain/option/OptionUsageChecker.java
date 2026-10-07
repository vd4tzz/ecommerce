package io.d4tzz.ecommerce.catalog.domain.option;

@FunctionalInterface
public interface OptionUsageChecker {
    boolean isReferenced(OptionId id);
}
