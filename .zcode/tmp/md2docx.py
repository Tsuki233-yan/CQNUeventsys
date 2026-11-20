# -*- coding: utf-8 -*-
"""md2docx.py — 把 GFM Markdown 转换为排版良好的 Word (.docx)。

仅做排版转换，不改动原文内容与措辞。

用法:
    python md2docx.py <in.md> <out.docx> [<in.md> <out.docx> ...]

排版约定:
    - #/##/###        -> Word 内置 Heading 1/2/3（黑体加粗、黑色；H1 居中）
    - 普通段落        -> 中文宋体（w:eastAsia=宋体）、西文 Times New Roman、小四 12pt、
                         1.5 倍行距、两端对齐
    - 无序列表        -> List Bullet 样式
    - 有序列表        -> 真实 Word 自动编号；每个列表组新建独立编号定义，从 1 重新计数
    - GFM 表格        -> Word 表格：Table Grid + 显式单线边框；表头加粗、浅灰底纹、跨页重复；
                         表内文字五号 10.5pt
    - 行内 **加粗**、*斜体*、`代码` -> 加粗 / 斜体 / Consolas 等宽（支持嵌套，如 **含`代码`**）
    - > 引用块        -> 左侧灰色竖线 + 缩进的引用段落
    - ---             -> 水平分隔线（段落下边框）
    - 列表项的缩进续行 -> 与列表文字对齐的续行段落
    - 页面            -> A4，上下 2.54cm，左右 3.18cm
"""

import re
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

SONG = "宋体"          # 正文中文字体
HEI = "黑体"           # 标题中文字体
CODE = "Consolas"      # 行内代码字体
LATIN = "Times New Roman"  # 正文西文字体

BODY_SIZE = 12.0       # 小四
TABLE_SIZE = 10.5      # 五号
HEADING_SIZES = {1: 18.0, 2: 15.0, 3: 14.0}  # 小二 / 小三 / 四号

HEADING_PAT = re.compile(r"^(#{1,6})\s+(.*)$")
UL_PAT = re.compile(r"^(\s*)[-*+]\s+(.*)$")
OL_PAT = re.compile(r"^(\s*)(\d{1,3})[.)]\s+(.*)$")
HR_PAT = re.compile(r"^(?:-{3,}|\*{3,}|_{3,})$")
INLINE_PAT = re.compile(r"\*\*(.+?)\*\*|`([^`\n]+)`|\*([^*\n]+)\*")


# ---------------- 行内解析：**加粗**、*斜体*、`代码`（支持嵌套） ----------------
def parse_inline(text, fmt=None):
    """把一行文本解析为 [(片段, {bold/italic/code 标记}), ...]，剥离行内语法标记。"""
    if fmt is None:
        fmt = {}
    segs = []
    idx = 0
    for m in INLINE_PAT.finditer(text):
        if m.start() > idx:
            segs.append((text[idx:m.start()], dict(fmt)))
        sub = dict(fmt)
        if m.group(1) is not None:          # **加粗**（内部可再含 `代码`）
            sub["bold"] = True
            segs.extend(parse_inline(m.group(1), sub))
        elif m.group(2) is not None:        # `代码`
            sub["code"] = True
            segs.append((m.group(2), sub))
        else:                               # *斜体*（内部可再含 `代码`）
            sub["italic"] = True
            segs.extend(parse_inline(m.group(3), sub))
        idx = m.end()
    if idx < len(text):
        segs.append((text[idx:], dict(fmt)))
    return segs


# ---------------- GFM 表格行辅助 ----------------
def split_row(line):
    s = line.strip()
    if s.startswith("|"):
        s = s[1:]
    if s.endswith("|") and not s.endswith("\\|"):
        s = s[:-1]
    parts = s.replace("\\|", "\x00").split("|")
    return [p.replace("\x00", "|").strip() for p in parts]


def is_sep_row(line):
    """分隔行形如 |---|---| / | :---: | ---: |（必须以 | 开头，与水平线 --- 区分）。"""
    s = line.strip()
    if not s.startswith("|"):
        return False
    cells = split_row(s)
    return len(cells) >= 1 and all(re.fullmatch(r":?-{1,}:?", c) for c in cells)


