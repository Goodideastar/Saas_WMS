# WMS AI网关 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建Python FastAPI AI网关服务，实现状态机驱动的AI Agent，封装WMS全部16个API为LLM可调用的Function Calling Tool。

**Architecture:** 独立FastAPI服务(端口8090) → 状态机Agent引擎(PLAN→EXECUTE→OBSERVE→REPLAN→DONE) → Tool Registry(16个@tool装饰器) → WMS Java API。前端Vue扩展AI侧边栏面板 + 独立深度分析Dashboard。

**Tech Stack:** Python 3.11+, FastAPI, httpx, openai SDK(兼容模式), Redis, SSE streaming

---

### Task 1: 项目骨架搭建

**Files:**
- Create: `ai-gateway/requirements.txt`
- Create: `ai-gateway/.env.example`
- Create: `ai-gateway/main.py`（最小入口）
- Create: `ai-gateway/config.py`

- [ ] **Step 1: 创建目录结构和requirements.txt**

```bash
mkdir -p ai-gateway/{agents,tools,llm,api,models,utils}
```

```txt
# ai-gateway/requirements.txt
fastapi==0.115.0
uvicorn[standard]==0.30.0
httpx==0.27.0
openai==1.55.0
redis==5.1.0
pydantic==2.9.0
pydantic-settings==2.5.0
sse-starlette==2.1.0
python-dotenv==1.0.1
```

- [ ] **Step 2: 创建 .env.example**

```env
# LLM配置
LLM_PROVIDER=qwen
LLM_API_KEY=your-api-key
LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
LLM_MODEL=qwen-plus

# WMS后端地址
WMS_BASE_URL=http://localhost:8080

# Redis
REDIS_URL=redis://localhost:6379/0

# 服务端口
AI_GATEWAY_PORT=8090
```

- [ ] **Step 3: 创建 config.py**

```python
# ai-gateway/config.py
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    llm_provider: str = "qwen"
    llm_api_key: str = ""
    llm_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    llm_model: str = "qwen-plus"
    wms_base_url: str = "http://localhost:8080"
    redis_url: str = "redis://localhost:6379/0"
    ai_gateway_port: int = 8090
    max_agent_loops: int = 5

    class Config:
        env_file = ".env"

settings = Settings()
```

- [ ] **Step 4: 创建最小 main.py 验证启动**

```python
# ai-gateway/main.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from config import settings

app = FastAPI(title="WMS AI Gateway", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/health")
async def health():
    return {"status": "ok"}
```

- [ ] **Step 5: 安装依赖并验证**

```bash
cd ai-gateway && pip install -r requirements.txt
uvicorn main:app --port 8090 --reload &
curl http://localhost:8090/health
```

Expected: `{"status":"ok"}`

- [ ] **Step 6: Commit**

```bash
cd ai-gateway && git init && git add -A && git commit -m "feat: ai-gateway project scaffold"
```

---

### Task 2: WMS API客户端 + JWT透传

**Files:**
- Create: `ai-gateway/utils/wms_client.py`
- Create: `ai-gateway/utils/__init__.py`

- [ ] **Step 1: 创建 WMS客户端**

```python
# ai-gateway/utils/wms_client.py
import httpx
from config import settings

class WMSClient:
    def __init__(self, auth_token: str = ""):
        self.base_url = settings.wms_base_url.rstrip("/")
        self.token = auth_token

    def _headers(self) -> dict:
        h = {"Content-Type": "application/json"}
        if self.token:
            h["Authorization"] = f"Bearer {self.token}"
        return h

    async def get(self, path: str, params: dict | None = None) -> dict:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.get(
                f"{self.base_url}{path}",
                params=params,
                headers=self._headers(),
            )
            return resp.json()

    async def post(self, path: str, body: dict | None = None) -> dict:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.post(
                f"{self.base_url}{path}",
                json=body,
                headers=self._headers(),
            )
            return resp.json()

    async def put(self, path: str, body: dict | None = None) -> dict:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.put(
                f"{self.base_url}{path}",
                json=body,
                headers=self._headers(),
            )
            return resp.json()

    async def delete(self, path: str) -> dict:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.delete(
                f"{self.base_url}{path}",
                headers=self._headers(),
            )
            return resp.json()
```

- [ ] **Step 2: Commit**

```bash
git add -A && git commit -m "feat: WMS API client with JWT passthrough"
```

---

### Task 3: LLM Provider 抽象层

**Files:**
- Create: `ai-gateway/llm/__init__.py`
- Create: `ai-gateway/llm/provider.py`
- Create: `ai-gateway/llm/qwen.py`

- [ ] **Step 1: 创建 Provider 抽象基类**

