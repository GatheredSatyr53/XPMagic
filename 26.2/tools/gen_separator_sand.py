"""Regenerate the powder separator's falling-sand animation strip.

The separator's front is `orientable`; while it sifts, the blockstate swaps to
powder_separator_front_on.png -- an 8-frame animated strip (16x128, driven by the
sibling .png.mcmeta) of memory powder falling behind the window pane.

Pipeline (matches crystalize.py's mask workflow):
  1. Grains fall only inside the window's black *glass* (near-black interior,
     clipped to the inner rectangle) so they never touch the frame or bevel.
     The wrap period equals the glass height (8 rows) -> the loop is seamless.
  2. Grains are laid down in grey, with brightness varied per grain, then
     recoloured through crystalize.py --axis luma so each grain lands somewhere
     on the Memory Crystal's navy->gold palette. A white stencil masks the
     recolour to the grain pixels alone; the frame and glass pass through.

Re-run after tweaking grain columns/phases/brightness below:
    python gen_separator_sand.py
Requires Pillow + numpy (and crystalize.py + memory_crystal_transform.json here).
"""
import subprocess
import sys
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).parent
BLOCK = HERE / "../src/main/resources/assets/xpmagic/textures/block"
FRONT = BLOCK / "powder_separator_front.png"
OUT = BLOCK / "powder_separator_front_on.png"

FRAMES = 8
IY0, IH = 4, 8  # glass spans rows 4..11; wrap period == IH keeps the loop seamless

# (column, phase, grey): grey is the source brightness that --axis luma maps onto
# the crystal palette (dark -> navy, bright -> gold). Phase staggers the fall.
GRAINS = [
    (3, 0, 210), (3, 5, 90),
    (4, 2, 160),
    (5, 6, 235),
    (6, 1, 70),  (6, 4, 190),
    (7, 3, 140),
    (8, 7, 100),
    (9, 0, 225), (9, 5, 120),
    (10, 2, 80),
    (11, 6, 200),
    (12, 1, 150), (12, 4, 60),
]


def main():
    base = Image.open(FRONT).convert("RGBA")
    a = np.array(base)

    # Glass = near-black interior, clipped to the inner rectangle so the dark
    # outer border/rails are excluded -- grains land only on real glass.
    mx = a[:, :, :3].max(2)
    Y, X = np.mgrid[0:16, 0:16]
    glass = (a[:, :, 3] > 0) & (mx < 40) & (X >= 3) & (X <= 13) & (Y >= 4) & (Y <= 11)

    strip = Image.new("RGBA", (16, 16 * FRAMES))
    mask = Image.new("RGBA", (16, 16 * FRAMES), (0, 0, 0, 255))
    sp, mp = strip.load(), mask.load()

    for f in range(FRAMES):
        strip.paste(base, (0, f * 16))
        for (gx, ph, grey) in GRAINS:
            gy = IY0 + ((ph + f) % IH)
            if glass[gy, gx]:
                sp[gx, f * 16 + gy] = (grey, grey, grey, 255)
                mp[gx, f * 16 + gy] = (255, 255, 255, 255)
            ty = IY0 + ((ph + f - 1) % IH)  # dim trailing grain (the flow behind it)
            if glass[ty, gx]:
                t = max(30, grey - 90)
                sp[gx, f * 16 + ty] = (t, t, t, 255)
                mp[gx, f * 16 + ty] = (255, 255, 255, 255)

    src_png = HERE / "_sand_src.png"
    mask_png = HERE / "_sand_mask.png"
    strip.save(src_png)
    mask.save(mask_png)
    try:
        subprocess.run(
            [sys.executable, str(HERE / "crystalize.py"), str(src_png), str(OUT),
             "--axis", "luma", "--mask", str(mask_png)],
            check=True,
        )
    finally:
        src_png.unlink(missing_ok=True)
        mask_png.unlink(missing_ok=True)
    print("wrote", OUT.resolve())


if __name__ == "__main__":
    main()
