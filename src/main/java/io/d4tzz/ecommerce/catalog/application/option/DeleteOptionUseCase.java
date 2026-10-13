package io.d4tzz.ecommerce.catalog.application.option;

import io.d4tzz.ecommerce.common.exception.InvalidOperationException;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.catalog.domain.option.OptionId;
import io.d4tzz.ecommerce.catalog.domain.option.OptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Deletes an option that no product uses. */
@Service
@Transactional
public class DeleteOptionUseCase {

    private final OptionRepository optionRepository;

    public DeleteOptionUseCase(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    /**
     * Deletes an option together with its values.
     *
     * @throws NotFoundException if the option does not exist
     * @throws InvalidOperationException if a product uses the option
     */
    public void execute(Long optionId) {
        OptionId id = OptionId.of(optionId);
        if (!optionRepository.existsById(id)) {
            throw new NotFoundException("Option not found with id: " + optionId);
        }

        if (optionRepository.isOptionReferenced(id)) {
            throw new InvalidOperationException("Cannot delete an option in use");
        }
        optionRepository.delete(id);
    }
}
