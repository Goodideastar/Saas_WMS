# WMS 项目接口文档

> 项目整体架构：前端 → AI网关(8090) → Spring Boot后端(8080)

---

## 一、认证模块 `/api/auth`

所有接口**无需权限**，公开访问。

| 方法 | 路径 | 说明 | 请求体 | 返回值 |
|------|------|------|--------|--------|
| POST | `/api/auth/login` | 用户登录 | `LoginDto` (username, password, captchaKey, captchaCode) | `Result<LoginVo>` (access_token, expires_in) |
| POST | `/api/auth/register` | 用户注册 | `RegisterDto` (username, password, email) | `Result<Void>` |
| GET | `/api/auth/userInfo` | 获取当前用户信息 | Header: `Authorization: Bearer {token}` | `Result<UserInfoVo>` (用户信息 + roles + permissions) |
| POST | `/api/auth/logout` | 退出登录 | Header: `Authorization: Bearer {token}` | `Result<Void>` |

**认证流程：**
1. 登录成功返回 JWT `access_token`，前端存入 localStorage
2. 后续请求在 Header 中携带 `Authorization: Bearer <token>`
3. `JwtAuthenticationFilter` 拦截请求 → 从 Redis 加载用户权限 → 设置 SecurityContext
4. 权限缓存 key 格式：`user:perm:{userId}`

---

## 二、仪表盘模块 `/api/dashboard`

所有接口需要权限：`dashboard:query`

| 方法 | 路径 | 说明 | 参数 | 返回值 |
|------|------|------|------|--------|
| GET | `/api/dashboard/today-summary` | 今日经营摘要 | 无 | `Result<TodaySummaryVo>` |
| GET | `/api/dashboard/last-7-days-trend` | 近7天出入库趋势 | 无 | `Result<List<Map<String, Object>>>` |
| GET | `/api/dashboard/alert-stats` | 库存预警统计 | 无 | `Result<Map<String, Long>>` |
| GET | `/api/dashboard/top-products` | 货品出库排行 | 无 | `Result<List<Map<String, Object>>>` |
| GET | `/api/dashboard/warehouse-distribution` | 仓库库存分布 | 无 | `Result<List<Map<String, Object>>>` |

---

## 三、货品管理模块 `/api/product`

| 方法 | 路径 | 说明 | 权限 | 请求体/参数 | 返回值 |
|------|------|------|------|------------|--------|
| GET | `/api/product/page` | 分页查询货品 | `product:list` | Query: `ProductQueryDto` (page, size, name, code) | `Result<IPage<ProductVo>>` |
| POST | `/api/product` | 新增货品 | `product:add` | Body: `ProductDto` | `Result<Void>` |
| PUT | `/api/product` | 编辑货品 | `product:edit` | Body: `ProductDto` | `Result<Void>` |
| DELETE | `/api/product/{id}` | 删除货品 | `product:delete` | Path: `id` | `Result<Void>` |
| POST | `/api/product/adjustStock` | 调整库存 | `product:adjust` | Body: `StockAdjustDto` (productId, quantity, warehouseId, adjustType, remark) | `Result<Void>` |

> **库存调整说明**：`quantity` 正数为入库、负数为出库；`adjustType` 由前端根据 quantity 正负自动判断 `IN`/`OUT`。

---

## 四、入库单模块 `/api/inbound`

| 方法 | 路径 | 说明 | 权限 | 请求体/参数 | 返回值 |
|------|------|------|------|------------|--------|
| POST | `/api/inbound` | 创建入库单 | `inbound:create` | Body: `InboundOrderDto` | `Result<Void>` |
| PUT | `/api/inbound/audit/{id}` | 审核入库单 | `inbound:audit` | Path: `id` | `Result<Void>` |
| PUT | `/api/inbound/cancel/{id}` | 取消入库单 | `inbound:cancel` | Path: `id` | `Result<Void>` |
| GET | `/api/inbound/page` | 分页查询入库单 | `inbound:query` | Query: `InboundOrderQueryDto` (page, size, status, orderNo) | `Result<IPage<InboundOrderVo>>` |
| GET | `/api/inbound/{id}` | 入库单详情 | `inbound:query` | Path: `id` | `Result<InboundOrderVo>` |

