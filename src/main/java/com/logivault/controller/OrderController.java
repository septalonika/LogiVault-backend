package com.logivault.controller;

import org.springframework.http.HttpStatus;
import java.util.List;
import com.logivault.dto.WebResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.logivault.dto.order.CancelOrderRequest;
import com.logivault.dto.order.CreateOrderRequest;
import com.logivault.dto.order.OrderResponse;
import com.logivault.dto.order.OrderSummary;
import com.logivault.entity.OrderStatus;
import com.logivault.service.OrderService;
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

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Orders", description = "Sales orders with all-or-nothing stock deduction")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Create an order and deduct stock")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "404", description = "VARIANT_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "INSUFFICIENT_STOCK, nothing is deducted")
    @ApiResponse(responseCode = "422", description = "VARIANT_INACTIVE")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<WebResponse<OrderResponse>> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + response.id()))
                .body(WebResponse.of(HttpStatus.CREATED, "Order created", response));
    }

    @Operation(summary = "Cancel an order and restore stock")
    @ApiResponse(responseCode = "200", description = "Cancelled")
    @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "INVALID_ORDER_STATUS")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<WebResponse<OrderResponse>> cancel(@PathVariable UUID id, @Valid @RequestBody CancelOrderRequest request) {
        return WebResponse.ok("Order cancelled", orderService.cancel(id, request));
    }

    @Operation(summary = "List orders")
    @ApiResponse(responseCode = "200", description = "Page of orders")
    @GetMapping
    public WebResponse<List<OrderSummary>> list(@RequestParam(required = false) OrderStatus status,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                            @RequestParam(required = false) UUID createdBy,
                                            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return WebResponse.page("Orders retrieved", orderService.list(status, from, to, createdBy, pageable));
    }

    @Operation(summary = "Get an order with its lines")
    @ApiResponse(responseCode = "200", description = "Order")
    @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND")
    @GetMapping("/{id}")
    public ResponseEntity<WebResponse<OrderResponse>> getById(@PathVariable UUID id) {
        return WebResponse.ok("Order retrieved", orderService.getById(id));
    }
}
