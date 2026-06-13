# ai-gateway/agents/engine.py
"""基于LangGraph的Agent引擎"""
import json
from langgraph.graph import StateGraph, END
from agents.state import AgentState
from agents.nodes import plan_node, execute_node, observe_node, replan_node, summarize_node


def should_continue(state: AgentState) -> str:
    """路由函数：决定下一步"""
    if state.get("is_complete"):
        return "summarize"
    loop_count = state.get("loop_count", 0)
    if loop_count >= 5:
        return "summarize"
    return "replan"


def build_graph() -> StateGraph:
    """构建LangGraph工作流"""
    workflow = StateGraph(AgentState)

    workflow.add_node("plan", plan_node)
    workflow.add_node("execute", execute_node)
    workflow.add_node("observe", observe_node)
    workflow.add_node("replan", replan_node)
    workflow.add_node("summarize", summarize_node)

    workflow.set_entry_point("plan")

    workflow.add_edge("plan", "execute")
    workflow.add_edge("execute", "observe")
    workflow.add_conditional_edges("observe", should_continue, {
        "replan": "replan",
        "summarize": "summarize"
    })
    workflow.add_edge("replan", "execute")
    workflow.add_edge("summarize", END)

    return workflow.compile()


graph = build_graph()


async def run_agent(
    user_message: str,
    auth_token: str,
    history: list[dict] | None = None,
):
    """运行Agent，实时流式返回SSE事件"""
    from llm.provider import get_provider
    from tools.registry import registry

    provider = get_provider()
    messages = history or []
    messages.append({"role": "user", "content": user_message})

    initial_state = {
        "messages": messages,
        "auth_token": auth_token,
        "tools_schema": registry.get_schema_for_llm(),
        "openai_tools": registry.to_openai_tools(),
        "tool_calls": [],
        "tool_results": [],
        "trace": [],
        "loop_count": 0,
        "is_complete": False,
        "final_response": "",
        "chart_data": {},
    }

    yielded_count = 0
    final_state = initial_state

    async for state in graph.astream(initial_state, {"recursion_limit": 25}):
        final_state = state
        traces = state.get("trace", [])
        while yielded_count < len(traces):
            yield traces[yielded_count]
            yielded_count += 1

    # 流式生成最终回复
    tool_results = final_state.get("tool_results", [])
    summary_prompt = f"根据以下工具调用结果，用自然语言回答用户。结果: {json.dumps(tool_results, ensure_ascii=False)}"

    full_text = ""
    async for chunk in provider.chat_stream([
        {"role": "system", "content": provider.system_prompt()},
        *messages[-3:],
        {"role": "user", "content": summary_prompt},
    ]):
        delta = chunk.choices[0].delta
        if delta and delta.content:
            full_text += delta.content
            yield {"type": "text_chunk", "content": delta.content}

    yield {
        "type": "done",
        "summary": full_text,
        "trace": final_state.get("trace", []),
        "chart_data": final_state.get("chart_data", {}),
    }
