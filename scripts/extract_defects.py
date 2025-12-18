#!/usr/bin/env python3
"""
提取测试用例设计文档中的缺陷信息（包含截图占位的评价字段）并填入缺陷报告文档
"""

import xml.etree.ElementTree as ET
import zipfile
import copy
from pathlib import Path
from typing import List, Dict, Any
import re

NS = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}

def extract_defects_from_testcase() -> List[Dict[str, Any]]:
    """从测试用例设计文档中提取缺陷信息"""
    testcase_path = Path("data/权限系统接口测试用例设计.docx")
    
    with zipfile.ZipFile(testcase_path, "r") as zf:
        doc_xml = zf.read("word/document.xml")
    
    tree = ET.fromstring(doc_xml)
    body = tree.find("w:body", NS)
    
    if body is None:
        return []
    
    defects = []
    
    # 遍历所有表格，查找测试用例表格
    for i, elem in enumerate(body):
        if elem.tag == f"{{{NS['w']}}}tbl":
            text = "".join(elem.itertext())
            # 查找包含"测试用例名称"和"截图占位"的表格
            if "测试用例名称" in text and "截图占位" in text:
                defect_info = extract_defect_from_table(elem, i)
                if defect_info:
                    defects.append(defect_info)
    
    return defects

def extract_defect_from_table(table: ET.Element, table_index: int) -> Dict[str, Any]:
    """从表格中提取缺陷信息"""
    content = {}
    rows = table.findall("w:tr", NS)
    
    if rows is None:
        return {}
    
    # 提取表格内容
    for row in rows:
        cells = row.findall("w:tc", NS)
        if len(cells) >= 2:
            key_text = "".join(cells[0].itertext()).strip() if cells[0] else ""
            value_text = "".join(cells[1].itertext()).strip() if cells[1] else ""
            if key_text:
                content[key_text] = value_text
    
    # 查找测试用例基本信息
    case_name = content.get("测试用例名称", "")
    case_id = content.get("测试用例标识", "")
    test_object = content.get("测试对象", "")
    test_purpose = content.get("测试目的", "")
    
    # 查找评价准则（包含截图占位）
    evaluation = content.get("评价准则", "")
    
    if not evaluation or "截图占位" not in evaluation:
        return {}
    
    return {
        "case_name": case_name,
        "case_id": case_id,
        "test_object": test_object,
        "test_purpose": test_purpose,
        "evaluation": evaluation,
        "content": content,
        "table_index": table_index
    }

def load_defect_report_template() -> Dict[str, Any]:
    """加载缺陷报告模板"""
    report_path = Path("data/2023081023朱奕萌-缺陷报告.docx")
    
    with zipfile.ZipFile(report_path, "r") as zf:
        doc_xml = zf.read("word/document.xml")
    
    tree = ET.fromstring(doc_xml)
    body = tree.find("w:body", NS)
    
    # 查找现有的缺陷表格模板
    defect_tables = []
    if body is not None:
        for elem in body:
            if elem.tag == f"{{{NS['w']}}}tbl":
                text = "".join(elem.itertext())
                if "缺陷编号" in text or "缺陷描述" in text:
                    defect_tables.append(copy.deepcopy(elem))
    
    return {
        "tree": tree,
        "body": body,
        "defect_tables": defect_tables,
        "doc_xml": doc_xml,
        "template_path": report_path
    }

def create_defect_table(template: ET.Element, defect_info: Dict[str, Any]) -> ET.Element:
    """创建缺陷表格"""
    if not template:
        # 创建基本的缺陷表格结构
        return create_simple_defect_table(defect_info)
    
    new_table = copy.deepcopy(template)
    fill_defect_table(new_table, defect_info)
    return new_table

