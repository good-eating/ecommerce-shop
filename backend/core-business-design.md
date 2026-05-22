# 核心业务逻辑设计

## 1. 下单事务伪代码

```java
@Service
@Transactional
public class OrderService {
    
    public OrderDTO createOrder(OrderCreateRequest request) {
        // 1. 校验用户和收货地址
        User user = validateUser(request.getUserId());
        validateShippingAddress(request.getShippingAddress());
        
        // 2. 获取购物车商品（带Redis分布式锁）
        List<CartItemDTO> cartItems = getCartItemsWithLock(user.getId());
        
        // 3. 校验库存和价格
        List<ProductStock> productStocks = validateStockAndPrice(cartItems);
        
        // 4. 生成订单号
        String orderNo = generateOrderNo();
        
        // 5. 创建订单主记录
        Order order = createOrderRecord(orderNo, user, request, cartItems);
        
        // 6. 创建订单商品项（含快照）
        List<OrderItem> orderItems = createOrderItems(order, cartItems, productStocks);
        
        // 7. 扣减库存（乐观锁）
        deductStock(productStocks);
        
        // 8. 清空购物车
        clearCart(user.getId());
        
        // 9. 记录销售数据
        recordSalesData(order, orderItems);
        
        // 10. 异步发送邮件
        asyncSendOrderEmail(order, user);
        
        // 11. 失效相关缓存
        invalidateCaches(user.getId(), orderItems);
        
        return convertToDTO(order, orderItems);
    }
    
    private List<ProductStock> validateStockAndPrice(List<CartItemDTO> cartItems) {
        List<ProductStock> stocks = new ArrayList<>();
        
        for (CartItemDTO item : cartItems) {
            // 使用Redis分布式锁防止超卖
            String lockKey = "lock:product:stock:" + item.getProductId();
            boolean locked = redisLockService.tryLock(lockKey, 5, TimeUnit.SECONDS);
            
            if (!locked) {
                throw new BusinessException("商品库存校验中，请稍后重试");
            }
            
            try {
                Product product = productService.getById(item.getProductId());
                if (product == null || product.getStatus() != 1) {
                    throw new BusinessException("商品不存在或已下架");
                }
                
                if (product.getStock() < item.getQuantity()) {
                    throw new BusinessException("商品库存不足");
                }
                
                // 校验价格是否发生变化
                if (!product.getPrice().equals(item.getPrice())) {
                    throw new BusinessException("商品价格已更新，请重新确认");
                }
                
                stocks.add(new ProductStock(product.getId(), item.getQuantity(), product.getVersion()));
            } finally {
                redisLockService.unlock(lockKey);
            }
        }
        
        return stocks;
    }
    
    private List<OrderItem> createOrderItems(Order order, List<CartItemDTO> cartItems, 
                                           List<ProductStock> productStocks) {
        List<OrderItem> orderItems = new ArrayList<>();
        
        for (int i = 0; i < cartItems.size(); i++) {
            CartItemDTO cartItem = cartItems.get(i);
            ProductStock stock = productStocks.get(i);
            
            Product product = productService.getById(cartItem.getProductId());
            
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName()); // 快照字段
            orderItem.setSku(product.getSku()); // 快照字段
            orderItem.setPriceAtPurchase(product.getPrice()); // 快照字段
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            
            // 完整商品快照
            ProductSnapshot snapshot = createProductSnapshot(product);
            orderItem.setSnapshotJson(JSON.toJSONString(snapshot));
            
            orderItems.add(orderItem);
        }
        
        return orderItemMapper.batchInsert(orderItems);
    }
    
    @Async
    public void asyncSendOrderEmail(Order order, User user) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(user.getEmail());
            helper.setSubject("订单创建成功 - " + order.getOrderNo());
            helper.setText(buildOrderEmailContent(order, user), true);
            
            javaMailSender.send(message);
        } catch (Exception e) {
            log.error("发送订单邮件失败: {}", order.getOrderNo(), e);
        }
    }
}
```

## 2. 购物车同步逻辑

