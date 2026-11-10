"""
Проверка карты: схема (виды, хранители, ссылки преград) и проходимость пешком.

Проходимость проверяется «как играет отряд»: от привала идём (прыгаем, лезем, плывём — см. `Walker`), всё достижимое —
побеждаем; открываются преграды, чьи условия выполнены; повторяем, пока что-то меняется. В конце должны быть достижимы
все отряды, хранители и разлом. Запертые на время боя двери («lock») при проверке открыты.

Ещё ищутся западни (дошёл, а к разлому и привалу оттуда не выбраться) и долгие заплывы под водой. Для этапов,
построенных набором вертикали (`STRICT = True` в модуле этапа), это ошибки, для старых — предупреждения.
"""
import os
import re
from collections import deque

from kit import JUMPS, MAX_DROP, climbable, current, is_water, passable, standable, tall

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


STAND, CLIMB, SWIM, UP, DOWN = 1, 2, 4, 8, 16
DIRS4 = ((1, 0), (-1, 0), (0, 1), (0, -1))


class Walker:
    """Как ходит игрок: шагом (подъём на блок), прыжком через разрыв (`kit.JUMPS`), падением (на твёрдое — не выше
    `kit.MAX_DROP`, в воду и на лестницу — с любой высоты), по лестницам и лозам вверх-вниз, вплавь в любую сторону;
    пузырьки (песок душ) несут только вверх, магма — только вниз. Узел — клетка ног (над ней голова)."""

    def __init__(self, m, opened, floor_y=-64):
        self.m = m
        self.opened = opened
        self.floor_y = floor_y   # ниже — пропасть: Система вытащит, но это не путь
        self._kind = {}

    def block(self, x, y, z):
        if (x, y, z) in self.opened:
            return None
        return self.m.blocks.get((x, y, z))

    def air(self, x, y, z):
        if (x, y, z) in self.opened:
            return True
        s = self.m.blocks.get((x, y, z))
        if s and s.split('[')[0] in BREAKABLE:
            return True
        return passable(s) and not (s and 'lava' in s)

    def kind(self, x, y, z):
        """Чем является клетка ног: STAND/CLIMB/SWIM (+UP/DOWN — течение); 0 — здесь не удержаться."""
        key = (x, y, z)
        k = self._kind.get(key)
        if k is not None:
            return k
        k = 0
        if self.air(x, y, z) and self.air(x, y + 1, z):
            s = self.block(x, y, z)
            if climbable(s):
                k |= CLIMB
            if is_water(s):
                k |= SWIM
                c = current(s)
                k |= UP if c > 0 else DOWN if c < 0 else 0
            below = self.m.blocks.get((x, y - 1, z))
            if standable(below) and not tall(below) and (x, y - 1, z) not in self.opened:
                k |= STAND
        self._kind[key] = k
        return k

    def stand(self, x, y, z):
        return bool(self.kind(x, y, z) & STAND)

    def node(self, x, y, z):
        return self.kind(x, y, z) != 0

    def fall(self, x, y, z, y0):
        """Шагнули в пустоту (x, y, z) с высоты ног y0: где поймает пол, вода или лестница."""
        for yy in range(y, y - 96, -1):
            if yy < self.floor_y or not self.air(x, yy, z):
                return None
            k = self.kind(x, yy, z)
            if k & (SWIM | CLIMB):
                return x, yy, z
            if k & STAND:
                return (x, yy, z) if y0 - yy <= MAX_DROP else None
        return None

    def jumps(self, x, y, z, dx, dz):
        """Прыжок с разбега по прямой через разрыв 1–3 клетки (над разрывом и над головой — простор)."""
        out = []
        if not self.air(x, y + 2, z):
            return out
        for g in range(1, 4):
            cols = [(x + dx * i, z + dz * i) for i in range(1, g + 1)]
            if not all(self.air(cx, y + h, cz) for cx, cz in cols for h in (0, 1, 2)):
                break
            lx, lz = x + dx * (g + 1), z + dz * (g + 1)
            for dy, gmax in JUMPS.items():
                if g > gmax:
                    continue
                ly = y + dy
                if not self.stand(lx, ly, lz):
                    continue
                if dy < 0 and not all(self.air(cx, h, cz) for cx, cz in cols[-1:] for h in range(ly, y)):
                    continue
                out.append((lx, ly, lz))
        return out

    def moves(self, x, y, z):
        k = self.kind(x, y, z)
        out = []
        for dx, dz in DIRS4:
            nx, nz = x + dx, z + dz
            if self.node(nx, y, nz):
                out.append((nx, y, nz))
                continue
            if not k & DOWN and self.node(nx, y + 1, nz) and self.air(x, y + 2, z):
                out.append((nx, y + 1, nz))   # шаг на блок (из воды — на берег, с лестницы — на площадку)
                continue
            if self.air(nx, y, nz) and self.air(nx, y + 1, nz):
                f = self.fall(nx, y - 1, nz, y)
                if f:
                    out.append(f)
                if k & STAND:
                    out.extend(self.jumps(x, y, z, dx, dz))
        if k & CLIMB:
            if self.node(x, y + 1, z):
                out.append((x, y + 1, z))
            if self.node(x, y - 1, z):
                out.append((x, y - 1, z))
            elif self.air(x, y - 1, z):
                f = self.fall(x, y - 1, z, y)
                if f:
                    out.append(f)
        if k & SWIM:
            if not k & DOWN and self.kind(x, y + 1, z) & (SWIM | CLIMB):
                out.append((x, y + 1, z))
            if not k & UP and self.node(x, y - 1, z):
                out.append((x, y - 1, z))
        return out

    def reach(self, start, limit_box, edges=None):
        x1, y1, z1, x2, y2, z2 = limit_box
        seen = {start}
        q = deque([start])
        while q:
            c = q.popleft()
            for n in self.moves(*c):
                if not (x1 <= n[0] <= x2 and z1 <= n[2] <= z2 and y1 <= n[1] <= y2):
                    continue
                if edges is not None:
                    edges.setdefault(n, []).append(c)   # обратные рёбра: куда можно вернуться
                if n not in seen:
                    seen.add(n)
                    q.append(n)
        return seen


