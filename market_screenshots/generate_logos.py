#!/usr/bin/env python3
"""Generate in-app logos: logo.jpg (splash) and mylogo.JPG (about screen)."""

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
    TAXI_YELLOW_DEEP,
    TAXI_YELLOW_LIGHT,
    TEXT_SECONDARY,
    WHITE,
    render_svg,
)

ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable"
FONT_BOLD = Path(r"C:\Windows\Fonts\segoeuib.ttf")
FONT_LIGHT = Path(r"C:\Windows\Fonts\segoeuil.ttf")
FONT_REGULAR = Path(r"C:\Windows\Fonts\segoeui.ttf")


def lerp(a: int, b: int, t: float) -> int:
    return int(a + (b - a) * t)


def radial_gradient(size: int, center: tuple[int, int], inner: tuple[int, int, int], outer: tuple[int, int, int]) -> Image.Image:
    img = Image.new("RGB", (size, size))
    cx, cy = center
    max_r = (size ** 2 + size ** 2) ** 0.5 / 2
    px = img.load()
    for y in range(size):
        for x in range(size):
            t = min(((x - cx) ** 2 + (y - cy) ** 2) ** 0.5 / max_r, 1.0)
            t = t ** 1.4
            px[x, y] = tuple(lerp(inner[i], outer[i], t) for i in range(3))
    return img


def vertical_gradient(size: tuple[int, int], top: tuple[int, int, int], bottom: tuple[int, int, int]) -> Image.Image:
    w, h = size
    img = Image.new("RGB", size)
    px = img.load()
    for y in range(h):
        t = y / max(h - 1, 1)
        row = tuple(lerp(top[i], bottom[i], t) for i in range(3))
        for x in range(w):
            px[x, y] = row
    return img


def add_glow(base: Image.Image, center: tuple[int, int], radius: int, color: tuple[int, int, int], alpha: int = 60) -> Image.Image:
    glow = Image.new("RGBA", base.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(glow)
    x, y = center
    draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=color + (alpha,))
    glow = glow.filter(ImageFilter.GaussianBlur(radius // 3))
    return Image.alpha_composite(base.convert("RGBA"), glow)


def draw_dot_grid(canvas: Image.Image, spacing: int = 28, alpha: int = 14) -> Image.Image:
    overlay = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    w, h = canvas.size
    for y in range(0, h, spacing):
        for x in range(0, w, spacing):
            if (x + y) % (spacing * 2) == 0:
                draw.ellipse((x, y, x + 2, y + 2), fill=WHITE + (alpha,))
    return Image.alpha_composite(canvas.convert("RGBA"), overlay)


def fit_font(text: str, max_width: int, start: int, path: Path) -> ImageFont.FreeTypeFont:
    for size in range(start, 12, -2):
        font = ImageFont.truetype(str(path), size)
        bbox = ImageDraw.Draw(Image.new("RGB", (1, 1))).textbbox((0, 0), text, font=font)
        if bbox[2] - bbox[0] <= max_width:
            return font
    return ImageFont.truetype(str(path), 12)


def generate_mylogo() -> Image.Image:
    size = 512
    base = radial_gradient(size, (256, 230), KYIV_BLUE_LIGHT, KYIV_BLUE_DARK).convert("RGBA")
    base = add_glow(base, (256, 240), 200, TAXI_YELLOW, 45)
    base = draw_dot_grid(base, spacing=32, alpha=10)

    mark = render_svg(LOGO_MARK_SVG, 340)
    shadow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    shadow_mark = mark.copy()
    shadow_layer = Image.new("RGBA", mark.size, (0, 0, 0, 0))
    alpha = mark.split()[3]
    shadow_layer.putalpha(alpha.point(lambda a: int(a * 0.35)))
    shadow.alpha_composite(shadow_layer, (96, 108))
    shadow = shadow.filter(ImageFilter.GaussianBlur(12))
    base = Image.alpha_composite(base, shadow)
    base.alpha_composite(mark, ((size - 340) // 2, (size - 340) // 2 - 8))

    return base.convert("RGB")


def generate_logo() -> Image.Image:
    w, h = 520, 420
    base = vertical_gradient((w, h), KYIV_BLUE_MID, KYIV_BLUE_DARK).convert("RGBA")
    base = add_glow(base, (w // 2, 120), 160, TAXI_YELLOW, 35)
    base = draw_dot_grid(base, spacing=30, alpha=12)
    draw = ImageDraw.Draw(base)

    mark = render_svg(LOGO_MARK_SVG, 160)
    mx = (w - 160) // 2
    base.alpha_composite(mark, (mx, 36))

    word1 = "Таксі"
    word2 = "Київ"
    font1 = fit_font(word1, w - 40, 56, FONT_LIGHT)
    font2 = fit_font(word2, w - 40, 64, FONT_BOLD)

    y_text = 210
    bbox1 = draw.textbbox((0, 0), word1, font=font1)
    bbox2 = draw.textbbox((0, 0), word2, font=font2)
    gap = 10
    total_w = (bbox1[2] - bbox1[0]) + gap + (bbox2[2] - bbox2[0])
    x0 = (w - total_w) // 2

    draw.text((x0, y_text), word1, fill=(230, 238, 248), font=font1)
    x2 = x0 + (bbox1[2] - bbox1[0]) + gap
    draw.text((x2, y_text - 4), word2, fill=TAXI_YELLOW_LIGHT, font=font2)

    line_y = y_text + max(bbox1[3], bbox2[3]) + 14
    line_w = min(total_w + 40, 220)
    lx = (w - line_w) // 2
    draw.rounded_rectangle((lx, line_y, lx + line_w * 0.55, line_y + 3), radius=2, fill=TAXI_YELLOW)
    draw.rounded_rectangle((lx + line_w * 0.55, line_y, lx + line_w, line_y + 3), radius=2, fill=KYIV_BLUE_LIGHT + (180,))

    subtitle = "Швидкий виклик авто · фіксована ціна"
    sub_font = fit_font(subtitle, w - 48, 22, FONT_REGULAR)
    sub_bbox = draw.textbbox((0, 0), subtitle, font=sub_font)
    sub_w = sub_bbox[2] - sub_bbox[0]
    draw.text(((w - sub_w) // 2, line_y + 18), subtitle, fill=TEXT_SECONDARY, font=sub_font)

    tag = "КИЇВ"
    tag_font = fit_font(tag, 80, 14, FONT_BOLD)
    tag_bbox = draw.textbbox((0, 0), tag, font=tag_font)
    tag_w = tag_bbox[2] - tag_bbox[0]
    tag_h = tag_bbox[3] - tag_bbox[1]
    tx = w - tag_w - 28
    ty = h - tag_h - 24
    draw.rounded_rectangle((tx - 10, ty - 6, tx + tag_w + 10, ty + tag_h + 6), radius=8, outline=TAXI_YELLOW_DEEP + (140,), width=1)
    draw.text((tx, ty), tag, fill=TAXI_YELLOW_DEEP, font=tag_font)

    return base.convert("RGB")


def main() -> None:
    mylogo = generate_mylogo()
    mylogo_path = DRAWABLE / "mylogo.JPG"
    mylogo.save(mylogo_path, format="JPEG", quality=94, optimize=True)
    print(f"Saved {mylogo_path} ({mylogo.size[0]}x{mylogo.size[1]})")

    logo = generate_logo()
    logo_path = DRAWABLE / "logo.jpg"
    logo.save(logo_path, format="JPEG", quality=94, optimize=True)
    print(f"Saved {logo_path} ({logo.size[0]}x{logo.size[1]})")


if __name__ == "__main__":
    main()
