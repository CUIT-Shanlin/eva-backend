from __future__ import annotations

import copy
import datetime as dt
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any, Dict, List
import zipfile


NS = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}


def load_template(table_keyword: str) -> Dict[str, Any]:
    template_path = Path("data/2023081023朱奕萌-智慧评教系统测试用例设计.docx")
    with zipfile.ZipFile(template_path, "r") as zf:
        doc_xml = zf.read("word/document.xml")
    tree = ET.fromstring(doc_xml)
    body = tree.find("w:body", NS)
    assert body is not None
    table_template = None
    caption_template = None
    for idx, elem in enumerate(list(body)):
        if elem.tag == f"{{{NS['w']}}}tbl":
            text = "".join(elem.itertext())
            if table_keyword in text:
                table_template = copy.deepcopy(elem)
                caption_template = copy.deepcopy(list(body)[idx - 1])
                break
    if table_template is None or caption_template is None:
        raise RuntimeError("未找到表格模板")
    sect_pr = body.find("w:sectPr", NS)
    assert sect_pr is not None
    step_row_template = None
    for row in table_template.findall("w:tr", NS):
        first_cell = row.find("w:tc", NS)
        if first_cell is None:
            continue
        text = "".join(first_cell.itertext()).strip()
        if text.isdigit():
            step_row_template = copy.deepcopy(row)
            break
    if step_row_template is None:
        raise RuntimeError("未找到步骤模板")
    return {
        "tree": tree,
        "body": body,
        "table": table_template,
        "caption": caption_template,
        "sectPr": copy.deepcopy(sect_pr),
        "stepRow": step_row_template,
        "doc_xml": doc_xml,
        "template_path": template_path,
    }


def clear_body(body: ET.Element) -> None:
    children = list(body)
    for child in children:
        body.remove(child)


def set_cell_text(cell: ET.Element, text: str) -> None:
    for child in list(cell):
        if child.tag != f"{{{NS['w']}}}tcPr":
            cell.remove(child)
    p = ET.SubElement(cell, f"{{{NS['w']}}}p")
    r = ET.SubElement(p, f"{{{NS['w']}}}r")
    t = ET.SubElement(r, f"{{{NS['w']}}}t")
    if text.startswith(" "):
        t.set("{http://www.w3.org/XML/1998/namespace}space", "preserve")
    t.text = text


def find_row_by_header(table: ET.Element, header: str) -> ET.Element:
    for row in table.findall("w:tr", NS):
        cell = row.find("w:tc", NS)
        if cell is None:
            continue
        text = "".join(cell.itertext()).strip()
        if text == header:
            return row
    raise KeyError(f"未找到标题为 {header} 的行")


def build_caption(caption_template: ET.Element, index: int, case_name: str) -> ET.Element:
    caption = copy.deepcopy(caption_template)
    for child in list(caption):
        caption.remove(child)
    p_pr = ET.SubElement(caption, f"{{{NS['w']}}}pPr")
    ET.SubElement(p_pr, f"{{{NS['w']}}}pStyle", {f"{{{NS['w']}}}val": "Caption"})
    run = ET.SubElement(caption, f"{{{NS['w']}}}r")
    text = ET.SubElement(run, f"{{{NS['w']}}}t")
    text.text = f"表A.{index} {case_name}测试用例记录表"
    return caption


def clean_step_rows(table: ET.Element) -> None:
    for row in list(table.findall("w:tr", NS)):
        first_cell = row.find("w:tc", NS)
        if first_cell is None:
            continue
        cell_text = "".join(first_cell.itertext()).strip()
        if cell_text.isdigit():
            table.remove(row)


def insert_steps(table: ET.Element, step_template: ET.Element, steps: List[Dict[str, str]]) -> None:
    clean_step_rows(table)
    rows = list(table.findall("w:tr", NS))
    insert_idx = None
    for idx, row in enumerate(rows):
        first_cell = row.find("w:tc", NS)
        if first_cell is None:
            continue
        text = "".join(first_cell.itertext()).strip()
        if text == "假设与约束":
            insert_idx = idx
            break
    if insert_idx is None:
        raise RuntimeError("未找到插入步骤的位置")
    current_idx = insert_idx
    for step in steps:
        new_row = copy.deepcopy(step_template)
        cells = new_row.findall("w:tc", NS)
        set_cell_text(cells[0], step["no"])
        set_cell_text(cells[1], step["operation"])
        set_cell_text(cells[2], step["scene"])
        set_cell_text(cells[3], step["expect"])
        set_cell_text(cells[4], step["check"])
        table.insert(current_idx, new_row)
        current_idx += 1


def fill_table(table_template: ET.Element, step_template: ET.Element, case: Dict[str, Any]) -> ET.Element:
    table = copy.deepcopy(table_template)
    header_row = table.findall("w:tr", NS)[0]
    header_cells = header_row.findall("w:tc", NS)
    set_cell_text(header_cells[1], case["name"])
    set_cell_text(header_cells[3], case["id"])

    set_cell_text(find_row_by_header(table, "测试对象").findall("w:tc", NS)[1], case["object"])
    set_cell_text(find_row_by_header(table, "测试目的").findall("w:tc", NS)[1], case["purpose"])
    set_cell_text(find_row_by_header(table, "测试方法").findall("w:tc", NS)[1], case["method"])
    set_cell_text(find_row_by_header(table, "测试工具").findall("w:tc", NS)[1], case["tool"])
    set_cell_text(find_row_by_header(table, "测试前提条件").findall("w:tc", NS)[1], case["preconditions"])

    insert_steps(table, step_template, case["steps"])

    set_cell_text(find_row_by_header(table, "假设与约束").findall("w:tc", NS)[1], case["assumption"])
    termination_row = find_row_by_header(table, "终止条件")
    termination_cells = termination_row.findall("w:tc", NS)
    set_cell_text(termination_cells[1], case["termination_normal"])

    rows = list(table.findall("w:tr", NS))
    abnormal_row = rows[rows.index(termination_row) + 1]
    set_cell_text(abnormal_row.findall("w:tc", NS)[0], "")
    set_cell_text(abnormal_row.findall("w:tc", NS)[1], case["termination_abnormal"])

    set_cell_text(find_row_by_header(table, "评价准则").findall("w:tc", NS)[1], case["evaluation"])

    footer_row = table.findall("w:tr", NS)[-1]
    footer_cells = footer_row.findall("w:tc", NS)
    set_cell_text(footer_cells[1], case["tester"])
    set_cell_text(footer_cells[3], case["date"])
    return table


def create_paragraph(text: str, bold: bool = False, align: str | None = None, style: str | None = None) -> ET.Element:
    p = ET.Element(f"{{{NS['w']}}}p")
    p_pr = ET.SubElement(p, f"{{{NS['w']}}}pPr")
    if style:
        ET.SubElement(p_pr, f"{{{NS['w']}}}pStyle", {f"{{{NS['w']}}}val": style})
    if align:
        ET.SubElement(p_pr, f"{{{NS['w']}}}jc", {f"{{{NS['w']}}}val": align})
    r = ET.SubElement(p, f"{{{NS['w']}}}r")
    if bold:
        r_pr = ET.SubElement(r, f"{{{NS['w']}}}rPr")
        ET.SubElement(r_pr, f"{{{NS['w']}}}b")
    t = ET.SubElement(r, f"{{{NS['w']}}}t")
    t.text = text
    return p