```python
# ai-gateway/llm/provider.py
from abc import ABC, abstractmethod
from openai import AsyncOpenAI
from config import settings

class LLMProvider(ABC):
    def __init__(self):
        self.client = AsyncOpenAI(
            api_key=settings.llm_api_key,
            base_url=settings.llm_base_url,
        )
        self.model = settings.llm_model

    @abstractmethod
    def system_prompt(self) -> str: ...

    async def chat(self, messages: list[dict], tools: list[dict] | None = None) -> dict:
        return await self.client.chat.completions.create(
            model=self.model,
            messages=messages,
            tools=tools,
            temperature=0.3,
        )

    async def chat_stream(self, messages: list[dict], tools: list[dict] | None = None):
        return await self.client.chat.completions.create(
            model=self.model,
            messages=messages,
            tools=tools,
            temperature=0.3,
            stream=True,
        )


def get_provider() -> LLMProvider:
    from llm.qwen import QwenProvider
    from llm.deepseek import DeepSeekProvider
    providers = {"qwen": QwenProvider, "deepseek": DeepSeekProvider}
    cls = providers.get(settings.llm_provider, QwenProvider)
    return cls()
```

- [ ] **Step 2: 创建通义千问适配器**

```python
# ai-gateway/llm/qwen.py
from llm.provider import LLMProvider

class QwenProvider(LLMProvider):
    def system_prompt(self) -> str:
        return """你是WMS仓储管理系统的AI助手。你可以：
1. 查询商品信息、库存状态
2. 查询入库单、出库单记录
3. 查看今日经营 summary 和趋势数据
4. 检查库存告警并处理
5. 调用工具执行写操作（审核入库/出库、调整库存、处理告警）

重要规则：
- 涉及写操作前必须向用户确认
- 数据以工具返回为准，不要编造
- 当用户意图不明确时，主动询问"""
```

- [ ] **Step 3: 创建DeepSeek适配器**

```python
# ai-gateway/llm/deepseek.py
from llm.provider import LLMProvider

class DeepSeekProvider(LLMProvider):
    def system_prompt(self) -> str:
        return """你是WMS仓储管理系统的AI助手。你可以：
1. 查询商品信息、库存状态
2. 查询入库单、出库单记录
3. 查看今日经营 summary 和趋势数据
4. 检查库存告警并处理
5. 调用工具执行写操作（审核入库/出库、调整库存、处理告警）

重要规则：
- 涉及写操作前必须向用户确认
- 数据以工具返回为准，不要编造
- 当用户意图不明确时，主动询问"""
```

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat: LLM provider abstraction with Qwen and DeepSeek adapters"
```

---

### Task 4: Tool Registry + 装饰器

**Files:**
- Create: `ai-gateway/tools/__init__.py`
- Create: `ai-gateway/tools/decorator.py`
- Create: `ai-gateway/tools/registry.py`

- [ ] **Step 1: 创建 @tool 装饰器**

```python
# ai-gateway/tools/decorator.py
from typing import Callable

class ToolDef:
    def __init__(self, name: str, description: str, parameters: dict, permission: str, func: Callable):
        self.name = name
        self.description = description
        self.parameters = parameters
        self.permission = permission  # "read" | "write"
        self.func = func

    def to_openai_schema(self) -> dict:
        return {
            "type": "function",
            "function": {
                "name": self.name,
                "description": self.description,
                "parameters": self.parameters,
            }
        }


def tool(name: str, description: str, parameters: dict, permission: str = "read"):
    def decorator(func: Callable) -> ToolDef:
        return ToolDef(
            name=name,
            description=description,
            parameters=parameters,
            permission=permission,
            func=func,
        )
    return decorator
```

- [ ] **Step 2: 创建 ToolRegistry**

```python
# ai-gateway/tools/registry.py
from tools.decorator import ToolDef

class ToolRegistry:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._tools: dict[str, ToolDef] = {}
        return cls._instance

    def register(self, tool_def: ToolDef):
        self._tools[tool_def.name] = tool_def

    def get(self, name: str) -> ToolDef | None:
        return self._tools.get(name)

    def list_all(self) -> list[ToolDef]:
        return list(self._tools.values())

    def to_openai_tools(self) -> list[dict]:
        return [t.to_openai_schema() for t in self._tools.values()]

    def get_schema_for_llm(self) -> list[dict]:
        """返回简化的工具列表给LLM做planning"""
        return [
            {"name": t.name, "description": t.description, "permission": t.permission}
            for t in self._tools.values()
        ]

registry = ToolRegistry()
```

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat: Tool registry with decorator pattern"
```

---

### Task 5: 16个Tool实现

**Files:**
- Create: `ai-gateway/tools/product_tools.py`
- Create: `ai-gateway/tools/inbound_tools.py`
- Create: `ai-gateway/tools/outbound_tools.py`
- Create: `ai-gateway/tools/dashboard_tools.py`
- Create: `ai-gateway/tools/alert_tools.py`

- [ ] **Step 1: 商品Tools**

