# SQL 优化 — 面试专用指南

---

## 一、通用优化原则（面试官必问）

### 1. 索引优化（最重要）

| 原则 | 说明 | 反面案例 |
|------|------|----------|
| 最左前缀原则 | 联合索引 (a,b,c)，查询必须从 a 开始 | WHERE b=? AND c=? 不走索引 |
| 索引列不做运算 | 索引列不能放函数或表达式里 | WHERE YEAR(create_time)=2024 索引失效 |
| 左模糊用索引，右模糊不用 | LIKE 'abc%' 走索引，LIKE '%abc' 不走 | LIKE '%王%' 全表扫描 |
| 类型要匹配 | 字符串字段传数字会隐式转换，索引失效 | VARCHAR 字段传 INT |
| OR 条件都要有索引 | OR 连接的条件如果有一列没索引，整条不走 | WHERE a=1 OR b=2，b 没索引 |

---

### 2. SQL 写法优化

**❌ 差：SELECT \***
```sql
SELECT * FROM wms_product WHERE product_code = 'P001'
```
**✅ 好：只查需要的字段**
```sql
SELECT id, product_name, current_stock FROM wms_product WHERE product_code = 'P001'
```

**❌ 差：分页查询总数用 COUNT(*) 全表扫描**
```sql
SELECT COUNT(*) FROM wms_inbound_order WHERE status = 'PENDING'
-- 如果表有100万条数据，每次分页都要全表扫描
```
**✅ 好：先判断数据量，大数据量用估算**
```sql
-- MyBatis Plus 的 IPage 内部会分别执行 count 和 list，
-- 大数据量可以分开处理，或者用 ES 替代
```

**❌ 差：在 Java 代码中聚合数据**
```java
// DashboardServiceImpl.java 里的实际代码
List<OutboundOrder> orders = outboundOrderMapper.selectList(
    new LambdaQueryWrapper<OutboundOrder>().eq(OutboundOrder::getStatus, "COMPLETED"));
// 然后把结果在 Java 中分组统计 → 慢！
```
**✅ 好：用 SQL GROUP BY 聚合**
```sql
SELECT product_id, SUM(actual_quantity) as total_qty
FROM wms_outbound_order_item
WHERE outbound_order_id IN (#{ids})
GROUP BY product_id
ORDER BY total_qty DESC LIMIT 10
```

---

### 3. 查询架构优化

| 场景 | 方案 |
|------|------|
| 热点数据频繁查询 | Redis 缓存 |
| 复杂统计报表 | 数据仓库 / 定时汇总 |
| 模糊搜索量大 | Elasticsearch |
| 大字段频繁查询 | 拆分表，大字段单独存 |

---

## 二、WMS 项目的具体优化点（面试加分）

### 问题1：产品搜索 LIKE 全模糊（索引失效）

```java
// 当前代码：ProductServiceImpl.java L35-36
wrapper.like(StringUtils.hasText(queryDto.getProductCode()),
    Product::getProductCode, queryDto.getProductCode())
    .like(StringUtils.hasText(queryDto.getProductName()),
    Product::getProductName, queryDto.getProductName())
```

**问题：** `LIKE '%关键词%'` 全模糊，索引完全失效，数据量大时会全表扫描。

**优化方案：**
```sql
-- 方案1：给 product_code 和 product_name 建索引（虽然全模糊不走索引，
-- 但至少能减少回表）
CREATE INDEX idx_product_code ON wms_product(product_code);
CREATE INDEX idx_product_name ON wms_product(product_name);

-- 方案2：左模糊搜索（用户输入时前端限制只能左模糊）
SELECT * FROM wms_product WHERE product_code LIKE 'P001%'

-- 方案3：数据量大时接入 Elasticsearch
```

---

### 问题2：仪表盘统计查询效率低

```java
// 当前代码：DashboardServiceImpl.java L154-169
// 先查出所有已完成的出库单，再在 Java 中分组统计
List<OutboundOrder> completedOrders = outboundOrderMapper.selectList(
    new LambdaQueryWrapper<OutboundOrder>().eq(OutboundOrder::getStatus, "COMPLETED"));
// 然后在 Java 中遍历统计每个产品的出库量
```

**问题：** 数据量大时一次性加载所有出库单到内存，再分组聚合，效率低。

**优化方案：**
```sql
-- 用 SQL GROUP BY 直接在数据库聚合
SELECT oi.product_id, SUM(oi.actual_quantity) as total_qty
FROM wms_outbound_order_item oi
JOIN wms_outbound_order o ON oi.outbound_order_id = o.id
WHERE o.status = 'COMPLETED'
GROUP BY oi.product_id
ORDER BY total_qty DESC
LIMIT 10
```

---

### 问题3：库存排序查询缺索引

```java
// 当前代码：DashboardServiceImpl.java L196-199
wrapper.eq(Product::getStatus, 1)
    .orderByDesc(Product::getCurrentStock)  -- 按库存排序
```

**问题：** 没有对 current_stock 建索引，ORDER BY 时会文件排序（filesort），大数据量慢。

**优化方案：**
```sql
-- 建联合索引
CREATE INDEX idx_status_stock ON wms_product(status, current_stock DESC);
```

---

### 问题4：出入库单时间范围查询缺索引

