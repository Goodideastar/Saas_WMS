import json


async def plan(provider, messages, tools_schema, previous_results=None, missing=None) -> dict:
    tools_desc = json.dumps(tools_schema, ensure_ascii=False)
    context = ""
    if previous_results:
        context = f"\n之前的执行结果: {json.dumps(previous_results, ensure_ascii=False)}"
    if missing:
        context += f"\n缺少的信息: {json.dumps(missing, ensure_ascii=False)}"

    prompt = f"""你是一个任务规划器。根据用户意图，制定工具调用计划。

可用工具: {tools_desc}
{context}

返回JSON格式:
{{"goal": "目标描述", "steps": [{{"tool": "工具名", "args": {{参数}}}}], "reason": "规划理由"}}

规则:
1. 只使用列出的工具名，args严格匹配工具参数定义
2. 先读后写，写操作前确认
3. 报表类查询直接用一个工具即可
4. 步骤最少化，不要冗余调用"""

    resp = await provider.chat([
        {"role": "system", "content": provider.system_prompt()},
        *messages[-5:],
        {"role": "user", "content": prompt},
    ])
    content = resp.choices[0].message.content
    content = content.strip().removeprefix("```json").removesuffix("```").strip()
    return json.loads(content)
