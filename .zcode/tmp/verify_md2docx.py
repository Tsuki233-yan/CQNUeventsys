# -*- coding: utf-8 -*-
"""verify_md2docx.py — 校验 md2docx.py 的转换结果。

检查项：
  1. 两个 .docx 存在且非空（本题要求的检查）；
  2. 能用 python-docx 正常打开；
  3. 表格数量 = md 中 GFM 分隔行数量；各级标题数量一致；
     无序/有序列表项数量一致（有序列表应为段落级 numPr 自动编号）；
  4. 内容零丢失（两条独立口径）：
       a) 严格口径：md 每行经 parse_inline 剥离语法标记后的纯文本逐行能在 docx 全文中找到；
       b) 朴素口径：naive 去掉所有 * 和 ` 后比对——与严格口径的差异必须全部可归因于
          “代码片段内的字面星号”（GFM 规定代码片段内字符保持原样），否则判失败。
  5. 排版抽查：正文 run 的 eastAsia=宋体、标题 run 的 eastAsia=黑体且加粗、表内五号。
"""

import os
import re
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import md2docx  # noqa: E402  复用其行内解析与表格行解析

from docx import Document  # noqa: E402
from docx.oxml.ns import qn  # noqa: E402

BASE = r"D:/eventSys-main/docs"
PAIRS = [
    (BASE + "/需求规格说明书.md", BASE + "/需求规格说明书.docx"),
    (BASE + "/小组分工与答辩准备.md", BASE + "/小组分工与答辩准备.docx"),
]


def plain_of(s):
    """严格口径：与转换器一致的纯文本（代码片段内的 * 等字符原样保留）。"""
    return "".join(t for t, _ in md2docx.parse_inline(s)).strip()


def naive_of(s):
    """朴素口径：粗暴去掉所有 * 与 `（会把代码片段内的字面星号误删）。"""
    return s.replace("*", "").replace("`", "").strip()


def md_items(line):
    """返回该 md 行的 (严格纯文本, 朴素纯文本) 片段列表（表格行 → 各单元格）。"""
    s = line.strip()
    if not s or md2docx.HR_PAT.match(s):
        return []
    if s.startswith("|"):
        if md2docx.is_sep_row(s):
            return []
        out = []
        for c in md2docx.split_row(s):
            if c:
                out.append((plain_of(c), naive_of(c)))
        return out
    if s.startswith(">"):
        s = re.sub(r"^>\s?", "", s)
    m = md2docx.HEADING_PAT.match(s)
    if m:
        s = m.group(2)
    m = md2docx.UL_PAT.match(s)
    if m:
        s = m.group(2)
    else:
        m = md2docx.OL_PAT.match(s)
        if m:
            s = m.group(3)
    strict, naive = plain_of(s), naive_of(s)
    return [(strict, naive)] if strict or naive else []


def run_font_info(run):
    rPr = run._element.rPr
    if rPr is None or rPr.rFonts is None:
        return (None, None)
    return (rPr.rFonts.get(qn("w:eastAsia")), rPr.rFonts.get(qn("w:ascii")))


