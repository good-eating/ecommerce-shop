# Redis Key 约定与缓存策略

## Redis Key 命名规范
- 使用冒号分隔层级：`prefix:entity:id:field`
- 所有key统一小写
- 使用有意义的命名，避免缩写

## 核心业务Key定义

### 1. 用户相关
```redis
# 用户信息缓存
user:info:{userId} -> hash (TTL: 30分钟)
    - username, email, phone, avatar, etc.

# JWT黑名单
jwt:black:{tokenId} -> string (TTL: token剩余时间)

# 用户刷新token
jwt:refresh:{userId} -> string (TTL: 7天)
```

### 2. 购物车相关
```redis
# 用户购物车（hash结构）
cart:user:{userId} -> hash (TTL: 30天)
    - field: product:{productId}
    - value: {"quantity": 2, "price": 99.99, "added_at": timestamp}

# 购物车锁（防止并发操作）
lock:cart:user:{userId} -> string (TTL: 10秒)
```

### 3. 商品相关
```redis
# 商品详情缓存
product:detail:{productId} -> string (TTL: 1小时)
    - JSON格式商品详情

# 商品列表缓存（分页）
product:list:category:{categoryId}:page:{page}:size:{size} -> string (TTL: 30分钟)

# 商品搜索缓存
product:search:{keyword}:page:{page}:size:{size} -> string (TTL: 15分钟)
```

### 4. 排行榜与推荐
```redis
# 每日热销商品排行榜
product:top:daily -> zset (TTL: 24小时)
    - member: product:{productId}
    - score: 销量

# 每周热销商品排行榜
product:top:weekly -> zset (TTL: 7天)

# 商品相似度（item-based CF）
item:sim:{itemId} -> zset (TTL: 7天)
    - member: product:{similarProductId}
    - score: 相似度分数

# 用户个性化推荐
reco:user:{userId} -> list (TTL: 1天)
    - 推荐商品ID列表

# 新用户冷启动推荐
reco:cold:start -> list (TTL: 1小时)
    - 热门商品ID列表
```

### 5. 分布式锁
```redis
# 商品库存锁（防止超卖）
lock:product:stock:{productId} -> string (TTL: 5秒)

# 订单创建锁
lock:order:create:{userId} -> string (TTL: 10秒)

# 推荐计算锁
lock:reco:compute -> string (TTL: 1分钟)
```

### 6. 统计与分析
```redis
# 实时在线用户数
stats:online:users -> set (TTL: 5分钟)

# 页面访问统计
stats:page:view:{path} -> counter (TTL: 1小时)

# 商品浏览量
stats:product:view:{productId} -> counter (TTL: 24小时)

# 销售趋势缓存
stats:sales:trend:{type}:{date} -> string (TTL: 1小时)
    - type: daily/weekly/monthly
```

## TTL策略

### 短时效缓存（分钟级）
- 分布式锁：5-30秒
- 搜索缓存：15分钟
- 页面统计：1小时

### 中时效缓存（小时级）
- 商品详情：1小时
- 商品列表：30分钟
- 用户信息：30分钟

### 长时效缓存（天级）
- 排行榜：1-7天
- 推荐结果：1天
- 购物车：30天
- 刷新token：7天

## 缓存失效策略

### 写后失效
- 商品修改：失效 `product:detail:*` 和 `product:list:*`
- 订单创建：失效推荐相关缓存
- 用户信息更新：失效 `user:info:*`

### 定时刷新
- 排行榜：每日凌晨刷新
- 推荐系统：每小时刷新
- 统计缓存：按需刷新

### 手动刷新
- 管理员操作后手动刷新相关缓存
- 缓存预热机制

## 缓存穿透防御
- 空值缓存：`null`值设置短TTL（2-5分钟）
- 布隆过滤器：用于商品ID存在性校验
- 参数校验：前端+后端双重校验

## 缓存雪崩防御
- 随机TTL：在基础TTL上增加随机时间
- 多级缓存：本地缓存+Redis缓存
- 热点数据永不过期：手动更新策略