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


# ---------------------------------------------------------------------------- посох Системы

@texture("item/staff.png")
def system_staff():
    c = Canvas(16, 16)
    dark, wood, light = hexrgb("#2E2238"), hexrgb("#4B3760"), hexrgb("#6E5590")
    silver, silver_l = hexrgb("#9AA4B8"), hexrgb("#D9E0EC")
    orb_d, orb, orb_l = hexrgb("#5A2E9C"), hexrgb("#B46BFF"), hexrgb("#E7C9FF")
    for i in range(10):
        x, y = 2 + i, 14 - i
        c.set(x, y, dark)
        c.set(x + 1, y, wood if i % 2 else light)
    for (x, y) in [(5, 10), (6, 10), (6, 9), (9, 6), (10, 6), (10, 5)]:
        c.set(x, y, silver)
    c.set(6, 9, silver_l)
    # кольцо-оправа и сфера
    for (x, y) in [(11, 2), (12, 1), (13, 1), (14, 2), (14, 3), (14, 4), (13, 5), (12, 5), (11, 4), (11, 3)]:
        c.set(x, y, silver)
    for (x, y, col) in [(12, 2, orb_l), (13, 2, orb), (12, 3, orb), (13, 3, orb_d), (12, 4, orb_d), (13, 4, orb_d)]:
        c.set(x, y, col)
    c.blend(15, 0, (231, 201, 255, 150))
    return c


# ---------------------------------------------------------------------------- украшения

@texture("item/ring.png")
def ring():
    c = Canvas(16, 16)
    gold, gold_d, gold_l = hexrgb("#D9A53A"), hexrgb("#8C6420"), hexrgb("#FFE29A")
    gem, gem_l = hexrgb("#3FB6FF"), hexrgb("#BDEBFF")
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 9) ** 2) ** 0.5
            if 3.2 <= d <= 5.2:
                c.set(x, y, gold_l if y < 8 and x < 8 else gold if d < 4.6 else gold_d)
    for (x, y, col) in [(7, 2, gem_l), (8, 2, gem), (6, 3, gem), (7, 3, gem), (8, 3, gem), (9, 3, gem), (7, 4, gem), (8, 4, gem)]:
        c.set(x, y, col)
    for (x, y) in [(6, 4), (9, 4), (6, 2), (9, 2)]:
        c.set(x, y, gold_d)
    return c


