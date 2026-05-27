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
    result = await plan(mock_provider, [{"role": "user", "content": "hello"}], [], [])
    assert "goal" in result
    assert "steps" in result
