package com.logivault.item;

import com.logivault.common.web.PageResponse;
import com.logivault.item.dto.CreateItemRequest;
import com.logivault.item.dto.ItemResponse;
import com.logivault.item.dto.ItemSummary;
import com.logivault.item.dto.UpdateItemRequest;
import com.logivault.variant.VariantService;
import com.logivault.variant.dto.CreateVariantRequest;
import com.logivault.variant.dto.VariantResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    private final ItemService itemService;
    private final VariantService variantService;

    public ItemController(ItemService itemService, VariantService variantService) {
        this.itemService = itemService;
        this.variantService = variantService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody CreateItemRequest request) {
        ItemResponse response = itemService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/items/" + response.id())).body(response);
    }

    @GetMapping
    public PageResponse<ItemSummary> list(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) Boolean active,
                                           @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return itemService.list(q, active, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(itemService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateItemRequest request) {
        return ResponseEntity.ok(itemService.update(id, request));
    }

    @GetMapping("/{id}/variants")
    public ResponseEntity<List<VariantResponse>> listVariants(@PathVariable UUID id) {
        return ResponseEntity.ok(variantService.listByItem(id));
    }

    @PostMapping("/{id}/variants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VariantResponse> addVariant(@PathVariable UUID id, @Valid @RequestBody CreateVariantRequest request) {
        VariantResponse response = variantService.addVariant(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
