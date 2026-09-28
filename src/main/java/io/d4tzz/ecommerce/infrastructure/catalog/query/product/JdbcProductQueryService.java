package io.d4tzz.ecommerce.infrastructure.catalog.query.product;

import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.query.catalog.product.AdminProductSearchCriteria;
import io.d4tzz.ecommerce.query.catalog.product.AdminProductTableRowView;
import io.d4tzz.ecommerce.query.catalog.product.FilterSidebarView;
import io.d4tzz.ecommerce.query.catalog.product.ProductCardView;
import io.d4tzz.ecommerce.query.catalog.product.ProductDetailView;
import io.d4tzz.ecommerce.query.catalog.product.ProductQueryService;
import io.d4tzz.ecommerce.query.catalog.product.ProductSearchCriteria;
import io.d4tzz.ecommerce.query.catalog.product.ProductVariantSummaryView;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JdbcProductQueryService implements ProductQueryService {

    private final ProductSearchQuery searchQuery;
    private final ProductFacetQuery facetQuery;
    private final ProductDetailQuery detailQuery;
    private final AdminProductSearchQuery adminQuery;
    private final VariantSearchQuery variantSearchQuery;

    public JdbcProductQueryService(
        ProductSearchQuery searchQuery,
        ProductFacetQuery facetQuery,
        ProductDetailQuery detailQuery,
        AdminProductSearchQuery adminQuery,
        VariantSearchQuery variantSearchQuery
    ) {
        this.searchQuery = searchQuery;
        this.facetQuery = facetQuery;
        this.detailQuery = detailQuery;
        this.adminQuery = adminQuery;
        this.variantSearchQuery = variantSearchQuery;
    }

    @Override
    public PageResponse<ProductCardView> searchProducts(ProductSearchCriteria criteria) {
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
    public PageResponse<AdminProductTableRowView> getAdminProducts(AdminProductSearchCriteria criteria) {
        return adminQuery.search(criteria);
    }

    @Override
    public Map<String, Long> getAdminProductStatusCounts() {
        return adminQuery.getProductStatusCounts();
    }

    @Override
    public List<ProductVariantSummaryView> lookupVariants(String keyword, int limit) {
        return variantSearchQuery.execute(keyword, limit);
    }
}
