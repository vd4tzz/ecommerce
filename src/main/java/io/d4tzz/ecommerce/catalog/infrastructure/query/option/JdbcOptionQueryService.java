package io.d4tzz.ecommerce.catalog.infrastructure.query.option;

import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.catalog.query.option.OptionDetailView;
import io.d4tzz.ecommerce.catalog.query.option.OptionQueryService;
import io.d4tzz.ecommerce.catalog.query.option.OptionSearchCriteria;
import io.d4tzz.ecommerce.catalog.query.option.OptionTableRowView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JdbcOptionQueryService implements OptionQueryService {

    private final JdbcClient jdbcClient;

    public JdbcOptionQueryService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public PageResponse<OptionTableRowView> getOptions(OptionSearchCriteria criteria) {
        OptionSearchCriteria safeCriteria = criteria != null ? criteria : OptionSearchCriteria.empty();

        List<String> where = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (safeCriteria.keyword() != null && !safeCriteria.keyword().isBlank()) {
            where.add("immutable_unaccent(o.name) ILIKE immutable_unaccent(:kwLike)");
            params.put("kwLike", "%" + safeCriteria.keyword().trim() + "%");
        }

        String whereClause = where.isEmpty() ? "" : "WHERE " + String.join("\n  AND ", where);

        String countSql = "SELECT COUNT(*) FROM options o\n" + whereClause;
        var countSpec = jdbcClient.sql(countSql);
        params.forEach(countSpec::param);
        Long total = countSpec.query(Long.class).single();
        long totalElements = total != null ? total : 0L;

        if (totalElements == 0) {
            return PageResponse.empty(safeCriteria.getPage(), safeCriteria.getSize());
        }

        String dataSql =
            """
            SELECT
                o.id,
                o.name,
                (SELECT COUNT(*) FROM option_values ov WHERE ov.option_id = o.id) AS value_count,
                (
                    SELECT string_agg(ov_sub.name, '||')
                    FROM (
                        SELECT name FROM option_values
                        WHERE option_id = o.id
                        ORDER BY id ASC
                        LIMIT 4
                    ) ov_sub
                ) AS preview_csv
            FROM options o
            """ +
            (whereClause.isEmpty() ? "" : whereClause + "\n") +
            """
            ORDER BY o.id DESC
            LIMIT :limit OFFSET :offset""";

        var dataSpec = jdbcClient.sql(dataSql);
        params.forEach(dataSpec::param);
        dataSpec.param("limit", safeCriteria.getSize());
        dataSpec.param("offset", safeCriteria.getOffset());

        List<OptionTableRowView> content = dataSpec
            .query((rs, rowNum) -> {
                String previewCsv = rs.getString("preview_csv");
                List<String> previewValues =
                    previewCsv != null && !previewCsv.isBlank()
                        ? Arrays.asList(previewCsv.split("\\|\\|"))
                        : Collections.emptyList();

                return new OptionTableRowView(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getInt("value_count"),
                    previewValues
                );
            })
            .list();

        return PageResponse.of(content, safeCriteria.getPage(), safeCriteria.getSize(), totalElements);
    }

    @Override
    public Optional<OptionDetailView> getOptionDetail(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        String optionSql = """
            SELECT
                o.id,
                o.name,
                EXISTS (SELECT 1 FROM product_options po WHERE po.option_id = o.id) AS in_use
            FROM options o
            WHERE o.id = :id
        """;

        record RawOption(Long id, String name, boolean inUse) {}

        Optional<RawOption> opt = jdbcClient
            .sql(optionSql)
            .param("id", id)
            .query((rs, rowNum) -> new RawOption(rs.getLong("id"), rs.getString("name"), rs.getBoolean("in_use")))
            .optional();

        if (opt.isEmpty()) {
            return Optional.empty();
        }

        String valuesSql = """
            SELECT
                ov.id,
                ov.name,
                EXISTS (SELECT 1 FROM product_option_values pov WHERE pov.value_id = ov.id) AS in_use
            FROM option_values ov
            WHERE ov.option_id = :id
            ORDER BY ov.id ASC
        """;

        List<OptionDetailView.Value> values = jdbcClient
            .sql(valuesSql)
            .param("id", id)
            .query((rs, rowNum) ->
                new OptionDetailView.Value(rs.getLong("id"), rs.getString("name"), rs.getBoolean("in_use"))
            )
            .list();

        RawOption raw = opt.get();
        return Optional.of(new OptionDetailView(raw.id(), raw.name(), raw.inUse(), values));
    }
}
