package io.d4tzz.ecommerce.catalog.inbound.rest;

import io.d4tzz.ecommerce.spring.SpringValidation;
import io.d4tzz.ecommerce.spring.ValidationException;

import io.d4tzz.ecommerce.catalog.application.product.ChangeProductStatusUseCase;
import io.d4tzz.ecommerce.catalog.application.product.CreateProductUseCase;
import io.d4tzz.ecommerce.catalog.application.product.DeleteProductUseCase;
import io.d4tzz.ecommerce.catalog.application.product.UpdateProductUseCase;
import io.d4tzz.ecommerce.catalog.application.product.command.ChangeProductStatusCommand;
import io.d4tzz.ecommerce.catalog.application.product.command.CreateProductCommand;
import io.d4tzz.ecommerce.catalog.application.product.command.UpdateProductCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.product.ProductStatus;
import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.catalog.query.product.AdminProductSearchCriteria;
import io.d4tzz.ecommerce.catalog.query.product.AdminProductTableRowView;
import io.d4tzz.ecommerce.catalog.query.product.ProductDetailView;
import io.d4tzz.ecommerce.catalog.query.product.ProductQueryService;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductQueryService productQueryService;
    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final ChangeProductStatusUseCase changeProductStatusUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final Validator validator;

    public AdminProductController(
        ProductQueryService productQueryService,
        CreateProductUseCase createProductUseCase,
        UpdateProductUseCase updateProductUseCase,
        ChangeProductStatusUseCase changeProductStatusUseCase,
        DeleteProductUseCase deleteProductUseCase,
        Validator validator
    ) {
        this.productQueryService = productQueryService;
        this.createProductUseCase = createProductUseCase;
        this.updateProductUseCase = updateProductUseCase;
        this.changeProductStatusUseCase = changeProductStatusUseCase;
        this.deleteProductUseCase = deleteProductUseCase;
        this.validator = validator;
    }

    @PostMapping
    public ResponseEntity<CreateProductResponse> createProduct(@RequestBody CreateProductRequest request) {
        var command = new CreateProductCommand(
            request.name(),
            request.slug(),
            request.description(),
            request.categoryId(),
            request.attributes() != null ? request.attributes() : Map.of()
        );
        SpringValidation.validate(command, validator);
        Long productId = createProductUseCase.execute(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(productId)
            .toUri();
        return ResponseEntity.created(location).body(new CreateProductResponse(productId));
    }

    @GetMapping
    public ResponseEntity<PageResponse<AdminProductTableRowView>> getProducts(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String sortBy,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var criteria = new AdminProductSearchCriteria(keyword, status, categoryId, sortBy, page, size);
        return ResponseEntity.ok(productQueryService.getAdminProducts(criteria));
    }

    @GetMapping("/status-counts")
    public ResponseEntity<Map<String, Long>> getStatusCounts() {
        return ResponseEntity.ok(productQueryService.getAdminProductStatusCounts());
    }


    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailView> getProductDetail(@PathVariable Long id) {
        return productQueryService
            .getProductDetailById(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateProduct(@PathVariable Long id, @RequestBody UpdateProductRequest request) {
        var command = new UpdateProductCommand(
            id,
            request.name(),
            request.slug(),
            request.description(),
            request.categoryId(),
            request.attributes() != null ? request.attributes() : Map.of()
        );
        SpringValidation.validate(command, validator);
        updateProductUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(@PathVariable Long id, @RequestBody ChangeProductStatusRequest request) {
        var command = new ChangeProductStatusCommand(id, request.status());
        SpringValidation.validate(command, validator);
        changeProductStatusUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        deleteProductUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}

record CreateProductRequest(
    String name,
    String slug,
    String description,
    Long categoryId,
    Map<String, List<String>> attributes
) {}

record CreateProductResponse(Long id) {}

record UpdateProductRequest(
    String name,
    String slug,
    String description,
    Long categoryId,
    Map<String, List<String>> attributes
) {}

record ChangeProductStatusRequest(ProductStatus status) {}