def align_of(sep_cell):
    c = sep_cell.strip()
    if c.startswith(":") and c.endswith(":"):
        return WD_ALIGN_PARAGRAPH.CENTER
    if c.endswith(":"):
        return WD_ALIGN_PARAGRAPH.RIGHT
    return WD_ALIGN_PARAGRAPH.LEFT


# ---------------- 底层 XML 辅助 ----------------
# insert_element_before 内部会自行调用 qn()，这里传普通标签名字符串
_PPR_SUCCESSORS = (
    "w:shd", "w:tabs", "w:suppressAutoHyphens", "w:kinsoku",
    "w:wordWrap", "w:overflowPunct", "w:topLinePunct",
    "w:autoSpaceDE", "w:autoSpaceDN", "w:bidi",
    "w:adjustRightInd", "w:snapToGrid", "w:spacing",
    "w:ind", "w:contextualSpacing", "w:mirrorIndents",
    "w:suppressOverlap", "w:jc", "w:textDirection",
    "w:textAlignment", "w:textboxTightWrap", "w:outlineLvl",
    "w:divId", "w:cnfStyle", "w:rPr", "w:sectPr", "w:pPrChange",
)


def _paragraph_border(p, edges):
    """给段落加 pBdr 边框（引用块左竖线 / 水平分隔线下边框）。"""
    pPr = p._p.get_or_add_pPr()
    pbdr = OxmlElement("w:pBdr")
    for edge, attrs in edges.items():
        el = OxmlElement("w:" + edge)
        for k, v in attrs.items():
            el.set(qn("w:" + k), v)
        pbdr.append(el)
    pPr.insert_element_before(pbdr, *_PPR_SUCCESSORS)


def new_num_id(doc):
    """为新的有序列表组创建独立编号定义（十进制、从 1 重新计数），返回 numId。"""
    numbering = doc.part.numbering_part.element
    abs_ids = [int(e.get(qn("w:abstractNumId"))) for e in numbering.findall(qn("w:abstractNum"))]
    num_ids = [int(e.get(qn("w:numId"))) for e in numbering.findall(qn("w:num"))]
    abs_id = (max(abs_ids) + 1) if abs_ids else 0
    num_id = (max(num_ids) + 1) if num_ids else 1

    abstract = OxmlElement("w:abstractNum")
    abstract.set(qn("w:abstractNumId"), str(abs_id))
    mlt = OxmlElement("w:multiLevelType")
    mlt.set(qn("w:val"), "multilevel")
    abstract.append(mlt)
    for ilvl in range(3):
        lvl = OxmlElement("w:lvl")
        lvl.set(qn("w:ilvl"), str(ilvl))
        start = OxmlElement("w:start"); start.set(qn("w:val"), "1")
        numFmt = OxmlElement("w:numFmt"); numFmt.set(qn("w:val"), "decimal")
        lvlText = OxmlElement("w:lvlText"); lvlText.set(qn("w:val"), "%" + str(ilvl + 1) + ".")
        lvlJc = OxmlElement("w:lvlJc"); lvlJc.set(qn("w:val"), "left")
        for e in (start, numFmt, lvlText, lvlJc):
            lvl.append(e)
        pPr = OxmlElement("w:pPr")
        ind = OxmlElement("w:ind")
        ind.set(qn("w:left"), str(720 + 360 * ilvl))
        ind.set(qn("w:hanging"), "360")
        pPr.append(ind)
        lvl.append(pPr)
        abstract.append(lvl)
    first_num = numbering.find(qn("w:num"))
    if first_num is not None:
        first_num.addprevious(abstract)   # schema: 所有 w:abstractNum 必须在 w:num 之前
    else:
        numbering.append(abstract)
    num = OxmlElement("w:num")
    num.set(qn("w:numId"), str(num_id))
    aref = OxmlElement("w:abstractNumId")
    aref.set(qn("w:val"), str(abs_id))
    num.append(aref)
    numbering.append(num)
    return num_id


