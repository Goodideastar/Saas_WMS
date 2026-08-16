from pydantic_settings import BaseSettings
from pydantic import field_validator

class Settings(BaseSettings):
    llm_provider: str = "qwen"
    llm_api_key: str = ""
    llm_base_url: str = "https://maas-api.cn-huabei-1.xf-yun.com/v2"
    llm_model: str = "xopqwen36v35b"
    wms_base_url: str = "http://localhost:8080"
    redis_url: str = "redis://localhost:6379/0"
    ai_gateway_port: int = 8090
    max_agent_loops: int = 5
    recursion_limit: int = 25

    @field_validator("llm_base_url", mode="before")
    @classmethod
    def validate_base_url(cls, v):
        if not v or not str(v).strip():
            return "https://maas-api.cn-huabei-1.xf-yun.com/v2"
        url = str(v).strip()
        if not url.startswith("http://") and not url.startswith("https://"):
            raise ValueError(f"LLM_BASE_URL must start with http:// or https://, got: {url}")
        return url

    @field_validator("llm_api_key", mode="before")
    @classmethod
    def validate_api_key(cls, v):
        if not v or not str(v).strip():
            raise ValueError("LLM_API_KEY is required (set LLM_API_KEY env var)")
        return str(v).strip()

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"

settings = Settings()
