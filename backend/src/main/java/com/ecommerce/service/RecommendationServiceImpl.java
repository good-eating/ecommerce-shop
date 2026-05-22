package com.ecommerce.service;

import com.alibaba.fastjson2.JSON;
import com.ecommerce.dto.ProductDTO;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.SalesRecord;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.SalesRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    private final ProductMapper productMapper;
    private final SalesRecordMapper salesRecordMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public RecommendationServiceImpl(ProductMapper productMapper, SalesRecordMapper salesRecordMapper, RedisTemplate<String, Object> redisTemplate) {
        this.productMapper = productMapper;
        this.salesRecordMapper = salesRecordMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<ProductDTO> getRecommendations(Long userId, Integer limit) {
        // 未登录用户直接使用热门商品推荐
        if (userId == null) {
            return getPopularProducts(limit);
        }

        try {
            // 检查用户是否有购买历史，没有则直接返回热门商品
            List<Long> userPurchaseHistory = getRecentUserPurchases(userId, 10);
            if (userPurchaseHistory.isEmpty()) {
                return getPopularProducts(limit);
            }

            // 1. 尝试获取个性化推荐
            List<Long> recommendedProductIds = getPersonalizedRecommendations(userId, limit, userPurchaseHistory);

            // 2. 如果个性化推荐不足，使用冷启动策略
            if (recommendedProductIds.size() < limit) {
                int needMore = limit - recommendedProductIds.size();
                List<Long> coldStartIds = getColdStartRecommendations(needMore, recommendedProductIds);
                recommendedProductIds.addAll(coldStartIds);
            }

            // 3. 获取商品详情
            return getProductDetails(recommendedProductIds);
        } catch (Exception e) {
            log.error("获取推荐失败，使用热门商品兜底", e);
            return getPopularProducts(limit);
        }
    }

    @Override
    public List<ProductDTO> getPopularProducts(Integer limit) {
        String cacheKey = "product:top:daily";

        // 尝试从缓存获取
        Set<ZSetOperations.TypedTuple<Object>> cachedProducts =
                redisTemplate.opsForZSet().reverseRangeWithScores(cacheKey, 0, limit - 1);

        if (cachedProducts != null && !cachedProducts.isEmpty()) {
            List<Long> productIds = cachedProducts.stream()
                    .map(tuple -> Long.parseLong(((String) tuple.getValue()).replace("product:", "")))
                    .collect(Collectors.toList());
            return getProductDetails(productIds);
        }

        // 从数据库获取热销商品
        List<Product> products = productMapper.selectTopProducts(limit);

        // 缓存到Redis
        for (Product product : products) {
            redisTemplate.opsForZSet().add(cacheKey,
                    "product:" + product.getId(), product.getSalesCount());
        }
        redisTemplate.expire(cacheKey, 24, TimeUnit.HOURS);

        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void computeItemSimilarity() {
        log.info("开始计算商品相似度...");

        try {
            // 1. 获取最近30天的销售记录
            LocalDateTime startTime = LocalDateTime.now().minusDays(30);
            List<SalesRecord> salesRecords = salesRecordMapper.selectRecentRecords(startTime);

            // 2. 构建共现矩阵
            Map<Long, Map<Long, Integer>> cooccurrenceMatrix = buildCooccurrenceMatrix(salesRecords);

            // 3. 计算相似度（余弦相似度）
            Map<Long, List<SimilarityItem>> similarityMap = computeCosineSimilarity(cooccurrenceMatrix);

            // 4. 存储到Redis
            storeSimilarityToRedis(similarityMap);

            log.info("商品相似度计算完成，共处理 {} 个商品", similarityMap.size());
        } catch (Exception e) {
            log.error("计算商品相似度失败", e);
        }
    }

    private List<Long> getPersonalizedRecommendations(Long userId, Integer limit, List<Long> userPurchaseHistory) {
        // 检查是否有缓存推荐
        String recoKey = "reco:user:" + userId;
        List<Object> cachedRecos = redisTemplate.opsForList().range(recoKey, 0, limit - 1);

        if (cachedRecos != null && !cachedRecos.isEmpty()) {
            return cachedRecos.stream()
                    .map(obj -> Long.parseLong(obj.toString().replace("product:", "")))
                    .collect(Collectors.toList());
        }

        // 实时计算推荐（简化版）
        Set<Long> recommendations = new LinkedHashSet<>();

        for (Long purchasedProductId : userPurchaseHistory) {
            String simKey = "item:sim:" + purchasedProductId;
            Set<ZSetOperations.TypedTuple<Object>> similarProducts =
                    redisTemplate.opsForZSet().reverseRangeWithScores(simKey, 0, 5);

            if (similarProducts != null) {
                for (ZSetOperations.TypedTuple<Object> tuple : similarProducts) {
                    Long productId = Long.parseLong(((String) tuple.getValue()).replace("product:", ""));
                    if (!userPurchaseHistory.contains(productId) && !recommendations.contains(productId)) {
                        recommendations.add(productId);
                        if (recommendations.size() >= limit) break;
                    }
                }
            }

            if (recommendations.size() >= limit) break;
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

    private List<Long> getColdStartRecommendations(Integer limit, List<Long> excludeIds) {
        // 使用热门商品作为冷启动推荐
        String cacheKey = "product:top:daily";
        Set<ZSetOperations.TypedTuple<Object>> topProducts =
                redisTemplate.opsForZSet().reverseRangeWithScores(cacheKey, 0, limit * 2);

        List<Long> recommendations = new ArrayList<>();

        if (topProducts != null) {
            for (ZSetOperations.TypedTuple<Object> tuple : topProducts) {
                Long productId = Long.parseLong(((String) tuple.getValue()).replace("product:", ""));
                if (!excludeIds.contains(productId)) {
                    recommendations.add(productId);
                    if (recommendations.size() >= limit) break;
                }
            }
        }

        // 如果热门商品不足，随机补充
        if (recommendations.size() < limit) {
            List<Product> randomProducts = productMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                            .eq(Product::getStatus, 1)
                            .last("LIMIT " + (limit - recommendations.size()))
            );

            for (Product product : randomProducts) {
                if (!excludeIds.contains(product.getId()) && !recommendations.contains(product.getId())) {
                    recommendations.add(product.getId());
                }
            }
        }

        return recommendations;
    }

    private List<Long> getRecentUserPurchases(Long userId, Integer limit) {
        List<SalesRecord> records = salesRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SalesRecord>()
                        .eq(SalesRecord::getUserId, userId)
                        .orderByDesc(SalesRecord::getCreatedAt)
                        .last("LIMIT " + limit)
        );

        return records.stream()
                .map(SalesRecord::getProductId)
                .distinct()
                .collect(Collectors.toList());
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

    private Map<Long, List<SimilarityItem>> computeCosineSimilarity(Map<Long, Map<Long, Integer>> cooccurrenceMatrix) {
        Map<Long, List<SimilarityItem>> similarityMap = new HashMap<>();

        // 计算每个商品的购买次数
        Map<Long, Integer> productCounts = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Integer>> entry : cooccurrenceMatrix.entrySet()) {
            Long productId = entry.getKey();
            Map<Long, Integer> cooccurrences = entry.getValue();

            int count = cooccurrences.values().stream().mapToInt(Integer::intValue).sum();
            productCounts.put(productId, count);
        }

        // 计算余弦相似度
        for (Map.Entry<Long, Map<Long, Integer>> entry : cooccurrenceMatrix.entrySet()) {
            Long productId = entry.getKey();
            Map<Long, Integer> cooccurrences = entry.getValue();

            List<SimilarityItem> similarities = new ArrayList<>();

            for (Map.Entry<Long, Integer> coEntry : cooccurrences.entrySet()) {
                Long otherProductId = coEntry.getKey();
                Integer coCount = coEntry.getValue();

                // 余弦相似度 = 共现次数 / sqrt(商品1购买次数 * 商品2购买次数)
                double similarity = coCount / Math.sqrt(
                        productCounts.get(productId) * productCounts.get(otherProductId));

                similarities.add(new SimilarityItem(otherProductId, similarity));
            }

            // 按相似度排序，只保留前20个
            similarities.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
            if (similarities.size() > 20) {
                similarities = similarities.subList(0, 20);
            }

            similarityMap.put(productId, similarities);
        }

        return similarityMap;
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

    private List<ProductDTO> getProductDetails(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Product> products = productMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                        .in(Product::getId, productIds)
                        .eq(Product::getStatus, 1)
        );

        // 保持推荐顺序
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<ProductDTO> result = new ArrayList<>();
        for (Long productId : productIds) {
            Product product = productMap.get(productId);
            if (product != null) {
                result.add(convertToDTO(product));
            }
        }

        return result;
    }

    private ProductDTO convertToDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .description(product.getDescription())
                .categoryId(product.getCategoryId())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .stock(product.getStock())
                .salesCount(product.getSalesCount())
                .attributesJson(product.getAttributesJson())
                .image(product.getImage())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .build();
    }

    // 内部类：相似度项
    private static class SimilarityItem {
        private final Long productId;
        private final double similarity;

        public SimilarityItem(Long productId, double similarity) {
            this.productId = productId;
            this.similarity = similarity;
        }

        public Long getProductId() { return productId; }
        public double getSimilarity() { return similarity; }
    }
}