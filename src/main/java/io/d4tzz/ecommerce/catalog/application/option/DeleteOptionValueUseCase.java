package io.d4tzz.ecommerce.catalog.application.option;

import io.d4tzz.ecommerce.catalog.application.option.command.DeleteOptionValueCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.option.Option;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.OptionRepository;
import io.d4tzz.ecommerce.catalog.domain.option.ValueId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteOptionValueUseCase {

    private final OptionRepository optionRepository;

    public DeleteOptionValueUseCase(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    public void execute(DeleteOptionValueCommand command) {
        Option option = optionRepository
            .find(OptionId.of(command.optionId()))
            .orElseThrow(() -> new NotFoundException("Option not found with id: " + command.optionId()));

        option.removeValue(ValueId.of(command.valueId()), optionRepository::isValueReferenced);

        optionRepository.save(option);
    }
}
