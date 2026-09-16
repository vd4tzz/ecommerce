package io.d4tzz.ecommerce.infrastructure.catalog.product;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class JpaProductOptionValueId implements Serializable {

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "option_id")
    private Long optionId;

    @Column(name = "value_id")
    private Long valueId;

    public JpaProductOptionValueId() {}

    public JpaProductOptionValueId(Long productId, Long optionId, Long valueId) {
        this.productId = productId;
        this.optionId = optionId;
        this.valueId = valueId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getOptionId() {
        return optionId;
    }

    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }

    public Long getValueId() {
        return valueId;
    }

    public void setValueId(Long valueId) {
        this.valueId = valueId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JpaProductOptionValueId that = (JpaProductOptionValueId) o;
        return (
            Objects.equals(productId, that.productId) &&
            Objects.equals(optionId, that.optionId) &&
            Objects.equals(valueId, that.valueId)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, optionId, valueId);
    }
}
