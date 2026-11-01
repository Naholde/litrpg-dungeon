"""Модели GeckoLib (geo + анимации + текстура + маска свечения), описанные кодом.

    python tools/gen_models.py [--only имя] [--preview build/preview]

Модель — список костей с кубами. Развёртка (box UV) раскладывается по атласу сама; каждая грань куба
красится своим материалом (камень, бронза, кристалл…), светящиеся материалы попадают в маску свечения.
Анимации — словарь «кость → ключевые кадры», формат Bedrock 1.8.0.
"""
import json
import math
import random
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from png import Canvas, hexrgb, shade  # noqa: E402

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/litrpg"

# ---------------------------------------------------------------------------- материалы

MATERIALS = {
    # имя: (основной цвет, разброс, светится, узор)
    "slate": ("#3A4150", 0.10, False, "runes"),
    "slate_dark": ("#272C36", 0.08, False, "plain"),
    "bronze": ("#9C6B2E", 0.12, False, "rivets"),
    "bronze_dark": ("#6B4520", 0.10, False, "plain"),
    "crystal": ("#3FD8FF", 0.10, True, "facets"),
    "visor": ("#7FF0FF", 0.05, True, "plain"),
    "rune": ("#5FC8FF", 0.05, True, "plain"),
}


def paint(mat, x, y, w, h, rnd):
    """Цвет точки грани материала (x, y — внутри грани w×h)."""
    base, var, _, pattern = MATERIALS[mat]
    c = hexrgb(base)
    c = shade(c, 1 + rnd.uniform(-var, var))
    if pattern == "runes" and w >= 6 and h >= 6:
        # тонкие светлые линии-руны, как резьба по камню
        if (x + 2 * y) % 9 == 0 and 1 <= x < w - 1 and 1 <= y < h - 1:
            c = shade(hexrgb("#5A6478"), 1.05)
    if pattern == "rivets" and (x % 4 == 1 and y % 4 == 1):
        c = shade(c, 1.35)
    if pattern == "facets":
        c = shade(c, 1.25 if (x + y) % 3 == 0 else 0.92)
    # края граней чуть темнее — объём
    if x == 0 or y == 0 or x == w - 1 or y == h - 1:
        c = shade(c, 0.82)
    return c


def glows(mat):
    return MATERIALS[mat][2]


# ---------------------------------------------------------------------------- описание модели

class Cube:
    def __init__(self, origin, size, mat, faces=None, inflate=0.0):
        self.origin, self.size, self.mat, self.inflate = origin, size, mat, inflate
        self.faces = faces or {}  # переопределение материала граней: {"north": "visor"}
        self.uv = None


class Bone:
    def __init__(self, name, pivot, parent=None, cubes=(), rotation=None):
        self.name, self.pivot, self.parent, self.cubes = name, pivot, parent, list(cubes)
        self.rotation = rotation


def layout(bones, tex_w):
    """Полочная раскладка развёрток: каждой коробке нужно (2d+2w) × (d+h)."""
    x = y = shelf = 0
    for b in bones:
        for c in b.cubes:
            w, h, d = (math.ceil(v) for v in c.size)
            uw, uh = 2 * d + 2 * w, d + h
            if x + uw > tex_w:
                x, y, shelf = 0, y + shelf, 0
            c.uv = (x, y)
            x += uw
            shelf = max(shelf, uh)
    return y + shelf


def faces_of(c):
    w, h, d = (math.ceil(v) for v in c.size)
    u, v = c.uv
    # раскладка box UV Bedrock/Java
    return {
        "up": (u + d, v, w, d), "down": (u + d + w, v, w, d),
        "east": (u, v + d, d, h), "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h),
    }


def build(name, bones, anims, tex_w=128, tex_h=128, seed=1):
    used = layout(bones, tex_w)
    assert used <= tex_h, f"{name}: развёртки не влезли в {tex_w}×{tex_h} ({used})"
    tex, glow = Canvas(tex_w, tex_h), Canvas(tex_w, tex_h)
    rnd = random.Random(seed)
    for b in bones:
        for c in b.cubes:
            for face, (fx, fy, fw, fh) in faces_of(c).items():
                mat = c.faces.get(face, c.mat)
                for yy in range(fh):
                    for xx in range(fw):
                        col = paint(mat, xx, yy, fw, fh, rnd)
                        tex.set(fx + xx, fy + yy, col)
                        if glows(mat):
                            glow.set(fx + xx, fy + yy, col)
    geo = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": f"geometry.{name}", "texture_width": tex_w, "texture_height": tex_h,
                "visible_bounds_width": 6, "visible_bounds_height": 6, "visible_bounds_offset": [0, 2, 0],
            },
            "bones": [],
        }],
    }
    for b in bones:
        bj = {"name": b.name, "pivot": list(b.pivot)}
        if b.parent:
            bj["parent"] = b.parent
        if b.rotation:
            bj["rotation"] = list(b.rotation)
        if b.cubes:
            bj["cubes"] = []
            for c in b.cubes:
                cj = {"origin": list(c.origin), "size": list(c.size), "uv": list(c.uv)}
                if c.inflate:
                    cj["inflate"] = c.inflate
                bj["cubes"].append(cj)
        geo["minecraft:geometry"][0]["bones"].append(bj)
    anim = {"format_version": "1.8.0", "animations": {}}
    for aname, a in anims.items():
        anim["animations"][f"animation.{name}.{aname}"] = a
    return geo, anim, tex, glow