def set_paragraph_numbering(p, num_id):
    pPr = p._p.get_or_add_pPr()
    numPr = pPr.get_or_add_numPr()
    ilvl = OxmlElement("w:ilvl"); ilvl.set(qn("w:val"), "0")
    numIdEl = OxmlElement("w:numId"); numIdEl.set(qn("w:val"), str(num_id))
    numPr.append(ilvl)
    numPr.append(numIdEl)


def set_table_borders(table):
    """显式设置全表单线边框（外框 + 内部横竖线），不依赖样式表。"""
    tblPr = table._tbl.tblPr
    borders = OxmlElement("w:tblBorders")
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        el = OxmlElement("w:" + edge)
        el.set(qn("w:val"), "single")
        el.set(qn("w:sz"), "4")          # 0.5pt
        el.set(qn("w:space"), "0")
        el.set(qn("w:color"), "auto")
        borders.append(el)
    tblPr.insert_element_before(
        borders,
        "w:shd", "w:tblLayout", "w:tblCellMar",
        "w:tblLook", "w:tblCaption", "w:tblDescription",
    )


def shade_cell(cell, fill):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), fill)
    tcPr.append(shd)


def mark_header_row(table):
    trPr = table.rows[0]._tr.get_or_add_trPr()
    th = OxmlElement("w:tblHeader")
    th.set(qn("w:val"), "true")
    trPr.append(th)


# ---------------- run / 段落构建 ----------------
def style_run(run, fmt, size_pt, heading=False, force_bold=False):
    f = run.font
    if fmt.get("code"):
        latin = CODE
    elif heading:
        latin = HEI
    else:
        latin = LATIN
    f.name = latin                        # 设置 w:ascii 与 w:hAnsi
    rPr = run._element.get_or_add_rPr()
    rPr.get_or_add_rFonts().set(qn("w:eastAsia"), HEI if heading else SONG)  # 关键：eastAsia
    f.size = Pt(size_pt)
    if fmt.get("bold") or force_bold:
        f.bold = True
    if fmt.get("italic"):
        f.italic = True
    if heading:
        f.color.rgb = RGBColor(0, 0, 0)   # 标题黑色（覆盖模板主题色）


def add_runs(p, segs, size_pt, heading=False, force_bold=False):
    for text, fmt in segs:
        if text == "":
            continue
        r = p.add_run(text)
        style_run(r, fmt, size_pt, heading=heading, force_bold=force_bold)


def add_heading(doc, level, text):
    segs = parse_inline(text)
    p = None
    if level <= 6:
        try:
            p = doc.add_paragraph(style="Heading %d" % level)
        except KeyError:
            p = None
    if p is None:
        p = doc.add_paragraph()
        pf = p.paragraph_format
        pf.space_before = Pt(12)
        pf.space_after = Pt(6)
        pf.line_spacing = 1.2
    if level == 1:
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    add_runs(p, segs, HEADING_SIZES.get(level, 14), heading=True, force_bold=True)
    return p


def add_body(doc, segs, size_pt=BODY_SIZE, align=WD_ALIGN_PARAGRAPH.JUSTIFY,
             indent_pt=None, line_spacing=1.5, space_after_pt=6):
    p = doc.add_paragraph()
    pf = p.paragraph_format
    if align is not None:
        p.alignment = align
    if indent_pt is not None:
        pf.left_indent = Pt(indent_pt)
    pf.line_spacing = line_spacing
    pf.space_after = Pt(space_after_pt)
    add_runs(p, segs, size_pt)
    return p


def add_hr(doc):
    p = doc.add_paragraph()
    pf = p.paragraph_format
    pf.space_before = Pt(6)
    pf.space_after = Pt(6)
    pf.line_spacing = 1.0
    _paragraph_border(p, {"bottom": {"val": "single", "sz": "6", "space": "1", "color": "999999"}})
    return p


def add_quote(doc, segs):
    p = doc.add_paragraph()
    pf = p.paragraph_format
    pf.left_indent = Pt(18)
    pf.space_before = Pt(3)
    pf.space_after = Pt(3)
    pf.line_spacing = 1.5
    _paragraph_border(p, {"left": {"val": "single", "sz": "18", "space": "4", "color": "BFBFBF"}})
    add_runs(p, segs, BODY_SIZE)
    return p


