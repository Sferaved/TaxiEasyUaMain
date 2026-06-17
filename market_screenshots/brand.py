"""Shared brand assets for Таксі Київ (PAS_1)."""

from __future__ import annotations

from io import BytesIO

from PIL import Image
from reportlab.graphics import renderPM
from svglib.svglib import svg2rlg

KYIV_BLUE = (13, 40, 71)
KYIV_BLUE_DARK = (8, 24, 42)
KYIV_BLUE_MID = (22, 58, 98)
KYIV_BLUE_LIGHT = (30, 72, 120)
KYIV_RING = (18, 46, 79)
TAXI_YELLOW = (255, 199, 0)
TAXI_YELLOW_LIGHT = (255, 220, 64)
TAXI_YELLOW_DEEP = (230, 168, 0)
TAXI_YELLOW_SOFT = (255, 236, 140)
TAXI_YELLOW_HI = (255, 235, 102)
TEXT_SECONDARY = (160, 175, 195)
WHITE = (255, 255, 255)

# Launcher icon — location pin only, solid fills (svglib-safe).
TAXI_ICON_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="108" height="108" viewBox="0 0 108 108">
  <circle cx="54" cy="54" r="46" fill="#163A5F"/>
  <circle cx="54" cy="54" r="46" fill="none" stroke="#E6A800" stroke-width="2.2" opacity="0.9"/>
  <circle cx="54" cy="54" r="40" fill="none" stroke="#FFC700" stroke-width="0.8" opacity="0.35"/>
  <path fill="#D9A000" d="M54,22 C40,22 30,34 30,46 C30,64 54,84 54,84 C54,84 78,64 78,46 C78,34 68,22 54,22 Z"/>
  <path fill="#FFC700" d="M54,24 C41,24 32,35 32,46 C32,62 54,80 54,80 C54,80 76,62 76,46 C76,35 67,24 54,24 Z"/>
  <path fill="#FFE566" d="M54,26 C43,26 35,36 35,46 C35,59 54,75 54,75 C54,75 73,59 73,46 C73,36 65,26 54,26 Z" opacity="0.4"/>
  <circle cx="54" cy="44" r="14" fill="#0D2847"/>
  <circle cx="54" cy="44" r="9" fill="#163A5F"/>
  <circle cx="54" cy="44" r="5" fill="#FFC700"/>
</svg>"""

# Detailed in-app mark, 200×200, pin only.
LOGO_MARK_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200" viewBox="0 0 200 200">
  <circle cx="100" cy="100" r="92" fill="#122E4F"/>
  <circle cx="100" cy="100" r="92" fill="none" stroke="#E6A800" stroke-width="2.8" opacity="0.75"/>
  <circle cx="100" cy="100" r="84" fill="none" stroke="#FFC700" stroke-width="1" opacity="0.3"/>
  <path fill="#D9A000" d="M100,38 C74,38 56,56 56,76 C56,106 100,150 100,150 C100,150 144,106 144,76 C144,56 126,38 100,38 Z"/>
  <path fill="#FFC700" d="M100,40 C76,40 58,58 58,76 C58,104 100,146 100,146 C100,146 142,104 142,76 C142,56 124,40 100,40 Z"/>
  <path fill="#FFE566" d="M100,42 C78,42 62,58 62,74 C62,98 100,138 100,138 C100,138 138,98 138,74 C138,58 122,42 100,42 Z" opacity="0.45"/>
  <circle cx="100" cy="72" r="26" fill="#0D2847"/>
  <circle cx="100" cy="72" r="17" fill="#163A5F"/>
  <circle cx="100" cy="72" r="9" fill="#FFC700"/>
  <circle cx="100" cy="72" r="4" fill="#FFE566"/>
</svg>"""


def render_svg(svg: str, size: int) -> Image.Image:
    drawing = svg2rlg(BytesIO(svg.encode("utf-8")))
    if drawing is None:
        raise RuntimeError("Failed to parse SVG")
    import re
    m = re.search(r'viewBox="[\d.\s]+([\d.]+)\s+([\d.]+)"', svg)
    if m:
        vb_w, vb_h = float(m.group(1)), float(m.group(2))
        scale = size / max(vb_w, vb_h)
    else:
        scale = size / max(drawing.width or size, drawing.height or size, 1)
    drawing.width = size
    drawing.height = size
    drawing.scale(scale, scale)
    img = Image.open(BytesIO(renderPM.drawToString(drawing, fmt="PNG"))).convert("RGBA")
    data = []
    for r, g, b, a in img.getdata():
        if r > 250 and g > 250 and b > 250:
            data.append((r, g, b, 0))
        else:
            data.append((r, g, b, a))
    img.putdata(data)
    return img
