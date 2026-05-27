from tools.registry import registry


async def execute(tool_name: str, args: dict, client) -> dict:
    tool_def = registry.get(tool_name)
    if not tool_def:
        raise ValueError(f"Unknown tool: {tool_name}")

    result = await tool_def.func(client, **args)
    return result
