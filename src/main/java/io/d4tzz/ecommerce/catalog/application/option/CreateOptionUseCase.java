package io.d4tzz.ecommerce.catalog.application.option;

import io.d4tzz.ecommerce.common.exception.ConflictException;
import io.d4tzz.ecommerce.catalog.application.option.command.CreateOptionCommand;
import io.d4tzz.ecommerce.catalog.domain.option.Option;
import io.d4tzz.ecommerce.catalog.domain.option.OptionName;
import io.d4tzz.ecommerce.catalog.domain.option.OptionRepository;
import io.d4tzz.ecommerce.catalog.domain.option.ValueName;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates an option such as "Color" with its initial values. */
@Service
@Transactional
public class CreateOptionUseCase {

    private final OptionRepository optionRepository;

    public CreateOptionUseCase(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    /**
     * Creates an option with its values and returns its id.
     *
     * @throws ConflictException if the option name is already used, or two values have the same name
     */
    public Long execute(CreateOptionCommand command) {
        List<ValueName> valueNames = command
            .values()
            .stream()
            .map(ValueName::of)
            .toList();

        Option option = Option.create(
            OptionName.of(command.name()),
            valueNames,
            optionRepository::existsByName
        );

        Option saved = optionRepository.save(option);
        return saved.getId().value();
    }
}