@texture("item/amulet.png")
def amulet():
    c = Canvas(16, 16)
    chain, chain_d = hexrgb("#B8BCC6"), hexrgb("#6E7380")
    gold, gold_d = hexrgb("#D9A53A"), hexrgb("#8C6420")
    gem, gem_d, gem_l = hexrgb("#D83A5A"), hexrgb("#7A1430"), hexrgb("#FF9AB0")
    for i in range(7):
        c.set(3 + i // 2, 1 + i, chain if i % 2 else chain_d)
        c.set(12 - i // 2, 1 + i, chain if i % 2 else chain_d)
    for y in range(8, 15):
        for x in range(4, 12):
            d = abs(x - 7.5) + abs(y - 11)
            if d <= 3.6:
                c.set(x, y, gold if d > 2.4 else gem_d if y > 11 else gem)
    c.set(7, 10, gem_l)
    c.set(6, 11, gem_l)
    c.set(7, 14, gold_d)
    c.set(8, 14, gold_d)
    return c


@texture("item/charm.png")
def charm():
    c = Canvas(16, 16)
    bone, bone_d = hexrgb("#E8DCC0"), hexrgb("#9C8C6C")
    cord = hexrgb("#6E4E28")
    rune = hexrgb("#5FC8FF")
    for x in range(3, 13):
        c.set(x, 2, cord)
    c.set(7, 3, cord)
    c.set(8, 3, cord)
    for y in range(4, 14):
        w = 3 if y < 6 or y > 11 else 4
        for x in range(8 - w, 8 + w):
            c.set(x, y, bone if (x + y) % 5 else bone_d)
    for (x, y) in [(7, 6), (8, 7), (7, 8), (8, 9), (7, 10), (6, 8), (9, 8)]:
        c.set(x, y, rune)
    return c


# ---------------------------------------------------------------------------- монеты

def coin(base, dark, light, mark):
    c = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d <= 6.2:
                col = dark if d > 5.3 else light if (x + y) < 12 and d < 4 else base
                c.set(x, y, col)
    for (x, y) in mark:
        c.set(x, y, dark)
    return c


SIGIL = [(7, 4), (8, 4), (6, 5), (9, 5), (6, 6), (9, 6), (7, 7), (8, 7), (6, 8), (9, 8), (6, 9), (9, 9), (7, 10), (8, 10)]


@texture("item/copper_coin.png")
def copper_coin():
    return coin(hexrgb("#C2703A"), hexrgb("#7A3E1A"), hexrgb("#E8A070"), SIGIL)


@texture("item/silver_coin.png")
def silver_coin():
    return coin(hexrgb("#B8C0CC"), hexrgb("#6E7684"), hexrgb("#E8EEF6"), SIGIL)


@texture("item/gold_coin.png")
def gold_coin():
    return coin(hexrgb("#E0B040"), hexrgb("#8C6420"), hexrgb("#FFE79A"), SIGIL)


# ---------------------------------------------------------------------------- эликсир и свиток

@texture("item/elixir_of_life.png")
def elixir():
    c = Canvas(16, 16)
    glass, glass_d = hexrgb("#CFE8F0"), hexrgb("#7FA0B0")
    liquid, liquid_l, liquid_d = hexrgb("#E0303A"), hexrgb("#FF8A8A"), hexrgb("#8C1018")
    cork = hexrgb("#8C6438")
    for x in range(7, 9):
        c.set(x, 1, cork)
        c.set(x, 2, cork)
    for y in range(3, 6):
        c.set(6, y, glass_d)
        c.set(9, y, glass_d)
        c.set(7, y, glass)
        c.set(8, y, glass)
    for y in range(6, 15):
        for x in range(3, 13):
            d = ((x - 7.5) ** 2 + (y - 10.5) ** 2) ** 0.5
            if d <= 4.6:
                edge = d > 3.9
                col = glass_d if edge else liquid_l if (x < 7 and y < 10) else liquid if y > 8 else glass
                if not edge and y > 12:
                    col = liquid_d
                c.set(x, y, col)
    c.set(5, 9, (255, 255, 255, 220))
    return c


@texture("item/scroll_of_oblivion.png")
def scroll():
    c = Canvas(16, 16)
    paper, paper_d = hexrgb("#E8DDB8"), hexrgb("#B0A07A")
    roll, roll_d = hexrgb("#8C6438"), hexrgb("#5A3E20")
    ink = hexrgb("#5A4AA0")
    for y in range(3, 13):
        for x in range(3, 13):
            c.set(x, y, paper if (x + y) % 7 else paper_d)
    for y in range(2, 14):
        c.set(2, y, roll)
        c.set(13, y, roll)
    c.set(2, 2, roll_d)
    c.set(13, 13, roll_d)
    for y in (5, 7, 9, 11):
        for x in range(5, 11):
            if (x * 3 + y) % 4:
                c.set(x, y, ink)
    return c


# ---------------------------------------------------------------------------- блоки подземелья

@texture("block/seal_top.png")
def seal_top():
    """Печать сверху: золотой круг с рунами на чёрном камне."""
    c = Canvas(16, 16)
    stone, stone_l = hexrgb("#1E1A22"), hexrgb("#2C2732")
    gold, gold_l, gold_d = hexrgb("#C9962E"), hexrgb("#F2D27A"), hexrgb("#7A5418")
    rune = hexrgb("#B88CFF")
    for y in range(16):
        for x in range(16):
            c.set(x, y, stone_l if (x * 7 + y * 3) % 11 == 0 else stone)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 6.0 <= d < 7.2:
                c.set(x, y, gold_l if (x + y) % 3 == 0 else gold)
            elif 3.4 <= d < 4.2:
                c.set(x, y, gold_d)
    for (x, y) in [(7, 2), (8, 2), (13, 7), (13, 8), (7, 13), (8, 13), (2, 7), (2, 8)]:
        c.set(x, y, rune)
    for (x, y) in [(7, 7), (8, 8), (7, 8), (8, 7)]:
        c.set(x, y, (255, 240, 200, 255))
    for (x, y) in [(6, 7), (9, 8), (7, 6), (8, 9)]:
        c.set(x, y, gold_l)
    return c


@texture("block/seal_side.png")
def seal_side():
    c = Canvas(16, 16)
    stone, stone_l, stone_d = hexrgb("#1E1A22"), hexrgb("#2C2732"), hexrgb("#141117")
    gold, gold_l = hexrgb("#C9962E"), hexrgb("#F2D27A")
    for y in range(16):
        for x in range(16):
            c.set(x, y, stone_d if y in (0, 15) else stone_l if (x * 5 + y * 3) % 9 == 0 else stone)
    for x in range(16):
        c.set(x, 2, gold if x % 4 else gold_l)
        c.set(x, 13, gold if x % 4 else gold_l)
    for x in (3, 7, 8, 12):
        for y in (6, 7, 8, 9):
            c.set(x, y, hexrgb("#9B6CFF") if (x + y) % 2 else hexrgb("#6A44C0"))
    return c


@texture("block/sealed_gate.png")
def sealed_gate():
    """Печать-преграда: полупрозрачная фиолетовая решётка рун, 4 кадра мерцания."""
    frames = 4
    c = Canvas(16, 16 * frames)
    for f in range(frames):
        for y in range(16):
            for x in range(16):
                wave = 0.5 + 0.5 * math.sin((x + y * 0.7 + f * 4) * 0.8)
                a = int(95 + 50 * wave)
                base = (int(120 + 40 * wave), int(60 + 30 * wave), 230, a)
                if x % 8 == 0 or y % 8 == 0:
                    base = (200, 150, 255, 200)
                if (x + y + f) % 8 == 3 and (x - y) % 4 == 0:
                    base = (240, 220, 255, 230)
                c.set(x, 16 * f + y, base)
    return c


@texture("block/rift_on.png")
def rift_on():
    """Открытый разлом: вихрь пурпура и черноты, 8 кадров."""
    frames = 8
    c = Canvas(16, 16 * frames)
    for f in range(frames):
        t = f / frames * math.tau
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5
                r = math.hypot(dx, dy)
                ang = math.atan2(dy, dx)
                v = 0.5 + 0.5 * math.sin(ang * 3 + r * 0.9 - t * 2)
                k = max(0.0, 1 - r / 11)
                col = (int(40 + 200 * v * k), int(10 + 40 * v * k), int(70 + 170 * k), int(170 + 70 * k))
                if v > 0.92 and r < 7:
                    col = (255, 200, 255, 255)
                c.set(x, 16 * f + y, col)
    return c


@texture("block/rift_off.png")
def rift_off():
    """Закрытый разлом: тёмная трещина с тусклыми искрами."""
    c = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            c.set(x, y, (12, 6, 20, 150))
    for y in range(16):
        x = int(7.5 + 2.5 * math.sin(y * 0.9))
        c.set(x, y, (120, 50, 160, 220))
        c.set(x + 1, y, (70, 25, 100, 200))
    for (x, y) in [(3, 4), (12, 9), (5, 13), (11, 2)]:
        c.set(x, y, (180, 90, 220, 200))
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
