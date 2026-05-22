# 电商购物网站项目

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