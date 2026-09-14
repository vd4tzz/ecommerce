package io.d4tzz.ecommerce.rest;

import io.d4tzz.ecommerce.storage.PresignedUploadRequest;
import io.d4tzz.ecommerce.storage.PresignedUploadResponse;
import io.d4tzz.ecommerce.storage.ProductMediaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/products/media")
public class AdminProductMediaController {

    private final ProductMediaService productMediaService;

    public AdminProductMediaController(ProductMediaService productMediaService) {
        this.productMediaService = productMediaService;
    }

    @PostMapping("/presign")
    public ResponseEntity<PresignedUploadResponse> presign(@RequestBody PresignedUploadRequest request) {
        PresignedUploadResponse response = productMediaService.generatePresignedUploadUrl(request);
        return ResponseEntity.ok(response);
    }
}
