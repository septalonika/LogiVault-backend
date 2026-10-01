package com.logivault.stock;

import com.logivault.common.web.PageResponse;
import com.logivault.stock.dto.AdjustStockRequest;
import com.logivault.stock.dto.LowStockResponse;
import com.logivault.stock.dto.MovementResponse;
import com.logivault.stock.dto.StockChangeResponse;
import com.logivault.stock.dto.StockInRequest;
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

@RestController
@RequestMapping("/api/v1")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/variants/{id}/stock-in")
    public ResponseEntity<StockChangeResponse> stockIn(@PathVariable UUID id, @Valid @RequestBody StockInRequest request) {
        return ResponseEntity.ok(stockService.stockIn(id, request));
    }

    @PostMapping("/variants/{id}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockChangeResponse> adjust(@PathVariable UUID id, @Valid @RequestBody AdjustStockRequest request) {
        return ResponseEntity.ok(stockService.adjust(id, request));
    }

    @GetMapping("/variants/{id}/movements")
    public PageResponse<MovementResponse> history(@PathVariable UUID id,
                                                   @RequestParam(required = false) MovementType type,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                   @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return stockService.history(id, type, from, to, pageable);
    }

    @GetMapping("/stock/low")
    public PageResponse<LowStockResponse> lowStock(@PageableDefault Pageable pageable) {
        return stockService.lowStock(pageable);
    }
}
