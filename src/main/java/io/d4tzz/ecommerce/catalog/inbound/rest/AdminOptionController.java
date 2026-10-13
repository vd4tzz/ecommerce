package io.d4tzz.ecommerce.catalog.inbound.rest;

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
import io.d4tzz.ecommerce.catalog.query.option.OptionDetailView;
import io.d4tzz.ecommerce.catalog.query.option.OptionQueryService;
import io.d4tzz.ecommerce.catalog.query.option.OptionSearchCriteria;
import io.d4tzz.ecommerce.catalog.query.option.OptionTableRowView;
import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.spring.SpringValidation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import org.springframework.http.ProblemDetail;
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

/**
 * Admin management of the options (such as Color) and their values (such as Red) that products can use.
 */
@RestController
@Tag(name = "Admin - Options", description = "Manage product options such as Color or Size")
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

    @Operation(summary = "Create an option", description = "Creates an option with its initial values.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Option created, with its id"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "The name is already used, or two values have the same name",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PostMapping
    public ResponseEntity<CreateOptionResponse> createOption(@RequestBody CreateOptionRequest request) {
        var command = new CreateOptionCommand(request.name(), request.values());
        SpringValidation.validate(command, validator);
        Long optionId = createOptionUseCase.execute(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(optionId).toUri();
        return ResponseEntity.created(location).body(new CreateOptionResponse(optionId));
    }

    @Operation(summary = "List options", description = "Paged list of options, optionally filtered by a keyword.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "A page of options") })
    @GetMapping
    public ResponseEntity<PageResponse<OptionTableRowView>> getOptions(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var criteria = new OptionSearchCriteria(keyword, page, size);
        return ResponseEntity.ok(optionQueryService.getOptions(criteria));
    }

    @Operation(summary = "Get an option", description = "Returns one option with its values.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The option"),
        @ApiResponse(
            responseCode = "404",
            description = "Option not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @GetMapping("/{id}")
    public ResponseEntity<OptionDetailView> getOptionDetail(@PathVariable Long id) {
        return optionQueryService
            .getOptionDetail(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new NotFoundException("Option not found with id: " + id));
    }

    @Operation(summary = "Rename an option", description = "Changes the option name. Keeping the same name is allowed.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Option renamed"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Option not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Another option already uses the name",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateOption(@PathVariable Long id, @RequestBody UpdateOptionRequest request) {
        var command = new UpdateOptionCommand(id, request.name());
        SpringValidation.validate(command, validator);
        updateOptionUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add a value", description = "Adds a value to the end of the option's values.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Value added"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Option not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "The option already has a value with that name",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PostMapping("/{id}/values")
    public ResponseEntity<Void> addOptionValue(@PathVariable Long id, @RequestBody AddOptionValueRequest request) {
        var command = new AddOptionValueCommand(id, request.name());
        SpringValidation.validate(command, validator);
        addOptionValueUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Rename a value", description = "Changes the name of a value of the option.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Value renamed"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input, or the option has no such value",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Option not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Another value of the option already has the name",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PutMapping("/{optionId}/values/{valueId}")
    public ResponseEntity<Void> updateOptionValue(
        @PathVariable Long optionId,
        @PathVariable Long valueId,
        @RequestBody UpdateOptionValueRequest request
    ) {
        var command = new UpdateOptionValueCommand(optionId, valueId, request.name());
        SpringValidation.validate(command, validator);
        updateOptionValueUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Delete a value",
        description = "Removes a value from the option. Nothing happens if the option has no such value."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Value deleted"),
        @ApiResponse(
            responseCode = "400",
            description = "A product uses the value",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Option not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @DeleteMapping("/{optionId}/values/{valueId}")
    public ResponseEntity<Void> deleteOptionValue(@PathVariable Long optionId, @PathVariable Long valueId) {
        deleteOptionValueUseCase.execute(new DeleteOptionValueCommand(optionId, valueId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete an option", description = "Deletes an option together with its values.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Option deleted"),
        @ApiResponse(
            responseCode = "400",
            description = "A product uses the option",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Option not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
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
