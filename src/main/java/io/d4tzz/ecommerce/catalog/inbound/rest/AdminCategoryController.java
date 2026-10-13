package io.d4tzz.ecommerce.catalog.inbound.rest;

import io.d4tzz.ecommerce.catalog.application.category.CreateCategoryUseCase;
import io.d4tzz.ecommerce.catalog.application.category.DeleteCategoryUseCase;
import io.d4tzz.ecommerce.catalog.application.category.UpdateCategoryAttributesUseCase;
import io.d4tzz.ecommerce.catalog.application.category.UpdateCategoryUseCase;
import io.d4tzz.ecommerce.catalog.application.category.command.CreateCategoryCommand;
import io.d4tzz.ecommerce.catalog.application.category.command.UpdateCategoryAttributesCommand;
import io.d4tzz.ecommerce.catalog.application.category.command.UpdateCategoryCommand;
import io.d4tzz.ecommerce.catalog.domain.category.AttributeType;
import io.d4tzz.ecommerce.catalog.query.category.AdminCategoryDetailView;
import io.d4tzz.ecommerce.catalog.query.category.AdminCategoryTableRowView;
import io.d4tzz.ecommerce.catalog.query.category.CategoryQueryService;
import io.d4tzz.ecommerce.catalog.query.category.EffectiveAttributeView;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.spring.SpringValidation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Admin management of categories. A category's attributes describe what products in it can specify, and sub-categories inherit them.
 */
@RestController
@Tag(name = "Admin - Categories", description = "Manage the category tree and its attributes")
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryQueryService categoryQueryService;
    private final CreateCategoryUseCase createCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final UpdateCategoryAttributesUseCase updateCategoryAttributesUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;
    private final Validator validator;

    public AdminCategoryController(
        CategoryQueryService categoryQueryService,
        CreateCategoryUseCase createCategoryUseCase,
        UpdateCategoryUseCase updateCategoryUseCase,
        UpdateCategoryAttributesUseCase updateCategoryAttributesUseCase,
        DeleteCategoryUseCase deleteCategoryUseCase,
        Validator validator
    ) {
        this.categoryQueryService = categoryQueryService;
        this.createCategoryUseCase = createCategoryUseCase;
        this.updateCategoryUseCase = updateCategoryUseCase;
        this.updateCategoryAttributesUseCase = updateCategoryAttributesUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;
        this.validator = validator;
    }

    @Operation(
        summary = "Create a category",
        description = "Creates a category at the top level, or under the parent if parentId is given. A sub-category cannot reuse an attribute code of its ancestors."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Category created, with its id"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Parent category not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "The slug is already used, or an attribute code is duplicated or inherited",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PostMapping
    public ResponseEntity<CreateCategoryResponse> createCategory(@RequestBody CreateCategoryRequest request) {
        List<CreateCategoryCommand.Attribute> attributes =
            request.attributes() != null
                ? request.attributes().stream().map(this::toCreateCommandAttribute).toList()
                : List.of();

        var command = new CreateCategoryCommand(request.name(), request.slug(), request.parentId(), attributes);
        SpringValidation.validate(command, validator);

        Long categoryId = createCategoryUseCase.execute(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(categoryId)
            .toUri();

        return ResponseEntity.created(location).body(new CreateCategoryResponse(categoryId));
    }

    @Operation(
        summary = "List categories as a table",
        description = "Returns every category as a row of a table, in tree order, for the admin screen."
    )
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "The category rows") })
    @GetMapping
    public ResponseEntity<List<AdminCategoryTableRowView>> getCategoryTreeTable() {
        return ResponseEntity.ok(categoryQueryService.getAdminCategoryTreeTable());
    }

    @Operation(
        summary = "Get a category",
        description = "Returns one category with its attributes and whether it can be deleted."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The category"),
        @ApiResponse(
            responseCode = "404",
            description = "Category not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @GetMapping("/{id}")
    public ResponseEntity<AdminCategoryDetailView> getCategoryDetail(@PathVariable Long id) {
        return categoryQueryService
            .getAdminCategoryDetail(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));
    }

    @Operation(
        summary = "Update a category",
        description = "Changes the name and the slug. Keeping the same slug is allowed."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Category updated"),
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
            description = "Another category already uses the slug",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCategory(@PathVariable Long id, @RequestBody UpdateCategoryRequest request) {
        var command = new UpdateCategoryCommand(id, request.name(), request.slug());
        SpringValidation.validate(command, validator);
        updateCategoryUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "List effective attributes",
        description = "Returns the attributes products in this category can use: those of the category itself plus those inherited from its ancestors."
    )
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "The attributes") })
    @GetMapping("/{id}/effective-attributes")
    public ResponseEntity<List<EffectiveAttributeView>> getCategoryAttributes(@PathVariable Long id) {
        return ResponseEntity.ok(categoryQueryService.getCategoryEffectiveAttributes(id));
    }

    @Operation(
        summary = "Update category attributes",
        description = "Replaces the attribute list. The list must include every existing attribute: attributes and values can be added, but not removed, and an attribute cannot change its type."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Attributes updated"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input, or an existing attribute or value would be removed or change type",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Category not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "An attribute code is duplicated, inherited from an ancestor, or already used by a sub-category",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PutMapping("/{id}/attributes")
    public ResponseEntity<Void> updateCategoryAttributes(
        @PathVariable Long id,
        @RequestBody List<CategoryAttributeRequest> attributes
    ) {
        List<UpdateCategoryAttributesCommand.Attribute> commandAttributes =
            attributes != null ? attributes.stream().map(this::toUpdateCommandAttribute).toList() : List.of();

        var command = new UpdateCategoryAttributesCommand(id, commandAttributes);
        SpringValidation.validate(command, validator);
        updateCategoryAttributesUseCase.execute(command);

        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Delete a category",
        description = "Deletes a category that has no sub-categories and no products."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Category deleted"),
        @ApiResponse(
            responseCode = "400",
            description = "The category has sub-categories or products",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Category not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        deleteCategoryUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    private CreateCategoryCommand.Attribute toCreateCommandAttribute(CategoryAttributeRequest attr) {
        var values =
            attr.values() == null
                ? List.<CreateCategoryCommand.Attribute.Value>of()
                : attr
                      .values()
                      .stream()
                      .map(v -> new CreateCategoryCommand.Attribute.Value(v.code(), v.label()))
                      .toList();
        return new CreateCategoryCommand.Attribute(attr.code(), attr.label(), attr.type(), values);
    }

    private UpdateCategoryAttributesCommand.Attribute toUpdateCommandAttribute(CategoryAttributeRequest attr) {
        var values =
            attr.values() == null
                ? List.<UpdateCategoryAttributesCommand.Attribute.Value>of()
                : attr
                      .values()
                      .stream()
                      .map(v -> new UpdateCategoryAttributesCommand.Attribute.Value(v.code(), v.label()))
                      .toList();
        return new UpdateCategoryAttributesCommand.Attribute(attr.code(), attr.label(), attr.type(), values);
    }
}

record CategoryAttributeRequest(String code, String label, AttributeType type, List<Value> values) {
    public record Value(String code, String label) {}
}

record CreateCategoryRequest(String name, String slug, Long parentId, List<CategoryAttributeRequest> attributes) {}

record CreateCategoryResponse(Long id) {}

record UpdateCategoryRequest(String name, String slug) {}
