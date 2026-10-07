package io.d4tzz.ecommerce.cart.inbound.rest;

import io.d4tzz.ecommerce.cart.application.AddToCartUseCase;
import io.d4tzz.ecommerce.cart.application.ClearCartUseCase;
import io.d4tzz.ecommerce.cart.application.MergeCartUseCase;
import io.d4tzz.ecommerce.cart.application.RemoveCartItemUseCase;
import io.d4tzz.ecommerce.cart.application.UpdateCartItemUseCase;
import io.d4tzz.ecommerce.cart.application.command.AddToCartCommand;
import io.d4tzz.ecommerce.cart.application.command.ClearCartCommand;
import io.d4tzz.ecommerce.cart.application.command.MergeCartCommand;
import io.d4tzz.ecommerce.cart.application.command.RemoveCartItemCommand;
import io.d4tzz.ecommerce.cart.application.command.UpdateCartItemCommand;
import io.d4tzz.ecommerce.cart.query.CartQueryService;
import io.d4tzz.ecommerce.cart.query.CartView;
import io.d4tzz.ecommerce.rest.ValidationHelper;
import io.d4tzz.ecommerce.rest.security.CurrentUser;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The current user's cart. To remove an item, use DELETE; PUT only sets a quantity from 1 to 99. */
@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartQueryService cartQueryService;
    private final AddToCartUseCase addToCartUseCase;
    private final UpdateCartItemUseCase updateCartItemUseCase;
    private final RemoveCartItemUseCase removeCartItemUseCase;
    private final ClearCartUseCase clearCartUseCase;
    private final MergeCartUseCase mergeCartUseCase;
    private final Validator validator;

    public CartController(
        CartQueryService cartQueryService,
        AddToCartUseCase addToCartUseCase,
        UpdateCartItemUseCase updateCartItemUseCase,
        RemoveCartItemUseCase removeCartItemUseCase,
        ClearCartUseCase clearCartUseCase,
        MergeCartUseCase mergeCartUseCase,
        Validator validator
    ) {
        this.cartQueryService = cartQueryService;
        this.addToCartUseCase = addToCartUseCase;
        this.updateCartItemUseCase = updateCartItemUseCase;
        this.removeCartItemUseCase = removeCartItemUseCase;
        this.clearCartUseCase = clearCartUseCase;
        this.mergeCartUseCase = mergeCartUseCase;
        this.validator = validator;
    }

    @GetMapping
    public ResponseEntity<CartView> getCart(@CurrentUser Long userId) {
        return ResponseEntity.ok(cartQueryService.getCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartView> addItem(@CurrentUser Long userId, @RequestBody AddToCartRequest request) {
        var command = new AddToCartCommand(userId, request.variantId(), request.quantity());
        ValidationHelper.validate(command, validator);
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
        ValidationHelper.validate(command, validator);
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
        ValidationHelper.validate(command, validator);
        mergeCartUseCase.execute(command);
        return ResponseEntity.ok(cartQueryService.getCart(userId));
    }
}

record AddToCartRequest(Long variantId, int quantity) {}

record UpdateCartItemRequest(int quantity) {}

record MergeCartItemRequest(Long variantId, int quantity) {}
