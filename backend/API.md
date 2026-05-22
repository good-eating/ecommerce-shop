# 电商后端 API 文档

## 基本信息

- **Base URL**: `/api/v1`
- **端口**: 8080
- **响应格式**:

```json
// 成功响应
{ "code": 200, "message": "success", "data": { ... } }

// 错误响应
{ "code": 500, "message": "错误信息", "data": null }
```

- **分页响应** (data 字段内):

```json
{ "total": 100, "items": [...], "page": 1, "size": 10 }
```

- **认证方式**: JWT Bearer Token，在请求头中携带 `Authorization: Bearer <token>`
- **角色体系**:
  - `ADMIN` — 管理员，最高权限
  - `SALES` — 销售员，可管理商品、类目、订单发货
  - 普通用户 — 已认证用户，可管理购物车和订单

---

## 目录

1. [认证 Auth](#1-认证-auth)
2. [商品 Products](#2-商品-products)
3. [分类 Categories](#3-分类-categories)
4. [购物车 Cart](#4-购物车-cart)
5. [订单 Orders](#5-订单-orders)
6. [推荐 Recommendations](#6-推荐-recommendations)
7. [后台管理 - 用户 Admin Users](#7-后台管理---用户-admin-users)
8. [后台管理 - 订单管理 Sales Orders](#8-后台管理---订单管理-sales-orders)
9. [后台管理 - 运营监控 Sales Monitor](#9-后台管理---运营监控-sales-monitor)
10. [后台管理 - 数据分析 Analytics](#10-后台管理---数据分析-analytics)
11. [后台管理 - 操作日志 Operation Logs](#11-后台管理---操作日志-operation-logs)
12. [行为日志 Behavior Logs](#12-行为日志-behavior-logs)
13. [文件上传 File Upload](#13-文件上传-file-upload)

---

## 1. 认证 Auth

**前缀**: `/auth`

### POST `/auth/login`

用户登录。

**Request Body**:

```json
{
  "username": "string (必填)",
  "password": "string (必填)"
}
```

**Response `data`**:

```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": 1,
    "username": "string",
    "email": "string",
    "phone": "string",
    "age": 0,
    "gender": 0,
    "city": "string",
    "avatar": "string",
    "role": "string",
    "createdAt": "2024-01-01 00:00:00"
  }
}
```

### POST `/auth/register`

用户注册。

**Request Body**:

```json
{
  "username": "string (必填, 3-50字符)",
  "password": "string (必填, 至少6位)",
  "email": "string (必填, 邮箱格式)",
  "phone": "string (选填)",
  "age": 0,
  "gender": 0,
  "city": "string"
}
```

**Response**: 同登录返回的 `AuthResponse`。

### POST `/auth/logout`

**Headers**: `Authorization: Bearer <token>`

退出登录，使当前 token 失效。

### POST `/auth/refresh`

**Headers**: `Authorization: Bearer <token>`

刷新 token。

**Response**: 返回新的 `AuthResponse`。

### GET `/auth/profile`

**权限**: 已认证用户。

获取当前用户个人信息。

**Response `data`**: `UserDTO` 对象。

### PUT `/auth/profile`

**权限**: 已认证用户。

更新个人信息。

**Request Body**: `UserDTO` (部分字段)。

**Response `data`**: 更新后的 `UserDTO`。

---

## 2. 商品 Products

**前缀**: `/products`

### GET `/products`

获取商品列表（公开）。

**Query Parameters**:

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | int | 1 | 页码 |
| size | int | 10 | 每页条数 |
| categoryId | long | - | 分类筛选 |
| keyword | string | - | 关键词搜索 |
| sort | string | - | 排序方式 |

**Response `data`**: `PageResult<ProductDTO>`。

### GET `/products/admin/all`

**权限**: ADMIN, SALES。

获取所有商品列表（含下架商品）。

**Query Parameters**: page, size。

### GET `/products/{id}`

获取商品详情（公开）。

### POST `/products`

**权限**: ADMIN, SALES。

创建商品。

**Request Body**:

```json
{
  "name": "string",
  "sku": "string",
  "description": "string",
  "categoryId": 1,
  "price": 99.99,
  "originalPrice": 129.99,
  "stock": 100,
  "salesCount": 0,
  "attributesJson": "string",
  "image": "string (URL)",
  "status": 1
}
```

### PUT `/products/{id}`

**权限**: ADMIN, SALES。

更新商品信息。

### DELETE `/products/{id}`

**权限**: ADMIN, SALES。

删除商品。

### PATCH `/products/{id}/status`

**权限**: ADMIN, SALES。

更新商品上下架状态。

**Request Body**:

```json
{ "status": 1 }
```

### GET `/products/top`

获取销量前十商品（公开）。

**Query Parameters**: limit (默认10)。

---

## 3. 分类 Categories

**前缀**: `/categories`

### GET `/categories`

获取全部分类列表（公开）。

**Response `data`**:

```json
[
  {
    "id": 1,
    "name": "分类名",
    "description": "描述",
    "sortOrder": 0,
    "status": 1,
    "createdAt": "2024-01-01 00:00:00"
  }
]
```

### GET `/categories/{id}`

获取分类详情。

### POST `/categories`

**权限**: SALES。

创建分类。

### PUT `/categories/{id}`

**权限**: SALES。

更新分类。

### DELETE `/categories/{id}`

**权限**: SALES。

删除分类。

---

## 4. 购物车 Cart

**前缀**: `/cart`  
**权限**: 所有接口需登录。

### GET `/cart`

获取当前用户的购物车。

**Response `data`**:

```json
{
  "id": 1,
  "totalItems": 3,
  "totalAmount": 299.97,
  "items": [
    {
      "id": 1,
      "productId": 1,
      "productName": "商品名",
      "productImage": "URL",
      "quantity": 2,
      "price": 99.99,
      "subtotal": 199.98
    }
  ]
}
```

### POST `/cart/items`

添加商品到购物车。

**Query Parameters**:

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| productId | long | - | 商品ID |
| quantity | int | 1 | 数量 |

### PUT `/cart/items/{cartItemId}`

更新购物车项数量。

**Query Parameters**: quantity (int)。

### DELETE `/cart/items/{cartItemId}`

删除购物车项。

### DELETE `/cart`

清空购物车。

### POST `/cart/merge`

登录后合并本地购物车数据。

**Request Body**: `CartItemDTO[]` (从本地存储传入的购物车项列表)。

---

## 5. 订单 Orders

**前缀**: `/orders`  
**权限**: 所有接口需登录。

### POST `/orders`

创建订单。

**Request Body**:

```json
{
  "shippingAddress": "收货地址 (必填)",
  "remark": "备注 (选填)",
  "paymentMethod": "支付方式 (必填, 如: ALIPAY, WECHAT)"
}
```

### GET `/orders`

获取当前用户订单列表。

**Query Parameters**: page, size。

### GET `/orders/{orderId}`

获取订单详情。

**Response `data`**:

```json
{
  "id": 1,
  "orderNo": "ORD202401010001",
  "userId": 1,
  "totalAmount": 299.97,
  "discountAmount": 0,
  "payAmount": 299.97,
  "status": 0,
  "statusText": "待付款",
  "paymentMethod": "ALIPAY",
  "paymentTime": null,
  "shippingAddress": "收货地址",
  "remark": "备注",
  "receivedTime": null,
  "createdAt": "2024-01-01 00:00:00",
  "items": [
    {
      "id": 1,
      "productId": 1,
      "productName": "商品名",
      "sku": "SKU001",
      "priceAtPurchase": 99.99,
      "quantity": 2,
      "subtotal": 199.98,
      "snapshotJson": "{}"
    }
  ]
}
```

**订单状态**:

| status | 说明 |
|--------|------|
| 0 | 待付款 |
| 1 | 已付款/待发货 |
| 2 | 已发货 |
| 3 | 已完成 |
| 4 | 已取消 |

### POST `/orders/{orderId}/cancel`

取消订单。

### POST `/orders/{orderId}/pay`

支付订单。

### POST `/orders/{orderId}/confirm-receipt`

确认收货。

---

## 6. 推荐 Recommendations

**前缀**: `/recommendations`

### GET `/recommendations`

获取个性化推荐商品（公开接口，登录用户可获得个性化推荐）。

**Query Parameters**: limit (默认10)。

### GET `/recommendations/popular`

获取热门商品（公开）。

**Query Parameters**: limit (默认10)。

---

## 7. 后台管理 - 用户 Admin Users

**前缀**: `/admin/users`  
**权限**: ADMIN。

### GET `/admin/users`

获取用户列表。

**Query Parameters**:

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | int | 1 | 页码 |
| size | int | 10 | 每页条数 |
| roleCode | string | - | 按角色筛选 (如 ADMIN, SALES) |

**Response `data`**: `PageResult<User>` (密码字段返回 null)。

### GET `/admin/users/roles`

获取所有角色列表。

### GET `/admin/users/{id}`

获取用户详情。

### GET `/admin/users/{id}/roles`

获取用户角色列表。

### POST `/admin/users`

创建用户。

**Request Body**:

```json
{
  "username": "string (必填)",
  "password": "string (必填)",
  "email": "string",
  "phone": "string",
  "status": 1
}
```

### PUT `/admin/users/{id}`

更新用户信息。

### PUT `/admin/users/{id}/reset-password`

重置用户密码。

**Request Body**:

```json
{ "password": "新密码" }
```

### DELETE `/admin/users/{id}`

禁用用户（软删除，设置 status=0）。

### POST `/admin/users/{id}/roles`

分配用户角色。

**Request Body**: `[roleId1, roleId2]` (角色ID数组)。

---

## 8. 后台管理 - 订单管理 Sales Orders

**前缀**: `/sales/orders`  
**权限**: SALES。

### GET `/sales/orders/paid`

获取已付款订单列表。

**Query Parameters**: page, size。

### GET `/sales/orders/shipped`

获取已发货订单列表。

**Query Parameters**: page, size。

### POST `/sales/orders/{orderId}/ship`

订单发货（更新状态为已发货）。

---

## 9. 后台管理 - 运营监控 Sales Monitor

**前缀**: `/sales-monitor`  
**权限**: SALES。

### GET `/sales-monitor/stats`

获取运营统计概览。

**Response `data`**:

```json
{
  "totalProducts": 100,
  "totalSales": 500,
  "lowStockCount": 5,
  "outOfStockCount": 2
}
```

### GET `/sales-monitor/low-stock-products`

获取低库存商品列表（库存 < 50，最多20条）。

### GET `/sales-monitor/recent-orders`

获取最近20条订单。

### GET `/sales-monitor/all-products`

获取全部商品（含销量排序）。

---

## 10. 后台管理 - 数据分析 Analytics

**前缀**: `/admin/analytics`  
**权限**: ADMIN。

### GET `/admin/analytics/stats`

获取核心数据统计。

**Response `data`**:

```json
{
  "todayOrders": 15,
  "todaySales": 2999.50,
  "totalUsers": 200,
  "totalProducts": 100
}
```

### GET `/admin/analytics/today-overview`

获取今日运营概览（含今日新用户、今日已付款订单等）。

### GET `/admin/analytics/top-products`

获取销量排行商品。

**Query Parameters**: limit (默认10)。

### GET `/admin/analytics/sales-trend`

获取销售趋势。

**Query Parameters**: days (默认7，分析最近N天)。

**Response `data`**:

```json
[
  { "date": "2024-01-01", "amount": 1000.00, "percentage": 50 }
]
```

### GET `/admin/analytics/sales-by-category`

获取分类销售分析（按分类统计商品数、总销量、总库存）。

### GET `/admin/analytics/order-status-distribution`

获取订单状态分布。

**Response `data`**:

```json
{
  "unpaid": 10,
  "paid": 20,
  "shipped": 15,
  "completed": 100,
  "cancelled": 5
}
```

### GET `/admin/analytics/sales-performance`

获取销售人员业绩排行。

### GET `/admin/analytics/user-list`

获取用户列表（ID、用户名、邮箱、手机、状态、创建时间）。

### GET `/admin/analytics/product-list`

获取商品列表（ID、名称、SKU、价格、库存、销量、状态）。

### GET `/admin/analytics/user-profile-city`

获取用户城市分布。

### GET `/admin/analytics/user-purchasing-power`

获取用户消费能力分析（高/中/低/未消费分布）。

### GET `/admin/analytics/user-category-preference`

获取用户类目偏好。

### GET `/admin/analytics/sales-prediction`

获取销售预测（基于移动平均法）。

**Query Parameters**: days (默认7)。

**Response `data`**:

```json
{
  "history": [{ "date": "...", "amount": 0, "type": "history" }],
  "predictions": [{ "date": "...", "amount": 0, "type": "prediction" }]
}
```

### GET `/admin/analytics/sales-anomaly-detection`

获取销售异常检测（基于 Z-Score 算法）。

**Query Parameters**: days (默认30)。

**Response `data`**:

```json
{
  "mean": 1000.00,
  "stdDev": 200.50,
  "threshold": 2.0,
  "dailyData": [{ "date": "...", "amount": 0, "zScore": 0.5, "isAnomaly": false }],
  "anomalies": [{ "date": "...", "amount": 0, "zScore": 3.2, "type": "突增" }],
  "totalAnomalies": 1
}
```

### GET `/admin/analytics/login-logs`

获取登录日志。

**Query Parameters**: limit (默认50)。

---

## 11. 后台管理 - 操作日志 Operation Logs

**前缀**: `/admin/operation-logs`  
**权限**: ADMIN, SALES。

### GET `/admin/operation-logs`

获取操作日志。

**Query Parameters**:

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| username | string | - | 按用户名筛选 |
| operation | string | - | 按操作类型筛选 |
| resource | string | - | 按资源名称筛选 |
| limit | int | 100 | 返回条数上限 |

---

## 12. 行为日志 Behavior Logs

**前缀**: `/logs`  
**权限**: SALES。

### GET `/logs`

获取用户行为日志（分页）。

**Query Parameters**:

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | int | 1 | 页码 |
| size | int | 20 | 每页条数 |
| userId | long | - | 按用户筛选 |
| path | string | - | 按请求路径筛选 |

---

## 13. 文件上传 File Upload

**前缀**: `/upload`  
**权限**: ADMIN, SALES。

### POST `/upload/image`

上传图片文件。

**Request**: `multipart/form-data`，字段名 `file`。

**支持的格式**: JPG, JPEG, PNG, GIF  
**最大文件大小**: 10MB

**Response `data`**:

```json
"string (文件访问URL)"
```

文件可通过 `/uploads/{filename}` 直接访问。

---

## 数据模型

### ProductDTO

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 商品ID |
| name | String | 商品名称 |
| sku | String | SKU编码 |
| description | String | 商品描述 |
| categoryId | Long | 分类ID |
| price | BigDecimal | 售价 |
| originalPrice | BigDecimal | 原价 |
| stock | Integer | 库存 |
| salesCount | Integer | 销量 |
| attributesJson | String | 属性JSON |
| image | String | 商品图片URL |
| status | Integer | 状态(1上架/0下架) |
| createdAt | LocalDateTime | 创建时间 |

### UserDTO

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户ID |
| username | String | 用户名 |
| email | String | 邮箱 |
| phone | String | 手机号 |
| age | Integer | 年龄 |
| gender | Integer | 性别 |
| city | String | 城市 |
| avatar | String | 头像URL |
| role | String | 角色 |
| createdAt | LocalDateTime | 创建时间 |
