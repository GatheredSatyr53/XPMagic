"""Reproduce the diamond->Memory Crystal transform on any diamond-based sprite.

The crystal was NOT a plain luminance gradient map: it is the vanilla diamond
with a diagonal (x+y) gradient overlaid, plus colour-correction stages. All of
that collapses to one transferable rule:

    crystal_colour(x, y) = M( diamond_colour(x, y),  x + y )

M is baked into crystal_transform.json (a colour+position lookup, ~65 entries) --
NOT the vanilla texture itself, so no Mojang asset ships in the repo. Feed your
own local diamond tool sprites in; the gold accents fall out of position
(bottom-right of the diagonal), exactly as on the crystal.

Usage:
    python crystalize.py <diamond_sprite.png> <out.png>
    python crystalize.py --learn <diamond.png> <crystal.png>   # rebuild the JSON
                                                                 # (inputs stay local)
Requires Pillow + numpy.
"""
import sys, json
from pathlib import Path
from PIL import Image
import numpy as np
from collections import defaultdict

HERE = Path(__file__).parent
TABLE = HERE / "crystal_transform.json"


def load_table():
    raw = json.loads(TABLE.read_text())
    per = {}
    for col, td in raw.items():
        key = tuple(int(v) for v in col.split(","))
        per[key] = {int(t): np.array(rgb, float) for t, rgb in td.items()}
    return per


def learn(diamond_png, crystal_png):
    d = np.array(Image.open(diamond_png).convert("RGBA"))
    c = np.array(Image.open(crystal_png).convert("RGBA"))
    both = (d[:, :, 3] > 0) & (c[:, :, 3] > 0)
    ys, xs = np.where(both)
    acc = defaultdict(lambda: defaultdict(list))
    for (dr, dg, db), t, cr in zip(d[both][:, :3], xs + ys, c[both][:, :3]):
        acc[f"{dr},{dg},{db}"][int(t)].append([int(v) for v in cr])
    out = {col: {str(t): [int(round(v)) for v in np.mean(vs, 0)] for t, vs in td.items()}
           for col, td in acc.items()}
    TABLE.write_text(json.dumps(out, separators=(",", ":"), sort_keys=True))
    print("wrote", TABLE, f"({len(out)} diamond colours)")


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


def apply(inp, outp):
    predict = make_predict(load_table())
    a = np.array(Image.open(inp).convert("RGBA"))
    h, w = a.shape[:2]
    for y in range(h):
        for x in range(w):
            if a[y, x, 3] == 0:
                continue
            a[y, x, :3] = np.clip(predict(tuple(int(v) for v in a[y, x, :3]), x + y), 0, 255)
    Image.fromarray(a, "RGBA").save(outp)
    print("wrote", outp)


if __name__ == "__main__":
    if len(sys.argv) == 4 and sys.argv[1] == "--learn":
        learn(sys.argv[2], sys.argv[3])
    elif len(sys.argv) == 3:
        apply(sys.argv[1], sys.argv[2])
    else:
        sys.exit(__doc__)
