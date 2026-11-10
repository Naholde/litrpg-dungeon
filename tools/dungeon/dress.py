"""
Отделка этапа поверх готовой карты (фаза 13, отзыв владельца «карта простая»): у каждого этапа своё убранство.

- ниши в стенах на уровне груди — черепа и свечи, жемчуг, наковальни, кристаллы (по теме этапа);
- пилястры вдоль прямых стен залов — стены перестают быть гладкими;
- арки в коридорах — ритм и ощущение пути;
- свисающее с потолка — цепи с фонарями, сталактиты, корни, паутина, аметист;
- завалы и мелочь у стен — плиты, ступени, кости, мох.

Ничего не ставится там, где идёт игра: у преград, механизмов, тайников, табличек, на привале, у торговца, у разлома,
на аренах хранителей и у точек отрядов. Всё решает хеш координат — карта одинакова при каждой сборке.
Проходимость после отделки проверяет check.py, как и раньше.
"""
import math

from kit import base, h3

# тема этапа: ниши, пилястры, арки, потолок, завалы
STYLE = {
    1: dict(niche=[('skeleton_skull', {'rotation': 0}), ('candle', {'candles': 3, 'lit': True, 'waterlogged': False}),
                   ('decorated_pot', {'facing': 'north', 'cracked': False, 'waterlogged': False}), ('bone_block', {'axis': 'y'})],
            niche_base='chiseled_stone_bricks', pilaster='stone_bricks', pilaster_cap='chiseled_stone_bricks',
            arch='stone_brick_stairs', hang=['cobweb', 'chain_lantern_soul', 'chain'],
            rubble=[('cobblestone_slab', 'slab'), ('stone_brick_stairs', 'stairs'), ('mossy_cobblestone_slab', 'slab'),
                    ('stone_brick_slab', 'slab'), ('skeleton_skull', 'skull')]),
    2: dict(niche=[('brown_mushroom', {}), ('red_mushroom', {}), ('decorated_pot', {'facing': 'north', 'cracked': True, 'waterlogged': False}),
                   ('cobweb', {})],
            niche_base='moss_block', pilaster='stripped_dark_oak_log', pilaster_cap='dark_oak_wood',
            arch='dark_oak_stairs', hang=['hanging_roots', 'cobweb', 'chain_lantern'],
            rubble=[('moss_carpet', 'block'), ('mossy_cobblestone_slab', 'slab'), ('dark_oak_slab', 'slab'), ('moss_carpet', 'block')]),
    3: dict(niche=[('sea_pickle', {'pickles': 3, 'waterlogged': False}), ('decorated_pot', {'facing': 'north', 'cracked': False, 'waterlogged': False}),
                   ('prismarine_wall', {}), ('sea_lantern', {})],
            niche_base='dark_prismarine', pilaster='prismarine_bricks', pilaster_cap='dark_prismarine',
            arch='prismarine_brick_stairs', hang=['chain_lantern_sea', 'chain', 'dripstone'],
            rubble=[('prismarine_slab', 'slab'), ('dark_prismarine_stairs', 'stairs'), ('prismarine_brick_slab', 'slab')]),
    4: dict(niche=[('anvil', {'facing': 'north'}), ('iron_bars', {}), ('magma_block', {}), ('blackstone', {})],
            niche_base='polished_blackstone_bricks', pilaster='polished_basalt', pilaster_cap='polished_blackstone',
            arch='polished_blackstone_brick_stairs', hang=['chain', 'chain_lantern', 'chain'],
            rubble=[('blackstone_slab', 'slab'), ('polished_blackstone_brick_stairs', 'stairs'), ('polished_blackstone_slab', 'slab')]),
    5: dict(niche=[('amethyst_cluster', {'facing': 'up', 'waterlogged': False}), ('end_rod', {'facing': 'up'}),
                   ('purpur_pillar', {'axis': 'y'}), ('amethyst_block', {})],
            niche_base='purpur_block', pilaster='purpur_pillar', pilaster_cap='end_stone_bricks',
            arch='purpur_stairs', hang=['amethyst_down', 'end_rod_down', 'chain_lantern_soul'],
            rubble=[('purpur_slab', 'slab'), ('end_stone_brick_slab', 'slab'), ('purpur_stairs', 'stairs')]),
}

DIRS = ((1, 0, 'east'), (-1, 0, 'west'), (0, 1, 'south'), (0, -1, 'north'))
OPP = {'east': 'west', 'west': 'east', 'south': 'north', 'north': 'south'}


