package io.d4tzz.ecommerce.infrastructure.catalog.product;

import io.d4tzz.ecommerce.infrastructure.catalog.option.JpaOption;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity(name = "ProductOption")
@Table(name = "product_options")
public class JpaProductOption {

    @EmbeddedId
    private JpaProductOptionId id = new JpaProductOptionId();

    @Column(name = "position", nullable = false)
    private Integer position;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private JpaProduct product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", insertable = false, updatable = false)
    private JpaOption option;

    @Version
    Long version;

    public JpaProductOption() {}

    public JpaProductOption(
        JpaProduct product,
        Long optionId,
        Integer position
    ) {
        this.product = product;
        this.id = new JpaProductOptionId(
            product != null ? product.getId() : null,
            optionId
        );
        this.position = position != null ? position : 0;
    }

    public JpaProductOption(Long productId, Long optionId, Integer position) {
        this.id = new JpaProductOptionId(productId, optionId);
        this.position = position != null ? position : 0;
    }

    public JpaProductOptionId getId() {
        return id;
    }

    public void setId(JpaProductOptionId id) {
        this.id = id;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public JpaProduct getProduct() {
        return product;
    }

    public void setProduct(JpaProduct product) {
        this.product = product;
        if (this.id == null) {
            this.id = new JpaProductOptionId();
        }
        if (product != null && product.getId() != null) {
            this.id.setProductId(product.getId());
        }
    }

    public JpaOption getOption() {
        return option;
    }

    public void setOption(JpaOption option) {
        this.option = option;
    }

    public Long getOptionId() {
        return id != null ? id.getOptionId() : null;
    }

    public Long getProductId() {
        return id != null ? id.getProductId() : null;
    }
}
