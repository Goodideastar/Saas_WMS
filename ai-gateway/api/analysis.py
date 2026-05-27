# ai-gateway/api/analysis.py
from fastapi import APIRouter, Request
from models.schemas import AnalysisRequest
from agents.engine import run_agent

router = APIRouter()


@router.post("/analysis")
async def analysis(req: AnalysisRequest, request: Request):
    auth_token = request.headers.get("Authorization", "").removeprefix("Bearer ")
    results = []
    async for event in run_agent(req.query, auth_token):
        results.append(event)
        if event["type"] == "done":
            return {
                "insight": event["summary"],
                "trace": event.get("trace", []),
            }
    return {"insight": "Analysis failed", "trace": results}
