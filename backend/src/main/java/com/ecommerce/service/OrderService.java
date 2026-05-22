package com.ecommerce.service;

import com.ecommerce.dto.OrderCreateRequest;
import com.ecommerce.dto.OrderDTO;
import com.ecommerce.common.PageResult;

import java.util.List;

public interface OrderService {
    OrderDTO createOrder(Long userId, OrderCreateRequest request);
    OrderDTO getOrderById(Long userId, Long orderId);
    PageResult<OrderDTO> getUserOrders(Long userId, Integer page, Integer size);
    void cancelOrder(Long userId, Long orderId);
    void payOrder(Long userId, Long orderId);
    void shipOrder(Long orderId);
    void confirmReceipt(Long userId, Long orderId);
    PageResult<OrderDTO> getPaidOrders(Integer page, Integer size);
    PageResult<OrderDTO> getShippedOrders(Integer page, Integer size);
}