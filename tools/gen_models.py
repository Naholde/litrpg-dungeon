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


# ---------------------------------------------------------------------------- Торговец привала

MATERIALS.update({
    "robe": ("#3A2E5A", 0.08, False, "plain"),
    "robe_trim": ("#C9A04A", 0.08, False, "rivets"),
    "skin": ("#C69C7A", 0.05, False, "plain"),
    "leather": ("#6B4A2E", 0.10, False, "plain"),
    "lantern": ("#FFD27A", 0.05, True, "plain"),
    "eyes": ("#7FF0FF", 0.0, True, "plain"),
})


def trader():
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 12, 0], "root", [
            Cube([-4, 0, -3], [8, 12, 6], "robe"),             # полы мантии до земли
            Cube([-4.5, 12, -3.5], [9, 11, 7], "robe"),
            Cube([-4.5, 12, -3.6], [9, 1, 1], "robe_trim"),
            Cube([-1, 13, -3.8], [2, 9, 1], "robe_trim"),
            Cube([-5, 14, 3.5], [10, 10, 4], "leather"),      # заплечный мешок
        ]),
        Bone("head", [0, 23, 0], "body", [
            Cube([-4, 23, -4], [8, 8, 8], "skin", faces={"north": "skin"}),
            Cube([-5, 27, -5], [10, 6, 10], "robe"),           # капюшон
            Cube([-2.5, 26, -4.2], [5, 1, 1], "eyes"),
        ]),
        Bone("right_arm", [-5.5, 22, 0], "body", [Cube([-8, 11, -2], [3, 11, 4], "robe")]),
        Bone("left_arm", [5.5, 22, 0], "body", [Cube([5, 11, -2], [3, 11, 4], "robe")]),
        Bone("lantern", [7, 11, -1], "left_arm", [
            Cube([5.5, 6, -2.5], [3, 4, 3], "lantern"),
            Cube([6, 10, -2], [2, 1, 2], "leather"),
        ]),
    ]
    anims = {"idle": loop(3.0, {
        "body": {"position": kf([(0, [0, 0, 0]), (1.5, [0, -0.3, 0]), (3.0, [0, 0, 0])])},
        "head": {"rotation": kf([(0, [0, -6, 0]), (1.5, [2, 6, 0]), (3.0, [0, -6, 0])])},
        "lantern": {"rotation": kf([(0, [0, 0, 4]), (1.5, [0, 0, -4]), (3.0, [0, 0, 4])])},
        "left_arm": {"rotation": kf([(0, [-30, 0, 0]), (3.0, [-30, 0, 0])])},
    })}
    return bones, anims


MODELS = {"trial_warden": trial_warden, "power_node": power_node, "trader": trader}


# ---------------------------------------------------------------------------- бестиарий: материалы и шаблоны

MATERIALS.update({
    "bone": ("#E2D6B8", 0.08, False, "cracks"),
    "bone_dark": ("#A89C80", 0.08, False, "cracks"),
    "rust": ("#7A4A2A", 0.14, False, "rivets"),
    "steel": ("#8E98A6", 0.08, False, "rivets"),
    "cloth_black": ("#2A2830", 0.10, False, "plain"),
    "cloth_red": ("#5A1E22", 0.10, False, "plain"),
    "flesh": ("#93A585", 0.10, False, "plain"),
    "flesh_dark": ("#5E6E58", 0.10, False, "plain"),
    "wood": ("#6B4A2E", 0.10, False, "plain"),
    "socket": ("#141218", 0.05, False, "plain"),
    "eye_green": ("#7FFF8A", 0.0, True, "plain"),
    "eye_red": ("#FF4A3A", 0.0, True, "plain"),
    "eye_blue": ("#7FE0FF", 0.0, True, "plain"),
    "gem_green": ("#4AF07A", 0.08, True, "facets"),
})

_orig_paint = paint


def paint(mat, x, y, w, h, rnd):  # noqa: F811 — дополняем узор «трещины»
    c = _orig_paint(mat, x, y, w, h, rnd)
    if MATERIALS[mat][3] == "cracks" and (x * 7 + y * 3) % 11 == 0 and 0 < x < w - 1:
        c = shade(c, 0.72)
    return c


