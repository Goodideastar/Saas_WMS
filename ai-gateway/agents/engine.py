import json
from config import settings
from llm.provider import get_provider
from tools.registry import registry
from utils.wms_client import WMSClient
from agents.planner import plan
from agents.executor import execute
from agents.observer import observe


class AgentState:
    PLAN = "plan"
    EXECUTE = "execute"
    OBSERVE = "observe"
    REPLAN = "replan"
    DONE = "done"


async def run_agent(
    user_message: str,
    auth_token: str,
    history: list[dict] | None = None,
):
    provider = get_provider()
    client = WMSClient(auth_token=auth_token)
    messages = history or []
    messages.append({"role": "user", "content": user_message})

    tools_schema = registry.get_schema_for_llm()
    openai_tools = registry.to_openai_tools()

    trace = []
    step_results = []
    loop_count = 0
    state = AgentState.PLAN
    plan_json = {}
    obs = {}

    while loop_count < settings.max_agent_loops:
        loop_count += 1

        if state == AgentState.PLAN:
            plan_json = await plan(provider, messages, tools_schema, step_results)
            trace.append({"type": "plan", "plan": plan_json})
            yield {"type": "plan_start", "plan": plan_json}
            state = AgentState.EXECUTE

        elif state == AgentState.EXECUTE:
            for step in plan_json.get("steps", []):
                tool_name = step["tool"]
                args = step.get("args", {})
                yield {"type": "step_start", "tool": tool_name, "args": args}
                try:
                    result = await execute(tool_name, args, client)
                    step_results.append({"tool": tool_name, "result": result, "success": True})
                    yield {"type": "step_end", "tool": tool_name, "result": result}
                except Exception as e:
                    step_results.append({"tool": tool_name, "error": str(e), "success": False})
                    yield {"type": "error", "tool": tool_name, "message": str(e), "recoverable": True}
            state = AgentState.OBSERVE

        elif state == AgentState.OBSERVE:
            obs = await observe(provider, messages, plan_json, step_results)
            trace.append({"type": "observe", "assessment": obs})
            yield {"type": "observe", "assessment": obs}
            if obs.get("complete"):
                state = AgentState.DONE
            else:
                state = AgentState.REPLAN

        elif state == AgentState.REPLAN:
            missing = obs.get("missing_info", [])
            yield {"type": "replan", "missing": missing}
            plan_json = await plan(provider, messages, tools_schema, step_results, missing=missing)
            trace.append({"type": "replan", "plan": plan_json})
            yield {"type": "replan_start", "plan": plan_json}
            state = AgentState.EXECUTE

        elif state == AgentState.DONE:
            summary = await _summarize(provider, messages, step_results)
            yield {"type": "done", "summary": summary, "trace": trace}
            return

    yield {"type": "error", "message": "Agent loop limit exceeded", "recoverable": False, "trace": trace}


async def _summarize(provider, messages, step_results) -> str:
    prompt = f"根据以下工具调用结果，用自然语言回答用户。结果: {json.dumps(step_results, ensure_ascii=False)}"
    resp = await provider.chat([
        {"role": "system", "content": provider.system_prompt()},
        *messages[-3:],
        {"role": "user", "content": prompt},
    ])
    return resp.choices[0].message.content
