package io.d4tzz.ecommerce.rest;

import io.d4tzz.ecommerce.application.catalog.product.AddProductOptionUseCase;
import io.d4tzz.ecommerce.application.catalog.product.AddProductOptionValueUseCase;
import io.d4tzz.ecommerce.application.catalog.product.RemoveProductOptionUseCase;
import io.d4tzz.ecommerce.application.catalog.product.RemoveProductOptionValueUseCase;
import io.d4tzz.ecommerce.application.catalog.product.ReorderProductOptionValuesUseCase;
import io.d4tzz.ecommerce.application.catalog.product.ReorderProductOptionsUseCase;
import io.d4tzz.ecommerce.application.catalog.product.command.AddProductOptionCommand;
import io.d4tzz.ecommerce.application.catalog.product.command.AddProductOptionValueCommand;
import io.d4tzz.ecommerce.application.catalog.product.command.ReorderProductOptionValuesCommand;
import io.d4tzz.ecommerce.application.catalog.product.command.ReorderProductOptionsCommand;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
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

    @PostMapping
    public ResponseEntity<Void> addOption(@PathVariable Long productId, @RequestBody AddProductOptionRequest request) {
        var command = new AddProductOptionCommand(productId, request.optionId(), request.valueIds());
        ValidationHelper.validate(command, validator);
        addProductOptionUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{optionId}")
    public ResponseEntity<Void> removeOption(@PathVariable Long productId, @PathVariable Long optionId) {
        removeProductOptionUseCase.execute(productId, optionId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderOptions(
        @PathVariable Long productId,
        @RequestBody ReorderProductOptionsRequest request
    ) {
        var command = new ReorderProductOptionsCommand(productId, request.optionIds());
        ValidationHelper.validate(command, validator);
        reorderProductOptionsUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{optionId}/values")
    public ResponseEntity<Void> addOptionValues(
        @PathVariable Long productId,
        @PathVariable Long optionId,
        @RequestBody AddProductOptionValuesRequest request
    ) {
        var command = new AddProductOptionValueCommand(productId, optionId, request.valueIds());
        ValidationHelper.validate(command, validator);
        addProductOptionValueUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{optionId}/values/{valueId}")
    public ResponseEntity<Void> removeOptionValue(
        @PathVariable Long productId,
        @PathVariable Long optionId,
        @PathVariable Long valueId
    ) {
        removeProductOptionValueUseCase.execute(productId, optionId, valueId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{optionId}/values/reorder")
    public ResponseEntity<Void> reorderOptionValues(
        @PathVariable Long productId,
        @PathVariable Long optionId,
        @RequestBody ReorderProductOptionValuesRequest request
    ) {
        var command = new ReorderProductOptionValuesCommand(productId, optionId, request.valueIds());
        ValidationHelper.validate(command, validator);
        reorderProductOptionValuesUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }
}

record AddProductOptionRequest(Long optionId, List<Long> valueIds) {}

record ReorderProductOptionsRequest(List<Long> optionIds) {}

record AddProductOptionValuesRequest(List<Long> valueIds) {}

record ReorderProductOptionValuesRequest(List<Long> valueIds) {}
