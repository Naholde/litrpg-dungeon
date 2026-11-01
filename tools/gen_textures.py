"""Текстуры предметов и сущностей мода (16×16 и 64×64), рисуются кодом.

    python tools/gen_textures.py                 — записать всё в src/main/resources/assets/litrpg/textures
    python tools/gen_textures.py --preview DIR   — ещё и увеличенные копии в DIR (смотреть через Read)
"""
import math
import random
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from png import Canvas, hexrgb, shade  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/litrpg/textures"
OUT = {}


def texture(path):
    def deco(fn):
        OUT[path] = fn
        return fn
    return deco


# ---------------------------------------------------------------------------- посох ученика

@texture("item/apprentice_staff.png")
def apprentice_staff():
    c = Canvas(16, 16)
    wood_dark, wood, wood_light = hexrgb("#4A2F1A"), hexrgb("#7A5230"), hexrgb("#A87A4A")
    gold, gold_light = hexrgb("#C9962E"), hexrgb("#F2D27A")
    gem_dark, gem, gem_light, gem_core = hexrgb("#1B6E9C"), hexrgb("#3FB6FF"), hexrgb("#A6E1FF"), hexrgb("#FFFFFF")
    # древко по диагонали: снизу слева вверх направо, в два пикселя с бликом
    for i in range(11):
        x, y = 1 + i, 14 - i
        c.set(x, y, wood_dark)
        c.set(x + 1, y, wood if i % 3 else wood_light)
        c.set(x, y - 1, wood_light if i % 4 == 1 else wood)
    # оплётка у навершия и в середине
    for (x, y) in [(9, 6), (10, 5), (10, 6), (5, 10), (6, 9)]:
        c.set(x, y, gold)
    c.set(10, 5, gold_light)
    c.set(6, 9, gold_light)
    # навершие: «лапки», держащие кристалл
    for (x, y) in [(11, 4), (12, 4)]:
        c.set(x, y, gold)
    # кристалл-ромб
    for (x, y, col) in [(13, 1, gem_light), (12, 2, gem_light), (13, 2, gem_core), (14, 2, gem),
                        (12, 3, gem), (13, 3, gem_light), (14, 3, gem_dark), (13, 4, gem_dark), (15, 2, gem_dark)]:
        c.set(x, y, col)
    # искорка
    c.blend(15, 0, (166, 225, 255, 160))
    c.blend(11, 0, (166, 225, 255, 90))
    return c


# ---------------------------------------------------------------------------- сгусток магии

@texture("entity/magic_bolt.png")
def magic_bolt():
    """Белый мягкий сгусток — цвет даёт рендерер (цвет посоха)."""
    c = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            a = max(0.0, 1 - r / 7.6) ** 1.6
            star = max(0.0, 1 - min(abs(dx), abs(dy)) / 1.2) * max(0.0, 1 - r / 8) * 0.6
            v = min(1.0, a + star)
            c.set(x, y, (255, 255, 255, round(255 * v)))
    return c


# ---------------------------------------------------------------------------- манекен (раскладка зомби 64×64)

def cube(c, u, v, w, h, d, paint):
    """Раскрасить развёртку куба: paint(face, x, y, fx, fy) для каждой точки грани."""
    faces = {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }
    for name, (x0, y0, fw, fh) in faces.items():
        for fy in range(fh):
            for fx in range(fw):
                c.set(x0 + fx, y0 + fy, paint(name, fx, fy, fw, fh))


@texture("entity/training_dummy.png")
def training_dummy():
    c = Canvas(64, 64)
    rnd = random.Random(7)
    straw = [hexrgb(h) for h in ("#C9A55A", "#D8B868", "#B8924A", "#E2C77E", "#A8843E")]
    sack = [hexrgb(h) for h in ("#B89668", "#A88858", "#C6A678", "#9C7C4E")]
    rope, rope_dark = hexrgb("#6E4E28"), hexrgb("#4E3418")
    stitch = hexrgb("#3A2412")
    red, white = hexrgb("#C8322E"), hexrgb("#EDE3CF")

    def straw_px(fx, fy):
        base = straw[rnd.randrange(len(straw))]
        if (fx + fy * 3) % 7 == 0:
            base = shade(base, 0.8)
        return base

    def head(face, fx, fy, fw, fh):
        col = sack[rnd.randrange(len(sack))]
        if face == "front":
            if (fx, fy) in [(1, 2), (2, 3), (1, 3), (2, 2), (5, 2), (6, 3), (5, 3), (6, 2)]:
                return stitch  # глаза-крестики
            if fy == 5 and 2 <= fx <= 5:
                return stitch if fx % 2 == 0 else shade(col, 0.7)  # шов-рот
        if face == "top" and (fx + fy) % 3 == 0:
            return straw_px(fx, fy)  # солома торчит из мешка
        if fy == fh - 1 and face != "top":
            return rope  # завязка на шее
        return col

    def body(face, fx, fy, fw, fh):
        if fy in (6, 7):
            return rope_dark if fy == 7 else rope  # пояс
        if face == "front":
            cx, cy = (fw - 1) / 2, 3.5
            r = math.hypot(fx - cx, fy - cy)
            if r < 1.2:
                return red
            if r < 2.2:
                return white
            if r < 3.3:
                return red
        return straw_px(fx, fy)

    def limb(face, fx, fy, fw, fh):
        if face in ("top", "bottom"):
            return shade(straw[0], 0.85)
        if fy in (3, 9):
            return rope
        return straw_px(fx, fy)

    cube(c, 0, 0, 8, 8, 8, head)
    cube(c, 16, 16, 8, 12, 4, body)
    cube(c, 40, 16, 4, 12, 4, limb)
    cube(c, 0, 16, 4, 12, 4, limb)
    return c


def main():
    preview = None
    if "--preview" in sys.argv:
        preview = Path(sys.argv[sys.argv.index("--preview") + 1])
    for rel, fn in OUT.items():
        img = fn()
        img.save(ROOT / rel)
        if preview:
            k = 16 if img.w <= 16 else 6
            img.scaled(k).save(preview / rel.replace("/", "_"))
        print("written", rel)


if __name__ == "__main__":
    main()
