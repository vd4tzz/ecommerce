package io.d4tzz.ecommerce.domain.catalog.option;

@FunctionalInterface
public interface OptionNameUniqueChecker {
    boolean exists(OptionName name);
}
