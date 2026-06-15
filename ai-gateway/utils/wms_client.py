import httpx
import logging
from config import settings

logger = logging.getLogger(__name__)


class WMSClient:
    def __init__(self, auth_token: str = ""):
        self.base_url = settings.wms_base_url.rstrip("/")
        self.token = auth_token

    def _headers(self) -> dict:
        h = {"Content-Type": "application/json"}
        if self.token:
            h["Authorization"] = f"Bearer {self.token}"
        return h

    async def get(self, path: str, params: dict | None = None) -> dict:
        url = f"{self.base_url}{path}"
        logger.info(f"[WMS GET] {url} params={params}")
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.get(url, params=params, headers=self._headers())
            logger.info(f"[WMS GET] {url} status={resp.status_code} body={resp.text[:500]}")
            return resp.json()

    async def post(self, path: str, body: dict | None = None) -> dict:
        url = f"{self.base_url}{path}"
        logger.info(f"[WMS POST] {url} body={body}")
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.post(url, json=body, headers=self._headers())
            logger.info(f"[WMS POST] {url} status={resp.status_code} body={resp.text[:500]}")
            return resp.json()

    async def put(self, path: str, body: dict | None = None) -> dict:
        url = f"{self.base_url}{path}"
        logger.info(f"[WMS PUT] {url} body={body}")
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.put(url, json=body, headers=self._headers())
            logger.info(f"[WMS PUT] {url} status={resp.status_code} body={resp.text[:500]}")
            return resp.json()

    async def delete(self, path: str) -> dict:
        url = f"{self.base_url}{path}"
        logger.info(f"[WMS DELETE] {url}")
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.delete(url, headers=self._headers())
            logger.info(f"[WMS DELETE] {url} status={resp.status_code} body={resp.text[:500]}")
            return resp.json()
