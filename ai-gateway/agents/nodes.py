# ai-gateway/agents/nodes.py
"""LangGraph节点定义：每个节点对应一个处理步骤"""
import json
from llm.provider import get_provider
from tools.registry import registry
from utils.wms_client import WMSClient


async def plan_node(state: dict) -> dict:
    """PLAN节点：LLM分析意图并生成工具调用计划"""
    provider = get_provider()
    tools_schema = state["tools_schema"]
    messages = state["messages"]
    tool_results = state.get("tool_results", [])

    tools_desc = json.dumps(tools_schema, ensure_ascii=False)
    context = ""
    if tool_results:
        context = f"\n之前的执行结果: {json.dumps(tool_results, ensure_ascii=False)}"

    prompt = f"""你是一个任务规划器。根据用户意图，制定工具调用计划。

可用工具: {tools_desc}
{context}

返回JSON格式:
{{"goal": "目标描述", "steps": [{{"tool": "工具名", "args": {{参数}}}}], "reason": "规划理由"}}

规则:
1. 只使用列出的工具名，args严格匹配工具参数定义
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
    plan = json.loads(content)

    return {
        "messages": [{"role": "assistant", "content": f"规划: {plan.get('reason', '')}"}],
        "trace": [{"type": "plan", "plan": plan}],
        "tool_calls": plan.get("steps", []),
    }


async def execute_node(state: dict) -> dict:
    """EXECUTE节点：执行工具调用"""
    client = WMSClient(auth_token=state["auth_token"])
    tool_calls = state.get("tool_calls", [])
    tool_results = []
    trace = []

    for step in tool_calls:
        tool_name = step["tool"]
        args = step.get("args", {})
        trace.append({"type": "step_start", "tool": tool_name, "args": args})
        try:
            tool_def = registry.get(tool_name)
            if not tool_def:
                raise ValueError(f"Unknown tool: {tool_name}")
            result = await tool_def.func(client, **args)
            tool_results.append({"tool": tool_name, "result": result, "success": True})
            trace.append({"type": "step_end", "tool": tool_name, "result": result})
        except Exception as e:
            tool_results.append({"tool": tool_name, "error": str(e), "success": False})
            trace.append({"type": "error", "tool": tool_name, "message": str(e), "recoverable": True})

    # 提取图表数据（如果是dashboard相关查询）
    chart_data = {}
    for r in tool_results:
        if r.get("success") and r.get("result"):
            tool = r["tool"]
            result = r["result"]
            if tool == "dashboard_trend" and result.get("data"):
                chart_data["trend"] = result["data"]
            elif tool == "dashboard_top" and result.get("data"):
                chart_data["top_products"] = result["data"]
            elif tool == "dashboard_warehouse" and result.get("data"):
                chart_data["warehouse"] = result["data"]
            elif tool == "dashboard_summary" and result.get("data"):
                chart_data["summary"] = result["data"]
            elif tool == "alert_stats" and result.get("data"):
                chart_data["alerts"] = result["data"]

    return {
        "tool_results": tool_results,
        "trace": trace,
        "chart_data": chart_data,
    }


async def observe_node(state: dict) -> dict:
    """OBSERVE节点：评估执行结果是否完成目标"""
    provider = get_provider()
    tool_calls = state.get("tool_calls", [])
    tool_results = state.get("tool_results", [])
    messages = state["messages"]

    prompt = f"""评估以下工具执行结果是否完成了用户目标。

计划: {json.dumps(tool_calls, ensure_ascii=False)}
执行结果: {json.dumps(tool_results, ensure_ascii=False)}

返回JSON:
{{"complete": true/false, "assessment": "评估说明", "missing_info": ["缺少的信息"]}}"""

    resp = await provider.chat([
        {"role": "system", "content": provider.system_prompt()},
        *messages[-5:],
        {"role": "user", "content": prompt},
    ])
    content = resp.choices[0].message.content
    content = content.strip().removeprefix("```json").removesuffix("```").strip()
    obs = json.loads(content)

    return {
        "is_complete": obs.get("complete", False),
        "trace": [{"type": "observe", "assessment": obs}],
    }


async def replan_node(state: dict) -> dict:
    """REPLAN节点：基于观察结果重新规划"""
    provider = get_provider()
    tools_schema = state["tools_schema"]
    messages = state["messages"]
    tool_results = state.get("tool_results", [])
    trace = state.get("trace", [])

    prompt = f"""根据之前的执行结果，重新制定工具调用计划。

可用工具: {json.dumps(tools_schema, ensure_ascii=False)}
之前的执行结果: {json.dumps(tool_results, ensure_ascii=False)}

返回JSON格式:
{{"goal": "目标描述", "steps": [{{"tool": "工具名", "args": {{参数}}}}], "reason": "规划理由"}}

规则:
1. 只使用列出的工具名
2. 补充之前缺少的步骤
3. 步骤最少化"""

    resp = await provider.chat([
        {"role": "system", "content": provider.system_prompt()},
        *messages[-5:],
        {"role": "user", "content": prompt},
    ])
    content = resp.choices[0].message.content
    content = content.strip().removeprefix("```json").removesuffix("```").strip()
    plan = json.loads(content)

    return {
        "messages": [{"role": "assistant", "content": f"重新规划: {plan.get('reason', '')}"}],
        "tool_calls": plan.get("steps", []),
        "trace": [{"type": "replan", "plan": plan}],
    }


async def summarize_node(state: dict) -> dict:
    """SUMMARIZE节点：整合结果生成自然语言回复"""
    provider = get_provider()
    messages = state["messages"]
    tool_results = state.get("tool_results", [])

    prompt = f"根据以下工具调用结果，用自然语言回答用户。结果: {json.dumps(tool_results, ensure_ascii=False)}"
    resp = await provider.chat([
        {"role": "system", "content": provider.system_prompt()},
        *messages[-3:],
        {"role": "user", "content": prompt},
    ])
    summary = resp.choices[0].message.content

    return {
        "final_response": summary,
        "trace": [{"type": "done", "summary": summary}],
    }
