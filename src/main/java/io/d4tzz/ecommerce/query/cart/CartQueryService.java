package io.d4tzz.ecommerce.query.cart;

public interface CartQueryService {
    CartView getCart(Long userId);
}
