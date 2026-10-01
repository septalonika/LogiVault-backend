package com.logivault.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.logivault.dto.PageResponse;
import com.logivault.dto.stock.AdjustStockRequest;
import com.logivault.dto.stock.LowStockResponse;
import com.logivault.dto.stock.MovementResponse;
import com.logivault.dto.stock.StockChangeResponse;
import com.logivault.dto.stock.StockInRequest;
import com.logivault.entity.MovementType;
import com.logivault.service.StockService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Stock", description = "Stock-in, adjustments, movement history and low-stock list")
@RestController
@RequestMapping("/api/v1")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @Operation(summary = "Receive stock for a variant")
    @ApiResponse(responseCode = "200", description = "New stock level and movement")
    @ApiResponse(responseCode = "404", description = "VARIANT_NOT_FOUND")
    @ApiResponse(responseCode = "422", description = "VARIANT_INACTIVE")
    @PostMapping("/variants/{id}/stock-in")
    public ResponseEntity<StockChangeResponse> stockIn(@PathVariable UUID id, @Valid @RequestBody StockInRequest request) {
        return ResponseEntity.ok(stockService.stockIn(id, request));
    }

    @Operation(summary = "Manually adjust stock (ADMIN)")
    @ApiResponse(responseCode = "200", description = "New stock level and movement")
    @ApiResponse(responseCode = "404", description = "VARIANT_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "INSUFFICIENT_STOCK when the result would be negative")
    @PostMapping("/variants/{id}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockChangeResponse> adjust(@PathVariable UUID id, @Valid @RequestBody AdjustStockRequest request) {
        return ResponseEntity.ok(stockService.adjust(id, request));
    }

    @Operation(summary = "Stock movement history of a variant")
    @ApiResponse(responseCode = "200", description = "Page of movements")
    @ApiResponse(responseCode = "404", description = "VARIANT_NOT_FOUND")
    @GetMapping("/variants/{id}/movements")
    public PageResponse<MovementResponse> history(@PathVariable UUID id,
                                                   @RequestParam(required = false) MovementType type,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                   @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return stockService.history(id, type, from, to, pageable);
    }

    @Operation(summary = "Variants at or below their minimum stock")
    @ApiResponse(responseCode = "200", description = "Page of low-stock variants")
    @GetMapping("/stock/low")
    public PageResponse<LowStockResponse> lowStock(@PageableDefault Pageable pageable) {
        return stockService.lowStock(pageable);
    }
}
