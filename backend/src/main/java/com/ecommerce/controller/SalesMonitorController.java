package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.Product;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.mapper.ProductMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sales-monitor")
@PreAuthorize("hasRole('SALES')")
public class SalesMonitorController {

    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;

    public SalesMonitorController(ProductMapper productMapper, OrderMapper orderMapper) {
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        List<Product> allProducts = productMapper.selectList(null);
        long totalProducts = allProducts.size();
        long totalSales = allProducts.stream().mapToInt(Product::getSalesCount).sum();
        long lowStockCount = allProducts.stream().filter(p -> p.getStock() != null && p.getStock() < 50).count();
        long outOfStockCount = allProducts.stream().filter(p -> p.getStock() == null || p.getStock() == 0).count();

        stats.put("totalProducts", totalProducts);
        stats.put("totalSales", totalSales);
        stats.put("lowStockCount", lowStockCount);
        stats.put("outOfStockCount", outOfStockCount);

        return Result.success(stats);
    }

    @GetMapping("/low-stock-products")
    public Result<List<Product>> getLowStockProducts() {
        List<Product> products = productMapper.selectList(null).stream()
                .filter(p -> p.getStatus() == 1 && p.getStock() != null && p.getStock() < 50)
                .sorted(Comparator.comparing(Product::getStock))
                .limit(20)
                .collect(Collectors.toList());
        return Result.success(products);
    }

    @GetMapping("/recent-orders")
    public Result<List<Map<String, Object>>> getRecentOrders() {
        List<Order> orders = orderMapper.selectAllIgnoreLogic().stream()
                .filter(o -> o.getCreatedAt() != null)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(20)
                .collect(Collectors.toList());

        List<Map<String, Object>> result = orders.stream().map(order -> {
            Map<String, Object> item = new HashMap<>();
            item.put("orderNo", order.getOrderNo());
            item.put("totalAmount", order.getTotalAmount());
            item.put("status", order.getStatus());
            item.put("createdAt", order.getCreatedAt());
            return item;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @GetMapping("/all-products")
    public Result<List<Map<String, Object>>> getAllProducts() {
        List<Product> products = productMapper.selectList(null).stream()
                .sorted((a, b) -> {
                    int salesCompare = Integer.compare(
                            b.getSalesCount() != null ? b.getSalesCount() : 0,
                            a.getSalesCount() != null ? a.getSalesCount() : 0);
                    return salesCompare != 0 ? salesCompare : Long.compare(a.getId(), b.getId());
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> result = products.stream().map(p -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("name", p.getName());
            item.put("sku", p.getSku());
            item.put("price", p.getPrice());
            item.put("stock", p.getStock());
            item.put("salesCount", p.getSalesCount());
            item.put("status", p.getStatus());
            return item;
        }).collect(Collectors.toList());

        return Result.success(result);
    }
}
