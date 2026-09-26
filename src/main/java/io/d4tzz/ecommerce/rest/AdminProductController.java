package io.d4tzz.ecommerce.rest;

import io.d4tzz.ecommerce.application.catalog.product.ChangeProductStatusCommand;
import io.d4tzz.ecommerce.application.catalog.product.ChangeProductStatusUseCase;
import io.d4tzz.ecommerce.application.catalog.product.CreateProductCommand;
import io.d4tzz.ecommerce.application.catalog.product.CreateProductUseCase;
import io.d4tzz.ecommerce.application.catalog.product.DeleteProductUseCase;
import io.d4tzz.ecommerce.application.catalog.product.ProductDto;
import io.d4tzz.ecommerce.application.catalog.product.UpdateProductCommand;
import io.d4tzz.ecommerce.application.catalog.product.UpdateProductUseCase;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.catalog.product.ProductStatus;
import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.query.catalog.AdminProductSearchCriteria;
import io.d4tzz.ecommerce.query.catalog.AdminProductTableRowView;
import io.d4tzz.ecommerce.query.catalog.ProductDetailView;
import io.d4tzz.ecommerce.query.catalog.ProductQueryService;
import io.d4tzz.ecommerce.query.catalog.ProductVariantSummaryView;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
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

    public AdminProductController(
        ProductQueryService productQueryService,
        CreateProductUseCase createProductUseCase,
        UpdateProductUseCase updateProductUseCase,
        ChangeProductStatusUseCase changeProductStatusUseCase,
        DeleteProductUseCase deleteProductUseCase
    ) {
        this.productQueryService = productQueryService;
        this.createProductUseCase = createProductUseCase;
        this.updateProductUseCase = updateProductUseCase;
        this.changeProductStatusUseCase = changeProductStatusUseCase;
        this.deleteProductUseCase = deleteProductUseCase;
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@RequestBody CreateProductCommand command) {
        ProductDto response = createProductUseCase.execute(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
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

    @GetMapping("/variants")
    public ResponseEntity<List<ProductVariantSummaryView>> getVariants(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(productQueryService.getVariants(keyword, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailView> getProductDetail(@PathVariable Long id) {
        return productQueryService
            .getProductDetailById(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @RequestBody UpdateProductRequest request) {
        var command = new UpdateProductCommand(
            id,
            request.name(),
            request.slug(),
            request.description(),
            request.categoryId(),
            request.attributes()
        );
        return ResponseEntity.ok(updateProductUseCase.execute(command));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable Long id, @RequestBody ChangeProductStatusRequest request) {
        var command = new ChangeProductStatusCommand(id, request.status());
        changeProductStatusUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        deleteProductUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}

record UpdateProductRequest(
    String name,
    String slug,
    String description,
    Long categoryId,
    Map<String, Object> attributes
) {}

record ChangeProductStatusRequest(ProductStatus status) {}