def humanoid(o):
    """Человекоподобный: o — словарь материалов и деталей.
    head, body, arm, leg — материалы; eyes — материал глаз; extras — список (кость-родитель, Cube);
    lean — наклон корпуса вперёд (горбун), arm_len — длина рук."""
    arm_len = o.get("arm_len", 12)
    head = o.get("head", "bone")
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 12, 0], "root", [Cube([-4, 12, -2], [8, 12, 4], o.get("body", "bone_dark"))],
             rotation=[o.get("lean", 0), 0, 0] if o.get("lean") else None),
        Bone("head", [0, 24, 0], "body", [Cube([-4, 24, -4], [8, 8, 8], head, faces={"north": o.get("face", head)}),
                                          Cube([-3, 27, -4.2], [2, 2, 1], o.get("eyes", "socket")),
                                          Cube([1, 27, -4.2], [2, 2, 1], o.get("eyes", "socket"))]),
        Bone("right_arm", [-5, 22, 0], "body", [Cube([-7, 24 - arm_len, -1.5], [3, arm_len, 3], o.get("arm", "bone"))]),
        Bone("left_arm", [5, 22, 0], "body", [Cube([4, 24 - arm_len, -1.5], [3, arm_len, 3], o.get("arm", "bone"))]),
        Bone("right_leg", [-2, 12, 0], "root", [Cube([-3.5, 0, -1.5], [3, 12, 3], o.get("leg", "bone"))]),
        Bone("left_leg", [2, 12, 0], "root", [Cube([0.5, 0, -1.5], [3, 12, 3], o.get("leg", "bone"))]),
    ]
    by = {b.name: b for b in bones}
    for parent, cube in o.get("extras", []):
        by[parent].cubes.append(cube)
    anims = {
        "idle": loop(2.6, {
            "head": {"rotation": kf([(0, [0, 0, 0]), (1.3, [3, 8, 0]), (2.6, [0, 0, 0])])},
            "right_arm": {"rotation": kf([(0, [0, 0, 3]), (1.3, [-5, 0, 5]), (2.6, [0, 0, 3])])},
            "left_arm": {"rotation": kf([(0, [0, 0, -3]), (1.3, [-5, 0, -5]), (2.6, [0, 0, -3])])},
            "body": {"position": kf([(0, [0, 0, 0]), (1.3, [0, -0.3, 0]), (2.6, [0, 0, 0])])},
        }),
        "walk": loop(0.9, {
            "right_leg": {"rotation": kf([(0, [30, 0, 0]), (0.45, [-30, 0, 0]), (0.9, [30, 0, 0])])},
            "left_leg": {"rotation": kf([(0, [-30, 0, 0]), (0.45, [30, 0, 0]), (0.9, [-30, 0, 0])])},
            "right_arm": {"rotation": kf([(0, [-22, 0, 0]), (0.45, [22, 0, 0]), (0.9, [-22, 0, 0])])},
            "left_arm": {"rotation": kf([(0, [22, 0, 0]), (0.45, [-22, 0, 0]), (0.9, [22, 0, 0])])},
        }),
        "attack": once(0.55, {
            "right_arm": {"rotation": kf([(0, [0, 0, 0]), (0.2, [-120, 0, 15]), (0.35, [15, 0, 0]), (0.55, [0, 0, 0])])},
            "body": {"rotation": kf([(0, [0, 0, 0]), (0.2, [0, 12, 0]), (0.35, [0, -10, 0]), (0.55, [0, 0, 0])])},
        }),
        "death": {"animation_length": 1.0, "hold_on_last_frame": True, "bones": {
            "root": {"rotation": kf([(0, [0, 0, 0]), (1.0, [-90, 0, 0])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (1.0, [20, 30, 0])])},
        }},
    }
    return bones, anims


def quadruped(o):
    """Четвероногое (крыса, волк): body, head, leg, tail — материалы; size — множитель."""
    m = o.get("mat", "bone")
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 4, 0], "root", [Cube([-2.5, 3, -4], [5, 4, 9], o.get("body", m))]),
        Bone("head", [0, 6, -4], "body", [Cube([-2, 4, -8], [4, 4, 4], o.get("head", m)),
                                          Cube([-1, 4, -10], [2, 2, 2], o.get("snout", m)),
                                          Cube([-2, 6.5, -8.2], [1, 1, 1], o.get("eyes", "eye_red")),
                                          Cube([1, 6.5, -8.2], [1, 1, 1], o.get("eyes", "eye_red"))]),
        Bone("tail", [0, 6, 5], "body", [Cube([-0.5, 5.5, 5], [1, 1, 7], o.get("tail", m))]),
        Bone("leg_fl", [-1.5, 3, -3], "root", [Cube([-2.5, 0, -3.5], [2, 3, 2], o.get("leg", m))]),
        Bone("leg_fr", [1.5, 3, -3], "root", [Cube([0.5, 0, -3.5], [2, 3, 2], o.get("leg", m))]),
        Bone("leg_bl", [-1.5, 3, 3], "root", [Cube([-2.5, 0, 2.5], [2, 3, 2], o.get("leg", m))]),
        Bone("leg_br", [1.5, 3, 3], "root", [Cube([0.5, 0, 2.5], [2, 3, 2], o.get("leg", m))]),
    ]
    anims = {
        "idle": loop(1.6, {"tail": {"rotation": kf([(0, [0, -15, 0]), (0.8, [0, 15, 0]), (1.6, [0, -15, 0])])},
                           "head": {"rotation": kf([(0, [0, 0, 0]), (0.8, [5, 6, 0]), (1.6, [0, 0, 0])])}}),
        "walk": loop(0.4, {
            "leg_fl": {"rotation": kf([(0, [35, 0, 0]), (0.2, [-35, 0, 0]), (0.4, [35, 0, 0])])},
            "leg_br": {"rotation": kf([(0, [35, 0, 0]), (0.2, [-35, 0, 0]), (0.4, [35, 0, 0])])},
            "leg_fr": {"rotation": kf([(0, [-35, 0, 0]), (0.2, [35, 0, 0]), (0.4, [-35, 0, 0])])},
            "leg_bl": {"rotation": kf([(0, [-35, 0, 0]), (0.2, [35, 0, 0]), (0.4, [-35, 0, 0])])},
            "tail": {"rotation": kf([(0, [0, -25, 0]), (0.2, [0, 25, 0]), (0.4, [0, -25, 0])])},
        }),
        "attack": once(0.4, {"head": {"rotation": kf([(0, [0, 0, 0]), (0.15, [-30, 0, 0]), (0.25, [20, 0, 0]), (0.4, [0, 0, 0])]),
                                      "position": kf([(0, [0, 0, 0]), (0.2, [0, 0, -2]), (0.4, [0, 0, 0])])}}),
        "death": {"animation_length": 0.8, "hold_on_last_frame": True,
                  "bones": {"root": {"rotation": kf([(0, [0, 0, 0]), (0.8, [0, 0, 90])])}}},
    }
    return bones, anims


