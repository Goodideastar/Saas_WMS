from typing import Callable


class ToolDef:
    def __init__(self, name: str, description: str, parameters: dict, permission: str, func: Callable):
        self.name = name
        self.description = description
        self.parameters = parameters
        self.permission = permission  # "read" | "write"
        self.func = func

    def to_openai_schema(self) -> dict:
        return {
            "type": "function",
            "function": {
                "name": self.name,
                "description": self.description,
                "parameters": self.parameters,
            }
        }


def tool(name: str, description: str, parameters: dict, permission: str = "read"):
    def decorator(func: Callable) -> ToolDef:
        return ToolDef(
            name=name,
            description=description,
            parameters=parameters,
            permission=permission,
            func=func,
        )
    return decorator