def create_simple_defect_table(defect_info: Dict[str, Any]) -> ET.Element:
    """创建简单的缺陷表格"""
    table = ET.Element(f"{{{NS['w']}}}tbl")
    
    # 创建表格属性
    tbl_pr = ET.SubElement(table, f"{{{NS['w']}}}tblPr")
    tbl_w = ET.SubElement(tbl_pr, f"{{{NS['w']}}}tblW")
    tbl_w.set(f"{{{NS['w']}}}w", "8000")
    tbl_w.set(f"{{{NS['w']}}}type", "dxa")
    
    # 创建表格行
    defect_fields = [
        ("缺陷编号", generate_defect_id()),
        ("缺陷标题", extract_defect_title(defect_info)),
        ("缺陷描述", extract_defect_description(defect_info)),
        ("严重程度", "中等"),
        ("优先级", "中"),
        ("发现阶段", "系统测试"),
        ("发现人", "朱奕萌"),
        ("发现日期", "2025-12-13"),
        ("状态", "待修复"),
        ("修复建议", extract_fix_suggestion(defect_info)),
        ("截图", "[需要插入截图]")
    ]
    
    for field_name, field_value in defect_fields:
        row = ET.SubElement(table, f"{{{NS['w']}}}tr")
        
        # 字段名单元格
        cell1 = ET.SubElement(row, f"{{{NS['w']}}}tc")
        tc_pr1 = ET.SubElement(cell1, f"{{{NS['w']}}}tcPr")
        tc_w1 = ET.SubElement(tc_pr1, f"{{{NS['w']}}}tcW")
        tc_w1.set(f"{{{NS['w']}}}w", "2000")
        tc_w1.set(f"{{{NS['w']}}}type", "dxa")
        set_cell_text(cell1, field_name)
        
        # 字段值单元格
        cell2 = ET.SubElement(row, f"{{{NS['w']}}}tc")
        tc_pr2 = ET.SubElement(cell2, f"{{{NS['w']}}}tcPr")
        tc_w2 = ET.SubElement(tc_pr2, f"{{{NS['w']}}}tcW")
        tc_w2.set(f"{{{NS['w']}}}w", "6000")
        tc_w2.set(f"{{{NS['w']}}}type", "dxa")
        set_cell_text(cell2, field_value)
    
    return table

def fill_defect_table(table: ET.Element, defect_info: Dict[str, Any]) -> None:
    """填充缺陷表格内容"""
    rows = table.findall("w:tr", NS)
    
    if rows is None:
        return
    
    defect_mapping = {
        "缺陷编号": generate_defect_id(),
        "缺陷标题": extract_defect_title(defect_info),
        "缺陷描述": extract_defect_description(defect_info),
        "严重程度": "中等",
        "优先级": "中",
        "发现阶段": "系统测试",
        "发现人": "朱奕萌",
        "发现日期": "2025-12-13",
        "状态": "待修复",
        "修复建议": extract_fix_suggestion(defect_info),
        "截图": "[需要插入截图]"
    }
    
    for row in rows:
        cells = row.findall("w:tc", NS)
        if len(cells) >= 2:
            key_text = "".join(cells[0].itertext()).strip() if cells[0] else ""
            if key_text in defect_mapping:
                set_cell_text(cells[1], defect_mapping[key_text])

def set_cell_text(cell: ET.Element, text: str) -> None:
    """设置单元格文本"""
    if cell is None:
        return
    
    for child in list(cell):
        if child.tag != f"{{{NS['w']}}}tcPr":
            cell.remove(child)
    
    p = ET.SubElement(cell, f"{{{NS['w']}}}p")
    r = ET.SubElement(p, f"{{{NS['w']}}}r")
    t = ET.SubElement(r, f"{{{NS['w']}}}t")
    
    if text.startswith(" "):
        t.set("{http://www.w3.org/XML/1998/namespace}space", "preserve")
    t.text = text

def generate_defect_id() -> str:
    """生成缺陷编号"""
    return f"DEFECT-20251213-{len(defect_list) + 1:03d}"

def extract_defect_title(defect_info: Dict[str, Any]) -> str:
    """提取缺陷标题"""
    case_name = defect_info.get("case_name", "未命名缺陷")
    case_id = defect_info.get("case_id", "")
    if case_id:
        return f"{case_name} ({case_id}) - 需要截图验证"
    return f"{case_name} - 需要截图验证"

def extract_defect_description(defect_info: Dict[str, Any]) -> str:
    """提取缺陷描述"""
    description_parts = []
    
    # 添加测试用例基本信息
    case_id = defect_info.get("case_id", "")
    case_name = defect_info.get("case_name", "")
    test_object = defect_info.get("test_object", "")
    test_purpose = defect_info.get("test_purpose", "")
    
    if case_id and case_name:
        description_parts.append(f"测试用例: {case_id} - {case_name}")
    
    if test_object:
        description_parts.append(f"测试对象: {test_object}")
    
    if test_purpose:
        description_parts.append(f"测试目的: {test_purpose}")
    
    # 添加评价准则（包含截图占位）
    evaluation = defect_info.get("evaluation", "")
    if evaluation:
        description_parts.append(f"评价准则: {evaluation}")
    
    return "\n\n".join(description_parts) if description_parts else "测试过程中发现的缺陷"