def add_bullet(doc, segs, use_style):
    p = doc.add_paragraph(style="List Bullet") if use_style else doc.add_paragraph()
    if not use_style:                     # 手工项目符号兜底
        segs = [("• ", {})] + segs
        pf0 = p.paragraph_format
        pf0.left_indent = Pt(36)
        pf0.first_line_indent = Pt(-18)
    pf = p.paragraph_format
    pf.line_spacing = 1.5
    pf.space_after = Pt(4)
    add_runs(p, segs, BODY_SIZE)
    return p


def add_ordered(doc, segs, num_id, counter):
    p = doc.add_paragraph()
    pf = p.paragraph_format
    pf.left_indent = Pt(36)               # 720 twips
    pf.first_line_indent = Pt(-18)        # hanging 360 twips
    pf.line_spacing = 1.5
    pf.space_after = Pt(4)
    if num_id is not None:
        set_paragraph_numbering(p, num_id)
    else:
        segs = [("%d. " % counter, {})] + segs   # 编号部件不可用时按原文数字兜底
    add_runs(p, segs, BODY_SIZE)
    return p


def add_gfm_table(doc, header, rows, aligns):
    ncols = max(len(header), max((len(r) for r in rows), default=0))
    header = list(header) + [""] * (ncols - len(header))
    norm_rows = [list(r) + [""] * (ncols - len(r)) for r in rows]
    table = doc.add_table(rows=1 + len(norm_rows), cols=ncols)
    try:
        table.style = "Table Grid"
    except KeyError:
        pass
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = True
    set_table_borders(table)
    mark_header_row(table)
    for j, cell_text in enumerate(header):
        cell = table.cell(0, j)
        shade_cell(cell, "EDEDED")
        p = cell.paragraphs[0]
        pf = p.paragraph_format
        pf.space_before = Pt(2)
        pf.space_after = Pt(2)
        pf.line_spacing = 1.0
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        add_runs(p, parse_inline(cell_text), TABLE_SIZE, force_bold=True)
    for i, row in enumerate(norm_rows, start=1):
        for j, cell_text in enumerate(row):
            cell = table.cell(i, j)
            p = cell.paragraphs[0]
            pf = p.paragraph_format
            pf.space_before = Pt(1)
            pf.space_after = Pt(1)
            pf.line_spacing = 1.0
            p.alignment = aligns[j] if j < len(aligns) else WD_ALIGN_PARAGRAPH.LEFT
            add_runs(p, parse_inline(cell_text), TABLE_SIZE)
    return table


# ---------------- 文档级设置 ----------------
def setup_document(doc):
    sec = doc.sections[0]
    sec.page_width = Cm(21.0)             # A4
    sec.page_height = Cm(29.7)
    sec.top_margin = Cm(2.54)
    sec.bottom_margin = Cm(2.54)
    sec.left_margin = Cm(3.18)
    sec.right_margin = Cm(3.18)

    normal = doc.styles["Normal"]
    normal.font.name = LATIN
    normal.font.size = Pt(BODY_SIZE)
    normal.element.get_or_add_rPr().get_or_add_rFonts().set(qn("w:eastAsia"), SONG)
    npf = normal.paragraph_format
    npf.line_spacing = 1.5
    npf.space_after = Pt(6)

    for name, size, before, after in (
        ("Heading 1", 18, 6, 16),
        ("Heading 2", 15, 14, 8),
        ("Heading 3", 14, 12, 6),
    ):
        st = doc.styles[name]
        st.font.name = HEI
        st.font.size = Pt(size)
        st.font.bold = True
        st.font.color.rgb = RGBColor(0, 0, 0)
        srPr = st.element.get_or_add_rPr()
        color_el = srPr.find(qn("w:color"))
        if color_el is not None:          # 清除主题色属性，确保纯黑
            for a in ("themeColor", "themeTint", "themeShade"):
                if color_el.get(qn("w:" + a)) is not None:
                    del color_el.attrib[qn("w:" + a)]
        srPr.get_or_add_rFonts().set(qn("w:eastAsia"), HEI)
        spf = st.paragraph_format
        spf.space_before = Pt(before)
        spf.space_after = Pt(after)
        spf.line_spacing = 1.2
        spf.keep_with_next = True


