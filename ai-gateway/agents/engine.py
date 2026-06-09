# ai-gateway/agents/engine.py
"""基于LangGraph的Agent引擎"""
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

    # 添加节点
    workflow.add_node("plan", plan_node)
    workflow.add_node("execute", execute_node)
    workflow.add_node("observe", observe_node)
    workflow.add_node("replan", replan_node)
    workflow.add_node("summarize", summarize_node)

    # 设置入口点
    workflow.set_entry_point("plan")

    # 添加边
    workflow.add_edge("plan", "execute")
    workflow.add_edge("execute", "observe")
    workflow.add_conditional_edges("observe", should_continue, {
        "replan": "replan",
        "summarize": "summarize"
    })
    workflow.add_edge("replan", "execute")
    workflow.add_edge("summarize", END)

    return workflow.compile()


# 全局图实例
graph = build_graph()


async def run_agent(
    user_message: str,
    auth_token: str,
    history: list[dict] | None = None,
):
    """运行Agent，返回SSE事件流"""
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

    # 执行图
    result = await graph.ainvoke(initial_state, {"recursion_limit": 25})

    # 生成事件流
    for event in result.get("trace", []):
        yield event

    # 最终结果
    yield {
        "type": "done",
        "summary": result.get("final_response", ""),
        "trace": result.get("trace", []),
        "chart_data": result.get("chart_data", {}),
    }