def extract_fix_suggestion(defect_info: Dict[str, Any]) -> str:
    """提取修复建议"""
    evaluation = defect_info.get("evaluation", "")
    
    # 根据评价准则中的截图要求生成修复建议
    if "成功登录响应" in evaluation:
        return "需要验证登录接口的响应格式和数据正确性，确保返回的token有效。"
    elif "成功注销响应" in evaluation:
        return "需要验证退出接口能正确使token失效，并处理重复请求。"
    elif "用户详情响应" in evaluation:
        return "需要验证用户详情接口返回的数据完整性和权限控制。"
    elif "分页响应" in evaluation:
        return "需要验证分页查询的参数校验和数据返回格式。"
    elif "课程评分响应" in evaluation:
        return "需要验证课程评分数据的准确性和权限控制。"
    else:
        return "需要按照评价准则中的要求完成相应功能的开发和测试，确保接口响应正确。"

def create_paragraph(text: str, bold: bool = False, align: str | None = None, style: str | None = None) -> ET.Element:
    """创建段落"""
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

def save_defect_report(template: Dict[str, Any], defects: List[Dict[str, Any]], output_path: Path) -> None:
    """保存缺陷报告"""
    tree = template["tree"]
    body = template["body"]
    
    if body is None:
        return
    
    # 清空现有内容
    for child in list(body):
        body.remove(child)
    
    # 添加标题
    body.append(create_paragraph("智慧评教系统缺陷报告", bold=True, align="center", style="Heading1"))
    body.append(create_paragraph("编制日期：2025年12月13日", align="center"))
    body.append(create_paragraph(""))
    
    # 添加概述
    body.append(create_paragraph(f"本报告共包含 {len(defects)} 个需要截图验证的缺陷项目。"))
    body.append(create_paragraph(""))
    
    # 添加缺陷表格
    if template["defect_tables"]:
        table_template = template["defect_tables"][0]
        for i, defect in enumerate(defects):
            if i > 0:
                body.append(create_paragraph(""))
            
            defect_table = create_defect_table(table_template, defect)
            body.append(defect_table)
    else:
        # 如果没有模板，创建简单的表格格式
        for i, defect in enumerate(defects):
            if i > 0:
                body.append(create_paragraph(""))
            
            defect_table = create_simple_defect_table(defect)
            body.append(defect_table)
    
    # 保存文档
    new_xml = ET.tostring(tree, encoding="utf-8", xml_declaration=True)
    
    with zipfile.ZipFile(template["template_path"], "r") as zin:
        with zipfile.ZipFile(output_path, "w") as zout:
            for item in zin.infolist():
                data = zin.read(item.filename)
                if item.filename == "word/document.xml":
                    zout.writestr(item, new_xml)
                else:
                    zout.writestr(item, data)

def main():
    """主函数"""
    global defect_list
    
    print("开始提取缺陷信息...")
    
    # 提取缺陷信息
    defects = extract_defects_from_testcase()
    defect_list = defects
    
    print(f"发现 {len(defects)} 个缺陷")
    
    if not defects:
        print("未发现缺陷信息，检查文档格式")
        return
    
    # 打印缺陷信息概览
    for i, defect in enumerate(defects, 1):
        print(f"缺陷 {i}: {defect.get('case_name', 'Unknown')} ({defect.get('case_id', 'Unknown')})")
        evaluation = defect.get('evaluation', '')
        if '截图占位' in evaluation:
            # 提取截图要求
            screenshot_match = re.search(r'截图占位：请插入"(.*?)"', evaluation)
            if screenshot_match:
                print(f"  需要截图: {screenshot_match.group(1)}")
    
    # 加载缺陷报告模板
    template = load_defect_report_template()
    
    # 保存新的缺陷报告
    output_path = Path("data/2023081023朱奕萌-缺陷报告_更新.docx")
    save_defect_report(template, defects, output_path)
    
    print(f"缺陷报告已生成: {output_path}")

if __name__ == "__main__":
    defect_list = []
    main()