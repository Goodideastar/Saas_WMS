# ai-gateway/main.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from config import settings
from api.chat import router as chat_router
from api.analysis import router as analysis_router
from api.sessions import router as sessions_router
from api.charts import router as charts_router
from tools.product_tools import register_all as register_product
from tools.inbound_tools import register_all as register_inbound
from tools.outbound_tools import register_all as register_outbound
from tools.dashboard_tools import register_all as register_dashboard
from tools.alert_tools import register_all as register_alert

register_product()
register_inbound()
register_outbound()
register_dashboard()
register_alert()

app = FastAPI(title="WMS AI Gateway", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(chat_router, prefix="/ai")
app.include_router(analysis_router, prefix="/ai")
app.include_router(sessions_router, prefix="/ai")
app.include_router(charts_router, prefix="/ai/charts")

@app.get("/health")
async def health():
    return {"status": "ok"}
