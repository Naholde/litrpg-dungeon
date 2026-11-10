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
            'hanging_roots', 'spore_blossom', 'small_dripleaf', 'big_dripleaf_stem', 'rift', 'bubble_column')
# Похожи на проходимые по имени, но твёрдые.
SOLID_EXCEPT = ('sea_lantern', 'redstone_lamp', 'jack_o_lantern', 'mushroom_block', 'brown_mushroom_block',
                'red_mushroom_block', 'mushroom_stem', 'grass_block', 'chain_command_block', 'snow_block',
                'mangrove_roots', 'muddy_mangrove_roots', 'flowering_azalea', 'flower_pot', 'powder_snow',
                'divine_cache')


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


def tall(s):
    """Забор, стенка, калитка — 1,5 блока: на них не шагнуть и не запрыгнуть, это перила."""
    if s is None:
        return False
    b = base(s)
    return _joinable(b) in ('fence', 'nether', 'wall') or b.endswith('fence_gate')


def standable(s):
    """На блоке можно стоять (полный блок, плита, ступени, сундук…), но не на жидкости и не на разломе."""
    if s is None or passable(s):
        return False
    b = base(s)
    return 'lava' not in b and 'magma' not in b


# ---------------- вертикаль: общие правила (их же использует check.py) ----------------
WATER = 'minecraft:water[level=0]'
DIRV = {'east': (1, 0), 'west': (-1, 0), 'south': (0, 1), 'north': (0, -1)}
OPPOSITE = {'east': 'west', 'west': 'east', 'south': 'north', 'north': 'south'}
# Прыжок с разбега: подъём dy → наибольший разрыв (пустых клеток между краями). Выше чем на 1 не запрыгнуть.
JUMPS = {1: 2, 0: 3, -1: 3, -2: 3, -3: 3}
# Падение на твёрдое без серьёзного урона (урон с 4-го блока: 1–2 единицы из 20, то есть 5–10% здоровья).
# В воду и на лестницу/лозу — с любой высоты.
MAX_DROP = 5


def climbable(s):
    """По блоку лезут вверх и вниз (лестница, лозы, строительные леса)."""
    if s is None:
        return False
    b = base(s)
    return b in ('ladder', 'vine', 'scaffolding') or b.startswith(('twisting_vines', 'weeping_vines', 'cave_vines'))


def is_water(s):
    return s is not None and base(s) in ('water', 'bubble_column')


def current(s):
    """Течение пузырей: +1 — несёт вверх (песок душ), -1 — тянет вниз (магма), 0 — нет."""
    if s is None or base(s) != 'bubble_column':
        return 0
    return -1 if 'drag=true' in s else 1


def jump_ok(a, b):
    """Можно ли перейти или перепрыгнуть с клетки ног a на клетку ног b (по прямой вдоль оси)."""
    dx, dy, dz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
    if dx and dz:
        return False
    dist = abs(dx) + abs(dz)
    if dist == 0:
        return False
    if dist == 1:
        return dy <= 1
    return dy in JUMPS and dist - 1 <= JUMPS[dy]


def _face(dx, dz):
    for k, v in DIRV.items():
        if v == (dx, dz):
            return k
    raise ValueError((dx, dz))


def _axis(block, axis='y'):
    """Свойство оси — только у столбовых блоков (брёвна, колонны, базальт)."""
    b = base(state(block)) if block else ''
    if b.endswith('mushroom_stem'):
        return {}                                   # у ножки гриба нет оси (в отличие от багровой/искажённой)
    if block and b.endswith(('log', 'pillar', 'basalt', '_wood', '_stem', 'hyphae', 'bone_block', 'chain')):
        return {'axis': axis}
    return {}


def _joinable(b):
    if 'fence' in b and 'gate' not in b:
        return 'nether' if b.startswith('nether') else 'fence'
    if b == 'iron_bars' or b.endswith('glass_pane'):
        return 'pane'
    if b.endswith('_wall') and not any(k in b for k in ('torch', 'sign', 'banner', 'head', 'skull', 'fan')):
        return 'wall'
    return None


