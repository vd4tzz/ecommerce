package io.d4tzz.ecommerce.infrastructure.catalog.persistence.repository;

import io.d4tzz.ecommerce.common.Money;
import io.d4tzz.ecommerce.domain.catalog.category.CategoryId;
import io.d4tzz.ecommerce.domain.catalog.option.OptionId;
import io.d4tzz.ecommerce.domain.catalog.option.ValueId;
import io.d4tzz.ecommerce.domain.catalog.product.Description;
import io.d4tzz.ecommerce.domain.catalog.product.Dimensions;
import io.d4tzz.ecommerce.domain.catalog.product.MediaId;
import io.d4tzz.ecommerce.domain.catalog.product.Product;
import io.d4tzz.ecommerce.domain.catalog.product.ProductId;
import io.d4tzz.ecommerce.domain.catalog.product.ProductName;
import io.d4tzz.ecommerce.domain.catalog.product.ProductRepository;
import io.d4tzz.ecommerce.domain.catalog.product.ProductSlug;
import io.d4tzz.ecommerce.domain.catalog.product.ProductStatus;
import io.d4tzz.ecommerce.domain.catalog.product.Sku;
import io.d4tzz.ecommerce.domain.catalog.product.Variant;
import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import io.d4tzz.ecommerce.domain.catalog.product.Weight;
import io.d4tzz.ecommerce.infrastructure.catalog.persistence.entity.JpaProduct;
import io.d4tzz.ecommerce.infrastructure.catalog.persistence.entity.JpaProductMedia;
import io.d4tzz.ecommerce.infrastructure.catalog.persistence.entity.JpaProductOption;
import io.d4tzz.ecommerce.infrastructure.catalog.persistence.entity.JpaProductOptionValue;
import io.d4tzz.ecommerce.infrastructure.catalog.persistence.entity.JpaProductOptionValueId;
import io.d4tzz.ecommerce.infrastructure.catalog.persistence.entity.JpaVariant;
import io.d4tzz.ecommerce.infrastructure.catalog.persistence.entity.JpaVariantCombination;
import io.d4tzz.ecommerce.infrastructure.catalog.persistence.entity.JpaVariantCombinationId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
interface SpringProductRepository extends JpaRepository<JpaProduct, Long> {
    boolean existsBySlug(String slug);

    @Query(
        """
        SELECT CASE WHEN EXISTS (
            SELECT 1 FROM Variant v WHERE v.sku = :sku
        ) THEN true ELSE false END
        """
    )
    boolean existsBySku(@Param("sku") String sku);
}

