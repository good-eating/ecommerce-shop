package com.ecommerce.service;

import com.alibaba.fastjson2.JSON;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.dto.OrderCreateRequest;
import com.ecommerce.dto.OrderDTO;
import com.ecommerce.dto.OrderItemDTO;
import com.ecommerce.dto.CartDTO;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.SalesRecord;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.mapper.OrderItemMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.SalesRecordMapper;
import com.ecommerce.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final SalesRecordMapper salesRecordMapper;
    private final UserMapper userMapper;
    private final CartService cartService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;

    public OrderServiceImpl(OrderMapper orderMapper, OrderItemMapper orderItemMapper, ProductMapper productMapper, SalesRecordMapper salesRecordMapper, UserMapper userMapper, CartService cartService, RedisTemplate<String, Object> redisTemplate, EmailService emailService) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.salesRecordMapper = salesRecordMapper;
        this.userMapper = userMapper;
        this.cartService = cartService;
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public OrderDTO createOrder(Long userId, OrderCreateRequest request) {
        // 1. 获取用户购物车
        CartDTO cart = cartService.getCart(userId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessException("购物车为空");
        }

        // 2. 校验库存并锁定
        List<ProductStock> productStocks = validateAndLockStock(cart.getItems());

        // 3. 生成订单号
        String orderNo = generateOrderNo();

        // 4. 计算订单金额
        BigDecimal totalAmount = cart.getItems().stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. 创建订单主记录
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(totalAmount);
        order.setStatus(0); // 待付款
        order.setShippingAddress(request.getShippingAddress());
        order.setRemark(request.getRemark());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.insert(order);

        // 6. 创建订单商品项（含快照）
        List<OrderItem> orderItems = createOrderItems(order.getId(), cart.getItems(), productStocks);

        // 7. 扣减库存（乐观锁）
        deductStock(productStocks);

        // 8. 清空购物车
        cartService.clearCart(userId);

        // 9. 记录销售数据
        recordSalesData(order, orderItems);

        // 10. 异步发送邮件
        User user = userMapper.selectById(userId);
        if (user != null) {
            emailService.sendOrderEmail(order, user, orderItems);
        }

        // 11. 失效推荐相关缓存
        invalidateRecommendationCache(userId);

        return convertToDTO(order, orderItems);
    }

    @Override
    public OrderDTO getOrderById(Long userId, Long orderId) {
        Order order = orderMapper.selectByIdIgnoreLogic(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权查看此订单");
        }

        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
        return convertToDTO(order, orderItems);
    }

    @Override
    public PageResult<OrderDTO> getUserOrders(Long userId, Integer page, Integer size) {
        // 手动分页，避免 MyBatis-Plus 全局逻辑删除（status=0 被当作已删除）影响订单查询
        int offset = (page - 1) * size;
        List<Order> orderList = orderMapper.selectPageByUserId(userId, offset, size);
        Long total = orderMapper.countByUserId(userId);

        List<OrderDTO> items = orderList.stream()
                .map(order -> {
                    List<OrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId());
                    return convertToDTO(order, orderItems);
                })
                .collect(Collectors.toList());

        return PageResult.of(total, items, (long) page, (long) size);
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectByIdIgnoreLogic(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }

        if (order.getStatus() != 0) {
            throw new BusinessException("只能取消待付款订单");
        }

        order.setStatus(4); // 已取消
        orderMapper.updateStatus(orderId, 4);

        // 恢复库存
        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
        for (OrderItem item : orderItems) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                productMapper.updateById(product);
            }
        }
    }

    @Override
    @Transactional
    public void payOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectByIdIgnoreLogic(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }

        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态不正确");
        }

        // 模拟支付成功
        orderMapper.updatePaymentStatus(orderId, 1, "模拟支付", LocalDateTime.now());
    }

    @Override
    @Transactional
    public void shipOrder(Long orderId) {
        Order order = orderMapper.selectByIdIgnoreLogic(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (order.getStatus() != 1) {
            throw new BusinessException("只能发货已付款的订单");
        }

        orderMapper.updateStatus(orderId, 2);
    }

    @Override
    @Transactional
    public void confirmReceipt(Long userId, Long orderId) {
        Order order = orderMapper.selectByIdIgnoreLogic(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }

        if (order.getStatus() != 2) {
            throw new BusinessException("只能确认已发货的订单");
        }

        orderMapper.updateStatus(orderId, 3);
        orderMapper.updateReceivedTime(orderId, LocalDateTime.now());
    }

    @Override
    public PageResult<OrderDTO> getPaidOrders(Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<Order> orderList = orderMapper.selectPaidOrders(offset, size);
        Long total = orderMapper.countPaidOrders();

        List<OrderDTO> items = orderList.stream()
                .map(order -> {
                    List<OrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId());
                    return convertToDTO(order, orderItems);
                })
                .collect(Collectors.toList());

        return PageResult.of(total, items, (long) page, (long) size);
    }

    @Override
    public PageResult<OrderDTO> getShippedOrders(Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<Order> orderList = orderMapper.selectShippedOrders(offset, size);
        Long total = orderMapper.countShippedOrders();

        List<OrderDTO> items = orderList.stream()
                .map(order -> {
                    List<OrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId());
                    return convertToDTO(order, orderItems);
                })
                .collect(Collectors.toList());

        return PageResult.of(total, items, (long) page, (long) size);
    }

    private List<ProductStock> validateAndLockStock(List<CartItemDTO> cartItems) {
        List<ProductStock> stocks = new ArrayList<>();

        for (CartItemDTO item : cartItems) {
            // 使用Redis分布式锁防止超卖
            String lockKey = "lock:product:stock:" + item.getProductId();
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);

            if (!locked) {
                throw new BusinessException("商品库存校验中，请稍后重试");
            }

            try {
                Product product = productMapper.selectById(item.getProductId());
                if (product == null || product.getStatus() != 1) {
                    throw new BusinessException("商品不存在或已下架");
                }

                if (product.getStock() < item.getQuantity()) {
                    throw new BusinessException("商品【" + product.getName() + "】库存不足");
                }

                // 校验价格是否发生变化
                if (product.getPrice().compareTo(item.getPrice()) != 0) {
                    throw new BusinessException("商品【" + product.getName() + "】价格已更新，请重新确认");
                }

                stocks.add(new ProductStock(product.getId(), item.getQuantity(), product.getVersion()));
            } finally {
                redisTemplate.delete(lockKey);
            }
        }

        return stocks;
    }

    private List<OrderItem> createOrderItems(Long orderId, List<CartItemDTO> cartItems, List<ProductStock> productStocks) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (int i = 0; i < cartItems.size(); i++) {
            CartItemDTO cartItem = cartItems.get(i);
            Product product = productMapper.selectById(cartItem.getProductId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(orderId);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName()); // 快照字段
            orderItem.setSku(product.getSku()); // 快照字段
            orderItem.setPriceAtPurchase(product.getPrice()); // 快照字段
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));

            // 完整商品快照
            ProductSnapshot snapshot = new ProductSnapshot();
            snapshot.setId(product.getId());
            snapshot.setName(product.getName());
            snapshot.setSku(product.getSku());
            snapshot.setDescription(product.getDescription());
            snapshot.setPrice(product.getPrice());
            snapshot.setOriginalPrice(product.getOriginalPrice());
            snapshot.setAttributesJson(product.getAttributesJson());
            snapshot.setCreatedAt(product.getCreatedAt());

            orderItem.setSnapshotJson(JSON.toJSONString(snapshot));

            orderItemMapper.insert(orderItem);
            orderItems.add(orderItem);
        }

        return orderItems;
    }

    private void deductStock(List<ProductStock> productStocks) {
        for (ProductStock stock : productStocks) {
            Product product = productMapper.selectById(stock.getProductId());
            if (product != null) {
                product.setStock(product.getStock() - stock.getQuantity());
                product.setSalesCount(product.getSalesCount() + stock.getQuantity());
                productMapper.updateById(product);
            }
        }
    }

    private void recordSalesData(Order order, List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            SalesRecord record = new SalesRecord();
            record.setUserId(order.getUserId());
            record.setProductId(item.getProductId());
            record.setOrderId(order.getId());
            record.setQuantity(item.getQuantity());
            record.setAmount(item.getSubtotal());
            salesRecordMapper.insert(record);
        }
    }

    private void invalidateRecommendationCache(Long userId) {
        // 失效用户推荐缓存
        redisTemplate.delete("reco:user:" + userId);
        // 失效热销榜缓存
        redisTemplate.delete("product:top:daily");
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", (int) (Math.random() * 10000));
        return "ORD" + timestamp + random;
    }

    private OrderDTO convertToDTO(Order order, List<OrderItem> orderItems) {
        List<OrderItemDTO> itemDTOs = orderItems.stream()
                .map(item -> OrderItemDTO.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .sku(item.getSku())
                        .priceAtPurchase(item.getPriceAtPurchase())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .snapshotJson(item.getSnapshotJson())
                        .build())
                .collect(Collectors.toList());

        String statusText;
        switch (order.getStatus()) {
            case 0: statusText = "待付款"; break;
            case 1: statusText = "已付款"; break;
            case 2: statusText = "已发货"; break;
            case 3: statusText = "已完成"; break;
            case 4: statusText = "已取消"; break;
            default: statusText = "未知"; break;
        }

        return OrderDTO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .payAmount(order.getPayAmount())
                .status(order.getStatus())
                .statusText(statusText)
                .paymentMethod(order.getPaymentMethod())
                .paymentTime(order.getPaymentTime())
                .shippingAddress(order.getShippingAddress())
                .remark(order.getRemark())
                .receivedTime(order.getReceivedTime())
                .createdAt(order.getCreatedAt())
                .items(itemDTOs)
                .build();
    }

    // 内部类：库存信息
    private static class ProductStock {
        private final Long productId;
        private final Integer quantity;
        private final Integer version;

        public ProductStock(Long productId, Integer quantity, Integer version) {
            this.productId = productId;
            this.quantity = quantity;
            this.version = version;
        }

        public Long getProductId() { return productId; }
        public Integer getQuantity() { return quantity; }
        public Integer getVersion() { return version; }
    }

    // 内部类：商品快照
    private static class ProductSnapshot {
        private Long id;
        private String name;
        private String sku;
        private String description;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private String attributesJson;
        private LocalDateTime createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public BigDecimal getOriginalPrice() { return originalPrice; }
        public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
        public String getAttributesJson() { return attributesJson; }
        public void setAttributesJson(String attributesJson) { this.attributesJson = attributesJson; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}