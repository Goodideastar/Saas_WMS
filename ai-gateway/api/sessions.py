# ai-gateway/api/sessions.py
from fastapi import APIRouter
from models.session import session_store
from tools.registry import registry

router = APIRouter()


@router.get("/tools")
async def list_tools():
    tools = [
        {"name": t.name, "description": t.description, "permission": t.permission}
        for t in registry.list_all()
    ]
    return {"tools": tools}


@router.get("/sessions/{session_id}")
async def get_session(session_id: str):
    history = await session_store.get_history(session_id)
    return {"session_id": session_id, "messages": history}


@router.delete("/sessions/{session_id}")
async def delete_session(session_id: str):
    await session_store.clear(session_id)
    return {"message": "ok"}
