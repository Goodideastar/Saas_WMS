# ai-gateway/models/session.py
import json
import redis.asyncio as aioredis
from config import settings


class SessionStore:
    def __init__(self):
        self.redis = aioredis.from_url(settings.redis_url)

    async def get_history(self, session_id: str) -> list[dict]:
        key = f"ai:session:{session_id}"
        data = await self.redis.get(key)
        return json.loads(data) if data else []

    async def append(self, session_id: str, role: str, content: str):
        key = f"ai:session:{session_id}"
        history = await self.get_history(session_id)
        history.append({"role": role, "content": content})
        if len(history) > 40:
            history = history[-40:]
        await self.redis.setex(key, 86400, json.dumps(history, ensure_ascii=False))

    async def clear(self, session_id: str):
        await self.redis.delete(f"ai:session:{session_id}")


session_store = SessionStore()