```python
# ai-gateway/tools/product_tools.py
from tools.decorator import tool
from tools.registry import registry
from utils.wms_client import WMSClient

_search_params = {
    "type": "object",
    "properties": {
        "page": {"type": "integer", "description": "页码"},
        "size": {"type": "integer", "description": "每页条数"},
        "name": {"type": "string", "description": "商品名称模糊搜索"},
        "code": {"type": "string", "description": "商品编码精确搜索"},
    },
    "required": []
}

_detail_params = {
    "type": "object",
    "properties": {"id": {"type": "integer", "description": "商品ID"}},
    "required": ["id"]
}

_adjust_params = {
    "type": "object",
    "properties": {
        "productId": {"type": "integer", "description": "商品ID"},
        "quantity": {"type": "integer", "description": "调整数量(正数增加,负数减少)"},
        "remark": {"type": "string", "description": "备注"},
    },
    "required": ["productId", "quantity"]
}

@tool("product_search", "查询商品列表，支持按名称、编码筛选，返回分页结果", _search_params)
async def product_search(client: WMSClient, **kwargs) -> dict:
    return await client.get("/api/product/page", params=kwargs)

@tool("product_detail", "获取单个商品详细信息", _detail_params)
async def product_detail(client: WMSClient, **kwargs) -> dict:
    return await client.get(f"/api/product/{kwargs['id']}")

@tool("product_adjust_stock", "调整商品库存数量，需确认后执行", _adjust_params, permission="write")
async def product_adjust_stock(client: WMSClient, **kwargs) -> dict:
    return await client.post("/api/product/adjustStock", body=kwargs)

def register_all():
    registry.register(product_search)
    registry.register(product_detail)
    registry.register(product_adjust_stock)
```

- [ ] **Step 2: 入库Tools**

```python
# ai-gateway/tools/inbound_tools.py
from tools.decorator import tool
from tools.registry import registry
from utils.wms_client import WMSClient

_page_params = {
    "type": "object",
    "properties": {
        "page": {"type": "integer", "description": "页码"},
        "size": {"type": "integer", "description": "每页条数"},
        "status": {"type": "string", "description": "状态: pending/audited/cancelled"},
        "orderNo": {"type": "string", "description": "入库单号"},
    },
    "required": []
}

_detail_params = {
    "type": "object",
    "properties": {"id": {"type": "integer", "description": "入库单ID"}},
    "required": ["id"]
}

_audit_params = {
    "type": "object",
    "properties": {"id": {"type": "integer", "description": "入库单ID"}},
    "required": ["id"]
}

@tool("inbound_search", "查询入库单列表，支持按状态、单号筛选", _page_params)
async def inbound_search(client: WMSClient, **kwargs) -> dict:
    return await client.get("/api/inbound/page", params=kwargs)

@tool("inbound_detail", "获取单个入库单详细信息，含商品明细", _detail_params)
async def inbound_detail(client: WMSClient, **kwargs) -> dict:
    return await client.get(f"/api/inbound/{kwargs['id']}")

@tool("inbound_audit", "审核入库单，审核后库存增加，需确认后执行", _audit_params, permission="write")
async def inbound_audit(client: WMSClient, **kwargs) -> dict:
    return await client.put(f"/api/inbound/audit/{kwargs['id']}")

def register_all():
    registry.register(inbound_search)
    registry.register(inbound_detail)
    registry.register(inbound_audit)
```

- [ ] **Step 3: 出库Tools**

```python
# ai-gateway/tools/outbound_tools.py
from tools.decorator import tool
from tools.registry import registry
from utils.wms_client import WMSClient

_page_params = {
    "type": "object",
    "properties": {
        "page": {"type": "integer", "description": "页码"},
        "size": {"type": "integer", "description": "每页条数"},
        "status": {"type": "string", "description": "状态: pending/audited/cancelled"},
        "orderNo": {"type": "string", "description": "出库单号"},
    },
    "required": []
}

_detail_params = {
    "type": "object",
    "properties": {"id": {"type": "integer", "description": "出库单ID"}},
    "required": ["id"]
}

_audit_params = {
    "type": "object",
    "properties": {"id": {"type": "integer", "description": "出库单ID"}},
    "required": ["id"]
}

@tool("outbound_search", "查询出库单列表，支持按状态、单号筛选", _page_params)
async def outbound_search(client: WMSClient, **kwargs) -> dict:
    return await client.get("/api/outbound/page", params=kwargs)

@tool("outbound_detail", "获取单个出库单详细信息，含商品明细", _detail_params)
async def outbound_detail(client: WMSClient, **kwargs) -> dict:
    return await client.get(f"/api/outbound/{kwargs['id']}")

@tool("outbound_audit", "审核出库单，审核后库存减少，需确认后执行", _audit_params, permission="write")
async def outbound_audit(client: WMSClient, **kwargs) -> dict:
    return await client.put(f"/api/outbound/audit/{kwargs['id']}")

def register_all():
    registry.register(outbound_search)
    registry.register(outbound_detail)
    registry.register(outbound_audit)
```

- [ ] **Step 4: 报表Tools**

```python
# ai-gateway/tools/dashboard_tools.py
from tools.decorator import tool
from tools.registry import registry
from utils.wms_client import WMSClient

_empty = {"type": "object", "properties": {}, "required": []}

@tool("dashboard_summary", "获取今日经营摘要：总入库量、总出库量、库存告警数", _empty)
async def dashboard_summary(client: WMSClient, **kwargs) -> dict:
    return await client.get("/api/dashboard/today-summary")

@tool("dashboard_trend", "获取近7天出入库趋势数据", _empty)
async def dashboard_trend(client: WMSClient, **kwargs) -> dict:
    return await client.get("/api/dashboard/last-7-days-trend")

@tool("dashboard_top", "获取出入库TOP商品排行", _empty)
async def dashboard_top(client: WMSClient, **kwargs) -> dict:
    return await client.get("/api/dashboard/top-products")

@tool("dashboard_warehouse", "获取各仓库库存分布情况", _empty)
async def dashboard_warehouse(client: WMSClient, **kwargs) -> dict:
    return await client.get("/api/dashboard/warehouse-distribution")

def register_all():
    registry.register(dashboard_summary)
    registry.register(dashboard_trend)
    registry.register(dashboard_top)
    registry.register(dashboard_warehouse)
```

