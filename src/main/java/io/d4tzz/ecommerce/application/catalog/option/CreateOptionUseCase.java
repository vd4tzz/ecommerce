package io.d4tzz.ecommerce.application.catalog.option;

import io.d4tzz.ecommerce.application.catalog.option.command.CreateOptionCommand;
import io.d4tzz.ecommerce.domain.catalog.option.Option;
import io.d4tzz.ecommerce.domain.catalog.option.OptionName;
import io.d4tzz.ecommerce.domain.catalog.option.OptionRepository;
import io.d4tzz.ecommerce.domain.catalog.option.ValueName;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateOptionUseCase {

    private final OptionRepository optionRepository;

    public CreateOptionUseCase(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

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
