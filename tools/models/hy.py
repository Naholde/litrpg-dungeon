"""Каркас моделей «как Hytale»: крупные выразительные формы, текстура вдвое плотнее ванильной, мягкое затенение.

Модель — кости и кубы, как в Blockbench, но описанные кодом:

    m = Model("gatekeeper")
    body = m.bone("body", [0, 14, 0], "root")
    m.cube(body, [-5, 14, -3], [10, 11, 6], "bone", decal={"north": ribs})
    m.anim("idle", loop(2.4, {...}))
    m.save()

Каждая грань куба получает свой участок атласа (UV по граням) с плотностью D текселей на единицу и красится
материалом: базовый цвет и узор (ткань, металл, кость, чешуя…), затем свет — верх светлее, низ темнее,
кромки подсвечены, внизу модели — мягкая тень. Светящиеся материалы (и светящиеся части узоров) попадают
в маску свечения. Поверх — декали: глаза, рты, руны, пряжки, швы, нарисованные кодом на нужной грани.
"""
import json
import math
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from png import Canvas  # noqa: E402

ASSETS = Path(__file__).resolve().parent.parent.parent / "src/main/resources/assets/litrpg"
D = 2  # текселей на единицу модели (ваниль — 1)


# ============================================================================ цвет и шум

def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, k):
    k = max(0.0, min(1.0, k))
    return tuple(a[i] + (b[i] - a[i]) * k for i in range(3))


def mul(c, k):
    return tuple(max(0.0, min(255.0, v * k)) for v in c)


def h32(*v):
    """Детерминированный хеш → [0, 1)."""
    x = 2166136261
    for n in v:
        x = ((x ^ (int(n) & 0xFFFFFFFF)) * 16777619) & 0xFFFFFFFF
    x ^= x >> 13
    x = (x * 1274126177) & 0xFFFFFFFF
    x ^= x >> 16
    return x / 4294967296.0


def vnoise(x, y, s, seed=0):
    """Гладкий шум значения (масштаб s текселей) → [0, 1)."""
    gx, gy = x / s, y / s
    x0, y0 = math.floor(gx), math.floor(gy)
    fx, fy = gx - x0, gy - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a, b = h32(x0, y0, seed), h32(x0 + 1, y0, seed)
    c, d = h32(x0, y0 + 1, seed), h32(x0 + 1, y0 + 1, seed)
    return (a + (b - a) * fx) * (1 - fy) + (c + (d - c) * fx) * fy


def fbm(x, y, s, seed=0, oct_=3):
    t, amp, tot = 0.0, 1.0, 0.0
    for o in range(oct_):
        t += vnoise(x, y, s / (2 ** o), seed + o * 31) * amp
        tot += amp
        amp *= 0.5
    return t / tot


# ============================================================================ материалы

class Mat:
    """Материал: основной цвет, тень, свет, узор, свечение (True — весь, «pattern» — только где узор светится)."""

    def __init__(self, base, pattern="plain", dark=None, light=None, glow=False, accent=None, scale=1.0, gloss=0.0):
        self.base = rgb(base)
        self.dark = rgb(dark) if dark else mul(self.base, 0.62)
        self.light = rgb(light) if light else mix(self.base, (255, 255, 255), 0.22)
        self.accent = rgb(accent) if accent else self.light
        self.pattern, self.glow, self.scale, self.gloss = pattern, glow, scale, gloss


MATS = {}


def mat(name, *a, **k):
    MATS[name] = Mat(*a, **k)
    return name


