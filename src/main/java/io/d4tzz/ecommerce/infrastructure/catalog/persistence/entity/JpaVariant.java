package io.d4tzz.ecommerce.infrastructure.catalog.persistence.entity;

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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.BatchSize;

@Entity(name = "Variant")
@Table(
    name = "variants",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_variants_sku", columnNames = "sku")
    },
    indexes = {
        @Index(name = "idx_variants_product_id", columnList = "product_id")
    }
)
public class JpaVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String sku;

    private BigDecimal price;

    @Column(name = "compare_at")
    private BigDecimal compareAt;

    @Column(name = "media_id")
    private String mediaId;

    @Column(name = "length")
    private Integer length;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "weight")
    private Integer weight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private JpaProduct product;

    @OneToMany(
        mappedBy = "variant",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @BatchSize(size = 30)
    private List<JpaVariantCombination> combinations = new ArrayList<>();

    public JpaVariant() {}

    public JpaVariant(String sku, BigDecimal price, String mediaId) {
        this.sku = sku;
        this.price = price;
        this.mediaId = mediaId;
    }

    public JpaVariant(Long id, String sku, BigDecimal price, String mediaId) {
        this.id = id;
        this.sku = sku;
        this.price = price;
        this.mediaId = mediaId;
    }

    public JpaVariant(
        String sku,
        BigDecimal price,
        String mediaId,
        Integer length,
        Integer width,
        Integer height
    ) {
        this(sku, price, mediaId, length, width, height, null);
    }

    public JpaVariant(
        String sku,
        BigDecimal price,
        String mediaId,
        Integer length,
        Integer width,
        Integer height,
        Integer weight
    ) {
        this.sku = sku;
        this.price = price;
        this.mediaId = mediaId;
        this.length = length;
        this.width = width;
        this.height = height;
        this.weight = weight;
    }

    public JpaVariant(
        Long id,
        String sku,
        BigDecimal price,
        String mediaId,
        Integer length,
        Integer width,
        Integer height
    ) {
        this(id, sku, price, mediaId, length, width, height, null);
    }

    public JpaVariant(
        Long id,
        String sku,
        BigDecimal price,
        String mediaId,
        Integer length,
        Integer width,
        Integer height,
        Integer weight
    ) {
        this.id = id;
        this.sku = sku;
        this.price = price;
        this.mediaId = mediaId;
        this.length = length;
        this.width = width;
        this.height = height;
        this.weight = weight;
    }

    public void addCombination(JpaVariantCombination combination) {
        this.combinations.add(combination);
        combination.setVariant(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getCompareAt() {
        return compareAt;
    }

    public void setCompareAt(BigDecimal compareAt) {
        this.compareAt = compareAt;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public JpaProduct getProduct() {
        return product;
    }

    public void setProduct(JpaProduct product) {
        this.product = product;
    }

    public List<JpaVariantCombination> getCombinations() {
        return combinations;
    }

    public void setCombinations(List<JpaVariantCombination> combinations) {
        this.combinations = combinations;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }
}
