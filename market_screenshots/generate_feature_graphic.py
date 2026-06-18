#!/usr/bin/env python3
"""Generate Google Play Feature Graphic (1024x500) — trilingual."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

from brand import (
    KYIV_BLUE,
    KYIV_BLUE_DARK,
    KYIV_BLUE_LIGHT,
    KYIV_BLUE_MID,
    LOGO_MARK_SVG,
    TAXI_YELLOW,
    TAXI_YELLOW_LIGHT,
    TAXI_YELLOW_SOFT,
    TEXT_SECONDARY,
    WHITE,
    render_svg,
)
from generate_logos import add_glow, draw_dot_grid, fit_font, vertical_gradient

ROOT = Path(__file__).resolve().parent
OUTPUT = ROOT / "output" / "feature_graphic.png"

W, H = 1024, 500
LEFT_PAD = 56
TEXT_MAX_W = 600
FONT_BOLD = Path(r"C:\Windows\Fonts\segoeuib.ttf")
FONT_LIGHT = Path(r"C:\Windows\Fonts\segoeuil.ttf")
FONT_REGULAR = Path(r"C:\Windows\Fonts\segoeui.ttf")

LANG_ROWS = [
    ("UK", "Замовлення таксі у Києві", "Виклик авто · тарифи · оплата · історія"),
    ("RU", "Заказ такси в Киеве", "Вызов авто · тарифы · оплата · история"),
    ("EN", "Taxi booking in Kyiv", "Car call · fares · payment · trip history"),
]


def draw_blob(draw: ImageDraw.ImageDraw, center: tuple[int, int], radius: int, color: tuple[int, int, int, int]) -> None:
    x, y = center
    draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=color)


def draw_lang_badge(draw: ImageDraw.ImageDraw, x: int, y: int, label: str, font: ImageFont.FreeTypeFont) -> int:
    bbox = draw.textbbox((0, 0), label, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    pad_x, pad_y = 8, 4
    draw.rounded_rectangle(
        (x, y, x + tw + pad_x * 2, y + th + pad_y * 2),
        radius=6,
        fill=TAXI_YELLOW,
    )
    draw.text((x + pad_x, y + pad_y - 1), label, fill=KYIV_BLUE_DARK, font=font)
    return tw + pad_x * 2


def generate() -> Image.Image:
    base = vertical_gradient((W, H), KYIV_BLUE_MID, KYIV_BLUE_DARK).convert("RGBA")
    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    draw_blob(draw, (920, 70), 170, TAXI_YELLOW + (26,))
    draw_blob(draw, (-50, 430), 140, KYIV_BLUE + (38,))
    canvas = Image.alpha_composite(base, overlay)
    canvas = draw_dot_grid(canvas, spacing=34, alpha=10)
    canvas = add_glow(canvas, (800, 250), 200, TAXI_YELLOW, 28)
    draw = ImageDraw.Draw(canvas)

    y = 44
    title1, title2 = "Таксі", "Київ"
    t1_font = fit_font(title1, TEXT_MAX_W, 58, FONT_LIGHT)
    t2_font = fit_font(title2, TEXT_MAX_W, 64, FONT_BOLD)
    draw.text((LEFT_PAD, y), title1, fill=(235, 242, 250), font=t1_font)
    b1 = draw.textbbox((LEFT_PAD, y), title1, font=t1_font)
    draw.text((b1[2] + 12, y - 4), title2, fill=TAXI_YELLOW_LIGHT, font=t2_font)
    b2 = draw.textbbox((b1[2] + 12, y - 4), title2, font=t2_font)

    names = "Такси Киев  ·  Taxi Kyiv"
    names_font = fit_font(names, TEXT_MAX_W, 22, FONT_REGULAR)
    names_y = max(b1[3], b2[3]) + 8
    draw.text((LEFT_PAD, names_y), names, fill=TEXT_SECONDARY, font=names_font)
    names_bb = draw.textbbox((LEFT_PAD, names_y), names, font=names_font)

    line_y = names_bb[3] + 14
    draw.rounded_rectangle((LEFT_PAD, line_y, LEFT_PAD + 100, line_y + 3), radius=2, fill=TAXI_YELLOW)
    draw.rounded_rectangle((LEFT_PAD + 100, line_y, LEFT_PAD + 170, line_y + 3), radius=2, fill=KYIV_BLUE_LIGHT + (150,))

    badge_font = ImageFont.truetype(str(FONT_BOLD), 13)
    row_title_font = fit_font(LANG_ROWS[0][1], TEXT_MAX_W - 60, 21, FONT_BOLD)
    row_sub_font = fit_font(LANG_ROWS[0][2], TEXT_MAX_W - 60, 17, FONT_REGULAR)

    row_y = line_y + 20
    row_gap = 14
    for code, title, subtitle in LANG_ROWS:
        badge_w = draw_lang_badge(draw, LEFT_PAD, row_y + 2, code, badge_font)
        text_x = LEFT_PAD + badge_w + 12
        draw.text((text_x, row_y), title, fill=WHITE, font=row_title_font)
        title_bb = draw.textbbox((text_x, row_y), title, font=row_title_font)
        draw.text((text_x, title_bb[3] + 2), subtitle, fill=(175, 188, 205), font=row_sub_font)
        sub_bb = draw.textbbox((text_x, title_bb[3] + 2), subtitle, font=row_sub_font)
        row_y = sub_bb[3] + row_gap

    # bottom feature strip (make it readable)
    strip_y = H - 58
    strip = "Online booking  ·  Онлайн заказ  ·  Онлайн замовлення"
    strip_font = fit_font(strip, TEXT_MAX_W, 20, FONT_BOLD)
    strip_bb = draw.textbbox((LEFT_PAD, strip_y), strip, font=strip_font)

    # darker pill + subtle stroke for contrast
    pill = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    pill_draw = ImageDraw.Draw(pill)
    pill_draw.rounded_rectangle(
        (LEFT_PAD - 14, strip_y - 10, strip_bb[2] + 18, strip_bb[3] + 10),
        radius=14,
        fill=(8, 24, 42, 175),
        outline=TAXI_YELLOW + (110,),
        width=1,
    )
    canvas = Image.alpha_composite(canvas, pill)
    draw = ImageDraw.Draw(canvas)

    # text with soft shadow
    draw.text((LEFT_PAD + 1, strip_y + 1), strip, fill=(0, 0, 0, 110), font=strip_font)
    draw.text((LEFT_PAD, strip_y), strip, fill=(245, 248, 252), font=strip_font)

    mark = render_svg(LOGO_MARK_SVG, 250)
    glow = Image.new("RGBA", (300, 300), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((25, 25, 275, 275), fill=TAXI_YELLOW + (42,))
    glow = glow.filter(ImageFilter.GaussianBlur(22))
    px, py = W - 310, (H - 250) // 2 - 6
    canvas.alpha_composite(glow, (px - 24, py - 24))
    canvas.alpha_composite(mark, (px, py))

    return canvas.convert("RGB")


def main() -> None:
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    img = generate()
    img.save(OUTPUT, format="PNG", optimize=True)
    print(f"Saved {OUTPUT} ({img.size[0]}x{img.size[1]})")


if __name__ == "__main__":
    main()
