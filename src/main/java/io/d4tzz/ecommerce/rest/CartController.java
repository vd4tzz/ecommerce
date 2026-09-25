package io.d4tzz.ecommerce.rest;

import io.d4tzz.ecommerce.application.cart.AddToCartUseCase;
import io.d4tzz.ecommerce.application.cart.CartDto;
import io.d4tzz.ecommerce.application.cart.ClearCartUseCase;
import io.d4tzz.ecommerce.application.cart.GetCartUseCase;
import io.d4tzz.ecommerce.application.cart.MergeCartUseCase;
import io.d4tzz.ecommerce.application.cart.RemoveCartItemUseCase;
import io.d4tzz.ecommerce.application.cart.UpdateCartItemUseCase;
import io.d4tzz.ecommerce.rest.security.CurrentUser;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final GetCartUseCase getCartUseCase;
    private final AddToCartUseCase addToCartUseCase;
    private final UpdateCartItemUseCase updateCartItemUseCase;
    private final RemoveCartItemUseCase removeCartItemUseCase;
    private final ClearCartUseCase clearCartUseCase;
    private final MergeCartUseCase mergeCartUseCase;

    public CartController(
        GetCartUseCase getCartUseCase,
        AddToCartUseCase addToCartUseCase,
        UpdateCartItemUseCase updateCartItemUseCase,
        RemoveCartItemUseCase removeCartItemUseCase,
        ClearCartUseCase clearCartUseCase,
        MergeCartUseCase mergeCartUseCase
    ) {
        this.getCartUseCase = getCartUseCase;
        this.addToCartUseCase = addToCartUseCase;
        this.updateCartItemUseCase = updateCartItemUseCase;
        this.removeCartItemUseCase = removeCartItemUseCase;
        this.clearCartUseCase = clearCartUseCase;
        this.mergeCartUseCase = mergeCartUseCase;
    }

    @GetMapping
    public ResponseEntity<CartDto> getCart(@CurrentUser Long userId) {
        return ResponseEntity.ok(getCartUseCase.execute(new GetCartUseCase.Query(userId)));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addItem(
        @CurrentUser Long userId,
        @RequestBody AddToCartRequest request
    ) {
        var command = new AddToCartUseCase.Command(userId, request.variantId(), request.quantity());
        return ResponseEntity.ok(addToCartUseCase.execute(command));
    }

    @PutMapping("/items/{variantId}")
    public ResponseEntity<CartDto> updateQuantity(
        @CurrentUser Long userId,
        @PathVariable Long variantId,
        @RequestBody UpdateCartItemRequest request
    ) {
        var command = new UpdateCartItemUseCase.Command(userId, variantId, request.quantity());
        return ResponseEntity.ok(updateCartItemUseCase.execute(command));
    }

    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<CartDto> removeItem(
        @CurrentUser Long userId,
        @PathVariable Long variantId
    ) {
        var command = new RemoveCartItemUseCase.Command(userId, variantId);
        return ResponseEntity.ok(removeCartItemUseCase.execute(command));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@CurrentUser Long userId) {
        clearCartUseCase.execute(new ClearCartUseCase.Command(userId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/merge")
    public ResponseEntity<CartDto> mergeCart(
        @CurrentUser Long userId,
        @RequestBody List<MergeCartItemRequest> items
    ) {
        List<MergeCartUseCase.Item> domainItems = items != null ? items.stream()
            .map(item -> new MergeCartUseCase.Item(item.variantId(), item.quantity()))
            .toList() : List.of();

        var command = new MergeCartUseCase.Command(userId, domainItems);
        return ResponseEntity.ok(mergeCartUseCase.execute(command));
    }
}

record AddToCartRequest(Long variantId, int quantity) {}

record UpdateCartItemRequest(int quantity) {}

record MergeCartItemRequest(Long variantId, int quantity) {}