# ---------------------------------------------------------------------------- Этап 1: Склеп Забытых

def crypt_warrior():
    return humanoid({"head": "bone", "body": "bone_dark", "eyes": "eye_blue", "extras": [
        ("head", Cube([-4.5, 29, -4.5], [9, 4, 9], "rust")),                 # ржавый шлем
        ("body", Cube([-5, 21, -2.5], [10, 3, 5], "rust")),                  # наплечье
        ("right_arm", Cube([-6, 2, -3.5], [1, 11, 1], "steel")),             # клинок
        ("right_arm", Cube([-7, 12, -4], [3, 1, 2], "rust")),                # гарда
    ]})


def crypt_archer():
    return humanoid({"head": "bone", "body": "bone_dark", "eyes": "eye_blue", "extras": [
        ("head", Cube([-4.5, 25, -4.5], [9, 8, 9], "cloth_black")),          # капюшон
        ("body", Cube([2, 14, 2], [3, 10, 3], "wood")),                      # колчан
        ("left_arm", Cube([6, 6, -4], [1, 16, 1], "wood")),                  # лук
        ("left_arm", Cube([6.2, 6, -3], [0.5, 16, 0.5], "bone_dark")),       # тетива
    ]})


def ghoul():
    return humanoid({"head": "flesh", "body": "flesh_dark", "arm": "flesh", "leg": "flesh_dark", "eyes": "eye_green",
                     "lean": 25, "arm_len": 15, "extras": [
        ("body", Cube([-4.5, 12, -2.5], [9, 6, 5], "cloth_red")),            # обрывки ткани
        ("right_arm", Cube([-7, 7, -2.5], [3, 2, 1], "bone")),               # когти
        ("left_arm", Cube([4, 7, -2.5], [3, 2, 1], "bone")),
    ]})


def bone_guard():
    return humanoid({"head": "bone", "body": "bone_dark", "eyes": "eye_blue", "extras": [
        ("head", Cube([-4.5, 28, -4.5], [9, 5, 9], "steel")),
        ("head", Cube([-6, 31, -1], [2, 4, 2], "bone")), ("head", Cube([4, 31, -1], [2, 4, 2], "bone")),  # рога
        ("body", Cube([-5, 14, -3], [10, 10, 6], "steel")),                  # кираса
        ("left_arm", Cube([1, 4, -6], [8, 18, 2], "rust")),                  # башенный щит перед собой
        ("left_arm", Cube([4, 12, -6.5], [2, 2, 1], "steel")),
        ("right_arm", Cube([-7, 2, -2.5], [3, 4, 3], "steel")),              # булава
    ]})


def acolyte():
    return humanoid({"head": "bone", "face": "bone", "body": "cloth_black", "arm": "cloth_black", "leg": "cloth_black",
                     "eyes": "eye_green", "extras": [
        ("head", Cube([-4.5, 25, -4.5], [9, 8, 9], "cloth_black")),          # капюшон
        ("body", Cube([-4.5, 0, -2.5], [9, 12, 5], "cloth_black")),          # полы рясы
        ("right_arm", Cube([-6, -2, -2.5], [1, 20, 1], "wood")),             # посох
        ("right_arm", Cube([-7, 17, -3.5], [3, 3, 3], "gem_green")),         # зелёный камень
    ]})


def bone_rat():
    return quadruped({"mat": "bone", "body": "bone_dark", "eyes": "eye_red"})


MODELS.update({"crypt_warrior": crypt_warrior, "crypt_archer": crypt_archer, "ghoul": ghoul,
               "bone_guard": bone_guard, "acolyte": acolyte, "bone_rat": bone_rat})