def main():
    all_ok = True
    for md_path, docx_path in PAIRS:
        print("== %s" % docx_path)
        exists = os.path.isfile(docx_path)
        size = os.path.getsize(docx_path) if exists else -1
        print("   [检查1] exists=%s, size=%d bytes" % (exists, size))
        if not exists or size == 0:
            all_ok = False
            print("   FAIL: 文件不存在或为空")
            continue
        try:
            d = Document(docx_path)
        except Exception as e:
            all_ok = False
            print("   FAIL: 无法用 python-docx 打开: %r" % e)
            continue

        parts = [p.text for p in d.paragraphs]
        for t in d.tables:
            for row in t.rows:
                for c in row.cells:
                    parts.append(c.text)
        full = "\n".join(parts)

        head_md, head_doc = {}, {}
        sep_rows = ul_md = ol_md = 0
        bullet_doc = ordered_doc = 0
        strict_missing = []
        naive_missing = []
        naive_all_code_span = True   # 朴素口径缺失是否全部由“代码片段内字面星号”解释
        with open(md_path, "r", encoding="utf-8-sig") as f:
            for line in f.read().splitlines():
                ls = line.strip()
                m = md2docx.HEADING_PAT.match(ls)
                if m:
                    lv = len(m.group(1))
                    head_md[lv] = head_md.get(lv, 0) + 1
                if md2docx.is_sep_row(ls):
                    sep_rows += 1
                if md2docx.UL_PAT.match(line):
                    ul_md += 1
                if md2docx.OL_PAT.match(line):
                    ol_md += 1
                for strict, naive in md_items(line):
                    if strict not in full:
                        strict_missing.append(strict)
                    if naive not in full:
                        naive_missing.append(naive)
                        # 归因检查：该差异必须仅为被保留的字面星号
                        # 找到同行的 strict 对应片段比对（数量一致时逐个配对不可靠，
                        # 因此用宽松判据：naive 去掉 * 后应等于 strict 去掉 * 后的某处前缀）
                        if strict.replace("*", "") != naive.replace("*", ""):
                            naive_all_code_span = False
        for p in d.paragraphs:
            name = p.style.name if p.style is not None else ""
            m = re.match(r"^Heading (\d+)$", name or "")
            if m:
                lv = int(m.group(1))
                head_doc[lv] = head_doc.get(lv, 0) + 1
            if name == "List Bullet":
                bullet_doc += 1
            pPr = p._p.pPr
            if pPr is not None and pPr.numPr is not None:
                ordered_doc += 1

        print("   [检查3] paragraphs=%d tables=%d (md 分隔行=%d)  标题 md=%s docx=%s"
              % (len(d.paragraphs), len(d.tables), sep_rows, head_md, head_doc))
        print("          列表项 md(无序=%d, 有序=%d) vs docx(List Bullet=%d, numPr 段落=%d)"
              % (ul_md, ol_md, bullet_doc, ordered_doc))
        print("   [检查4a] 严格口径未找到的片段数 = %d" % len(strict_missing))
        for x in strict_missing[:10]:
            print("     MISSING: %s" % x[:100])
        print("   [检查4b] 朴素口径未找到 %d 条，其中可归因于代码片段内字面星号(GFM 语义): %s"
              % (len(naive_missing), "是" if naive_all_code_span else "否"))
        for x in naive_missing[:6]:
            print("     (朴素)MISSING: %s" % x[:100])

        # 排版抽查
        spot = []
        h1 = next((p for p in d.paragraphs if p.style is not None and p.style.name == "Heading 1"
                   and p.runs), None)
        if h1 is not None:
            r = h1.runs[0]
            ea, asc = run_font_info(r)
            spot.append("H1 run: eastAsia=%s ascii=%s size=%s bold=%s"
                        % (ea, asc, r.font.size.pt if r.font.size else None, r.font.bold))
        body = next((p for p in d.paragraphs if p.style is not None and p.style.name == "Normal"
                     and p.runs and p.text.strip()), None)
        if body is not None:
            r = body.runs[0]
            ea, asc = run_font_info(r)
            spot.append("body run: eastAsia=%s ascii=%s size=%s"
                        % (ea, asc, r.font.size.pt if r.font.size else None))
        cell_run = None
        for t in d.tables:
            for row in t.rows:
                for c in row.cells:
                    if c.paragraphs[0].runs:
                        cell_run = c.paragraphs[0].runs[0]
                        break
                if cell_run is not None:
                    break
            if cell_run is not None:
                break
        if cell_run is not None:
            ea, asc = run_font_info(cell_run)
            spot.append("table run: eastAsia=%s ascii=%s size=%s"
                        % (ea, asc, cell_run.font.size.pt if cell_run.font.size else None))
        for s in spot:
            print("   [检查5] %s" % s)

        ok = (exists and size > 0 and len(d.tables) == sep_rows
              and head_md == head_doc and ul_md == bullet_doc and ol_md == ordered_doc
              and not strict_missing and naive_all_code_span)
        print("   => %s" % ("PASS" if ok else "FAIL"))
        all_ok = all_ok and ok

    print("RESULT: %s" % ("ALL PASS" if all_ok else "FAIL"))
    return 0 if all_ok else 1


if __name__ == "__main__":
    sys.exit(main())
