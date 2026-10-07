package io.d4tzz.ecommerce.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class JpaProductOptionId implements Serializable {

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "option_id")
    private Long optionId;

    public JpaProductOptionId() {}

    public JpaProductOptionId(Long productId, Long optionId) {
        this.productId = productId;
        this.optionId = optionId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JpaProductOptionId that = (JpaProductOptionId) o;
        return (
            Objects.equals(productId, that.productId) &&
            Objects.equals(optionId, that.optionId)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, optionId);
    }
}
