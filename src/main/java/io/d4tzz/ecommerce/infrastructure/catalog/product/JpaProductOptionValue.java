package io.d4tzz.ecommerce.infrastructure.catalog.product;

import io.d4tzz.ecommerce.infrastructure.catalog.option.JpaOption;
import io.d4tzz.ecommerce.infrastructure.catalog.option.JpaValue;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity(name = "ProductOptionValue")
@Table(name = "product_option_values")
public class JpaProductOptionValue {

    @EmbeddedId
    private JpaProductOptionValueId id = new JpaProductOptionValueId();

    @Column(name = "position", nullable = false)
    private Integer position;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private JpaProduct product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", insertable = false, updatable = false)
    private JpaOption option;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "value_id", insertable = false, updatable = false)
    private JpaValue value;

    @Version
    Long version;

    public JpaProductOptionValue() {}

    public JpaProductOptionValue(
        JpaProduct product,
        Long optionId,
        Long valueId,
        Integer position
    ) {
        this.product = product;
        this.id = new JpaProductOptionValueId(
            product != null ? product.getId() : null,
            optionId,
            valueId
        );
        this.position = position != null ? position : 0;
    }

    public JpaProductOptionValue(Long productId, Long optionId, Long valueId) {
        this(productId, optionId, valueId, 0);
    }

    public JpaProductOptionValue(
        Long productId,
        Long optionId,
        Long valueId,
        Integer position
    ) {
        this.id = new JpaProductOptionValueId(productId, optionId, valueId);
        this.position = position != null ? position : 0;
    }

    public JpaProductOptionValueId getId() {
        return id;
    }

    public void setId(JpaProductOptionValueId id) {
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
            this.id = new JpaProductOptionValueId();
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

    public JpaValue getValue() {
        return value;
    }

    public void setValue(JpaValue value) {
        this.value = value;
    }

    public Long getOptionId() {
        return id != null ? id.getOptionId() : null;
    }

    public Long getValueId() {
        return id != null ? id.getValueId() : null;
    }
}
