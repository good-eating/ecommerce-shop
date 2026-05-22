# 电商项目部署指南 — 阿里云服务器

- 公网 IP：39.96.40.38
- 技术栈：Spring Boot + Vue.js + MySQL + Redis + Nginx
- 部署方式：Docker Compose

---

## 一、阿里云安全组配置

登录阿里云控制台 → 云服务器 ECS → 网络与安全 → 安全组，**添加入方向规则**放行以下端口：

| 端口 | 用途 | 协议 |
|------|------|------|
| 22 | SSH | TCP |
| 80 | HTTP（Nginx 对外服务） | TCP |
| 443 | HTTPS（可选） | TCP |

---

## 二、服务器上安装 Docker

SSH 连接服务器后执行：

```bash
ssh root@39.96.40.38

# 安装 Docker
curl -fsSL https://get.docker.com | bash -s docker

# 安装 Docker Compose 插件
apt update && apt install docker-compose-plugin -y

# 验证安装
docker --version
docker compose version
```

---

## 三、本地构建前端

在 **Windows 开发机** 上执行：

```bash
cd D:\study\junior2\shop\frontend
npm install
npm run build
```

构建成功后，`frontend/dist/` 目录会生成生产环境静态文件。

> 如果 `node_modules` 已存在，可直接执行 `npm run build`。

---

## 四、上传整个项目到服务器

需要上传**整个项目目录**（不只是 dist），因为 `docker-compose.prod.yml` 依赖以下文件和目录：

| 文件/目录 | 用途 |
|-----------|------|
| `docker-compose.prod.yml` | 生产环境容器编排 |
| `docker-compose.yml` | 开发环境编排（可选） |
| `docker-compose.db.yml` | 数据库编排（可选） |
| `backend/Dockerfile` | 构建后端镜像 |
| `backend/pom.xml` | Maven 依赖配置 |
| `backend/src/` | 后端源码 |
| `frontend/dist/` | 前端构建产物（由第3步生成） |
| `deploy/nginx.conf` | Nginx 反向代理配置 |
| `database/ddl.sql` | 数据库表结构初始化 |
| `database/init-data.sql` | 数据库初始数据 |

### 方法一：SCP 上传（推荐）

```bash
# 在 Windows PowerShell 中执行
scp -r D:\study\junior2\shop root@39.96.40.38:/root/
```

### 方法二：压缩后上传（大文件更快）

```bash
# Windows 上先压缩（需要安装 tar 或使用 7z/WinRAR）
cd D:\study\junior2\shop
tar -czf shop.tar.gz *

# 上传压缩包
scp D:\study\junior2\shop.tar.gz root@39.96.40.38:/root/

# 服务器上解压
ssh root@39.96.40.38
cd /root
tar -xzf shop.tar.gz -C shop
```

### 方法三：Git 推送

如果项目关联了 Git 仓库，也可直接在服务器上 `git clone`。

---

## 五、服务器上启动服务

```bash
ssh root@39.96.40.38
cd /root/shop

# 启动所有容器（后台运行）
docker compose -f docker-compose.prod.yml up -d

# 查看运行状态
docker compose -f docker-compose.prod.yml ps
```

首次启动会自动完成以下操作：

| 服务 | 说明 | 启动耗时（首次） |
|------|------|------------------|
| MySQL | 创建数据库并初始化表结构和数据 | 约 1-2 分钟 |
| Redis | 启动缓存服务 | 约 10 秒 |
| Backend | 编译源码并启动 Spring Boot | 约 3-5 分钟 |
| Nginx | 代理前端静态文件和 API 请求 | 约 10 秒 |

---

## 六、验证部署

在浏览器中访问以下地址：

| 地址 | 预期结果 |
|------|----------|
| `http://39.96.40.38` | 前端页面正常显示 |
| `http://39.96.40.38/api/swagger-ui.html` | Swagger API 文档（如已配置） |

### 查看日志排查问题

```bash
# 查看所有服务日志
docker compose -f docker-compose.prod.yml logs -f

# 查看指定服务日志
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs -f nginx
docker compose -f docker-compose.prod.yml logs -f mysql

# 查看容器是否正常运行
docker compose -f docker-compose.prod.yml ps
```

---

## 七、日常管理命令

```bash
# 停止所有服务
docker compose -f docker-compose.prod.yml down

# 重启某个服务
docker compose -f docker-compose.prod.yml restart backend

# 重新构建并启动（修改了后端代码后）
docker compose -f docker-compose.prod.yml up -d --build backend

# 进入容器内部
docker exec -it ecommerce-backend sh

# 查看磁盘占用
docker system df
```

---

## 八、常见问题

### Q1：端口被占用？
```bash
# 检查端口占用
lsof -i :80
lsof -i :8080
# 停止占用端口的进程或修改 docker-compose 中的映射端口
```

### Q2：后端启动失败？
```bash
# 查看详细错误日志
docker compose -f docker-compose.prod.yml logs backend
# 常见原因：MySQL 未就绪（healthcheck 会自动等待）、数据库连接错误
```

### Q3：前端页面白屏/接口 502 ？
- 确认 `frontend/dist/` 目录存在且有内容
- 确认后端容器已成功启动
- 检查 nginx.conf 代理配置是否正确

### Q4：部署后更新代码？
```bash
# 1. 本地修改代码
# 2. 本地重新构建前端
cd frontend && npm run build
# 3. 上传项目到服务器（或用 git pull）
# 4. 重新构建并启动
docker compose -f docker-compose.prod.yml up -d --build
```

---

## 九、Nginx 配置说明

关键配置项（`deploy/nginx.conf`）：

- **`server_name 39.96.40.38;`** — 绑定的域名/IP
- **`location /api/`** — 反向代理到后端 Spring Boot（`http://backend:8080/api/v1/`）
- **`location /uploads/`** — 代理文件上传访问路径
- **`location /`** — 前端 SPA 路由，所有非文件请求指向 `index.html`
- **`location /assets/`** — 静态资源缓存策略（一年）
