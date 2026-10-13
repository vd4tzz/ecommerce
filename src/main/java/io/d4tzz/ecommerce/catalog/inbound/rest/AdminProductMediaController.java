package io.d4tzz.ecommerce.catalog.inbound.rest;

import io.d4tzz.ecommerce.catalog.application.product.AddProductMediaUseCase;
import io.d4tzz.ecommerce.catalog.application.product.RemoveProductMediaUseCase;
import io.d4tzz.ecommerce.catalog.application.product.ReorderProductMediaUseCase;
import io.d4tzz.ecommerce.catalog.application.product.command.AddProductMediaCommand;
import io.d4tzz.ecommerce.catalog.application.product.command.ReorderProductMediaCommand;
import io.d4tzz.ecommerce.spring.SpringValidation;
import io.d4tzz.ecommerce.storage.PresignedUploadRequest;
import io.d4tzz.ecommerce.storage.PresignedUploadResponse;
import io.d4tzz.ecommerce.storage.ProductMediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin management of a product's image gallery. Images are uploaded directly to storage using a presigned URL.
 */
@RestController
@Tag(name = "Admin - Product media", description = "Manage product images")
@RequestMapping("/admin/products")
public class AdminProductMediaController {

    private final ProductMediaService productMediaService;
    private final AddProductMediaUseCase addProductMediaUseCase;
    private final RemoveProductMediaUseCase removeProductMediaUseCase;
    private final ReorderProductMediaUseCase reorderProductMediaUseCase;
    private final Validator validator;

    public AdminProductMediaController(
        ProductMediaService productMediaService,
        AddProductMediaUseCase addProductMediaUseCase,
        RemoveProductMediaUseCase removeProductMediaUseCase,
        ReorderProductMediaUseCase reorderProductMediaUseCase,
        Validator validator
    ) {
        this.productMediaService = productMediaService;
        this.addProductMediaUseCase = addProductMediaUseCase;
        this.removeProductMediaUseCase = removeProductMediaUseCase;
        this.reorderProductMediaUseCase = reorderProductMediaUseCase;
        this.validator = validator;
    }

    @Operation(
        summary = "Get an upload URL",
        description = "Returns a short-lived URL to upload one image straight to storage, with the key to add it to a product afterwards and its public URL. Allowed types are jpeg, png, webp, gif, svg and avif, up to 10 MB."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The upload URL"),
        @ApiResponse(
            responseCode = "400",
            description = "Unsupported type or invalid size",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PostMapping("/media/presign")
    public ResponseEntity<PresignedUploadResponse> presign(@RequestBody PresignedUploadRequest request) {
        PresignedUploadResponse response = productMediaService.generatePresignedUploadUrl(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Add media to a product",
        description = "Adds uploaded images, by key, to the end of the product's gallery. Images already there are ignored."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Media added"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PostMapping("/{id}/media")
    public ResponseEntity<Void> addMedia(@PathVariable Long id, @RequestBody AddProductMediaRequest request) {
        var command = new AddProductMediaCommand(id, request.mediaIds());
        SpringValidation.validate(command, validator);
        addProductMediaUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Remove media from a product",
        description = "Removes an image from the gallery. Nothing happens if it is not in the gallery."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Media removed"),
        @ApiResponse(
            responseCode = "400",
            description = "A variant uses the image",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @DeleteMapping("/{id}/media")
    public ResponseEntity<Void> removeMedia(@PathVariable Long id, @RequestParam(name = "mediaId") String mediaId) {
        removeProductMediaUseCase.execute(id, mediaId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Reorder product media",
        description = "Sets the gallery order. The list must contain every image of the gallery exactly once."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Media reordered"),
        @ApiResponse(
            responseCode = "400",
            description = "The list has duplicates or unknown images, or leaves some out",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
    })
    @PutMapping("/{id}/media/reorder")
    public ResponseEntity<Void> reorderMedia(@PathVariable Long id, @RequestBody ReorderProductMediaRequest request) {
        var command = new ReorderProductMediaCommand(id, request.mediaIds());
        SpringValidation.validate(command, validator);
        reorderProductMediaUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }
}

record AddProductMediaRequest(List<String> mediaIds) {}

record ReorderProductMediaRequest(List<String> mediaIds) {}
