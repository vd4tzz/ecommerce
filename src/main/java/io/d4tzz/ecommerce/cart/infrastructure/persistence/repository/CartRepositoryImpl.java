package io.d4tzz.ecommerce.cart.infrastructure.persistence.repository;

import io.d4tzz.ecommerce.cart.domain.Cart;
import io.d4tzz.ecommerce.cart.domain.CartId;
import io.d4tzz.ecommerce.cart.domain.CartItem;
import io.d4tzz.ecommerce.cart.domain.CartItemId;
import io.d4tzz.ecommerce.cart.domain.CartRepository;
import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import io.d4tzz.ecommerce.user.domain.UserId;
import io.d4tzz.ecommerce.cart.infrastructure.persistence.entity.JpaCart;
import io.d4tzz.ecommerce.cart.infrastructure.persistence.entity.JpaCartItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CartRepositoryImpl implements CartRepository {

    private final SpringCartRepository springRepo;
    private final JdbcClient jdbcClient;

    public CartRepositoryImpl(SpringCartRepository springRepo, JdbcClient jdbcClient) {
        this.springRepo = springRepo;
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<Cart> find(CartId id) {
        return springRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<Cart> findByUserId(UserId userId) {
        return springRepo.findByUserIdWithItems(userId.value()).map(this::toDomain);
    }

    @Override
    public void lockForUser(UserId userId) {
        // Postgres advisory lock keyed by the user id, released when the transaction ends. A row lock cannot be
        // used because the cart row may not exist yet.
        jdbcClient.sql("SELECT pg_advisory_xact_lock(:key)").param("key", userId.value()).query().listOfRows();
    }

    @Override
    public Cart save(Cart cart) {
        JpaCart jpaCart;
        if (cart.getId() == null) {
            jpaCart = new JpaCart();
            jpaCart.setUserId(cart.getUserId().value());
        } else {
            jpaCart = springRepo.findById(cart.getId().value()).orElseThrow();
        }

        // Sync items
        Map<Long, CartItem> existingDomainItemsById = new HashMap<>();
        List<CartItem> newDomainItems = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            if (item.getId() != null) {
                existingDomainItemsById.put(item.getId().value(), item);
            } else {
                newDomainItems.add(item);
            }
        }

        jpaCart.getItems().removeIf(jpaItem -> !existingDomainItemsById.containsKey(jpaItem.getId()));

        for (JpaCartItem jpaItem : jpaCart.getItems()) {
            CartItem domainItem = existingDomainItemsById.get(jpaItem.getId());
            if (domainItem != null) {
                jpaItem.setQuantity(domainItem.getQuantity());
            }
        }

        for (CartItem newItem : newDomainItems) {
            JpaCartItem jpaItem = new JpaCartItem();
            jpaItem.setVariantId(newItem.getVariantId().value());
            jpaItem.setQuantity(newItem.getQuantity());
            jpaCart.addItem(jpaItem);
        }

        JpaCart saved = springRepo.save(jpaCart);
        return toDomain(saved);
    }

    @Override
    public void delete(CartId id) {
        springRepo.deleteById(id.value());
    }

    private Cart toDomain(JpaCart jpa) {
        List<CartItem> domainItems = jpa
            .getItems()
            .stream()
            .map(item ->
                CartItem.restore(CartItemId.of(item.getId()), VariantId.of(item.getVariantId()), item.getQuantity())
            )
            .collect(Collectors.toList());

        return new Cart(CartId.of(jpa.getId()), UserId.of(jpa.getUserId()), domainItems);
    }
}
