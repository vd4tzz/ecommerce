package io.d4tzz.ecommerce.catalog.application.option;

import io.d4tzz.ecommerce.catalog.application.option.command.UpdateOptionValueCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.option.Option;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.OptionRepository;
import io.d4tzz.ecommerce.catalog.domain.option.ValueId;
import io.d4tzz.ecommerce.catalog.domain.option.ValueName;
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
