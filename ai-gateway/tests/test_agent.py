import pytest
from unittest.mock import AsyncMock, patch
from agents.nodes import plan_node, observe_node


@pytest.mark.asyncio
async def test_plan_node_structure():
    """测试 plan_node 返回正确的状态更新结构"""
    mock_provider = AsyncMock()
    choice = type("Choice", (), {"message": type("Msg", (), {"content": '{"goal":"test","steps":[{"tool":"product_search","args":{"keyword":"test"}}],"reason":"test"}'})()})()
    mock_provider.chat.return_value = type("Resp", (), {"choices": [choice]})()
    mock_provider.system_prompt.return_value = "test prompt"

    state = {
        "messages": [{"role": "user", "content": "搜索产品"}],
        "tools_schema": [{"name": "product_search", "description": "搜索产品", "parameters": {"keyword": "string"}}],
        "tool_results": [],
    }

    with patch("agents.nodes.get_provider", return_value=mock_provider):
        result = await plan_node(state)

    assert "tool_calls" in result
    assert len(result["tool_calls"]) == 1
    assert result["tool_calls"][0]["tool"] == "product_search"
    assert "trace" in result
    assert result["trace"][0]["type"] == "plan"


@pytest.mark.asyncio
async def test_observe_node_all_success():
    """测试 observe_node：所有工具成功 → 完成"""
    state = {
        "tool_calls": [{"tool": "product_search", "args": {}}],
        "tool_results": [
            {"tool": "product_search", "result": {"data": [{"id": 1}]}, "success": True}
        ],
    }

    result = await observe_node(state)
    assert result["is_complete"] is True


@pytest.mark.asyncio
async def test_observe_node_some_failed():
    """测试 observe_node：部分工具失败 → 未完成"""
    state = {
        "tool_calls": [{"tool": "product_search", "args": {}}],
        "tool_results": [
            {"tool": "product_search", "error": "timeout", "success": False}
        ],
    }

    result = await observe_node(state)
    assert result["is_complete"] is False


@pytest.mark.asyncio
async def test_observe_node_empty_calls():
    """测试 observe_node：无工具调用 → 直接完成"""
    state = {
        "tool_calls": [],
        "tool_results": [],
    }

    result = await observe_node(state)
    assert result["is_complete"] is True