# Общая палитра — модели дописывают свои материалы через mat(...).
mat("bone", "#E4D8BA", "bone", dark="#9C8E70", light="#FFF6DE")
mat("bone_old", "#C9B994", "bone", dark="#7E6E52", light="#EDE0BE")
mat("iron", "#6E7682", "metal", dark="#3A3F48", light="#B8C2CE")
mat("iron_dark", "#43484F", "metal", dark="#24272C", light="#7C848F")
mat("rust", "#8A5232", "rust", dark="#4E2C1A", light="#C07A4A")
mat("steel", "#9AA6B4", "metal", dark="#55606C", light="#E6EEF6", gloss=0.6)
mat("gold", "#D8A838", "metal", dark="#8A6418", light="#FFE68A", gloss=0.7)
mat("bronze", "#A8703A", "metal", dark="#62401E", light="#E0A868", gloss=0.4)
mat("chain", "#5E636B", "chain", dark="#2E3136", light="#9AA2AC")
mat("leather", "#7A5234", "leather", dark="#45291A", light="#A8784E")
mat("leather_dark", "#4E3424", "leather", dark="#281A10", light="#74503A")
mat("wood", "#7A5532", "wood", dark="#45301A", light="#A87A4C")
mat("wood_dark", "#4A3420", "wood", dark="#261A0E", light="#6E5030")
mat("stone", "#6E6E72", "stone", dark="#3E3E44", light="#9A9AA0")
mat("cloth_black", "#2C2A34", "cloth", dark="#16151B", light="#4A4656")
mat("cloth_red", "#7A2228", "cloth", dark="#3E1014", light="#A8424A")
mat("cloth_purple", "#4E2C72", "cloth", dark="#26143A", light="#7A50A4")
mat("cloth_blue", "#22325E", "cloth", dark="#101830", light="#3C5490")
mat("cloth_green", "#2C4E32", "cloth", dark="#142818", light="#4A7A50")
mat("cloth_white", "#D8D4CA", "cloth", dark="#9A968C", light="#F6F2E8")
mat("socket", "#121016", "plain", dark="#08070A", light="#1E1A24")
mat("eye_blue", "#8AF0FF", "glow", glow=True, light="#FFFFFF", dark="#3AB8E0")
mat("eye_red", "#FF5A40", "glow", glow=True, light="#FFE0C0", dark="#C02A1A")
mat("eye_green", "#8AFF96", "glow", glow=True, light="#F0FFF0", dark="#3AC050")
mat("eye_gold", "#FFD860", "glow", glow=True, light="#FFFFFF", dark="#D89A20")
mat("eye_purple", "#D08AFF", "glow", glow=True, light="#FFF0FF", dark="#8A40D0")
mat("soul", "#7FE8FF", "glow", glow=True, light="#FFFFFF", dark="#2AA0D0")
mat("ice", "#A8E0F4", "crystal", glow="pattern", dark="#5A9AC0", light="#F0FCFF")
mat("flesh", "#93A585", "skin", dark="#5E6E58", light="#B8C8A8")
mat("skin", "#D8A884", "skin", dark="#9A6E52", light="#F4CCA8")


# ============================================================================ узоры

