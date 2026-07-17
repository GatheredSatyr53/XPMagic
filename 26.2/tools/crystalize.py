"""Reproduce the diamond->Memory Crystal transform on any diamond-based sprite.

The crystal was NOT a plain luminance gradient map: it is the vanilla diamond
with a diagonal (x+y) gradient overlaid, plus colour-correction stages. All of
that collapses to one transferable rule:

    crystal_colour(x, y) = M( diamond_colour(x, y),  x + y )

M is baked into crystal_transform.json (a colour+position lookup, ~65 entries) --
NOT the vanilla texture itself, so no Mojang asset ships in the repo. Feed your
own local diamond tool sprites in; the gold accents fall out of position
(bottom-right of the diagonal), exactly as on the crystal.

The gradient axis is a per-tool choice (--axis), because a fixed x+y sweep cuts
across features that run parallel to it (e.g. a sword crossguard splits into two
colours). Aligning the axis with the tool's long dimension keeps perpendicular
features one colour:
    diag       t = x + y   (default; raw canvas coords -- exact crystal repro)
    blade      t = x - y   (along a bottom-left->top-right blade; gold at tip)
    blade-rev  t = y - x   (same axis, gold at pommel)
    luma       t = pixel luminance   (no position -> tileable; see below)
Rotated axes are stretched onto the learned gradient range; diag stays raw.

For a TILING texture (blocks), any position axis breaks seamlessness: opposite
edges land at different points on the sweep, so the tile shows a repeating
hotspot. Use --axis luma -- it drives t from each pixel's own brightness instead
of its position, so the output inherits the (seamless) source's tiling exactly
while still mapping the block's light/dark through the crystal's palette.

luma normally normalises t across THIS image's own luma min/max, so overlaying
anything (flowers on leaves) shifts the window and recolours the base too. Pin
the window with --range to stop that: recolour the plain texture normally, then
recolour the overlaid variant with --range ref:<plain.png> (or --range lo,hi).
Shared pixels then map identically and the overlay only adds; values outside the
window clamp to the palette ends.

Each crystal is one baked transform table (--table, default
memory_crystal_transform.json). Bake a new one per crystal with --learn, then
reuse the same script to apply any -- e.g. time_crystal_transform.json.

Usage:
    python crystalize.py <diamond_sprite.png> <out.png>
                                        [--axis diag|blade|blade-rev|luma]
                                        [--table <transform.json>]
                                        [--range lo,hi | --range ref:<plain.png>]
    python crystalize.py --learn <diamond.png> <crystal.png> [<out_table.json>]
                                                 # inputs stay local, only the
                                                 # colour+position table is written
Requires Pillow + numpy.
"""
import sys, json
from pathlib import Path
from PIL import Image
import numpy as np
from collections import defaultdict

HERE = Path(__file__).parent
DEFAULT_TABLE = "memory_crystal_transform.json"


def resolve_table(name):
    """Accept a path as given, else look for it next to this script."""
    p = Path(name)
    return p if p.exists() else HERE / name


def load_table(name=DEFAULT_TABLE):
    raw = json.loads(resolve_table(name).read_text())
    per = {}
    for col, td in raw.items():
        key = tuple(int(v) for v in col.split(","))
        per[key] = {int(t): np.array(rgb, float) for t, rgb in td.items()}
    return per


def learn(diamond_png, crystal_png, out_table=DEFAULT_TABLE):
    d = np.array(Image.open(diamond_png).convert("RGBA"))
    c = np.array(Image.open(crystal_png).convert("RGBA"))
    both = (d[:, :, 3] > 0) & (c[:, :, 3] > 0)
    ys, xs = np.where(both)
    acc = defaultdict(lambda: defaultdict(list))
    for (dr, dg, db), t, cr in zip(d[both][:, :3], xs + ys, c[both][:, :3]):
        acc[f"{dr},{dg},{db}"][int(t)].append([int(v) for v in cr])
    out = {col: {str(t): [int(round(v)) for v in np.mean(vs, 0)] for t, vs in td.items()}
           for col, td in acc.items()}
    dest = Path(out_table) if Path(out_table).parent != Path(".") else HERE / out_table
    dest.write_text(json.dumps(out, separators=(",", ":"), sort_keys=True))
    print("wrote", dest, f"({len(out)} diamond colours)")


