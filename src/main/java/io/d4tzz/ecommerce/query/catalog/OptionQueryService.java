package io.d4tzz.ecommerce.query.catalog;

import io.d4tzz.ecommerce.query.PageResponse;
import java.util.Optional;

public interface OptionQueryService {
    /**
     * Lấy danh sách Options phân trang và tìm kiếm (dùng chung cho cả Bảng quản trị lẫn Dropdown).
     * Điều khiển tính toán trạng thái sử dụng qua criteria.isIncludeInUse().
     */
    PageResponse<OptionTableRowView> getOptions(OptionSearchCriteria criteria);

    /**
     * Lấy chi tiết 1 Option và toàn bộ danh sách Values của nó để xem/chỉnh sửa hoặc khi chọn Option.
     */
    Optional<OptionDetailView> getOptionDetail(Long id);
}