def pattern_color(m, x, y, W, H, face, seed, info):
    """Цвет тексела (x, y) грани W×H материала m до освещения. → (rgb, светится)."""
    p, s = m.pattern, m.scale
    base, dark, light = m.base, m.dark, m.light
    n = fbm(x, y, 6 * s, seed)
    glow = m.glow is True
    if p == "plain":
        return mix(base, light if n > 0.5 else dark, abs(n - 0.5) * 0.35), glow
    if p == "glow":
        # ядро ярче, к краю — насыщенный цвет
        cx, cy = (x + 0.5) / W - 0.5, (y + 0.5) / H - 0.5
        r = min(1.0, math.hypot(cx, cy) * 2)
        return mix(light, base, r * 0.9), True
    if p == "cloth":
        weave = 0.04 if (x + y) % 2 == 0 else -0.04
        fold = math.sin((x + h32(seed, 7) * 40) / (3.2 * s)) * 0.5 + 0.5
        fold = fold ** 2.2
        c = mix(base, dark, fold * 0.45)
        c = mix(c, light, max(0.0, n - 0.62) * 0.8)
        return mul(c, 1 + weave), glow
    if p == "leather":
        c = mix(dark, base, 0.55 + n * 0.45)
        if fbm(x, y, 2.2, seed + 9) > 0.72:
            c = mix(c, light, 0.35)
        return c, glow
    if p in ("metal", "chain"):
        streak = vnoise(x * 0.25, y * 3, 2, seed) * 0.12 - 0.06
        band = math.exp(-((y / max(1, H) - 0.3) ** 2) / 0.02) * (0.25 + m.gloss * 0.35)
        c = mix(base, light, band)
        c = mul(c, 1 + streak)
        if p == "chain":
            # звенья: овалы по 4×3
            lx, ly = x % 4, y % 6
            ring = (ly in (0, 5) and lx in (1, 2)) or (lx in (0, 3) and 1 <= ly <= 4)
            c = mix(dark, light, 0.55) if ring else mul(dark, 0.7)
        # царапины
        if h32(x // 3, y, seed + 3) > 0.985:
            c = mix(c, light, 0.6)
        return c, glow
    if p == "rust":
        c = mix(base, dark, n * 0.6)
        if fbm(x, y, 3, seed + 5) > 0.64:
            c = mix(c, rgb("#C8823E"), 0.5)
        if fbm(x, y, 4, seed + 11) < 0.3:
            c = mix(c, rgb("#5C636E"), 0.45)   # проступает металл
        return c, glow
    if p == "bone":
        big = fbm(x, y, 14 * s, seed + 4, 2)
        c = mix(base, dark, max(0.0, 0.5 - big) * 0.7)
        c = mix(c, light, max(0.0, big - 0.58) * 0.9)
        c = mul(c, 0.97 + n * 0.06)
        if crack(x, y, seed, 0.07 / s):
            c = mix(c, dark, 0.55)
        if h32(x, y, seed + 2) > 0.988:
            c = mul(c, 0.86)   # поры
        return c, glow
    if p == "stone":
        cell = voronoi(x, y, 5 * s, seed)
        c = mix(dark, base, 0.55 + cell[1] * 0.45)
        if cell[0] < 0.7:
            c = mul(dark, 0.8)
        c = mix(c, light, max(0.0, n - 0.65) * 0.8)
        return c, glow
    if p == "scales":
        row = int(y // 3)
        off = 2 if row % 2 else 0
        cx = (x + off) % 4
        cy = y % 3
        k = cy / 2.0
        c = mix(light, dark, k * 0.8)
        if cx == 0:
            c = mul(dark, 0.9)
        return mix(c, base, 0.35), glow
    if p == "chitin":
        band = (math.sin(y / (2.2 * s)) * 0.5 + 0.5)
        c = mix(dark, base, 0.4 + band * 0.6)
        if (y % max(3, int(6 * s))) == 0:
            c = mul(dark, 0.8)
        hl = math.exp(-((x / max(1, W) - 0.35) ** 2) / 0.01) * 0.35 * (1 + m.gloss)
        return mix(c, light, hl), glow
    if p == "fur":
        strand = vnoise(x * 1.6, y * 0.35, 1.3, seed)
        c = mix(dark, light, strand * 0.85)
        return mix(c, base, 0.45), glow
    if p == "bark":
        groove = vnoise(x * 1.8, y * 0.22, 1.5, seed)
        c = mix(dark, base, groove)
        if groove < 0.28:
            c = mul(dark, 0.75)
        return mix(c, light, max(0.0, n - 0.7)), glow
    if p == "wood":
        grain = math.sin((x + vnoise(x, y * 0.3, 3, seed) * 6) / 1.4) * 0.5 + 0.5
        return mix(dark, base, 0.45 + grain * 0.55), glow
    if p == "skin":
        return mix(base, light if n > 0.5 else dark, abs(n - 0.5) * 0.4), glow
    if p == "hair":
        strand = vnoise(x * 2.2, y * 0.25, 1.1, seed)
        return mix(dark, light, strand), glow
    if p == "crystal":
        f = (int((x + y) / (3 * s)) + int((x - y) / (3 * s))) % 3
        c = [light, base, dark][f]
        g = m.glow is True or (m.glow == "pattern" and f == 0)
        return c, g
    if p == "moss":
        c = mix(dark, light, fbm(x, y, 2.5, seed))
        return c, glow
    if p == "lava":
        v = fbm(x, y, 4 * s, seed)
        if v > 0.55:
            return mix(rgb("#FF7A1E"), rgb("#FFE27A"), (v - 0.55) * 2.5), True
        return mix(dark, base, v), False
    if p == "runes":
        c = mix(dark, base, 0.5 + n * 0.5)
        if rune_px(x, y, seed):
            return m.accent, m.glow == "pattern" or glow
        return c, glow
    return base, glow


def crack(x, y, seed, density):
    """Тонкие трещины: линия, где шум пересекает середину."""
    v = vnoise(x, y, 5, seed + 17)
    return abs(v - 0.5) < density * 0.25 and h32(x // 4, y // 4, seed) < 0.55


def voronoi(x, y, s, seed):
    """(расстояние до края ячейки, значение ячейки)."""
    gx, gy = x / s, y / s
    ix, iy = math.floor(gx), math.floor(gy)
    best, second, val = 9.0, 9.0, 0.0
    for oy in (-1, 0, 1):
        for ox in (-1, 0, 1):
            cx, cy = ix + ox, iy + oy
            px, py = cx + h32(cx, cy, seed), cy + h32(cy, cx, seed + 1)
            d = math.hypot(gx - px, gy - py)
            if d < best:
                best, second, val = d, best, h32(cx, cy, seed + 2)
            elif d < second:
                second = d
    return (second - best) * s, val


def rune_px(x, y, seed):
    """Руны: решётка 6×6, в каждой клетке — случайный знак из штрихов."""
    cx, cy = x // 6, y // 6
    lx, ly = x % 6, y % 6
    if lx in (0, 5) or ly in (0, 5):
        return False
    k = int(h32(cx, cy, seed) * 8)
    return {0: lx == 2, 1: ly == 2, 2: lx == ly, 3: lx + ly == 5, 4: lx == 3 or ly == 1,
            5: (lx == 1 or lx == 4) and ly < 4, 6: ly == 3 and lx > 1, 7: False}[k]


# ============================================================================ модель

class Cube:
    def __init__(self, origin, size, mat, faces=None, inflate=0.0, rot=None, pivot=None, decal=None, light=1.0, skip=()):
        self.origin, self.size, self.mat, self.inflate = list(origin), list(size), mat, inflate
        self.faces = faces or {}    # {"north": "other_mat"}
        self.rot, self.pivot = rot, pivot
        self.decal = decal or {}    # {"north": fn(face)}
        self.light = light          # множитель яркости (тени во впадинах, яркие кромки)
        self.skip = tuple(skip)     # грани, которых нет (открытый капюшон, полотно без торцов)
        self.uv = {}


class Bone:
    def __init__(self, name, pivot, parent=None, rot=None):
        self.name, self.pivot, self.parent, self.rot = name, list(pivot), parent, rot
        self.cubes = []


FACE_ID = {"north": 0, "south": 1, "east": 2, "west": 3, "up": 4, "down": 5}


class Face:
    """Холст одной грани для декалей: координаты в текселях от левого-верхнего угла грани."""

    def __init__(self, w, h, px, glow):
        self.w, self.h, self.px, self.gl = w, h, px, glow

    def set(self, x, y, color, glow=False):
        x, y = int(x), int(y)
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = rgb(color) if isinstance(color, str) else tuple(color)
            if glow:
                self.gl[y][x] = True

    def get(self, x, y):
        return self.px[int(y)][int(x)]

    def cut(self, x, y):
        """Прозрачный тексел (рваный край ткани, дырка)."""
        x, y = int(x), int(y)
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = None
            self.gl[y][x] = False

    def rect(self, x0, y0, x1, y1, color, glow=False):
        for y in range(int(y0), int(y1)):
            for x in range(int(x0), int(x1)):
                self.set(x, y, color, glow)

    def shade(self, x0, y0, x1, y1, k):
        for y in range(max(0, int(y0)), min(self.h, int(y1))):
            for x in range(max(0, int(x0)), min(self.w, int(x1))):
                if self.px[y][x] is not None:
                    self.px[y][x] = mul(self.px[y][x], k)

    def line(self, x0, y0, x1, y1, color, glow=False):
        n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
        for i in range(n):
            t = i / max(1, n - 1)
            self.set(round(x0 + (x1 - x0) * t), round(y0 + (y1 - y0) * t), color, glow)


class Model:
    def __init__(self, name, seed=None):
        self.name = name
        self.seed = seed if seed is not None else int(h32(*[ord(c) for c in name]) * 1e6)
        self.bones, self.by = [], {}
        self.anims = {}
        self.bone("root", [0, 0, 0])

    # ------------------------------------------------------------------ построение
    def bone(self, name, pivot, parent="root", rot=None):
        assert name not in self.by, f"{self.name}: кость {name} уже есть"
        b = Bone(name, pivot, parent if name != "root" else None, rot)
        self.bones.append(b)
        self.by[name] = b
        return name

    def cube(self, bone, origin, size, mat, **kw):
        assert mat in MATS, f"{self.name}: нет материала {mat}"
        c = Cube(origin, size, mat, **kw)
        self.by[bone].cubes.append(c)
        return c

    def box(self, bone, center, size, mat, **kw):
        """Куб по центру (удобнее для симметричных деталей)."""
        o = [center[i] - size[i] / 2 for i in range(3)]
        return self.cube(bone, o, size, mat, **kw)

    def mirror(self, bone, origin, size, mat, **kw):
        """Пара кубов, зеркальных по x (левый и правый)."""
        a = self.cube(bone, origin, size, mat, **kw)
        o2 = [-(origin[0] + size[0]), origin[1], origin[2]]
        kw2 = dict(kw)
        if "rot" in kw and kw["rot"]:
            r = kw["rot"]
            kw2["rot"] = [r[0], -r[1], -r[2]]
        if "pivot" in kw and kw["pivot"]:
            p = kw["pivot"]
            kw2["pivot"] = [-p[0], p[1], p[2]]
        b = self.cube(bone, o2, size, mat, **kw2)
        return a, b

    def taper(self, bone, base, top, length, r0, r1, mat, steps=3, axis=1, **kw):
        """Сужающаяся часть из нескольких кубов (рог, конечность, хвост) вдоль оси y (или -y при length < 0)."""
        out = []
        for i in range(steps):
            t0, t1 = i / steps, (i + 1) / steps
            r = r0 + (r1 - r0) * (t0 + t1) / 2
            y0 = base[1] + length * t0
            h = abs(length) / steps
            cx = base[0] + (top[0] - base[0]) * (t0 + t1) / 2
            cz = base[2] + (top[2] - base[2]) * (t0 + t1) / 2
            yy = y0 if length > 0 else y0 - h
            out.append(self.cube(bone, [cx - r / 2, yy, cz - r / 2], [r, h + 0.01, r], mat, **kw))
        return out

    def segments(self, prefix, parent, pivot, parts, mat, up=True, **kw):
        """Изогнутая цепочка (рог, хвост, щупальце, коса): каждая часть — своя кость, повёрнутая относительно
        предыдущей, поэтому изгиб гладкий и части не расходятся. parts — [(длина, толщина, [rx, ry, rz]), …];
        up — растёт вверх по y (рог), иначе вниз (хвост, щупальце). Возвращает имена костей."""
        names, p = [], list(pivot)
        for i, (ln, th, r) in enumerate(parts):
            name = self.bone(f"{prefix}{i}", p, parent, rot=r)
            y0 = p[1] if up else p[1] - ln
            mt = mat[i] if isinstance(mat, (list, tuple)) else mat
            self.cube(name, [p[0] - th / 2, y0, p[2] - th / 2], [th, ln + 0.05, th], mt, **kw)
            names.append(name)
            parent = name
            p = [p[0], p[1] + ln if up else p[1] - ln, p[2]]
        return names

    def anim(self, name, a):
        self.anims[name] = a

    # ------------------------------------------------------------------ выгрузка
    def bounds(self):
        ys = [c.origin[1] for b in self.bones for c in b.cubes] + \
             [c.origin[1] + c.size[1] for b in self.bones for c in b.cubes]
        return (min(ys), max(ys)) if ys else (0, 1)

    def paint(self):
        rects = []
        for b in self.bones:
            for c in b.cubes:
                w, h, d = (max(1, math.ceil(v * D - 1e-6)) for v in c.size)
                for f, (fw, fh) in {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h),
                                    "up": (w, d), "down": (w, d)}.items():
                    if f not in c.skip:
                        rects.append((fh, fw, c, f))
        area = sum(r[0] * r[1] for r in rects)
        tw = 128
        while tw * tw < area * 1.5:
            tw *= 2
        rects.sort(key=lambda r: (-r[0], -r[1]))
        x = y = shelf = 0
        for fh, fw, c, f in rects:
            if x + fw > tw:
                x, y, shelf = 0, y + shelf, 0
            c.uv[f] = (x, y, fw, fh)
            x += fw
            shelf = max(shelf, fh)
        th = y + shelf
        th = max(16, (th + 15) // 16 * 16)
        tex, glow = Canvas(tw, th), Canvas(tw, th)
        ymin, ymax = self.bounds()
        for bi, b in enumerate(self.bones):
            for ci, c in enumerate(b.cubes):
                for f, (fx, fy, fw, fh) in c.uv.items():
                    m = MATS[c.faces.get(f, c.mat)]
                    seed = self.seed + bi * 101 + ci * 13 + FACE_ID[f]
                    px = [[None] * fw for _ in range(fh)]
                    gl = [[False] * fw for _ in range(fh)]
                    for yy in range(fh):
                        for xx in range(fw):
                            col, g = pattern_color(m, xx + fx * 0.37, yy + fy * 0.29, fw, fh, f, seed, c)
                            col = light_px(col, m, xx, yy, fw, fh, f, c, ymin, ymax) if not g else col
                            px[yy][xx] = mul(col, c.light if not g else 1.0)
                            gl[yy][xx] = g
                    if f in c.decal:
                        c.decal[f](Face(fw, fh, px, gl))
                    for yy in range(fh):
                        for xx in range(fw):
                            if px[yy][xx] is None:
                                continue
                            col = tuple(int(round(v)) for v in px[yy][xx]) + (255,)
                            tex.set(fx + xx, fy + yy, col)
                            if gl[yy][xx]:
                                glow.set(fx + xx, fy + yy, col)
        return tex, glow, tw, th

    def geo(self, tw, th):
        bones = []
        lo, hi = self.bounds()
        for b in self.bones:
            bj = {"name": b.name, "pivot": [round(v, 4) for v in b.pivot]}
            if b.parent:
                bj["parent"] = b.parent
            if b.rot:
                bj["rotation"] = list(b.rot)
            if b.cubes:
                bj["cubes"] = []
                for c in b.cubes:
                    cj = {"origin": [round(v, 4) for v in c.origin], "size": [round(v, 4) for v in c.size],
                          "uv": {f: {"uv": [u, v], "uv_size": [w, h]} for f, (u, v, w, h) in c.uv.items()}}
                    if c.inflate:
                        cj["inflate"] = c.inflate
                    if c.rot:
                        cj["rotation"] = list(c.rot)
                        cj["pivot"] = list(c.pivot if c.pivot else [c.origin[i] + c.size[i] / 2 for i in range(3)])
                    bj["cubes"].append(cj)
            bones.append(bj)
        size = max(4, (hi - lo) / 16 + 2)
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": f"geometry.{self.name}", "texture_width": tw, "texture_height": th,
                            "visible_bounds_width": size, "visible_bounds_height": size,
                            "visible_bounds_offset": [0, size / 2, 0]},
            "bones": bones}]}

    def zfight(self):
        """Грани разных кубов в одной плоскости и смотрят в одну сторону — в игре рябь (z-fighting).
        Проверка в покое для кубов без собственного поворота в костях без поворота. → [(куб, куб, описание)]."""
        flat = []
        for b in self.bones:
            if b.rot:
                continue
            for c in b.cubes:
                if c.rot:
                    continue
                i = c.inflate
                lo = [c.origin[k] - i for k in range(3)]
                hi = [c.origin[k] + c.size[k] + i for k in range(3)]
                flat.append((b.name, c, lo, hi))
        ground = min((f[2][1] for f in flat), default=0)
        out = []
        for ai in range(len(flat)):
            for bi in range(ai + 1, len(flat)):
                na, ca, la, ha = flat[ai]
                nb, cb, lb, hb = flat[bi]
                for ax in range(3):
                    o = [k for k in range(3) if k != ax]
                    if not all(min(ha[k], hb[k]) - max(la[k], lb[k]) > 0.05 for k in o):
                        continue
                    for side, va, vb in (("-", la[ax], lb[ax]), ("+", ha[ax], hb[ax])):
                        if abs(va - vb) < 0.02 and not (ax == 1 and side == "-" and abs(va - ground) < 0.02):
                            out.append((ca, cb, f"{na}/{nb} {'xyz'[ax]}{side}={va:.2f}"))
        return out

    def save(self, preview=None):
        # Совпадающие грани разводим на 0,02 (незаметно глазу, но рябь пропадает): у второго куба пары — inflate.
        for _ in range(4):
            zf = self.zfight()
            if not zf:
                break
            for ca, cb, _d in zf:
                small = cb if cb.size[0] * cb.size[1] * cb.size[2] <= ca.size[0] * ca.size[1] * ca.size[2] else ca
                small.inflate = round(small.inflate + 0.02, 3)
        left = self.zfight()
        if left:
            print(f"  ! {self.name}: осталась рябь: " + "; ".join(d for _a, _b, d in left[:8]))
        tex, glow, tw, th = self.paint()
        geo = self.geo(tw, th)
        anim = {"format_version": "1.8.0", "animations": {f"animation.{self.name}.{k}": v for k, v in self.anims.items()}}
        (ASSETS / "geo/entity").mkdir(parents=True, exist_ok=True)
        (ASSETS / "animations/entity").mkdir(parents=True, exist_ok=True)
        (ASSETS / f"geo/entity/{self.name}.geo.json").write_text(json.dumps(geo, indent=1), encoding="utf-8", newline="\n")
        (ASSETS / f"animations/entity/{self.name}.animation.json").write_text(json.dumps(anim, indent=1),
                                                                             encoding="utf-8", newline="\n")
        tex.save(ASSETS / f"textures/entity/{self.name}.png")
        gf = ASSETS / f"textures/entity/{self.name}_glowmask.png"
        if any(p[3] for p in glow.px):
            glow.save(gf)
        elif gf.exists():
            gf.unlink()
        cubes = sum(len(b.cubes) for b in self.bones)
        return f"{self.name}: {len(self.bones)} костей, {cubes} кубов, текстура {tw}x{th}"


