package io.d4tzz.ecommerce.infrastructure.catalog.persistence.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity(name = "VariantCombination")
@Table(name = "variant_combinations")
public class JpaVariantCombination {

    @EmbeddedId
    private JpaVariantCombinationId id = new JpaVariantCombinationId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("variantId")
    @JoinColumn(name = "variant_id")
    private JpaVariant variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", insertable = false, updatable = false)
    private JpaOption option;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "value_id", insertable = false, updatable = false)
    private JpaValue value;

    @Version
    Long version;

    public JpaVariantCombination() {}

    public JpaVariantCombination(JpaVariant variant, Long optionId, Long valueId) {
        this.variant = variant;
        this.id = new JpaVariantCombinationId(variant.getId(), optionId, valueId);
    }

    public JpaVariantCombination(Long variantId, Long optionId, Long valueId) {
        this.id = new JpaVariantCombinationId(variantId, optionId, valueId);
    }

    public JpaVariantCombinationId getId() {
        return id;
    }

    public void setId(JpaVariantCombinationId id) {
        this.id = id;
    }

    public JpaVariant getVariant() {
        return variant;
    }

    public void setVariant(JpaVariant variant) {
        this.variant = variant;
        if (this.id == null) {
            this.id = new JpaVariantCombinationId();
        }
        if (variant != null && variant.getId() != null) {
            this.id.setVariantId(variant.getId());
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
