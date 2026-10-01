package com.logivault.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.logivault.dto.PageResponse;
import com.logivault.dto.item.CreateItemRequest;
import com.logivault.dto.item.ItemResponse;
import com.logivault.dto.item.ItemSummary;
import com.logivault.dto.item.UpdateItemRequest;
import com.logivault.dto.variant.CreateVariantRequest;
import com.logivault.dto.variant.VariantResponse;
import com.logivault.service.ItemService;
import com.logivault.service.VariantService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Items", description = "Catalog items and their variants")
@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    private final ItemService itemService;
    private final VariantService variantService;

    public ItemController(ItemService itemService, VariantService variantService) {
        this.itemService = itemService;
        this.variantService = variantService;
    }

    @Operation(summary = "Create an item (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Created; a Default variant is added when none is given")
    @ApiResponse(responseCode = "409", description = "SKU_ALREADY_EXISTS")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody CreateItemRequest request) {
        ItemResponse response = itemService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/items/" + response.id())).body(response);
    }

    @Operation(summary = "List items")
    @ApiResponse(responseCode = "200", description = "Page of items")
    @GetMapping
    public PageResponse<ItemSummary> list(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) Boolean active,
                                           @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return itemService.list(q, active, pageable);
    }

    @Operation(summary = "Get an item with its variants")
    @ApiResponse(responseCode = "200", description = "Item")
    @ApiResponse(responseCode = "404", description = "ITEM_NOT_FOUND")
    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(itemService.getById(id));
    }

    @Operation(summary = "Update an item (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Updated")
    @ApiResponse(responseCode = "404", description = "ITEM_NOT_FOUND")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateItemRequest request) {
        return ResponseEntity.ok(itemService.update(id, request));
    }

    @Operation(summary = "Deactivate an item (ADMIN)")
    @ApiResponse(responseCode = "204", description = "Deactivated")
    @ApiResponse(responseCode = "404", description = "ITEM_NOT_FOUND")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        itemService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reactivate an item (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Reactivated")
    @ApiResponse(responseCode = "404", description = "ITEM_NOT_FOUND")
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(itemService.activate(id));
    }

    @Operation(summary = "List the variants of an item")
    @ApiResponse(responseCode = "200", description = "Variants")
    @ApiResponse(responseCode = "404", description = "ITEM_NOT_FOUND")
    @GetMapping("/{id}/variants")
    public ResponseEntity<List<VariantResponse>> listVariants(@PathVariable UUID id) {
        return ResponseEntity.ok(variantService.listByItem(id));
    }

    @Operation(summary = "Add a variant to an item (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "404", description = "ITEM_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "SKU_ALREADY_EXISTS")
    @PostMapping("/{id}/variants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VariantResponse> addVariant(@PathVariable UUID id, @Valid @RequestBody CreateVariantRequest request) {
        VariantResponse response = variantService.addVariant(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
