package com.logivault.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.logivault.dto.variant.UpdateVariantRequest;
import com.logivault.dto.variant.VariantDetailResponse;
import com.logivault.dto.variant.VariantResponse;
import com.logivault.service.VariantService;
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

@Tag(name = "Variants", description = "Variant lookup and maintenance")
@RestController
@RequestMapping("/api/v1/variants")
public class VariantController {

    private final VariantService variantService;

    public VariantController(VariantService variantService) {
        this.variantService = variantService;
    }

    @Operation(summary = "Get a variant by id")
    @ApiResponse(responseCode = "200", description = "Variant")
    @ApiResponse(responseCode = "404", description = "VARIANT_NOT_FOUND")
    @GetMapping("/{id}")
    public ResponseEntity<VariantDetailResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(variantService.getById(id));
    }

    @Operation(summary = "Get a variant by SKU (case-insensitive)")
    @ApiResponse(responseCode = "200", description = "Variant")
    @ApiResponse(responseCode = "404", description = "VARIANT_NOT_FOUND")
    @GetMapping("/sku/{sku}")
    public ResponseEntity<VariantDetailResponse> getBySku(@PathVariable String sku) {
        return ResponseEntity.ok(variantService.getBySku(sku));
    }

    @Operation(summary = "Update a variant (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Updated")
    @ApiResponse(responseCode = "404", description = "VARIANT_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "SKU_ALREADY_EXISTS or SKU_IMMUTABLE")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VariantResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateVariantRequest request) {
        return ResponseEntity.ok(variantService.update(id, request));
    }

    @Operation(summary = "Deactivate a variant (ADMIN)")
    @ApiResponse(responseCode = "204", description = "Deactivated")
    @ApiResponse(responseCode = "404", description = "VARIANT_NOT_FOUND")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        variantService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reactivate a variant (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Reactivated")
    @ApiResponse(responseCode = "404", description = "VARIANT_NOT_FOUND")
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VariantResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(variantService.activate(id));
    }
}
