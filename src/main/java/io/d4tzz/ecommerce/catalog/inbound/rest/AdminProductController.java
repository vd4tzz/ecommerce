package io.d4tzz.ecommerce.catalog.inbound.rest;

import io.d4tzz.ecommerce.catalog.application.product.ChangeProductStatusUseCase;
import io.d4tzz.ecommerce.catalog.application.product.CreateProductUseCase;
import io.d4tzz.ecommerce.catalog.application.product.DeleteProductUseCase;
import io.d4tzz.ecommerce.catalog.application.product.UpdateProductUseCase;
import io.d4tzz.ecommerce.catalog.application.product.command.ChangeProductStatusCommand;
import io.d4tzz.ecommerce.catalog.application.product.command.CreateProductCommand;
import io.d4tzz.ecommerce.catalog.application.product.command.UpdateProductCommand;
import io.d4tzz.ecommerce.catalog.domain.product.ProductStatus;
import io.d4tzz.ecommerce.catalog.query.product.AdminProductSearchCriteria;
import io.d4tzz.ecommerce.catalog.query.product.AdminProductTableRowView;
import io.d4tzz.ecommerce.catalog.query.product.ProductDetailView;
import io.d4tzz.ecommerce.catalog.query.product.ProductQueryService;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.spring.SpringValidation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ProblemDetail;
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

/**
 * Admin management of products. A new product is a draft; it can be published, archived, and deleted only while it is a draft.
 */
@RestController
@Tag(name = "Admin - Products", description = "Manage products")
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

    @Operation(
        summary = "Create a product",
        description = "Creates a draft product in a category. Options, media and variants are added afterwards. The attributes must match the category's attributes."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product created, with its id"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Category not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "The slug is already used",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
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
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(productId).toUri();
        return ResponseEntity.created(location).body(new CreateProductResponse(productId));
    }

    @Operation(
        summary = "Search products",
        description = "Paged product list for the admin screen. All filters are optional. sortBy can be id_asc, price_asc, price_desc, name_asc or name_desc; the default is newest first."
    )
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "A page of products") })
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

    @Operation(
        summary = "Count products by status",
        description = "Returns how many products there are in each status."
    )
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "The count for each status") })
    @GetMapping("/status-counts")
    public ResponseEntity<Map<String, Long>> getStatusCounts() {
        return ResponseEntity.ok(productQueryService.getAdminProductStatusCounts());
    }

    @Operation(
        summary = "Get a product",
        description = "Returns one product with its options, media and variants, whatever its status."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The product"),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailView> getProductDetail(@PathVariable Long id) {
        return productQueryService
            .getProductDetailById(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));
    }

    @Operation(
        summary = "Update a product",
        description = "Changes the name, slug, description, category and attributes. If the category changes, the attributes are checked against the new category and replace the old ones."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Product updated"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product or category not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Another product already uses the slug",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
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

    @Operation(
        summary = "Change product status",
        description = "Sets the status to PUBLISHED, ARCHIVED or DRAFT. Setting the status it already has changes nothing. A product cannot go back to DRAFT."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Status changed"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input, or the product cannot go back to DRAFT",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(@PathVariable Long id, @RequestBody ChangeProductStatusRequest request) {
        var command = new ChangeProductStatusCommand(id, request.status());
        SpringValidation.validate(command, validator);
        changeProductStatusUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Delete a product",
        description = "Deletes a product together with its options, media and variants. Only draft products can be deleted."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Product deleted"),
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
