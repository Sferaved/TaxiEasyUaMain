#!/usr/bin/env python3
"""Generate PDF from google-pay-pas1-testing-ru.md (UTF-8, Cyrillic)."""
from __future__ import annotations

import re
from pathlib import Path

from fpdf import FPDF

DOCS = Path(__file__).resolve().parent
MD_FILE = DOCS / "google-pay-pas1-testing-ru.md"
PDF_FILE = DOCS / "google-pay-pas1-testing-ru.pdf"
FONT_REG = r"C:\Windows\Fonts\arial.ttf"
FONT_BOLD = r"C:\Windows\Fonts\arialbd.ttf"


class TestingGuidePDF(FPDF):
    def __init__(self) -> None:
        super().__init__()
        self.set_margins(15, 15, 15)
        self.add_font("Arial", "", FONT_REG)
        self.add_font("Arial", "B", FONT_BOLD)
        self.set_auto_page_break(auto=True, margin=18)

    def footer(self) -> None:
        self.set_y(-12)
        self.set_font("Arial", "", 8)
        self.set_text_color(100, 100, 100)
        self.cell(0, 8, f"PAS_1 Google Pay — тестирование | стр. {self.page_no()}", align="C")

    @property
    def content_width(self) -> float:
        return self.epw


def write_line(pdf: TestingGuidePDF, text: str, size: int = 10, bold: bool = False, spacing: float = 5) -> None:
    text = text.strip()
    if not text:
        pdf.ln(2)
        return
    pdf.set_font("Arial", "B" if bold else "", size)
    pdf.set_text_color(0, 0, 0)
    pdf.multi_cell(pdf.content_width, spacing, text)


def render_table_row(pdf: TestingGuidePDF, cells: list[str]) -> None:
    pdf.set_font("Arial", "", 8)
    line = " | ".join(c.strip() for c in cells if c.strip())
    pdf.multi_cell(pdf.content_width, 4.2, line)
    pdf.ln(1)


def main() -> None:
    if not MD_FILE.is_file():
        raise SystemExit(f"Missing {MD_FILE}")

    lines = MD_FILE.read_text(encoding="utf-8").splitlines()
    pdf = TestingGuidePDF()
    pdf.add_page()

    in_table = False
    table_header_done = False

    for raw in lines:
        line = raw.rstrip()

        if line.startswith("|") and "|" in line[1:]:
            if re.match(r"^\|[-:\s|]+\|$", line):
                table_header_done = True
                continue
            cells = [c.strip() for c in line.strip("|").split("|")]
            if not in_table:
                in_table = True
                table_header_done = False
                pdf.ln(2)
            render_table_row(pdf, cells)
            continue
        else:
            if in_table:
                in_table = False
                table_header_done = False
                pdf.ln(3)

        if line.startswith("# "):
            pdf.ln(4)
            write_line(pdf, line[2:], size=16, bold=True, spacing=7)
            pdf.ln(2)
        elif line.startswith("## "):
            pdf.ln(3)
            write_line(pdf, line[3:], size=13, bold=True, spacing=6)
            pdf.ln(1)
        elif line.startswith("### "):
            pdf.ln(2)
            write_line(pdf, line[4:], size=11, bold=True, spacing=5)
        elif line.startswith("---"):
            pdf.ln(2)
            pdf.set_draw_color(200, 200, 200)
            pdf.line(10, pdf.get_y(), 200, pdf.get_y())
            pdf.ln(3)
        elif line.startswith("- [ ]"):
            write_line(pdf, "[ ] " + line[5:].strip(), size=10)
        elif line.startswith("- [x]") or line.startswith("- [X]"):
            write_line(pdf, "[x] " + line[5:].strip(), size=10)
        elif line.startswith("- "):
            write_line(pdf, "• " + line[2:], size=10)
        elif re.match(r"^\d+\.\s", line):
            write_line(pdf, line, size=10)
        elif line.startswith("```"):
            continue
        elif line.startswith("*") and line.endswith("*") and not line.startswith("**"):
            write_line(pdf, line.strip("*"), size=9, spacing=4)
        else:
            # strip simple markdown bold/backticks
            cleaned = re.sub(r"\*\*([^*]+)\*\*", r"\1", line)
            cleaned = re.sub(r"`([^`]+)`", r"\1", cleaned)
            if cleaned:
                write_line(pdf, cleaned, size=10)

    pdf.output(str(PDF_FILE))
    print(f"Written: {PDF_FILE}")


if __name__ == "__main__":
    main()