### 购物车操作流程
```java
@Service
public class CartService {
    
    public void addToCart(CartAddRequest request) {
        // 1. 获取Redis购物车锁
        String lockKey = "lock:cart:user:" + request.getUserId();
        boolean locked = redisLockService.tryLock(lockKey, 3, TimeUnit.SECONDS);
        
        if (!locked) {
            throw new BusinessException("购物车操作中，请稍后重试");
        }
        
        try {
            // 2. 同步操作Redis和DB
            addToRedisCart(request);
            addToDBCart(request);
            
            // 3. 更新购物车统计
            updateCartStats(request.getUserId());
        } finally {
            redisLockService.unlock(lockKey);
        }
    }
    
    private void addToRedisCart(CartAddRequest request) {
        String cartKey = "cart:user:" + request.getUserId();
        String itemKey = "product:" + request.getProductId();
        
        CartItemRedisDTO redisItem = new CartItemRedisDTO();
        redisItem.setQuantity(request.getQuantity());
        redisItem.setPrice(request.getPrice());
        redisItem.setAddedAt(System.currentTimeMillis());
        
        // 使用hash结构存储
        redisTemplate.opsForHash().put(cartKey, itemKey, JSON.toJSONString(redisItem));
        
        // 设置TTL
        redisTemplate.expire(cartKey, 30, TimeUnit.DAYS);
    }
    
    private void addToDBCart(CartAddRequest request) {
        // 查询或创建购物车
        Cart cart = cartMapper.selectByUserId(request.getUserId());
        if (cart == null) {
            cart = createNewCart(request.getUserId());
        }
        
        // 查询购物车项
        CartItem cartItem = cartItemMapper.selectByCartIdAndProductId(cart.getId(), request.getProductId());
        
        if (cartItem != null) {
            // 更新数量
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
            cartItemMapper.updateById(cartItem);
        } else {
            // 新增购物车项
            cartItem = new CartItem();
            cartItem.setCartId(cart.getId());
            cartItem.setProductId(request.getProductId());
            cartItem.setQuantity(request.getQuantity());
            cartItem.setPrice(request.getPrice());
            cartItemMapper.insert(cartItem);
        }
    }
    
    // 登录/结算时强制合并
    public void mergeCartOnLogin(Long userId, String sessionCartKey) {
        // 1. 获取会话购物车
        Map<Object, Object> sessionCart = redisTemplate.opsForHash().entries(sessionCartKey);
        
        if (!sessionCart.isEmpty()) {
            // 2. 合并到用户购物车
            String userCartKey = "cart:user:" + userId;
            
            for (Map.Entry<Object, Object> entry : sessionCart.entrySet()) {
                String productKey = (String) entry.getKey();
                CartItemRedisDTO sessionItem = JSON.parseObject((String) entry.getValue(), CartItemRedisDTO.class);
                
                // 合并策略：数量相加，取最新价格
                CartItemRedisDTO userItem = getUserCartItem(userCartKey, productKey);
                
                if (userItem != null) {
                    userItem.setQuantity(userItem.getQuantity() + sessionItem.getQuantity());
                    userItem.setPrice(sessionItem.getPrice()); // 取最新价格
                } else {
                    userItem = sessionItem;
                }
                
                redisTemplate.opsForHash().put(userCartKey, productKey, JSON.toJSONString(userItem));
            }
            
            // 3. 同步到DB
            syncCartToDB(userId);
            
            // 4. 清理会话购物车
            redisTemplate.delete(sessionCartKey);
        }
    }
}
```

## 3. 推荐模块架构

### 离线计算步骤
```java
@Component
public class RecommendationService {
    
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void computeItemSimilarity() {
        log.info("开始计算商品相似度...");
        
        // 1. 获取销售记录（最近30天）
        List<SalesRecord> salesRecords = getRecentSalesRecords(30);
        
        // 2. 构建共现矩阵
        Map<Long, Map<Long, Integer>> cooccurrenceMatrix = buildCooccurrenceMatrix(salesRecords);
        
        // 3. 计算相似度（余弦相似度）
        Map<Long, List<SimilarityItem>> similarityMap = computeCosineSimilarity(cooccurrenceMatrix);
        
        // 4. 存储到Redis
        storeSimilarityToRedis(similarityMap);
        
        log.info("商品相似度计算完成");
    }
    
    private Map<Long, Map<Long, Integer>> buildCooccurrenceMatrix(List<SalesRecord> salesRecords) {
        // 按用户分组
        Map<Long, List<Long>> userProductMap = salesRecords.stream()
            .collect(Collectors.groupingBy(SalesRecord::getUserId,
                Collectors.mapping(SalesRecord::getProductId, Collectors.toList())));
        
        Map<Long, Map<Long, Integer>> matrix = new HashMap<>();
        
        for (List<Long> products : userProductMap.values()) {
            // 对每个用户的购买商品两两组合
            for (int i = 0; i < products.size(); i++) {
                for (int j = i + 1; j < products.size(); j++) {
                    Long product1 = products.get(i);
                    Long product2 = products.get(j);
                    
                    matrix.computeIfAbsent(product1, k -> new HashMap<>())
                         .merge(product2, 1, Integer::sum);
                    matrix.computeIfAbsent(product2, k -> new HashMap<>())
                         .merge(product1, 1, Integer::sum);
                }
            }
        }
        
        return matrix;
    }
    
    private void storeSimilarityToRedis(Map<Long, List<SimilarityItem>> similarityMap) {
        for (Map.Entry<Long, List<SimilarityItem>> entry : similarityMap.entrySet()) {
            String key = "item:sim:" + entry.getKey();
            
            // 清空旧数据
            redisTemplate.delete(key);
            
            // 存储相似商品（zset结构）
            for (SimilarityItem item : entry.getValue()) {
                if (item.getSimilarity() > 0.1) { // 过滤低相似度
                    redisTemplate.opsForZSet().add(key, 
                        "product:" + item.getProductId(), item.getSimilarity());
                }
            }
            
            // 设置TTL
            redisTemplate.expire(key, 7, TimeUnit.DAYS);
        }
    }
    
    // 在线推荐API
    public List<ProductDTO> getRecommendations(Long userId, int limit) {
        // 1. 尝试获取个性化推荐
        List<Long> recommendedProductIds = getPersonalizedRecommendations(userId, limit);
        
        // 2. 如果个性化推荐不足，使用冷启动策略
        if (recommendedProductIds.size() < limit) {
            List<Long> coldStartIds = getColdStartRecommendations(limit - recommendedProductIds.size());
            recommendedProductIds.addAll(coldStartIds);
        }
        
        // 3. 获取商品详情
        return productService.getProductsByIds(recommendedProductIds);
    }
    
    private List<Long> getPersonalizedRecommendations(Long userId, int limit) {
        // 检查是否有缓存推荐
        String recoKey = "reco:user:" + userId;
        List<Object> cachedRecos = redisTemplate.opsForList().range(recoKey, 0, limit - 1);
        
        if (cachedRecos != null && !cachedRecos.isEmpty()) {
            return cachedRecos.stream()
                .map(obj -> Long.parseLong(obj.toString().replace("product:", "")))
                .collect(Collectors.toList());
        }
        
        // 实时计算推荐（简化版）
        List<Long> userPurchaseHistory = getRecentUserPurchases(userId, 10);
        Set<Long> recommendations = new HashSet<>();
        
        for (Long purchasedProductId : userPurchaseHistory) {
            String simKey = "item:sim:" + purchasedProductId;
            Set<ZSetOperations.TypedTuple<String>> similarProducts = 
                redisTemplate.opsForZSet().reverseRangeWithScores(simKey, 0, 5);
            
            if (similarProducts != null) {
                for (ZSetOperations.TypedTuple<String> tuple : similarProducts) {
                    Long productId = Long.parseLong(tuple.getValue().replace("product:", ""));
                    if (!userPurchaseHistory.contains(productId)) {
                        recommendations.add(productId);
                        if (recommendations.size() >= limit) break;
                    }
                }
            }
        }
        
        // 缓存推荐结果
        if (!recommendations.isEmpty()) {
            List<String> recoValues = recommendations.stream()
                .map(id -> "product:" + id)
                .collect(Collectors.toList());
            
            redisTemplate.opsForList().rightPushAll(recoKey, recoValues);
            redisTemplate.expire(recoKey, 1, TimeUnit.DAYS);
        }
        
        return new ArrayList<>(recommendations);
    }
}
```

