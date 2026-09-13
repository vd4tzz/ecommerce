package io.d4tzz.ecommerce.application.catalog.option;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.domain.catalog.option.Option;
import io.d4tzz.ecommerce.domain.catalog.option.OptionId;
import io.d4tzz.ecommerce.domain.catalog.option.OptionName;
import io.d4tzz.ecommerce.domain.catalog.option.OptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateOptionUseCase {

    private final OptionRepository optionRepository;

    public UpdateOptionUseCase(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    public OptionDto execute(UpdateOptionCommand command) {
        Option option = optionRepository
            .find(OptionId.of(command.optionId()))
            .orElseThrow(() -> new NotFoundException("Option not found with id: " + command.optionId()));

        option.rename(OptionName.of(command.name()), optionRepository::existsByName);

        Option saved = optionRepository.save(option);
        return OptionDto.from(saved);
    }
}
