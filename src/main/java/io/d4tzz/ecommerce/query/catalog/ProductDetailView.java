package io.d4tzz.ecommerce.query.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductDetailView(
    Long id,
    String name,
    String slug,
    String description,
    String status,
    Long categoryId,
    String categoryName,
    String categorySlug,
    List<BreadcrumbItem> breadcrumbs,
    List<String> mediaUrls,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    List<ProductOptionView> options,
    List<ProductVariantView> variants,
    Map<String, Object> attributes
) {
    public record BreadcrumbItem(
        Long id,
        String name,
        String slug
    ) {}

    public record ProductOptionView(
        Long optionId,
        String optionName,
        List<OptionValueView> values
    ) {
        public record OptionValueView(
            Long valueId,
            String valueName
        ) {}
    }

    public record ProductVariantView(
        Long variantId,
        String sku,
        BigDecimal price,
        BigDecimal compareAt,
        String mediaUrl,
        Integer length,
        Integer width,
        Integer height,
        Integer weight,
        Map<Long, Long> combination
    ) {}
}
