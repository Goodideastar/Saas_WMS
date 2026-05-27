import json


async def observe(provider, messages, plan, step_results) -> dict:
    prompt = f"""评估以下工具执行结果是否完成了用户目标。

计划: {json.dumps(plan, ensure_ascii=False)}
执行结果: {json.dumps(step_results, ensure_ascii=False)}

返回JSON:
{{"complete": true/false, "assessment": "评估说明", "missing_info": ["缺少的信息"]}}"""

    resp = await provider.chat([
        {"role": "system", "content": provider.system_prompt()},
        *messages[-5:],
        {"role": "user", "content": prompt},
    ])
    content = resp.choices[0].message.content
    content = content.strip().removeprefix("```json").removesuffix("```").strip()
    return json.loads(content)