@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private final SpringProductRepository springRepo;

    public ProductRepositoryImpl(SpringProductRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public boolean existsBySlug(ProductSlug slug) {
        if (slug == null || slug.value() == null) return false;
        return springRepo.existsBySlug(slug.value());
    }

    @Override
    public boolean existsBySku(Sku sku) {
        if (sku == null || sku.value() == null) return false;
        return springRepo.existsBySku(sku.value());
    }

    @Override
    public Optional<Product> find(ProductId id) {
        return springRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Product save(Product product) {
        JpaProduct jpaProduct = product.getId() == null ? saveNew(product) : saveExisting(product);
        return toDomain(jpaProduct);
    }

    private JpaProduct saveNew(Product product) {
        JpaProduct jpaProduct = new JpaProduct();
        mapBasicAttributes(jpaProduct, product);

        int optionPos = 0;
        for (Map.Entry<OptionId, Set<ValueId>> entry : product.getOptions().entrySet()) {
            OptionId optId = entry.getKey();
            Set<ValueId> valIds = entry.getValue();

            jpaProduct.addOption(new JpaProductOption(jpaProduct, optId.value(), optionPos++));

            int valuePos = 0;
            for (ValueId valId : valIds) {
                jpaProduct.addOptionValue(
                    new JpaProductOptionValue(jpaProduct, optId.value(), valId.value(), valuePos++)
                );
            }
        }

        int mediaPos = 0;
        for (MediaId m : product.getMedia()) {
            jpaProduct.addMedia(new JpaProductMedia(m.value(), mediaPos++));
        }

        product.getVariants().forEach(v -> {
            JpaVariant jpaV = new JpaVariant(
                v.getSku().value(),
                v.getPrice().amount(),
                v.getMediaId() != null ? v.getMediaId().value() : null,
                v.getDimensions().length(),
                v.getDimensions().width(),
                v.getDimensions().height(),
                v.getWeight().grams()
            );
            if (v.getCompareAt() != null) {
                jpaV.setCompareAt(v.getCompareAt().amount());
            }
            jpaProduct.addVariant(jpaV);
        });

        for (int i = 0; i < product.getVariants().size(); i++) {
            Variant domainV = product.getVariants().get(i);
            JpaVariant jpaV = jpaProduct.getVariants().get(i);

            domainV
                .getCombination()
                .forEach((optId, valId) ->
                    jpaV.addCombination(new JpaVariantCombination(jpaV, optId.value(), valId.value()))
                );
        }

        return springRepo.saveAndFlush(jpaProduct);
    }

    private JpaProduct saveExisting(Product product) {
        JpaProduct jpaProduct = springRepo.findById(product.getId().value()).orElseThrow();
        mapBasicAttributes(jpaProduct, product);

        syncOptions(jpaProduct, product.getOptions());
        syncMedia(jpaProduct, product.getMedia());
        syncVariants(jpaProduct, product.getVariants());

        return springRepo.saveAndFlush(jpaProduct);
    }

    @Override
    public void delete(ProductId id) {
        springRepo.deleteById(id.value());
    }

    // ----- Private helpers -----

    private void mapBasicAttributes(JpaProduct jpaProduct, Product product) {
        jpaProduct.setName(product.getName().value());
        jpaProduct.setSlug(product.getSlug().value());
        jpaProduct.setDescription(product.getDescription() != null ? product.getDescription().value() : "");
        jpaProduct.setCategoryId(product.getCategoryId().value());
        jpaProduct.setStatus(product.getStatus().name());
        jpaProduct.setMinPrice(product.getMinPrice().map(Money::amount).orElse(null));
        jpaProduct.setMaxPrice(product.getMaxPrice().map(Money::amount).orElse(null));
        jpaProduct.setAttributes(product.getAttributes());
    }

    private void syncOptions(JpaProduct jpaProduct, Map<OptionId, Set<ValueId>> options) {
        Long productId = jpaProduct.getId();

        // Sync options
        Map<Long, Integer> incomingOptions = new HashMap<>();
        int optPos = 0;
        for (OptionId optId : options.keySet()) {
            incomingOptions.put(optId.value(), optPos++);
        }

        jpaProduct.getOptions().removeIf(o -> !incomingOptions.containsKey(o.getOptionId()));

        Map<Long, JpaProductOption> existingOptions = jpaProduct
            .getOptions()
            .stream()
            .collect(Collectors.toMap(JpaProductOption::getOptionId, o -> o));

        for (Map.Entry<Long, Integer> entry : incomingOptions.entrySet()) {
            Long optId = entry.getKey();
            int pos = entry.getValue();
            JpaProductOption existing = existingOptions.get(optId);
            if (existing != null) {
                existing.setPosition(pos);
            } else {
                jpaProduct.addOption(new JpaProductOption(jpaProduct, optId, pos));
            }
        }

        // Sync option values
        Map<JpaProductOptionValueId, Integer> incomingOptionValues = new HashMap<>();
        for (Map.Entry<OptionId, Set<ValueId>> entry : options.entrySet()) {
            Long optId = entry.getKey().value();
            Set<ValueId> valIds = entry.getValue();
            int valPos = 0;
            for (ValueId valId : valIds) {
                incomingOptionValues.put(new JpaProductOptionValueId(productId, optId, valId.value()), valPos++);
            }
        }

        jpaProduct.getOptionValues().removeIf(ov -> !incomingOptionValues.containsKey(ov.getId()));

        Map<JpaProductOptionValueId, JpaProductOptionValue> existingValues = jpaProduct
            .getOptionValues()
            .stream()
            .collect(Collectors.toMap(JpaProductOptionValue::getId, ov -> ov));

        for (Map.Entry<JpaProductOptionValueId, Integer> entry : incomingOptionValues.entrySet()) {
            JpaProductOptionValueId id = entry.getKey();
            int pos = entry.getValue();
            JpaProductOptionValue existing = existingValues.get(id);
            if (existing != null) {
                existing.setPosition(pos);
            } else {
                jpaProduct.addOptionValue(
                    new JpaProductOptionValue(jpaProduct, id.getOptionId(), id.getValueId(), pos)
                );
            }
        }
    }

    private void syncMedia(JpaProduct jpaProduct, Set<MediaId> media) {
        Map<String, Integer> incomingMedia = new HashMap<>();
        int pos = 0;
        for (MediaId m : media) {
            incomingMedia.put(m.value(), pos++);
        }

        jpaProduct.getMedia().removeIf(m -> !incomingMedia.containsKey(m.getId()));

        Map<String, JpaProductMedia> existingMedia = jpaProduct
            .getMedia()
            .stream()
            .collect(Collectors.toMap(JpaProductMedia::getId, m -> m, (existing, replace) -> existing));

        for (Map.Entry<String, Integer> entry : incomingMedia.entrySet()) {
            String id = entry.getKey();
            int targetPos = entry.getValue();
            JpaProductMedia existing = existingMedia.get(id);
            if (existing != null) {
                existing.setPosition(targetPos);
            } else {
                jpaProduct.addMedia(new JpaProductMedia(id, targetPos));
            }
        }
    }

    private void syncVariants(JpaProduct jpaProduct, List<Variant> domainVariants) {
        Map<Long, Variant> incomingById = new HashMap<>();
        List<Variant> newVariants = new ArrayList<>();

        for (Variant v : domainVariants) {
            if (v.getId() == null) {
                newVariants.add(v);
            } else {
                incomingById.put(v.getId().value(), v);
            }
        }

        jpaProduct.getVariants().removeIf(jpaV -> !incomingById.containsKey(jpaV.getId()));

        for (JpaVariant jpaV : jpaProduct.getVariants()) {
            Variant incoming = incomingById.get(jpaV.getId());
            if (incoming != null) {
                jpaV.setSku(incoming.getSku().value());
                jpaV.setPrice(incoming.getPrice().amount());
                jpaV.setCompareAt(incoming.getCompareAt() != null ? incoming.getCompareAt().amount() : null);
                jpaV.setMediaId(incoming.getMediaId() != null ? incoming.getMediaId().value() : null);
                jpaV.setLength(incoming.getDimensions().length());
                jpaV.setWidth(incoming.getDimensions().width());
                jpaV.setHeight(incoming.getDimensions().height());
                jpaV.setWeight(incoming.getWeight().grams());

                syncVariantCombinations(jpaV, incoming.getCombination());
            }
        }

        for (Variant newV : newVariants) {
            JpaVariant newJpaVariant = new JpaVariant(
                newV.getSku().value(),
                newV.getPrice().amount(),
                newV.getMediaId() != null ? newV.getMediaId().value() : null,
                newV.getDimensions().length(),
                newV.getDimensions().width(),
                newV.getDimensions().height(),
                newV.getWeight().grams()
            );
            if (newV.getCompareAt() != null) {
                newJpaVariant.setCompareAt(newV.getCompareAt().amount());
            }
            jpaProduct.addVariant(newJpaVariant);

            newV.getCombination().forEach((optId, valId) ->
                newJpaVariant.addCombination(new JpaVariantCombination(newJpaVariant, optId.value(), valId.value()))
            );
        }
    }

    private void syncVariantCombinations(JpaVariant jpaVariant, Map<OptionId, ValueId> combination) {
        Long variantId = jpaVariant.getId();
        Set<JpaVariantCombinationId> incomingIds = combination
            .entrySet()
            .stream()
            .map(e -> new JpaVariantCombinationId(variantId, e.getKey().value(), e.getValue().value()))
            .collect(Collectors.toSet());

        jpaVariant.getCombinations().removeIf(c -> !incomingIds.contains(c.getId()));

        Set<JpaVariantCombinationId> existingIds = jpaVariant
            .getCombinations()
            .stream()
            .map(JpaVariantCombination::getId)
            .collect(Collectors.toSet());

        for (JpaVariantCombinationId id : incomingIds) {
            if (!existingIds.contains(id)) {
                jpaVariant.addCombination(new JpaVariantCombination(jpaVariant, id.getOptionId(), id.getValueId()));
            }
        }
    }

    private Product toDomain(JpaProduct jpaProduct) {
        List<JpaProductOption> sortedOptions = jpaProduct
            .getOptions()
            .stream()
            .sorted(Comparator.comparingInt(JpaProductOption::getPosition))
            .toList();

        Map<Long, List<JpaProductOptionValue>> valuesByOption = jpaProduct
            .getOptionValues()
            .stream()
            .collect(Collectors.groupingBy(JpaProductOptionValue::getOptionId));

        LinkedHashMap<OptionId, Set<ValueId>> options = new LinkedHashMap<>();
        if (!sortedOptions.isEmpty()) {
            for (JpaProductOption opt : sortedOptions) {
                List<JpaProductOptionValue> optValues = valuesByOption.getOrDefault(opt.getOptionId(), List.of());
                Set<ValueId> sortedValues = optValues
                    .stream()
                    .sorted(Comparator.comparingInt(JpaProductOptionValue::getPosition))
                    .map(ov -> ValueId.of(ov.getValueId()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
                options.put(OptionId.of(opt.getOptionId()), sortedValues);
            }
        } else {
            // Defensive fallback if options table was not populated
            for (Map.Entry<Long, List<JpaProductOptionValue>> entry : valuesByOption.entrySet()) {
                Set<ValueId> sortedValues = entry
                    .getValue()
                    .stream()
                    .sorted(Comparator.comparingInt(JpaProductOptionValue::getPosition))
                    .map(ov -> ValueId.of(ov.getValueId()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
                options.put(OptionId.of(entry.getKey()), sortedValues);
            }
        }

        Set<MediaId> media = jpaProduct
            .getMedia()
            .stream()
            .sorted(Comparator.comparingInt(JpaProductMedia::getPosition))
            .map(m -> MediaId.of(m.getId()))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Variant> variants = jpaProduct.getVariants().stream().map(this::toDomainVariant).toList();

        return restore(
            ProductId.of(jpaProduct.getId()),
            ProductName.of(jpaProduct.getName()),
            ProductSlug.of(jpaProduct.getSlug()),
            Description.of(jpaProduct.getDescription()),
            CategoryId.of(jpaProduct.getCategoryId()),
            ProductStatus.valueOf(jpaProduct.getStatus()),
            options,
            media,
            variants,
            jpaProduct.getAttributes()
        );
    }

    private Variant toDomainVariant(JpaVariant jpaVariant) {
        Map<OptionId, ValueId> combination = new HashMap<>();
        for (JpaVariantCombination c : jpaVariant.getCombinations()) {
            combination.put(OptionId.of(c.getOptionId()), ValueId.of(c.getValueId()));
        }

        Dimensions dimensions = Dimensions.of(jpaVariant.getLength(), jpaVariant.getWidth(), jpaVariant.getHeight());

        Weight weight = Weight.ofGrams(jpaVariant.getWeight());

        Money compareAt = jpaVariant.getCompareAt() != null ? Money.of(jpaVariant.getCompareAt()) : null;

        return Variant.restore(
            VariantId.of(jpaVariant.getId()),
            Sku.of(jpaVariant.getSku()),
            Money.of(jpaVariant.getPrice()),
            compareAt,
            jpaVariant.getMediaId() != null ? MediaId.of(jpaVariant.getMediaId()) : null,
            dimensions,
            weight,
            combination
        );
    }
}
