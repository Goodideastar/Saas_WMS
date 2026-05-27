# WMS AI网关改造设计文档

## 概述

为WMS仓储管理系统构建Python AI网关层，实现全链路AI Agent能力：自然语言查询、智能分析、自动化决策与执行。采用独立FastAPI服务 + 状态机Agent引擎 + 国产大模型的架构。

## 整体架构

```
Vue前端 ←→ Python FastAPI AI Gateway (:8090) ←→ WMS Java Backend (:8080)
                  ↕ OpenAI兼容API
             通义千问 / DeepSeek
```

三层设计：
- **接入层**：Chat API（SSE流式对话）+ Analysis API（深度分析报告）
- **引擎层**：Agent Engine状态机 + Tool Registry工具注册中心
- **适配层**：LLM Provider抽象，OpenAI兼容接口统一调用

## Agent Engine 状态机

```
PLAN → EXECUTE → OBSERVE → REPLAN → DONE
  ↑                            │
  └────────────────────────────┘
```

| 状态 | 职责 |
|------|------|
| PLAN | LLM分析意图，拆解为有序工具调用链 |
| EXECUTE | 按计划依次调用Tool，收集结果 |
| OBSERVE | 检查执行结果，判断是否达成目标 |
| REPLAN | 基于观察修正计划，补充新步骤 |
| DONE | 所有步骤完成，整合生成自然语言输出 |

循环上限5次，每次记录execution_trace日志。

## Tool Registry

16个Tool封装全部WMS API：

| 分类 | Tool名称 | 对应WMS API | 权限 |
|------|---------|------------|------|
| 商品 | product_search | GET /api/product/page | R |
| 商品 | product_detail | GET /api/product/{id} | R |
| 商品 | product_adjust_stock | POST /api/product/adjustStock | W |
| 入库 | inbound_search | GET /api/inbound/page | R |
| 入库 | inbound_detail | GET /api/inbound/{id} | R |
| 入库 | inbound_audit | PUT /api/inbound/audit/{id} | W |
| 出库 | outbound_search | GET /api/outbound/page | R |
| 出库 | outbound_detail | GET /api/outbound/{id} | R |
| 出库 | outbound_audit | PUT /api/outbound/audit/{id} | W |
| 报表 | dashboard_summary | GET /api/dashboard/today-summary | R |
| 报表 | dashboard_trend | GET /api/dashboard/last-7-days-trend | R |
| 报表 | dashboard_top | GET /api/dashboard/top-products | R |
| 报表 | dashboard_warehouse | GET /api/dashboard/warehouse-distribution | R |
| 告警 | alert_search | GET /api/alert/page | R |
| 告警 | alert_stats | GET /api/alert/stats | R |
| 告警 | alert_handle | PUT /api/alert/handle | W |

Tool定义结构：name + description + parameters(JSON Schema) + executor(async function)

装饰器注册 `@tool(name, description)`，启动时自动扫描。

## API契约

Python AI Gateway (端口8090)：

```
POST   /ai/chat              SSE流式对话
POST   /ai/analysis          非流式深度分析
GET    /ai/tools             可用Tool列表
GET    /ai/sessions/{id}     会话历史
DELETE /ai/sessions/{id}     清除会话
```

### SSE流式协议

```
data: {"type":"plan_start","plan_id":"xxx","steps":["dashboard_summary"]}
data: {"type":"step_start","tool":"dashboard_summary"}
data: {"type":"step_end","tool":"dashboard_summary","result":{...}}
data: {"type":"observe","assessment":"..."}
data: {"type":"replan","reason":"...","new_steps":["warehouse_query"]}
data: {"type":"error","tool":"...","message":"...","recoverable":true}
data: {"type":"done","summary":"自然语言回复"}
```

## 前端集成

### AI助手面板
- 位置：现有前端右侧浮动按钮 → 展开侧边栏聊天面板
- 通信：SSE连接 /ai/chat
- 功能：对话输入、Tool调用过程可视化、历史记录

### 深度分析Dashboard
- 独立页面路由 `/ai-dashboard`
- 预设分析卡片：库存健康度、出库趋势预测、异常告警分析
- 支持自由输入分析问题
- 通信：HTTP调用 /ai/analysis

## 权限与安全

- **JWT透传**：Python不存储用户，JWT原样转发WMS API
- **Tool权限标记**：每个Tool标记读/写权限，Agent执行写操作需用户确认
- **Token刷新**：Python检测401返回特定code，前端自动刷新重试

## 技术栈

- **Web框架**：FastAPI + uvicorn
- **HTTP客户端**：httpx（async）
- **LLM Provider**：openai Python SDK（兼容模式调用通义千问/DeepSeek）
- **会话存储**：Redis
- **前端**：现有Vue 3项目扩展

## 项目结构

```
ai-gateway/
├── main.py                 # FastAPI入口
├── config.py               # 配置管理（环境变量）
├── agents/
│   ├── engine.py           # 状态机引擎
│   ├── planner.py          # PLAN状态
│   ├── executor.py         # EXECUTE状态
│   └── observer.py         # OBSERVE/REPLAN状态
├── tools/
│   ├── registry.py         # Tool注册中心
│   ├── decorator.py        # @tool装饰器
│   ├── product_tools.py    # 商品相关Tool
│   ├── inbound_tools.py    # 入库相关Tool
│   ├── outbound_tools.py   # 出库相关Tool
│   ├── dashboard_tools.py  # 报表相关Tool
│   └── alert_tools.py      # 告警相关Tool
├── llm/
│   ├── provider.py         # LLM Provider抽象
│   ├── qwen.py             # 通义千问适配
│   └── deepseek.py         # DeepSeek适配
├── api/
│   ├── chat.py             # /ai/chat端点
│   ├── analysis.py         # /ai/analysis端点
│   └── sessions.py         # 会话管理端点
├── models/
│   ├── schemas.py          # Pydantic模型
│   └── session.py          # 会话数据模型
└── utils/
    ├── jwt.py              # JWT透传工具
    └── wms_client.py       # WMS API客户端封装
```
