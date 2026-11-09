"""Предпросмотр модели GeckoLib без игры: geo.json + текстура → png с тремя видами (спереди-сбоку, сбоку, сзади).

    python tools/model_preview.py имя [--anim cast --time 0.4] [--out build/preview/models] [--size 360]
    python tools/model_preview.py --all [--out ...]          — все модели, по картинке на каждую
    python tools/model_preview.py --sheet a,b,c --out x.png   — несколько моделей на одном листе (сравнить размеры)

Повторяет то, как GeckoLib строит модель: x зеркалится, поворот кости — Z·Y·X вокруг опоры, X и Y со знаком минус.
Свет мягкий сверху-спереди, светящиеся пиксели (маска свечения) — без затенения. Нужен, чтобы проверить модель
глазами через Read до запуска клиента; окончательная проверка — снимками в игре.
"""
import json
import math
import struct
import sys
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from png import Canvas  # noqa: E402

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/litrpg"


# ---------------------------------------------------------------------------- чтение png

def read_png(path):
    data = Path(path).read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", path
    pos, idat = 8, b""
    w = h = 0
    ctype = 6
    while pos < len(data):
        ln = struct.unpack(">I", data[pos:pos + 4])[0]
        kind = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + ln]
        if kind == b"IHDR":
            w, h, depth, ctype = struct.unpack(">IIBB", body[:10])
            assert depth == 8 and ctype in (2, 6), f"{path}: поддержан только RGB/RGBA 8 бит"
        elif kind == b"IDAT":
            idat += body
        pos += 12 + ln
    bpp = 4 if ctype == 6 else 3
    raw = zlib.decompress(idat)
    stride = w * bpp
    out, prev, i = [], bytearray(stride), 0
    for _ in range(h):
        f = raw[i]
        line = bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if f == 1:
                line[x] = (line[x] + a) & 255
            elif f == 2:
                line[x] = (line[x] + b) & 255
            elif f == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        out.append(bytes(line))
        prev = line
    px = []
    for row in out:
        for x in range(w):
            px.append(tuple(row[x * bpp:x * bpp + bpp]) + ((255,) if bpp == 3 else ()))
    return w, h, px


# ---------------------------------------------------------------------------- матрицы

def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def translate(x, y, z):
    return [[1, 0, 0, x], [0, 1, 0, y], [0, 0, 1, z], [0, 0, 0, 1]]


