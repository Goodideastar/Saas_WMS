# Saas_WMS — 仓储管理系统

全栈 SaaS WMS：Spring Boot 3.2 (Java 21) + Vue 3 / Element Plus + MySQL 8.0 + Redis + FastAPI AI Gateway。

## 功能

- 数据看板：今日经营汇总、近7天出入库趋势、预警统计、货品排行、仓库分布（ECharts）
- 货品管理：CRUD、单个/批量库存调整、库存预警阈值
- 仓库管理：CRUD、编码唯一校验、被出入库单引用禁止删除
- 入库/出库管理：建单、审核（行级锁 + 乐观锁防超卖）、取消、详情（含明细/仓库/创建人）
- 库存预警：出入库触发 + 每日 02:00 定时全量扫描、处理闭环
- AI 分析：SSE 流式对话、自然语言查数据（LLM Agent 调用 WMS 接口）
- 权限：JWT + RBAC 按钮级控制（`@PreAuthorize` / `v-permission`）、图形验证码单次有效

## 快速开始

```bash
cp .env.example .env     # 填写 LLM_API_KEY
docker compose up -d     # 访问 http://localhost  admin / admin123
```

## 文档

| 文档 | 内容 |
|---|---|
| [docs/API.md](docs/API.md) | 接口文档：后端 REST API + AI Gateway，含统一响应格式、错误码、权限标识清单 |
| [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) | 部署文档：架构图、部署文件清单、开发/生产部署流程、健康检查与排障 |

## 本地开发

```bash
docker compose up -d mysql redis   # 仅基础设施
mvn spring-boot:run                # 后端 :8080
cd frontend && npm run dev         # 前端 :3000（代理 /api → 8080）
```

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.2 / Java 21 / MyBatis / Spring Security + JWT / Redisson |
| 前端 | Vue 3 / Vite / Element Plus / Pinia / ECharts |
| AI 网关 | FastAPI / SSE / LLM Agent（qwen / deepseek） |
| 基础设施 | MySQL 8.0 / Redis 7 / Nginx / Docker Compose |
