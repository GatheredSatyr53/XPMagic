"""Apply the Memory Crystal gradient to any item texture.

The gradient is a luminance -> color map extracted from memory_crystal.png:
dark pixels become deep navy, midtones magenta/rose, highlights lavender-cyan.
This lets you give any texture the crystal's palette without redrawing it --
just author the texture in grayscale (form + shading) and run it through here.

The crystal itself is this map applied to the vanilla diamond gem, so the
authentic way to make a matching tool set is to run the vanilla diamond
TOOL sprites (pickaxe/sword/axe/shovel/hoe) through here -- they already
carry correct Minecraft shading, so the set stays consistent for free.

Two LUTs ship alongside this script:
  gradient_lut.png      cleaned ramp (no muddy midtones)          [default]
  gradient_lut_raw.png  original ramp, keeps the warm gold band   [--raw]
Use --raw to reproduce the exact diamond->crystal transform (gold accents).

Usage:
    python apply_gradient.py <input.png> <output.png> [--raw]

Alpha is preserved; only the RGB of visible pixels is remapped.
Requires Pillow + numpy.
"""
import sys
from pathlib import Path
from PIL import Image
import numpy as np


def load_lut(raw):
    name = "gradient_lut_raw.png" if raw else "gradient_lut.png"
    return np.array(Image.open(Path(__file__).with_name(name)).convert("RGB")).reshape(256, 3)


def apply(inp, outp, raw=False):
    lut = load_lut(raw)
    a = np.array(Image.open(inp).convert("RGBA"))
    rgb = a[:, :, :3].astype(float)
    lum = (0.299 * rgb[:, :, 0] + 0.587 * rgb[:, :, 1] + 0.114 * rgb[:, :, 2])
    lum = np.clip(lum.round().astype(int), 0, 255)
    a[:, :, :3] = lut[lum]
    Image.fromarray(a, "RGBA").save(outp)
    print("wrote", outp, "(raw ramp)" if raw else "(cleaned ramp)")


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if a != "--raw"]
    raw = "--raw" in sys.argv[1:]
    if len(args) != 2:
        sys.exit("usage: python apply_gradient.py <input.png> <output.png> [--raw]")
    apply(args[0], args[1], raw)
