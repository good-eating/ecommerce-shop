# 电商购物网站 - 整体提示词（主Prompt）

**角色设定**：请以项目经理/资深工程师角色，基于以下技术栈与约定，为我生成后端和前端的详细设计文档、必要的代码骨架（关键类/配置/Mapper/SQL DDL/接口示例）以及部署脚本。注意把所有关键约定落实到代码/配置中并给出测试/验证步骤。不要直接输出过多不必要的实现细节，先输出设计与清单，随后在我确认后逐步输出代码。

## 项目概述与目标
实现一个电子商务购物网站（后端 Java 17 + Spring Boot 3.x，前端 Vue3 + Vite + Element Plus）。三个角色：Customer、Sales、Admin。使用 JWT 做认证授权。

## 关键业务约定
1. **订单不可变**：所有订单条目（order_item）必须保存商品快照字段：product_id, product_name, sku, price_at_purchase, quantity, snapshot_json
2. **商品逻辑删除**：product 表支持逻辑删除（status），允许物理删除前进行归档，但删除或修改 product 不应影响历史订单可读性
3. **购物车 write-through**：采用 Redis + 同步写 DB（write-through）：每次 cart 操作同时写 Redis 与 DB；登录/结算时强制做一次 DB 同步/合并
4. **推荐系统基线**：热门（popularity）+ 简化 item-based CF，离线计算相似度并把结果缓存到 Redis，cold-start 使用热门与分类推荐
5. **行为采集**：使用 Spring AOP（Controller 层）写入 behavior_log（异步/批量）
6. **分析聚合**：使用 MySQL 聚合查询 + 每日/每小时汇总表（daily_summary），并缓存热点结果到 Redis
7. **异步处理**：异步邮件使用 Spring Mail + @Async（或 MQ），定时任务使用 @Scheduled（单机部署需注意多实例分布式调度）
8. **缓存策略**：Cache-Aside，为热榜/推荐/商品详情设 TTL，写操作后失效缓存；分布式锁用 Redis 防止超卖

## 技术栈（简要列出依赖）
**后端**：Spring Boot, Spring Security (JWT), MyBatis-Plus, Spring Data Redis, Spring Mail, Druid, Lombok, Spring AOP, Spring Scheduling
**前端**：Vue 3, Vite, Element Plus, Pinia, Axios, ECharts
**DB/缓存**：MySQL 8.0, Redis
**部署**：Nginx, systemd 或 Docker（可选）

## 请输出（优先级顺序）

### 1. 数据库核心表 DDL
至少包含以下表结构，并给出重要索引建议：
- user, role（用户与角色）
- product, product_image（商品与图片，支持逻辑删除）
- cart, cart_item（购物车）
- order, order_item（订单与订单项，含快照字段）
- sales_record（销售记录，用于推荐）
- behavior_log（行为日志）
- daily_summary（每日汇总）

### 2. 后端关键配置示例
- application.yml（MySQL/Redis/JWT/Mail profiles）
- Spring Security 核心配置（Filter 链、JWT 验证、角色权限示例）
- Redis key 约定与 TTL 建议

### 3. 下单事务伪代码 + MyBatis Mapper 样例
确保在事务中：
- 读取商品、锁定库存
- 写 order + order_item 快照
- 写 sales_record
- 异步发邮件

### 4. 购物车同步逻辑说明
- Controller -> Service -> Redis + DB 同步策略
- 示例 Redis key 与 DB DDL
- 合并策略（跨设备）

### 5. 推荐模块架构说明
- 离线计算步骤（输入表、计算过程、输出到 Redis 的 schema）
- 在线召回流程与 API 约定

### 6. 行为采集 AOP 设计
- 切点、脱敏、异步入库、批量写入示例

### 7. 常见坑与防御清单
- 并发下单、缓存一致性、日志存储增长、推荐冷启动等
- 对应的解决建议

### 8. 部署步骤与验证脚本
- 示例 Nginx + systemd 配置片段
- 最小化的验证脚本（如何验证下单/推荐/分析接口工作）

### 9. 逐步实施计划（8周为例）
列出每周的里程碑与验收点

## 额外要求（输出风格）
1. 输出时先给"设计与清单"，然后在我确认后再展开每个模块的可运行代码
2. 对每段关键 SQL/业务逻辑给出测试步骤
3. 代码示例需注重可读性并标注边界条件处理
4. 若某处需要权衡（例如购物车同步策略的强一致 vs 性能），请列出两种实现与优缺点并推荐默认方案

---

## 验收标准（用于任务完成判定）
1. 能完成：用户注册/登录、JWT 验证、商品 CRUD（含逻辑删除）、购物车（Redis+DB 同步）、下单（模拟支付）并能在 order_item 中看到快照字段
2. 行为日志：AOP 能记录并写入 behavior_log 表
3. 推荐：提供 /api/v1/recommendations?userId=&limit= 返回推荐结果（优先从 Redis 缓存）
4. 分析：提供 /api/v1/analytics/sales-trend、/api/v1/analytics/top-products 并返回合理聚合数据
5. 部署：能在目标服务器上用 systemd 启动后端并通过 Nginx 访问前端页面，演示完整下单流程
6. 测试：包含若干关键单元/集成测试（下单并发、购物车合并、权限拦截）和一次简单压力测试报告

## 重要接口/数据约定
- 统一 API 前缀：/api/v1
- 统一响应格式：{ code, message, data }
- Redis key 约定：cart:user:{userId}, product:{id}, product:top:daily, item:sim:{itemId}, reco:user:{userId}, jwt:black:{tokenId}

---

**请按照以上要求，先输出整体设计与技术清单，等待我确认后再逐步展开具体实现代码。**