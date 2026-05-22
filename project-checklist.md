# 电商购物网站开发清单

## 一、技术栈清单

### 后端技术栈
- **Java 17** + Spring Boot 3.x
- **MyBatis-Plus**（或 MyBatis）+ Mapper/XML
- **MySQL 8.0**（InnoDB，utf8mb4）
- **Redis**（缓存、购物车、分布式锁、推荐缓存）
- **Spring Security** + JWT（认证授权）
- **Spring Mail**、Spring AOP、Spring Scheduling、@Async
- **Druid**连接池、Lombok、Echarts（后端提供数据）

### 前端技术栈
- **Vue 3** + Vite + Element Plus
- **Pinia**（状态管理）+ vue-router
- **Axios**（带 JWT 拦截器）
- **ECharts**（可视化）

### 部署/运维
- **Nginx**反向代理 + 静态文件托管
- 阿里云 ECS / 腾讯云轻量 + systemd（或 Docker）
- 日志 & 监控：文件日志 + 建议 Prometheus/Grafana（可选）

### 辅助工具
- Git、CI（可选 GitHub Actions）
- 测试工具（JUnit、MockMvc）
- 压力测试（JMeter/Locust）

## 二、功能清单（含关键设计约束）

### 用户与权限
- [ ] 注册/登录（JWT），三角色：Customer / Sales / Admin
- [ ] 接口与方法两层权限控制（URL + @PreAuthorize）

### 商品管理
- [ ] 商品 CRUD（Admin/Sales）
- [ ] 商品支持逻辑删除（status 字段），允许物理删除前做归档
- [ ] 商品改/删 不影响历史订单：order_item 保留商品快照字段

### 购物车（一致性要求）
- [ ] 采用 Redis + 同步写 DB（write-through）
- [ ] Redis key：cart:user:{userId}（hash 或 json）
- [ ] 每次 cart 操作同时写 Redis 与 DB
- [ ] 登录/结算时强制合并并写 DB，防止跨设备冲突

### 下单与支付（模拟）
- [ ] 下单事务：校验库存 → 扣减库存 → 写 order + order_item（含快照）→ 异步发邮件
- [ ] 支付为模拟（不会集成真实支付回调）

### 日志与行为采集
- [ ] AOP 拦截 Controller，记录 behavior_log
- [ ] 异步/批量入库或写入消息队列

### 分析模块
- [ ] 用户画像（age/gender/city 分布）
- [ ] 销售趋势（日/周/月）
- [ ] 排行榜（top-products by sales/revenue）
- [ ] 异常检测（z-score 或 环比阈值报警）
- [ ] 定时任务每天/每小时生成汇总

### 推荐系统（Baseline）
- [ ] 基线：热门（popularity）+ 简化 item-based CF（离线计算）
- [ ] 离线任务：计算 item-item 相似度 → 写 Redis
- [ ] Cold-start：热门榜单 + 基于分类的 content-based 推荐

### 缓存策略
- [ ] Cache-Aside 模式为主；写后失效/更新缓存
- [ ] 推荐、排行榜为预计算并缓存
- [ ] JWT 黑名单/refresh token 存 Redis
- [ ] 分布式锁用 Redis lock:product:stock:{id}

### 异步与定时
- [ ] @Scheduled 做每日/每小时统计、刷新推荐与排行
- [ ] @Async 或 MQ 做邮件与耗时批处理

## 三、非功能/一致性约束

### 并发控制
- 下单并发：Redis 分布式锁防止超卖
- 库存扣减：DB 乐观锁或 Redis 锁
- 购物车合并：跨设备冲突处理

### 缓存失效策略
- 商品修改：失效 product:{id} 缓存
- 订单创建：失效推荐相关缓存
- 定时刷新：排行榜、推荐缓存 TTL

### 持久化策略
- 购物车：Redis + DB 双写
- 行为日志：异步批量入库
- 订单数据：事务性保证

### 安全要求
- JWT token 安全存储与刷新
- 接口权限校验（角色 + 方法级）
- 敏感数据脱敏（日志、参数）

## 四、接口 & 存储约定

### API 规范
- 统一前缀：/api/v1
- 响应格式：{ code, message, data }

### Redis Key 约定
- cart:user:{userId} -> hash
- product:{id} -> string/json
- product:top:daily -> zset
- item:sim:{itemId} -> zset
- reco:user:{userId} -> list/json
- jwt:black:{tokenId} -> string (TTL=剩余时间)

### 重要表结构
- user, role, product, product_image
- cart, cart_item, order, order_item (snapshots)
- sales_record, behavior_log, daily_summary

### order_item 快照字段
- product_id, product_name, sku
- price_at_purchase, quantity
- snapshot_json（完整商品快照）

## 五、验收标准

### 核心功能验收
- [ ] 用户注册/登录、JWT 验证
- [ ] 商品 CRUD（含逻辑删除）
- [ ] 购物车（Redis+DB 同步）
- [ ] 下单（模拟支付）并能在 order_item 中看到快照字段

### 扩展功能验收
- [ ] 行为日志：AOP 能记录并写入 behavior_log 表
- [ ] 推荐：提供 /api/v1/recommendations 返回推荐结果
- [ ] 分析：提供销售趋势和商品排行接口

### 部署验收
- [ ] 能在目标服务器上用 systemd 启动后端
- [ ] 通过 Nginx 访问前端页面
- [ ] 演示完整下单流程

### 测试验收
- [ ] 关键单元/集成测试（下单并发、购物车合并、权限拦截）
- [ ] 简单压力测试报告