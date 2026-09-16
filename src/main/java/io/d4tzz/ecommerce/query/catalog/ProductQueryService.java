package io.d4tzz.ecommerce.query.catalog;

import io.d4tzz.ecommerce.query.PageResponse;
import java.util.Map;
import java.util.Optional;

public interface ProductQueryService {
    /**
     * Tìm kiếm và lọc sản phẩm phân trang dạng lưới (Product Card Grid) cho Storefront.
     */
    PageResponse<ProductCardView> searchProducts(
        ProductSearchCriteria criteria
    );

    /**
     * Lấy toàn bộ dữ liệu bộ lọc (Sidebar) động cho Storefront dựa trên tiêu chí tìm kiếm hiện tại.
     */
    FilterSidebarView getFilterSidebar(ProductSearchCriteria criteria);

    /**
     * Lấy chi tiết sản phẩm theo slug cho trang Product Detail Page (PDP).
     */
    Optional<ProductDetailView> getProductDetailBySlug(String slug);

    /**
     * Lấy chi tiết sản phẩm theo ID (dùng cho Admin hoặc tra cứu nội bộ).
     */
    Optional<ProductDetailView> getProductDetailById(Long id);

    /**
     * Lấy danh sách sản phẩm phân trang và lọc cho Admin Table.
     */
    PageResponse<AdminProductTableRowView> getAdminProducts(
        AdminProductSearchCriteria criteria
    );

    /**
     * Lấy số lượng sản phẩm theo từng trạng thái (Status Tab Badges).
     */
    Map<String, Long> getAdminProductStatusCounts();
}