def traps(w, start, box, goals):
    """Западни: клетки, куда дойти можно, а оттуда до цели (разлом, привал) — нет. [(размер, пример клетки)]."""
    back = {}
    reached = w.reach(start, box, back)
    good = set()
    q = deque(c for c in reached if c in goals)
    good.update(q)
    while q:
        c = q.popleft()
        for p in back.get(c, ()):
            if p not in good:
                good.add(p)
                q.append(p)
    bad = reached - good
    out = []
    while bad:
        seed = min(bad)
        group = {seed}
        q = deque([seed])
        bad.discard(seed)
        while q:
            x, y, z = q.popleft()
            for n in ((x + 1, y, z), (x - 1, y, z), (x, y, z + 1), (x, y, z - 1), (x, y + 1, z), (x, y - 1, z)):
                if n in bad:
                    bad.discard(n)
                    group.add(n)
                    q.append(n)
        out.append((len(group), seed))
    return sorted(out, reverse=True)


# Водоворот (пузыри над магмой) несёт вниз быстрее, чем плывут: клетка трубы — доля клетки заплыва.
WHIRL_COST = 0.4


def breath(w, reached, limit=10):
    """Самый долгий путь под водой (в клетках заплыва) до места, где можно вдохнуть: голова над водой, пузыри вверх
    или суша рядом. Шаг вниз по водовороту стоит `WHIRL_COST` клетки."""
    import heapq
    sub = {c for c in reached if w.kind(*c) & SWIM}
    dist = {}
    q = []
    for (x, y, z) in sub:
        s = w.block(x, y + 1, z)
        if not is_water(s) or current(w.block(x, y, z)) > 0:
            dist[(x, y, z)] = 0
        elif any(not w.kind(*n) & SWIM for n in w.moves(x, y, z)):
            dist[(x, y, z)] = 1   # шаг из воды на сушу
        else:
            continue
        q.append((dist[(x, y, z)], (x, y, z)))
    heapq.heapify(q)
    while q:
        d, (x, y, z) = heapq.heappop(q)
        if d > dist.get((x, y, z), 1e9):
            continue
        down = current(w.block(x, y, z)) < 0
        for n in ((x + 1, y, z), (x - 1, y, z), (x, y, z + 1), (x, y, z - 1), (x, y + 1, z), (x, y - 1, z)):
            if n not in sub:
                continue
            if n[1] == y - 1 and current(w.block(*n)) < 0:
                continue                  # из водоворота вверх не всплыть — только вниз и вбок
            step = WHIRL_COST if down and n[1] == y + 1 and current(w.block(*n)) < 0 else 1
            nd = d + step
            if nd < dist.get(n, 1e9):
                dist[n] = nd
                heapq.heappush(q, (nd, n))
    worst = max(((round(d, 1), c) for c, d in dist.items()), default=(0, None))
    lost = [c for c in sub if c not in dist]
    return worst, lost


