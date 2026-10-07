package io.d4tzz.ecommerce.catalog.api;

import io.d4tzz.ecommerce.catalog.query.product.ProductQueryService;
import io.d4tzz.ecommerce.catalog.query.product.ProductVariantSummaryView;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/variants")
public class AdminVariantController {

    private final ProductQueryService productQueryService;

    public AdminVariantController(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
    }

    @GetMapping("/lookup")
    public ResponseEntity<List<ProductVariantSummaryView>> lookup(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(productQueryService.lookupVariants(keyword, limit));
    }
}