def save_document(tree: ET.Element, body: ET.Element, sect_pr: ET.Element, caption_template: ET.Element, table_template: ET.Element, step_template: ET.Element, cases: List[Dict[str, Any]], output_path: Path, template_path: Path) -> None:
    clear_body(body)
    today = dt.date.today().strftime("%Y年%m月%d日")
    body.append(create_paragraph("智慧评教系统-权限接口测试用例设计", bold=True, align="center", style="Heading1"))
    body.append(create_paragraph(f"编制日期：{today}", align="center"))
    for idx, case in enumerate(cases, start=1):
        body.append(build_caption(caption_template, idx, case["name"]))
        table = fill_table(table_template, step_template, case)
        body.append(table)
        body.append(create_paragraph(""))
    body.append(copy.deepcopy(sect_pr))

    new_xml = ET.tostring(tree, encoding="utf-8", xml_declaration=True)
    with zipfile.ZipFile(template_path, "r") as zin:
        with zipfile.ZipFile(output_path, "w") as zout:
            for item in zin.infolist():
                data = zin.read(item.filename)
                if item.filename == "word/document.xml":
                    zout.writestr(item, new_xml)
                else:
                    zout.writestr(item, data)


def build_cases() -> List[Dict[str, Any]]:
    base_date = "2025.02.13"
    tester = "朱奕萌"
    cases: List[Dict[str, Any]] = []
    cases.append(
        {
            "name": "登录接口鉴权流程",
            "id": "TE_EVA_AUTH_LOGIN_001",
            "object": "LoginController /login + Sa-Token 会话",
            "purpose": "验证合法教师账号可以正常登录并获取 token，同时异常密码或缺少字段会被拒绝。",
            "method": "场景法、等价类划分",
            "tool": "Apifox 2.5.18、RedisInsight、MySQL 8.0 客户端",
            "preconditions": "测试账号 teacher.zhang(ID=1024) 已在 LDAP 与用户表中启用，密码 Pwd@123456；Redis 中无残留 session；开启 JWT 认证日志。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 打开【登录接口 POST /login】，切换到 Body>JSON，依次输入 username=teacher.zhang、password=Pwd@123456、rememberMe=true，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 响应区显示 HTTP 200，返回体中的 code=200、msg=“成功”，data.token 为 24 位以上字符串，界面状态栏提示请求成功。",
                    "check": "打开 RedisInsight，刷新 satoken:* 列表，看到 satoken:teacher.zhang 新增；随后在 DBeaver 查询 login_log 表，确认记录一条成功登录数据。"
                },
                {
                    "no": "2",
                    "operation": "保持 Apifox 界面不变，仅把 Body 中的 password 改为 Pwd@000000，再次点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 响应区返回 code=400，msg 文本包含“密码错误”，data 区域无 token 字段，HTTP 状态为 400。",
                    "check": "在 DBeaver 打开 login_log 表，刷新后看到新增一条失败记录；RedisInsight 中未生成新的 session。"
                },
                {
                    "no": "3",
                    "operation": "在 Apifox 的 Body 中删除 username 字段，仅保留 password=Pwd@123456、rememberMe=true，然后点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 响应区提示 HTTP 422，返回 code=422，msg 中出现“username 不能为空”，data 为 null。",
                    "check": "RedisInsight 中没有新增 session，DBeaver 中 login_log 表无新增记录。"
                },
            ],
            "assumption": "LDAP、Redis、数据库均保持可用；密码算法固定为 BCrypt。",
            "termination_normal": "注销成功登录产生的 token 并清理 Redis Key。",
            "termination_abnormal": "一旦身份提供者不可达或接口 5xx，立即停止后续步骤并记录线程栈。",
            "evaluation": "以 HTTP code、响应 JSON、Redis 会话键和值、login_log 表记录为准；截图占位：请插入“成功登录响应”界面。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "退出登录接口验证",
            "id": "TE_EVA_AUTH_LOGOUT_002",
            "object": "LoginController /logout + Token 注销",
            "purpose": "验证退出接口能正确使 token 失效，并对重复或缺失凭证的请求给出合理提示。",
            "method": "场景法、状态迁移法",
            "tool": "Apifox 2.5.18、RedisInsight",
            "preconditions": "用户 teacher.zhang 已登录获得 token=Bearer eyJhbGc...ok，Redis 中存在 satoken:teacher.zhang；另外保留一个过期 token=Bearer expired-token。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 选择【退出接口 GET /logout】，在 Header 中填入 Authorization=Bearer eyJhbGc...ok，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 显示 HTTP 200，返回 code=200、msg=“注册成功”、data=null，控制台提示注销成功。",
                    "check": "通过 RedisInsight 删除节点页确认 satoken:teacher.zhang 已被移除。"
                },
                {
                    "no": "2",
                    "operation": "保持 Header 中的 Authorization 不变，再次在 Apifox 点击“发送”调用 /logout。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 401，body 中 code=401 或 msg 提示“token 无效”，且 data 为空。",
                    "check": "检查响应头出现 WWW-Authenticate: Bearer；RedisInsight 中仍无新的 session 生成。"
                },
                {
                    "no": "3",
                    "operation": "在 Apifox 中移除 Header 里的 Authorization 项，再次点击“发送”执行 /logout。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 401，msg 中提示“未登录”，body 中 data=null。",
                    "check": "接口侧仅返回鉴权失败提示，业务日志无实际注销行为。"
                },
            ],
            "assumption": "Sa-Token 的 is-read-cookie=false，所有 token 均走 Header；没有其他后台任务自动清理 token。",
            "termination_normal": "所有用例执行完后，重新登录建立新的 baseline 会话。",
            "termination_abnormal": "若 logout 返回 5xx，立即收集应用日志并停止继续调用。",
            "evaluation": "以 HTTP code、Redis 会话键为依据；截图占位：请插入“成功注销响应”与“401 响应”页面。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "按ID获取用户详情",
            "id": "TE_EVA_USER_DETAIL_003",
            "object": "SysUserController /user/{id}",
            "purpose": "确认带有 system.user.query 权限的 token 能拉取用户、角色、路由与按钮数据，异常 ID 或权限不足会被拦截。",
            "method": "场景法、权限验证",
            "tool": "Apifox 2.5.18、MySQL 8.0 客户端",
            "preconditions": "管理员账号 manager.lin(ID=9001) 拥有 system.user.query；教师账号 teacher.lee 无该权限；数据库已存在 ID=1024 的教师记录。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 选择【GET /user/{id}】，将路径参数 id 填写为 1024，Header 设置 Authorization=Bearer mgr-token，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，body 中 data.info.username=teacher.zhang，同时 routerList、roleList、buttonList 全部展示。",
                    "check": "DBeaver 仅用于比对 sys_user 信息，确认响应字段与数据库一致；截图响应数据用于存档。"
                },
                {
                    "no": "2",
                    "operation": "在 Apifox 中把路径参数改为 999999，并保持 Authorization=Bearer mgr-token，再次点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 404 或 200+业务码 404，body 中 msg=“用户不存在”，data=null。",
                    "check": "DBeaver 对照 sys_user，确认无该 ID 的记录，且无读取锁等待。"
                },
                {
                    "no": "3",
                    "operation": "在 Apifox 将 Authorization 换成 Bearer teacher-token，id 仍为 1024，再次点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 403，body 中 msg 提示“权限不足”，无 info 数据。",
                    "check": "查阅系统权限日志，确认记录一次无权限访问；响应中未泄露敏感字段。"
                },
            ],
            "assumption": "系统使用乐观读，查询不会修改数据；缓存层若命中需保证与数据库一致。",
            "termination_normal": "关闭 Apifox 的 token 自动刷新，保持请求可复现。",
            "termination_abnormal": "若接口 500，导出 user-service 的最近日志后暂停执行。",
            "evaluation": "核对 HTTP code、data.info、roleList、buttonList；截图占位：请插入“用户详情响应”JSON。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "分页获取用户列表",
            "id": "TE_EVA_USER_PAGE_004",
            "object": "SysUserController /users",
            "purpose": "验证分页检索支持关键词与时间区间过滤，同时非法分页参数会被拦截。",
            "method": "场景法、等价类划分",
            "tool": "Apifox 2.5.18、MySQL 8.0 客户端",
            "preconditions": "数据库 sys_user 表存在近 1000 条教师记录；管理 token 拥有 system.user.query 权限。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 进入【POST /users】，Body 中填写 page=1、size=10，queryObj.keyword=“张”，queryObj.startCreateTime=2024-01-01 00:00:00，queryObj.endCreateTime=2024-12-31 23:59:59，startUpdateTime 与 endUpdateTime 同样填入全年范围，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，body 中 records 数量不超过 10，且每条 info.username 都包含“张”，total 大于当前页数量。",
                    "check": "在 DBeaver 打开 MySQL general_log，确认执行的 SQL 包含 LIKE '张%' 与 BETWEEN 条件；记录 current 字段为 1。"
                },
                {
                    "no": "2",
                    "operation": "把 Body 中的 size 改为 0，其余参数保持不变，再次点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 422，body 中 msg 提示“size 必须大于 0”，data=null。",
                    "check": "查看应用日志，未记录任何 SQL 执行；DBeaver 中监控日志无对应查询。"
                },
                {
                    "no": "3",
                    "operation": "在 Body 中把 startCreateTime 改为 2024-12-31 23:59:59，把 endCreateTime 改为 2024-01-01 00:00:00，再点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 400，body 中 msg 提示“开始时间不能晚于结束时间”，data=null。",
                    "check": "DBeaver 中无新增 SQL 记录，说明校验在入参阶段完成。"
                },
            ],
            "assumption": "分页查询默认排序 createTime desc；ES 索引未启用，所有查询直接命中数据库。",
            "termination_normal": "恢复查询条件为默认，避免影响其他测试。",
            "termination_abnormal": "若分页接口响应超 5s，立即收集慢查询日志并停止压力。",
            "evaluation": "以 data.records、total、SQL 日志为主要判断；截图占位：请插入“分页响应”JSON。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "获取教师课程评分",
            "id": "TE_EVA_USER_SCORE_005",
            "object": "ScoreController /user/score/{userId}",
            "purpose": "验证系统可返回指定老师在不同课程下的平均得分，并正确处理非法用户或权限不足。",
            "method": "场景法、数据准确性校验",
            "tool": "Apifox 2.5.18、MySQL 8.0 客户端",
            "preconditions": "课程统计表已有 teacher.zhang 在 2023-2024-1 学期的成绩；管理员 token 拥有 system.user.score.query。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 选择【GET /user/score/{userId}】接口，将 userId 填为 1024，Query 中增加 semId=20241，Header 中使用 Authorization=Bearer mgr-token，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，body.data 是一个数组，包含课程名、score 值以及 evaNum，score 均在 0~100 之间。",
                    "check": "打开 DBeaver，查询 eva_course_score 表中 user_id=1024 且 sem_id=20241 的记录数量，与返回数组长度一致，平均分精度一致。"
                },
                {
                    "no": "2",
                    "operation": "把路径参数 userId 修改为 9999，保持同一 Header，再点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 404 或业务 code=404，msg 提示“未找到该教师”，data 返回空数组。",
                    "check": "DBeaver 查询 eva_course_score 时无记录，符合接口返回；日志中仅记录一次未命中的查询。"
                },
                {
                    "no": "3",
                    "operation": "把 Header 中的 Authorization 改为 Bearer teacher-token（普通教师），保持 userId=1024，点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 403，msg 提示“权限不足”，data 不含课程分数。",
                    "check": "在应用权限日志中确认出现一次 system.user.score.query 权限校验失败。"
                },
            ],
            "assumption": "统计数据每日凌晨同步，不涉及实时写入；score 字段精度为 1 位小数。",
            "termination_normal": "执行结束后释放查询使用的 read-only 连接。",
            "termination_abnormal": "若接口 500，导出 course-score 服务日志后停止执行。",
            "evaluation": "以 data 数组、score 数值及数据库比对结果为准；截图占位：请插入“课程评分响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "获取全部用户基础信息",
            "id": "TE_EVA_USER_LISTALL_006",
            "object": "SysUserController /users/all",
            "purpose": "确认该接口能在权限控制下返回精简的 id+name 列表，用于下拉框缓存。",
            "method": "场景法、权限验证",
            "tool": "Apifox 2.5.18、RedisInsight",
            "preconditions": "数据库 sys_user 表已存在 100+ 教师；管理员 token 拥有 system.user.list；teacher-only token 无该权限。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 里选择【GET /users/all】，在 Header 中写入 Authorization=Bearer mgr-token，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，body.data 为一个含有 id、name 的数组，数量大于 0，结构精简。",
                    "check": "使用 RedisInsight 打开缓存，确认 users:all 键被刷新并包含最新列表；记录响应时间小于 200ms。"
                },
                {
                    "no": "2",
                    "operation": "将 Header 的 Authorization 改为 Bearer teacher-token，再次点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 403，msg 提示“无操作权限”，data 不返回任何列表。",
                    "check": "检查应用日志，没有访问 Redis 缓存，也未输出用户列表；可截图 403 响应。"
                },
                {
                    "no": "3",
                    "operation": "移除 Header 中的 Authorization 字段后再次点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 401，body.msg 提示“未登录”，data=null。",
                    "check": "响应头中出现 WWW-Authenticate: Bearer，说明鉴权在网关阶段拦截。"
                },
            ],
            "assumption": "接口默认开启 10 分钟缓存；无分页。",
            "termination_normal": "清空 users:all 缓存，避免影响后续测试。",
            "termination_abnormal": "若缓存写入失败，记录 Redis 错误并停止。",
            "evaluation": "以 data 数组字段、缓存命中情况为准；截图占位：请插入“全量用户响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "获取当前用户信息",
            "id": "TE_EVA_USER_SELFINFO_007",
            "object": "SysUserController /user/info",
            "purpose": "验证自查询接口返回个人档案、菜单路由与按钮权限，并保障 token 无效时拒绝访问。",
            "method": "场景法、鉴权测试",
            "tool": "Apifox 2.5.18、RedisInsight",
            "preconditions": "teacher.zhang 已登录，token=Bearer teacher-token；准备一个伪造的过期 token=Bearer invalid-token。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 调用【GET /user/info】，Header 设置 Authorization=Bearer teacher-token，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，body.data.info.username=teacher.zhang，并列出 routerList、buttonList。",
                    "check": "核对 routerList 中包含 /dashboard、/system 等菜单，截图响应给前端同学参考。"
                },
                {
                    "no": "2",
                    "operation": "将 Header 改为 Authorization=Bearer invalid-token，再点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 401，msg 提示“token 已失效”，data=null。",
                    "check": "查看应用日志，鉴权阶段直接拒绝，没有触发数据库查询。"
                },
                {
                    "no": "3",
                    "operation": "在 RedisInsight 删除 satoken:teacher.zhang 后，继续在 Apifox 使用 Authorization=Bearer teacher-token 调用该接口。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 401，提示“会话不存在，请重新登录”。",
                    "check": "系统日志记录一次“token 不存在”的告警；Redis 中确实没有该键。"
                },
            ],
            "assumption": "接口无分页，默认依赖 Redis session；路由数据通过权限树生成。",
            "termination_normal": "重新登录恢复 teacher-token。",
            "termination_abnormal": "若接口 5xx，导出 user-service 日志再继续。",
            "evaluation": "依据返回 info、routerList、buttonList 与 HTTP code 判断；截图占位：请插入“个人信息响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "获取用户头像",
            "id": "TE_EVA_USER_AVATAR_008",
            "object": "SysUserController /user/avatar/{id}",
            "purpose": "确认头像接口能返回 Base64 图片并正确处理不存在或未上传头像的情况。",
            "method": "场景法、文件数据验证",
            "tool": "Apifox 2.5.18、MinIO 控制台",
            "preconditions": "用户 1024 已上传 avatar.png；用户 1050 未上传头像；配置默认头像图片。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 调用【GET /user/avatar/{id}】，将 id 设置为 1024，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，响应头 Content-Type=image/png，body.data 是一长串 Base64 字符串。",
                    "check": "把 Base64 粘贴到本地解码工具，还原的图片大小与 MinIO 控制台中 avatar.png 一致，MinIO 访问日志出现一次 GET。"
                },
                {
                    "no": "2",
                    "operation": "把路径参数改为 1050，再次点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 200，但 data 为空字符串或为默认头像的 Base64，提示该用户未上传头像。",
                    "check": "查看应用日志，出现“使用默认头像”提示；MinIO 控制台无新的访问记录。"
                },
                {
                    "no": "3",
                    "operation": "继续将 id 改为 999999，点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 404，msg 显示“用户不存在”，data=null。",
                    "check": "MinIO 日志里没有新的请求，说明在用户校验阶段即被拦截。"
                },
            ],
            "assumption": "头像存储在 MinIO，应用采用 Base64 编码返回；接口无需登录。",
            "termination_normal": "清理本地缓存的 Base64 文件。",
            "termination_abnormal": "若出现 500，抓取 MinIO 与应用日志定位。",
            "evaluation": "以 HTTP code、Content-Type、Base64 内容校验为准；截图占位：请插入“头像响应”截图。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "获取未达标教师TOP列表",
            "id": "TE_EVA_USER_UNQUALIFIED_TOP_009",
            "object": "SysUserController /users/unqualified/{type}/{num}",
            "purpose": "验证接口能按照 type 和 num 参数返回评教/被评教未达标教师列表，并限制非法参数。",
            "method": "场景法、边界值分析",
            "tool": "Apifox 2.5.18、MySQL 8.0 客户端",
            "preconditions": "配置文件 minEvaNum=5、minMyEvaNum=10；数据库 ev_stat_user 表已按学期计算统计。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 调用【GET /users/unqualified/{type}/{num}】，把 type=0、num=5，并在 Query 中填写 semId=20241，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，body.data.dataArr 列表长度不超过 5，且每条记录的 num<5，total 字段显示当前未达标教师总数。",
                    "check": "用 DBeaver 查询统计表（如 ev_stat_user）中 eva_num<5 且 sem_id=20241 的 COUNT 值，与 total 一致。"
                },
                {
                    "no": "2",
                    "operation": "把路径参数改为 type=1、num=3（查询被评教未达标），保留 semId，同样点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回的 dataArr 长度不超过 3，num 值均小于配置 minMyEvaNum=10，department 字段有值。",
                    "check": "DBeaver 查询被评教统计表，确认 num 字段与接口返回匹配，确保 department 对应正确学院。"
                },
                {
                    "no": "3",
                    "operation": "把 type 改为 5（非法值）、num 设为 5，点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 400，msg 显示“type 只能为 0 或 1”，data=null。",
                    "check": "应用日志显示参数校验失败，没有触发数据库查询。"
                },
            ],
            "assumption": "统计数据已预计算；num 参数最大不超过 50。",
            "termination_normal": "无额外动作。",
            "termination_abnormal": "若响应 total 与数据库不符，导出统计表核对后再继续。",
            "evaluation": "以 dataArr、total、SQL 对账结果为准；截图占位：请插入“未达标TOP响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "分页获取未达标教师",
            "id": "TE_EVA_USER_UNQUALIFIED_PAGE_010",
            "object": "SysUserController /users/unqualified/{type}",
            "purpose": "验证分页接口支持 keyword 与 department 条件筛选，并限制非法分页参数。",
            "method": "场景法、等价类划分",
            "tool": "Apifox 2.5.18、MySQL 8.0 客户端",
            "preconditions": "ev_stat_user 表存在不同学院未达标数据；system.user.query 权限可用。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 打开【POST /users/unqualified/{type}】，设置 type=0，在 Body 中录入 page=1、size=8、queryObj.keyword=“王”、queryObj.department=“软件工程学院”，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，records 数量不超过 8 且 department 字段均为软件工程学院，current=1。",
                    "check": "通过 DBeaver 查询 ev_stat_user 表确认筛选条件正确，total 字段与 COUNT(*) 对齐。"
                },
                {
                    "no": "2",
                    "operation": "在同一接口中把 type 改为 1，并在 Query 里新增 semId=20241，Body 改为 page=2、size=5、keyword 置空、department 置空，然后点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回第二页数据，current=2，records=5，且 num 小于 minMyEvaNum。",
                    "check": "DBeaver 中查询相应学期的统计表，确认 offset=5 且 num 值满足阈值。"
                },
                {
                    "no": "3",
                    "operation": "把 Body 里的 size 设为 0，再次点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 422，提示“size 必须大于 0”，data=null。",
                    "check": "应用日志表明未触发 SQL，快速返回参数错误。"
                },
            ],
            "assumption": "分页顺序按照 num 升序；keyword 表示姓名模糊匹配。",
            "termination_normal": "恢复默认 semId 与分页参数。",
            "termination_abnormal": "若出现 500，导出 SQL 与堆栈后暂停测试。",
            "evaluation": "以 records、total、分页信息为准；截图占位：请插入“未达标分页响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "修改本人资料",
            "id": "TE_EVA_USER_SELFUPDATE_011",
            "object": "SysUserController PUT /user/info",
            "purpose": "验证教师可修改自身除头像/密码外的字段，并执行服务器侧校验。",
            "method": "场景法、数据一致性验证",
            "tool": "Apifox 2.5.18、MySQL 8.0 客户端",
            "preconditions": "teacher.zhang 当前信息 phone=13800000001、email=zhang@eva.edu；token=Bearer teacher-token 有效。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 选择【PUT /user/info】，保持 Authorization=Bearer teacher-token，在 Body 中输入 id=1024、phone=13800001111、email=zhang_new@eva.edu、profTitle=副教授，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，msg=“注册成功”，data 为 {}。",
                    "check": "用 DBeaver 打开 sys_user 表，刷新后看到 id=1024 的 phone 和 email 已更新，同时 audit_log 中新增“self-update”记录。"
                },
                {
                    "no": "2",
                    "operation": "保持其他字段不变，仅把 email 改为 invalid-mail，再点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 422，msg 指出“邮箱格式不正确”，data=null。",
                    "check": "DBeaver 中 sys_user 表字段未发生变化，说明修改被拒绝。"
                },
                {
                    "no": "3",
                    "operation": "移除 Header 里的 Authorization，再次点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 401，msg=“尚未登录”，data=null。",
                    "check": "服务器日志仅记录一次鉴权失败，无数据库写入。"
                },
            ],
            "assumption": "接口只允许修改本人信息，忽略 status 等敏感字段。",
            "termination_normal": "将手机号邮箱恢复为原值。",
            "termination_abnormal": "若出现 500，导出 user-service 日志后停止。",
            "evaluation": "以 HTTP code、数据库字段比对为准；截图占位：请插入“修改本人资料响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "管理员修改用户信息",
            "id": "TE_EVA_USER_UPDATE_012",
            "object": "SysUserController PUT /user/{isUpdatePwd}",
            "purpose": "验证管理员可在是否更新密码的两种模式下修改用户档案，非法参数将被拒绝。",
            "method": "场景法、组合测试",
            "tool": "Apifox 2.5.18、MySQL 8.0 客户端",
            "preconditions": "管理员 token=Bearer mgr-token；目标用户 ID=1050；密码采用 BCrypt。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 打开【PUT /user/{isUpdatePwd}】，把路径参数 isUpdatePwd= false，Body 输入 id=1050、department=数据科学学院、status=0、profTitle=讲师，Header 使用管理员 token，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，msg=“成功”。",
                    "check": "DBeaver 查看 sys_user 表，确认 department 字段更新为“数据科学学院”，password 列 hash 未变化。"
                },
                {
                    "no": "2",
                    "operation": "把路径参数 isUpdatePwd 改为 true，Body 仅保留 id=1050、password=NewPwd@123，再次点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，提示重置成功。",
                    "check": "DBeaver 中 password 字段换成新的 BCrypt 值，并在 audit_log 看到“管理员重置密码”记录；使用旧密码尝试登录失败即可佐证。"
                },
                {
                    "no": "3",
                    "operation": "再次调用 isUpdatePwd=false，但故意在 Body 中删除 id 字段，仅传 department，点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 422，msg=“id 不能为空”。",
                    "check": "数据库无任何写入，日志显示参数校验异常。"
                },
            ],
            "assumption": "管理员具备 system.user.update；密码更新后立即失效旧 token。",
            "termination_normal": "通过 SQL 恢复 1050 用户初始密码。",
            "termination_abnormal": "若密码更新失败，收集 error log 并回滚。",
            "evaluation": "以数据库字段变化、login_log 记录为准；截图占位：请插入“管理员修改用户响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "启停用用户状态",
            "id": "TE_EVA_USER_STATUS_013",
            "object": "SysUserController PUT /user/status/{userId}/{status}",
            "purpose": "验证管理员能将用户置为禁用/启用，非法状态值将被拒绝。",
            "method": "场景法、状态迁移法",
            "tool": "Apifox 2.5.18、MySQL 8.0 客户端",
            "preconditions": "用户 ID=1050 当前 status=0；管理员 token 可用。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 选择【PUT /user/status/{userId}/{status}】，输入 userId=1050、status=1，并携带管理员 Authorization，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，msg=“注册成功”。",
                    "check": "用 DBeaver 检查 sys_user.status=1；随后模拟教师 1050 调用任意受控接口，Apifox 返回 403，证明账号被禁用。"
                },
                {
                    "no": "2",
                    "operation": "保持相同接口，把 status 改为 0，重新点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，提示启用成功。",
                    "check": "DBeaver 刷新 sys_user.status，值恢复为 0，教师 1050 再次登录成功。"
                },
                {
                    "no": "3",
                    "operation": "调用相同接口，把 status 改为 3，点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 400，msg=“状态只能是 0 或 1”。",
                    "check": "sys_user 表无任何变动，应用日志记录参数校验错误。"
                },
            ],
            "assumption": "状态变化即时生效，并同步清理用户 token。",
            "termination_normal": "保持用户最终状态为启用(0)。",
            "termination_abnormal": "若状态更新卡住，手工 SQL 回滚并记录日志。",
            "evaluation": "以数据库状态字段、后续登录结果为准；截图占位：请插入“启停用响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "修改个人密码",
            "id": "TE_EVA_USER_CHANGEPWD_014",
            "object": "SysUserController PUT /user/password",
            "purpose": "验证个人改密流程：正确旧密码可更新，新密码与旧密码相同或旧密码错误将被拒绝。",
            "method": "场景法、边界值",
            "tool": "Apifox 2.5.18",
            "preconditions": "teacher.lee 密码为 Pwd@111111；token=Bearer teacher-lee-token。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 打开【PUT /user/password】，Header 写入 Authorization=Bearer teacher-lee-token，在 Body 中填写 oldPassword=Pwd@111111、password=Pwd@222222，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，msg=“成功”，data=null。",
                    "check": "随后使用登录接口尝试：以新密码 Pwd@222222 能成功登录，以旧密码 Pwd@111111 会提示错误；在 DBeaver 查看 sys_user.password，hash 已更新。"
                },
                {
                    "no": "2",
                    "operation": "再次调用该接口，oldPassword=Pwd@222222、password 仍为 Pwd@222222，点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回业务 code=201，msg=“新密码不能和旧密码重复”。",
                    "check": "DBeaver 查看 sys_user.password 未发生变化。"
                },
                {
                    "no": "3",
                    "operation": "再调用一次接口，oldPassword=Pwd@000000（错误值）、password=Pwd@333333，点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 401 或业务提示“旧密码错误”。",
                    "check": "数据库密码保持为 Pwd@222222，对应的登录仍旧需要该密码才能成功。"
                },
            ],
            "assumption": "密码复杂度由前端保障，但后端也校验长度>=6。",
            "termination_normal": "恢复 teacher.lee 的密码为 Pwd@111111。",
            "termination_abnormal": "若密码修改失败，立即停止并人工重置。",
            "evaluation": "以 HTTP code、登录结果为准；截图占位：请插入“修改密码响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "上传个人头像",
            "id": "TE_EVA_USER_UPDATE_AVATAR_015",
            "object": "SysUserController POST /user/info/avatar",
            "purpose": "验证上传头像仅支持规定体积与格式，并将文件同步至存储。",
            "method": "场景法、文件上传验证",
            "tool": "Apifox 2.5.18、MinIO 控制台",
            "preconditions": "teacher.zhang 登录；准备 avatar_ok.jpg(120KB)、avatar_big.jpg(5MB)、avatar.gif。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 切到【POST /user/info/avatar】，选择 Body>Form-Data，将 avatarFile 字段类型设为 File，上传本地 avatar_ok.jpg（120KB），点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，msg=“注册成功”，data={}。",
                    "check": "登录 MinIO 控制台，刷新桶中头像文件，看到时间戳更新；随后调 GET /user/avatar/1024 可以返回新图片。"
                },
                {
                    "no": "2",
                    "operation": "同一接口上传 avatar_big.jpg（5MB），即把 Form-Data 的文件换成大文件，点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 413 或 422，msg 提示“文件超出 2MB 限制”。",
                    "check": "MinIO 控制台未出现新的对象；应用日志记录文件体积校验失败。"
                },
                {
                    "no": "3",
                    "operation": "再次上传 avatar.gif（不支持格式），点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 415，msg=“仅支持 JPG/PNG”或类似提示。",
                    "check": "MinIO 无新增对象，接口日志记录格式校验失败。"
                },
            ],
            "assumption": "最大 2MB，格式限 jpg/png；上传后立即覆盖旧文件。",
            "termination_normal": "保留最新上传的头像供后续测试使用。",
            "termination_abnormal": "若上传失败但文件已写入，手动删除对象并记录。",
            "evaluation": "以 HTTP code、MinIO 对象、/user/avatar 结果为准；截图占位：请插入“上传头像截图”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "删除单个用户",
            "id": "TE_EVA_USER_DELETE_016",
            "object": "SysUserController DELETE /user",
            "purpose": "验证管理员可删除未绑定关键数据的用户，对不存在或仍被引用的用户给出提示。",
            "method": "场景法、约束验证",
            "tool": "Apifox 2.5.18、MySQL 8.0 客户端",
            "preconditions": "候选用户 ID=1200(无排课)、ID=1300(仍绑定课程)；管理员 token 拥有 system.user.delete。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 选择【DELETE /user】，在 Query 参数填写 userId=1200，使用管理员 token 点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，msg=“注册成功”。",
                    "check": "用 DBeaver 查询 sys_user 表，确认 ID=1200 的记录已消失，同时 audit_log 表新增“delete user 1200”记录。"
                },
                {
                    "no": "2",
                    "operation": "再次调用同一接口，仍传 userId=1200，点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 404，msg=“用户不存在”。",
                    "check": "DBeaver 中 sys_user 表无变化，说明接口未重复删除。"
                },
                {
                    "no": "3",
                    "operation": "将 userId 改为 1300（仍绑定课程），再次发送请求。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 409，msg 提示“存在课程数据，无法删除”。",
                    "check": "DBeaver 查看课程关联表，确认仍有外键引用；sys_user 中记录未删。"
                },
            ],
            "assumption": "删除需要同时清理角色关联；数据库开启外键约束。",
            "termination_normal": "重建被删除的测试账号以备下次使用。",
            "termination_abnormal": "若删除操作锁表，手工回滚并记录。",
            "evaluation": "以 HTTP code、数据库记录、日志为准；截图占位：请插入“删除用户响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "新建用户",
            "id": "TE_EVA_USER_CREATE_017",
            "object": "SysUserController POST /user",
            "purpose": "验证管理员可创建新教师账户，并校验用户名唯一与必填字段。",
            "method": "场景法、等价类划分",
            "tool": "Apifox 2.5.18、MySQL 8.0 客户端",
            "preconditions": "管理员 token 可用，准备唯一用户名 \"teacher.new\"。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 打开【POST /user】，Body 中依次填写 username=teacher.new、name=新教师、department=人工智能学院、email=new.teacher@eva.edu、phone=13900002222、status=0、profTitle=讲师、password=Pwd@123456，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，msg=“成功”，data=null。",
                    "check": "使用 DBeaver 查询 sys_user，出现 username=teacher.new 的新记录；sys_user_role 表暂为空。"
                },
                {
                    "no": "2",
                    "operation": "保持 Body 不变再次点击“发送”，模拟重复创建。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 409，msg=“用户名已存在”，data=false。",
                    "check": "DBeaver 中 sys_user 表无重复记录。"
                },
                {
                    "no": "3",
                    "operation": "删除 Body 中的 password 字段，再次点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 422，提示“密码不能为空”。",
                    "check": "数据库未写入任何新记录，返回 data=null。"
                },
            ],
            "assumption": "密码由后端加密，status=0 表示正常。",
            "termination_normal": "删除测试用户 teacher.new。",
            "termination_abnormal": "若创建接口 500，记录请求体与日志后停止。",
            "evaluation": "以 HTTP 响应、数据库记录、唯一性约束为准；截图占位：请插入“创建用户响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "分配角色",
            "id": "TE_EVA_USER_ASSIGN_ROLE_018",
            "object": "SysUserController PUT /user/roles",
            "purpose": "验证管理员能批量绑定角色，空列表或非法角色需提示。",
            "method": "场景法、权限验证",
            "tool": "Apifox 2.5.18、MySQL 8.0 客户端",
            "preconditions": "角色表存在 ID=2(教师)、ID=3(督导)；目标用户 ID=1050；管理员 token 拥有 system.user.assignRole。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 调用【PUT /user/roles】，Body 输入 userId=1050、roleIdList=[2,3]，携带管理员 token 并点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，msg=“成功”。",
                    "check": "DBeaver 查看 user_role 表，新增两条记录，Redis 缓存中与权限相关的键被刷新。"
                },
                {
                    "no": "2",
                    "operation": "把 Body 改成 userId=1050、roleIdList=[]，再次点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 422，msg=“角色列表不能为空”。",
                    "check": "数据库未变，user_role 表仍保留之前的角色。"
                },
                {
                    "no": "3",
                    "operation": "将 roleIdList 改为 [2,9999]，点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 400，msg 提示“角色不存在”。",
                    "check": "应用日志记录校验失败，user_role 表未出现 roleId=9999。"
                },
            ],
            "assumption": "角色与菜单绑定已存在；更新后需刷新缓存。",
            "termination_normal": "恢复用户角色为最初配置。",
            "termination_abnormal": "若 role 绑定失败，记录 SQL 并回滚。",
            "evaluation": "以 HTTP code、user_role 表记录为准；截图占位：请插入“分配角色响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "校验用户名是否存在",
            "id": "TE_EVA_USER_USERNAME_CHECK_019",
            "object": "SysUserController GET /user/username/exist",
            "purpose": "验证用户名查重接口可返回布尔值并限制缺失参数的请求。",
            "method": "场景法、边界值",
            "tool": "Apifox 2.5.18",
            "preconditions": "数据库存在用户名 teacher.zhang；管理员 token 拥有 system.user.isExist。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 打开【GET /user/username/exist】，在 Query 中输入 username=teacher.zhang，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，data=true，表示已存在。",
                    "check": "响应耗时 <100ms，截图提供给前端校验。"
                },
                {
                    "no": "2",
                    "operation": "把 username 改为 teacher.future，再次点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 data=false，提示可用。",
                    "check": "用于验证前端提示时截图保存。"
                },
                {
                    "no": "3",
                    "operation": "清空 Query 参数 username，直接点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 422，msg=“username 不能为空”。",
                    "check": "日志显示缺少必须参数；接口未进行查库。"
                },
            ],
            "assumption": "接口不需要分页；命中缓存，延迟极低。",
            "termination_normal": "无额外动作。",
            "termination_abnormal": "若接口 500，导出日志并停止。",
            "evaluation": "以 data 布尔值、响应时间为准；截图占位：请插入“用户名查重响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "批量导入成员",
            "id": "TE_EVA_USER_IMPORT_020",
            "object": "SysUserController PUT /user/import",
            "purpose": "验证 CSV 导入能够新增多名教师，并对格式/重复用户名进行提示。",
            "method": "场景法、文件上传验证",
            "tool": "Apifox 2.5.18、Excel/CSV 校验工具",
            "preconditions": "准备 valid_users.csv(3 条唯一记录)、dup_users.csv(含已存在用户名)、invalid.txt；管理员 token 拥有 system.user.import。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 打开【PUT /user/import】，切到 Form-Data，把 file 字段设置为文件并上传 valid_users.csv（包含 3 条记录），点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，msg=“注册成功”，data.successCount=3。",
                    "check": "在 DBeaver 使用 SELECT 查询 sys_user，找到 3 条新用户；接口返回的 successCount 与数据库一致。"
                },
                {
                    "no": "2",
                    "operation": "更换上传文件为 dup_users.csv（内含 username=teacher.zhang），再次点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 409，msg 提示“用户名重复”，data.successCount < 文件总行数。",
                    "check": "DBeaver 中只新增未重复的记录，teacher.zhang 未被覆盖。"
                },
                {
                    "no": "3",
                    "operation": "上传 invalid.txt（非 CSV），点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 415，msg=“仅支持 csv 模板”。",
                    "check": "应用日志记录文件类型不正确，数据库无写入。"
                },
            ],
            "assumption": "CSV 列顺序遵循模板：username,name,department,email,phone。",
            "termination_normal": "删除导入的测试账号。",
            "termination_abnormal": "若导入过程中 500，保留 csv 与日志排查。",
            "evaluation": "以响应 successCount、数据库记录为准；截图占位：请插入“导入响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "同步LDAP用户",
            "id": "TE_EVA_USER_SYNC_021",
            "object": "SysUserController POST /user/sync",
            "purpose": "验证同步接口能将 LDAP 中的新增账号导入本地库，异常网络时给出提示。",
            "method": "场景法、接口健壮性",
            "tool": "Apifox 2.5.18、LDAP Browser、MySQL 8.0",
            "preconditions": "LDAP server 中存在新条目 cn=teacher.ldap；管理员 token 拥有 system.user.sync。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 选中【POST /user/sync】接口，使用管理员 token 点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，msg=“注册成功”，data 里统计新增数量。",
                    "check": "通过 DBeaver 查询 sys_user，发现新增用户名 teacher.ldap；应用日志记录同步总数。"
                },
                {
                    "no": "2",
                    "operation": "立即再次调用同一接口。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，但 data 中新增数量为 0。",
                    "check": "日志中出现“无新增用户”提示；数据库记录保持不变。"
                },
                {
                    "no": "3",
                    "operation": "暂时关闭 LDAP 服务（或修改 hosts 使之不可达），随后再次在 Apifox 点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 504 或 500，msg 提示“无法连接 LDAP”。",
                    "check": "接口耗时 >3s，error log 中记录连接失败堆栈。"
                },
            ],
            "assumption": "同步仅新增不删除；使用分页批量拉取 LDAP。",
            "termination_normal": "删除测试同步的 teacher.ldap 账号。",
            "termination_abnormal": "若 LDAP 无法恢复，停止后续测试并记录。",
            "evaluation": "以 HTTP code、数据库新增条目、日志统计为准；截图占位：请插入“同步响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "获取树型菜单数据",
            "id": "TE_EVA_MENU_TREE_022",
            "object": "SysMenuController POST /menus/tree",
            "purpose": "验证目录树查询支持关键字与状态过滤，返回结构化 children 列表。",
            "method": "场景法、树结构校验",
            "tool": "Apifox 2.5.18",
            "preconditions": "菜单表中存在“系统管理”父节点及多个子节点；管理员 token 拥有 system.menu.tree。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 调用【POST /menus/tree】，Body 填写 keyword=系统、status=1，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，data 中包含“系统管理”父节点以及 children 列表。",
                    "check": "检查返回的树结构 type=0/1/2 分层正确；截图提供给前端核对。"
                },
                {
                    "no": "2",
                    "operation": "把 keyword 改成 查看、status 改为 0，再次点击“发送”。",
                    "scene": "正常场景",
                    "expect": "返回的 data 只包含停用状态的查看按钮节点及其完整父链。",
                    "check": "核对响应中 parentName 与数据库一致，确认父节点链条齐全。"
                },
                {
                    "no": "3",
                    "operation": "将 status 改为 3 后再次发送请求。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 422，msg=“状态值非法”。",
                    "check": "应用日志显示入参校验失败，没有执行数据库查询。"
                },
            ],
            "assumption": "接口默认根据 sort 字段排序；返回 JSON 与前端树组件兼容。",
            "termination_normal": "无额外动作。",
            "termination_abnormal": "若返回结构断层，导出菜单表供排查。",
            "evaluation": "以 data 结构与节点数量为准；截图占位：请插入“菜单树响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "获取单个菜单详情",
            "id": "TE_EVA_MENU_DETAIL_023",
            "object": "SysMenuController GET /menu",
            "purpose": "验证菜单详情接口返回路径、组件、权限标识等信息，非法 ID 或权限不足时报错。",
            "method": "场景法、权限验证",
            "tool": "Apifox 2.5.18",
            "preconditions": "菜单 ID=21 存在；管理员拥有 system.menu.query；普通教师无该权限。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 打开【GET /menu】，在 Query 中输入 id=21，Header 使用管理员 token，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，data 中含 path、component、perms 等信息。",
                    "check": "通过 DBeaver 查询 sys_menu 表，确认 parentId、perms 与接口一致。"
                },
                {
                    "no": "2",
                    "operation": "把 Query 参数改成 id=9999，点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 404 或业务提示“菜单不存在”。",
                    "check": "数据库无该记录，接口未泄露任何数据。"
                },
                {
                    "no": "3",
                    "operation": "再次把 id 设为 21，但把 Authorization 改成 teacher-token，点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 403，msg=“无权访问”。",
                    "check": "响应中 data=null，没有返回菜单详情。"
                },
            ],
            "assumption": "查询仅支持单个 ID；无缓存。",
            "termination_normal": "无额外动作。",
            "termination_abnormal": "若接口 500，导出菜单服务日志。",
            "evaluation": "以 HTTP code、data 字段为准；截图占位：请插入“菜单详情响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "修改菜单信息",
            "id": "TE_EVA_MENU_UPDATE_024",
            "object": "SysMenuController PUT /menu",
            "purpose": "验证管理员可修改菜单名称、路径、权限标识，重复路径会被拒绝。",
            "method": "场景法、数据校验",
            "tool": "Apifox 2.5.18、MySQL 8.0",
            "preconditions": "菜单 ID=21 为“课程列表”，当前 path=/course/list；管理员 token 拥有 system.menu.update。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 调用【PUT /menu】，Body 输入 id=21、name=课程总览、path=/course/overview、component=system/course/index、perms=course.tabulation.query、status=1，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，msg=“成功”。",
                    "check": "使用 DBeaver 查看 sys_menu 表，path 与 component 均更新；前端刷新菜单后名称显示为课程总览。"
                },
                {
                    "no": "2",
                    "operation": "把 Body 中 path 改为已存在的 /system/user，再点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 409，msg=“路由路径重复”。",
                    "check": "数据库保持原状，未被覆盖。"
                },
                {
                    "no": "3",
                    "operation": "删除 Body 中的 id 字段，仅保留其他字段后发送请求。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 422，提示“菜单 ID 不能为空”。",
                    "check": "sys_menu 表无任何修改记录。"
                },
            ],
            "assumption": "修改时父节点合法；perms 需唯一。",
            "termination_normal": "将菜单恢复为初始配置。",
            "termination_abnormal": "若修改失败导致缓存脏数据，手动刷新缓存后记录。",
            "evaluation": "以 HTTP code、菜单表字段为准；截图占位：请插入“修改菜单响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "删除单个菜单",
            "id": "TE_EVA_MENU_DELETEONE_025",
            "object": "SysMenuController DELETE /menu/{menuId}",
            "purpose": "验证仅叶子节点允许删除，存在子节点时必须阻止。",
            "method": "场景法、约束验证",
            "tool": "Apifox 2.5.18、MySQL 8.0",
            "preconditions": "菜单 ID=45(叶子按钮)、ID=5(有子节点)；管理员具备 system.menu.delete。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 请求【DELETE /menu/{menuId}】，把 menuId=45（叶子按钮），点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，msg=“成功”。",
                    "check": "DBeaver 查看 sys_menu 表，ID=45 的记录被删除，sys_role_menu 关联同步清理。"
                },
                {
                    "no": "2",
                    "operation": "再次调用接口，把 menuId=5（有子节点），点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 409，提示“仍存在子菜单，无法删除”。",
                    "check": "数据库保持原状，ID=5 未被删除。"
                },
                {
                    "no": "3",
                    "operation": "将 menuId 改为 9999（不存在），再次发送。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 404，msg=“菜单不存在”。",
                    "check": "无数据库变化，应用日志记录一次未找到记录。"
                },
            ],
            "assumption": "删除需同步刷新缓存；子节点检测依赖数据库。",
            "termination_normal": "重新插入被删的测试按钮。",
            "termination_abnormal": "若角色关联未清理，手动清除并记录。",
            "evaluation": "以 HTTP code、数据库记录为准；截图占位：请插入“删除菜单响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "批量删除菜单",
            "id": "TE_EVA_MENU_DELETEBATCH_026",
            "object": "SysMenuController DELETE /menus",
            "purpose": "验证批量删除仅允许传入同层级叶子节点，空数组或非法节点需提示。",
            "method": "场景法、参数校验",
            "tool": "Apifox 2.5.18",
            "preconditions": "叶子菜单 ID=[60,61]；ID=70 仍有子节点。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 调用【DELETE /menus】，Body 填写数组 [60,61]，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，msg=“成功”。",
                    "check": "DBeaver 检查 sys_menu 表，ID 为 60、61 的记录消失，sys_role_menu 中对应行同步被删。"
                },
                {
                    "no": "2",
                    "operation": "把 Body 改成空数组[]，再次发送请求。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 422，提示“菜单 ID 列表不能为空”。",
                    "check": "sys_menu 表无删除动作。"
                },
                {
                    "no": "3",
                    "operation": "将 Body 改成 [70]（含子节点），点击“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 409，提示“存在子节点”。",
                    "check": "数据库未删除 ID=70，日志记录事务回滚。"
                },
            ],
            "assumption": "批量删除使用事务，任一失败整体回滚。",
            "termination_normal": "恢复已删除的测试菜单。",
            "termination_abnormal": "若事务回滚失败，手工检查残留数据。",
            "evaluation": "以 HTTP 响应、数据库记录为准；截图占位：请插入“批量删除响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    cases.append(
        {
            "name": "新建菜单",
            "id": "TE_EVA_MENU_CREATE_027",
            "object": "SysMenuController POST /menu",
            "purpose": "验证创建菜单时需填写名称、类型、父节点，路径/权限唯一且状态合法。",
            "method": "场景法、数据校验",
            "tool": "Apifox 2.5.18、MySQL 8.0",
            "preconditions": "父节点 ID=1(系统管理)；计划新增 path=/system/audit。",
            "steps": [
                {
                    "no": "1",
                    "operation": "在 Apifox 调用【POST /menu】，Body 输入 name=审计日志、type=1、path=/system/audit、component=system/log/audit、perms=system.audit.query、icon=&#xe6aa;、status=1、parentId=1，点击“发送”。",
                    "scene": "正常场景",
                    "expect": "Apifox 返回 HTTP 200，msg=“成功”。",
                    "check": "DBeaver 查询 sys_menu 表，出现新的记录且 parentId=1。"
                },
                {
                    "no": "2",
                    "operation": "删除 Body 中的 name 字段，再次“发送”。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 422，提示“名称不能为空”。",
                    "check": "数据库未写入新记录。"
                },
                {
                    "no": "3",
                    "operation": "把 name 恢复、perms 保持 system.audit.query（与第一步重复），再次发送请求。",
                    "scene": "异常场景",
                    "expect": "Apifox 返回 HTTP 409，msg=“权限标识已存在”。",
                    "check": "sys_menu 表没有新增重复的 perms，说明校验生效。"
                },
            ],
            "assumption": "type=0/1/2；父节点必须存在；perms 唯一。",
            "termination_normal": "删除新增的“审计日志”菜单，保持环境整洁。",
            "termination_abnormal": "若创建失败产生半成品节点，手工清理后记录。",
            "evaluation": "以 HTTP code、数据库记录为准；截图占位：请插入“新建菜单响应”。",
            "tester": tester,
            "date": base_date,
        }
    )
    return cases


def main() -> None:
    cases = build_cases()
    template = load_template("测试用例名称")
    output_path = Path("data/权限系统接口测试用例设计.docx")
    save_document(
        template["tree"],
        template["body"],
        template["sectPr"],
        template["caption"],
        template["table"],
        template["stepRow"],
        cases,
        output_path,
        template["template_path"],
    )
    print(f"生成 {output_path} 成功，覆盖 {len(cases)} 条测试用例。")


if __name__ == "__main__":
    main()
