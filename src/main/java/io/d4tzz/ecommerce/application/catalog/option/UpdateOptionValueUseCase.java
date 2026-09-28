package io.d4tzz.ecommerce.application.catalog.option;

import io.d4tzz.ecommerce.application.catalog.option.command.UpdateOptionValueCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.catalog.option.Option;
import io.d4tzz.ecommerce.domain.catalog.option.OptionId;
import io.d4tzz.ecommerce.domain.catalog.option.OptionRepository;
import io.d4tzz.ecommerce.domain.catalog.option.ValueId;
import io.d4tzz.ecommerce.domain.catalog.option.ValueName;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateOptionValueUseCase {

    private final OptionRepository optionRepository;

    public UpdateOptionValueUseCase(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    public void execute(UpdateOptionValueCommand command) {
        Option option = optionRepository
            .find(OptionId.of(command.optionId()))
            .orElseThrow(() -> new NotFoundException("Option not found with id: " + command.optionId()));

        option.renameValue(ValueId.of(command.valueId()), ValueName.of(command.name()));

        optionRepository.save(option);
    }
}
