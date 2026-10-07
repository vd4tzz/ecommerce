package io.d4tzz.ecommerce.catalog.domain.option;

@FunctionalInterface
public interface OptionNameUniqueChecker {
    boolean exists(OptionName name);
}
