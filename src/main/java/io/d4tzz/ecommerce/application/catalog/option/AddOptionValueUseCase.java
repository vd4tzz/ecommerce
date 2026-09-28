package io.d4tzz.ecommerce.application.catalog.option;

import io.d4tzz.ecommerce.application.catalog.option.command.AddOptionValueCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.catalog.option.Option;
import io.d4tzz.ecommerce.domain.catalog.option.OptionId;
import io.d4tzz.ecommerce.domain.catalog.option.OptionRepository;
import io.d4tzz.ecommerce.domain.catalog.option.ValueName;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddOptionValueUseCase {

    private final OptionRepository optionRepository;

    public AddOptionValueUseCase(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    public void execute(AddOptionValueCommand command) {
        Option option = optionRepository
            .find(OptionId.of(command.optionId()))
            .orElseThrow(() -> new NotFoundException("Option not found with id: " + command.optionId()));

        option.addValue(ValueName.of(command.name()));

        optionRepository.save(option);
    }
}