- [ ] **Step 5: 告警Tools**

```python
# ai-gateway/tools/alert_tools.py
from tools.decorator import tool
from tools.registry import registry
from utils.wms_client import WMSClient

_page_params = {
    "type": "object",
    "properties": {
        "page": {"type": "integer", "description": "页码"},
        "size": {"type": "integer", "description": "每页条数"},
        "status": {"type": "string", "description": "状态: pending/handled"},
    },
    "required": []
}

_handle_params = {
    "type": "object",
    "properties": {
        "id": {"type": "integer", "description": "告警ID"},
        "remark": {"type": "string", "description": "处理备注"},
    },
    "required": ["id"]
}

@tool("alert_stats", "获取库存告警统计：各级别告警数量", _empty := {"type": "object", "properties": {}, "required": []})
async def alert_stats(client: WMSClient, **kwargs) -> dict:
    return await client.get("/api/alert/stats")

@tool("alert_search", "查询库存告警列表，支持按状态筛选", _page_params)
async def alert_search(client: WMSClient, **kwargs) -> dict:
    return await client.get("/api/alert/page", params=kwargs)

@tool("alert_handle", "处理库存告警，标记为已处理", _handle_params, permission="write")
async def alert_handle(client: WMSClient, **kwargs) -> dict:
    return await client.put(f"/api/alert/handle", body=kwargs)

def register_all():
    registry.register(alert_stats)
    registry.register(alert_search)
    registry.register(alert_handle)
```

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat: all 16 WMS tools implemented"
```

---

### Task 6: Agent Engine 状态机

**Files:**
- Create: `ai-gateway/agents/__init__.py`
- Create: `ai-gateway/agents/engine.py`
- Create: `ai-gateway/agents/planner.py`
- Create: `ai-gateway/agents/executor.py`
- Create: `ai-gateway/agents/observer.py`

- [ ] **Step 1: 创建引擎主循环**

```python
# ai-gateway/agents/engine.py
import json
import asyncio
from config import settings
from llm.provider import get_provider
from tools.registry import registry
from utils.wms_client import WMSClient
from agents.planner import plan
from agents.executor import execute
from agents.observer import observe

class AgentState:
    PLAN = "plan"
    EXECUTE = "execute"
    OBSERVE = "observe"
    REPLAN = "replan"
    DONE = "done"

async def run_agent(
    user_message: str,
    auth_token: str,
    history: list[dict] | None = None,
):
    provider = get_provider()
    client = WMSClient(auth_token=auth_token)
    messages = history or []
    messages.append({"role": "user", "content": user_message})

    tools_schema = registry.get_schema_for_llm()
    openai_tools = registry.to_openai_tools()

    trace = []
    step_results = []
    loop_count = 0
    state = AgentState.PLAN

    while loop_count < settings.max_agent_loops:
        loop_count += 1

        if state == AgentState.PLAN:
            plan_json = await plan(provider, messages, tools_schema, step_results)
            trace.append({"type": "plan", "plan": plan_json})
            yield {"type": "plan_start", "plan": plan_json}
            state = AgentState.EXECUTE

        elif state == AgentState.EXECUTE:
            for step in plan_json.get("steps", []):
                yield {"type": "step_start", "tool": step["tool"], "args": step.get("args", {})}
                try:
                    result = await execute(step["tool"], step.get("args", {}), client)
                    step_results.append({"tool": step["tool"], "result": result, "success": True})
                    yield {"type": "step_end", "tool": step["tool"], "result": result}
                except Exception as e:
                    step_results.append({"tool": step["tool"], "error": str(e), "success": False})
                    yield {"type": "error", "tool": step["tool"], "message": str(e), "recoverable": True})
            state = AgentState.OBSERVE

        elif state == AgentState.OBSERVE:
            obs = await observe(provider, messages, plan_json, step_results)
            trace.append({"type": "observe", "assessment": obs})
            yield {"type": "observe", "assessment": obs}
            if obs.get("complete"):
                state = AgentState.DONE
            else:
                state = AgentState.REPLAN

        elif state == AgentState.REPLAN:
            yield {"type": "replan", "missing": obs.get("missing_info", [])}
            plan_json = await plan(provider, messages, tools_schema, step_results, missing=obs.get("missing_info"))
            trace.append({"type": "replan", "plan": plan_json})
            yield {"type": "replan_start", "plan": plan_json}
            state = AgentState.EXECUTE

        elif state == AgentState.DONE:
            summary = await summarize(provider, messages, step_results)
            yield {"type": "done", "summary": summary, "trace": trace}
            return

    yield {"type": "error", "message": "Agent loop limit exceeded", "recoverable": False, "trace": trace}


async def summarize(provider, messages, step_results) -> str:
    prompt = f"根据以下工具调用结果，用自然语言回答用户。结果: {json.dumps(step_results, ensure_ascii=False)}"
    resp = await provider.chat([
        {"role": "system", "content": provider.system_prompt()},
        *messages[-3:],
        {"role": "user", "content": prompt},
    ])
    return resp.choices[0].message.content
