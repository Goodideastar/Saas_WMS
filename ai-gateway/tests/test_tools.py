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

    rp()
    ri()
    ro()
    rd()
    ra()

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
