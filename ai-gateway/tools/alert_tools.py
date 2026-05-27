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

_empty = {"type": "object", "properties": {}, "required": []}

@tool("alert_stats", "获取库存告警统计：各级别告警数量", _empty)
async def alert_stats(client: WMSClient, **kwargs) -> dict:
    return await client.get("/api/alert/stats")

@tool("alert_search", "查询库存告警列表，支持按状态筛选", _page_params)
async def alert_search(client: WMSClient, **kwargs) -> dict:
    return await client.get("/api/alert/page", params=kwargs)

@tool("alert_handle", "处理库存告警，标记为已处理", _handle_params, permission="write")
async def alert_handle(client: WMSClient, **kwargs) -> dict:
    return await client.put("/api/alert/handle", body=kwargs)

def register_all():
    registry.register(alert_stats)
    registry.register(alert_search)
    registry.register(alert_handle)