def _protected(stage):
    """Где отделки быть не должно: (x, z, радиус) и коробки (x1, z1, x2, z2)."""
    pts, boxes = [], []
    for g in stage.gates:
        x1, _, z1, x2, _, z2 = g['box']
        boxes.append((min(x1, x2) - 3, min(z1, z2) - 3, max(x1, x2) + 3, max(z1, z2) + 3))
    for mch in stage.mechs:
        for p in mch['points']:
            pts.append((p[0], p[2], 2.5))
        if 'area' in mch:
            a = mch['area']
            boxes.append((min(a[0], a[3]) - 1, min(a[2], a[5]) - 1, max(a[0], a[3]) + 1, max(a[2], a[5]) + 1))
    for c in stage.caches:
        pts.append((c['at'][0], c['at'][2], 2.5))
    for s in stage.signs:
        pts.append((s['at'][0], s['at'][2], 2))
    for p in (stage.camp, stage.trader, stage.rift):
        if p:
            pts.append((p[0], p[2], 7 if p is stage.camp else 3))
    for b in stage.bosses:
        pts.append((b['at'][0], b['at'][2], b['arena'] + 1.5))
        if 'arena2' in b:
            a = b['arena2']
            pts.append((a[0], a[2], a[3] + 1.5))
    for pk in stage.packs:
        for mob in pk['mobs']:
            pts.append((mob['at'][0], mob['at'][2], 1.5))
    return pts, boxes


def _blocked(x, z, pts, boxes):
    for (px, pz, r) in pts:
        if (x - px) ** 2 + (z - pz) ** 2 <= r * r:
            return True
    for (x1, z1, x2, z2) in boxes:
        if x1 <= x <= x2 and z1 <= z <= z2:
            return True
    return False


def _place(m, x, y, z, item):
    name, props = item
    if name == 'skeleton_skull':
        props = dict(props, rotation=int(h3(x, y, z, 3) * 16) % 16)
    m.set(x, y, z, name, **props)


def _hang(m, x, top, z, kind, floor_y):
    """Что-то свисает с потолка в клетке (x, top, z); ниже floor_y + 3 не опускаемся."""
    room = top - floor_y
    if kind == 'cobweb':
        m.set(x, top, z, 'cobweb')
    elif kind == 'hanging_roots':
        m.set(x, top, z, 'hanging_roots', waterlogged=False)
    elif kind == 'chain':
        n = max(1, min(3, room - 4))
        for i in range(n):
            m.set(x, top - i, z, 'chain', axis='y', waterlogged=False)
    elif kind.startswith('chain_lantern'):
        n = max(1, min(3, room - 5))
        for i in range(n):
            m.set(x, top - i, z, 'chain', axis='y', waterlogged=False)
        if kind == 'chain_lantern_sea':
            m.set(x, top - n, z, 'sea_lantern')
        else:
            m.lantern(x, top - n, z, soul=kind.endswith('soul'), hanging=True)
    elif kind == 'dripstone':
        m.set(x, top, z, 'pointed_dripstone', thickness='base', vertical_direction='down', waterlogged=False)
        if room >= 6:
            m.set(x, top - 1, z, 'pointed_dripstone', thickness='tip', vertical_direction='down', waterlogged=False)
    elif kind == 'amethyst_down':
        m.set(x, top, z, 'amethyst_cluster', facing='down', waterlogged=False)
    elif kind == 'end_rod_down':
        m.set(x, top, z, 'end_rod', facing='down')


