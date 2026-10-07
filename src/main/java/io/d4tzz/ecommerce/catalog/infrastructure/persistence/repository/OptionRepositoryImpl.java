package io.d4tzz.ecommerce.catalog.infrastructure.persistence.repository;

import io.d4tzz.ecommerce.catalog.domain.option.Option;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.OptionName;
import io.d4tzz.ecommerce.catalog.domain.option.OptionRepository;
import io.d4tzz.ecommerce.catalog.domain.option.Value;
import io.d4tzz.ecommerce.catalog.domain.option.ValueId;
import io.d4tzz.ecommerce.catalog.domain.option.ValueName;
import io.d4tzz.ecommerce.catalog.infrastructure.persistence.entity.JpaOption;
import io.d4tzz.ecommerce.catalog.infrastructure.persistence.entity.JpaValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
interface SpringOptionRepository extends JpaRepository<JpaOption, Long> {
    boolean existsByName(String name);

    @Query(
        """
        SELECT CASE WHEN COUNT(pov) > 0 THEN true ELSE false END
        FROM ProductOptionValue pov
        WHERE pov.id.valueId = :valueId
        """
    )
    boolean isValueReferenced(@Param("valueId") Long valueId);

    @Query(
        """
        SELECT CASE WHEN COUNT(po) > 0 THEN true ELSE false END
        FROM ProductOption po
        WHERE po.id.optionId = :optionId
        """
    )
    boolean isOptionReferenced(@Param("optionId") Long optionId);

    @Query(
        """
        SELECT COUNT(v)
        FROM OptionValue v
        WHERE v.option.id = :optionId AND v.id IN :valueIds
        """
    )
    long countByOptionIdAndValueIds(@Param("optionId") Long optionId, @Param("valueIds") Collection<Long> valueIds);
}

@Repository
public class OptionRepositoryImpl implements OptionRepository {

    private final SpringOptionRepository springRepo;

    public OptionRepositoryImpl(SpringOptionRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<Option> find(OptionId id) {
        return springRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Option save(Option option) {
        JpaOption jpaOption;

        if (option.getId() == null) {
            jpaOption = new JpaOption(null, option.getName().value());
            for (Value value : option.getValues()) {
                jpaOption.addValue(new JpaValue(null, value.getName().value(), jpaOption));
            }
        } else {
            jpaOption = springRepo.findById(option.getId().value()).orElseThrow();
            jpaOption.setName(option.getName().value());
            syncValues(jpaOption, option.getValues());
        }

        jpaOption = springRepo.save(jpaOption);
        return toDomain(jpaOption);
    }

    private void syncValues(JpaOption jpaOption, List<Value> domainValues) {
        Map<Long, Value> incomingById = new HashMap<>();
        List<Value> newValues = new ArrayList<>();

        for (Value val : domainValues) {
            if (val.getId() == null) {
                newValues.add(val);
            } else {
                incomingById.put(val.getId().value(), val);
            }
        }

        jpaOption.getValues().removeIf(jpaVal -> !incomingById.containsKey(jpaVal.getId()));

        for (JpaValue jpaVal : jpaOption.getValues()) {
            Value incoming = incomingById.get(jpaVal.getId());
            if (incoming != null) {
                jpaVal.setName(incoming.getName().value());
            }
        }

        for (Value newVal : newValues) {
            jpaOption.addValue(new JpaValue(null, newVal.getName().value(), jpaOption));
        }
    }

    private Option toDomain(JpaOption jpaOption) {
        List<Value> domainValues = jpaOption
            .getValues()
            .stream()
            .map(jv -> Value.restore(ValueId.of(jv.getId()), ValueName.of(jv.getName())))
            .toList();

        return new Option(OptionId.of(jpaOption.getId()), OptionName.of(jpaOption.getName()), domainValues);
    }

    @Override
    public void delete(OptionId id) {
        springRepo.deleteById(id.value());
    }

    @Override
    public boolean existsByName(OptionName name) {
        return springRepo.existsByName(name.value());
    }

    @Override
    public boolean isValueReferenced(ValueId id) {
        return springRepo.isValueReferenced(id.value());
    }

    @Override
    public boolean isOptionReferenced(OptionId id) {
        return springRepo.isOptionReferenced(id.value());
    }

    @Override
    public boolean existsById(OptionId id) {
        return springRepo.existsById(id.value());
    }

    @Override
    public boolean areValuesBelongToOption(OptionId optionId, Collection<ValueId> valueIds) {
        if (valueIds.isEmpty()) {
            return true;
        }
        Set<Long> uniqueIds = valueIds.stream().map(ValueId::value).collect(Collectors.toSet());
        long count = springRepo.countByOptionIdAndValueIds(optionId.value(), uniqueIds);
        return count == uniqueIds.size();
    }
}
