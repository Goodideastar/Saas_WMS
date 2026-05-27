# ai-gateway/models/schemas.py
from pydantic import BaseModel


class ChatRequest(BaseModel):
    message: str
    session_id: str = "default"


class AnalysisRequest(BaseModel):
    query: str
    context: dict | None = None


class ToolInfo(BaseModel):
    name: str
    description: str
    permission: str


class ToolListResponse(BaseModel):
    tools: list[ToolInfo]
