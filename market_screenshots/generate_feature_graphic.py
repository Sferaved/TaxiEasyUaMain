#!/usr/bin/env python3
"""Generate Google Play Feature Graphic (1024x500) for Таксі Київ."""

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
from generate_logos import add_glow, draw_dot_grid, fit_font, lerp, vertical_gradient

ROOT = Path(__file__).resolve().parent
OUTPUT = ROOT / "output" / "feature_graphic.png"

W, H = 1024, 500
FONT_BOLD = Path(r"C:\Windows\Fonts\segoeuib.ttf")
FONT_LIGHT = Path(r"C:\Windows\Fonts\segoeuil.ttf")
FONT_REGULAR = Path(r"C:\Windows\Fonts\segoeui.ttf")


def draw_blob(draw: ImageDraw.ImageDraw, center: tuple[int, int], radius: int, color: tuple[int, int, int, int]) -> None:
    x, y = center
    draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=color)


def generate() -> Image.Image:
    base = vertical_gradient((W, H), KYIV_BLUE_MID, KYIV_BLUE_DARK).convert("RGBA")
    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    draw_blob(draw, (900, 80), 180, TAXI_YELLOW + (28,))
    draw_blob(draw, (-40, 420), 150, KYIV_BLUE + (40,))
    draw_blob(draw, (760, 460), 120, TAXI_YELLOW_SOFT + (22,))
    canvas = Image.alpha_composite(base, overlay)
    canvas = draw_dot_grid(canvas, spacing=34, alpha=10)
    canvas = add_glow(canvas, (780, 250), 220, TAXI_YELLOW, 30)
    draw = ImageDraw.Draw(canvas)

    left_pad = 72
    word1, word2 = "Таксі", "Київ"
    font1 = fit_font(word1, 480, 68, FONT_LIGHT)
    font2 = fit_font(word2, 480, 76, FONT_BOLD)
    y = 108
    bbox1 = draw.textbbox((0, 0), word1, font=font1)
    bbox2 = draw.textbbox((0, 0), word2, font=font2)
    gap = 14
    total = (bbox1[2] - bbox1[0]) + gap + (bbox2[2] - bbox2[0])
    x0 = left_pad
    draw.text((x0, y), word1, fill=(235, 242, 250), font=font1)
    x2 = x0 + (bbox1[2] - bbox1[0]) + gap
    draw.text((x2, y - 6), word2, fill=TAXI_YELLOW_LIGHT, font=font2)

    line_y = y + max(bbox1[3], bbox2[3]) + 16
    draw.rounded_rectangle((left_pad, line_y, left_pad + 130, line_y + 4), radius=2, fill=TAXI_YELLOW)
    draw.rounded_rectangle((left_pad + 130, line_y, left_pad + 210, line_y + 4), radius=2, fill=KYIV_BLUE_LIGHT + (160,))

    subtitle = "Швидкий виклик авто, фіксована ціна та подача"
    tagline = "Онлайн замовлення таксі у Києві"
    sub_font = fit_font(subtitle, 520, 34, FONT_BOLD)
    tag_font = fit_font(tagline, 520, 26, FONT_REGULAR)
    draw.text((left_pad, line_y + 22), subtitle, fill=WHITE, font=sub_font)
    draw.text((left_pad, line_y + 68), tagline, fill=TEXT_SECONDARY, font=tag_font)

    bullets = [
        "Онлайн виклик авто за кілька секунд",
        "Зручна оплата та попереднє замовлення",
        "Тарифи, історія поїздок і улюблені адреси",
    ]
    bullet_font = fit_font(bullets[0], 520, 23, FONT_REGULAR)
    by = line_y + 118
    for line in bullets:
        draw.ellipse((left_pad, by + 8, left_pad + 7, by + 15), fill=TAXI_YELLOW)
        draw.text((left_pad + 16, by), line, fill=(205, 212, 225), font=bullet_font)
        by = draw.textbbox((left_pad + 16, by), line, font=bullet_font)[3] + 10

    mark = render_svg(LOGO_MARK_SVG, 300)
    glow = Image.new("RGBA", (360, 360), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((30, 30, 330, 330), fill=TAXI_YELLOW + (45,))
    glow = glow.filter(ImageFilter.GaussianBlur(24))
    px, py = W - 350, (H - 300) // 2 - 8
    canvas.alpha_composite(glow, (px - 28, py - 28))
    canvas.alpha_composite(mark, (px, py))

    return canvas.convert("RGB")


def main() -> None:
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    img = generate()
    img.save(OUTPUT, format="PNG", optimize=True)
    print(f"Saved {OUTPUT} ({img.size[0]}x{img.size[1]})")


if __name__ == "__main__":
    main()