```

- [ ] **Step 2: 创建 Planner 模块**

```python
# ai-gateway/agents/planner.py
import json

async def plan(provider, messages, tools_schema, previous_results=None, missing=None) -> dict:
    tools_desc = json.dumps(tools_schema, ensure_ascii=False)
    context = ""
    if previous_results:
        context = f"\n之前的执行结果: {json.dumps(previous_results, ensure_ascii=False)}"
    if missing:
        context += f"\n缺少的信息: {json.dumps(missing, ensure_ascii=False)}"

    prompt = f"""你是一个任务规划器。根据用户意图，制定工具调用计划。

可用工具: {tools_desc}
{context}

返回JSON格式:
{{"goal": "目标描述", "steps": [{{"tool": "工具名", "args": {{参数}}}}, ...], "reason": "规划理由"}}

规则:
1. 只使用列出工具名，args严格匹配工具参数定义
2. 先读后写，写操作前确认
3. 报表类查询直接用一个工具即可
4. 步骤最少化，不要冗余调用"""

    resp = await provider.chat([
        {"role": "system", "content": provider.system_prompt()},
        *messages[-5:],
        {"role": "user", "content": prompt},
    ])
    content = resp.choices[0].message.content
    content = content.strip().removeprefix("```json").removesuffix("```").strip()
    return json.loads(content)
```

- [ ] **Step 3: 创建 Executor 模块**

```python
# ai-gateway/agents/executor.py
from tools.registry import registry

async def execute(tool_name: str, args: dict, client) -> dict:
    tool_def = registry.get(tool_name)
    if not tool_def:
        raise ValueError(f"Unknown tool: {tool_name}")

    result = await tool_def.func(client, **args)
    return result
```

- [ ] **Step 4: 创建 Observer 模块**

```python
# ai-gateway/agents/observer.py
import json

async def observe(provider, messages, plan, step_results) -> dict:
    prompt = f"""评估以下工具执行结果是否完成了用户目标。

计划: {json.dumps(plan, ensure_ascii=False)}
执行结果: {json.dumps(step_results, ensure_ascii=False)}

返回JSON:
{{"complete": true/false, "assessment": "评估说明", "missing_info": ["缺少的信息"]}}"""

    resp = await provider.chat([
        {"role": "system", "content": provider.system_prompt()},
        *messages[-5:],
        {"role": "user", "content": prompt},
    ])
    content = resp.choices[0].message.content
    content = content.strip().removeprefix("```json").removesuffix("```").strip()
    return json.loads(content)
```

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: Agent engine with state machine (PLAN→EXECUTE→OBSERVE→REPLAN→DONE)"
```

---

### Task 7: API端点

**Files:**
- Create: `ai-gateway/api/__init__.py`
- Create: `ai-gateway/api/chat.py`
- Create: `ai-gateway/api/analysis.py`
- Create: `ai-gateway/api/sessions.py`
- Create: `ai-gateway/models/__init__.py`
- Create: `ai-gateway/models/schemas.py`
- Create: `ai-gateway/models/session.py`

- [ ] **Step 1: 创建Pydantic模型**

```python
# ai-gateway/models/schemas.py
from pydantic import BaseModel

class ChatRequest(BaseModel):
    message: str
    session_id: str = "default"

class AnalysisRequest(BaseModel):
    query: str
    context: dict | None = None

class ToolInfo(BaseModel):
    name: str
    description: str
    permission: str

class ToolListResponse(BaseModel):
    tools: list[ToolInfo]
```

```python
# ai-gateway/models/session.py
import json
import redis.asyncio as aioredis
from config import settings

class SessionStore:
    def __init__(self):
        self.redis = aioredis.from_url(settings.redis_url)

    async def get_history(self, session_id: str) -> list[dict]:
        key = f"ai:session:{session_id}"
        data = await self.redis.get(key)
        return json.loads(data) if data else []

    async def append(self, session_id: str, role: str, content: str):
        key = f"ai:session:{session_id}"
        history = await self.get_history(session_id)
        history.append({"role": role, "content": content})
        if len(history) > 40:
            history = history[-40:]
        await self.redis.setex(key, 86400, json.dumps(history, ensure_ascii=False))

    async def clear(self, session_id: str):
        await self.redis.delete(f"ai:session:{session_id}")

session_store = SessionStore()
```

- [ ] **Step 2: 创建 Chat SSE端点**

```python
# ai-gateway/api/chat.py
import json
import traceback
from fastapi import APIRouter, Request
from sse_starlette.sse import EventSourceResponse
from models.schemas import ChatRequest
from models.session import session_store
from agents.engine import run_agent

router = APIRouter()

@router.post("/chat")
async def chat(req: ChatRequest, request: Request):
    auth_token = request.headers.get("Authorization", "").removeprefix("Bearer ")

    async def event_stream():
        try:
            history = await session_store.get_history(req.session_id)
            await session_store.append(req.session_id, "user", req.message)

            full_response = ""
            async for event in run_agent(req.message, auth_token, history):
                if event["type"] == "done":
                    full_response = event.get("summary", "")
                data = json.dumps(event, ensure_ascii=False)
                yield {"event": "message", "data": data}

            if full_response:
                await session_store.append(req.session_id, "assistant", full_response)

        except Exception as e:
            yield {"event": "error", "data": json.dumps({"type": "error", "message": str(e)})}

    return EventSourceResponse(event_stream())
```

