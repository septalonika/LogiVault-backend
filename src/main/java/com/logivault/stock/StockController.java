package com.logivault.stock;

import com.logivault.stock.dto.AdjustStockRequest;
import com.logivault.stock.dto.StockChangeResponse;
import com.logivault.stock.dto.StockInRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/variants")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/{id}/stock-in")
    public ResponseEntity<StockChangeResponse> stockIn(@PathVariable UUID id, @Valid @RequestBody StockInRequest request) {
        return ResponseEntity.ok(stockService.stockIn(id, request));
    }

    @PostMapping("/{id}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockChangeResponse> adjust(@PathVariable UUID id, @Valid @RequestBody AdjustStockRequest request) {
        return ResponseEntity.ok(stockService.adjust(id, request));
    }
}
