package io.d4tzz.ecommerce.catalog.inbound.rest;

import io.d4tzz.ecommerce.spring.SpringValidation;
import io.d4tzz.ecommerce.spring.ValidationException;

import io.d4tzz.ecommerce.catalog.application.product.AddProductMediaUseCase;
import io.d4tzz.ecommerce.catalog.application.product.RemoveProductMediaUseCase;
import io.d4tzz.ecommerce.catalog.application.product.ReorderProductMediaUseCase;
import io.d4tzz.ecommerce.catalog.application.product.command.AddProductMediaCommand;
import io.d4tzz.ecommerce.catalog.application.product.command.ReorderProductMediaCommand;
import io.d4tzz.ecommerce.storage.PresignedUploadRequest;
import io.d4tzz.ecommerce.storage.PresignedUploadResponse;
import io.d4tzz.ecommerce.storage.ProductMediaService;
import java.util.List;
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

@RestController
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

    @PostMapping("/media/presign")
    public ResponseEntity<PresignedUploadResponse> presign(@RequestBody PresignedUploadRequest request) {
        PresignedUploadResponse response = productMediaService.generatePresignedUploadUrl(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/media")
    public ResponseEntity<Void> addMedia(@PathVariable Long id, @RequestBody AddProductMediaRequest request) {
        var command = new AddProductMediaCommand(id, request.mediaIds());
        SpringValidation.validate(command, validator);
        addProductMediaUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/media")
    public ResponseEntity<Void> removeMedia(@PathVariable Long id, @RequestParam(name = "mediaId") String mediaId) {
        removeProductMediaUseCase.execute(id, mediaId);
        return ResponseEntity.noContent().build();
    }

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
