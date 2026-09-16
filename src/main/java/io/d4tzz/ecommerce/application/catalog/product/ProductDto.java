package io.d4tzz.ecommerce.application.catalog.product;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.domain.catalog.product.MediaId;
import io.d4tzz.ecommerce.domain.catalog.product.Product;
import io.d4tzz.ecommerce.domain.catalog.product.Variant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ProductDto(
    Long id,
    String name,
    String slug,
    String description,
    Long categoryId,
    String status,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    List<String> media,
    List<ProductOptionDto> options,
    List<ProductVariantDto> variants,
    Map<String, Object> attributes
) {
    public record ProductOptionDto(Long optionId, List<Long> valueIds) {}

    public record ProductVariantDto(
        Long id,
        String sku,
        BigDecimal price,
        BigDecimal compareAt,
        String mediaId,
        int length,
        int width,
        int height,
        int weight,
        Map<Long, Long> combination
    ) {
        public static ProductVariantDto from(Variant v) {
            Map<Long, Long> comb = new HashMap<>();
            v.getCombination().forEach((optId, valId) -> comb.put(optId.value(), valId.value()));
            return new ProductVariantDto(
                v.getId() != null ? v.getId().value() : null,
                v.getSku().value(),
                v.getPrice().amount(),
                v.getCompareAt() != null ? v.getCompareAt().amount() : null,
                v.getMediaId() != null ? v.getMediaId().value() : null,
                v.getDimensions().length(),
                v.getDimensions().width(),
                v.getDimensions().height(),
                v.getWeight().grams(),
                comb
            );
        }
    }

    public static ProductDto from(Product product) {
        List<ProductOptionDto> options = new ArrayList<>();
        product.getOptions().forEach((optId, valIds) ->
            options.add(
                new ProductOptionDto(
                    optId.value(),
                    valIds
                        .stream()
                        .map(v -> v.value())
                        .toList()
                )
            )
        );

        List<ProductVariantDto> variants = product.getVariants().stream().map(ProductVariantDto::from).toList();

        List<String> media = product.getMedia().stream().map(MediaId::value).toList();

        return new ProductDto(
            product.getId() != null ? product.getId().value() : null,
            product.getName().value(),
            product.getSlug().value(),
            product.getDescription() != null ? product.getDescription().value() : "",
            product.getCategoryId().value(),
            product.getStatus().name(),
            product.getMinPrice().map(Money::amount).orElse(null),
            product.getMaxPrice().map(Money::amount).orElse(null),
            media,
            options,
            variants,
            product.getAttributes()
        );
    }

    public static ProductDto simple(Product product) {
        return new ProductDto(
            product.getId() != null ? product.getId().value() : null,
            product.getName().value(),
            product.getSlug().value(),
            product.getDescription() != null ? product.getDescription().value() : "",
            product.getCategoryId().value(),
            product.getStatus().name(),
            product.getMinPrice().map(Money::amount).orElse(null),
            product.getMaxPrice().map(Money::amount).orElse(null),
            null,
            null,
            null,
            null
        );
    }
}
