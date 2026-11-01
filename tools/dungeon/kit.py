"""
Набор строителя карты подземелья.

Карта «вырезается»: задаём пустоты (залы, коридоры, пещеры, пропасти), а стены, пол и потолок вокруг пустот
достраиваются сами — материалом темы того места (`Map.shell`). Потом — отделка, свет и схема этапа
(отряды, хранители, преграды, разлом) в `Layout`.

Выход:
  src/main/resources/litrpg_map/map.bin     — блоки (gzip: палитра + секции 16³ с RLE), читает DungeonMap
  src/main/resources/litrpg_map/layout.json — схема прохождения, читает Layout

Координаты абсолютные, мир litrpg:dungeon. Преддверие — около x=0, этап n — около x = 1024·n.
"""
import gzip
import json
import math
import os
import struct
import sys

ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'litrpg_map')
AIR = 'minecraft:air'

# Блоки, сквозь которые проходят (для проверки проходимости) и которые не считаются опорой.
PASSABLE = ('air', 'light', 'torch', 'wall_torch', 'soul_torch', 'soul_wall_torch', 'redstone_torch', 'carpet', 'candle',
            'button', 'pressure_plate', 'lever', 'rail', 'ladder', 'vine', 'sign', 'banner', 'cobweb', 'chain', 'flower',
            'fern', 'short_grass', 'tall_grass', 'dead_bush', 'mushroom', 'sapling', 'glow_lichen', 'sculk_vein',
            'moss_carpet', 'pink_petals', 'tripwire', 'string', 'snow', 'water', 'lantern', 'roots', 'sprouts',
            'hanging_roots', 'spore_blossom', 'small_dripleaf', 'big_dripleaf_stem', 'rift')
# Похожи на проходимые по имени, но твёрдые.
SOLID_EXCEPT = ('sea_lantern', 'redstone_lamp', 'jack_o_lantern', 'mushroom_block', 'brown_mushroom_block',
                'red_mushroom_block', 'mushroom_stem', 'grass_block', 'chain_command_block', 'snow_block',
                'mangrove_roots', 'muddy_mangrove_roots', 'flowering_azalea', 'flower_pot', 'powder_snow')


def h3(x, y, z, seed=0):
    """Детерминированный хеш точки (0..1)."""
    n = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791) ^ (seed * 2654435761)
    n &= 0xFFFFFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFFFF) / 0xFFFFFF


def noise2(x, z, scale=8.0, seed=0):
    """Гладкий шум 2D (билинейная интерполяция хешей решётки)."""
    fx, fz = x / scale, z / scale
    x0, z0 = math.floor(fx), math.floor(fz)
    tx, tz = fx - x0, fz - z0
    tx, tz = tx * tx * (3 - 2 * tx), tz * tz * (3 - 2 * tz)
    a, b = h3(x0, 0, z0, seed), h3(x0 + 1, 0, z0, seed)
    c, d = h3(x0, 0, z0 + 1, seed), h3(x0 + 1, 0, z0 + 1, seed)
    return (a * (1 - tx) + b * tx) * (1 - tz) + (c * (1 - tx) + d * tx) * tz


def state(name, **props):
    """Строка состояния блока: 'minecraft:lantern[hanging=true]'. Свойства — по алфавиту."""
    if ':' not in name:
        name = 'minecraft:' + name
    if not props:
        return name
    kv = ','.join(f'{k}={str(v).lower() if isinstance(v, bool) else v}' for k, v in sorted(props.items()))
    return f'{name}[{kv}]'


def base(s):
    """Имя блока без свойств и пространства имён."""
    return s.split('[')[0].split(':')[1]


def passable(s):
    """Сквозь блок проходят (воздух, свет, факелы, ковры, цепи…)."""
    if s is None:
        return True
    b = base(s)
    if b in SOLID_EXCEPT:
        return False
    return b == 'air' or any(k in b for k in PASSABLE)


def standable(s):
    """На блоке можно стоять (полный блок, плита, ступени, сундук…), но не на жидкости и не на разломе."""
    if s is None or passable(s):
        return False
    b = base(s)
    return 'lava' not in b and 'magma' not in b


