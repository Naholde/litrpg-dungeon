"""Превью облика класса на манекене игрока.
    python -I tools/look_preview.py guardian [--size 420]        → build/preview/looks/guardian.png (3 ракурса)
    python -I tools/look_preview.py --sheet guardian,paladin,...  → build/preview/looks/sheet.png
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "tools" / "models"))
sys.path.insert(0, str(ROOT / "tools"))
import hy  # noqa: E402
import looks  # noqa: E402
import model_preview as mp  # noqa: E402
from png import Canvas  # noqa: E402

OUT = ROOT / "build/preview/looks"
TMP = ROOT / "build/preview/look_assets"
hy.ASSETS = TMP
mp.ASSETS = TMP
(TMP / "textures/entity").mkdir(parents=True, exist_ok=True)

hy.mat("mq_skin", "#C8986E", "skin", dark="#8A6446", light="#ECC09A")
hy.mat("mq_hair", "#3A2618", "hair", dark="#1E140C", light="#5A3E28")
hy.mat("mq_shirt", "#3E8A9A", "cloth", dark="#1E4A54", light="#6AB4C4")
hy.mat("mq_pants", "#3A3E8A", "cloth", dark="#1C1E4A", light="#5E64B4")
hy.mat("mq_shoe", "#4A4A4E", "leather", dark="#242428", light="#707076")


def face(f):
    w, h = f.w, f.h
    for sx in (int(w * 0.18), int(w * 0.62)):
        f.rect(sx, int(h * 0.5), sx + 3, int(h * 0.5) + 2, "#F4F4F4")
        f.rect(sx + 1, int(h * 0.5), sx + 3, int(h * 0.5) + 2, "#3A2A8A")
    f.rect(int(w * 0.38), int(h * 0.75), int(w * 0.62), int(h * 0.8), "#6A3A2A")
    f.rect(0, 0, w, int(h * 0.22), "#3A2618")


def mannequin(m):
    m.box("head", [0, 28, 0], [8, 8, 8], "mq_skin", decal={"north": face})
    m.box("body", [0, 18, 0], [8, 12, 4], "mq_shirt")
    for b, sx in (("right_arm", -1), ("left_arm", 1)):
        m.box(b, [sx * 6, 18, 0], [4, 12, 4], "mq_skin")
    for b, sx in (("right_leg", -1), ("left_leg", 1)):
        m.box(b, [sx * 1.9, 6, 0], [4, 12, 4], "mq_pants")


def build(cls):
    m = looks.MODELS["look_" + cls]()
    mannequin(m)
    m.name = "demo_" + cls
    m.save()
    return m.name


def main():
    a = sys.argv[1:]
    size = int(a[a.index("--size") + 1]) if "--size" in a else 420
    OUT.mkdir(parents=True, exist_ok=True)
    if "--sheet" in a:
        names = a[a.index("--sheet") + 1].split(",")
        yaw = float(a[a.index("--yaw") + 1]) if "--yaw" in a else -30
        tiles = [mp.render(build(n), size, yaws=(yaw,)) for n in names]
        cols = min(6, len(tiles))
        rows = (len(tiles) + cols - 1) // cols
        sheet = Canvas(size * cols, size * rows)
        for i, t in enumerate(tiles):
            ox, oy = (i % cols) * size, (i // cols) * size
            for y in range(size):
                for x in range(size):
                    sheet.set(ox + x, oy + y, t.get(x, y))
        out = OUT / (a[a.index("--out") + 1] if "--out" in a else "sheet.png")
        sheet.save(out)
        print(out)
        return
    cls = a[0]
    out = OUT / f"{cls}.png"
    mp.render(build(cls), size, yaws=(-30, 90, 160)).save(out)
    print(out)


if __name__ == "__main__":
    main()
