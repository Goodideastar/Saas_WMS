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