class Theme:
    """Материалы места: стены, пол, потолок. Палитра — [(блок, вес)] или один блок."""

    def __init__(self, name, wall, floor=None, ceil=None, seed=1):
        self.name = name
        self.wall = self._norm(wall)
        self.floor = self._norm(floor or wall)
        self.ceil = self._norm(ceil or wall)
        self.seed = seed

    @staticmethod
    def _norm(p):
        if isinstance(p, str):
            return [(state(p), 1)]
        return [(state(b), w) for b, w in p]

    @staticmethod
    def pick(pal, x, y, z, seed):
        total = sum(w for _, w in pal)
        r = h3(x, y, z, seed) * total
        for b, w in pal:
            r -= w
            if r <= 0:
                return b
        return pal[-1][0]


class Map:
    def __init__(self, seed=7):
        self.blocks = {}       # (x,y,z) -> строка состояния
        self.open = {}         # пустоты: (x,y,z) -> имя темы
        self.nowall = set()    # пустоты без оболочки снизу (пропасти)
        self.themes = {}
        self.seed = seed

    # ---------------- блоки ----------------

    def set(self, x, y, z, name, **props):
        self.blocks[(x, y, z)] = state(name, **props)

    def get(self, x, y, z):
        return self.blocks.get((x, y, z))

    def clear(self, x, y, z):
        self.blocks.pop((x, y, z), None)

    def fill(self, x1, y1, z1, x2, y2, z2, name, **props):
        s = state(name, **props)
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.blocks[(x, y, z)] = s

    def fillp(self, x1, y1, z1, x2, y2, z2, pal, seed=0):
        pal = Theme._norm(pal)
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.blocks[(x, y, z)] = Theme.pick(pal, x, y, z, seed + self.seed)

    def solid(self, x, y, z):
        return not passable(self.blocks.get((x, y, z)))

    def is_air(self, x, y, z):
        return passable(self.blocks.get((x, y, z)))

    # ---------------- пустоты ----------------

    def theme(self, t):
        self.themes[t.name] = t
        return t

    def carve(self, x1, y1, z1, x2, y2, z2, theme, wall=True):
        """Пустота-параллелепипед; y1 — уровень, где стоят ноги (пол — блок y1-1)."""
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.open[(x, y, z)] = theme.name
                    if not wall:
                        self.nowall.add((x, y, z))

    def room(self, x1, z1, x2, z2, y, h, theme, vault=0):
        """Зал: пол на y-1, высота h, сводчатый потолок (vault — на сколько выше в середине)."""
        self.carve(x1, y, z1, x2, y + h - 1, z2, theme)
        if vault > 0:
            cx, cz = (x1 + x2) / 2, (z1 + z2) / 2
            hx, hz = max(1, (x2 - x1) / 2), max(1, (z2 - z1) / 2)
            for x in range(x1, x2 + 1):
                for z in range(z1, z2 + 1):
                    k = 1 - max(((x - cx) / hx) ** 2, ((z - cz) / hz) ** 2)
                    extra = int(round(vault * math.sqrt(max(0, k))))
                    for dy in range(extra):
                        self.open[(x, y + h + dy, z)] = theme.name

    def round_room(self, cx, cz, r, y, h, theme, dome=0):
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            for z in range(int(cz - r) - 1, int(cz + r) + 2):
                d = math.hypot(x - cx, z - cz)
                if d <= r:
                    top = h + (int(round(dome * math.sqrt(max(0, 1 - (d / r) ** 2)))) if dome else 0)
                    for yy in range(y, y + top):
                        self.open[(x, yy, z)] = theme.name

    def cave(self, cx, cy, cz, rx, ry, rz, theme, rough=0.25, seed=0, floor=None):
        """Пещера-капля с шумной стенкой; floor — ровный пол на этой высоте (ниже не вырезать)."""
        for x in range(int(cx - rx) - 2, int(cx + rx) + 3):
            for z in range(int(cz - rz) - 2, int(cz + rz) + 3):
                n = noise2(x, z, 6, seed + 11) - 0.5
                for y in range(int(cy - ry) - 2, int(cy + ry) + 3):
                    if floor is not None and y < floor:
                        continue
                    dy = 0 if floor is not None and y < cy else (y - cy)
                    d = ((x - cx) / rx) ** 2 + (dy / ry) ** 2 + ((z - cz) / rz) ** 2
                    if d <= 1 + n * rough * 2 + (h3(x, y, z, seed) - 0.5) * rough * 0.4:
                        self.open[(x, y, z)] = theme.name

    def corridor(self, points, w, h, theme, y=None):
        """Коридор по ломаной (x, z[, y]) шириной w и высотой h. Подъём — пологий пандус ступенями."""
        pts = [(p[0], p[1], p[2] if len(p) > 2 else y) for p in points]
        for (ax, az, ay), (bx, bz, by) in zip(pts, pts[1:]):
            n = max(abs(bx - ax), abs(bz - az), 1)
            for i in range(n + 1):
                t = i / n
                x = round(ax + (bx - ax) * t)
                z = round(az + (bz - az) * t)
                yy = round(ay + (by - ay) * t)
                for dx in range(-(w // 2), w - w // 2):
                    for dz in range(-(w // 2), w - w // 2):
                        for dy in range(h + (1 if by != ay else 0)):
                            self.open[(x + dx, yy + dy, z + dz)] = theme.name

    def pit(self, x1, z1, x2, z2, ytop, depth, theme):
        """Пропасть: пустота вниз без дна (упавшего Система вернёт на край)."""
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                for y in range(ytop - depth, ytop):
                    self.open[(x, y, z)] = theme.name
                    self.nowall.add((x, y, z))

    # ---------------- оболочка ----------------

    def shell(self, thick=2):
        """Стены, пол и потолок вокруг всех пустот (материал — тема ближайшей пустоты). Ставится до отделки."""
        opn = self.open
        blocks = self.blocks
        border = [p for p in opn if any(q not in opn for q in (
            (p[0] + 1, p[1], p[2]), (p[0] - 1, p[1], p[2]), (p[0], p[1] + 1, p[2]),
            (p[0], p[1] - 1, p[2]), (p[0], p[1], p[2] + 1), (p[0], p[1], p[2] - 1)))]
        rng = range(-thick, thick + 1)
        for (x, y, z) in border:
            t = self.themes[opn[(x, y, z)]]
            pit = (x, y, z) in self.nowall
            for dy in rng:
                if pit and dy < 0:
                    continue
                for dx in rng:
                    for dz in rng:
                        p = (x + dx, y + dy, z + dz)
                        if p in opn or p in blocks:
                            continue
                        if dy < 0 and abs(dx) <= 1 and abs(dz) <= 1:
                            pal = t.floor
                        elif dy > 0 and abs(dx) <= 1 and abs(dz) <= 1:
                            pal = t.ceil
                        else:
                            pal = t.wall
                        blocks[p] = Theme.pick(pal, p[0], p[1], p[2], t.seed)

    # ---------------- отделка ----------------

    def floor_y(self, x, z, ytop):
        """Уровень ног над полом под точкой (первая пустота над твёрдым)."""
        for y in range(ytop, ytop - 64, -1):
            if (x, y, z) in self.open and self.is_air(x, y, z) and self.solid(x, y - 1, z):
                return y
        return ytop

    def ceil_y(self, x, y, z, limit=40):
        """Последняя открытая клетка над (x, y, z) до потолка."""
        top = y
        while (x, top + 1, z) in self.open and self.is_air(x, top + 1, z) and top - y < limit:
            top += 1
        return top

    def pillar(self, x, z, y, h, block, cap=None, base_=None):
        for yy in range(y, y + h):
            self.set(x, yy, z, block)
        if base_:
            self.set(x, y, z, base_)
        if cap:
            self.set(x, y + h - 1, z, cap)

    def lantern(self, x, y, z, soul=False, hanging=False):
        self.set(x, y, z, 'soul_lantern' if soul else 'lantern', hanging=hanging, waterlogged=False)

    def chain_lantern(self, x, ytop, z, length=2, soul=False):
        for i in range(length):
            self.set(x, ytop - i, z, 'chain', axis='y', waterlogged=False)
        self.lantern(x, ytop - length, z, soul, hanging=True)

    def candle(self, x, y, z, n=3, color=None):
        self.set(x, y, z, f'{color}_candle' if color else 'candle', candles=n, lit=True, waterlogged=False)

    def wall_torch(self, x, y, z, facing, soul=False):
        self.set(x, y, z, 'soul_wall_torch' if soul else 'wall_torch', facing=facing)

    def stairs(self, x, y, z, block, facing, half='bottom'):
        self.set(x, y, z, block, facing=facing, half=half, shape='straight', waterlogged=False)

    def slab(self, x, y, z, block, half='bottom'):
        self.set(x, y, z, block, type=half, waterlogged=False)

    def glow(self, spacing=6, level=10, themes=None, height=2):
        """Невидимые источники света (minecraft:light) по сетке — ровная полутьма без лишних фонарей."""
        for (x, y, z), t in list(self.open.items()):
            if themes and t not in themes:
                continue
            if x % spacing or z % spacing:
                continue
            if not (self.solid(x, y - 1, z) and (x, y - 1, z) not in self.open or (x, y - 1, z) in self.blocks and self.solid(x, y - 1, z)):
                continue
            ly = y + height
            if (x, ly, z) not in self.open or (x, ly, z) in self.blocks:
                continue
            self.set(x, ly, z, 'light', level=level, waterlogged=False)

    def hanging_lights(self, spacing=7, soul=True, themes=None, min_h=5, chain=2, offset=0):
        """Фонари на цепях под потолком по сетке (там, где высоко и свободно)."""
        for (x, y, z), t in list(self.open.items()):
            if themes and t not in themes:
                continue
            if (x + offset) % spacing or (z + offset) % spacing:
                continue
            if (x, y - 1, z) in self.open:
                continue
            top = self.ceil_y(x, y, z)
            if top - y + 1 < min_h or (x, top + 1, z) in self.nowall:
                continue
            ln = min(chain, max(0, top - y - 4))
            if ln > 0:
                self.chain_lantern(x, top, z, ln, soul)
            else:
                self.lantern(x, top, z, soul, hanging=True)

    def sign(self, x, y, z, facing, wood='dark_oak'):
        """Табличка на стене (текст ставит игра из схемы — в map.bin только блок)."""
        self.set(x, y, z, f'{wood}_wall_sign', facing=facing, waterlogged=False)

    # ---------------- выгрузка ----------------

    def sections(self):
        """{(sx, sy, sz): {(lx, ly, lz): state}} только с не-воздухом."""
        out = {}
        for (x, y, z), s in self.blocks.items():
            if s == AIR:
                continue
            out.setdefault((x >> 4, y >> 4, z >> 4), {})[(x & 15, y & 15, z & 15)] = s
        return out


def _varint(n):
    out = bytearray()
    while True:
        b = n & 0x7F
        n >>= 7
        if n:
            out.append(b | 0x80)
        else:
            out.append(b)
            return bytes(out)


def write_map(maps, path):
    """Записать несколько карт (преддверие и этапы) в один map.bin. Блоки карт не должны пересекаться."""
    palette = [AIR]
    index = {AIR: 0}
    secs = {}
    for m in maps:
        for k, sec in m.sections().items():
            if k in secs:
                secs[k].update(sec)
            else:
                secs[k] = dict(sec)
    body = bytearray()
    for (sx, sy, sz) in sorted(secs):
        sec = secs[(sx, sy, sz)]
        body += struct.pack('>iii', sx, sy, sz)
        run_val, run_len = -1, 0
        for i in range(4096):
            x, z, y = i & 15, (i >> 4) & 15, i >> 8
            s = sec.get((x, y, z), AIR)
            v = index.get(s)
            if v is None:
                v = index[s] = len(palette)
                palette.append(s)
            if v == run_val:
                run_len += 1
            else:
                if run_len:
                    body += _varint(run_len) + _varint(run_val)
                run_val, run_len = v, 1
        body += _varint(run_len) + _varint(run_val)
    head = bytearray(b'LRPGMAP1')
    head += struct.pack('>i', len(palette))
    for s in palette:
        b = s.encode('utf-8')
        head += struct.pack('>H', len(b)) + b
    head += struct.pack('>i', len(secs))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with gzip.GzipFile(path, 'wb', 9, mtime=0) as f:
        f.write(bytes(head) + bytes(body))
    return len(palette), len(secs)


# ---------------- схема этапа ----------------

class Stage:
    """Схема этапа: привал, отряды, хранители, преграды, разлом. Уходит в layout.json."""

    def __init__(self, n, key, name, levels, biome):
        self.n, self.key, self.name, self.levels, self.biome = n, key, name, list(levels), biome
        self.box = None
        self.camp = None
        self.trader = None
        self.rift = None
        self.rift_when = []
        self.packs, self.bosses, self.gates = [], [], []
        self.mechs, self.caches, self.lore, self.signs = [], [], [], []

    def pack(self, pid, mobs, weight=1.0, title=None):
        """mobs: [(вид, x, y, z, ур.[, ранг[, мин. отряд]])]. Центр — среднее точек."""
        out = []
        for m in mobs:
            sp, x, y, z, lv = m[:5]
            o = {'species': sp, 'at': [x, y, z], 'level': lv}
            if len(m) > 5 and m[5]:
                o['rank'] = m[5]
            if len(m) > 6 and m[6]:
                o['min_party'] = m[6]
            out.append(o)
        cx = sum(o['at'][0] for o in out) / len(out)
        cy = min(o['at'][1] for o in out)
        cz = sum(o['at'][2] for o in out) / len(out)
        p = {'id': f's{self.n}_{pid}', 'at': [round(cx, 1), cy, round(cz, 1)], 'weight': weight, 'mobs': out}
        if title:
            p['title'] = title
            p['weight'] = max(weight, 3.0)
        self.packs.append(p)
        return p['id']

    def boss(self, bid, btype, at, arena, final=False, arena2=None):
        """arena2 — (x, y, z, r) второго зала: на половине здоровья хранитель переносит бой туда."""
        b = {'id': f's{self.n}_{bid}', 'type': btype, 'at': list(at), 'arena': arena, 'final': final}
        if arena2:
            b['arena2'] = [float(v) for v in arena2]
        self.bosses.append(b)
        return b['id']

    def gate(self, gid, box, open_when=(), lock=None):
        """Преграда-печать. open_when: ['boss:id', 'pack:id'] — открывается, когда всё выполнено.
        lock — id хранителя: открытая преграда закрывается на время боя с ним."""
        g = {'id': f's{self.n}_{gid}', 'box': list(box), 'open_when': list(open_when)}
        if lock:
            g['lock'] = lock
        self.gates.append(g)
        return g['id']

    def mech(self, mid, mtype, points=(), area=None, answers=(), text='', **num):
        """Механизм: plates / levers / sequence / memory / riddle / hazard (см. Mechanisms.java)."""
        m = {'id': f's{self.n}_{mid}', 'type': mtype, 'points': [list(p) for p in points]}
        if area:
            m['area'] = list(area)
        if answers:
            m['answers'] = list(answers)
        if text:
            m['text'] = text
        if num:
            m['num'] = {k: float(v) for k, v in num.items()}
        self.mechs.append(m)
        return m['id']

    def cache(self, cid, at, tier):
        self.caches.append({'id': f's{self.n}_{cid}', 'at': list(at), 'tier': tier})

    def say(self, box, text):
        """Голос Системы: запись в чат, когда отряд впервые входит в область."""
        self.lore.append({'box': list(box), 'text': text})

    def sign(self, at, lines):
        self.signs.append({'at': list(at), 'lines': list(lines)})

    def json(self):
        return {'n': self.n, 'key': self.key, 'name': self.name, 'levels': self.levels, 'biome': self.biome,
                'box': list(self.box), 'camp': list(self.camp), 'trader': list(self.trader), 'rift': list(self.rift),
                'rift_when': self.rift_when or [f'boss:{b["id"]}' for b in self.bosses],
                'packs': self.packs, 'bosses': self.bosses, 'gates': self.gates, 'mechs': self.mechs,
                'caches': self.caches, 'lore': self.lore, 'signs': self.signs}


def apply_gates(m, stage, block='litrpg:sealed_gate'):
    """Преграды в исходном виде: с условием открытия — закрыты блоком печати, «замки» боя — открыты."""
    for g in stage.gates:
        x1, y1, z1, x2, y2, z2 = g['box']
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    if g['open_when']:
                        m.set(x, y, z, block)
                    else:
                        m.blocks.pop((x, y, z), None)


def write_layout(lobby, stages, path):
    data = {'version': 1, 'lobby': lobby, 'stages': [s.json() for s in stages]}
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
        f.write('\n')


# ---------------- превью ----------------

COLORS = [('lava', (230, 100, 20)), ('water', (50, 100, 200)), ('cobweb', (230, 230, 230)), ('moss', (80, 130, 60)),
          ('leaves', (60, 130, 50)), ('grass', (90, 150, 70)), ('lantern', (255, 220, 120)), ('torch', (255, 200, 90)),
          ('candle', (255, 230, 150)), ('froglight', (250, 240, 200)), ('glowstone', (250, 220, 140)),
          ('sea_lantern', (200, 240, 240)), ('shroomlight', (250, 160, 80)), ('campfire', (255, 140, 40)),
          ('gold', (240, 200, 60)), ('amethyst', (160, 100, 220)), ('prismarine', (60, 140, 140)), ('bone', (225, 220, 200)),
          ('skull', (220, 220, 210)), ('iron_bars', (160, 160, 170)), ('chain', (120, 120, 130)), ('deepslate', (70, 70, 80)),
          ('blackstone', (45, 40, 50)), ('basalt', (80, 80, 90)), ('nether', (100, 30, 35)), ('crimson', (140, 40, 60)),
          ('warped', (40, 120, 110)), ('soul', (90, 70, 50)), ('mud', (90, 80, 70)), ('tuff', (100, 100, 90)),
          ('dark_oak', (70, 50, 30)), ('spruce', (110, 80, 50)), ('oak', (160, 130, 80)), ('stone_brick', (125, 125, 125)),
          ('cobble', (115, 115, 115)), ('stone', (130, 130, 130)), ('andesite', (140, 140, 140)), ('sand', (220, 210, 160)),
          ('dirt', (130, 100, 70)), ('quartz', (235, 230, 225)), ('purpur', (170, 120, 170)), ('end_stone', (220, 220, 160)),
          ('obsidian', (30, 20, 40)), ('magma', (160, 60, 20)), ('ice', (160, 200, 250)), ('rift', (220, 60, 220)),
          ('sealed_gate', (190, 90, 255)), ('seal', (255, 210, 90)), ('cache', (255, 200, 40)), ('carpet', (150, 40, 40)), ('wool', (150, 40, 40))]


def color_of(s):
    n = base(s)
    for k, c in COLORS:
        if k in n:
            return c
    return (150, 150, 150)


def preview(m, path, box, marks=(), scale=2):
    """Вид сверху на коробку box (x1, z1, x2, z2). marks — [(x, z, (r, g, b))] точки схемы."""
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))
    from png import Canvas
    x1, z1, x2, z2 = box
    w, h = x2 - x1 + 1, z2 - z1 + 1
    floor = {}
    for (x, y, z) in m.open:
        if x1 <= x <= x2 and z1 <= z <= z2 and m.is_air(x, y, z) and m.solid(x, y - 1, z):
            k = (x, z)
            if k not in floor or floor[k] < y:
                floor[k] = y
    c = Canvas(w * scale, h * scale, (10, 10, 14, 255))
    ys = list(floor.values()) or [64]
    lo, hi = min(ys), max(ys) + 1
    for (x, z), y in floor.items():
        b = m.blocks.get((x, y - 1, z), 'minecraft:stone')
        top = m.blocks.get((x, y, z))
        col = color_of(top) if top and top != AIR and base(top) not in ('light',) else color_of(b)
        k = 0.55 + 0.45 * (y - lo) / max(1, hi - lo)
        rgb = tuple(int(v * k) for v in col)
        for i in range(scale):
            for j in range(scale):
                c.set((x - x1) * scale + i, (z - z1) * scale + j, rgb)
    for (x, z, rgb) in marks:
        for i in range(-2, 3):
            for j in range(-2, 3):
                c.set(int((x - x1) * scale) + i, int((z - z1) * scale) + j, rgb)
    c.save(path)