def drops(w, reached):
    """Обрывы без перил: с достижимой клетки шагнуть (или сорваться с лестницы) туда, где пол ниже `kit.MAX_DROP`,
    а внизу нет ни воды, ни лестницы/лозы. Пропасть до бездны (ниже пола этапа) — не обрыв: Система вытаскивает.
    Так же ловится лоза в одной клетке широкой шахты: с разбега игрок пролетает мимо неё в соседнюю пустоту.
    [(высота, откуда, куда)] — по одному на место (соседние клетки края склеиваются), самые высокие первыми."""
    found = {}
    for c in reached:
        x, y, z = c
        k = w.kind(x, y, z)
        steps = [(x + dx, y, z + dz) for dx, dz in DIRS4]
        if k & CLIMB and not k & STAND:
            steps.append((x, y - 1, z))
        for n in steps:
            nx, ny, nz = n
            if ny == y and (w.node(nx, ny, nz) or (w.node(nx, ny + 1, nz) and w.air(x, y + 2, z))):
                continue
            if ny == y and not (w.air(nx, ny, nz) and w.air(nx, ny + 1, nz)):
                continue
            if ny < y and (w.node(nx, ny, nz) or not w.air(nx, ny, nz)):
                continue
            for yy in range(ny - 1 if ny == y else ny, ny - 128, -1):
                if yy < w.floor_y:
                    break                                   # бездна
                if not w.air(nx, yy, nz):
                    break                                   # лава или упор в блок — не наше дело
                kk = w.kind(nx, yy, nz)
                if kk & (SWIM | CLIMB):
                    break
                if kk & STAND:
                    h = y - yy
                    if h > MAX_DROP:
                        key = (nx // 4, nz // 4, yy)
                        if h > found.get(key, (0,))[0]:
                            found[key] = (h, c, (nx, yy, nz))
                    break
    return sorted(found.values(), reverse=True)


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


def check_stage(m, st, species, bosses, errors, strict=False, warns=None):
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
    floor_y = st['box'][1]
    w = Walker(m, opened, floor_y)
    if not w.stand(*camp):
        errors.append(f'{sid}: на привале негде стоять {camp}')
        return
    while True:
        w = Walker(m, opened, floor_y)
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
    # Отряд до шести (фаза 11): на привале и в залах хранителей всем хватает места, а на каждой плите
    # помещаются двое (вшестером на трёх плитах стоят по двое).
    def room(cx, cy, cz, r, dy=3):
        return sum(1 for (x, y, z) in reached if (x - cx) ** 2 + (z - cz) ** 2 <= r * r and abs(y - cy) <= dy)
    cx, cy, cz = st['camp'][:3]
    if room(cx, cy, cz, 5) < 24:
        errors.append(f'{sid}: на привале тесно для шестерых ({room(cx, cy, cz, 5)} клеток в 5 блоках)')
    for b in st['bosses']:
        bx, by, bz = b['at']
        if room(bx, by, bz, b['arena'] * 0.7, dy=4) < 80:
            errors.append(f'{sid}: зал {b["id"]} тесен для шестерых')
    if strict:
        # Арены под приёмы (обновление 14, пункт 10): радиус и пол — по `arenas.ARENAS`.
        from arenas import ARENAS, FLOOR_SHARE
        for b in st['bosses']:
            a = ARENAS.get(b['type'])
            if not a:
                errors.append(f'{sid}: хранителя {b["type"]} нет в arenas.py — нужен радиус под его приёмы')
                continue
            if b['arena'] < a['need']:
                errors.append(f'{sid}: арена {b["id"]} радиусом {b["arena"]} меньше нужной {a["need"]} ({a["why"]})')
            bx, by, bz = b['at']
            rr = a['need'] * 0.8
            have, want = room(bx, by, bz, rr, dy=4), int(3.14159 * rr * rr * FLOOR_SHARE)
            if have < want:
                errors.append(f'{sid}: в зале {b["id"]} мало пола — {have} кл. в {rr:.0f} блоках, нужно {want}')
    for mc in st.get('mechs', []):
        if mc['type'] != 'plates':
            continue
        for x, y, z in mc['points']:
            spots = [(x + dx, yy, z + dz) for dx, dz in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1))
                     for yy in (y - 1, y, y + 1)]
            if sum(1 for c in spots if c in reached) < 2:
                errors.append(f'{sid}: {mc["id"]}: на плите {x, y, z} не встать вдвоём')
    for b in st['bosses']:
        if 'arena2' in b:
            x, y, z, r = b['arena2']
            if not Walker(m, opened).stand(int(x // 1), int(y // 1), int(z // 1)):
                errors.append(f'{sid}: второй зал {b["id"]}: в центре негде стоять')
    # западни и вода — в итоговом состоянии (все преграды открыты)
    sink = errors if strict else (warns if warns is not None else [])
    goals = {c for c in reached if any(abs(c[0] - x) <= 1 and abs(c[2] - z) <= 1 and abs(c[1] - y) <= 1
                                       for x, y, z in box_cells(st['rift']))} | {camp}
    for size, at in traps(w, camp, st['box'], goals)[:6]:
        sink.append(f'{sid}: западня — {size} кл., оттуда не вернуться к разлому и привалу (напр. {at})')
    cliffs = drops(w, reached)
    for h, at, to in cliffs[:6]:
        sink.append(f'{sid}: обрыв без перил — {h} бл. вниз с {at} на {to} (больше {MAX_DROP}: бортик, вода или лоза)')
    if len(cliffs) > 6:
        sink.append(f'{sid}: обрывов без перил ещё {len(cliffs) - 6}')
    (worst, at), lost = breath(w, reached)
    if worst > 10:
        sink.append(f'{sid}: под водой {worst} кл. до вдоха (у {at}) — утонут')
    if lost:
        sink.append(f'{sid}: под водой без воздуха вовсе (у {min(lost)})')
    return len(reached)


def check(maps, layout, strict=(), warns=None):
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
        check_stage(maps[st['n']], st, species, bosses, errors, st['n'] in strict, warns)
    return errors
