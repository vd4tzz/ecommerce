package io.d4tzz.ecommerce.rest;

import io.d4tzz.ecommerce.application.catalog.product.AddProductVariantUseCase;
import io.d4tzz.ecommerce.application.catalog.product.RemoveProductVariantUseCase;
import io.d4tzz.ecommerce.application.catalog.product.UpdateProductVariantUseCase;
import io.d4tzz.ecommerce.application.catalog.product.command.AddProductVariantCommand;
import io.d4tzz.ecommerce.application.catalog.product.command.UpdateProductVariantCommand;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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

    @PostMapping
    public ResponseEntity<Void> addVariant(@PathVariable Long productId, @RequestBody AddProductVariantRequest request) {
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
        ValidationHelper.validate(command, validator);
        addProductVariantUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

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
        ValidationHelper.validate(command, validator);
        updateProductVariantUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

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