def light_px(col, m, x, y, w, h, face, c, ymin, ymax):
    """Свет: верх светлее, низ темнее; кромка сверху — блик, снизу и по бокам — тень; у пола модели — тень."""
    k = 1.0
    if face == "up":
        k *= 1.14
    elif face == "down":
        k *= 0.62
    else:
        # высота тексела в модели: от низа куба к верху
        wy = c.origin[1] + c.size[1] * (1 - (y + 0.5) / h)
        t = (wy - ymin) / max(1e-6, ymax - ymin)
        k *= 0.78 + 0.27 * t
        k *= 1.06 - 0.12 * (y / max(1, h - 1))        # градиент по самой грани
        if face in ("east", "west"):
            k *= 0.9                                     # боковые грани чуть темнее лицевых
        if face == "south":
            k *= 0.94
    # кромки
    if face not in ("up", "down"):
        if y == 0:
            k *= 1.16
        elif y == h - 1:
            k *= 0.8
        if x == 0 or x == w - 1:
            k *= 0.9
    else:
        if x == 0 or y == 0 or x == w - 1 or y == h - 1:
            k *= 0.9
    col = mul(col, k)
    if m.gloss and face == "up":
        col = mix(col, m.light, 0.25 * m.gloss)
    return col


# ============================================================================ анимации

def kf(pairs, smooth=True):
    """[(время, [x, y, z]), …] → ключи; smooth — плавные кривые (catmullrom)."""
    out = {}
    for t, v in pairs:
        key = f"{t:.3f}".rstrip("0").rstrip(".") or "0"
        out[key] = {"post": [round(x, 3) for x in v], "lerp_mode": "catmullrom"} if smooth else [round(x, 3) for x in v]
    return out


def loop(length, bones):
    return {"loop": True, "animation_length": round(length, 3), "bones": bones}


def once(length, bones, hold=False):
    a = {"animation_length": round(length, 3), "bones": bones}
    if hold:
        a["hold_on_last_frame"] = True
    return a


def sway(length, amp, phase=0.0, axis=0, base=None):
    """Плавное качание по кругу: ключи в 0, ¼, ½, ¾, 1 длины."""
    base = base or [0, 0, 0]
    pts = []
    for i in range(5):
        t = length * i / 4
        v = list(base)
        v[axis] += amp * math.sin(2 * math.pi * (i / 4 + phase))
        pts.append((t, v))
    return kf(pts)


def beats(length, frames):
    """Ключи по долям длины: [(0.0, [..]), (0.4, [..]), …] → kf."""
    return kf([(length * t, v) for t, v in frames])