```java
// 当前代码：InboundOrderServiceImpl.java L193-194
.ge(queryDto.getStartTime() != null, InboundOrder::getCreateTime, queryDto.getStartTime())
.le(queryDto.getEndTime() != null, InboundOrder::getCreateTime, queryDto.getEndTime())
```

**问题：** create_time 字段没有索引，时间范围查询会全表扫描。

**优化方案：**
```sql
-- 建联合索引，覆盖常用查询条件
CREATE INDEX idx_order_no_status_time ON wms_inbound_order(order_no, status, create_time);
CREATE INDEX idx_out_order_no_status_time ON wms_outbound_order(order_no, status, create_time);
```

---

### 问题5：订单号生成查询效率

```java
// 当前代码：InboundOrderServiceImpl.java L254-255
wrapper.likeRight(InboundOrder::getOrderNo, "IN" + dateStr)
    .orderByDesc(InboundOrder::getOrderNo)
    .last("LIMIT 1")
```

**问题：** LIKE RIGHT 左匹配，索引失效（需要查当天所有订单再排序取第一条）。

**优化方案：**
```sql
-- 方案1：在 order_no 字段上加索引（虽然 LIKE RIGHT 不走索引，
-- 但可以加一个日期字段做范围查询）
ALTER TABLE wms_inbound_order ADD COLUMN order_date DATE;
CREATE INDEX idx_order_date ON wms_inbound_order(order_date);

-- 方案2：用 Redis 自增生成序号，完全不用查数据库
-- INCR order-no:inbound:20260802
-- 这样完全避免了数据库查询
```

---

## 三、面试标准答案模板

### Q: 你们项目做过 SQL 优化吗？

> 做过几个优化点：
>
> **第一，索引优化。** 出入库单表加了联合索引（order_no、status、create_time），分页查询和状态筛选能走索引，避免全表扫描。
>
> **第二，SQL 聚合下推。** 原来的出库排行统计是查出所有数据然后在 Java 里分组，优化后改成 SQL 的 GROUP BY + ORDER BY + LIMIT，数据库层完成聚合，减少网络传输和内存消耗。
>
> **第三，缓存优化。** 仪表盘统计数据用 Redis 缓存 1 小时，库存变更时主动失效，避免每次刷新都查数据库。
>
> **第四，分页优化。** 大数据量分页用 MyBatis Plus 的分页插件，分页参数通过 SQL LIMIT 限制，避免加载全表数据。
>
> 目前数据量还不大（几百条），问题不明显，但如果到百万级，会考虑接入 Elasticsearch 做搜索，用 MySQL 做主存储。

---

### Q: 怎么排查慢 SQL？

> 三步：
>
> **第一步：** 开启慢查询日志，找到慢 SQL
> ```sql
> SHOW VARIABLES LIKE 'slow_query_log%';
> SET GLOBAL slow_query_log = 'ON';
> SET GLOBAL long_query_time = 1; -- 超过1秒的SQL记录
> ```
>
> **第二步：** 用 EXPLAIN 分析执行计划
> ```sql
> EXPLAIN SELECT * FROM wms_product WHERE product_name LIKE '%手机%';
> ```
> 重点关注 type（最好是 ref 或 range，避免 ALL）、key（实际使用的索引）、rows（扫描行数）、Extra（有没有 Using filesort 或 Using temporary）。
>
> **第三步：** 针对性优化
> - type=ALL → 加索引
> - Using filesort → 加排序索引或改 SQL
> - Using temporary → 避免在 Java 中分组，用 GROUP BY

---

### Q: 索引什么情况下会失效？

> 五种常见情况：
>
> 1. **对索引列做运算：** `WHERE YEAR(create_time) = 2024` → 改成 `WHERE create_time >= '2024-01-01' AND create_time < '2025-01-01'`
> 2. **模糊查询左通配符：** `LIKE '%abc'` → 索引失效，改成 `LIKE 'abc%'`
> 3. **隐式类型转换：** VARCHAR 字段传数字 → `WHERE phone = 13800138000` → 改成字符串 `'13800138000'`
> 4. **OR 条件有一列没索引：** → 给所有 OR 条件列都加索引
> 5. **不满足最左前缀：** 联合索引 (a,b,c)，查询 WHERE b=? → 改成 WHERE a=? AND b=?

---

### Q: 你们项目的表结构有什么设计问题？

> 主要两点：
>
> **1. 部分字段缺少索引。** 比如产品搜索的 product_name 字段用全模糊 LIKE，索引无效，数据量大时需要考虑 Elasticsearch。
>
> **2. 仪表盘统计查询效率不够好。** 出库排行是查出所有出库单在 Java 中聚合，优化后改成 SQL GROUP BY。
>
> 目前数据量小（几百条），这些问题不明显，但如果业务增长到百万级，需要考虑分库分表、引入搜索引擎、增加汇总表等方案。

---

## 四、速记卡片

```
索引失效5种：函数运算 / 左通配符LIKE / 隐式类型转换 / OR缺索引 / 不满足最左
慢SQL排查3步：开启慢日志 → EXPLAIN分析 → 针对性优化
聚合优化：Java分组 → SQL GROUP BY
分页优化：LIMIT offset,size，大数据量用游标分页
缓存优化：先删缓存再写DB，或先写DB再删缓存（我们用的先删）
```
