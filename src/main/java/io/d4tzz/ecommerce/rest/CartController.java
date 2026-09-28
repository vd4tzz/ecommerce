package io.d4tzz.ecommerce.rest;

import io.d4tzz.ecommerce.application.cart.AddToCartUseCase;
import io.d4tzz.ecommerce.application.cart.ClearCartUseCase;
import io.d4tzz.ecommerce.application.cart.MergeCartUseCase;
import io.d4tzz.ecommerce.application.cart.RemoveCartItemUseCase;
import io.d4tzz.ecommerce.application.cart.UpdateCartItemUseCase;
import io.d4tzz.ecommerce.application.cart.command.AddToCartCommand;
import io.d4tzz.ecommerce.application.cart.command.ClearCartCommand;
import io.d4tzz.ecommerce.application.cart.command.MergeCartCommand;
import io.d4tzz.ecommerce.application.cart.command.RemoveCartItemCommand;
import io.d4tzz.ecommerce.application.cart.command.UpdateCartItemCommand;
import io.d4tzz.ecommerce.query.cart.CartQueryService;
import io.d4tzz.ecommerce.query.cart.CartView;
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

    private final CartQueryService cartQueryService;
    private final AddToCartUseCase addToCartUseCase;
    private final UpdateCartItemUseCase updateCartItemUseCase;
    private final RemoveCartItemUseCase removeCartItemUseCase;
    private final ClearCartUseCase clearCartUseCase;
    private final MergeCartUseCase mergeCartUseCase;

    public CartController(
        CartQueryService cartQueryService,
        AddToCartUseCase addToCartUseCase,
        UpdateCartItemUseCase updateCartItemUseCase,
        RemoveCartItemUseCase removeCartItemUseCase,
        ClearCartUseCase clearCartUseCase,
        MergeCartUseCase mergeCartUseCase
    ) {
        this.cartQueryService = cartQueryService;
        this.addToCartUseCase = addToCartUseCase;
        this.updateCartItemUseCase = updateCartItemUseCase;
        this.removeCartItemUseCase = removeCartItemUseCase;
        this.clearCartUseCase = clearCartUseCase;
        this.mergeCartUseCase = mergeCartUseCase;
    }

    @GetMapping
    public ResponseEntity<CartView> getCart(@CurrentUser Long userId) {
        return ResponseEntity.ok(cartQueryService.getCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartView> addItem(@CurrentUser Long userId, @RequestBody AddToCartRequest request) {
        var command = new AddToCartCommand(userId, request.variantId(), request.quantity());
        addToCartUseCase.execute(command);
        return ResponseEntity.ok(cartQueryService.getCart(userId));
    }

    @PutMapping("/items/{variantId}")
    public ResponseEntity<CartView> updateQuantity(
        @CurrentUser Long userId,
        @PathVariable Long variantId,
        @RequestBody UpdateCartItemRequest request
    ) {
        var command = new UpdateCartItemCommand(userId, variantId, request.quantity());
        updateCartItemUseCase.execute(command);
        return ResponseEntity.ok(cartQueryService.getCart(userId));
    }

    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<CartView> removeItem(@CurrentUser Long userId, @PathVariable Long variantId) {
        var command = new RemoveCartItemCommand(userId, variantId);
        removeCartItemUseCase.execute(command);
        return ResponseEntity.ok(cartQueryService.getCart(userId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@CurrentUser Long userId) {
        clearCartUseCase.execute(new ClearCartCommand(userId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/merge")
    public ResponseEntity<CartView> mergeCart(@CurrentUser Long userId, @RequestBody List<MergeCartItemRequest> items) {
        List<MergeCartCommand.Item> domainItems =
            items != null
                ? items
                      .stream()
                      .map(item -> new MergeCartCommand.Item(item.variantId(), item.quantity()))
                      .toList()
                : List.of();

        var command = new MergeCartCommand(userId, domainItems);
        mergeCartUseCase.execute(command);
        return ResponseEntity.ok(cartQueryService.getCart(userId));
    }
}

record AddToCartRequest(Long variantId, int quantity) {}

record UpdateCartItemRequest(int quantity) {}

record MergeCartItemRequest(Long variantId, int quantity) {}
