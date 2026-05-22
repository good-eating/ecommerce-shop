package com.ecommerce.controller;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.OrderCreateRequest;
import com.ecommerce.dto.OrderDTO;
import com.ecommerce.security.UserDetailsImpl;
import com.ecommerce.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@PreAuthorize("isAuthenticated()")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Result<OrderDTO> createOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody OrderCreateRequest request) {
        return Result.success(orderService.createOrder(userDetails.getId(), request));
    }

    @GetMapping
    public Result<PageResult<OrderDTO>> getUserOrders(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(orderService.getUserOrders(userDetails.getId(), page, size));
    }

    @GetMapping("/{orderId}")
    public Result<OrderDTO> getOrderById(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long orderId) {
        return Result.success(orderService.getOrderById(userDetails.getId(), orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancelOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long orderId) {
        orderService.cancelOrder(userDetails.getId(), orderId);
        return Result.success();
    }

    @PostMapping("/{orderId}/pay")
    public Result<Void> payOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long orderId) {
        orderService.payOrder(userDetails.getId(), orderId);
        return Result.success();
    }

    @PostMapping("/{orderId}/confirm-receipt")
    public Result<Void> confirmReceipt(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long orderId) {
        orderService.confirmReceipt(userDetails.getId(), orderId);
        return Result.success();
    }
}