package io.d4tzz.ecommerce.query.catalog.product;

import io.d4tzz.ecommerce.query.PageResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProductQueryService {
    PageResponse<ProductCardView> searchProducts(ProductSearchCriteria criteria);

    FilterSidebarView getFilterSidebar(ProductSearchCriteria criteria);

    Optional<ProductDetailView> getProductDetailBySlug(String slug);

    Optional<ProductDetailView> getProductDetailById(Long id);

    PageResponse<AdminProductTableRowView> getAdminProducts(AdminProductSearchCriteria criteria);

    Map<String, Long> getAdminProductStatusCounts();

    List<ProductVariantSummaryView> lookupVariants(String keyword, int limit);

    default List<ProductVariantSummaryView> lookupVariants(String keyword) {
        return lookupVariants(keyword, 20);
    }
}