# ---------------------------------------------------------------------------- хранители: общий каркас

MATERIALS.update({
    "gold": ("#D4A93A", 0.10, False, "rivets"),
    "gold_glow": ("#FFE27A", 0.05, True, "plain"),
    "ice": ("#9FD8F0", 0.08, True, "facets"),
    "ice_dark": ("#5A9AC0", 0.08, False, "facets"),
    "cloth_purple": ("#4A2A6A", 0.10, False, "plain"),
    "cloth_green": ("#2A4A30", 0.10, False, "plain"),
    "cloth_blue": ("#1E2A4A", 0.10, False, "plain"),
    "eye_purple": ("#C27FFF", 0.0, True, "plain"),
    "soul": ("#7FE8FF", 0.05, True, "plain"),
    "iron_dark": ("#3E434C", 0.08, False, "rivets"),
    "chain": ("#5A5E66", 0.12, False, "rivets"),
})


def boss_humanoid(o):
    """Хранитель-человекоподобный: как humanoid, плюс «каст» (руки вверх) и «удар оземь» (обеими руками)."""
    bones, anims = humanoid(o)
    anims["cast"] = loop(1.2, {
        "right_arm": {"rotation": kf([(0, [-150, 0, 20]), (0.6, [-160, 0, 30]), (1.2, [-150, 0, 20])])},
        "left_arm": {"rotation": kf([(0, [-150, 0, -20]), (0.6, [-160, 0, -30]), (1.2, [-150, 0, -20])])},
        "head": {"rotation": kf([(0, [-15, 0, 0]), (0.6, [-20, 0, 0]), (1.2, [-15, 0, 0])])},
    })
    anims["slam"] = once(0.8, {
        "right_arm": {"rotation": kf([(0, [0, 0, 0]), (0.35, [-170, 0, 10]), (0.55, [-20, 0, 0]), (0.8, [0, 0, 0])])},
        "left_arm": {"rotation": kf([(0, [0, 0, 0]), (0.35, [-170, 0, -10]), (0.55, [-20, 0, 0]), (0.8, [0, 0, 0])])},
        "body": {"rotation": kf([(0, [0, 0, 0]), (0.35, [-12, 0, 0]), (0.55, [25, 0, 0]), (0.8, [0, 0, 0])])},
    })
    return bones, anims


# ---------------------------------------------------------------------------- хранители этапа 1

def gatekeeper():
    """Привратник Склепа: огромный скелет в железном ошейнике, рога, цепь с гирей в правой руке."""
    return boss_humanoid({"head": "bone", "body": "bone_dark", "arm": "bone", "leg": "bone", "eyes": "eye_blue",
                          "lean": 8, "arm_len": 13, "extras": [
        ("head", Cube([-5, 23, -5], [10, 2, 10], "iron_dark")),              # ошейник
        ("head", Cube([-7, 30, -1], [3, 2, 2], "bone_dark")), ("head", Cube([-8, 32, -1], [2, 4, 2], "bone")),
        ("head", Cube([4, 30, -1], [3, 2, 2], "bone_dark")), ("head", Cube([6, 32, -1], [2, 4, 2], "bone")),
        ("body", Cube([-4.5, 14, -2.5], [9, 1, 5], "bone")), ("body", Cube([-4.5, 17, -2.5], [9, 1, 5], "bone")),
        ("body", Cube([-4.5, 20, -2.5], [9, 1, 5], "bone")),                 # рёбра
        ("body", Cube([-6, 20, -3], [12, 4, 6], "rust")),                    # наплечник
        ("right_arm", Cube([-6.5, 0, -1], [2, 13, 2], "chain")),             # цепь
        ("right_arm", Cube([-8, -4, -2.5], [5, 5, 5], "iron_dark")),         # гиря
        ("left_arm", Cube([3.5, 10, -2], [4, 3, 4], "chain")),               # обрывок кандалов
    ]})


def morra():
    """Морра: некромантка в чёрно-красном, маска-череп, коса."""
    return boss_humanoid({"head": "bone", "face": "bone", "body": "cloth_black", "arm": "cloth_black", "leg": "cloth_black",
                          "eyes": "eye_red", "extras": [
        ("head", Cube([-4.5, 25, -4.5], [9, 9, 9], "cloth_red")),            # капюшон
        ("body", Cube([-5, 0, -3], [10, 13, 6], "cloth_black")),             # полы платья
        ("body", Cube([-4.5, 18, -2.6], [9, 2, 1], "gold")),                 # пояс-цепочка
        ("right_arm", Cube([-6, -6, -1.5], [1, 30, 1], "wood")),             # древко косы
        ("right_arm", Cube([-14, 21, -2], [9, 2, 1], "steel")),              # лезвие
        ("right_arm", Cube([-14, 19, -2], [2, 2, 1], "steel")),
    ]})


