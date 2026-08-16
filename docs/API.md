# WMS 接口文档

> 后端基础路径：`/api`（默认端口 8080）｜ AI Gateway 基础路径：`/ai`（默认端口 8090，生产经 Nginx 反代）

## 通用约定

### 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1730000000000
}
```

| code | 说明 |
|---|---|
| 200 | 成功 |
| 4001 | 验证码错误/已过期 |
| 4002 | 用户名或密码错误 |
| 4003 | 账号已禁用 |
| 401 | 未登录或 token 失效 |
| 403 | 无权限 |
| 500 | 服务器/业务错误（message 为提示信息） |

### 鉴权

- 除 `POST /api/auth/login`、`POST /api/auth/register`、`GET /api/auth/captcha`、`GET /api/alert/stats` 外，所有接口需在请求头携带 JWT：

```
Authorization: Bearer <access_token>
```

- 登录与权限校验基于 Redis 缓存（`user:perm:{userId}`），重新授权后需重新登录刷新。

### 分页请求参数

| 参数 | 类型 | 默认 | 说明 |
|---|---|---|---|
| pageNum | Integer | 1 | 页码 |
| pageSize | Integer | 10 | 每页条数 |

分页响应 `data` 结构：`{ records: [...], total: 123 }`

---

## 1. 认证模块 `/api/auth`

### 1.1 获取图形验证码

```
GET /api/auth/captcha
```

响应 `data`：

| 字段 | 类型 | 说明 |
|---|---|---|
| key | String | 验证码唯一 key，登录时回传 |
| image | String | Base64 PNG 图片，前端拼 `data:image/png;base64,` 前缀 |

> 验证码单次有效，校验后立即从 Redis 删除。

### 1.2 登录

```
POST /api/auth/login
```

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码（明文传输，生产需 HTTPS） |
| captchaKey | String | 是 | 1.1 返回的 key |
| captchaCode | String | 是 | 用户输入的验证码 |

响应 `data`（LoginVo）：`{ accessToken, tokenType: "Bearer", userInfo: {...} }`

### 1.3 注册

```
POST /api/auth/register
```

请求体：`{ username, password, email?, phone? }`

### 1.4 获取当前用户信息

```
GET /api/auth/userInfo
```

响应 `data`（UserInfoVo）：`{ id, username, nickname, email, phone, roles: [...], permissions: [...] }`

### 1.5 退出登录

```
POST /api/auth/logout
```

使当前 token 失效。

---

## 2. 货品管理 `/api/product`

### 2.1 分页查询 `product:list`

```
GET /api/product/page
```

查询参数：`productCode?`、`productName?`、`keyword?`、`category?`、`minStock?`、`maxStock?`、`status?` + 分页参数

### 2.2 新增货品 `product:add`

```
POST /api/product
```

请求体（ProductDto）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| productCode | String | 是 | 货品编码（唯一） |
| productName | String | 是 | 货品名称 |
| unit | String | 是 | 单位 |
| currentStock | Integer | 是 | 当前库存（≥0） |
| specification | String | 否 | 规格 |
| category | String | 否 | 分类 |
| imageUrl | String | 否 | 图片 |
| referenceCost / referencePrice | BigDecimal | 否 | 参考成本/售价 |
| alertMin / alertMax | Integer | 否 | 库存预警阈值 |
| status | Integer | 否 | 1 启用 / 0 停用 |
| remark | String | 否 | 备注 |

### 2.3 修改货品 `product:edit`

```
PUT /api/product
```

请求体同 2.2，必须含 `id`。修改时不会重置密码/库存等未传字段以外的数据。

### 2.4 删除货品 `product:delete`

```
DELETE /api/product/{id}
```

逻辑删除。

### 2.5 库存调整 `product:adjust`

```
POST /api/product/adjustStock
```

请求体（StockAdjustDto）：`{ productId, adjustType: "IN"|"OUT", quantity, remark? }`

### 2.6 批量库存调整 `product:adjust`

```
POST /api/product/batchAdjustStock
```

请求体（BatchStockAdjustDto）：`{ adjustType, quantity, remark?, productIds: [...] }`

---

## 3. 仓库管理 `/api/warehouse`

### 3.1 分页查询 `warehouse:list`

```
GET /api/warehouse/page
```

查询参数：`keyword?`（编码/名称模糊）、`status?` + 分页参数

### 3.2 启用仓库列表（下拉框用）`warehouse:list`

```
GET /api/warehouse/list
```

返回全部启用状态的仓库，无分页。

### 3.3 仓库详情 `warehouse:list`

```
GET /api/warehouse/{id}
```

### 3.4 新增仓库 `warehouse:add`

```
POST /api/warehouse
```

请求体（WarehouseDto）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| warehouseCode | String | 是 | 仓库编码（≤50 字符，唯一） |
| warehouseName | String | 是 | 仓库名称（≤100 字符） |
| location | String | 否 | 地址（≤255） |
| contactPerson | String | 否 | 联系人（≤50） |
| contactPhone | String | 否 | 联系电话（≤20） |
| status | Integer | 否 | 1 启用 / 0 停用 |
| remark | String | 否 | 备注 |

> 编码唯一性在应用层校验；数据库不做唯一索引（兼容逻辑删除）。

### 3.5 修改仓库 `warehouse:edit`

```
PUT /api/warehouse
```

请求体同 3.4，必须含 `id`。

### 3.6 删除仓库 `warehouse:delete`

```
DELETE /api/warehouse/{id}
```

逻辑删除。已关联出入库单的仓库禁止删除（返回业务错误）。

---

## 4. 入库管理 `/api/inbound`

### 4.1 创建入库单 `inbound:create`

```
POST /api/inbound
```

请求体（InboundOrderDto）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| warehouseId | Long | 是 | 仓库 ID |
| inboundType | String | 是 | PURCHASE 采购 / RETURN 退货 / INVENTORY 盘盈 |
| items | List | 是 | 入库明细 |
| supplier | String | 否 | 供应商 |
| relatedOrderNo | String | 否 | 关联单号 |
| remark | String | 否 | 备注 |

item（InboundOrderItemDto）：`{ productId, expectedQuantity, actualQuantity, unitPrice? }`

### 4.2 审核 `inbound:audit`

```
PUT /api/inbound/audit/{id}
```

审核通过后：订单状态 → COMPLETED，行级锁 + 乐观锁更新库存，写入 stock_log，触发库存预警检查。

### 4.3 取消 `inbound:cancel`

```
PUT /api/inbound/cancel/{id}
```

仅 PENDING 状态可取消。

### 4.4 分页查询 `inbound:query`

```
GET /api/inbound/page
```

查询参数：`orderNo?`、`status?`（PENDING/COMPLETED/CANCELLED）、`startTime?`、`endTime?`（格式 `yyyy-MM-dd HH:mm:ss`）+ 分页参数

### 4.5 入库单详情 `inbound:query`

```
GET /api/inbound/{id}
```

响应含订单头 + items（含 productCode/productName）+ warehouseName + createBy + inboundTime。

---

## 5. 出库管理 `/api/outbound`

与入库管理结构对称。

### 5.1 创建出库单 `outbound:create`

```
POST /api/outbound
```

请求体（OutboundOrderDto）：`{ warehouseId(必填), outboundType(必填: SALE 销售/RETURN 退货/INVENTORY 盘亏), customer?, relatedOrderNo?, remark?, items }`

### 5.2 审核 `outbound:audit`

```
PUT /api/outbound/audit/{id}
```

库存不足时审核失败并回滚。

### 5.3 取消 `outbound:cancel`

```
PUT /api/outbound/cancel/{id}
```

### 5.4 分页查询 `outbound:query`

```
GET /api/outbound/page
```

查询参数同入库（orderNo/status/startTime/endTime + 分页）。

### 5.5 出库单详情 `outbound:query`

```
GET /api/outbound/{id}
```

---

## 6. 库存预警 `/api/alert`

### 6.1 预警统计（无需权限）

```
GET /api/alert/stats
```

响应 `data`：`{ unhandled, belowMin, aboveMax }`

### 6.2 分页查询 `alert:query`

```
GET /api/alert/page
```

查询参数：`status?`（PENDING/PROCESSED）、`productId?`、`startTime?`、`endTime?` + 分页参数

### 6.3 处理预警 `alert:handle`

```
PUT /api/alert/handle
```

请求体（StockAlertHandleDto）：`{ alertId, handleRemark }`

> 预警自动触发时机：每次出入库审核后 + 每日 02:00 定时任务全量扫描。

---

## 7. 数据看板 `/api/dashboard`

均需 `dashboard:query` 权限。

| 接口 | 说明 | data 结构 |
|---|---|---|
| `GET /api/dashboard/today-summary` | 今日经营汇总 | TodaySummaryVo：今日入库/出库单量、总库存、库存总值等 |
| `GET /api/dashboard/last-7-days-trend` | 近7天出入库趋势 | `[{ date, inboundQuantity, outboundQuantity }]`（数值型，已补全空日期） |
| `GET /api/dashboard/alert-stats` | 预警统计 | `{ unhandled, belowMin, aboveMax }` |
| `GET /api/dashboard/top-products` | 出库货品排行 | `[{ productName, ... }]` |
| `GET /api/dashboard/warehouse-distribution` | 仓库库存分布 | `[{ warehouseName, ... }]` |

---

## 8. AI Gateway `/ai`（FastAPI，端口 8090）

生产环境经 Nginx `/ai/` 反代（SSE 已配置禁用缓冲），不直接暴露端口。

| 接口 | 方法 | 说明 |
|---|---|---|
| `/ai/chat` | POST | SSE 流式对话。请求体 `{ session_id, message }`；`message` 事件流式返回，结束时可能有 `chart_data` 事件 |
| `/ai/analysis` | POST | 同步智能分析。请求体 `{ query }`，返回 `{ insight, trace }` |
| `/ai/tools` | GET | 已注册的 Agent 工具清单 |
| `/ai/sessions/{session_id}` | GET | 查询会话历史 |
| `/ai/sessions/{session_id}` | DELETE | 清空会话 |
| `/ai/charts/trend` | GET | 近7天趋势（透传看板接口） |
| `/ai/charts/top-products` | GET | 出库排行 |
| `/ai/charts/warehouse` | GET | 仓库分布 |
| `/ai/charts/alerts` | GET | 预警统计 |
| `/ai/charts/summary` | GET | 今日摘要 |
| `/health` | GET | 健康检查 |

> `/ai/**` 接口同样需要 `Authorization: Bearer <token>`，网关转发给后端校验。

---

## 附录：权限标识清单

| 模块 | 权限标识 |
|---|---|
| 货品 | product:list / product:add / product:edit / product:delete / product:adjust |
| 仓库 | warehouse:list / warehouse:add / warehouse:edit / warehouse:delete |
| 入库 | inbound:query / inbound:create / inbound:audit / inbound:cancel |
| 出库 | outbound:query / outbound:create / outbound:audit / outbound:cancel |
| 预警 | alert:query / alert:handle |
| 看板 | dashboard:query |
