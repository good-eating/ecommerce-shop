# 电商购物网站项目

1.环境
  ┌────────────────┬──────┬──────────────────┐
  │      环境      │ 版本 │       用途       │
  ├────────────────┼──────┼──────────────────┤
  │ JDK 17         │ 17+  │ 运行后端         │
  ├────────────────┼──────┼──────────────────┤
  │ Maven          │ 3.8+ │ 编译后端         │
  ├────────────────┼──────┼──────────────────┤
  │ Node.js        │ 18+  │ 运行前端         │
  ├────────────────┼──────┼──────────────────┤
  │ Docker Desktop │ -    │ 跑 MySQL + Redis │
  └────────────────┴──────┴──────────────────┘

2.启动步骤

  第一步：启动数据库

  # 在项目目录执行（启动 MySQL 和 Redis）
  docker compose -f docker-compose.db.yml up -d

  第二步：启动后端（新开一个终端）

  cd backend
  mvn spring-boot:run

  看到 Started EcommerceShopApplication 说明启动成功。

  第三步：启动前端（再新开一个终端）

  cd frontend
  npm install
  npm run dev

  访问 http://localhost:3000 。

## 项目结构
```
shop/
├── backend/                 # 后端项目
│   ├── src/main/java/      # Java源码
│   ├── src/main/resources/ # 配置文件
│   ├── src/test/java/      # 测试代码
│   └── pom.xml            # Maven配置
├── frontend/               # 前端项目
│   ├── src/               # Vue源码
│   ├── public/            # 静态资源
│   └── package.json       # Node配置
├── database/              # 数据库脚本
├── deployment/            # 部署配置
└── docs/                  # 项目文档
```

## 技术栈
- 后端：Java 17 + Spring Boot 3.x + MyBatis-Plus + Redis
- 前端：Vue 3 + Vite + Element Plus + Pinia
- 数据库：MySQL 8.0 + Redis
- 部署：Nginx + systemd

## 核心功能
- 用户认证授权（JWT + 三角色）
- 商品管理（逻辑删除）
- 购物车（Redis+DB write-through）
- 下单支付（订单快照）
- 推荐系统（热门+item-based CF）
- 行为日志采集（AOP）
- 数据分析聚合