def vessa():
    """Весса: колдунья в фиолетово-зелёном, книга на поясе, посох с зелёным огнём."""
    return boss_humanoid({"head": "skin", "face": "bone", "body": "cloth_purple", "arm": "cloth_purple", "leg": "cloth_purple",
                          "eyes": "eye_green", "extras": [
        ("head", Cube([-4.5, 25, -4.5], [9, 9, 9], "cloth_green")),
        ("head", Cube([-3, 33, -3], [6, 3, 6], "cloth_green")),              # острый капюшон
        ("body", Cube([-5, 0, -3], [10, 13, 6], "cloth_purple")),
        ("body", Cube([3, 12, -3.5], [3, 4, 2], "leather")),                 # книга
        ("left_arm", Cube([5, -4, -1.5], [1, 26, 1], "wood")),               # посох
        ("left_arm", Cube([4, 21, -2.5], [3, 3, 3], "gem_green")),
    ]})


def lich():
    """Король-лич Карн: высокий скелет в королевской мантии, золотая корона, посох с ледяным кристаллом."""
    return boss_humanoid({"head": "bone", "face": "bone", "body": "cloth_blue", "arm": "bone", "leg": "cloth_blue",
                          "eyes": "soul", "arm_len": 13, "extras": [
        ("head", Cube([-4.5, 31, -4.5], [9, 2, 9], "gold")),                 # корона
        ("head", Cube([-4.5, 33, -4.5], [2, 3, 2], "gold")), ("head", Cube([2.5, 33, -4.5], [2, 3, 2], "gold")),
        ("head", Cube([-1, 33, -4.5], [2, 4, 2], "gold_glow")),
        ("body", Cube([-6, 0, -3.5], [12, 14, 7], "cloth_blue")),            # мантия
        ("body", Cube([-6.5, 20, -3.5], [13, 4, 7], "ice_dark")),            # ледяной воротник
        ("body", Cube([-6, 6, 3], [12, 18, 1], "cloth_purple")),             # плащ
        ("right_arm", Cube([-6, -8, -1.5], [1, 32, 1], "iron_dark")),        # посох
        ("right_arm", Cube([-7.5, 22, -3], [4, 6, 4], "ice")),               # кристалл
    ]})


def _node(mat_main, mat_core, name):
    bones = [Bone("root", [0, 0, 0]),
             Bone("core", [0, 10, 0], "root", [Cube([-5, 2, -5], [10, 18, 10], mat_main),
                                               Cube([-3, 20, -3], [6, 6, 6], mat_main),
                                               Cube([-2, 8, -6], [4, 6, 1], mat_core)])]
    anims = {"idle": loop(3, {"core": {"rotation": kf([(0, [0, 0, 0]), (1.5, [0, 180, 0]), (3, [0, 360, 0])]),
                                       "position": kf([(0, [0, 0, 0]), (1.5, [0, 1, 0]), (3, [0, 0, 0])])}}),
             "walk": loop(3, {}), "death": {"animation_length": 0.5, "hold_on_last_frame": True,
                                            "bones": {"core": {"scale": kf([(0, [1, 1, 1]), (0.5, [0.1, 0.1, 0.1])])}}}}
    return bones, anims


def ice_tomb():
    return _node("ice", "ice_dark", "ice_tomb")


def phylactery():
    bones = [Bone("root", [0, 0, 0]),
             Bone("core", [0, 12, 0], "root", [Cube([-3, 8, -3], [6, 8, 6], "soul"),
                                               Cube([-4, 6, -4], [8, 1, 8], "gold"), Cube([-4, 17, -4], [8, 1, 8], "gold"),
                                               Cube([-4, 7, -4], [1, 10, 1], "gold"), Cube([3, 7, -4], [1, 10, 1], "gold"),
                                               Cube([-4, 7, 3], [1, 10, 1], "gold"), Cube([3, 7, 3], [1, 10, 1], "gold")])]
    anims = {"idle": loop(2, {"core": {"rotation": kf([(0, [0, 0, 0]), (2, [0, 360, 0])]),
                                       "position": kf([(0, [0, 0, 0]), (1, [0, 1.5, 0]), (2, [0, 0, 0])])}}),
             "walk": loop(2, {}), "death": {"animation_length": 0.5, "hold_on_last_frame": True,
                                            "bones": {"core": {"scale": kf([(0, [1, 1, 1]), (0.5, [0.1, 0.1, 0.1])])}}}}
    return bones, anims


MODELS.update({"gatekeeper": gatekeeper, "morra": morra, "vessa": vessa, "lich": lich, "ice_tomb": ice_tomb,
               "phylactery": phylactery})


# ---------------------------------------------------------------------------- этап 2: пауки, оса, ведьма, древень

