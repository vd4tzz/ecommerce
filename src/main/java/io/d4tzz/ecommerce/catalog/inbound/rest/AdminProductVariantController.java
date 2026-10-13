package io.d4tzz.ecommerce.catalog.inbound.rest;

import io.d4tzz.ecommerce.catalog.application.product.AddProductVariantUseCase;
import io.d4tzz.ecommerce.catalog.application.product.RemoveProductVariantUseCase;
import io.d4tzz.ecommerce.catalog.application.product.UpdateProductVariantUseCase;
import io.d4tzz.ecommerce.catalog.application.product.command.AddProductVariantCommand;
import io.d4tzz.ecommerce.catalog.application.product.command.UpdateProductVariantCommand;
import io.d4tzz.ecommerce.spring.SpringValidation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin management of a product's variants. Each variant has its own SKU, price, size and weight, and one value for every option of the product.
 */
@RestController
@Tag(name = "Admin - Product variants", description = "Manage the variants of a product")
@RequestMapping("/admin/products/{productId}/variants")
public class AdminProductVariantController {

    private final AddProductVariantUseCase addProductVariantUseCase;
    private final UpdateProductVariantUseCase updateProductVariantUseCase;
    private final RemoveProductVariantUseCase removeProductVariantUseCase;
    private final Validator validator;

    public AdminProductVariantController(
        AddProductVariantUseCase addProductVariantUseCase,
        UpdateProductVariantUseCase updateProductVariantUseCase,
        RemoveProductVariantUseCase removeProductVariantUseCase,
        Validator validator
    ) {
        this.addProductVariantUseCase = addProductVariantUseCase;
        this.updateProductVariantUseCase = updateProductVariantUseCase;
        this.removeProductVariantUseCase = removeProductVariantUseCase;
        this.validator = validator;
    }

    @Operation(
        summary = "Add variants to a product",
        description = "Adds one or more variants, all saved together. Each combination must give one value for every option of the product, and no two variants can share a combination."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Variants added"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input, or a combination does not match the product's options",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found, or a variant's image is not in the gallery",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "A SKU is already used, or two variants have the same combination",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PostMapping
    public ResponseEntity<Void> addVariant(
        @PathVariable Long productId,
        @RequestBody AddProductVariantRequest request
    ) {
        var command = new AddProductVariantCommand(
            productId,
            request
                .variants()
                .stream()
                .map(v ->
                    new AddProductVariantCommand.Variant(
                        v.sku(),
                        v.price(),
                        v.mediaId(),
                        v.length(),
                        v.width(),
                        v.height(),
                        v.weight(),
                        v.combination()
                    )
                )
                .toList()
        );
        SpringValidation.validate(command, validator);
        addProductVariantUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Update a variant",
        description = "Changes the price, compare-at price, dimensions, weight and image of a variant. A blank image id removes its image, and no compare-at price removes it. The SKU and combination cannot be changed here."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Variant updated"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input, or the compare-at price is lower than the price",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product or variant not found, or the image is not in the gallery",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PutMapping("/{variantId}")
    public ResponseEntity<Void> updateVariant(
        @PathVariable Long productId,
        @PathVariable Long variantId,
        @RequestBody UpdateProductVariantRequest request
    ) {
        var command = new UpdateProductVariantCommand(
            productId,
            variantId,
            request.price(),
            request.compareAt(),
            request.mediaId(),
            request.length(),
            request.width(),
            request.height(),
            request.weight()
        );
        SpringValidation.validate(command, validator);
        updateProductVariantUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Remove a variant",
        description = "Removes a variant from a draft product. Nothing happens if the product has no such variant."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Variant removed"),
        @ApiResponse(
            responseCode = "400",
            description = "The product is not a draft",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @DeleteMapping("/{variantId}")
    public ResponseEntity<Void> removeVariant(@PathVariable Long productId, @PathVariable Long variantId) {
        removeProductVariantUseCase.execute(productId, variantId);
        return ResponseEntity.noContent().build();
    }
}

record AddProductVariantRequest(List<VariantItem> variants) {
    record VariantItem(
        String sku,
        BigDecimal price,
        BigDecimal compareAt,
        String mediaId,
        Integer length,
        Integer width,
        Integer height,
        Integer weight,
        Map<Long, Long> combination
    ) {}
}

record UpdateProductVariantRequest(
    BigDecimal price,
    BigDecimal compareAt,
    String mediaId,
    Integer length,
    Integer width,
    Integer height,
    Integer weight
) {}
