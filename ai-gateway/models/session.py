# ai-gateway/models/session.py
import json
import redis.asyncio as aioredis
from config import settings


class SessionStore:
    def __init__(self):
        self.redis = None
        self._redis_available = False
        self._memory: dict[str, list[dict]] = {}
        try:
            self.redis = aioredis.from_url(settings.redis_url)
        except Exception:
            pass

    async def _check_redis(self):
        if self._redis_available:
            return True
        if self.redis is None:
            return False
        try:
            await self.redis.ping()
            self._redis_available = True
            return True
        except Exception:
            return False

    async def get_history(self, session_id: str) -> list[dict]:
        if await self._check_redis():
            key = f"ai:session:{session_id}"
            data = await self.redis.get(key)
            return json.loads(data) if data else []
        return self._memory.get(session_id, [])

    async def append(self, session_id: str, role: str, content: str):
        if await self._check_redis():
            key = f"ai:session:{session_id}"
            history = await self.get_history(session_id)
            history.append({"role": role, "content": content})
            if len(history) > 40:
                history = history[-40:]
            await self.redis.setex(key, 86400, json.dumps(history, ensure_ascii=False))
        else:
            if session_id not in self._memory:
                self._memory[session_id] = []
            self._memory[session_id].append({"role": role, "content": content})
            if len(self._memory[session_id]) > 40:
                self._memory[session_id] = self._memory[session_id][-40:]

    async def clear(self, session_id: str):
        if await self._check_redis():
            await self.redis.delete(f"ai:session:{session_id}")
        self._memory.pop(session_id, None)


session_store = SessionStore()