> **审核通过后**：订单状态变为"已审核"，商品库存增加（带版本号乐观锁防超卖），写入 `stock_log` 记录，触发库存预警检查。

---

## 五、出库单模块 `/api/outbound`

| 方法 | 路径 | 说明 | 权限 | 请求体/参数 | 返回值 |
|------|------|------|------|------------|--------|
| POST | `/api/outbound` | 创建出库单 | `outbound:create` | Body: `OutboundOrderDto` | `Result<Void>` |
| PUT | `/api/outbound/audit/{id}` | 审核出库单 | `outbound:audit` | Path: `id` | `Result<Void>` |
| PUT | `/api/outbound/cancel/{id}` | 取消出库单 | `outbound:cancel` | Path: `id` | `Result<Void>` |
| GET | `/api/outbound/page` | 分页查询出库单 | `outbound:query` | Query: `OutboundOrderQueryDto` (page, size, status, orderNo) | `Result<IPage<OutboundOrderVo>>` |
| GET | `/api/outbound/{id}` | 出库单详情 | `outbound:query` | Path: `id` | `Result<OutboundOrderVo>` |

> **审核通过后**：订单状态变为"已审核"，商品库存减少（带版本号乐观锁），写入 `stock_log`，触发库存预警检查。

---

## 六、库存预警模块 `/api/alert`

| 方法 | 路径 | 说明 | 权限 | 请求体/参数 | 返回值 |
|------|------|------|------|------------|--------|
| GET | `/api/alert/stats` | 预警统计 | **公开** | 无 | `Result<Map<String, Object>>` |
| GET | `/api/alert/page` | 分页查询预警 | `alert:query` | Query: `StockAlertQueryDto` (page, size, status) | `Result<IPage<StockAlertVo>>` |
| PUT | `/api/alert/handle` | 处理预警 | `alert:handle` | Body: `StockAlertHandleDto` (id, remark) | `Result<Void>` |

> 预警检查时机：① 每次出入库审核后实时检查；② 每天凌晨2点全量定时扫描（`@Scheduled cron="0 0 2 * * ?"`）

---

## 七、权限码汇总

| 权限码 | 使用接口 |
|--------|----------|
| `dashboard:query` | Dashboard 全部5个接口 |
| `product:list` | GET /api/product/page |
| `product:add` | POST /api/product |
| `product:edit` | PUT /api/product |
| `product:delete` | DELETE /api/product/{id} |
| `product:adjust` | POST /api/product/adjustStock |
| `inbound:create` | POST /api/inbound |
| `inbound:audit` | PUT /api/inbound/audit/{id} |
| `inbound:cancel` | PUT /api/inbound/cancel/{id} |
| `inbound:query` | GET /api/inbound/page, GET /api/inbound/{id} |
| `outbound:create` | POST /api/outbound |
| `outbound:audit` | PUT /api/outbound/audit/{id} |
| `outbound:cancel` | PUT /api/outbound/cancel/{id} |
| `outbound:query` | GET /api/outbound/page, GET /api/outbound/{id} |
| `alert:query` | GET /api/alert/page |
| `alert:handle` | PUT /api/alert/handle |

---

# AI 网关接口 (端口 8090)

网关启动时注册所有路由前缀为 `/ai`（汇总接口除外）。

## 一、聊天与分析 (Agent)

| 方法 | 路径 | 说明 | 请求体 | 返回值 |
|------|------|------|--------|--------|
| POST | `/ai/chat` | AI 对话（SSE流式） | `ChatRequest` {message, session_id?} | SSE 事件流 (trace/tool/chart_data/done) |
| POST | `/ai/analysis` | AI 数据分析（非流式） | `AnalysisRequest` {query, context?} | `{insight, trace}` |

**执行流程：** plan（意图解析+生成计划） → execute（调用WMS工具获取数据） → observe（检查结果） → 条件分支 → replan（重试）或 summarize（总结输出），最多5轮循环。

