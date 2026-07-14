"""Apply the Memory Crystal gradient to any item texture.

The gradient is a luminance -> color map extracted from memory_crystal.png:
dark pixels become deep navy, midtones magenta/rose, highlights lavender-cyan.
This lets you give any texture the crystal's palette without redrawing it --
just author the texture in grayscale (form + shading) and run it through here.

Usage:
    python apply_gradient.py <input.png> <output.png>

Alpha is preserved; only the RGB of visible pixels is remapped.
Requires Pillow + numpy. The palette lives in gradient_lut.png next to this file.
"""
import sys
from pathlib import Path
from PIL import Image
import numpy as np

LUT_PATH = Path(__file__).with_name("gradient_lut.png")
LUT = np.array(Image.open(LUT_PATH).convert("RGB")).reshape(256, 3)


def apply(inp, outp):
    a = np.array(Image.open(inp).convert("RGBA"))
    rgb = a[:, :, :3].astype(float)
    lum = (0.299 * rgb[:, :, 0] + 0.587 * rgb[:, :, 1] + 0.114 * rgb[:, :, 2])
    lum = np.clip(lum.round().astype(int), 0, 255)
    a[:, :, :3] = LUT[lum]
    Image.fromarray(a, "RGBA").save(outp)
    print("wrote", outp)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit("usage: python apply_gradient.py <input.png> <output.png>")
    apply(sys.argv[1], sys.argv[2])
