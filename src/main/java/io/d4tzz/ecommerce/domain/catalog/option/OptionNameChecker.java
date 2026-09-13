package io.d4tzz.ecommerce.domain.catalog.option;

@FunctionalInterface
public interface OptionNameChecker {
    boolean exists(OptionName name);
}