- [ ] **Step 3: 创建 Analysis端点 + Sessions端点**

```python
# ai-gateway/api/analysis.py
import json
from fastapi import APIRouter, Request
from models.schemas import AnalysisRequest
from models.session import session_store
from agents.engine import run_agent

router = APIRouter()

@router.post("/analysis")
async def analysis(req: AnalysisRequest, request: Request):
    auth_token = request.headers.get("Authorization", "").removeprefix("Bearer ")
    results = []
    async for event in run_agent(req.query, auth_token):
        results.append(event)
        if event["type"] == "done":
            return {
                "insight": event["summary"],
                "trace": event.get("trace", []),
            }
    return {"insight": "Analysis failed", "trace": results}
```

```python
# ai-gateway/api/sessions.py
from fastapi import APIRouter
from models.session import session_store
from tools.registry import registry

router = APIRouter()

@router.get("/tools")
async def list_tools():
    tools = [
        {"name": t.name, "description": t.description, "permission": t.permission}
        for t in registry.list_all()
    ]
    return {"tools": tools}

@router.get("/sessions/{session_id}")
async def get_session(session_id: str):
    history = await session_store.get_history(session_id)
    return {"session_id": session_id, "messages": history}

@router.delete("/sessions/{session_id}")
async def delete_session(session_id: str):
    await session_store.clear(session_id)
    return {"message": "ok"}
```

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat: API endpoints - chat SSE, analysis, sessions, tools list"
```

---

### Task 8: 总装 main.py

**Files:**
- Modify: `ai-gateway/main.py`

- [ ] **Step 1: 更新 main.py**

```python
# ai-gateway/main.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from config import settings
from api.chat import router as chat_router
from api.analysis import router as analysis_router
from api.sessions import router as sessions_router
from tools.product_tools import register_all as register_product
from tools.inbound_tools import register_all as register_inbound
from tools.outbound_tools import register_all as register_outbound
from tools.dashboard_tools import register_all as register_dashboard
from tools.alert_tools import register_all as register_alert

register_product()
register_inbound()
register_outbound()
register_dashboard()
register_alert()

app = FastAPI(title="WMS AI Gateway", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(chat_router, prefix="/ai")
app.include_router(analysis_router, prefix="/ai")
app.include_router(sessions_router, prefix="/ai")

@app.get("/health")
async def health():
    return {"status": "ok"}
```

- [ ] **Step 2: 启动验证**

```bash
cd ai-gateway && python main.py 2>&1 &
curl http://localhost:8090/health
curl http://localhost:8090/ai/tools
```

Expected: health返回 `{"status":"ok"}`，tools返回16个工具列表

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat: wire up main.py with all routers and tool registration"
```

---

### Task 9: Vue前端 — AI对话面板组件

**Files:**
- Create: `frontend/src/components/AiChat/AiChatPanel.vue`
- Create: `frontend/src/components/AiChat/AiChatTrigger.vue`

- [ ] **Step 1: 创建AI面板 Vue组件**

```vue
<!-- frontend/src/components/AiChat/AiChatPanel.vue -->
<template>
  <div class="ai-panel" :class="{ open: visible }">
    <div class="ai-panel-header">
      <span>AI 助手</span>
      <button @click="$emit('close')">×</button>
    </div>
    <div class="ai-panel-messages" ref="msgContainer">
      <div v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role]">
        <div class="msg-content">{{ msg.content }}</div>
        <div v-if="msg.toolCalls" class="tool-calls">
          <div v-for="tc in msg.toolCalls" :key="tc.tool" class="tool-chip">
            {{ tc.status === 'running' ? '⏳' : '✓' }} {{ tc.tool }}
          </div>
        </div>
      </div>
    </div>
    <div class="ai-panel-input">
      <input v-model="input" @keyup.enter="send" placeholder="输入问题..." :disabled="loading" />
      <button @click="send" :disabled="loading">发送</button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'
import { useUserStore } from '@/store/user'

const props = defineProps({ visible: Boolean })
defineEmits(['close'])

const userStore = useUserStore()
const input = ref('')
const loading = ref(false)
const messages = ref([])
const msgContainer = ref(null)

const send = async () => {
  if (!input.value.trim() || loading.value) return
  const text = input.value
  input.value = ''
  messages.value.push({ role: 'user', content: text })
  loading.value = true

  const assistantMsg = { role: 'assistant', content: '', toolCalls: [] }
  messages.value.push(assistantMsg)

  const resp = await fetch('http://localhost:8090/ai/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${userStore.token}` },
    body: JSON.stringify({ message: text, session_id: 'default' }),
  })

  const reader = resp.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''
    for (const line of lines) {
      if (line.startsWith('data: ')) {
        const data = JSON.parse(line.slice(6))
        if (data.type === 'step_start') {
          assistantMsg.toolCalls.push({ tool: data.tool, status: 'running' })
        } else if (data.type === 'step_end') {
          const tc = assistantMsg.toolCalls.find(t => t.tool === data.tool && t.status === 'running')
          if (tc) tc.status = 'done'
        } else if (data.type === 'done') {
          assistantMsg.content = data.summary
        }
      }
    }
  }
  loading.value = false
  scrollToBottom()
}

