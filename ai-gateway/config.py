from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    llm_provider: str = "qwen"
    llm_api_key: str = ""
    llm_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    llm_model: str = "qwen-plus"
    wms_base_url: str = "http://localhost:8080"
    redis_url: str = "redis://localhost:6379/0"
    ai_gateway_port: int = 8090
    max_agent_loops: int = 5

    class Config:
        env_file = ".env"

settings = Settings()
