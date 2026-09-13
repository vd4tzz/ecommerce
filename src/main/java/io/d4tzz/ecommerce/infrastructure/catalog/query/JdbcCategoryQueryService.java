package io.d4tzz.ecommerce.infrastructure.catalog.query;

import io.d4tzz.ecommerce.query.catalog.AdminCategoryDetailView;
import io.d4tzz.ecommerce.query.catalog.AdminCategoryTableRowView;
import io.d4tzz.ecommerce.query.catalog.CategoryQueryService;
import io.d4tzz.ecommerce.query.catalog.CategoryTreeNode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class JdbcCategoryQueryService implements CategoryQueryService {

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public JdbcCategoryQueryService(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<CategoryTreeNode> getCategoryTree() {
        List<RawCategoryNode> rawNodes = jdbcClient
            .sql(
                """
                SELECT id, name, slug, path, parent_id
                FROM categories
                ORDER BY id ASC
                """
            )
            .query((rs, rowNum) ->
                new RawCategoryNode(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("slug"),
                    rs.getString("path"),
                    rs.getObject("parent_id", Long.class)
                )
            )
            .list();

        Map<Long, List<RawCategoryNode>> byParent = rawNodes
            .stream()
            .collect(Collectors.groupingBy(n -> n.parentId() == null ? -1L : n.parentId()));

        return buildTreeRecursive(-1L, byParent);
    }

    private List<CategoryTreeNode> buildTreeRecursive(
        Long parentId,
        Map<Long, List<RawCategoryNode>> childrenByParent
    ) {
        List<RawCategoryNode> children = childrenByParent.getOrDefault(parentId, List.of());
        return children
            .stream()
            .map(c ->
                new CategoryTreeNode(
                    c.id(),
                    c.name(),
                    c.slug(),
                    c.path(),
                    c.parentId(),
                    buildTreeRecursive(c.id(), childrenByParent)
                )
            )
            .toList();
    }

    private record RawCategoryNode(Long id, String name, String slug, String path, Long parentId) {}

    @Override
    public List<AdminCategoryTableRowView> getAdminCategoryTreeTable() {
        String sql = """
            WITH RECURSIVE category_tree AS (
                SELECT
                    c.id,
                    c.name,
                    c.slug,
                    c.path,
                    c.parent_id,
                    c.attributes,
                    1 AS level,
                    ARRAY[c.id] AS sort_path
                FROM categories c
                WHERE c.parent_id IS NULL

                UNION ALL

                SELECT
                    child.id,
                    child.name,
                    child.slug,
                    child.path,
                    child.parent_id,
                    child.attributes,
                    parent.level + 1 AS level,
                    parent.sort_path || child.id AS sort_path
                FROM categories child
                JOIN category_tree parent ON child.parent_id = parent.id
            )
            SELECT
                ct.id,
                ct.name,
                ct.slug,
                ct.path,
                ct.parent_id,
                ct.level,
                EXISTS (SELECT 1 FROM categories sub WHERE sub.parent_id = ct.id) AS has_children,
                (SELECT COUNT(*) FROM products p WHERE p.category_id = ct.id)     AS product_count,
                COALESCE(jsonb_array_length(ct.attributes), 0)                    AS attribute_count
            FROM category_tree ct
            ORDER BY ct.sort_path
        """;

        return jdbcClient
            .sql(sql)
            .query((rs, rowNum) ->
                new AdminCategoryTableRowView(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("slug"),
                    rs.getString("path"),
                    rs.getObject("parent_id", Long.class),
                    rs.getInt("level"),
                    rs.getBoolean("has_children"),
                    rs.getLong("product_count"),
                    rs.getInt("attribute_count")
                )
            )
            .list();
    }

    @Override
    public Optional<AdminCategoryDetailView> getAdminCategoryDetail(Long id) {
        String sql = """
            SELECT
                c.id,
                c.name,
                c.slug,
                c.parent_id,
                c.path,
                c.attributes,
                NOT EXISTS (SELECT 1 FROM categories sub WHERE sub.parent_id = c.id)
                AND NOT EXISTS (SELECT 1 FROM products p WHERE p.category_id = c.id) AS is_deletable
            FROM categories c
            WHERE c.id = :id
        """;

        return jdbcClient
            .sql(sql)
            .param("id", id)
            .query((rs, rowNum) -> {
                String attributesJson = rs.getString("attributes");
                List<AdminCategoryDetailView.AttributeConfigView> attributes = parseAttributes(attributesJson);

                return new AdminCategoryDetailView(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("slug"),
                    rs.getObject("parent_id", Long.class),
                    rs.getString("path"),
                    rs.getBoolean("is_deletable"),
                    attributes
                );
            })
            .optional();
    }

    private List<AdminCategoryDetailView.AttributeConfigView> parseAttributes(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(
                json,
                new TypeReference<List<AdminCategoryDetailView.AttributeConfigView>>() {}
            );
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
