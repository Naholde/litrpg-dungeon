"""Отделка залов: саркофаги, свечи, кости, жаровни, колонны, ниши. Общая для этапов."""
import math

from kit import h3


def sarcophagus(m, x, y, z, along='x', lid='polished_andesite', body='stone_bricks', skull=True):
    """Каменный гроб 3×1 (крышка — плиты), у изголовья — череп."""
    for i in range(3):
        px, pz = (x + i, z) if along == 'x' else (x, z + i)
        m.set(px, y, pz, body)
        m.slab(px, y + 1, pz, f'{lid}_slab')
    if skull:
        hx, hz = (x - 1, z) if along == 'x' else (x, z - 1)
        if m.is_air(hx, y, hz):
            m.set(hx, y, hz, 'skeleton_skull', rotation=4 if along == 'x' else 0)


def candles(m, x, y, z, color=None, n=None, seed=0):
    m.candle(x, y, z, n or 1 + int(h3(x, y, z, seed) * 4) % 4, color)


def bones_pile(m, x, y, z, r=1.5, seed=0):
    """Груда костей: костяные блоки и черепа по полу."""
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if math.hypot(dx, dz) > r:
                continue
            k = h3(x + dx, y, z + dz, seed)
            if not m.is_air(x + dx, y, z + dz):
                continue
            if k < 0.35:
                m.set(x + dx, y, z + dz, 'bone_block', axis='y' if k < 0.2 else 'x')
            elif k < 0.45:
                m.set(x + dx, y, z + dz, 'skeleton_skull', rotation=int(k * 160) % 16)


def brazier(m, x, y, z, soul=True):
    """Жаровня: блок-подставка и костёр сверху (свет и дым)."""
    m.set(x, y, z, 'polished_blackstone_bricks')
    m.set(x, y + 1, z, 'soul_campfire' if soul else 'campfire', facing='north', lit=True, signal_fire=False, waterlogged=False)


def column(m, x, z, y, h, body='stone_bricks', base_='chiseled_stone_bricks', cap='chiseled_stone_bricks'):
    m.pillar(x, z, y, h, body, cap=cap, base_=base_)


def cobwebs(m, box, chance=0.04, seed=0):
    """Паутина по верхним углам области (ломается — тег breakable)."""
    x1, y1, z1, x2, y2, z2 = box
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            for y in range(y1, y2 + 1):
                if not m.is_air(x, y, z) or (x, y, z) not in m.open:
                    continue
                walls = sum(1 for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)) if m.solid(x + dx, y, z + dz))
                if (walls >= 1 and m.solid(x, y + 1, z)) and h3(x, y, z, seed + 5) < chance * (2 if walls >= 2 else 1):
                    m.set(x, y, z, 'cobweb')


def wall_niches(m, x1, x2, z, y, face, every=3, block='skeleton_skull'):
    """Ниши в стене вдоль x (z — плоскость стены): каждые every блоков — углубление с черепом или свечой."""
    for x in range(x1, x2 + 1, every):
        m.clear(x, y + 1, z)
        m.set(x, y, z, 'chiseled_stone_bricks')
        if block == 'skeleton_skull':
            m.set(x, y + 1, z, 'skeleton_skull', rotation={'south': 0, 'north': 8, 'east': 12, 'west': 4}[face])
        else:
            m.candle(x, y + 1, z, 3)


def carpet_line(m, x1, z1, x2, z2, y, color='red'):
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for z in range(min(z1, z2), max(z1, z2) + 1):
            if m.is_air(x, y, z) and m.solid(x, y - 1, z):
                m.set(x, y, z, f'{color}_carpet')


def banner(m, x, y, z, facing, color='black'):
    m.set(x, y, z, f'{color}_wall_banner', facing=facing)


def floor_pattern(m, x1, z1, x2, z2, y, blocks, seed=0):
    """Пол узором: случайно из списка по хешу (y — уровень пола, блок y-1)."""
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            if m.solid(x, y - 1, z) and (x, y, z) in m.open:
                m.set(x, y - 1, z, blocks[int(h3(x, y, z, seed) * len(blocks)) % len(blocks)])
