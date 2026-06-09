# ai-gateway/agents/state.py
from typing import TypedDict, Annotated, Sequence, Any
import operator


class AgentState(TypedDict):
    """LangGraph Agent状态定义"""
    messages: Annotated[list[dict], operator.add]
    auth_token: str
    tools_schema: list[dict]
    openai_tools: list[dict]
    tool_calls: Annotated[list[dict], operator.add]
    tool_results: Annotated[list[dict], operator.add]
    trace: Annotated[list[dict], operator.add]
    loop_count: int
    is_complete: bool
    final_response: str
    chart_data: dict[str, Any]
