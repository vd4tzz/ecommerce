package io.d4tzz.ecommerce.rest;

import io.d4tzz.ecommerce.application.catalog.product.AddProductMediaCommand;
import io.d4tzz.ecommerce.application.catalog.product.AddProductMediaUseCase;
import io.d4tzz.ecommerce.application.catalog.product.RemoveProductMediaUseCase;
import io.d4tzz.ecommerce.application.catalog.product.ReorderProductMediaCommand;
import io.d4tzz.ecommerce.application.catalog.product.ReorderProductMediaUseCase;
import io.d4tzz.ecommerce.storage.PresignedUploadRequest;
import io.d4tzz.ecommerce.storage.PresignedUploadResponse;
import io.d4tzz.ecommerce.storage.ProductMediaService;
import java.util.List;
import org.springframework.http.ResponseEntity;
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

    public AdminProductMediaController(
        ProductMediaService productMediaService,
        AddProductMediaUseCase addProductMediaUseCase,
        RemoveProductMediaUseCase removeProductMediaUseCase,
        ReorderProductMediaUseCase reorderProductMediaUseCase
    ) {
        this.productMediaService = productMediaService;
        this.addProductMediaUseCase = addProductMediaUseCase;
        this.removeProductMediaUseCase = removeProductMediaUseCase;
        this.reorderProductMediaUseCase = reorderProductMediaUseCase;
    }

    @PostMapping("/media/presign")
    public ResponseEntity<PresignedUploadResponse> presign(@RequestBody PresignedUploadRequest request) {
        PresignedUploadResponse response = productMediaService.generatePresignedUploadUrl(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/media")
    public ResponseEntity<?> addMedia(@PathVariable Long id, @RequestBody AddProductMediaRequest request) {
        var command = new AddProductMediaCommand(id, request.mediaIds());
        return ResponseEntity.ok(addProductMediaUseCase.execute(command));
    }

    @DeleteMapping("/{id}/media")
    public ResponseEntity<?> removeMedia(@PathVariable Long id, @RequestParam(name = "mediaId") String mediaId) {
        removeProductMediaUseCase.execute(id, mediaId);
        return ResponseEntity.noContent().build();
    }

    // @DeleteMapping("/{id}/media/{mediaId}")
    // public ResponseEntity<?> removeMediaByPath(@PathVariable Long id, @PathVariable String mediaId) {
    //     removeProductMediaUseCase.execute(id, mediaId);
    //     return ResponseEntity.noContent().build();
    // }

    @PutMapping("/{id}/media/reorder")
    public ResponseEntity<?> reorderMedia(@PathVariable Long id, @RequestBody ReorderProductMediaRequest request) {
        var command = new ReorderProductMediaCommand(id, request.mediaIds());
        return ResponseEntity.ok(reorderProductMediaUseCase.execute(command));
    }
}

record AddProductMediaRequest(List<String> mediaIds) {}

record ReorderProductMediaRequest(List<String> mediaIds) {}
