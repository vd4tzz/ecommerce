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
import java.net.URI;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
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

    @GetMapping
    public ResponseEntity<List<AdminCategoryTableRowView>> getCategoryTreeTable() {
        return ResponseEntity.ok(categoryQueryService.getAdminCategoryTreeTable());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminCategoryDetailView> getCategoryDetail(@PathVariable Long id) {
        return categoryQueryService
            .getAdminCategoryDetail(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCategory(@PathVariable Long id, @RequestBody UpdateCategoryRequest request) {
        var command = new UpdateCategoryCommand(id, request.name(), request.slug());
        SpringValidation.validate(command, validator);
        updateCategoryUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/effective-attributes")
    public ResponseEntity<List<EffectiveAttributeView>> getCategoryAttributes(@PathVariable Long id) {
        return ResponseEntity.ok(categoryQueryService.getCategoryEffectiveAttributes(id));
    }

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