## 4. 行为采集AOP设计

```java
@Aspect
@Component
@Slf4j
public class BehaviorLogAspect {
    
    @Autowired
    private BehaviorLogService behaviorLogService;
    
    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping) || " +
           "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
           "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
           "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
           "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object logBehavior(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        HttpServletRequest request = getCurrentRequest();
        
        BehaviorLog behaviorLog = new BehaviorLog();
        behaviorLog.setPath(request.getRequestURI());
        behaviorLog.setMethod(request.getMethod());
        behaviorLog.setIp(getClientIp(request));
        behaviorLog.setUserAgent(request.getHeader("User-Agent"));
        
        // 获取用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !(authentication instanceof AnonymousAuthenticationToken)) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            behaviorLog.setUserId(Long.parseLong(userDetails.getUsername()));
        }
        
        // 参数脱敏处理
        behaviorLog.setParams(desensitizeParams(getRequestParams(joinPoint, request)));
        
        Object result;
        try {
            result = joinPoint.proceed();
            behaviorLog.setStatusCode(200);
        } catch (Exception e) {
            behaviorLog.setStatusCode(500);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            behaviorLog.setDuration((int) duration);
            
            // 异步保存日志
            behaviorLogService.asyncSaveLog(behaviorLog);
        }
        
        return result;
    }
    
    private String desensitizeParams(String originalParams) {
        if (StringUtils.isBlank(originalParams)) {
            return originalParams;
        }
        
        // 脱敏敏感信息
        return originalParams
            .replaceAll("(?i)password[\\s]*[:=][\\s]*[^,&}]*", "password=***")
            .replaceAll("(?i)token[\\s]*[:=][\\s]*[^,&}]*", "token=***")
            .replaceAll("(?i)email[\\s]*[:=][\\s]*([^,&}@]+)@", "email=***@")
            .replaceAll("(?i)phone[\\s]*[:=][\\s]*\\d{3}(\\d{4})\\d{4}", "phone=***$1****");
    }
}

@Service
public class BehaviorLogService {
    
    @Async
    public void asyncSaveLog(BehaviorLog behaviorLog) {
        try {
            // 批量写入优化：先积累到队列，定时批量入库
            behaviorLogQueue.offer(behaviorLog);
            
            if (behaviorLogQueue.size() >= BATCH_SIZE) {
                batchSaveLogs();
            }
        } catch (Exception e) {
            log.error("保存行为日志失败", e);
        }
    }
    
    @Scheduled(fixedRate = 30000) // 每30秒执行一次
    public void batchSaveLogs() {
        List<BehaviorLog> logs = new ArrayList<>();
        BehaviorLog log;
        
        while ((log = behaviorLogQueue.poll()) != null && logs.size() < BATCH_SIZE) {
            logs.add(log);
        }
        
        if (!logs.isEmpty()) {
            behaviorLogMapper.batchInsert(logs);
        }
    }
}