MATERIALS.update({
    "chitin": ("#2E2A26", 0.10, False, "plain"),
    "chitin_light": ("#4A4238", 0.10, False, "plain"),
    "chitin_green": ("#3E5A2A", 0.10, False, "plain"),
    "chitin_white": ("#C8C8D0", 0.08, False, "plain"),
    "acid": ("#9CE04A", 0.05, True, "plain"),
    "wasp_yellow": ("#E8B828", 0.08, False, "plain"),
    "wasp_black": ("#1E1A16", 0.05, False, "plain"),
    "wing": ("#D8E8F0", 0.05, False, "plain"),
    "bark": ("#5A4228", 0.12, False, "cracks"),
    "bark_dark": ("#3A2A18", 0.10, False, "cracks"),
    "leaves": ("#4A7A2A", 0.14, False, "plain"),
    "moss": ("#5E8A3A", 0.12, False, "plain"),
    "rot": ("#6A3A7A", 0.10, False, "plain"),
    "mushroom_red": ("#B8302A", 0.08, False, "plain"),
    "skin_green": ("#8AA878", 0.06, False, "plain"),
    "eye_yellow": ("#FFE24A", 0.0, True, "plain"),
    "web": ("#E8E8F0", 0.04, False, "plain"),
})


def spider(o):
    """Паук: головогрудь, брюшко, 8 ног, клыки, глаза. o: body, abdomen, leg, eyes, marks (узор на брюшке)."""
    body, abdomen, leg = o.get("body", "chitin"), o.get("abdomen", "chitin_light"), o.get("leg", "chitin")
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 5, 0], "root", [Cube([-3, 3, -4], [6, 4, 6], body)]),
        Bone("head", [0, 5, -4], "body", [Cube([-2.5, 3, -8], [5, 4, 4], body),
                                          Cube([-2, 5.5, -8.2], [1, 1, 1], o.get("eyes", "eye_red")),
                                          Cube([1, 5.5, -8.2], [1, 1, 1], o.get("eyes", "eye_red")),
                                          Cube([-1, 6.2, -8.2], [2, 1, 1], o.get("eyes", "eye_red")),
                                          Cube([-2, 2, -9], [1, 2, 1], "bone"), Cube([1, 2, -9], [1, 2, 1], "bone")]),
        Bone("abdomen", [0, 6, 2], "body", [Cube([-4, 3, 2], [8, 7, 9], abdomen),
                                            Cube([-2, 9.5, 4], [4, 1, 5], o.get("marks", abdomen))]),
    ]
    for i, z in enumerate((-3, -1, 1, 3)):
        for side, sx in (("r", -1), ("l", 1)):
            name = f"leg_{side}{i}"
            x0 = -11 if sx < 0 else 3
            bones.append(Bone(name, [3 * sx, 5, z], "body", [Cube([x0, 4.5, z - 0.5], [8, 1, 1], leg)],
                              rotation=[0, (i - 1.5) * 18 * sx, 25 * sx]))
    walk = {}
    for i in range(4):
        for side in ("r", "l"):
            ph = (i % 2) ^ (side == "l")
            a, b = ([0, 18, 0], [0, -18, 0]) if ph else ([0, -18, 0], [0, 18, 0])
            walk[f"leg_{side}{i}"] = {"rotation": kf([(0, a), (0.2, b), (0.4, a)])}
    anims = {
        "idle": loop(2, {"abdomen": {"rotation": kf([(0, [0, 0, 0]), (1, [4, 0, 0]), (2, [0, 0, 0])])}}),
        "walk": loop(0.4, walk),
        "attack": once(0.4, {"head": {"rotation": kf([(0, [0, 0, 0]), (0.15, [-25, 0, 0]), (0.3, [15, 0, 0]), (0.4, [0, 0, 0])])},
                             "body": {"position": kf([(0, [0, 0, 0]), (0.15, [0, 1, -1]), (0.4, [0, 0, 0])])}}),
        "death": {"animation_length": 0.8, "hold_on_last_frame": True,
                  "bones": {"root": {"rotation": kf([(0, [0, 0, 0]), (0.8, [0, 0, 180])]),
                                     "position": kf([(0, [0, 0, 0]), (0.8, [0, 8, 0])])}}},
    }
    for i, extra in enumerate(o.get("extras", [])):
        parent, cube = extra
        next(b for b in bones if b.name == parent).cubes.append(cube)
    return bones, anims


def weaver():
    return spider({"body": "chitin_white", "abdomen": "web", "leg": "chitin_white", "eyes": "eye_purple", "marks": "chitin"})


def jumper():
    return spider({"body": "chitin_green", "abdomen": "chitin_green", "leg": "chitin", "eyes": "eye_yellow", "marks": "acid"})


def spitter():
    return spider({"body": "chitin", "abdomen": "acid", "leg": "chitin_light", "eyes": "eye_green", "marks": "chitin_green"})


