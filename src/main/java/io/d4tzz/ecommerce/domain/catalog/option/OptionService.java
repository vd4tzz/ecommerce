package io.d4tzz.ecommerce.domain.catalog.option;

import io.d4tzz.ecommerce.common.exception.InvalidOperationException;

public class OptionService {

    private final OptionRepository optionRepository;

    public OptionService(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    public void deleteValue(Option option, ValueId id) {
        option.removeValue(id, optionRepository::isValueReferenced);
    }

    public void deleteOption(Option option) {
        deleteOption(option.getId());
    }

    public void deleteOption(OptionId id) {
        if (optionRepository.isOptionReferenced(id)) {
            throw new InvalidOperationException("Cannot delete an option in use");
        }
        optionRepository.delete(id);
    }
}
