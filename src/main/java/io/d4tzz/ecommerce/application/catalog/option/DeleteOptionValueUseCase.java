package io.d4tzz.ecommerce.application.catalog.option;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.catalog.option.Option;
import io.d4tzz.ecommerce.domain.catalog.option.OptionId;
import io.d4tzz.ecommerce.domain.catalog.option.OptionRepository;
import io.d4tzz.ecommerce.domain.catalog.option.ValueId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteOptionValueUseCase {

    private final OptionRepository optionRepository;

    public DeleteOptionValueUseCase(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    public OptionDto execute(DeleteOptionValueCommand command) {
        return execute(command.optionId(), command.valueId());
    }

    public OptionDto execute(Long optionId, Long valueId) {
        Option option = optionRepository
            .find(OptionId.of(optionId))
            .orElseThrow(() -> new NotFoundException("Option not found with id: " + optionId));

        ValueId valId = ValueId.of(valueId);
        option.removeValue(valId, optionRepository::isValueReferenced);

        Option saved = optionRepository.save(option);
        return OptionDto.from(saved);
    }
}
