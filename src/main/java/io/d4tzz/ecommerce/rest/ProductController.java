package io.d4tzz.ecommerce.rest;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.query.catalog.FilterSidebarView;
import io.d4tzz.ecommerce.query.catalog.ProductCardView;
import io.d4tzz.ecommerce.query.catalog.ProductDetailView;
import io.d4tzz.ecommerce.query.catalog.ProductQueryService;
import io.d4tzz.ecommerce.query.catalog.ProductSearchCriteria;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductQueryService productQueryService;

    public ProductController(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProductCardView>> searchProducts(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) List<Long> optionValueIds,
        @RequestParam(required = false) String sortBy,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) Boolean fullFacets,
        @RequestParam MultiValueMap<String, String> allParams
    ) {
        Map<String, List<String>> attributes = extractAttributes(allParams);
        var criteria = new ProductSearchCriteria(
            keyword,
            categoryId,
            minPrice,
            maxPrice,
            optionValueIds,
            attributes,
            sortBy,
            page,
            size,
            fullFacets
        );
        return ResponseEntity.ok(productQueryService.searchProducts(criteria));
    }

    @GetMapping("/filters")
    public ResponseEntity<FilterSidebarView> getFilters(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) List<Long> optionValueIds,
        @RequestParam(required = false) Boolean fullFacets,
        @RequestParam MultiValueMap<String, String> allParams
    ) {
        Map<String, List<String>> attributes = extractAttributes(allParams);
        var criteria = new ProductSearchCriteria(
            keyword,
            categoryId,
            minPrice,
            maxPrice,
            optionValueIds,
            attributes,
            null,
            0,
            0,
            fullFacets
        );
        return ResponseEntity.ok(productQueryService.getFilterSidebar(criteria));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ProductDetailView> getProductBySlug(@PathVariable String slug) {
        return productQueryService
            .getProductDetailBySlug(slug)
            .filter(p -> !"DRAFT".equalsIgnoreCase(p.status()))
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new NotFoundException("Product not found with slug: " + slug));
    }

    private Map<String, List<String>> extractAttributes(MultiValueMap<String, String> allParams) {
        if (allParams == null || allParams.isEmpty()) {
            return null;
        }
        Map<String, List<String>> attributes = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : allParams.entrySet()) {
            String key = entry.getKey();
            String attrKey = null;
            if (key.startsWith("attr_")) {
                attrKey = key.substring(5);
            } else if (key.startsWith("attr.")) {
                attrKey = key.substring(5);
            } else if (key.startsWith("attrs.")) {
                attrKey = key.substring(6);
            } else if (key.startsWith("attr[") && key.endsWith("]")) {
                attrKey = key.substring(5, key.length() - 1);
            }

            if (attrKey != null && entry.getValue() != null) {
                List<String> values = new ArrayList<>();
                for (String rawVal : entry.getValue()) {
                    if (rawVal != null) {
                        for (String part : rawVal.split(",")) {
                            String trimmed = part.trim();
                            if (!trimmed.isEmpty()) {
                                values.add(trimmed);
                            }
                        }
                    }
                }
                if (!values.isEmpty()) {
                    attributes.computeIfAbsent(attrKey, k -> new ArrayList<>()).addAll(values);
                }
            }
        }
        return attributes.isEmpty() ? null : attributes;
    }
}
