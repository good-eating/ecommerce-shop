package com.ecommerce.task;

import com.ecommerce.mapper.OrderItemMapper;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderAutoCancelTask {

    private static final Logger log = LoggerFactory.getLogger(OrderAutoCancelTask.class);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;

    @Value("${file.order.auto-cancel-minutes:30}")
    private int autoCancelMinutes;

    public OrderAutoCancelTask(OrderMapper orderMapper, OrderItemMapper orderItemMapper, ProductMapper productMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredOrders() {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(autoCancelMinutes);
        
        List<Order> expiredOrders = orderMapper.selectExpiredOrders(expireTime);
        
        if (expiredOrders.isEmpty()) {
            log.debug("没有需要自动取消的订单");
            return;
        }
        
        log.info("开始处理过期订单，共 {} 个", expiredOrders.size());
        
        for (Order order : expiredOrders) {
            try {
                // 更新订单状态为已取消
                orderMapper.updateStatus(order.getId(), 4);
                
                // 恢复库存
                List<OrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId());
                for (OrderItem item : orderItems) {
                    Product product = productMapper.selectById(item.getProductId());
                    if (product != null) {
                        product.setStock(product.getStock() + item.getQuantity());
                        productMapper.updateById(product);
                    }
                }
                
                log.info("订单 {} 已自动取消", order.getOrderNo());
            } catch (Exception e) {
                log.error("取消订单 {} 失败: {}", order.getOrderNo(), e.getMessage());
            }
        }
        
        log.info("过期订单处理完成");
    }
}