# ---------------------------------------------------------------------------- анимации: помощники

def kf(times_values):
    """{время: [x,y,z]} → ключевые кадры Bedrock."""
    return {f"{t:g}": list(v) for t, v in times_values}


def loop(length, bones):
    return {"loop": True, "animation_length": length, "bones": bones}


def once(length, bones):
    return {"animation_length": length, "bones": bones}


# ---------------------------------------------------------------------------- Испытатель Системы

def trial_warden():
    S, B, C = "slate", "bronze", "crystal"
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("legs", [0, 12, 0], "root"),
        Bone("left_leg", [3, 12, 0], "legs", [Cube([1, 0, -2.5], [5, 12, 5], S), Cube([0.5, 0, -3], [6, 3, 6], B)]),
        Bone("right_leg", [-3, 12, 0], "legs", [Cube([-6, 0, -2.5], [5, 12, 5], S), Cube([-6.5, 0, -3], [6, 3, 6], B)]),
        Bone("body", [0, 12, 0], "root", [
            Cube([-6, 12, -3.5], [12, 14, 7], S),
            Cube([-7, 22, -4], [14, 4, 8], B),          # наплечный пояс
            Cube([-3, 13, -4.5], [6, 2, 1], B),          # пряжка
        ]),
        Bone("core", [0, 19, -4], "body", [Cube([-2.5, 16.5, -5.5], [5, 5, 2], C)]),
        Bone("head", [0, 26, 0], "body", [
            Cube([-4.5, 26, -4.5], [9, 9, 9], S, faces={"north": "slate_dark"}),
            Cube([-3.5, 29, -5], [7, 2, 1], "visor"),     # светящаяся щель-визор
            Cube([-5, 34, -5], [10, 2, 10], B),          # венец
        ]),
        Bone("halo", [0, 40, 0], "head", [
            Cube([-1, 39, -7], [2, 2, 2], "rune"), Cube([-1, 39, 5], [2, 2, 2], "rune"),
            Cube([-7, 39, -1], [2, 2, 2], "rune"), Cube([5, 39, -1], [2, 2, 2], "rune"),
            Cube([-5.5, 39, -5.5], [2, 2, 2], "rune"), Cube([3.5, 39, 3.5], [2, 2, 2], "rune"),
            Cube([-5.5, 39, 3.5], [2, 2, 2], "rune"), Cube([3.5, 39, -5.5], [2, 2, 2], "rune"),
        ]),
        Bone("right_arm", [-7, 24, 0], "body", [
            Cube([-12, 22, -3], [5, 5, 6], B),           # наплечник
            Cube([-11, 10, -2], [4, 13, 4], S),
            Cube([-12, 4, -3], [6, 7, 6], "bronze_dark"),  # кулак-молот
        ]),
        Bone("left_arm", [7, 24, 0], "body", [
            Cube([7, 22, -3], [5, 5, 6], B),
            Cube([7, 10, -2], [4, 13, 4], S),
            Cube([6, 4, -3], [6, 7, 6], "bronze_dark"),
        ]),
    ]
    anims = {
        "idle": loop(2.4, {
            "body": {"position": kf([(0, [0, 0, 0]), (1.2, [0, -0.6, 0]), (2.4, [0, 0, 0])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (1.2, [3, 0, 0]), (2.4, [0, 0, 0])])},
            "halo": {"rotation": kf([(0, [0, 0, 0]), (2.4, [0, 360, 0])])},
            "core": {"scale": kf([(0, [1, 1, 1]), (1.2, [1.15, 1.15, 1.15]), (2.4, [1, 1, 1])])},
            "right_arm": {"rotation": kf([(0, [0, 0, 0]), (1.2, [-4, 0, 3]), (2.4, [0, 0, 0])])},
            "left_arm": {"rotation": kf([(0, [0, 0, 0]), (1.2, [-4, 0, -3]), (2.4, [0, 0, 0])])},
        }),
        "walk": loop(1.2, {
            "left_leg": {"rotation": kf([(0, [25, 0, 0]), (0.6, [-25, 0, 0]), (1.2, [25, 0, 0])])},
            "right_leg": {"rotation": kf([(0, [-25, 0, 0]), (0.6, [25, 0, 0]), (1.2, [-25, 0, 0])])},
            "left_arm": {"rotation": kf([(0, [-18, 0, 0]), (0.6, [18, 0, 0]), (1.2, [-18, 0, 0])])},
            "right_arm": {"rotation": kf([(0, [18, 0, 0]), (0.6, [-18, 0, 0]), (1.2, [18, 0, 0])])},
            "body": {"position": kf([(0, [0, 0, 0]), (0.3, [0, -0.8, 0]), (0.6, [0, 0, 0]), (0.9, [0, -0.8, 0]), (1.2, [0, 0, 0])])},
            "halo": {"rotation": kf([(0, [0, 0, 0]), (1.2, [0, 180, 0])])},
        }),
        "attack": once(0.9, {
            "right_arm": {"rotation": kf([(0, [0, 0, 0]), (0.35, [-150, 0, 10]), (0.55, [-20, 0, 0]), (0.9, [0, 0, 0])])},
            "body": {"rotation": kf([(0, [0, 0, 0]), (0.35, [-8, 15, 0]), (0.55, [12, -10, 0]), (0.9, [0, 0, 0])])},
        }),
        "slam": once(1.2, {
            "right_arm": {"rotation": kf([(0, [0, 0, 0]), (0.5, [-170, 0, 15]), (0.75, [-10, 0, 0]), (1.2, [0, 0, 0])])},
            "left_arm": {"rotation": kf([(0, [0, 0, 0]), (0.5, [-170, 0, -15]), (0.75, [-10, 0, 0]), (1.2, [0, 0, 0])])},
            "body": {"rotation": kf([(0, [0, 0, 0]), (0.5, [-12, 0, 0]), (0.75, [20, 0, 0]), (1.2, [0, 0, 0])])},
        }),
        "cast": loop(1.0, {
            "right_arm": {"rotation": kf([(0, [-160, 0, 20]), (0.5, [-165, 0, 25]), (1.0, [-160, 0, 20])])},
            "left_arm": {"rotation": kf([(0, [-160, 0, -20]), (0.5, [-165, 0, -25]), (1.0, [-160, 0, -20])])},
            "halo": {"rotation": kf([(0, [0, 0, 0]), (1.0, [0, 720, 0])]),
                     "position": kf([(0, [0, 2, 0]), (0.5, [0, 3, 0]), (1.0, [0, 2, 0])])},
            "core": {"scale": kf([(0, [1.3, 1.3, 1.3]), (0.5, [1.6, 1.6, 1.6]), (1.0, [1.3, 1.3, 1.3])])},
            "head": {"rotation": kf([(0, [-15, 0, 0]), (1.0, [-15, 0, 0])])},
        }),
        "death": {"animation_length": 2.0, "hold_on_last_frame": True, "bones": {
            "root": {"rotation": kf([(0, [0, 0, 0]), (1.2, [0, 0, 0]), (2.0, [90, 0, 0])]),
                     "position": kf([(0, [0, 0, 0]), (2.0, [0, -4, 0])])},
            "halo": {"scale": kf([(0, [1, 1, 1]), (0.8, [0, 0, 0])])},
            "core": {"scale": kf([(0, [1.5, 1.5, 1.5]), (1.0, [0, 0, 0])])},
        }},
    }
    return bones, anims


