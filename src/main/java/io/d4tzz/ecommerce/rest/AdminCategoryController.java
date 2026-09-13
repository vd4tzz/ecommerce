package io.d4tzz.ecommerce.rest;

import io.d4tzz.ecommerce.application.catalog.category.CreateCategoryCommand;
import io.d4tzz.ecommerce.application.catalog.category.CreateCategoryUseCase;
import io.d4tzz.ecommerce.application.catalog.category.DeleteCategoryUseCase;
import io.d4tzz.ecommerce.application.catalog.category.UpdateCategoryAttributesCommand;
import io.d4tzz.ecommerce.application.catalog.category.UpdateCategoryAttributesUseCase;
import io.d4tzz.ecommerce.application.catalog.category.UpdateCategoryCommand;
import io.d4tzz.ecommerce.application.catalog.category.UpdateCategoryUseCase;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.query.catalog.AdminCategoryDetailView;
import io.d4tzz.ecommerce.query.catalog.AdminCategoryTableRowView;
import io.d4tzz.ecommerce.query.catalog.CategoryQueryService;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
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

    public AdminCategoryController(
        CategoryQueryService categoryQueryService,
        CreateCategoryUseCase createCategoryUseCase,
        UpdateCategoryUseCase updateCategoryUseCase,
        UpdateCategoryAttributesUseCase updateCategoryAttributesUseCase,
        DeleteCategoryUseCase deleteCategoryUseCase
    ) {
        this.categoryQueryService = categoryQueryService;
        this.createCategoryUseCase = createCategoryUseCase;
        this.updateCategoryUseCase = updateCategoryUseCase;
        this.updateCategoryAttributesUseCase = updateCategoryAttributesUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;
    }

    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody CreateCategoryCommand command) {
        var response = createCategoryUseCase.execute(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
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
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody UpdateCategoryRequest request) {
        var command = new UpdateCategoryCommand(id, request.name(), request.slug());
        return ResponseEntity.ok(updateCategoryUseCase.execute(command));
    }

    @PutMapping("/{id}/attributes")
    public ResponseEntity<?> updateCategoryAttributes(
        @PathVariable Long id,
        @RequestBody List<UpdateCategoryAttributesCommand.Attribute> attributes
    ) {
        var command = new UpdateCategoryAttributesCommand(id, attributes);
        return ResponseEntity.ok(updateCategoryAttributesUseCase.execute(command));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        deleteCategoryUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}

record UpdateCategoryRequest(String name, String slug) {}