def wasp():
    bones = [
        Bone("root", [0, 0, 0]),
        Bone("body", [0, 6, 0], "root", [Cube([-1.5, 5, -3], [3, 3, 4], "wasp_black"),
                                         Cube([-1.5, 5.5, -5], [3, 3, 2], "wasp_yellow"),
                                         Cube([-1, 6.5, -5.2], [1, 1, 1], "eye_red"), Cube([0, 6.5, -5.2], [1, 1, 1], "eye_red")]),
        Bone("tail", [0, 6.5, 1], "body", [Cube([-2, 4.5, 1], [4, 4, 2], "wasp_yellow"), Cube([-2, 4.5, 3], [4, 4, 2], "wasp_black"),
                                           Cube([-1.5, 5, 5], [3, 3, 2], "wasp_yellow"), Cube([-0.5, 6, 7], [1, 1, 2], "bone")]),
        Bone("wing_r", [-1, 8, -1], "body", [Cube([-7, 8, -2], [6, 0.5, 3], "wing")]),
        Bone("wing_l", [1, 8, -1], "body", [Cube([1, 8, -2], [6, 0.5, 3], "wing")]),
        Bone("legs", [0, 5, -1], "body", [Cube([-1.5, 2, -2], [3, 3, 1], "wasp_black")]),
    ]
    flap = {"wing_r": {"rotation": kf([(0, [0, 0, 30]), (0.05, [0, 0, -30]), (0.1, [0, 0, 30])])},
            "wing_l": {"rotation": kf([(0, [0, 0, -30]), (0.05, [0, 0, 30]), (0.1, [0, 0, -30])])}}
    hover = dict(flap)
    hover["body"] = {"position": kf([(0, [0, 2, 0]), (0.5, [0, 3, 0]), (1, [0, 2, 0])])}
    anims = {"idle": loop(1, hover), "walk": loop(1, hover),
             "attack": once(0.3, {"tail": {"rotation": kf([(0, [0, 0, 0]), (0.15, [-50, 0, 0]), (0.3, [0, 0, 0])])}}),
             "death": {"animation_length": 0.6, "hold_on_last_frame": True,
                       "bones": {"root": {"rotation": kf([(0, [0, 0, 0]), (0.6, [0, 0, 180])])}}}}
    return bones, anims


def forest_witch():
    return humanoid({"head": "skin_green", "face": "skin_green", "body": "moss", "arm": "moss", "leg": "bark_dark",
                     "eyes": "eye_yellow", "lean": 10, "extras": [
        ("head", Cube([-5, 31, -5], [10, 1, 10], "bark_dark")),              # поля шляпы
        ("head", Cube([-3, 32, -3], [6, 3, 6], "bark_dark")),
        ("head", Cube([-2, 35, -2], [4, 3, 4], "bark_dark")),
        ("head", Cube([-1, 38, -1], [2, 2, 2], "leaves")),
        ("head", Cube([-1, 25, -5], [2, 3, 1], "skin_green")),              # нос
        ("body", Cube([-4.5, 0, -2.5], [9, 12, 5], "moss")),
        ("right_arm", Cube([-6, -4, -1.5], [1, 24, 1], "bark")),            # посох-ветка
        ("right_arm", Cube([-7.5, 19, -3], [4, 3, 4], "mushroom_red")),
    ]})


def treant_guard():
    return humanoid({"head": "bark", "face": "bark_dark", "body": "bark", "arm": "bark", "leg": "bark_dark",
                     "eyes": "eye_green", "arm_len": 15, "extras": [
        ("head", Cube([-5, 30, -5], [10, 4, 10], "leaves")),
        ("head", Cube([-3, 34, -3], [6, 2, 6], "leaves")),
        ("body", Cube([-6, 20, -3.5], [12, 4, 7], "moss")),
        ("right_arm", Cube([-8, 7, -2.5], [5, 4, 5], "bark_dark")),
        ("left_arm", Cube([3, 7, -2.5], [5, 4, 5], "bark_dark")),
    ]})


# ---------------------------------------------------------------------------- хранители этапа 2

def arachna():
    """Аракна: огромная паучиха с короной из шипов и узором-черепом на брюшке."""
    return spider({"body": "chitin", "abdomen": "cloth_black", "leg": "chitin_light", "eyes": "eye_red", "marks": "bone",
                   "extras": [("head", Cube([-2.5, 7, -7], [1, 3, 1], "bone")), ("head", Cube([1.5, 7, -7], [1, 3, 1], "bone")),
                              ("head", Cube([-0.5, 7, -6], [1, 4, 1], "gold_glow")),
                              ("abdomen", Cube([-2, 10, 6], [4, 1, 3], "eye_red"))]})