def _slab_of(block):
    """Плита того же материала: spruce_planks → spruce_slab, stone_bricks → stone_brick_slab; None — не знаем."""
    b = block.split(':')[-1].split('[')[0]
    for end, rep in (('_planks', '_slab'), ('bricks', 'brick_slab'), ('tiles', 'tile_slab')):
        if b.endswith(end):
            return b[: -len(end)] + rep
    if b in ('stone', 'cobblestone', 'sandstone', 'blackstone', 'polished_blackstone', 'polished_andesite',
             'polished_diorite', 'polished_granite', 'polished_deepslate', 'cobbled_deepslate', 'purpur_block',
             'smooth_stone', 'prismarine', 'dark_prismarine', 'cut_copper', 'tuff', 'polished_tuff'):
        return b.replace('_block', '') + '_slab'
    return None


def _full(b):
    return not any(k in b for k in ('slab', 'stairs', 'fence', 'wall', 'pane', 'bars', 'door', 'lantern', 'chain',
                                    'carpet', 'button', 'plate', 'candle', 'torch', 'ladder', 'vine', 'glass'))


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
        self.keep = set()      # столбцы (x, z) построек вертикали — отделка их не трогает
        self._post = []        # постройки, ждущие оболочки
        self._shelled = False

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
        self._shelled = True
        posts, self._post = self._post, []
        for fn in posts:
            fn()

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
            if (x, z) in self.keep:
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
            if (x, z) in self.keep:
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

    # ---------------- вертикаль (обновление 14) ----------------
    # Всё, что ставит блоки поверх стен (лестницы, мосты, вода), откладывается до оболочки (`later`) — пустоты
    # вырезаются сразу. Клетки построек попадают в `keep`: отделка (dress) и фонари их не трогают.

    def later(self, fn):
        """Выполнить постройку после оболочки (или сразу, если оболочка уже стоит)."""
        if self._shelled:
            fn()
        else:
            self._post.append(fn)

    def reserve(self, x1, z1, x2, z2, pad=1):
        """Столбцы (x, z), где отделке нечего делать."""
        for x in range(min(x1, x2) - pad, max(x1, x2) + pad + 1):
            for z in range(min(z1, z2) - pad, max(z1, z2) + pad + 1):
                self.keep.add((x, z))

    def material(self, x, y, z, part='wall'):
        """Блок темы ближайшей пустоты — чем достроить опору, чтобы она не выделялась."""
        for r in (1, 2, 3):
            for dx in range(-r, r + 1):
                for dy in range(-r, r + 1):
                    for dz in range(-r, r + 1):
                        t = self.open.get((x + dx, y + dy, z + dz))
                        if t:
                            th = self.themes[t]
                            return Theme.pick(getattr(th, part), x, y, z, th.seed)
        return state('stone_bricks')

    def _support(self, x, y, z, block=None):
        if not self.solid(x, y, z):
            self.blocks[(x, y, z)] = state(block) if block else self.material(x, y, z)

    def shaft(self, x1, z1, x2, z2, y1, y2, theme, round_=False):
        """Вертикальная шахта: пустота от уровня ног y1 до y2 включительно (round_ — круглая, по вписанному кругу)."""
        if round_:
            cx, cz = (x1 + x2) / 2, (z1 + z2) / 2
            self.round_room(cx, cz, min(x2 - x1, z2 - z1) / 2 + 0.3, y1, y2 - y1 + 1, theme)
        else:
            self.carve(x1, y1, z1, x2, y2, z2, theme)

    def ladder(self, x, z, y1, y2, facing, support=None, theme=None, width=1):
        """Приставная лестница в клетке (x, z) от y1 до y2; facing — куда она смотрит (от стены к игроку).
        Наверху лестница кончается на блок ниже пола выхода: с последней перекладины шагают вперёд-вверх.
        Стена за лестницей достраивается (support или материал темы), если там пусто.
        width — ширина вдоль стены (от (x, z) в сторону +x или +z). Шахта шире одной клетки должна быть вся
        в лестницах или лозах: с разбега игрок пролетает мимо узкой лестницы в пустоту (`check.drops`)."""
        dx, dz = DIRV[facing]
        cols = [(x + i * abs(dz), z + i * abs(dx)) for i in range(width)]
        for cx, cz in cols:
            if theme:
                self.carve(cx, y1, cz, cx, y2 + 1, cz, theme)
            self.reserve(cx, cz, cx, cz)

        def build():
            for cx, cz in cols:
                for y in range(y1, y2 + 1):
                    self._support(cx - dx, y, cz - dz, support)
                    self.set(cx, y, cz, 'ladder', facing=facing, waterlogged=False)
        self.later(build)

    def vines(self, x, z, y1, y2, kind='vine', facing=None, berries=0.35, support=None, theme=None, to=None):
        """Лоза, по которой лезут: kind='vine' — по стене (facing — от стены), 'cave' — светящиеся ягоды
        свисают с потолка, 'weeping' — плачущие (с потолка), 'twisting' — вьющиеся (растут от пола).
        Опора (стена, потолок или пол) достраивается. Растения в возрасте 25 — сами не растут.
        to=(x2, z2) — заполнить прямоугольник x..x2, z..z2: шахта шире клетки должна быть в лозе вся, иначе
        с разбега игрок пролетает мимо лозы в соседнюю пустоту и разбивается (`check.drops`). У лозы
        по стене прямоугольник — линия вдоль стены."""
        if to is not None:
            x2, z2 = to
            if kind == 'vine' and ((x2 != x and DIRV[facing][0]) or (z2 != z and DIRV[facing][1])):
                raise ValueError('лоза по стене: to — только вдоль стены')
            for xx in range(min(x, x2), max(x, x2) + 1):
                for zz in range(min(z, z2), max(z, z2) + 1):
                    self.vines(xx, zz, y1, y2, kind, facing, berries, support, theme)
            return
        if theme:
            self.carve(x, y1, z, x, y2 + 1, z, theme)
        self.reserve(x, z, x, z)

        def build():
            if kind == 'vine':
                dx, dz = DIRV[facing]
                face = OPPOSITE[facing]
                for y in range(y1, y2 + 1):
                    self._support(x - dx, y, z - dz, support)
                    self.set(x, y, z, 'vine', **{d: d == face for d in ('east', 'north', 'south', 'west')}, up=False)
            elif kind in ('cave', 'weeping'):
                self._support(x, y2 + 1, z, support)
                for y in range(y1, y2 + 1):
                    tip = y == y1
                    if kind == 'cave':
                        b = h3(x, y, z, 77) < berries
                        if tip:
                            self.set(x, y, z, 'cave_vines', age=25, berries=b)
                        else:
                            self.set(x, y, z, 'cave_vines_plant', berries=b)
                    else:
                        if tip:
                            self.set(x, y, z, 'weeping_vines', age=25)
                        else:
                            self.set(x, y, z, 'weeping_vines_plant')
            elif kind == 'twisting':
                self._support(x, y1 - 1, z, support)
                for y in range(y1, y2 + 1):
                    if y == y2:
                        self.set(x, y, z, 'twisting_vines', age=25)
                    else:
                        self.set(x, y, z, 'twisting_vines_plant')
            else:
                raise ValueError(f'лоза: неизвестный вид {kind}')
        self.later(build)

    def spiral(self, cx, cz, y1, y2, r, theme, step='stone_bricks', core='polished_andesite', r_core=1.2,
               per_turn=12, cw=False, start=0.0, rail=None, carve=True, underside=None, guard=None):
        """Винтовая лестница вокруг столба (центр cx, cz — середина клетки: целое + 0.5).
        Ступени — полные блоки: на плитах проверка проходимости врёт на полблока. За оборот подъём per_turn блоков
        (≥ 5: над головой — ступень следующего витка). Начало — у пола y1 под углом start (рад, 0 — на восток),
        верхняя ступень — на y2-1 (ноги на y2). rail — перила по внешнему краю (если r ≥ 4).
        underside — блок «изнанки» (ступени вверх ногами под каждой ступенью) для красоты снизу.
        guard — бортик вокруг открытого колодца на верхней площадке (по умолчанию rail или каменная стенка).
        Возвращает {'top': (x, z) — клетка верхней ступени у внешнего края, 'angle': угол выхода}."""
        assert per_turn >= (6 if underside else 5), 'над головой должно быть место'
        rr = r + 0.5
        cells = []
        for x in range(int(cx - rr) - 1, int(cx + rr) + 2):
            for z in range(int(cz - rr) - 1, int(cz + rr) + 2):
                d = math.hypot(x + 0.5 - cx, z + 0.5 - cz)
                if d <= rr:
                    cells.append((x, z, d))
        if carve:
            # Вырез — ровно по клеткам ступеней. round_room меряет от угла клетки, а ступени — от середины:
            # по краю оставались щели без ступеней на всю высоту башни (падение до 24 блоков).
            for x, z, _ in cells:
                for y in range(y1, y2 + 4):
                    self.open[(x, y, z)] = theme.name
        self.reserve(int(cx - rr), int(cz - rr), int(cx + rr), int(cz + rr), pad=0)
        sign = -1 if cw else 1
        turns = (y2 - y1) / per_turn
        guard = guard or rail or 'stone_brick_wall'
        end = start + sign * turns * 2 * math.pi

        def frac(x, z):
            a = math.atan2(z + 0.5 - cz, x + 0.5 - cx)
            return ((sign * (a - start)) % (2 * math.pi)) / (2 * math.pi)

        def build():
            for x, z, d in cells:
                if d <= r_core:
                    for y in range(y1 - 1, y2 + 4):
                        if (x, y, z) in self.open:
                            self.set(x, y, z, core)
                    continue
                f = frac(x, z)
                t = 0
                while True:
                    h = y1 - 1 + int((t + f) * per_turn)
                    t += 1
                    if h < y1:
                        continue
                    if h > y2 - 1:
                        break
                    self.set(x, h, z, step)
                    if underside and (x, h - 1, z) in self.open and not self.solid(x, h - 1, z):
                        # ступень вверх ногами, «смотрит» на столб
                        ddx, ddz = cx - (x + 0.5), cz - (z + 0.5)
                        fc = ('east' if ddx > 0 else 'west') if abs(ddx) >= abs(ddz) else ('south' if ddz > 0 else 'north')
                        self.stairs(x, h - 1, z, underside, fc, half='top')
                    if rail and r >= 4 and d > r - 0.5 and y1 + 1 < h < y2 - 2:   # у входа и выхода — без перил
                        self.set(x, h + 1, z, rail)
            # Наверху колодец открыт: шаг с верхней площадки мимо ступеней — падение на виток ниже.
            # Бортик на уровне ног выхода над клетками, чья верхняя ступень ниже на MAX_DROP и больше.
            guarded = []
            for x, z, d in cells:
                if d <= r_core:
                    continue
                f = frac(x, z)
                tops = [y1 - 1 + int((t + f) * per_turn) for t in range(int(turns) + 3)]
                top = max([h for h in tops if y1 <= h <= y2 - 1], default=y1 - 1)
                if y2 - 1 - top > MAX_DROP and not self.solid(x, y2, z):
                    self.set(x, y2, z, guard)
                    guarded.append((x, y2, z))
            if rail or guarded:
                self.connect([(x, y, z) for x, z, d in cells for y in range(y1, y2 + 1)])
        self.later(build)
        tx = int(math.floor(cx + math.cos(end) * (r - 0.5)))
        tz = int(math.floor(cz + math.sin(end) * (r - 0.5)))
        return {'top': (tx, tz), 'angle': end}

    def bridge(self, a, b, y, deck, width=3, rail=None, posts=None, post_every=6, lanterns=True, sag=0, gaps=(),
               theme=None, under=None, slab=None, tall_posts=False):
        """Мост по прямой a → b ((x, z), концы включительно) на уровне ног y: настил на y-1, перила rail по краям,
        столбы posts (с фонарём) через post_every клеток, провис sag (середина ниже на sag блоков, перепад ≤ 1;
        на нижней клетке каждого перепада — плита slab (по умолчанию из настила): целый блок шагом не взять,
        а пол-блока — да; перила там на блок выше, чтобы с плиты их не перепрыгнуть),
        gaps — номера клеток вдоль моста без настила (обрушенный мост: разрыв ≤ 3 — перепрыгнуть).
        tall_posts — столбы на блок выше (фонарь на два блока над настилом): на низкий столб с фонарём запрыгивают
        с настила и оттуда шагают через перила — над обрывом (не над бездной) так нельзя.
        theme — вырезать над мостом пустоту (если моста ещё нет в пустоте). under — балки снизу (каждая 3-я клетка)."""
        (ax, az), (bx, bz) = a, b
        n = max(abs(bx - ax), abs(bz - az), 1)
        px, pz = (0, 1) if abs(bx - ax) >= abs(bz - az) else (1, 0)
        lo = -(width // 2)
        hi = width - width // 2 - 1
        path = []
        for i in range(n + 1):
            t = i / n
            x, z = round(ax + (bx - ax) * t), round(az + (bz - az) * t)
            yy = y - int(round(sag * math.sin(math.pi * t)))
            path.append((i, x, z, yy))
        for (_, _, _, y0), (_, _, _, y1_) in zip(path, path[1:]):
            assert abs(y1_ - y0) <= 1, 'провис слишком крутой для такой длины'
        run, longest = 0, 0
        for i, *_ in path:
            run = run + 1 if i in gaps else 0
            longest = max(longest, run)
        assert longest <= 3, 'разрыв больше трёх клеток не перепрыгнуть'
        for i, x, z, yy in path:
            if theme:
                for k in range(lo - 1, hi + 2):
                    for dy in range(3):
                        self.open.setdefault((x + px * k, yy + dy, z + pz * k), theme.name)
            self.keep.add((x, z))
            for k in range(lo - 2, hi + 3):
                self.keep.add((x + px * k, z + pz * k))

        slab_ = slab or _slab_of(deck)
        ys = {i: yy for i, _, _, yy in path}
        steps = {i for i, *_ in path if slab_ and i not in gaps
                 and any(ys.get(j, ys[i]) == ys[i] + 1 and j not in gaps for j in (i - 1, i + 1))}

        def build():
            rails = []
            for i, x, z, yy in path:
                if i in gaps:
                    continue
                for k in range(lo, hi + 1):
                    self.set(x + px * k, yy - 1, z + pz * k, deck)
                    if i in steps:
                        self.slab(x + px * k, yy, z + pz * k, slab_)
                if under and i % 3 == 0:
                    for k in range(lo, hi + 1):
                        if not self.solid(x + px * k, yy - 2, z + pz * k):
                            self.set(x + px * k, yy - 2, z + pz * k, under, **_axis(under, 'z' if px else 'x'))
                for k in (lo - 1, hi + 1):
                    sx, sz = x + px * k, z + pz * k
                    post = posts and post_every and i % post_every == 0
                    if post:
                        self.set(sx, yy - 1, sz, posts, **_axis(posts))
                        self.set(sx, yy, sz, posts, **_axis(posts))
                        if tall_posts:
                            self.set(sx, yy + 1, sz, posts, **_axis(posts))
                        if lanterns:
                            self.lantern(sx, yy + (2 if tall_posts else 1), sz, soul=False)
                    elif rail:
                        self.set(sx, yy - 1, sz, deck)
                        self.set(sx, yy, sz, rail)
                        rails.append((sx, yy, sz))
                        if i in steps:
                            self.set(sx, yy + 1, sz, rail)
                            rails.append((sx, yy + 1, sz))
            self.connect(rails)
        self.later(build)
        return path

    def ledge(self, points, y, block, width=2, corbel=None, theme=None):
        """Уступ вдоль стены по ломаной [(x, z), …] на уровне ног y. corbel — ступени вверх ногами под открытым краем."""
        for a, b in zip(points, points[1:]):
            path = self.bridge(a, b, y, block, width=width, theme=theme)
            if not corbel:
                continue
            (ax, az), (bx, bz) = a, b
            px, pz = (0, 1) if abs(bx - ax) >= abs(bz - az) else (1, 0)
            lo, hi = -(width // 2), width - width // 2 - 1

            def build(path=path, px=px, pz=pz, lo=lo, hi=hi):
                for i, x, z, yy in path:
                    if i % 2:
                        continue
                    for k, sgn in ((lo, -1), (hi, 1)):
                        if self.solid(x + px * (k + sgn), yy - 1, z + pz * (k + sgn)):
                            continue  # этот край у стены
                        cx_, cz_ = x + px * k, z + pz * k
                        if not self.solid(cx_, yy - 2, cz_):
                            self.stairs(cx_, yy - 2, cz_, corbel, _face(-sgn * px, -sgn * pz), half='top')
            self.later(build)

    def platform(self, cx, cz, y, r, deck, rim=None, support=None, support_block=None, chains=False, round_=True,
                 theme=None, rail=None, gaps=()):
        """Площадка на уровне ног y (центр cx, cz; радиус r): настил на y-1, rim — кайма другим блоком.
        support='pillar' — столб под серединой до опоры, chains — цепи с краёв к потолку.
        rail — перила по краю (площадка выше MAX_DROP над полом без них не проходит проверку),
        gaps — клетки края (x, z), где перил нет: вход с лестницы или лозы."""
        cells = []
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            for z in range(int(cz - r) - 1, int(cz + r) + 2):
                d = math.hypot(x + 0.5 - cx, z + 0.5 - cz) if round_ else max(abs(x + 0.5 - cx), abs(z + 0.5 - cz))
                if d <= r:
                    cells.append((x, z, d))
        if theme:
            for x, z, _ in cells:
                for dy in range(3):
                    self.open.setdefault((x, y + dy, z), theme.name)
        self.reserve(int(cx - r), int(cz - r), int(cx + r), int(cz + r))

        def build():
            for x, z, d in cells:
                self.set(x, y - 1, z, rim if rim and d > r - 1 else deck)
            if support == 'pillar':
                pr = max(0.8, r * 0.3)
                blk = support_block or deck
                for x, z, d in cells:
                    if d > pr:
                        continue
                    for yy in range(y - 2, y - 62, -1):
                        if self.solid(x, yy, z) or (x, yy, z) not in self.open:
                            break
                        self.set(x, yy, z, blk, **_axis(blk))
            if chains:
                for x, z, d in cells:
                    if d <= r - 1 or d > r or (x + z) % 3:
                        continue
                    yy = y
                    while (x, yy, z) in self.open and not self.solid(x, yy, z) and yy - y < 40:
                        yy += 1
                    if yy - y < 40:
                        for k in range(y + 2, yy):
                            self.set(x, k, z, 'chain', axis='y', waterlogged=False)
            if rail:
                have = {(x, z) for x, z, _ in cells}
                edge = [(x, y, z) for x, z, _ in cells if (x, z) not in gaps
                        and any((x + dx, z + dz) not in have for dx, dz in DIRV.values())]
                for c in edge:
                    if not self.solid(*c):
                        self.set(*c, rail)
                self.connect(edge)
        self.later(build)

    def sleeve(self, x1, z1, x2, z2, y1, y2, block):
        """Рукав: каменная труба вокруг шахты x1..x2, z1..z2 на высотах y1..y2 (кольцо клеток вокруг, только где пусто).
        Нужна, когда лестница или лоза спускается сквозь потолок зала: без рукава с неё можно соскочить вбок
        выше MAX_DROP. Низ рукава — не выше MAX_DROP над полом зала."""
        ring = [(x, z) for x in range(x1 - 1, x2 + 2) for z in range(z1 - 1, z2 + 2)
                if not (x1 <= x <= x2 and z1 <= z <= z2)]

        def build():
            for x, z in ring:
                for y in range(y1, y2 + 1):
                    if not self.solid(x, y, z):
                        self.set(x, y, z, block, **_axis(block))
        self.later(build)

    def railing(self, cells, block):
        """Бортик: block (стенка, забор, решётка) в клетках (x, y, z), соединённый с соседями и стенами."""
        cells = list(cells)

        def build():
            for c in cells:
                if not self.solid(*c):
                    self.set(*c, block)
            self.connect(cells)
        self.later(build)

    def parkour(self, pads, block, pillar=None, theme=None, size=1):
        """Паркур: pads — [(x, y, z)] клетки, где стоят ноги; под каждой — блок block (size×size), под ним
        столб pillar до опоры. Соседние площадки проверяются правилами прыжка (`jump_ok`) — иначе ошибка сразу."""
        for a, b in zip(pads, pads[1:]):
            if not jump_ok(a, b):
                raise ValueError(f'паркур: с {a} на {b} не допрыгнуть')
        for x, y, z in pads:
            if theme:
                for dx in range(size):
                    for dz in range(size):
                        for dy in range(3):
                            self.open.setdefault((x + dx, y + dy, z + dz), theme.name)
            self.reserve(x, z, x + size - 1, z + size - 1)

        def build():
            for x, y, z in pads:
                for dx in range(size):
                    for dz in range(size):
                        self.set(x + dx, y - 1, z + dz, block)
                        if pillar:
                            for yy in range(y - 2, y - 60, -1):
                                if self.solid(x + dx, yy, z + dz) or (x + dx, yy, z + dz) not in self.open:
                                    break
                                self.set(x + dx, yy, z + dz, pillar, **_axis(pillar))
        self.later(build)

    def pool(self, x1, z1, x2, z2, y_surface, depth, theme):
        """Бассейн, утопленный в пол: вода от y_surface-depth+1 до y_surface (поверхность вровень с блоками пола —
        из воды выходят шагом на берег). Проверяется, что воде некуда течь."""
        y0 = y_surface - depth + 1
        self.carve(x1, y0, z1, x2, y_surface, z2, theme)
        cells = [(x, y, z) for x in range(min(x1, x2), max(x1, x2) + 1) for y in range(y0, y_surface + 1)
                 for z in range(min(z1, z2), max(z1, z2) + 1)]
        self.reserve(x1, z1, x2, z2)

        def build():
            for c in cells:
                self.blocks[c] = WATER
            self.check_water(cells)
        self.later(build)

    def dropper(self, x1, z1, x2, z2, y_top, y_bottom, theme, depth=3, pad=1):
        """Шахта-спуск («дропер»): дыра в полу верхнего зала (ноги y_top) — прыжок в бассейн нижнего зала
        (ноги y_bottom, поверхность воды y_bottom-1, глубина depth). Бассейн шире дыры на pad: при падении сносит.
        Обратно так не подняться — подъём строится отдельно."""
        assert y_top - y_bottom >= 4, 'это не шахта, а ступенька'
        self.carve(x1, y_bottom, z1, x2, y_top + 2, z2, theme)
        self.pool(x1 - pad, z1 - pad, x2 + pad, z2 + pad, y_bottom - 1, depth, theme)
        self.reserve(x1, z1, x2, z2, pad=pad + 1)

    def bubble_lift(self, x, z, y_floor, y_top, entry, exit_, theme, glass='glass', basin='prismarine_bricks'):
        """Пузырьковый подъём (песок душ под водой): в нижнем зале (ноги y_floor) с той стороны, куда смотрит entry,
        — бассейн глубиной 2; нырнуть, проплыть под стеклом в колонну 1×1 — вода несёт вверх до y_top
        (верхний блок воды). Выход — на пол y_top+1 со стороны exit_. Стекло — где колонна идёт сквозь зал.
        Вниз по колонне не спуститься (течение выталкивает)."""
        ex, ez = DIRV[entry]
        ox, oz = DIRV[exit_]
        yb = y_floor
        e1, e2 = (x + ex, z + ez), (x + 2 * ex, z + 2 * ez)
        self.carve(x, yb - 2, z, x, y_top + 2, z, theme)
        for (px, pz) in (e1, e2):
            self.carve(px, yb - 2, pz, px, yb - 1, pz, theme)
        self.carve(x + ox, y_top + 1, z + oz, x + ox, y_top + 2, z + oz, theme)
        self.reserve(min(x, e2[0]), min(z, e2[1]), max(x, e2[0]), max(z, e2[1]))
        self.reserve(x + ox, z + oz, x + ox, z + oz)

        def build():
            self.set(x, yb - 3, z, 'soul_sand')
            for px, pz in (e1, e2):
                self._support(px, yb - 3, pz, basin)
            water = []
            for y in range(yb - 2, y_top + 1):
                self.set(x, y, z, 'bubble_column', drag=False)
                water.append((x, y, z))
            for px, pz in (e1, e2):
                for y in (yb - 2, yb - 1):
                    self.blocks[(px, y, pz)] = WATER
                    water.append((px, y, pz))
            # стенки колонны: сквозь открытый зал — стекло, в толще — материал
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                for y in range(yb - 2, y_top + 1):
                    c = (x + dx, y, z + dz)
                    if c in water:
                        continue
                    if (x + dx, z + dz) == (x + ox, z + oz) and y == y_top:
                        self._support(*c, basin)
                        continue
                    if c in self.open and not self.solid(*c):
                        self.set(*c, glass if y >= yb else basin)
                    else:
                        self._support(*c, basin)
            # бортик бассейна: под водой у соседей — облицовка
            for px, pz in (e1, e2):
                for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    for y in (yb - 2, yb - 1):
                        c = (px + dx, y, pz + dz)
                        if c not in water:
                            self._support(*c, basin)
            self.set(e1[0], yb, e1[1], glass)
            self.check_water(water)
        self.later(build)

    def check_water(self, cells):
        """Вода стоит, только если ей некуда течь: по бокам и снизу — вода или твёрдое."""
        cs = set(cells)
        for (x, y, z) in cells:
            for c in ((x + 1, y, z), (x - 1, y, z), (x, y, z + 1), (x, y, z - 1), (x, y - 1, z)):
                if c in cs or is_water(self.blocks.get(c)):
                    continue
                if not self.solid(*c):
                    raise ValueError(f'вода в {x, y, z} утечёт в {c}')

    def connect(self, cells):
        """Соединить заборы, стены, решётки и стёкла-панели с соседями (генератор их форму не пересчитывает)."""
        for (x, y, z) in cells:
            s = self.blocks.get((x, y, z))
            if not s:
                continue
            b = base(s)
            kind = _joinable(b)
            if not kind:
                continue
            props = {}
            for d, (dx, dz) in DIRV.items():
                n = self.blocks.get((x + dx, y, z + dz))
                ok = n is not None and (_joinable(base(n)) == kind or (not passable(n) and _full(base(n))))
                if kind == 'wall':
                    props[d] = 'low' if ok else 'none'
                else:
                    props[d] = ok
            if kind == 'wall':
                ew = props['east'] != 'none' and props['west'] != 'none'
                ns = props['north'] != 'none' and props['south'] != 'none'
                one = sum(v != 'none' for v in props.values())
                props['up'] = not (one == 2 and (ew or ns))
            props['waterlogged'] = False
            self.blocks[(x, y, z)] = state(s.split('[')[0], **props)

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

    def climb_trap(self, mid, box, period=3.0, warn=1.0, share=0.15, layer=2, sound=0, offset=0.0):
        """Ловушка на подъёме (шахта с лестницей, лозой, пузырями): бьёт ярусами по layer блоков через один,
        по очереди — лезть в такт, пережидая вспышку яруса над головой (pattern 3 в Mechanisms.hazard)."""
        return self.mech(mid, 'hazard', area=box, period=period, warn=warn, share=share, pattern=3, stripe=layer,
                         sound=sound, offset=offset)

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