def rot(axis, deg):
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    if axis == "x":
        return [[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]]
    if axis == "y":
        return [[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]]
    return [[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]


def apply(m, v):
    return [m[i][0] * v[0] + m[i][1] * v[1] + m[i][2] * v[2] + m[i][3] for i in range(3)]


def euler(rx, ry, rz):
    """Поворот как у GeckoLib: X и Y со знаком минус, порядок Z·Y·X."""
    return mat_mul(rot("z", rz), mat_mul(rot("y", -ry), rot("x", -rx)))


# ---------------------------------------------------------------------------- анимация

def sample(track, t, length):
    """Значение дорожки (rotation/position) в момент t: линейно между ключами."""
    if isinstance(track, list):
        return track
    if isinstance(track, dict) and "vector" in track:
        return track["vector"]
    keys = sorted((float(k), v) for k, v in track.items())

    def val(v):
        if isinstance(v, dict):
            v = v.get("post", v.get("vector", v.get("pre", [0, 0, 0])))
            if isinstance(v, dict):
                v = v.get("vector", [0, 0, 0])
        return [float(x) if not isinstance(x, str) else 0.0 for x in v]

    if t <= keys[0][0]:
        return val(keys[0][1])
    for (t0, v0), (t1, v1) in zip(keys, keys[1:]):
        if t0 <= t <= t1:
            k = 0 if t1 == t0 else (t - t0) / (t1 - t0)
            a, b = val(v0), val(v1)
            return [a[i] + (b[i] - a[i]) * k for i in range(3)]
    return val(keys[-1][1])


def pose(anim, t):
    """{кость: (поворот, сдвиг)} в момент t."""
    out = {}
    if not anim:
        return out
    length = anim.get("animation_length", 1)
    for bone, tr in anim.get("bones", {}).items():
        r = sample(tr["rotation"], t, length) if "rotation" in tr else [0, 0, 0]
        p = sample(tr["position"], t, length) if "position" in tr else [0, 0, 0]
        out[bone] = (r, p)
    return out


# ---------------------------------------------------------------------------- геометрия

FACES = {
    # грань: 4 угла (в долях куба), нормаль. Углы по порядку: верх-лево, верх-право, низ-право, низ-лево текстуры.
    "north": ([(1, 1, 0), (0, 1, 0), (0, 0, 0), (1, 0, 0)], (0, 0, -1)),
    "south": ([(0, 1, 1), (1, 1, 1), (1, 0, 1), (0, 0, 1)], (0, 0, 1)),
    "east": ([(0, 1, 0), (0, 1, 1), (0, 0, 1), (0, 0, 0)], (-1, 0, 0)),
    "west": ([(1, 1, 1), (1, 1, 0), (1, 0, 0), (1, 0, 1)], (1, 0, 0)),
    "up": ([(0, 1, 1), (1, 1, 1), (1, 1, 0), (0, 1, 0)], (0, 1, 0)),
    "down": ([(0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)], (0, -1, 0)),
}


def box_uv(cube):
    w, h, d = (math.ceil(v) for v in cube["size"])
    u, v = cube["uv"]
    return {
        "up": (u + d, v, w, d), "down": (u + d + w, v + d, w, -d),
        "east": (u, v + d, d, h), "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h),
    }


def face_uvs(cube):
    uv = cube.get("uv", [0, 0])
    if isinstance(uv, dict):
        out = {}
        for f, spec in uv.items():
            (u, v), (w, h) = spec["uv"], spec.get("uv_size", [0, 0])
            out[f] = (u, v, w, h)
        return out
    return box_uv(cube)


def triangles(geo, anim=None, t=0.0):
    """Все грани модели в мировых координатах: (4 точки, 4 uv, нормаль)."""
    bones = geo["minecraft:geometry"][0]["bones"]
    by = {b["name"]: b for b in bones}
    pz = pose(anim, t)
    cache = {}

    def world(name):
        if name in cache:
            return cache[name]
        b = by[name]
        px, py, pzv = b.get("pivot", [0, 0, 0])
        px = -px
        r = list(b.get("rotation", [0, 0, 0]))
        off = [0, 0, 0]
        if name in pz:
            ar, ap = pz[name]
            r = [r[i] + ar[i] for i in range(3)]
            off = [-ap[0], ap[1], ap[2]]
        local = mat_mul(translate(px + off[0], py + off[1], pzv + off[2]),
                        mat_mul(euler(*r), translate(-px, -py, -pzv)))
        m = mat_mul(world(b["parent"]), local) if b.get("parent") in by else local
        cache[name] = m
        return m

    out = []
    for b in bones:
        m = world(b["name"])
        for c in b.get("cubes", []):
            ox, oy, oz = c["origin"]
            sx, sy, sz = c["size"]
            inf = c.get("inflate", 0)
            x0 = -(ox + sx) - inf
            y0, z0 = oy - inf, oz - inf
            ex, ey, ez = sx + 2 * inf, sy + 2 * inf, sz + 2 * inf
            cm = m
            if "rotation" in c:
                cp = c.get("pivot", [0, 0, 0])
                cpx = -cp[0]
                cm = mat_mul(m, mat_mul(translate(cpx, cp[1], cp[2]),
                                        mat_mul(euler(*c["rotation"]), translate(-cpx, -cp[1], -cp[2]))))
            uvs = face_uvs(c)
            for f, (corners, n) in FACES.items():
                if f not in uvs:
                    continue
                u, v, w, h = uvs[f]
                if w == 0 or h == 0:
                    continue
                pts = [apply(cm, [x0 + ex * a, y0 + ey * bb, z0 + ez * cc]) for a, bb, cc in corners]
                quv = [(u, v), (u + w, v), (u + w, v + h), (u, v + h)]
                nn = apply(cm, list(n))
                o = apply(cm, [0, 0, 0])
                out.append((pts, quv, [nn[i] - o[i] for i in range(3)]))
    return out


# ---------------------------------------------------------------------------- растеризация

def render(name, size=360, anim_name=None, t=0.0, yaws=(-35, 90, 180), pitch=12, bg=(34, 36, 44)):
    geo = json.loads((ASSETS / f"geo/entity/{name}.geo.json").read_text(encoding="utf-8"))
    tw, th, tex = read_png(ASSETS / f"textures/entity/{name}.png")
    gpath = ASSETS / f"textures/entity/{name}_glowmask.png"
    glow = read_png(gpath)[2] if gpath.exists() else None
    anim = None
    if anim_name:
        aj = json.loads((ASSETS / f"animations/entity/{name}.animation.json").read_text(encoding="utf-8"))
        anim = aj["animations"].get(f"animation.{name}.{anim_name}")
        assert anim, f"{name}: нет анимации {anim_name}"
    tris = triangles(geo, anim, t)
    allp = [p for q in tris for p in q[0]]
    lo = [min(p[i] for p in allp) for i in range(3)]
    hi = [max(p[i] for p in allp) for i in range(3)]
    center = [(lo[i] + hi[i]) / 2 for i in range(3)]
    radius = max(math.dist(p, center) for p in allp) or 1
    img = Canvas(size * len(yaws), size, (*bg, 255))
    light = norm((0.4, 0.8, -0.6))
    for vi, yaw in enumerate(yaws):
        zbuf = [-1e9] * (size * size)
        view = mat_mul(rot("x", pitch), rot("y", yaw))
        scale = size * 0.46 / radius
        for pts, quv, n in tris:
            nv = apply(view, n)
            if nv[2] > 0.0001:   # грань смотрит от камеры (камера на -z)
                continue
            lam = 0.5 + 0.5 * max(0.0, dot(norm(n), light))
            sp = []
            for p in pts:
                q = apply(view, [p[i] - center[i] for i in range(3)])
                sp.append((size / 2 - q[0] * scale, size / 2 - q[1] * scale, -q[2]))
            for tri in ((0, 1, 2), (0, 2, 3)):
                raster(img, zbuf, size, vi * size, [sp[i] for i in tri], [quv[i] for i in tri],
                       tex, glow, tw, th, lam)
    return img


def norm(v):
    ln = math.sqrt(sum(c * c for c in v)) or 1
    return tuple(c / ln for c in v)


def dot(a, b):
    return sum(x * y for x, y in zip(a, b))


def raster(img, zbuf, size, xoff, p, uv, tex, glow, tw, th, lam):
    (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = p
    minx, maxx = max(0, int(min(x0, x1, x2))), min(size - 1, int(max(x0, x1, x2)) + 1)
    miny, maxy = max(0, int(min(y0, y1, y2))), min(size - 1, int(max(y0, y1, y2)) + 1)
    den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
    if abs(den) < 1e-9:
        return
    for y in range(miny, maxy + 1):
        for x in range(minx, maxx + 1):
            fx, fy = x + 0.5, y + 0.5
            a = ((y1 - y2) * (fx - x2) + (x2 - x1) * (fy - y2)) / den
            b = ((y2 - y0) * (fx - x2) + (x0 - x2) * (fy - y2)) / den
            c = 1 - a - b
            if a < -1e-6 or b < -1e-6 or c < -1e-6:
                continue
            z = a * z0 + b * z1 + c * z2
            i = y * size + x
            if z <= zbuf[i]:
                continue
            u = a * uv[0][0] + b * uv[1][0] + c * uv[2][0]
            v = a * uv[0][1] + b * uv[1][1] + c * uv[2][1]
            tu, tv = min(tw - 1, max(0, int(u))), min(th - 1, max(0, int(v)))
            col = tex[tv * tw + tu]
            if col[3] < 16:
                continue
            zbuf[i] = z
            gl = glow and glow[tv * tw + tu][3] > 16
            k = 1.0 if gl else lam
            img.set(xoff + x, y, (min(255, int(col[0] * k)), min(255, int(col[1] * k)), min(255, int(col[2] * k)), 255))


def main():
    a = sys.argv[1:]

    def opt(k, d=None):
        return a[a.index(k) + 1] if k in a else d

    out = Path(opt("--out", "build/preview/models"))
    size = int(opt("--size", "360"))
    anim, t = opt("--anim"), float(opt("--time", "0"))
    if "--sheet" in a:
        names = opt("--sheet").split(",")
        tiles = [render(n, size, anim, t, yaws=(-35,)) for n in names]
        sheet = Canvas(size * len(tiles), size)
        for i, tile in enumerate(tiles):
            for y in range(size):
                for x in range(size):
                    sheet.set(i * size + x, y, tile.get(x, y))
        sheet.save(out)
        print(out)
        return
    names = [p.name[:-9] for p in sorted((ASSETS / "geo/entity").glob("*.geo.json"))] if "--all" in a else [a[0]]
    out.mkdir(parents=True, exist_ok=True)
    for n in names:
        suffix = f"_{anim}_{t:g}" if anim else ""
        render(n, size, anim, t).save(out / f"{n}{suffix}.png")
        print(out / f"{n}{suffix}.png")


if __name__ == "__main__":
    main()
