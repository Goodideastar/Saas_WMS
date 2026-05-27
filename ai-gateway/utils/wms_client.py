import httpx
from config import settings


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
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.get(
                f"{self.base_url}{path}",
                params=params,
                headers=self._headers(),
            )
            return resp.json()

    async def post(self, path: str, body: dict | None = None) -> dict:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.post(
                f"{self.base_url}{path}",
                json=body,
                headers=self._headers(),
            )
            return resp.json()

    async def put(self, path: str, body: dict | None = None) -> dict:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.put(
                f"{self.base_url}{path}",
                json=body,
                headers=self._headers(),
            )
            return resp.json()

    async def delete(self, path: str) -> dict:
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.delete(
                f"{self.base_url}{path}",
                headers=self._headers(),
            )
            return resp.json()
