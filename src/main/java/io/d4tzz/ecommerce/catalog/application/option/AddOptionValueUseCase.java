package io.d4tzz.ecommerce.catalog.application.option;

import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.catalog.application.option.command.AddOptionValueCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.option.Option;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.OptionRepository;
import io.d4tzz.ecommerce.catalog.domain.option.ValueName;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Adds a value to an option. */
@Service
@Transactional
public class AddOptionValueUseCase {

    private final OptionRepository optionRepository;

    public AddOptionValueUseCase(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    /**
     * Adds a value to the end of an option's values.
     *
     * @throws NotFoundException if the option does not exist
     * @throws ConflictException if the option already has a value with that name
     */
    public void execute(AddOptionValueCommand command) {
        Option option = optionRepository
            .find(OptionId.of(command.optionId()))
            .orElseThrow(() -> new NotFoundException("Option not found with id: " + command.optionId()));

        option.addValue(ValueName.of(command.name()));

        optionRepository.save(option);
    }
}
