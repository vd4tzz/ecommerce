package io.d4tzz.ecommerce.domain.catalog.option;

@FunctionalInterface
public interface OptionUsageChecker {
    boolean isReferenced(OptionId id);
}
