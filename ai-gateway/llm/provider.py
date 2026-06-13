from abc import ABC, abstractmethod
from openai import AsyncOpenAI
from config import settings


class LLMProvider(ABC):
    def __init__(self):
        self.client = AsyncOpenAI(
            api_key=settings.llm_api_key,
            base_url=settings.llm_base_url,
        )
        self.model = settings.llm_model

    @abstractmethod
    def system_prompt(self) -> str: ...

    async def chat(self, messages: list[dict], tools: list[dict] | None = None) -> dict:
        return await self.client.chat.completions.create(
            model=self.model,
            messages=messages,
            tools=tools,
            temperature=0.3,
        )

    async def chat_stream(self, messages: list[dict], tools: list[dict] | None = None):
        return await self.client.chat.completions.create(
            model=self.model,
            messages=messages,
            tools=tools,
            temperature=0.3,
            stream=True,
        )


_provider_instance: "LLMProvider | None" = None


def get_provider() -> "LLMProvider":
    global _provider_instance
    if _provider_instance is not None:
        return _provider_instance
    from llm.qwen import QwenProvider
    from llm.deepseek import DeepSeekProvider

    providers = {"qwen": QwenProvider, "deepseek": DeepSeekProvider}
    cls = providers.get(settings.llm_provider, QwenProvider)
    _provider_instance = cls()
    return _provider_instance
