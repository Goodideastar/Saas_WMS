# ai-gateway/tools/inbound_tools.py
from tools.decorator import tool
from tools.registry import registry
from utils.wms_client import WMSClient

_page_params = {
    "type": "object",
    "properties": {
        "page": {"type": "integer", "description": "页码"},
        "size": {"type": "integer", "description": "每页条数"},
        "status": {"type": "string", "description": "状态: pending/audited/cancelled"},
        "orderNo": {"type": "string", "description": "入库单号"},
    },
    "required": []
}

_detail_params = {
    "type": "object",
    "properties": {"id": {"type": "integer", "description": "入库单ID"}},
    "required": ["id"]
}

_audit_params = {
    "type": "object",
    "properties": {"id": {"type": "integer", "description": "入库单ID"}},
    "required": ["id"]
}

@tool("inbound_search", "查询入库单列表，支持按状态、单号筛选", _page_params)
async def inbound_search(client: WMSClient, **kwargs) -> dict:
    return await client.get("/api/inbound/page", params=kwargs)

@tool("inbound_detail", "获取单个入库单详细信息，含商品明细", _detail_params)
async def inbound_detail(client: WMSClient, **kwargs) -> dict:
    return await client.get(f"/api/inbound/{kwargs['id']}")

@tool("inbound_audit", "审核入库单，审核后库存增加，需确认后执行", _audit_params, permission="write")
async def inbound_audit(client: WMSClient, **kwargs) -> dict:
    return await client.put(f"/api/inbound/audit/{kwargs['id']}")

def register_all():
    registry.register(inbound_search)
    registry.register(inbound_detail)
    registry.register(inbound_audit)
