package io.d4tzz.ecommerce.catalog.api;

import io.d4tzz.ecommerce.catalog.query.category.CategoryQueryService;
import io.d4tzz.ecommerce.catalog.query.category.CategoryTreeNode;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryQueryService categoryQueryService;

    public CategoryController(CategoryQueryService categoryQueryService) {
        this.categoryQueryService = categoryQueryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryTreeNode>> getCategories() {
        return ResponseEntity.ok(categoryQueryService.getCategoryTree());
    }
}