const scrollToBottom = () => {
  nextTick(() => {
    const el = msgContainer.value
    if (el) el.scrollTop = el.scrollHeight
  })
}
</script>

<style scoped>
.ai-panel {
  position: fixed; right: -400px; top: 0; width: 400px; height: 100vh;
  background: #fff; box-shadow: -2px 0 10px rgba(0,0,0,0.1); z-index: 1000;
  display: flex; flex-direction: column; transition: right 0.3s;
}
.ai-panel.open { right: 0; }
.ai-panel-header {
  padding: 16px; border-bottom: 1px solid #eee; display: flex;
  justify-content: space-between; align-items: center; font-weight: bold;
}
.ai-panel-header button { border: none; background: none; font-size: 20px; cursor: pointer; }
.ai-panel-messages { flex: 1; overflow-y: auto; padding: 16px; }
.ai-panel-input { padding: 12px; border-top: 1px solid #eee; display: flex; gap: 8px; }
.ai-panel-input input { flex: 1; padding: 8px 12px; border: 1px solid #ddd; border-radius: 6px; }
.ai-panel-input button { padding: 8px 16px; background: #409eff; color: #fff; border: none; border-radius: 6px; cursor: pointer; }
.msg { margin-bottom: 12px; }
.msg.user { text-align: right; }
.msg.user .msg-content { background: #409eff; color: #fff; display: inline-block; padding: 8px 12px; border-radius: 8px; max-width: 80%; }
.msg.assistant .msg-content { background: #f0f2f5; padding: 8px 12px; border-radius: 8px; }
.tool-calls { margin-top: 4px; }
.tool-chip { display: inline-block; padding: 2px 8px; margin: 2px; background: #e6f7ff; border-radius: 4px; font-size: 12px; }
</style>
```

```vue
<!-- frontend/src/components/AiChat/AiChatTrigger.vue -->
<template>
  <button class="ai-trigger" @click="$emit('toggle')" :title="open ? '收起AI助手' : '展开AI助手'">
    🤖
  </button>
</template>

<script setup>
defineProps({ open: Boolean })
defineEmits(['toggle'])
</script>

<style scoped>
.ai-trigger {
  position: fixed; right: 20px; bottom: 80px; width: 50px; height: 50px;
  border-radius: 50%; border: none; background: #409eff; color: #fff;
  font-size: 24px; cursor: pointer; box-shadow: 0 2px 10px rgba(0,0,0,0.2);
  z-index: 999; display: flex; align-items: center; justify-content: center;
}
</style>
```

- [ ] **Step 2: 在MainLayout中集成**

修改 `frontend/src/components/Layout/MainLayout.vue`，在 `<template>` 末尾添加：

```vue
<AiChatTrigger :open="aiPanelOpen" @toggle="aiPanelOpen = !aiPanelOpen" />
<AiChatPanel :visible="aiPanelOpen" @close="aiPanelOpen = false" />
```

在 `<script setup>` 中添加：

```js
import AiChatTrigger from '@/components/AiChat/AiChatTrigger.vue'
import AiChatPanel from '@/components/AiChat/AiChatPanel.vue'
const aiPanelOpen = ref(false)
```

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat: AI chat panel with SSE streaming in Vue frontend"
```

---

### Task 10: Vue前端 — AI深度分析Dashboard

**Files:**
- Create: `frontend/src/views/AiDashboard.vue`
- Modify: `frontend/src/router/index.js`

- [ ] **Step 1: 创建AI Dashboard页面**

```vue
<!-- frontend/src/views/AiDashboard.vue -->
<template>
  <div class="ai-dashboard">
    <h2>AI 深度分析</h2>

    <div class="analysis-input">
      <input v-model="query" @keyup.enter="analyze" placeholder="输入分析问题，如: 分析本月出库趋势并预测下周补货需求" />
      <button @click="analyze" :disabled="loading">分析</button>
    </div>

    <div v-if="loading" class="loading">AI分析中...</div>

    <div v-if="result" class="analysis-result">
      <div class="insight-card">
        <h3>分析洞察</h3>
        <p>{{ result.insight }}</p>
      </div>

      <div class="preset-grid">
        <div class="preset-card" v-for="p in presets" :key="p.label" @click="query = p.question; analyze()">
          <div class="preset-icon">{{ p.icon }}</div>
          <div class="preset-label">{{ p.label }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const query = ref('')
const loading = ref(false)
const result = ref(null)

const presets = [
  { icon: '📦', label: '库存健康度分析', question: '分析所有商品的库存健康度，识别低库存和积压商品' },
  { icon: '📈', label: '出库趋势预测', question: '分析近7天出库趋势，预测下周需要补货的商品' },
  { icon: '⚠️', label: '异常告警分析', question: '分析当前所有未处理的库存告警，给出处理建议' },
  { icon: '🏭', label: '仓库效率分析', question: '分析各仓库的出入库效率，找出瓶颈' },
]

const analyze = async () => {
  if (!query.value.trim() || loading.value) return
  loading.value = true
  result.value = null
  const resp = await fetch('http://localhost:8090/ai/analysis', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${userStore.token}` },
    body: JSON.stringify({ query: query.value }),
  })
  result.value = await resp.json()
  loading.value = false
}
</script>

<style scoped>
.ai-dashboard { padding: 24px; max-width: 1200px; margin: 0 auto; }
h2 { margin-bottom: 24px; }
.analysis-input { display: flex; gap: 12px; margin-bottom: 24px; }
.analysis-input input { flex: 1; padding: 12px 16px; border: 1px solid #ddd; border-radius: 8px; font-size: 15px; }
.analysis-input button { padding: 12px 24px; background: #409eff; color: #fff; border: none; border-radius: 8px; cursor: pointer; }
.loading { text-align: center; padding: 40px; color: #999; }
.insight-card { background: #f0f7ff; padding: 20px; border-radius: 8px; margin-bottom: 24px; line-height: 1.8; }
.preset-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.preset-card { text-align: center; padding: 24px; background: #fff; border: 1px solid #eee; border-radius: 8px; cursor: pointer; transition: box-shadow 0.2s; }
.preset-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.preset-icon { font-size: 32px; margin-bottom: 8px; }
.preset-label { font-size: 14px; color: #666; }
</style>
```

- [ ] **Step 2: 添加路由**

修改 `frontend/src/router/index.js`，在异步路由中添加：

```js
{
  path: '/ai-dashboard',
  name: 'AiDashboard',
  component: () => import('@/views/AiDashboard.vue'),
  meta: { title: 'AI分析', icon: 'cpu' }
}
```

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat: AI analysis dashboard page with presets"
```

---

### Task 11: Docker集成

**Files:**
- Create: `ai-gateway/Dockerfile`
- Modify: `docker-compose.yml`

- [ ] **Step 1: 创建 Dockerfile**

```dockerfile
# ai-gateway/Dockerfile
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8090

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8090"]
```

- [ ] **Step 2: 更新 docker-compose.yml**

在 `docker-compose.yml` 末尾添加：

```yaml
  ai-gateway:
    build: ./ai-gateway
    container_name: wms-ai-gateway
    ports:
      - "8090:8090"
    environment:
      - LLM_PROVIDER=qwen
      - LLM_API_KEY=${LLM_API_KEY}
      - LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
      - LLM_MODEL=qwen-plus
      - WMS_BASE_URL=http://backend:8080
      - REDIS_URL=redis://redis:6379/0
      - AI_GATEWAY_PORT=8090
    depends_on:
      - backend
      - redis
    restart: unless-stopped
```

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat: Dockerfile and docker-compose integration for ai-gateway"
```

---

### Task 12: 端到端集成测试

**Files:**
- Create: `ai-gateway/tests/__init__.py`
- Create: `ai-gateway/tests/test_tools.py`
- Create: `ai-gateway/tests/test_agent.py`

- [ ] **Step 1: Tool注册测试**

```python
# ai-gateway/tests/test_tools.py
import pytest
from tools.registry import ToolRegistry

def test_tool_registry_singleton():
    r1 = ToolRegistry()
    r2 = ToolRegistry()
    assert r1 is r2

def test_all_16_tools_registered():
    from tools.product_tools import register_all as rp
    from tools.inbound_tools import register_all as ri
    from tools.outbound_tools import register_all as ro
    from tools.dashboard_tools import register_all as rd
    from tools.alert_tools import register_all as ra

    registry = ToolRegistry()
    registry._tools.clear()

    rp(); ri(); ro(); rd(); ra()

    tools = registry.list_all()
    assert len(tools) == 16

    write_tools = [t for t in tools if t.permission == "write"]
    assert len(write_tools) == 4

    tool_names = {t.name for t in tools}
    assert "product_search" in tool_names
    assert "inbound_audit" in tool_names
    assert "alert_handle" in tool_names

def test_tool_to_openai_schema():
    from tools.product_tools import product_search
    schema = product_search.to_openai_schema()
    assert schema["type"] == "function"
    assert schema["function"]["name"] == "product_search"
    assert "parameters" in schema["function"]
```

- [ ] **Step 2: Agent状态机测试（mock LLM）**

```python
# ai-gateway/tests/test_agent.py
import pytest
from unittest.mock import AsyncMock
from agents.executor import execute

@pytest.mark.asyncio
async def test_execute_unknown_tool():
    with pytest.raises(ValueError, match="Unknown tool"):
        await execute("nonexistent_tool", {}, client=None)

@pytest.mark.asyncio
async def test_plan_structure():
    from agents.planner import plan
    mock_provider = AsyncMock()
    choice = type("Choice", (), {"message": type("Msg", (), {"content": '{"goal":"test","steps":[],"reason":"test"}'})})()
    mock_provider.chat.return_value = type("Resp", (), {"choices": [choice]})()
    mock_provider.system_prompt.return_value = "test prompt"
    result = await plan(mock_provider, [{"role":"user","content":"hello"}], [], [])
    assert "goal" in result
    assert "steps" in result
```

- [ ] **Step 3: 运行测试**

```bash
cd ai-gateway && python -m pytest tests/ -v
```

Expected: 3 tests pass

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "test: tool registry and agent engine unit tests"
```