def dress(m, stage, n):
    st = STYLE.get(n)
    if not st:
        return 0
    pts, boxes = _protected(stage)
    opn = m.open
    floors = [p for p in opn if m.is_air(*p) and m.solid(p[0], p[1] - 1, p[2]) and (p[0], p[1] - 1, p[2]) not in opn]
    floor_set = set(floors)
    placed = {'niche': [], 'pilaster': [], 'arch': 0, 'hang': 0, 'rubble': 0}

    def height(x, y, z):
        return m.ceil_y(x, y, z) - y + 1

    def run(x, y, z, dx, dz):
        """Сколько подряд пола вдоль направления (в обе стороны) — ширина прохода."""
        k = 1
        for s in (1, -1):
            i = 1
            while (x + dx * i * s, y, z + dz * i * s) in floor_set and i < 12:
                i += 1
                k += 1
        return k

    def far(lst, x, z, d):
        return all((x - a) ** 2 + (z - b) ** 2 >= d * d for a, b in lst)

    def near_breakable(x, y, z):
        """Рядом ломаемая стена (проход к секретке) — здесь ничего не ставим."""
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                for dy in (0, 1, 2):
                    b = m.get(x + dx, y + dy, z + dz)
                    if b is not None and 'infested' in b:
                        return True
        return False

    for (x, y, z) in sorted(floors):
        if (x, z) in m.keep or _blocked(x, z, pts, boxes) or near_breakable(x, y, z):
            continue
        walls = [(dx, dz, f) for dx, dz, f in DIRS if (x + dx, y, z + dz) not in opn and m.solid(x + dx, y, z + dz)]
        h = height(x, y, z)
        wx, wz = run(x, y, z, 1, 0), run(x, y, z, 0, 1)
        corridor = min(wx, wz) <= 4 and max(wx, wz) >= 5
        k = h3(x, y, z, 41)
        # ---- ниша на уровне груди в прямой стене
        if len(walls) == 1 and h >= 4 and k < 0.10 and far(placed['niche'], x, z, 5):
            dx, dz, face = walls[0]
            nx, nz = x + dx, z + dz
            # Только обычная стена темы: треснувшие (ломаются к секреткам), рычаги и таблички не трогаем.
            wall_names = {w[0].split(':')[-1] for w in m.themes[opn[(x, y, z)]].wall}
            ok = all((nx, yy, nz) not in opn and m.solid(nx, yy, nz) and m.get(nx, yy, nz) is not None
                     and base(m.get(nx, yy, nz)) in wall_names for yy in (y, y + 1, y + 2, y + 3))
            ok = ok and all(m.solid(nx + dx, yy, nz + dz) and (nx + dx, yy, nz + dz) not in opn for yy in (y + 1, y + 2))
            if ok:
                m.set(nx, y, nz, st['niche_base'])
                m.clear(nx, y + 1, nz)
                m.clear(nx, y + 2, nz)
                _place(m, nx, y + 1, nz, st['niche'][int(k * 100) % len(st['niche'])])
                if n in (1, 2) and int(k * 1000) % 3 == 0 and st['niche'][int(k * 100) % len(st['niche'])][0] != 'candle':
                    m.candle(nx, y + 2, nz, 1 + int(k * 37) % 3)
                placed['niche'].append((x, z))
                continue
        # ---- пилястра у прямой стены зала (не в коридоре, не у проёмов и углов)
        if not corridor and min(wx, wz) >= 7 and len(walls) == 1 and h >= 5 and k > 0.86 and far(placed['pilaster'], x, z, 6):
            dx, dz, _ = walls[0]
            sx, sz = -dz, dx  # вдоль стены
            straight = all((x + sx * i, y, z + sz * i) in floor_set
                           and m.solid(x + sx * i + dx, y, z + sz * i + dz)
                           and (x + sx * i + dx, y, z + sz * i + dz) not in opn for i in (-2, -1, 1, 2))
            if straight:
                top = m.ceil_y(x, y, z)
                for yy in range(y, top + 1):
                    m.set(x, yy, z, st['pilaster'], **({'axis': 'y'} if st['pilaster'].endswith(('log', 'pillar', 'basalt')) else {}))
                m.set(x, y, z, st['pilaster_cap'])
                placed['pilaster'].append((x, z))
                continue
        # ---- арка поперёк коридора
        if corridor and h >= 4:
            along_x = wx > wz
            idx = x if along_x else z
            if idx % 6 == 0:
                top = y + h - 1
                for dx, dz, face in walls:
                    if (dx != 0) == along_x:
                        continue  # стена в торце, а не сбоку
                    if m.is_air(x, top, z) and (x, top, z) in opn:
                        m.stairs(x, top, z, st['arch'], face, half='top')
                        placed['arch'] += 1
            continue
        # ---- свисает с потолка (в высоких залах)
        if h >= 6 and k > 0.45 and k < 0.47 + 0.01 * (n == 5):
            top = m.ceil_y(x, y, z)
            if m.is_air(x, top, z):
                _hang(m, x, top, z, st['hang'][int(k * 1000) % len(st['hang'])], y)
                placed['hang'] += 1
            continue
        # ---- завал у стены
        if walls and min(wx, wz) >= 5 and h >= 3 and 0.3 < k < 0.345 and m.is_air(x, y, z):
            name, kind = st['rubble'][int(k * 1000) % len(st['rubble'])]
            if kind == 'slab':
                m.slab(x, y, z, name)
            elif kind == 'stairs':
                m.stairs(x, y, z, name, walls[0][2])
            elif kind == 'skull':
                _place(m, x, y, z, ('skeleton_skull', {}))
            else:
                m.set(x, y, z, name, **({'axis': 'y'} if name in ('basalt', 'bone_block') else {}))
            placed['rubble'] += 1
    return {k: (len(v) if isinstance(v, list) else v) for k, v in placed.items()}
