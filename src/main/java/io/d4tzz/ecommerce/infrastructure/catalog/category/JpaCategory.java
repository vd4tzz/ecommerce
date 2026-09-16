package io.d4tzz.ecommerce.infrastructure.catalog.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity(name = "Category")
@Table(
    name = "categories",
    indexes = {
        @Index(name = "idx_categories_parent_id", columnList = "parent_id"),
        @Index(name = "idx_categories_path", columnList = "path")
    }
)
public class JpaCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    private String path;

    @Column(name = "parent_id")
    private Long parentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private JpaCategory parent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb")
    private List<JpaCategoryAttribute> attributes = new ArrayList<>();

    public JpaCategory() {}

    public JpaCategory(Long id, String name, String slug, String path, Long parentId) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.path = path;
        this.parentId = parentId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public JpaCategory getParent() {
        return parent;
    }

    public List<JpaCategoryAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<JpaCategoryAttribute> attributes) {
        this.attributes = attributes != null ? attributes : new ArrayList<>();
    }
}
