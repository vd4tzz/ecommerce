package io.d4tzz.ecommerce.catalog.inbound.rest;

import io.d4tzz.ecommerce.catalog.query.category.CategoryQueryService;
import io.d4tzz.ecommerce.catalog.query.category.CategoryTreeNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public category browsing for the storefront.
 */
@RestController
@Tag(name = "Categories", description = "Browse the category tree")
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryQueryService categoryQueryService;

    public CategoryController(CategoryQueryService categoryQueryService) {
        this.categoryQueryService = categoryQueryService;
    }

    @Operation(
        summary = "List categories",
        description = "Returns the whole category tree, each category with its sub-categories."
    )
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "The category tree") })
    @GetMapping
    public ResponseEntity<List<CategoryTreeNode>> getCategories() {
        return ResponseEntity.ok(categoryQueryService.getCategoryTree());
    }
}
