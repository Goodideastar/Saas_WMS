from tools.decorator import ToolDef


class ToolRegistry:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._tools: dict[str, ToolDef] = {}
        return cls._instance

    def register(self, tool_def: ToolDef):
        self._tools[tool_def.name] = tool_def

    def get(self, name: str) -> ToolDef | None:
        return self._tools.get(name)

    def list_all(self) -> list[ToolDef]:
        return list(self._tools.values())

    def to_openai_tools(self) -> list[dict]:
        return [t.to_openai_schema() for t in self._tools.values()]

    def get_schema_for_llm(self) -> list[dict]:
        """返回简化的工具列表给LLM做planning"""
        return [
            {"name": t.name, "description": t.description, "permission": t.permission}
            for t in self._tools.values()
        ]


registry = ToolRegistry()
