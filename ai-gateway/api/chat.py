# ai-gateway/api/chat.py
import json
from fastapi import APIRouter, Request
from sse_starlette.sse import EventSourceResponse
from models.schemas import ChatRequest
from models.session import session_store
from agents.engine import run_agent

router = APIRouter()


@router.post("/chat")
async def chat(req: ChatRequest, request: Request):
    auth_token = request.headers.get("Authorization", "").removeprefix("Bearer ")

    async def event_stream():
        try:
            history = await session_store.get_history(req.session_id)
            await session_store.append(req.session_id, "user", req.message)

            full_response = ""
            chart_data = {}
            async for event in run_agent(req.message, auth_token, history):
                if event["type"] == "done":
                    full_response = event.get("summary", "")
                    chart_data = event.get("chart_data", {})
                data = json.dumps(event, ensure_ascii=False)
                yield {"event": "message", "data": data}

            if chart_data:
                yield {"event": "chart_data", "data": json.dumps(chart_data, ensure_ascii=False)}

            if full_response:
                await session_store.append(req.session_id, "assistant", full_response)

        except Exception as e:
            yield {"event": "error", "data": json.dumps({"type": "error", "message": str(e)})}

    return EventSourceResponse(event_stream())
