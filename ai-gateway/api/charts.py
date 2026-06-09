# ai-gateway/api/charts.py
"""图表数据API - 返回真实业务数据供前端渲染"""
from fastapi import APIRouter, Request
from utils.wms_client import WMSClient

router = APIRouter()


@router.get("/trend")
async def get_trend_data(request: Request):
    """获取近7天出入库趋势数据"""
    auth_token = request.headers.get("Authorization", "").removeprefix("Bearer ")
    client = WMSClient(auth_token=auth_token)
    return await client.get("/api/dashboard/last-7-days-trend")


@router.get("/top-products")
async def get_top_products(request: Request):
    """获取货品出库排行"""
    auth_token = request.headers.get("Authorization", "").removeprefix("Bearer ")
    client = WMSClient(auth_token=auth_token)
    return await client.get("/api/dashboard/top-products")


@router.get("/warehouse")
async def get_warehouse_data(request: Request):
    """获取仓库库存分布"""
    auth_token = request.headers.get("Authorization", "").removeprefix("Bearer ")
    client = WMSClient(auth_token=auth_token)
    return await client.get("/api/dashboard/warehouse-distribution")


@router.get("/alerts")
async def get_alert_data(request: Request):
    """获取库存预警统计"""
    auth_token = request.headers.get("Authorization", "").removeprefix("Bearer ")
    client = WMSClient(auth_token=auth_token)
    return await client.get("/api/alert/stats")


@router.get("/summary")
async def get_summary_data(request: Request):
    """获取今日经营摘要"""
    auth_token = request.headers.get("Authorization", "").removeprefix("Bearer ")
    client = WMSClient(auth_token=auth_token)
    return await client.get("/api/dashboard/today-summary")
