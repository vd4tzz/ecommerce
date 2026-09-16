package io.d4tzz.ecommerce.infrastructure.catalog.query;

import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.query.catalog.AdminProductSearchCriteria;
import io.d4tzz.ecommerce.query.catalog.AdminProductTableRowView;
import io.d4tzz.ecommerce.query.catalog.FilterSidebarView;
import io.d4tzz.ecommerce.query.catalog.ProductCardView;
import io.d4tzz.ecommerce.query.catalog.ProductDetailView;
import io.d4tzz.ecommerce.query.catalog.ProductQueryService;
import io.d4tzz.ecommerce.query.catalog.ProductSearchCriteria;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Facade mỏng triển khai ProductQueryService, điều phối tới các Query Component chuyên biệt.
 */
@Service
@Transactional(readOnly = true)
public class JdbcProductQueryService implements ProductQueryService {

    private final ProductSearchQuery searchQuery;
    private final ProductFacetQuery facetQuery;
    private final ProductDetailQuery detailQuery;
    private final AdminProductQuery adminQuery;

    public JdbcProductQueryService(
        ProductSearchQuery searchQuery,
        ProductFacetQuery facetQuery,
        ProductDetailQuery detailQuery,
        AdminProductQuery adminQuery
    ) {
        this.searchQuery = searchQuery;
        this.facetQuery = facetQuery;
        this.detailQuery = detailQuery;
        this.adminQuery = adminQuery;
    }

    @Override
    public PageResponse<ProductCardView> searchProducts(
        ProductSearchCriteria criteria
    ) {
        return searchQuery.execute(criteria);
    }

    @Override
    public FilterSidebarView getFilterSidebar(ProductSearchCriteria criteria) {
        return facetQuery.execute(criteria);
    }

    @Override
    public Optional<ProductDetailView> getProductDetailBySlug(String slug) {
        return detailQuery.execute(slug);
    }

    @Override
    public Optional<ProductDetailView> getProductDetailById(Long id) {
        return detailQuery.execute(id);
    }

    @Override
    public PageResponse<AdminProductTableRowView> getAdminProducts(
        AdminProductSearchCriteria criteria
    ) {
        return adminQuery.getAdminProducts(criteria);
    }

    @Override
    public Map<String, Long> getAdminProductStatusCounts() {
        return adminQuery.getProductStatusCounts();
    }
}
