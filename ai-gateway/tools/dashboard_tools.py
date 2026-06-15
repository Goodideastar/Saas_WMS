# ai-gateway/tools/dashboard_tools.py
from tools.decorator import tool
from tools.registry import registry
from utils.wms_client import WMSClient

_empty = {"type": "object", "properties": {}, "required": []}

@tool("dashboard_summary", "获取今日经营摘要：总入库量、总出库量、今日金额、库存告警数。当用户问：今日概况、今天数据、经营摘要、整体情况时使用", _empty)
async def dashboard_summary(client: WMSClient, **kwargs) -> dict:
    return await client.get("/api/dashboard/today-summary")

@tool("dashboard_trend", "获取近7天出入库趋势数据。当用户问：趋势、最近一周、走势、图表、最近几天时使用", _empty)
async def dashboard_trend(client: WMSClient, **kwargs) -> dict:
    return await client.get("/api/dashboard/last-7-days-trend")

@tool("dashboard_top", "获取出入库TOP商品排行。当用户问：排行、TOP、热门、销量最好、出库最多时使用", _empty)
async def dashboard_top(client: WMSClient, **kwargs) -> dict:
    return await client.get("/api/dashboard/top-products")

@tool("dashboard_warehouse", "获取各仓库库存分布情况。当用户问：仓库分布、各仓库库存、仓库概况时使用", _empty)
async def dashboard_warehouse(client: WMSClient, **kwargs) -> dict:
    return await client.get("/api/dashboard/warehouse-distribution")

def register_all():
    registry.register(dashboard_summary)
    registry.register(dashboard_trend)
    registry.register(dashboard_top)
    registry.register(dashboard_warehouse)
