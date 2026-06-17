#!/usr/bin/env python3
"""Generate launcher icons for Таксі Київ — yellow taxi on Kyiv blue."""

from __future__ import annotations

from pathlib import Path

from PIL import Image

from brand import KYIV_BLUE, TAXI_ICON_SVG, render_svg

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
PLAY_ICON = ROOT / "market_screenshots" / "output" / "play_store_icon.png"

LEGACY_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

FOREGROUND_SIZES = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}


def render_icon(size: int, *, layers: str) -> Image.Image:
    fg = render_svg(TAXI_ICON_SVG, size)
    if layers == "full":
        base = Image.new("RGBA", (size, size), KYIV_BLUE + (255,))
        base.alpha_composite(fg)
        return base
    if layers == "foreground":
        return fg
    raise ValueError(layers)


def save_webp(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, format="WEBP", quality=95, method=6)
    print(f"Saved {path}")


def main() -> None:
    for folder, px in LEGACY_SIZES.items():
        icon = render_icon(px, layers="full")
        save_webp(icon, RES / folder / "ic_launcher.webp")
        save_webp(icon, RES / folder / "ic_launcher_round.webp")

    for folder, px in FOREGROUND_SIZES.items():
        save_webp(render_icon(px, layers="foreground"), RES / folder / "ic_launcher_foreground.webp")

    play = render_icon(512, layers="full")
    PLAY_ICON.parent.mkdir(parents=True, exist_ok=True)
    play.save(PLAY_ICON, format="PNG", optimize=True)
    print(f"Saved {PLAY_ICON} (512x512)")


if __name__ == "__main__":
    main()