def make_predict(per):
    keys = list(per)
    karr = np.array(keys)

    def predict(col, t):
        tbl = per.get(col)
        if not tbl:  # unseen diamond colour -> nearest in rgb
            col = keys[int(np.argmin(((karr - np.array(col)) ** 2).sum(1)))]
            tbl = per[col]
        ts = sorted(tbl)
        if t in tbl:
            return tbl[t]
        if t <= ts[0]:
            return tbl[ts[0]]
        if t >= ts[-1]:
            return tbl[ts[-1]]
        lo = max(x for x in ts if x < t)
        hi = min(x for x in ts if x > t)
        f = (t - lo) / (hi - lo)
        return tbl[lo] * (1 - f) + tbl[hi] * f

    return predict


def axis_field(a, axis, per, rng=None):
    """Return a per-pixel t map for the chosen gradient axis.

    diag keeps raw x+y (exact crystal repro); rotated axes are stretched onto
    the learned t-range so the same navy->gold sweep spans the sprite; luma
    drives t from pixel brightness (no position) so the result stays tileable.

    rng=(lo, hi) pins the normalisation window instead of taking it from this
    image's own min/max. Feed the same rng to a plain texture and to an overlaid
    variant (e.g. leaves vs flowering leaves) and shared pixels map identically,
    so the overlay can't shift the base's colours. Out-of-window values clamp."""
    h, w = a.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    if axis == "diag":
        return (xx + yy).astype(float)
    opaque = a[:, :, 3] > 0
    if axis == "luma":
        rgb = a[:, :, :3].astype(float)
        proj = 0.299 * rgb[:, :, 0] + 0.587 * rgb[:, :, 1] + 0.114 * rgb[:, :, 2]
    else:
        proj = (xx - yy) if axis == "blade" else (yy - xx)  # blade-rev
    lo, hi = rng if rng is not None else (proj[opaque].min(), proj[opaque].max())
    ts = [t for tbl in per.values() for t in tbl]
    tmin, tmax = min(ts), max(ts)
    frac = np.clip((proj - lo) / max(hi - lo, 1), 0.0, 1.0)
    return tmin + frac * (tmax - tmin)


def measure_range(inp, axis="luma"):
    """The (lo, hi) luma window a plain texture would use on its own -- pass it
    back as --range when recolouring an overlaid variant of the same texture."""
    a = np.array(Image.open(inp).convert("RGBA"))
    opaque = a[:, :, 3] > 0
    rgb = a[:, :, :3].astype(float)
    proj = 0.299 * rgb[:, :, 0] + 0.587 * rgb[:, :, 1] + 0.114 * rgb[:, :, 2]
    return float(proj[opaque].min()), float(proj[opaque].max())


def apply(inp, outp, axis="diag", table=DEFAULT_TABLE, rng=None):
    per = load_table(table)
    predict = make_predict(per)
    a = np.array(Image.open(inp).convert("RGBA"))
    tmap = axis_field(a, axis, per, rng)
    h, w = a.shape[:2]
    for y in range(h):
        for x in range(w):
            if a[y, x, 3] == 0:
                continue
            a[y, x, :3] = np.clip(predict(tuple(int(v) for v in a[y, x, :3]), tmap[y, x]), 0, 255)
    Image.fromarray(a, "RGBA").save(outp)
    print("wrote", outp, f"(axis={axis})")


def main(argv):
    if argv and argv[0] == "--learn":
        rest = argv[1:]
        if len(rest) == 2:
            return learn(rest[0], rest[1])
        if len(rest) == 3:
            return learn(rest[0], rest[1], rest[2])
        sys.exit(__doc__)
    axis, table, rng_arg = "diag", DEFAULT_TABLE, None
    pos = []
    i = 0
    while i < len(argv):
        a = argv[i]
        if a == "--axis" and i + 1 < len(argv):
            axis = argv[i + 1]; i += 2; continue
        if a.startswith("--axis="):
            axis = a.split("=", 1)[1]; i += 1; continue
        if a == "--table" and i + 1 < len(argv):
            table = argv[i + 1]; i += 2; continue
        if a.startswith("--table="):
            table = a.split("=", 1)[1]; i += 1; continue
        if a == "--range" and i + 1 < len(argv):
            rng_arg = argv[i + 1]; i += 2; continue
        if a.startswith("--range="):
            rng_arg = a.split("=", 1)[1]; i += 1; continue
        pos.append(a); i += 1
    if len(pos) != 2 or axis not in ("diag", "blade", "blade-rev", "luma"):
        sys.exit(__doc__)
    rng = None
    if rng_arg is not None:
        if rng_arg.startswith("ref:"):          # derive window from a reference png
            rng = measure_range(rng_arg[4:])
        else:                                    # explicit "lo,hi"
            rng = tuple(float(v) for v in rng_arg.split(","))
        print(f"pinned luma window: {rng[0]:.0f}..{rng[1]:.0f}")
    apply(pos[0], pos[1], axis, table, rng)


if __name__ == "__main__":
    main(sys.argv[1:])
