package io.d4tzz.ecommerce.catalog.inbound.rest;

import io.d4tzz.ecommerce.catalog.query.product.ProductQueryService;
import io.d4tzz.ecommerce.catalog.query.product.ProductVariantSummaryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin lookup of variants across all products.
 */
@RestController
@Tag(name = "Admin - Variants", description = "Find variants across products")
@RequestMapping("/admin/variants")
public class AdminVariantController {

    private final ProductQueryService productQueryService;

    public AdminVariantController(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
    }

    @Operation(
        summary = "Look up variants",
        description = "Finds variants by product name or SKU, up to the given limit. Used to pick a variant, for example when managing stock."
    )
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "The matching variants") })
    @GetMapping("/lookup")
    public ResponseEntity<List<ProductVariantSummaryView>> lookup(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(productQueryService.lookupVariants(keyword, limit));
    }
}
