package com.ecommerce.controller;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.OrderDTO;
import com.ecommerce.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sales/orders")
@PreAuthorize("hasRole('SALES')")
public class SalesOrderController {

    private final OrderService orderService;

    public SalesOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/paid")
    public Result<PageResult<OrderDTO>> getPaidOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(orderService.getPaidOrders(page, size));
    }

    @GetMapping("/shipped")
    public Result<PageResult<OrderDTO>> getShippedOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(orderService.getShippedOrders(page, size));
    }

    @PostMapping("/{orderId}/ship")
    public Result<Void> shipOrder(@PathVariable Long orderId) {
        orderService.shipOrder(orderId);
        return Result.success();
    }
}