# ---------------------------------------------------------------------------- Узел силы

def power_node():
    C = "crystal"
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("base", [0, 0, 0], "root", [
            Cube([-5, 0, -5], [10, 3, 10], "slate"),
            Cube([-4, 3, -4], [8, 2, 8], "bronze"),
            Cube([-1, 5, -6], [2, 2, 2], "rune"), Cube([-1, 5, 4], [2, 2, 2], "rune"),
            Cube([-6, 5, -1], [2, 2, 2], "rune"), Cube([4, 5, -1], [2, 2, 2], "rune"),
        ]),
        Bone("crystal", [0, 14, 0], "root", [
            Cube([-3, 9, -3], [6, 10, 6], C),
            Cube([-2, 19, -2], [4, 4, 4], C),
            Cube([-2, 6, -2], [4, 3, 4], C),
        ]),
    ]
    anims = {"idle": loop(3.0, {
        "crystal": {"rotation": kf([(0, [0, 0, 0]), (3.0, [0, 360, 0])]),
                    "position": kf([(0, [0, 0, 0]), (1.5, [0, 1.5, 0]), (3.0, [0, 0, 0])])},
    })}
    return bones, anims


MODELS = {"trial_warden": trial_warden, "power_node": power_node}


def main():
    only = sys.argv[sys.argv.index("--only") + 1] if "--only" in sys.argv else None
    preview = Path(sys.argv[sys.argv.index("--preview") + 1]) if "--preview" in sys.argv else None
    for name, fn in MODELS.items():
        if only and name != only:
            continue
        bones, anims = fn()
        geo, anim, tex, glow = build(name, bones, anims, tex_w=128, tex_h=128 if name != "power_node" else 64)
        (ASSETS / "geo/entity").mkdir(parents=True, exist_ok=True)
        (ASSETS / "animations/entity").mkdir(parents=True, exist_ok=True)
        (ASSETS / f"geo/entity/{name}.geo.json").write_text(json.dumps(geo, indent=1), encoding="utf-8")
        (ASSETS / f"animations/entity/{name}.animation.json").write_text(json.dumps(anim, indent=1), encoding="utf-8")
        tex.save(ASSETS / f"textures/entity/{name}.png")
        glow.save(ASSETS / f"textures/entity/{name}_glowmask.png")
        if preview:
            tex.scaled(4).save(preview / f"{name}_tex.png")
        print("written", name)


if __name__ == "__main__":
    main()
