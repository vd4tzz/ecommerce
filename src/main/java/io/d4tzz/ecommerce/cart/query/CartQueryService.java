package io.d4tzz.ecommerce.cart.query;

public interface CartQueryService {
    CartView getCart(Long userId);
}
