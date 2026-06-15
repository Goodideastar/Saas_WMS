# ai-gateway/tools/product_tools.py
from tools.decorator import tool
from tools.registry import registry
from utils.wms_client import WMSClient

_search_params = {
    "type": "object",
    "properties": {
        "page": {"type": "integer", "description": "页码"},
        "size": {"type": "integer", "description": "每页条数"},
        "name": {"type": "string", "description": "商品名称模糊搜索"},
        "code": {"type": "string", "description": "商品编码精确搜索"},
    },
    "required": []
}

_detail_params = {
    "type": "object",
    "properties": {"id": {"type": "integer", "description": "商品ID"}},
    "required": ["id"]
}

_adjust_params = {
    "type": "object",
    "properties": {
        "productId": {"type": "integer", "description": "商品ID"},
        "quantity": {"type": "integer", "description": "调整数量(正数增加,负数减少)"},
        "remark": {"type": "string", "description": "备注"},
    },
    "required": ["productId", "quantity"]
}

@tool("product_search", "查询商品/货品列表。当用户问：货品列表、库存情况、有哪些商品、商品信息、搜索商品时使用。返回分页结果含库存量", _search_params)
async def product_search(client: WMSClient, **kwargs) -> dict:
    return await client.get("/api/product/page", params=kwargs)

@tool("product_detail", "获取单个商品详细信息", _detail_params)
async def product_detail(client: WMSClient, **kwargs) -> dict:
    return await client.get(f"/api/product/{kwargs['id']}")

@tool("product_adjust_stock", "调整商品库存数量，需确认后执行", _adjust_params, permission="write")
async def product_adjust_stock(client: WMSClient, **kwargs) -> dict:
    return await client.post("/api/product/adjustStock", body=kwargs)

def register_all():
    registry.register(product_search)
    registry.register(product_detail)
    registry.register(product_adjust_stock)
