package io.d4tzz.ecommerce.catalog.query.option;

import io.d4tzz.ecommerce.query.PageResponse;
import java.util.Optional;

public interface OptionQueryService {
    PageResponse<OptionTableRowView> getOptions(OptionSearchCriteria criteria);

    Optional<OptionDetailView> getOptionDetail(Long id);
}
