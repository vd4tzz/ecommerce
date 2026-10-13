package io.d4tzz.ecommerce.catalog.inbound.rest;

import io.d4tzz.ecommerce.catalog.application.product.AddProductOptionUseCase;
import io.d4tzz.ecommerce.catalog.application.product.AddProductOptionValueUseCase;
import io.d4tzz.ecommerce.catalog.application.product.RemoveProductOptionUseCase;
import io.d4tzz.ecommerce.catalog.application.product.RemoveProductOptionValueUseCase;
import io.d4tzz.ecommerce.catalog.application.product.ReorderProductOptionValuesUseCase;
import io.d4tzz.ecommerce.catalog.application.product.ReorderProductOptionsUseCase;
import io.d4tzz.ecommerce.catalog.application.product.command.AddProductOptionCommand;
import io.d4tzz.ecommerce.catalog.application.product.command.AddProductOptionValueCommand;
import io.d4tzz.ecommerce.catalog.application.product.command.ReorderProductOptionValuesCommand;
import io.d4tzz.ecommerce.catalog.application.product.command.ReorderProductOptionsCommand;
import io.d4tzz.ecommerce.spring.SpringValidation;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin management of which options a product has, and which of each option's values it offers. Variants pick one value of every option.
 */
@RestController
@Tag(name = "Admin - Product options", description = "Manage the options and option values of a product")
@RequestMapping("/admin/products/{productId}/options")
public class AdminProductOptionController {

    private final AddProductOptionUseCase addProductOptionUseCase;
    private final RemoveProductOptionUseCase removeProductOptionUseCase;
    private final ReorderProductOptionsUseCase reorderProductOptionsUseCase;
    private final AddProductOptionValueUseCase addProductOptionValueUseCase;
    private final RemoveProductOptionValueUseCase removeProductOptionValueUseCase;
    private final ReorderProductOptionValuesUseCase reorderProductOptionValuesUseCase;
    private final Validator validator;

    public AdminProductOptionController(
        AddProductOptionUseCase addProductOptionUseCase,
        RemoveProductOptionUseCase removeProductOptionUseCase,
        ReorderProductOptionsUseCase reorderProductOptionsUseCase,
        AddProductOptionValueUseCase addProductOptionValueUseCase,
        RemoveProductOptionValueUseCase removeProductOptionValueUseCase,
        ReorderProductOptionValuesUseCase reorderProductOptionValuesUseCase,
        Validator validator
    ) {
        this.addProductOptionUseCase = addProductOptionUseCase;
        this.removeProductOptionUseCase = removeProductOptionUseCase;
        this.reorderProductOptionsUseCase = reorderProductOptionsUseCase;
        this.addProductOptionValueUseCase = addProductOptionValueUseCase;
        this.removeProductOptionValueUseCase = removeProductOptionValueUseCase;
        this.reorderProductOptionValuesUseCase = reorderProductOptionValuesUseCase;
        this.validator = validator;
    }

    @Operation(
        summary = "Add an option to a product",
        description = "Adds an option with the values the product offers. The first value is given to every existing variant for this option. The values must belong to the option."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Option added"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input, or some values do not belong to the option",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product or option not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "The product already has the option",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PostMapping
    public ResponseEntity<Void> addOption(@PathVariable Long productId, @RequestBody AddProductOptionRequest request) {
        var command = new AddProductOptionCommand(productId, request.optionId(), request.valueIds());
        SpringValidation.validate(command, validator);
        addProductOptionUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Remove an option from a product",
        description = "Removes the option from the product and from every variant. The option must have only one value left."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Option removed"),
        @ApiResponse(
            responseCode = "400",
            description = "The option still has more than one value",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product or option not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @DeleteMapping("/{optionId}")
    public ResponseEntity<Void> removeOption(@PathVariable Long productId, @PathVariable Long optionId) {
        removeProductOptionUseCase.execute(productId, optionId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Reorder product options",
        description = "Sets the option order. The list must contain every option of the product exactly once."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Options reordered"),
        @ApiResponse(
            responseCode = "400",
            description = "The list has duplicates or unknown options, or leaves some out",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderOptions(
        @PathVariable Long productId,
        @RequestBody ReorderProductOptionsRequest request
    ) {
        var command = new ReorderProductOptionsCommand(productId, request.optionIds());
        SpringValidation.validate(command, validator);
        reorderProductOptionsUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Add values to a product option",
        description = "Adds values to an option the product already has. The values must belong to the option."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Values added"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input, or some values do not belong to the option",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found, or it does not have the option",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "The product already has one of the values",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PostMapping("/{optionId}/values")
    public ResponseEntity<Void> addOptionValues(
        @PathVariable Long productId,
        @PathVariable Long optionId,
        @RequestBody AddProductOptionValuesRequest request
    ) {
        var command = new AddProductOptionValueCommand(productId, optionId, request.valueIds());
        SpringValidation.validate(command, validator);
        addProductOptionValueUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Remove a value from a product option",
        description = "Removes a value from the option on the product. If it was the last value, the option is removed too."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Value removed"),
        @ApiResponse(
            responseCode = "400",
            description = "A variant uses the value",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product, option or value not found on the product",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @DeleteMapping("/{optionId}/values/{valueId}")
    public ResponseEntity<Void> removeOptionValue(
        @PathVariable Long productId,
        @PathVariable Long optionId,
        @PathVariable Long valueId
    ) {
        removeProductOptionValueUseCase.execute(productId, optionId, valueId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Reorder option values",
        description = "Sets the order of an option's values. The list must contain every value of the option on the product exactly once."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Values reordered"),
        @ApiResponse(
            responseCode = "400",
            description = "The list has duplicates or unknown values, or leaves some out",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found, or it does not have the option",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PutMapping("/{optionId}/values/reorder")
    public ResponseEntity<Void> reorderOptionValues(
        @PathVariable Long productId,
        @PathVariable Long optionId,
        @RequestBody ReorderProductOptionValuesRequest request
    ) {
        var command = new ReorderProductOptionValuesCommand(productId, optionId, request.valueIds());
        SpringValidation.validate(command, validator);
        reorderProductOptionValuesUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }
}

record AddProductOptionRequest(Long optionId, List<Long> valueIds) {}

record ReorderProductOptionsRequest(List<Long> optionIds) {}

record AddProductOptionValuesRequest(List<Long> valueIds) {}

record ReorderProductOptionValuesRequest(List<Long> valueIds) {}
