#!/usr/bin/env python3
"""Regenerate all Таксі Київ brand assets (icon, logos, Play banner)."""

from generate_app_icon import main as gen_icon
from generate_feature_graphic import main as gen_banner
from generate_logos import main as gen_logos


def main() -> None:
    gen_icon()
    gen_logos()
    gen_banner()
    print("All brand assets generated.")


if __name__ == "__main__":
    main()
