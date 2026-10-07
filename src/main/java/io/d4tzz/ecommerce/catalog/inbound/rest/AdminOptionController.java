package io.d4tzz.ecommerce.catalog.inbound.rest;

import io.d4tzz.ecommerce.rest.ValidationHelper;
import io.d4tzz.ecommerce.rest.ValidationException;

import io.d4tzz.ecommerce.catalog.application.option.AddOptionValueUseCase;
import io.d4tzz.ecommerce.catalog.application.option.CreateOptionUseCase;
import io.d4tzz.ecommerce.catalog.application.option.DeleteOptionUseCase;
import io.d4tzz.ecommerce.catalog.application.option.DeleteOptionValueUseCase;
import io.d4tzz.ecommerce.catalog.application.option.UpdateOptionUseCase;
import io.d4tzz.ecommerce.catalog.application.option.UpdateOptionValueUseCase;
import io.d4tzz.ecommerce.catalog.application.option.command.AddOptionValueCommand;
import io.d4tzz.ecommerce.catalog.application.option.command.CreateOptionCommand;
import io.d4tzz.ecommerce.catalog.application.option.command.DeleteOptionValueCommand;
import io.d4tzz.ecommerce.catalog.application.option.command.UpdateOptionCommand;
import io.d4tzz.ecommerce.catalog.application.option.command.UpdateOptionValueCommand;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.catalog.query.option.OptionDetailView;
import io.d4tzz.ecommerce.catalog.query.option.OptionQueryService;
import io.d4tzz.ecommerce.catalog.query.option.OptionSearchCriteria;
import io.d4tzz.ecommerce.catalog.query.option.OptionTableRowView;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/admin/options")
public class AdminOptionController {

    private final OptionQueryService optionQueryService;
    private final CreateOptionUseCase createOptionUseCase;
    private final UpdateOptionUseCase updateOptionUseCase;
    private final AddOptionValueUseCase addOptionValueUseCase;
    private final UpdateOptionValueUseCase updateOptionValueUseCase;
    private final DeleteOptionValueUseCase deleteOptionValueUseCase;
    private final DeleteOptionUseCase deleteOptionUseCase;
    private final Validator validator;

    public AdminOptionController(
        OptionQueryService optionQueryService,
        CreateOptionUseCase createOptionUseCase,
        UpdateOptionUseCase updateOptionUseCase,
        AddOptionValueUseCase addOptionValueUseCase,
        UpdateOptionValueUseCase updateOptionValueUseCase,
        DeleteOptionValueUseCase deleteOptionValueUseCase,
        DeleteOptionUseCase deleteOptionUseCase,
        Validator validator
    ) {
        this.optionQueryService = optionQueryService;
        this.createOptionUseCase = createOptionUseCase;
        this.updateOptionUseCase = updateOptionUseCase;
        this.addOptionValueUseCase = addOptionValueUseCase;
        this.updateOptionValueUseCase = updateOptionValueUseCase;
        this.deleteOptionValueUseCase = deleteOptionValueUseCase;
        this.deleteOptionUseCase = deleteOptionUseCase;
        this.validator = validator;
    }

    @PostMapping
    public ResponseEntity<CreateOptionResponse> createOption(@RequestBody CreateOptionRequest request) {
        var command = new CreateOptionCommand(request.name(), request.values());
        ValidationHelper.validate(command, validator);
        Long optionId = createOptionUseCase.execute(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(optionId)
            .toUri();
        return ResponseEntity.created(location).body(new CreateOptionResponse(optionId));
    }

    @GetMapping
    public ResponseEntity<PageResponse<OptionTableRowView>> getOptions(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var criteria = new OptionSearchCriteria(keyword, page, size);
        return ResponseEntity.ok(optionQueryService.getOptions(criteria));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OptionDetailView> getOptionDetail(@PathVariable Long id) {
        return optionQueryService
            .getOptionDetail(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new NotFoundException("Option not found with id: " + id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateOption(@PathVariable Long id, @RequestBody UpdateOptionRequest request) {
        var command = new UpdateOptionCommand(id, request.name());
        ValidationHelper.validate(command, validator);
        updateOptionUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/values")
    public ResponseEntity<Void> addOptionValue(@PathVariable Long id, @RequestBody AddOptionValueRequest request) {
        var command = new AddOptionValueCommand(id, request.name());
        ValidationHelper.validate(command, validator);
        addOptionValueUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{optionId}/values/{valueId}")
    public ResponseEntity<Void> updateOptionValue(
        @PathVariable Long optionId,
        @PathVariable Long valueId,
        @RequestBody UpdateOptionValueRequest request
    ) {
        var command = new UpdateOptionValueCommand(optionId, valueId, request.name());
        ValidationHelper.validate(command, validator);
        updateOptionValueUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{optionId}/values/{valueId}")
    public ResponseEntity<Void> deleteOptionValue(@PathVariable Long optionId, @PathVariable Long valueId) {
        deleteOptionValueUseCase.execute(new DeleteOptionValueCommand(optionId, valueId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOption(@PathVariable Long id) {
        deleteOptionUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}

record CreateOptionRequest(String name, List<String> values) {}

record CreateOptionResponse(Long id) {}

record UpdateOptionRequest(String name) {}

record AddOptionValueRequest(String name) {}

record UpdateOptionValueRequest(String name) {}
