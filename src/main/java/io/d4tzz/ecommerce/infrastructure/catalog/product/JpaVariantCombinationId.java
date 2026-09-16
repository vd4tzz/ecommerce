package io.d4tzz.ecommerce.infrastructure.catalog.product;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class JpaVariantCombinationId implements Serializable {

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "option_id")
    private Long optionId;

    @Column(name = "value_id")
    private Long valueId;

    public JpaVariantCombinationId() {}

    public JpaVariantCombinationId(Long variantId, Long optionId, Long valueId) {
        this.variantId = variantId;
        this.optionId = optionId;
        this.valueId = valueId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
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
        JpaVariantCombinationId that = (JpaVariantCombinationId) o;
        return (
            Objects.equals(variantId, that.variantId) &&
            Objects.equals(optionId, that.optionId) &&
            Objects.equals(valueId, that.valueId)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(variantId, optionId, valueId);
    }
}
