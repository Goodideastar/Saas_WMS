# ai-gateway/agents/nodes.py
"""LangGraph节点定义：每个节点对应一个处理步骤"""
import json
import re
import logging
from llm.provider import get_provider
from tools.registry import registry
from utils.wms_client import WMSClient

logger = logging.getLogger(__name__)


def extract_json(content: str) -> dict | None:
    """从LLM返回内容中提取JSON，支持多种格式"""
    if not content or not content.strip():
        logger.warning("LLM返回空内容")
        return None

    text = content.strip()

    # 去除markdown代码块包裹
    text = re.sub(r"^```(?:json)?\s*", "", text)
    text = re.sub(r"\s*```$", "", text)
    text = text.strip()

    # 尝试直接解析
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        pass

    # 尝试提取第一个完整的JSON对象（从第一个{ 到最后一个}）
    start = text.find("{")
    end = text.rfind("}")
    if start != -1 and end != -1 and end > start:
        json_str = text[start:end + 1]
        try:
            return json.loads(json_str)
        except json.JSONDecodeError:
            pass

    # 尝试逐行提取（处理LLM在JSON前后添加说明文字的情况）
    lines = text.split("\n")
    json_lines = []
    in_json = False
    brace_count = 0
    for line in lines:
        if "{" in line and not in_json:
            in_json = True
        if in_json:
            json_lines.append(line)
            brace_count += line.count("{") - line.count("}")
            if brace_count <= 0:
                break
    if json_lines:
        json_str = "\n".join(json_lines)
        try:
            return json.loads(json_str)
        except json.JSONDecodeError:
            pass

    logger.error("无法从LLM返回中提取JSON: %s", content[:200])
    return None


def build_fallback_plan(state: dict, reason: str = "LLM返回格式异常，使用兜底计划") -> dict:
    """兜底计划：当LLM返回无法解析时，根据用户消息推断工具"""
    messages = state.get("messages", [])
    user_msg = ""
    for m in reversed(messages):
        if m.get("role") == "user":
            user_msg = m.get("content", "").lower()
            break

    # 根据关键词推断工具
    if any(kw in user_msg for kw in ["趋势", "排行", "排名", "top", "chart", "图表"]):
        steps = [{"tool": "dashboard_trend", "args": {}}]
    elif any(kw in user_msg for kw in ["预警", "告警", "alert"]):
        steps = [{"tool": "alert_search", "args": {}}]
    elif any(kw in user_msg for kw in ["入库", "inbound"]):
        steps = [{"tool": "inbound_search", "args": {}}]
    elif any(kw in user_msg for kw in ["出库", "outbound"]):
        steps = [{"tool": "outbound_search", "args": {}}]
    else:
        # 默认使用dashboard_summary兜底
        steps = [{"tool": "dashboard_summary", "args": {}}]

    return {
        "goal": "兜底查询",
        "steps": steps,
        "reason": reason,
    }


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

    prompt = f"""你是WMS仓储系统的任务规划器。用户想查询或操作数据，你必须选择合适的工具来执行。

可用工具: {tools_desc}
{context}

只返回纯JSON，不要包含任何其他文字、解释或markdown标记。
JSON格式:
{{"goal": "目标描述", "steps": [{{"tool": "工具名", "args": {{参数}}}}], "reason": "规划理由"}}

重要规则:
1. 用户问任何数据相关的问题，都必须至少调用一个工具查询，绝不返回空steps
2. 只使用列出的工具名，args严格匹配工具参数定义
3. 先读后写，写操作前确认
4. 用户问"库存"相关→用product_search或dashboard_summary；问"趋势/排行"→用dashboard_trend/dashboard_top；问"入库/出库单"→用inbound_search/outbound_search；问"预警/告警"→用alert_search
5. 不确定该用哪个工具时，至少用product_search或dashboard_summary兜底"""

    resp = await provider.chat([
        {"role": "system", "content": provider.system_prompt()},
        *messages[-5:],
        {"role": "user", "content": prompt},
    ])
    content = resp.choices[0].message.content
    plan = extract_json(content)

    if plan is None or not plan.get("steps"):
        plan = build_fallback_plan(state)

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

    chart_data = dict(state.get("chart_data", {}))
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
    """OBSERVE节点：规则评估执行结果（无LLM调用）"""
    tool_results = state.get("tool_results", [])
    tool_calls = state.get("tool_calls", [])

    if not tool_calls:
        return {
            "is_complete": False,
            "loop_count": state.get("loop_count", 0),
            "trace": [{"type": "observe", "assessment": {"complete": False, "assessment": "规划未生成工具调用，需要重新规划"}}],
        }

    if not tool_results:
        return {
            "is_complete": False,
            "trace": [{"type": "observe", "assessment": {"complete": False, "assessment": "无执行结果"}}],
        }

    all_success = all(r.get("success") for r in tool_results)
    has_data = any(
        r.get("success") and r.get("result") and r["result"] is not None
        for r in tool_results
    )

    complete = all_success and has_data

    return {
        "is_complete": complete,
        "trace": [{"type": "observe", "assessment": {
            "complete": complete,
            "assessment": "所有工具执行成功" if complete else "部分工具执行失败，需要重试",
        }}],
    }


async def replan_node(state: dict) -> dict:
    """REPLAN节点：基于观察结果重新规划"""
    provider = get_provider()
    tools_schema = state["tools_schema"]
    messages = state["messages"]
    tool_results = state.get("tool_results", [])

    prompt = f"""你是WMS仓储系统的任务规划器。之前的工具调用没有成功获取数据，你必须重新选择工具。

可用工具: {json.dumps(tools_schema, ensure_ascii=False)}
之前的执行结果: {json.dumps(tool_results, ensure_ascii=False)}

返回JSON格式:
{{"goal": "目标描述", "steps": [{{"tool": "工具名", "args": {{参数}}}}], "reason": "规划理由"}}

重要规则:
1. 必须至少调用一个工具，绝不返回空steps
2. 如果之前步骤失败，换一个相关工具重试
3. 只使用列出的工具名
4. 步骤最少化"""

    resp = await provider.chat([
        {"role": "system", "content": provider.system_prompt()},
        *messages[-5:],
        {"role": "user", "content": prompt},
    ])
    content = resp.choices[0].message.content
    plan = extract_json(content)

    if plan is None or not plan.get("steps"):
        plan = build_fallback_plan(state, reason="LLM重新规划返回格式异常，使用兜底计划")

    loop_count = state.get("loop_count", 0)
    return {
        "messages": [{"role": "assistant", "content": f"重新规划: {plan.get('reason', '')}"}],
        "tool_calls": plan.get("steps", []),
        "trace": [{"type": "replan", "plan": plan}],
        "loop_count": loop_count + 1,
    }


async def summarize_node(state: dict) -> dict:
    """SUMMARIZE节点：整合结果生成自然语言回复（LLM由run_agent流式处理）"""
    return {
        "final_response": "",
        "trace": [{"type": "summarize_start"}],
    }