## 二、图表代理 `/ai/charts`

所有接口提取 `Authorization` 头透传到后端，无请求体参数。

| 方法 | 路径 | 说明 | 代理到 |
|------|------|------|--------|
| GET | `/ai/charts/trend` | 近7天出入库趋势 | `GET /api/dashboard/last-7-days-trend` |
| GET | `/ai/charts/top-products` | 货品出库排行 | `GET /api/dashboard/top-products` |
| GET | `/ai/charts/warehouse` | 仓库库存分布 | `GET /api/dashboard/warehouse-distribution` |
| GET | `/ai/charts/alerts` | 库存预警统计 | `GET /api/alert/stats` |
| GET | `/ai/charts/summary` | 今日经营摘要 | `GET /api/dashboard/today-summary` |

## 三、会话管理

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| GET | `/ai/tools` | 获取可用工具列表 | 无 |
| GET | `/ai/sessions/{session_id}` | 获取会话历史 | Path: session_id |
| DELETE | `/ai/sessions/{session_id}` | 清空会话历史 | Path: session_id |

## 四、健康检查

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 返回 `{"status": "ok"}` |

---

# AI Agent 工具清单

Agent 通过这16个注册的工具函数调用 WMS 后端完成数据查询和操作。

## 货品工具 (3个)

| 工具名 | 类型 | 权限 | 后端接口 | 参数 |
|--------|------|------|----------|------|
| `product_search` | query | read | `GET /api/product/page` | page, size, name?, code? |
| `product_detail` | query | read | `GET /api/product/{id}` | id (必填) |
| `product_adjust_stock` | action | **write** | `POST /api/product/adjustStock` | productId, quantity, remark? |

## 入库工具 (3个)

| 工具名 | 类型 | 权限 | 后端接口 | 参数 |
|--------|------|------|----------|------|
| `inbound_search` | query | read | `GET /api/inbound/page` | page, size, status?, orderNo? |
| `inbound_detail` | query | read | `GET /api/inbound/{id}` | id (必填) |
| `inbound_audit` | action | **write** | `PUT /api/inbound/audit/{id}` | id (必填) |

## 出库工具 (3个)

| 工具名 | 类型 | 权限 | 后端接口 | 参数 |
|--------|------|------|----------|------|
| `outbound_search` | query | read | `GET /api/outbound/page` | page, size, status?, orderNo? |
| `outbound_detail` | query | read | `GET /api/outbound/{id}` | id (必填) |
| `outbound_audit` | action | **write** | `PUT /api/outbound/audit/{id}` | id (必填) |

## 仪表盘工具 (4个)

| 工具名 | 类型 | 权限 | 后端接口 | 参数 |
|--------|------|------|----------|------|
| `dashboard_summary` | query | read | `GET /api/dashboard/today-summary` | 无 |
| `dashboard_trend` | query | read | `GET /api/dashboard/last-7-days-trend` | 无 |
| `dashboard_top` | query | read | `GET /api/dashboard/top-products` | 无 |
| `dashboard_warehouse` | query | read | `GET /api/dashboard/warehouse-distribution` | 无 |

## 预警工具 (3个)

| 工具名 | 类型 | 权限 | 后端接口 | 参数 |
|--------|------|------|----------|------|
| `alert_stats` | query | read | `GET /api/alert/stats` | 无 |
| `alert_search` | query | read | `GET /api/alert/page` | page, size, status? |
| `alert_handle` | action | **write** | `PUT /api/alert/handle` | id, remark? |

> **Write 类工具**均要求LLM执行前向用户确认，system prompt已强硬约束。

---

# 统一响应格式

所有后端接口统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1718798400000
}
```

- `code=200`：成功
- `code=401`：未认证，前端跳转登录页
- `code=403`：无权限，前端跳转403页面
- `code=500`：服务端异常

---

# 接口总数统计

| 层 | 数量 |
|----|------|
| Java 后端 REST 接口 | 23 |
| Python 网关 REST 接口 | 11 |
| AI Agent 可调用工具 | 16 |
| **对外暴露端点合计** | **34** |