# ---------------- 逐行解析主循环 ----------------
def convert(md_path, docx_path):
    with open(md_path, "r", encoding="utf-8-sig") as f:
        lines = f.read().splitlines()

    doc = Document()
    setup_document(doc)
    bullet_style_ok = "List Bullet" in [s.name for s in doc.styles]

    last_kind = None
    in_list_context = False     # 缩进续行仅在被视作列表上下文时生效
    cur_num_id = None           # 当前有序列表组的编号定义
    ol_counter = 0

    i = 0
    n = len(lines)
    while i < n:
        raw = lines[i]
        stripped = raw.strip()
        if not stripped:
            last_kind = "blank"
            i += 1
            continue

        m = HEADING_PAT.match(stripped)
        if m:
            add_heading(doc, len(m.group(1)), m.group(2))
            last_kind = "heading"
            in_list_context = False
            cur_num_id = None
            i += 1
            continue

        if HR_PAT.match(stripped):
            add_hr(doc)
            last_kind = "hr"
            in_list_context = False
            cur_num_id = None
            i += 1
            continue

        if stripped.startswith("|") and i + 1 < n and is_sep_row(lines[i + 1]):
            if last_kind == "table":      # 相邻两个 w:tbl 会被 Word 合并，需隔一个空段
                doc.add_paragraph()
            header = split_row(stripped)
            aligns = [align_of(c) for c in split_row(lines[i + 1])]
            rows = []
            i += 2
            while i < n and lines[i].strip().startswith("|"):
                rows.append(split_row(lines[i]))
                i += 1
            add_gfm_table(doc, header, rows, aligns)
            last_kind = "table"
            continue

        if stripped.startswith(">"):
            while i < n and lines[i].strip().startswith(">"):
                q = re.sub(r"^\s*>\s?", "", lines[i]).strip()
                if q:
                    add_quote(doc, parse_inline(q))
                i += 1
            last_kind = "quote"
            in_list_context = False
            cur_num_id = None
            continue

        m_ol = OL_PAT.match(raw)
        if m_ol:
            if last_kind != "ol":         # 新的有序列表组：新建编号定义，从 1 开始
                cur_num_id = new_num_id(doc)
                ol_counter = 0
            ol_counter += 1
            add_ordered(doc, parse_inline(m_ol.group(3)), cur_num_id, ol_counter)
            last_kind = "ol"
            in_list_context = True
            i += 1
            continue

        m_ul = UL_PAT.match(raw)
        if m_ul:
            add_bullet(doc, parse_inline(m_ul.group(2)), bullet_style_ok)
            last_kind = "ul"
            in_list_context = True
            i += 1
            continue

        if raw[:1] in (" ", "\t") and in_list_context:
            # 列表项的缩进续行（如 md 中的 "要点：…"）
            add_body(doc, parse_inline(stripped), indent_pt=36,
                     align=WD_ALIGN_PARAGRAPH.LEFT, space_after_pt=4)
            last_kind = "cont"
            i += 1
            continue

        # 普通段落：连续非空普通行合并为一段（GFM 软换行）
        para_lines = [stripped]
        i += 1
        while i < n:
            nxt = lines[i]
            ns = nxt.strip()
            if (not ns or HEADING_PAT.match(ns) or ns.startswith(">") or ns.startswith("|")
                    or HR_PAT.match(ns) or UL_PAT.match(nxt) or OL_PAT.match(nxt)):
                break
            para_lines.append(ns)
            i += 1
        add_body(doc, parse_inline(" ".join(para_lines)))
        last_kind = "para"
        in_list_context = False
        cur_num_id = None

    doc.save(docx_path)
    return doc


def main(argv):
    args = argv[1:]
    if not args or len(args) % 2 != 0:
        print("用法: python md2docx.py <in.md> <out.docx> [<in.md> <out.docx> ...]")
        return 2
    for k in range(0, len(args), 2):
        md_path, docx_path = args[k], args[k + 1]
        doc = convert(md_path, docx_path)
        print("OK  %s -> %s  (paragraphs=%d, tables=%d)"
              % (md_path, docx_path, len(doc.paragraphs), len(doc.tables)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
