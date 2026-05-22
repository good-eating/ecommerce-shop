package com.ecommerce.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.Result;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.SalesRecord;
import com.ecommerce.entity.User;
import com.ecommerce.entity.UserRole;
import com.ecommerce.mapper.*;
import com.ecommerce.mapper.LoginLogMapper;
import com.ecommerce.service.CategoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final SalesRecordMapper salesRecordMapper;
    private final BehaviorLogMapper behaviorLogMapper;
    private final LoginLogMapper loginLogMapper;
    private final CategoryService categoryService;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    public AnalyticsController(OrderMapper orderMapper, ProductMapper productMapper,
                                UserMapper userMapper, SalesRecordMapper salesRecordMapper,
                                BehaviorLogMapper behaviorLogMapper,
                                LoginLogMapper loginLogMapper,
                                CategoryService categoryService,
                                UserRoleMapper userRoleMapper,
                                RoleMapper roleMapper) {
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.salesRecordMapper = salesRecordMapper;
        this.behaviorLogMapper = behaviorLogMapper;
        this.loginLogMapper = loginLogMapper;
        this.categoryService = categoryService;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        Long todayOrders = orderMapper.countTodayOrders(todayStart);
        BigDecimal todaySales = orderMapper.sumTodaySales(todayStart);
        long totalUsers = userMapper.selectCount(null);
        long totalProducts = productMapper.selectCount(null);

        stats.put("todayOrders", todayOrders != null ? todayOrders.intValue() : 0);
        stats.put("todaySales", todaySales != null ? todaySales : BigDecimal.ZERO);
        stats.put("totalUsers", (int) totalUsers);
        stats.put("totalProducts", (int) totalProducts);

        return Result.success(stats);
    }

    @GetMapping("/top-products")
    public Result<List<Product>> getTopProducts(@RequestParam(defaultValue = "10") Integer limit) {
        List<Product> products = productMapper.selectList(null).stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                .filter(p -> p.getSalesCount() != null)
                .sorted((a, b) -> b.getSalesCount().compareTo(a.getSalesCount()))
                .limit(limit)
                .collect(Collectors.toList());
        return Result.success(products);
    }

    @GetMapping("/sales-trend")
    public Result<List<Map<String, Object>>> getSalesTrend(@RequestParam(defaultValue = "week") String period) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        if ("day".equals(period)) {
            LocalDateTime todayStart = LocalDateTime.of(today, LocalTime.MIN);
            List<Map<String, Object>> hourlyRaw = orderMapper.selectHourlyStats(todayStart);
            Map<Integer, Map<String, Object>> byHour = new HashMap<>();
            for (Map<String, Object> row : hourlyRaw) {
                int h = ((Number) row.get("hour")).intValue();
                byHour.put(h, row);
            }
            for (int hour = 0; hour < 24; hour++) {
                Map<String, Object> item = new HashMap<>();
                item.put("label", hour + ":00");
                Object val = byHour.containsKey(hour) ? byHour.get(hour).get("sales_amount") : BigDecimal.ZERO;
                item.put("amount", val != null ? val : BigDecimal.ZERO);
                trend.add(item);
            }
        } else {
            int days = "month".equals(period) ? 30 : 7;
            List<Order> allOrders = orderMapper.selectAllIgnoreLogic();

            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);

                BigDecimal dayAmount = allOrders.stream()
                        .filter(o -> o.getCreatedAt() != null
                                && o.getCreatedAt().toLocalDate().equals(date)
                                && o.getStatus() != null && o.getStatus() >= 1
                                && o.getTotalAmount() != null)
                        .map(Order::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                Map<String, Object> item = new HashMap<>();
                item.put("label", (i == 0 ? "今天" : (i == 1 ? "昨天" : date.toString().substring(5))));
                item.put("date", date.toString());
                item.put("amount", dayAmount);
                trend.add(item);
            }
        }

        BigDecimal maxAmount = trend.stream()
                .map(m -> (BigDecimal) m.get("amount"))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);

        for (Map<String, Object> item : trend) {
            BigDecimal amount = (BigDecimal) item.get("amount");
            item.put("percentage", amount.multiply(BigDecimal.valueOf(100))
                    .divide(maxAmount, 0, BigDecimal.ROUND_HALF_UP).intValue());
        }

        return Result.success(trend);
    }

    @GetMapping("/sales-by-category")
    public Result<List<Map<String, Object>>> getSalesByCategory() {
        List<Product> products = productMapper.selectList(null).stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                .filter(p -> p.getCategoryId() != null)
                .collect(Collectors.toList());

        Map<Long, String> categoryNames = categoryService.getAllCategories().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        Map<Long, List<Product>> byCategory = products.stream()
                .collect(Collectors.groupingBy(Product::getCategoryId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<Product>> entry : byCategory.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("categoryId", entry.getKey());
            item.put("categoryName", categoryNames.getOrDefault(entry.getKey(), "未知分类"));
            item.put("count", entry.getValue().size());
            item.put("totalSales", entry.getValue().stream()
                    .filter(p -> p.getSalesCount() != null)
                    .map(Product::getSalesCount)
                    .reduce(0, Integer::sum));
            item.put("totalStock", entry.getValue().stream()
                    .filter(p -> p.getStock() != null)
                    .map(Product::getStock)
                    .reduce(0, Integer::sum));
            result.add(item);
        }

        return Result.success(result);
    }

    @GetMapping("/order-status-distribution")
    public Result<Map<String, Object>> getOrderStatusDistribution() {
        List<Order> allOrders = orderMapper.selectAllIgnoreLogic();

        long unpaid = allOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 0).count();
        long paid = allOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 1).count();
        long shipped = allOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 2).count();
        long completed = allOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 3).count();
        long cancelled = allOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 4).count();

        Map<String, Object> result = new HashMap<>();
        result.put("unpaid", (int) unpaid);
        result.put("paid", (int) paid);
        result.put("shipped", (int) shipped);
        result.put("completed", (int) completed);
        result.put("cancelled", (int) cancelled);

        return Result.success(result);
    }

    @GetMapping("/sales-performance")
    public Result<List<Map<String, Object>>> getSalesPerformance() {
        List<SalesRecord> records = salesRecordMapper.selectList(null);

        Map<Long, List<SalesRecord>> byUser = records.stream()
                .filter(r -> r.getUserId() != null)
                .collect(Collectors.groupingBy(SalesRecord::getUserId));

        // 只获取SALES角色的用户
        Role salesRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getCode, "SALES"));
        Set<Long> salesUserIds = new HashSet<>();
        if (salesRole != null) {
            List<UserRole> userRoles = userRoleMapper.selectList(
                    new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, salesRole.getId()));
            salesUserIds = userRoles.stream().map(UserRole::getUserId).collect(Collectors.toSet());
        }

        Map<Long, String> userNames = new HashMap<>();
        if (!salesUserIds.isEmpty()) {
            List<User> salesUsers = userMapper.selectBatchIds(salesUserIds);
            for (User user : salesUsers) {
                userNames.put(user.getId(), user.getUsername());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<SalesRecord>> entry : byUser.entrySet()) {
            Long userId = entry.getKey();
            // 只包含SALES角色的用户
            if (!salesUserIds.contains(userId)) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("userId", userId);
            item.put("userName", userNames.getOrDefault(userId, "未知用户"));
            item.put("orderCount", entry.getValue().stream()
                    .filter(r -> r.getOrderId() != null)
                    .map(SalesRecord::getOrderId)
                    .distinct()
                    .count());
            item.put("totalAmount", entry.getValue().stream()
                    .filter(r -> r.getAmount() != null)
                    .map(SalesRecord::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            item.put("totalQuantity", entry.getValue().stream()
                    .filter(r -> r.getQuantity() != null)
                    .map(SalesRecord::getQuantity)
                    .reduce(0, Integer::sum));
            result.add(item);
        }

        result.sort((a, b) -> ((BigDecimal) b.get("totalAmount"))
                .compareTo((BigDecimal) a.get("totalAmount")));

        return Result.success(result);
    }

    @GetMapping("/today-overview")
    public Result<Map<String, Object>> getTodayOverview() {
        Map<String, Object> overview = new HashMap<>();
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        Long todayOrders = orderMapper.countTodayOrders(todayStart);
        BigDecimal todaySales = orderMapper.sumTodaySales(todayStart);

        long todayNewUsers = userMapper.selectList(null).stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(todayStart))
                .count();

        long todayPaidOrders = orderMapper.countTodayPaidOrders(todayStart);

        long totalProducts = productMapper.selectCount(null);
        long totalUsers = userMapper.selectCount(null);

        overview.put("todayOrders", todayOrders != null ? todayOrders.intValue() : 0);
        overview.put("todaySales", todaySales != null ? todaySales : BigDecimal.ZERO);
        overview.put("todayNewUsers", (int) todayNewUsers);
        overview.put("todayPaidOrders", todayOrders != null ? todayOrders.intValue() : 0);
        overview.put("totalProducts", (int) totalProducts);
        overview.put("totalUsers", (int) totalUsers);

        return Result.success(overview);
    }

    @GetMapping("/today-orders-detail")
    public Result<List<Map<String, Object>>> getTodayOrdersDetail() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        List<Map<String, Object>> hourlyRaw = orderMapper.selectHourlyStats(todayStart);
        Map<Integer, Map<String, Object>> byHour = new HashMap<>();
        for (Map<String, Object> row : hourlyRaw) {
            int h = ((Number) row.get("hour")).intValue();
            byHour.put(h, row);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            Map<String, Object> item = new HashMap<>();
            item.put("hour", hour + ":00");
            Object val = byHour.containsKey(hour) ? byHour.get(hour).get("order_count") : 0;
            item.put("orders", val != null ? ((Number) val).intValue() : 0);
            result.add(item);
        }
        return Result.success(result);
    }

    @GetMapping("/today-sales-detail")
    public Result<List<Map<String, Object>>> getTodaySalesDetail() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        List<Map<String, Object>> hourlyRaw = orderMapper.selectHourlyStats(todayStart);
        Map<Integer, Map<String, Object>> byHour = new HashMap<>();
        for (Map<String, Object> row : hourlyRaw) {
            int h = ((Number) row.get("hour")).intValue();
            byHour.put(h, row);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            Map<String, Object> item = new HashMap<>();
            item.put("hour", hour + ":00");
            Object val = byHour.containsKey(hour) ? byHour.get(hour).get("sales_amount") : BigDecimal.ZERO;
            item.put("amount", val != null ? val : BigDecimal.ZERO);
            result.add(item);
        }
        return Result.success(result);
    }

    @GetMapping("/user-list")
    public Result<List<Map<String, Object>>> getUserList() {
        List<User> users = userMapper.selectList(null);
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("username", u.getUsername());
            item.put("email", u.getEmail());
            item.put("phone", u.getPhone());
            item.put("status", u.getStatus());
            item.put("createdAt", u.getCreatedAt());
            return item;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    @GetMapping("/product-list")
    public Result<List<Map<String, Object>>> getProductList() {
        List<Product> products = productMapper.selectList(null);
        List<Map<String, Object>> result = products.stream().map(p -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("name", p.getName());
            item.put("sku", p.getSku());
            item.put("price", p.getPrice());
            item.put("stock", p.getStock());
            item.put("salesCount", p.getSalesCount());
            item.put("status", p.getStatus());
            item.put("createdAt", p.getCreatedAt());
            return item;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    @GetMapping("/user-profile-city")
    public Result<List<Map<String, Object>>> getUserCityDistribution() {
        List<User> users = userMapper.selectList(null);
        Map<String, Long> cityCount = users.stream()
                .filter(u -> u.getCity() != null && !u.getCity().isEmpty())
                .collect(Collectors.groupingBy(User::getCity, Collectors.counting()));

        List<Map<String, Object>> result = cityCount.entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("city", e.getKey());
                    item.put("count", e.getValue().intValue());
                    return item;
                })
                .sorted((a, b) -> ((Integer) b.get("count")).compareTo((Integer) a.get("count")))
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @GetMapping("/user-purchasing-power")
    public Result<Map<String, Object>> getUserPurchasingPower() {
        List<Order> allOrders = orderMapper.selectAllIgnoreLogic();
        List<User> allUsers = userMapper.selectList(null);

        Map<Long, BigDecimal> userSpending = allOrders.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() >= 1 && o.getPayAmount() != null)
                .collect(Collectors.groupingBy(
                        Order::getUserId,
                        Collectors.reducing(BigDecimal.ZERO, Order::getPayAmount, BigDecimal::add)
                ));

        long highSpenders = userSpending.values().stream().filter(a -> a.compareTo(BigDecimal.valueOf(5000)) >= 0).count();
        long midSpenders = userSpending.values().stream()
                .filter(a -> a.compareTo(BigDecimal.valueOf(1000)) >= 0 && a.compareTo(BigDecimal.valueOf(5000)) < 0).count();
        long lowSpenders = userSpending.values().stream().filter(a -> a.compareTo(BigDecimal.valueOf(1000)) < 0).count();
        long noSpenders = allUsers.size() - userSpending.size();

        List<Map<String, Object>> distribution = new ArrayList<>();
        distribution.add(new HashMap<String, Object>() {{ put("level", "高消费(≥5000)"); put("count", (int) highSpenders); }});
        distribution.add(new HashMap<String, Object>() {{ put("level", "中消费(1000-5000)"); put("count", (int) midSpenders); }});
        distribution.add(new HashMap<String, Object>() {{ put("level", "低消费(<1000)"); put("count", (int) lowSpenders); }});
        distribution.add(new HashMap<String, Object>() {{ put("level", "未消费"); put("count", (int) noSpenders); }});

        Map<String, Object> result = new HashMap<>();
        result.put("distribution", distribution);
        result.put("totalUsers", allUsers.size());
        result.put("totalSpending", userSpending.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        return Result.success(result);
    }

    @GetMapping("/user-category-preference")
    public Result<List<Map<String, Object>>> getUserCategoryPreference() {
        List<SalesRecord> records = salesRecordMapper.selectList(null);
        Map<Long, String> categoryNames = categoryService.getAllCategories().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        List<Product> allProducts = productMapper.selectList(null);
        Map<Long, Long> productCategoryMap = allProducts.stream()
                .filter(p -> p.getCategoryId() != null)
                .collect(Collectors.toMap(Product::getId, Product::getCategoryId, (a, b) -> a));

        Map<Long, Long> categorySales = records.stream()
                .filter(r -> r.getProductId() != null)
                .map(r -> productCategoryMap.get(r.getProductId()))
                .filter(c -> c != null)
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()));

        List<Map<String, Object>> result = categorySales.entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("categoryId", e.getKey());
                    item.put("categoryName", categoryNames.getOrDefault(e.getKey(), "未知分类"));
                    item.put("salesCount", e.getValue().intValue());
                    return item;
                })
                .sorted((a, b) -> ((Integer) b.get("salesCount")).compareTo((Integer) a.get("salesCount")))
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @GetMapping("/sales-prediction")
    public Result<Map<String, Object>> getSalesPrediction(@RequestParam(defaultValue = "7") Integer days) {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> history = new ArrayList<>();

        List<Order> allOrders = orderMapper.selectAllIgnoreLogic();
        List<BigDecimal> dailyAmounts = new ArrayList<>();

        for (int i = 30; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            BigDecimal dayAmount = allOrders.stream()
                    .filter(o -> o.getCreatedAt() != null
                            && o.getCreatedAt().toLocalDate().equals(date)
                            && o.getStatus() != null && o.getStatus() >= 1
                            && o.getTotalAmount() != null)
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("amount", dayAmount);
            item.put("type", "history");
            history.add(item);
            dailyAmounts.add(dayAmount);
        }

        // 简单移动平均预测
        List<Map<String, Object>> predictions = new ArrayList<>();
        int window = 7;
        for (int i = 1; i <= days; i++) {
            LocalDate predDate = today.plusDays(i);
            BigDecimal predictedAmount = BigDecimal.ZERO;

            if (dailyAmounts.size() >= window) {
                List<BigDecimal> recent = dailyAmounts.subList(dailyAmounts.size() - window, dailyAmounts.size());
                predictedAmount = recent.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(window), 2, BigDecimal.ROUND_HALF_UP);
            }

            Map<String, Object> item = new HashMap<>();
            item.put("date", predDate.toString());
            item.put("amount", predictedAmount);
            item.put("type", "prediction");
            predictions.add(item);

            dailyAmounts.add(predictedAmount);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("history", history);
        result.put("predictions", predictions);
        return Result.success(result);
    }

    @GetMapping("/sales-anomaly-detection")
    public Result<Map<String, Object>> getSalesAnomalyDetection(@RequestParam(defaultValue = "30") Integer days) {
        LocalDate today = LocalDate.now();
        List<Order> allOrders = orderMapper.selectAllIgnoreLogic();

        // 计算最近N天每天的销售额
        List<BigDecimal> dailyAmounts = new ArrayList<>();
        List<String> dateLabels = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            BigDecimal dayAmount = allOrders.stream()
                    .filter(o -> o.getCreatedAt() != null
                            && o.getCreatedAt().toLocalDate().equals(date)
                            && o.getStatus() != null && o.getStatus() >= 1
                            && o.getTotalAmount() != null)
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dailyAmounts.add(dayAmount);
            dateLabels.add(date.toString());
        }

        // 计算均值与标准差 (Z-Score)
        BigDecimal mean = dailyAmounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(dailyAmounts.size()), 2, BigDecimal.ROUND_HALF_UP);

        double variance = dailyAmounts.stream()
                .mapToDouble(a -> Math.pow(a.subtract(mean).doubleValue(), 2))
                .average()
                .orElse(0);
        double stdDev = Math.sqrt(variance);
        double threshold = 2.0; // Z-Score > 2 视为异常

        List<Map<String, Object>> anomalies = new ArrayList<>();
        List<Map<String, Object>> dailyData = new ArrayList<>();

        for (int i = 0; i < dailyAmounts.size(); i++) {
            BigDecimal amount = dailyAmounts.get(i);
            double zScore = stdDev > 0 ? Math.abs(amount.subtract(mean).doubleValue()) / stdDev : 0;
            boolean isAnomaly = zScore > threshold;

            Map<String, Object> item = new HashMap<>();
            item.put("date", dateLabels.get(i));
            item.put("amount", amount);
            item.put("zScore", Math.round(zScore * 100.0) / 100.0);
            item.put("isAnomaly", isAnomaly);
            dailyData.add(item);

            if (isAnomaly) {
                Map<String, Object> anomaly = new HashMap<>();
                anomaly.put("date", dateLabels.get(i));
                anomaly.put("amount", amount);
                anomaly.put("zScore", Math.round(zScore * 100.0) / 100.0);
                anomaly.put("type", amount.compareTo(mean) > 0 ? "突增" : "突降");
                anomalies.add(anomaly);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("mean", mean);
        result.put("stdDev", Math.round(stdDev * 100.0) / 100.0);
        result.put("threshold", threshold);
        result.put("dailyData", dailyData);
        result.put("anomalies", anomalies);
        result.put("totalAnomalies", anomalies.size());
        return Result.success(result);
    }

    @GetMapping("/login-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> getLoginLogs(@RequestParam(defaultValue = "50") Integer limit) {
        List<com.ecommerce.entity.LoginLog> logs = loginLogMapper.selectList(
                new LambdaQueryWrapper<com.ecommerce.entity.LoginLog>()
                        .orderByDesc(com.ecommerce.entity.LoginLog::getLoginTime)
                        .last("LIMIT " + limit));
        List<Map<String, Object>> result = logs.stream().map(l -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", l.getId());
            item.put("userId", l.getUserId());
            item.put("username", l.getUsername());
            item.put("role", l.getRole());
            item.put("ip", l.getIp());
            item.put("status", l.getStatus());
            item.put("failureReason", l.getFailureReason());
            item.put("loginTime", l.getLoginTime());
            return item;
        }).collect(Collectors.toList());
        return Result.success(result);
    }
}
