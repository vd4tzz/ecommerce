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
import io.d4tzz.ecommerce.spring.SpringValidation;
import io.d4tzz.ecommerce.spring.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ProblemDetail;
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
@Tag(name = "Cart", description = "The current user's shopping cart")
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

    @Operation(
        summary = "Get my cart",
        description = "Returns the authenticated user's cart with product details and current prices. A user without items gets an empty cart."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The cart")
    })
    @GetMapping
    public ResponseEntity<CartView> getCart(@CurrentUser Long userId) {
        return ResponseEntity.ok(cartQueryService.getCart(userId));
    }

    @Operation(
        summary = "Add an item",
        description = "Adds a quantity of a variant to the cart. If the variant is already in the cart, the quantity is added to it. The variant must be for sale, and one item holds at most 99."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The cart after adding the item"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input, the variant is not for sale, or the item would exceed 99",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping("/items")
    public ResponseEntity<CartView> addItem(@CurrentUser Long userId, @RequestBody AddToCartRequest request) {
        var command = new AddToCartCommand(userId, request.variantId(), request.quantity());
        SpringValidation.validate(command, validator);
        addToCartUseCase.execute(command);
        return ResponseEntity.ok(cartQueryService.getCart(userId));
    }

    @Operation(
        summary = "Set an item's quantity",
        description = "Sets the quantity of a variant, from 1 to 99, adding it if it is not in the cart yet. To remove an item use the delete endpoint."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The cart after the change"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input, or the variant is not for sale",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PutMapping("/items/{variantId}")
    public ResponseEntity<CartView> updateQuantity(
        @CurrentUser Long userId,
        @PathVariable Long variantId,
        @RequestBody UpdateCartItemRequest request
    ) {
        var command = new UpdateCartItemCommand(userId, variantId, request.quantity());
        SpringValidation.validate(command, validator);
        updateCartItemUseCase.execute(command);
        return ResponseEntity.ok(cartQueryService.getCart(userId));
    }

    @Operation(
        summary = "Remove an item",
        description = "Removes a variant from the cart. Nothing changes if it is not in the cart."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The cart after removing the item")
    })
    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<CartView> removeItem(@CurrentUser Long userId, @PathVariable Long variantId) {
        var command = new RemoveCartItemCommand(userId, variantId);
        removeCartItemUseCase.execute(command);
        return ResponseEntity.ok(cartQueryService.getCart(userId));
    }

    @Operation(
        summary = "Clear my cart",
        description = "Removes every item from the cart. Nothing happens if there is no cart."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Cart cleared")
    })
    @DeleteMapping
    public ResponseEntity<Void> clearCart(@CurrentUser Long userId) {
        clearCartUseCase.execute(new ClearCartCommand(userId));
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Merge a guest cart",
        description = "Adds the items of a guest cart, kept by the client, to the user's cart after login. Variants already in the cart keep their quantity, quantities above 99 are lowered to 99, and variants that are not for sale are skipped."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The cart after merging"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
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
        SpringValidation.validate(command, validator);
        mergeCartUseCase.execute(command);
        return ResponseEntity.ok(cartQueryService.getCart(userId));
    }
}

record AddToCartRequest(Long variantId, int quantity) {}

record UpdateCartItemRequest(int quantity) {}

record MergeCartItemRequest(Long variantId, int quantity) {}
