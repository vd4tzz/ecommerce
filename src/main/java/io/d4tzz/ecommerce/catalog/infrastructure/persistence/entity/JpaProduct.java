package io.d4tzz.ecommerce.catalog.infrastructure.persistence.entity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity(name = "Product")
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_products_category_id", columnList = "category_id"),
        @Index(name = "idx_products_status_category_id", columnList = "status, category_id")
    }
)
public class JpaProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category_id")
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", updatable = false, insertable = false)
    private JpaCategory category;

    private String status;

    @Column(name = "min_price")
    private BigDecimal minPrice;

    @Column(name = "max_price")
    private BigDecimal maxPrice;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb")
    private Map<String, List<String>> attributes = new HashMap<>();

    @OneToMany(
        mappedBy = "product",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<JpaProductOption> options = new ArrayList<>();

    @OneToMany(
        mappedBy = "product",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<JpaProductOptionValue> optionValues = new ArrayList<>();

    @OneToMany(
        mappedBy = "product",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<JpaProductMedia> media = new ArrayList<>();

    @OneToMany(
        mappedBy = "product",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<JpaVariant> variants = new ArrayList<>();

    public JpaProduct() {}

    public void addOption(JpaProductOption option) {
        this.options.add(option);
        option.setProduct(this);
    }

    public void addOptionValue(JpaProductOptionValue optionValue) {
        this.optionValues.add(optionValue);
        optionValue.setProduct(this);
    }

    public void addMedia(JpaProductMedia media) {
        this.media.add(media);
        media.setProduct(this);
    }

    public void addVariant(JpaVariant variant) {
        this.variants.add(variant);
        variant.setProduct(this);
    }

    public void removeVariant(JpaVariant variant) {
        this.variants.remove(variant);
        variant.setProduct(null);
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public List<JpaProductOption> getOptions() {
        return options;
    }

    public void setOptions(List<JpaProductOption> options) {
        this.options = options;
    }

    public List<JpaProductOptionValue> getOptionValues() {
        return optionValues;
    }

    public void setOptionValues(List<JpaProductOptionValue> optionValues) {
        this.optionValues = optionValues;
    }

    public List<JpaProductMedia> getMedia() {
        return media;
    }

    public void setMedia(List<JpaProductMedia> media) {
        this.media = media;
    }

    public List<JpaVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<JpaVariant> variants) {
        this.variants = variants;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes != null ? attributes : new HashMap<>();
    }
}
