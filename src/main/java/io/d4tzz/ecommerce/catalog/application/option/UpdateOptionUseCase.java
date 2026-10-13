package io.d4tzz.ecommerce.catalog.application.option;

import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.catalog.application.option.command.UpdateOptionCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.option.Option;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.OptionName;
import io.d4tzz.ecommerce.catalog.domain.option.OptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Renames an option. */
@Service
@Transactional
public class UpdateOptionUseCase {

    private final OptionRepository optionRepository;

    public UpdateOptionUseCase(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    /**
     * Renames an option. Keeping the same name is allowed.
     *
     * @throws NotFoundException if the option does not exist
     * @throws ConflictException if another option already uses the name
     */
    public void execute(UpdateOptionCommand command) {
        Option option = optionRepository
            .find(OptionId.of(command.optionId()))
            .orElseThrow(() -> new NotFoundException("Option not found with id: " + command.optionId()));

        option.rename(OptionName.of(command.name()), optionRepository::existsByName);

        optionRepository.save(option);
    }
}
