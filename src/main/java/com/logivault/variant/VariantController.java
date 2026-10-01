package com.logivault.variant;

import com.logivault.variant.dto.UpdateVariantRequest;
import com.logivault.variant.dto.VariantDetailResponse;
import com.logivault.variant.dto.VariantResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/variants")
public class VariantController {

    private final VariantService variantService;

    public VariantController(VariantService variantService) {
        this.variantService = variantService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<VariantDetailResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(variantService.getById(id));
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<VariantDetailResponse> getBySku(@PathVariable String sku) {
        return ResponseEntity.ok(variantService.getBySku(sku));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VariantResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateVariantRequest request) {
        return ResponseEntity.ok(variantService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        variantService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VariantResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(variantService.activate(id));
    }
}