def rot_ent():
    """Гнилой Древень: исполинское дерево, мох, гнилые грибы на плечах, светящаяся сердцевина."""
    bones, anims = boss_humanoid({"head": "bark", "face": "bark_dark", "body": "bark_dark", "arm": "bark", "leg": "bark_dark",
                                  "eyes": "eye_purple", "arm_len": 16, "lean": 6, "extras": [
        ("head", Cube([-6, 30, -6], [12, 5, 12], "leaves")),
        ("head", Cube([-4, 35, -4], [8, 3, 8], "rot")),
        ("head", Cube([-7, 28, 0], [3, 6, 2], "bark")), ("head", Cube([4, 28, 0], [3, 6, 2], "bark")),   # сучья
        ("body", Cube([-2, 15, -2.6], [4, 5, 1], "gem_green")),            # сердцевина
        ("body", Cube([-6.5, 20, -3.5], [13, 4, 7], "moss")),
        ("body", Cube([-6, 22, 1], [3, 3, 3], "mushroom_red")), ("body", Cube([3, 23, 1], [3, 2, 3], "rot")),
        ("right_arm", Cube([-9, 6, -3], [6, 5, 6], "bark_dark")), ("left_arm", Cube([3, 6, -3], [6, 5, 6], "bark_dark")),
    ]})
    return bones, anims


def morena():
    """Ведьма Изнанки Морена: высокая, в лиловом, рога-ветви, белая маска, посох с глазом."""
    return boss_humanoid({"head": "skin_green", "face": "bone", "body": "cloth_purple", "arm": "cloth_purple",
                          "leg": "cloth_black", "eyes": "eye_purple", "extras": [
        ("head", Cube([-6, 30, -1], [2, 6, 2], "bark_dark")), ("head", Cube([-8, 34, -1], [2, 3, 2], "bark_dark")),
        ("head", Cube([4, 30, -1], [2, 6, 2], "bark_dark")), ("head", Cube([6, 34, -1], [2, 3, 2], "bark_dark")),
        ("head", Cube([-4.5, 24, -4.5], [9, 9, 9], "cloth_black")),
        ("body", Cube([-5.5, 0, -3.5], [11, 14, 7], "cloth_purple")),
        ("body", Cube([-5, 14, 3], [10, 10, 1], "cloth_black")),
        ("left_arm", Cube([5, -6, -1.5], [1, 30, 1], "bark_dark")),
        ("left_arm", Cube([4, 23, -2.5], [3, 3, 3], "eye_purple")),
    ]})


def cocoon():
    bones = [Bone("root", [0, 0, 0]),
             Bone("core", [0, 10, 0], "root", [Cube([-5, 0, -5], [10, 22, 10], "web"), Cube([-4, 22, -4], [8, 4, 8], "web"),
                                               Cube([-2, 12, -5.2], [4, 4, 1], "chitin_white")])]
    anims = {"idle": loop(2, {"core": {"rotation": kf([(0, [0, 0, -3]), (1, [0, 0, 3]), (2, [0, 0, -3])])}}),
             "walk": loop(2, {}), "death": {"animation_length": 0.5, "hold_on_last_frame": True,
                                            "bones": {"core": {"scale": kf([(0, [1, 1, 1]), (0.5, [0.1, 0.1, 0.1])])}}}}
    return bones, anims


def egg():
    bones = [Bone("root", [0, 0, 0]),
             Bone("core", [0, 6, 0], "root", [Cube([-4, 0, -4], [8, 9, 8], "web"), Cube([-3, 9, -3], [6, 2, 6], "acid")])]
    anims = {"idle": loop(1.2, {"core": {"scale": kf([(0, [1, 1, 1]), (0.6, [1.08, 0.95, 1.08]), (1.2, [1, 1, 1])])}}),
             "walk": loop(2, {}), "death": {"animation_length": 0.4, "hold_on_last_frame": True,
                                            "bones": {"core": {"scale": kf([(0, [1, 1, 1]), (0.4, [0.1, 0.1, 0.1])])}}}}
    return bones, anims


MODELS.update({"weaver": weaver, "jumper": jumper, "spitter": spitter, "wasp": wasp, "forest_witch": forest_witch,
               "treant_guard": treant_guard, "arachna": arachna, "rot_ent": rot_ent, "morena": morena,
               "cocoon": cocoon, "egg": egg})


def main():
    only = sys.argv[sys.argv.index("--only") + 1] if "--only" in sys.argv else None
    preview = Path(sys.argv[sys.argv.index("--preview") + 1]) if "--preview" in sys.argv else None
    for name, fn in MODELS.items():
        if only and name != only:
            continue
        bones, anims = fn()
        geo, anim, tex, glow = build(name, bones, anims, tex_w=128, tex_h=64 if name == "power_node" else 128)
        (ASSETS / "geo/entity").mkdir(parents=True, exist_ok=True)
        (ASSETS / "animations/entity").mkdir(parents=True, exist_ok=True)
        (ASSETS / f"geo/entity/{name}.geo.json").write_text(json.dumps(geo, indent=1), encoding="utf-8", newline="\n")
        (ASSETS / f"animations/entity/{name}.animation.json").write_text(json.dumps(anim, indent=1), encoding="utf-8", newline="\n")
        tex.save(ASSETS / f"textures/entity/{name}.png")
        glow.save(ASSETS / f"textures/entity/{name}_glowmask.png")
        if preview:
            tex.scaled(4).save(preview / f"{name}_tex.png")
        print("written", name)


if __name__ == "__main__":
    main()
