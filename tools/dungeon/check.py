"""
Проверка карты: схема (виды, хранители, ссылки преград) и проходимость пешком.

Проходимость проверяется «как играет отряд»: от привала идём пешком, всё достижимое — побеждаем; открываются
преграды, чьи условия выполнены; повторяем, пока что-то меняется. В конце должны быть достижимы все отряды,
хранители и разлом. Запертые на время боя двери («lock») при проверке открыты.
"""
import os
import re
from collections import deque

from kit import passable, standable

JAVA = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'java', 'com', 'andrey', 'litrpg')


# Ломаются игроком (тег litrpg:breakable) — для проверки проходимости это проход.
BREAKABLE = {'minecraft:cobweb', 'minecraft:decorated_pot', 'minecraft:infested_cracked_stone_bricks',
             'minecraft:infested_mossy_stone_bricks', 'minecraft:infested_deepslate',
             'minecraft:infested_stone', 'minecraft:infested_cobblestone'}


def known_ids(path, pattern):
    ids = set()
    for root, _, files in os.walk(os.path.join(JAVA, path)):
        for f in files:
            if f.endswith('.java'):
                ids |= set(re.findall(pattern, open(os.path.join(root, f), encoding='utf-8').read()))
    return ids


def box_cells(b):
    x1, y1, z1, x2, y2, z2 = b
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for y in range(min(y1, y2), max(y1, y2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                yield x, y, z


class Walker:
    def __init__(self, m, opened):
        self.m = m
        self.opened = opened

    def air(self, x, y, z):
        if (x, y, z) in self.opened:
            return True
        s = self.m.blocks.get((x, y, z))
        if s and s.split('[')[0] in BREAKABLE:
            return True
        return passable(s) and not (s and 'lava' in s)

    def stand(self, x, y, z):
        return self.air(x, y, z) and self.air(x, y + 1, z) and standable(self.m.blocks.get((x, y - 1, z))) \
            and (x, y - 1, z) not in self.opened

    def drop(self, x, y, z, limit=6):
        """Куда приземлимся, шагнув в (x, y, z): ищем опору вниз."""
        for d in range(limit):
            if not self.air(x, y - d, z) or not self.air(x, y - d + 1, z):
                return None
            if self.stand(x, y - d, z):
                return x, y - d, z
        return None

    def reach(self, start, limit_box):
        x1, y1, z1, x2, y2, z2 = limit_box
        seen = {start}
        q = deque([start])
        while q:
            x, y, z = q.popleft()
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, nz = x + dx, z + dz
                if not (x1 <= nx <= x2 and z1 <= nz <= z2):
                    continue
                cand = None
                if self.stand(nx, y, nz):
                    cand = (nx, y, nz)
                elif self.stand(nx, y + 1, nz) and self.air(x, y + 2, z):
                    cand = (nx, y + 1, nz)
                else:
                    cand = self.drop(nx, y, nz)
                if cand and cand not in seen and y1 <= cand[1] <= y2:
                    seen.add(cand)
                    q.append(cand)
        return seen


def near(reached, pos, r=1, dy=2):
    x, y, z = (int(v) if not isinstance(v, float) else int(v // 1) for v in pos[:3])
    for ddx in range(-r, r + 1):
        for ddz in range(-r, r + 1):
            for ddy in range(-dy, dy + 1):
                if (x + ddx, y + ddy, z + ddz) in reached:
                    return True
    return False


def m_get(m, pt):
    return m.blocks.get(tuple(pt))


def check_stage(m, st, species, bosses, errors):
    m_ = m
    sid = f'этап {st["n"]}'
    ids = {f'pack:{p["id"]}' for p in st['packs']} | {f'boss:{b["id"]}' for b in st['bosses']}
    ids |= {f'mech:{m["id"]}' for m in st.get('mechs', []) if m['type'] != 'hazard'}
    for mc in st.get('mechs', []):
        if mc['type'] not in ('plates', 'levers', 'sequence', 'memory', 'riddle', 'hazard'):
            errors.append(f'{sid}: механизм {mc["id"]}: неизвестный тип {mc["type"]}')
        if mc['type'] in ('plates', 'levers', 'sequence', 'memory') and len(mc['points']) < 2:
            errors.append(f'{sid}: механизм {mc["id"]}: мало точек')
        if mc['type'] in ('riddle', 'hazard') and 'area' not in mc:
            errors.append(f'{sid}: механизм {mc["id"]}: нет области')
        if mc['type'] == 'riddle' and not mc.get('answers'):
            errors.append(f'{sid}: загадка {mc["id"]}: нет ответов')
        if mc['type'] == 'levers':
            for pt in mc['points']:
                if 'lever' not in (m_get(m_, pt) or ''):
                    errors.append(f'{sid}: {mc["id"]}: в точке {pt} нет рычага')
        if mc['type'] == 'plates':
            for pt in mc['points']:
                if 'pressure_plate' not in (m_get(m_, pt) or ''):
                    errors.append(f'{sid}: {mc["id"]}: в точке {pt} нет плиты')
    for c in st.get('caches', []):
        if 'cache' not in (m_get(m_, c['at']) or ''):
            errors.append(f'{sid}: тайник {c["id"]}: в {c["at"]} нет блока тайника')
    for p in st['packs']:
        for mob in p['mobs']:
            if mob['species'] not in species:
                errors.append(f'{sid}: неизвестный вид «{mob["species"]}» в {p["id"]}')
            x, y, z = mob['at']
            if not (m.is_air(x, y, z) and m.is_air(x, y + 1, z) and standable(m.get(x, y - 1, z))):
                errors.append(f'{sid}: {p["id"]}: монстру негде стоять в {mob["at"]}')
    for b in st['bosses']:
        if b['type'] not in bosses:
            errors.append(f'{sid}: неизвестный хранитель «{b["type"]}»')
    for g in st['gates']:
        for c in g['open_when']:
            if c not in ids:
                errors.append(f'{sid}: преграда {g["id"]} ждёт неизвестного «{c}»')
        if 'lock' in g and f'boss:{g["lock"]}' not in ids:
            errors.append(f'{sid}: преграда {g["id"]} запирается неизвестным хранителем')
    for c in st['rift_when']:
        if c not in ids:
            errors.append(f'{sid}: разлом ждёт неизвестного «{c}»')

    # проходимость с открытием преград
    done = set()
    opened = set()
    for g in st['gates']:
        if not g['open_when']:
            opened |= set(box_cells(g['box']))
    camp = tuple(int(v // 1) for v in st['camp'][:3])
    w = Walker(m, opened)
    if not w.stand(*camp):
        errors.append(f'{sid}: на привале негде стоять {camp}')
        return
    while True:
        reached = w.reach(camp, st['box'])
        before = len(done)
        for p in st['packs']:
            if all(near(reached, mob['at']) for mob in p['mobs']):
                done.add(f'pack:{p["id"]}')
        for b in st['bosses']:
            if near(reached, b['at'], r=3):
                done.add(f'boss:{b["id"]}')
        for mc in st.get('mechs', []):
            if mc['type'] == 'hazard':
                continue
            pts = mc['points'] or ([] if 'area' not in mc else [mc['area'][:3]])
            if mc['type'] == 'riddle':
                a = mc['area']
                pts = [[(a[0] + a[3]) // 2, a[1], (a[2] + a[5]) // 2]]
            if pts and all(near(reached, pt, r=2, dy=2) for pt in pts):
                done.add(f'mech:{mc["id"]}')
        for g in st['gates']:
            if g['open_when'] and all(c in done for c in g['open_when']) and f'gate:{g["id"]}' not in done:
                done.add(f'gate:{g["id"]}')
                opened |= set(box_cells(g['box']))
        if len(done) == before:
            break
    for c in sorted(ids - done):
        errors.append(f'{sid}: недостижимо пешком — {c}')
    rift_ok = all(c in done for c in st['rift_when'])
    x1, y1, z1, x2, y2, z2 = st['rift']
    if not rift_ok or not any(near(reached, (x, y, z), r=1, dy=1) for x, y, z in box_cells(st['rift'])):
        errors.append(f'{sid}: разлом недостижим')
    if not near(reached, st['trader']):
        errors.append(f'{sid}: торговец недостижим')
    for c in st.get('caches', []):
        if not near(reached, c['at'], r=2, dy=2):
            errors.append(f'{sid}: тайник {c["id"]} недостижим')
    for b in st['bosses']:
        if 'arena2' in b:
            x, y, z, r = b['arena2']
            if not Walker(m, opened).stand(int(x // 1), int(y // 1), int(z // 1)):
                errors.append(f'{sid}: второй зал {b["id"]}: в центре негде стоять')
    return len(reached)


def check(maps, layout):
    species = known_ids('mob', r'reg\("([a-z_]+)"')
    bosses = known_ids('boss', r'reg\("([a-z_]+)"')
    errors = []
    lobby_m = maps['lobby']
    sx, sy, sz = (int(v // 1) for v in layout['lobby']['spawn'][:3])
    w = Walker(lobby_m, set())
    if not w.stand(sx, sy, sz):
        errors.append('преддверие: на месте появления негде стоять')
    else:
        r = w.reach((sx, sy, sz), layout['lobby']['box'])
        if not near(r, layout['lobby']['seal'], r=2):
            errors.append('преддверие: до Печати не дойти')
    for st in layout['stages']:
        check_stage(maps[st['n']], st, species, bosses, errors)
    return errors
