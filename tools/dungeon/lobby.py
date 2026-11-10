"""
Преддверие (обновление 14, пункт 16) — заново, в вертикали и под стиль этапов.

Игрок появляется наверху, в **Зале прибытия** (ноги 84), и выходит на **галерею** — кольцо под куполом огромного
круглого **Зала Печати** (r 20, купол до ~106). Внизу (ноги 64) посередине — **Печать на столпе над бездной**:
круглый остров, вокруг — провал до пустоты, к острову ведут четыре моста. Вниз с галереи три пути, и каждый — урок
того, что ждёт в подземелье:
  • **Башня-винт** (юго-запад) — винтовая лестница на 20 блоков, вверх и вниз пешком;
  • **Шахта лозы** (юго-восток) — вьющаяся лоза во всю ширину шахты, вверх и вниз;
  • **Прыжок веры** — колодец 3×3 со стенками у галереи, внизу купель (дропер; стенки гасят разбег — с мостка над
    открытым залом разбег уносил мимо купели, −48 % здоровья вживую).
С пола Зала Печати:
  • на север — **Зал пяти Врат**: врата каждого этапа в его материалах, с названием и уровнями (печати не открываются —
    в подземелье ведёт Печать);
  • на запад — **Зал ролей**: три святилища с витражами-эмблемами (танк, боец, поддержка);
  • на восток — **Двор испытаний**: манекены (урон в секунду) и злой манекен (бьёт того, кто ему опаснее, — танку
    проверить провокацию, лекарю — лечение); манекенов ставит игра по схеме (`dummies`);
  • на юг — **Скрижаль правил** под галереей.
Секрет: с края провала — прыжки по осколкам вниз, в **келью под Печатью** (летопись). Сорвался — бездна, Система
вытащит на последнее твёрдое место (в Преддверии — без урона).

Проверка строгая (`check.py`): обрывы без перил, западни, все таблички и манекены достижимы пешком от места появления.
"""
import math

from kit import Map, Theme, h3, jump_ok
from stage4 import lay, rails

CX, CZ = 48, 48          # середина Зала Печати (клетка)
FY = 64                  # пол Зала Печати (уровень ног)
GY = 84                  # галерея и Зал прибытия
R = 20                   # радиус Зала Печати
DAIS = 5.5               # остров Печати
PIT = 9.5                # внешний край провала
PIT_BOT = 30             # стены провала кончаются тут; ниже — пустота
GAL_IN = 17              # внутренний край галереи
CELL_Y = 57              # келья под Печатью (уровень ног)
BOX = (0, 44, 0, 104, 112, 100)
RAIL = 'polished_blackstone_brick_wall'
STRICT = True

STAGES = [  # (номер, название, уровни, рамка, вставки, свет, пол перед вратами)
    (1, 'Склеп Забытых', '1–10', 'deepslate_bricks', 'bone_block', 'soul_lantern', 'cracked_deepslate_bricks'),
    (2, 'Паучья чаща', '10–20', 'mossy_stone_bricks', 'moss_block', 'shroomlight', 'moss_block'),
    (3, 'Затопленный храм', '20–30', 'prismarine_bricks', 'dark_prismarine', 'sea_lantern', 'prismarine_bricks'),
    (4, 'Кузни Пепла', '30–40', 'polished_blackstone_bricks', 'gilded_blackstone', 'lantern', 'nether_bricks'),
    (5, 'Шпиль Бездны', '40–50', 'purpur_block', 'amethyst_block', 'end_rod', 'end_stone_bricks'),
]

# Эмблемы ролей 7×7 (сверху вниз, слева направо — как видит игрок из ниши): a — цвет роли, b — второй, y — золото.
EMBLEMS = {
    'tank': ('blue', 'light_blue', [
        '.aaaaa.',
        'abbbbba',
        'abbybba',
        'abyyyba',
        'abbybba',
        '.abbba.',
        '..aaa..']),
    'damage': ('red', 'orange', [
        '......a',
        '.....ab',
        '....ab.',
        'y..ab..',
        '.yab...',
        '.ay....',
        'a..y...']),
    'support': ('lime', 'white', [
        '..aaa..',
        '..aba..',
        'aaabaaa',
        'abbbbba',
        'aaabaaa',
        '..aba..',
        '..aaa..']),
}

# Путь к келье под Печатью: край провала → шесть осколков вниз → дверь кельи. Каждый прыжок — и обратно.
JUMP_FROM = (44, FY, 57)
PADS = [(44, 63, 55), (42, 62, 55), (42, 61, 53), (40, 60, 53), (40, 59, 51), (42, 58, 51)]
CELL_DOOR = (44, CELL_Y, 51)


def d(x, z):
    """Расстояние клетки от середины зала — той же мерой, что у `round_room`."""
    return math.hypot(x - CX, z - CZ)


def ring(r1, r2, cx=CX, cz=CZ):
    """Клетки кольца r1 < d ≤ r2 (мера `round_room`)."""
    return [(x, z) for x in range(int(cx - r2) - 1, int(cx + r2) + 2) for z in range(int(cz - r2) - 1, int(cz + r2) + 2)
            if r1 < math.hypot(x - cx, z - cz) <= r2]


def wall_post(m, x, y, z):
    m.set(x, y, z, 'polished_blackstone_brick_wall', up=True, north='none', south='none', east='none', west='none',
          waterlogged=False)


def build():
    m = Map(seed=11)
    hall = m.theme(Theme('lobby', wall=[('deepslate_bricks', 6), ('cracked_deepslate_bricks', 1), ('deepslate_tiles', 2)],
                         floor=[('polished_deepslate', 4), ('deepslate_tiles', 2)], ceil='deepslate_bricks', seed=3))
    shrine = m.theme(Theme('shrine', wall=[('polished_blackstone_bricks', 5), ('cracked_polished_blackstone_bricks', 1)],
                           floor='polished_blackstone', ceil='blackstone', seed=5))
    m.theme(Theme('abyss', wall=[('blackstone', 4), ('deepslate', 3), ('crying_obsidian', 1)],
                  floor='blackstone', ceil='blackstone', seed=9))
    yard = m.theme(Theme('yard', wall=[('tuff_bricks', 5), ('polished_tuff', 2), ('chiseled_tuff_bricks', 1)],
                         floor=[('polished_tuff', 3), ('tuff_bricks', 1)], ceil='tuff_bricks', seed=7))
    signs = []

    def sign(x, y, z, facing, lines):
        m.sign(x, y, z, facing)
        signs.append({'at': [x, y, z], 'lines': lines})

    # ================================================================== пустоты
    # Зал Печати — круг r 20 высотой 32 и купол.
    m.round_room(CX, CZ, R, FY, 32, hall, dome=10)
    # Провал вокруг острова Печати — до пустоты.
    pit_cells = ring(DAIS, PIT)
    for (x, z) in pit_cells:
        for y in range(PIT_BOT, FY):
            m.open[(x, y, z)] = 'abyss'
            m.nowall.add((x, y, z))
    # Келья под Печатью (секрет) и её дверь в стене столпа — к осколкам.
    m.round_room(CX, CZ, 4.2, CELL_Y, 4, shrine)
    m.carve(44, CELL_Y, 51, 45, CELL_Y + 2, 51, shrine)

    # Зал прибытия (юг, наверху) и проход на галерею.
    m.room(41, 72, 55, 82, GY, 8, hall, vault=3)
    m.carve(45, GY, 66, 51, GY + 5, 72, hall)
    # Зал пяти Врат (север): прямоугольник, открытый в Зал Печати; врата — в нишах глубиной в блок.
    m.room(30, 12, 66, 31, FY, 15, hall, vault=3)
    for i in range(5):
        gx = 32 + 8 * i
        m.carve(gx - 2, FY, 11, gx + 2, FY + 6, 11, hall)
    # Скрижаль правил (юг, под галереей).
    m.carve(45, FY, 66, 51, FY + 5, 70, shrine)
    # Зал ролей (запад): коридор, зал и три ниши-святилища.
    m.corridor([(22, 48), (29, 48)], 5, 5, hall, y=FY)
    m.room(6, 38, 21, 58, FY, 9, shrine, vault=3)
    shrines = {'tank': 'north', 'damage': 'west', 'support': 'south'}
    m.carve(2, FY, 45, 5, FY + 6, 51, shrine)
    m.carve(10, FY, 34, 16, FY + 6, 37, shrine)
    m.carve(10, FY, 59, 16, FY + 6, 62, shrine)
    # Двор испытаний (восток) и коридор.
    m.corridor([(67, 48), (75, 48)], 5, 5, hall, y=FY)
    m.room(75, 37, 98, 59, FY, 9, yard, vault=3)

    # Башня-винт (юго-запад): два оборота на 20 — вход внизу и выход наверху с севера, один над другим.
    m.spiral(35.5, 70.5, FY, GY, 4, hall, step='polished_deepslate', core='deepslate_tiles', r_core=1.5,
             per_turn=10, start=-math.pi / 2, underside='polished_deepslate_stairs')
    for y0 in (FY, GY):
        m.carve(35, y0, 62, 36, y0 + 2, 66, hall)
    # Шахта лозы (юго-восток): 2×2, вьющаяся лоза во всю ширину.
    for y0 in (FY, GY):
        m.carve(60, y0, 62, 61, y0 + 2, 68, hall)
    m.shaft(60, 69, 61, 70, FY, GY + 2, hall)          # над верхом лозы — простор для головы, иначе не выйти
    m.vines(60, 69, FY, GY - 1, kind='twisting', to=(61, 70))

    # «Прыжок веры» (восток-юго-восток): колодец 3×3 за стеной зала, дверь с галереи наверху и выход в зал внизу.
    m.dropper(67, 57, 69, 59, GY, FY, hall, depth=3, pad=1)
    for y0 in (FY, GY):
        m.carve(66, y0, 57 if y0 == FY else 58, 66, y0 + 2, 59 if y0 == FY else 58, hall)

    # ================================================================== оболочка
    m.shell(2)

    # ---------------------------------------------------------------- пол Зала Печати: кольца и лучи
    for (x, z) in ring(-1, R):
        if (x, FY - 1, z) in m.open or not m.solid(x, FY - 1, z):
            continue
        dd = d(x, z)
        b = None
        if dd <= DAIS:
            b = 'polished_blackstone' if dd > 4.6 else ('chiseled_polished_blackstone' if (x == CX or z == CZ)
                                                        else 'polished_blackstone_bricks')
        elif dd <= PIT + 1.2:
            b = 'gilded_blackstone' if h3(x, 0, z, 4) < 0.25 else 'polished_blackstone'
        elif 13.5 < dd <= 14.4:
            b = 'gilded_blackstone' if (x + z) % 3 == 0 else 'polished_blackstone_bricks'
        elif dd > GAL_IN:
            b = 'deepslate_tiles' if (x + z) % 2 else 'polished_deepslate'
        elif x == CX or z == CZ:
            b = 'polished_blackstone_bricks'
        if b:
            m.set(x, FY - 1, z, b)

    # ---------------------------------------------------------------- остров Печати
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if abs(dx) == 2 or abs(dz) == 2:
                m.slab(CX + dx, FY, CZ + dz, 'polished_blackstone_brick_slab')
            elif (dx, dz) != (0, 0):
                m.set(CX + dx, FY, CZ + dz, 'polished_blackstone_bricks')
    m.set(CX, FY, CZ, 'chiseled_polished_blackstone')
    m.set(CX, FY + 1, CZ, 'litrpg:seal')
    for dx, dz in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
        wall_post(m, CX + dx, FY, CZ + dz)
        m.candle(CX + dx, FY + 1, CZ + dz, 4, 'purple')
    # Четыре обелиска с кристаллами.
    for dx, dz in ((-4, -3), (4, -3), (-4, 3), (4, 3)):
        ox, oz = CX + dx, CZ + dz
        for y in range(FY, FY + 4):
            m.set(ox, y, oz, 'polished_blackstone' if y < FY + 3 else 'crying_obsidian')
        m.set(ox, FY + 4, oz, 'amethyst_cluster', facing='up', waterlogged=False)

    # Мосты на остров (с четырёх сторон) — над провалом.
    bridge_cells = set()
    for (a, b) in (((CX, CZ - 9), (CX, CZ - 6)), ((CX, CZ + 6), (CX, CZ + 9)),
                   ((CX - 9, CZ), (CX - 6, CZ)), ((CX + 6, CZ), (CX + 9, CZ))):
        path = m.bridge(a, b, FY, 'polished_blackstone_bricks', width=3, rail=RAIL, under='polished_blackstone',
                        lanterns=False)
        along_x = a[1] == b[1]
        for _, x, z, _ in path:
            for k in (-1, 0, 1):
                bridge_cells.add((x, z + k) if along_x else (x + k, z))

    # Перила по краю провала (на краю пола и острова), кроме мостов и места прыжка к келье.
    edge = []
    for (x, z) in ring(-1, PIT + 1.5):
        if (x, z) in bridge_cells or (x, FY, z) == JUMP_FROM:
            continue
        if d(x, z) <= DAIS and any(n in bridge_cells for n in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1))):
            continue                                          # у моста на острове — проход
        if (x, FY - 1, z) in m.open or not m.solid(x, FY - 1, z):
            continue
        if any((nx, nz) not in bridge_cells and (nx, FY - 1, nz) in m.open
               for nx, nz in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1))):
            if m.is_air(x, FY, z):
                edge.append((x, FY, z))
    m.railing(edge, RAIL)

    # Стены провала: аметист и плачущий обсидиан, глубже — сильнее свечение; редкий свет в глубине.
    for (x, z) in pit_cells:
        for y in range(PIT_BOT, FY - 2):
            for nx, nz in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
                if (nx, y, nz) in m.open or not m.solid(nx, y, nz):
                    continue
                depth = (FY - y) / (FY - PIT_BOT)
                r = h3(nx, y, nz, 21)
                if r < 0.05 + 0.25 * depth:
                    m.set(nx, y, nz, 'crying_obsidian' if r < 0.15 * depth + 0.02 else 'amethyst_block')
        if (x + z) % 5 == 0:
            for y in (FY - 8, FY - 18, FY - 28):
                if y >= PIT_BOT and m.is_air(x, y, z):
                    m.set(x, y, z, 'light', level=9, waterlogged=False)
    # Цепи из-под острова в бездну.
    for (x, z) in ring(DAIS, DAIS + 1.1):
        if (x + 2 * z) % 4 == 0 and (x, z) not in bridge_cells and (x, FY - 3, z) in m.open:
            if any(abs(x - px) <= 1 and abs(z - pz) <= 1 for px, _, pz in PADS):
                continue
            for y in range(FY - 12, FY - 2):
                m.set(x, y, z, 'chain', axis='y', waterlogged=False)

    # Секрет: осколки вниз к келье под Печатью.
    assert jump_ok(JUMP_FROM, PADS[0]) and jump_ok(PADS[0], JUMP_FROM)
    assert jump_ok(PADS[-1], CELL_DOOR) and jump_ok(CELL_DOOR, PADS[-1])
    assert all(jump_ok(b, a) for a, b in zip(PADS, PADS[1:])), 'с осколков должно быть можно вернуться'
    m.parkour(PADS, 'amethyst_block')
    for (x, y, z) in PADS:
        m.set(x, y - 2, z, 'crying_obsidian')
        m.set(x, y - 3, z, 'pointed_dripstone', thickness='tip', vertical_direction='down', waterlogged=False)
    # Келья: столп-корень Печати, кристаллы, летопись.
    for y in range(CELL_Y, CELL_Y + 4):
        m.set(CX, y, CZ, 'crying_obsidian' if y % 2 else 'amethyst_block')
    for (x, z) in ring(-1, 4.2):
        if (x, CELL_Y - 1, z) not in m.open:
            m.set(x, CELL_Y - 1, z, 'purpur_block' if (x + z) % 2 else 'crying_obsidian')
    sign(CX, CELL_Y + 1, CZ - 4, 'south', ['ПОД ПЕЧАТЬЮ', 'Здесь корень', 'Системы. Она', 'видит тебя.'])
    sign(CX + 4, CELL_Y + 1, CZ, 'west', ['Пять этапов —', 'пять печатей.', 'Каждую держит', 'свой страж.'])
    sign(CX, CELL_Y + 1, CZ + 4, 'north', ['Кто дойдёт до', 'Вершины, тот', 'узнает, чья', 'это Система.'])
    for (x, z) in ring(2.9, 4.2):
        if (x, z) in ((44, 51), (45, 51), (45, 50), (CX, CZ - 4), (CX + 4, CZ), (CX, CZ + 4)):
            continue
        if m.is_air(x, CELL_Y, z) and h3(x, 1, z, 31) < 0.3:
            m.set(x, CELL_Y, z, 'amethyst_cluster', facing='up', waterlogged=False)
        if m.is_air(x, CELL_Y + 3, z) and (x + z) % 2 == 0:
            m.set(x, CELL_Y + 3, z, 'light', level=11, waterlogged=False)

    # ---------------------------------------------------------------- галерея
    gal = {(x, z): GY for (x, z) in ring(GAL_IN - 0.01, R) if (x, GY, z) in m.open}
    lay(m, gal, 'polished_deepslate', rim='polished_blackstone_bricks', corbel='polished_deepslate_stairs')
    gal_rails = rails(m, gal, RAIL, double=False)
    for (x, y, z) in gal_rails:
        if (x + z) % 7 == 0 and m.is_air(x, y + 1, z):
            m.lantern(x, y + 1, z, soul=True)
    # Колодец «Прыжка веры»: золотая кайма у края, свет над купелью.
    for z in range(57, 60):
        m.set(66, GY - 1, z, 'gilded_blackstone')
    for (x, z) in ((67, 57), (69, 59)):
        m.set(x, FY + 3, z, 'light', level=13, waterlogged=False)
    # Опоры галереи: колонны 2×2 по кругу (там, где не мешают проходам и мостам).
    busy = set(bridge_cells) | {(x, z) for x in range(62, 67) for z in range(55, 62)}
    for ang in (12, 77, 103, 155, 168, 196, 222, 318, 342):
        a = math.radians(ang)
        px, pz = math.floor(CX + 18.5 * math.cos(a)), math.floor(CZ + 18.5 * math.sin(a))
        cells = [(px, pz), (px + 1, pz), (px, pz + 1), (px + 1, pz + 1)]
        if any((x, FY, z) not in m.open or (x, z) in busy for x, z in cells):
            continue
        for x, z in cells:
            for y in range(FY, GY - 1):
                m.set(x, y, z, 'polished_blackstone_bricks' if y == FY else
                      'chiseled_deepslate' if (y - FY) % 6 == 0 else 'deepslate_tiles')
        for x, z in cells:
            for nx, nz, f in ((x + 1, z, 'west'), (x - 1, z, 'east'), (x, z + 1, 'north'), (x, z - 1, 'south')):
                if (nx, nz) not in cells and (nx, GY - 2, nz) in m.open and m.is_air(nx, GY - 2, nz):
                    m.stairs(nx, GY - 2, nz, 'polished_deepslate_stairs', f, half='top')

    # Купол: Око Системы — кольцо из аметиста и плачущего обсидиана, фиолетовое стекло, фонари на цепях.
    top = max(y for (x, y, z), t in m.open.items() if (x, z) == (CX, CZ) and t == 'lobby')
    for (x, z) in ring(3.5, 5.0):
        m.set(x, top, z, 'crying_obsidian' if (x + z) % 2 else 'amethyst_block')
    for (x, z) in ring(-1, 3.5):
        m.set(x, top, z, 'purple_stained_glass')
    m.set(CX, top - 1, CZ, 'light', level=15, waterlogged=False)
    for (x, z) in ring(4.4, 5.0):
        if (x + z) % 3 == 0:
            for y in range(top - 6, top):
                m.set(x, y, z, 'chain', axis='y', waterlogged=False)
            m.set(x, top - 7, z, 'soul_lantern', hanging=True, waterlogged=False)
    # Над Печатью — столб света (невидимые источники).
    for y in range(FY + 3, FY + 26, 3):
        m.set(CX, y, CZ, 'light', level=14, waterlogged=False)

    # ---------------------------------------------------------------- Зал пяти Врат
    def axis(block, ax):
        return {'axis': ax} if block == 'bone_block' else {}

    for i, (n, name, lv, frame, accent, lamp, path) in enumerate(STAGES):
        gx = 32 + 8 * i
        for x in range(gx - 2, gx + 3):
            for y in range(FY, FY + 7):
                m.set(x, y, 11, 'litrpg:sealed_gate')
        for y in range(FY, FY + 9):
            for x in (gx - 3, gx + 3):
                blk = accent if y in (FY, FY + 4, FY + 8) else frame
                m.set(x, y, 12, blk, **axis(blk, 'y'))
        for x in range(gx - 2, gx + 3):
            for y in (FY + 7, FY + 8):
                blk = accent if (x == gx and y == FY + 8) else frame
                m.set(x, y, 12, blk, **axis(blk, 'x'))
        for x in (gx - 3, gx + 3):
            if lamp == 'end_rod':
                m.set(x, FY + 9, 12, 'end_rod', facing='up')
            elif lamp in ('soul_lantern', 'lantern'):
                m.set(x, FY + 9, 12, lamp, hanging=False, waterlogged=False)
            else:
                m.set(x, FY + 9, 12, lamp)
        for x in range(gx - 1, gx + 2):
            for z in range(13, 19):
                m.set(x, FY - 1, z, path)
        m.set(gx, FY + 3, 13, 'light', level=13, waterlogged=False)
        sign(gx - 3, FY + 2, 13, 'south', [f'ЭТАП {n}', name, f'уровни {lv}', ''])
    m.set(48, FY, 21, 'chiseled_deepslate')
    m.set(48, FY + 1, 21, 'amethyst_cluster', facing='up', waterlogged=False)
    sign(48, FY, 22, 'south', ['ВРАТА', 'не открываются:', 'в подземелье', 'ведёт Печать'])

    # ---------------------------------------------------------------- Зал ролей
    texts = {'tank': ['ТАНК', 'держит врагов', 'на себе.', 'Провокация'],
             'damage': ['БОЕЦ', 'наносит урон', 'и не стоит', 'в разметке'],
             'support': ['ПОДДЕРЖКА', 'лечит отряд.', 'Цель лечения —', 'Ctrl+1…6']}
    for name, side in shrines.items():
        col, col2, rows = EMBLEMS[name]
        # витраж на задней стене ниши: за стеклом — свет и белая подложка
        for j, row in enumerate(rows):
            y = FY + 6 - j
            for i, ch in enumerate(row):
                if side == 'west':      # смотрят на запад: слева — юг
                    g, lt, bk = (1, y, 51 - i), (0, y, 51 - i), (-1, y, 51 - i)
                elif side == 'north':   # смотрят на север: слева — запад
                    g, lt, bk = (10 + i, y, 33), (10 + i, y, 32), (10 + i, y, 31)
                else:                   # смотрят на юг: слева — восток
                    g, lt, bk = (16 - i, y, 63), (16 - i, y, 64), (16 - i, y, 65)
                if ch == '.':
                    m.set(*g, 'polished_blackstone_bricks')
                    continue
                c = {'a': col, 'b': col2, 'y': 'yellow'}[ch]
                m.set(*g, f'{c}_stained_glass')
                m.set(*lt, 'light', level=15, waterlogged=False)
                m.set(*bk, 'white_concrete')
        # постамент со свечами и табличка
        px, pz, f = {'west': (3, 48, 'east'), 'north': (13, 35, 'south'), 'south': (13, 61, 'north')}[side]
        m.set(px, FY, pz, 'chiseled_polished_blackstone')
        m.candle(px, FY + 1, pz, 4, col)
        dx, dz = {'east': (1, 0), 'south': (0, 1), 'north': (0, -1)}[f]
        sign(px + dx, FY, pz + dz, f, texts[name])
        for k in (-2, 2):
            qx, qz = (px, pz + k) if side == 'west' else (px + k, pz)
            wall_post(m, qx, FY, qz)
            m.set(qx, FY + 1, qz, f'{col}_candle', candles=3, lit=True, waterlogged=False)
    sign(21, FY + 1, 44, 'west', ['ЗАЛ РОЛЕЙ', 'Роль выбирают', 'в окне персо-', 'нажа: клавиша K'])
    sign(21, FY + 1, 52, 'west', ['Отряд — 3–6.', 'Нужны все три', 'роли: танк,', 'боец, поддержка'])

    # ---------------------------------------------------------------- Двор испытаний
    for x in range(76, 98):
        for z in range(38, 59):
            if (x - 87) ** 2 / 100 + (z - 48) ** 2 / 81 <= 1 and m.solid(x, FY - 1, z):
                m.set(x, FY - 1, z, 'packed_mud' if h3(x, 2, z, 5) < 0.7 else 'mud_bricks')
    dummies = []
    for z, angry in ((42, False), (48, True), (54, False)):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                m.set(93 + dx, FY - 1, z + dz, 'red_terracotta' if angry else 'chiseled_tuff')
        m.set(93, FY - 1, z, 'chiseled_tuff_bricks')
        dummies.append({'at': [93.5, FY, z + 0.5], 'yaw': 90, 'aggressive': angry})
    for x in range(79, 97, 4):
        for z in (37, 59):
            m.set(x, FY + 2, z, 'target')
            m.set(x, FY, z, 'hay_block', axis='y')
    for x, b, props in ((81, 'anvil', {'facing': 'west'}), (85, 'grindstone', {'face': 'floor', 'facing': 'north'}),
                        (89, 'smithing_table', {}), (92, 'barrel', {'facing': 'up', 'open': False})):
        m.set(x, FY, 58, b, **props)
        m.set(x, FY, 38, b, **props)
    sign(75, FY + 1, 41, 'east', ['ДВОР ИСПЫТАНИЙ', 'Проверьте', 'навыки', 'до похода'])
    sign(75, FY + 1, 44, 'east', ['МАНЕКЕН', 'Над ним — урон', 'в секунду. Не', 'бить — сброс'])
    sign(75, FY + 1, 52, 'east', ['ЗЛОЙ МАНЕКЕН', 'бьёт того, кто', 'ему опаснее.', 'Танк, проверь!'])
    sign(75, FY + 1, 55, 'east', ['ЛЕКАРЬ', 'лечит того,', 'кого бьют:', 'Ctrl+1…6'])

    # ---------------------------------------------------------------- Скрижаль правил (юг, под галереей)
    rules = [['НАВЫКИ', 'Z X C V B G —', 'навыки роли', 'и класса'],
             ['РЫВОК', 'Левый Alt —', 'рывок туда,', 'куда идёте'],
             ['РАЗМЕТКА', 'Удар хранителя', 'виден заранее:', 'выйдите из зоны'],
             ['ДУХ', 'Павшего', 'воскрешает', 'союзник: Shift'],
             ['ВАЙП', 'Все пали —', 'этап заново.', 'Вещи остаются'],
             ['ДОБЫЧА', 'У каждого своя', '— делить', 'нечего'],
             ['КОДЕКС', 'Клавиша J —', 'о боях, классах', 'и этапах']]
    for i, lines in enumerate(rules):
        sign(45 + i, FY + 1, 70, 'north', lines)
        m.set(45 + i, FY - 1, 70, 'polished_blackstone_bricks')
    for x in (45, 51):
        m.set(x, FY + 4, 69, 'light', level=13, waterlogged=False)
    m.candle(46, FY, 67, 3, 'white')
    m.candle(50, FY, 67, 3, 'white')

    # ---------------------------------------------------------------- Зал прибытия
    for x in range(44, 53):
        for z in range(75, 84):
            r2 = (x - 48) ** 2 + (z - 79) ** 2
            if r2 <= 5:
                m.set(x, GY - 1, z, 'gilded_blackstone' if r2 >= 4 else 'chiseled_polished_blackstone')
    for z in range(66, 77):
        if m.is_air(48, GY, z):
            m.set(48, GY, z, 'purple_carpet')
    arrival = [
        (41, 75, 'east', ['ПРЕДДВЕРИЕ', 'Система ждёт', 'отряд. Отсюда —', 'в подземелье']),
        (41, 78, 'east', ['1. РОЛЬ', 'Клавиша K —', 'танк, боец или', 'поддержка']),
        (41, 81, 'east', ['2. ОТРЯД', '3–6 игроков', 'с ролями —', 'у Печати внизу']),
        (55, 75, 'west', ['3. ПЕЧАТЬ', 'в центре зала', 'внизу: ПКМ —', 'начать поход']),
        (55, 78, 'west', ['ВНИЗ', 'Башня-винт —', 'слева, лоза —', 'справа']),
        (55, 81, 'west', ['ПРЫЖОК ВЕРЫ', 'Колодец справа', 'на галерее —', 'прыжок в купель']),
    ]
    for x, z, f, lines in arrival:
        sign(x, GY + 1, z, f, lines)
    for x in (41, 55):
        for z in (73, 77, 80):
            m.wall_torch(x, GY + 3, z, 'east' if x == 41 else 'west', soul=True)
    for x in (43, 53):
        for z in (74, 77, 80):
            m.stairs(x, GY, z, 'polished_blackstone_stairs', 'west' if x == 43 else 'east')
    # Таблички у дверей башен (на галерее).
    sign(37, GY, 64, 'north', ['БАШНЯ-ВИНТ', 'вниз, к Печати', 'пешком', ''])
    sign(59, GY, 64, 'north', ['ЛОЗА', 'Вниз — держись', 'за лозу. Shift —', 'зависнуть'])
    sign(65, GY + 1, 57, 'west', ['ПРЫЖОК ВЕРЫ', 'Шагни в колодец', '— купель внизу', 'поймает'])

    # ================================================================== свет
    # Шахта лозы и колодец «Прыжка веры»: светящиеся камни в стенах (внутри иначе темно хоть глаз выколи).
    for (x1, z1, x2, z2, blk) in ((60, 69, 61, 70, 'verdant_froglight'), (67, 57, 69, 59, 'pearlescent_froglight')):
        for y in range(FY + 3, GY - 1, 4):
            for x in range(x1 - 1, x2 + 2):
                for z in range(z1 - 1, z2 + 2):
                    edge = x in (x1 - 1, x2 + 1) or z in (z1 - 1, z2 + 1)
                    corner = x in (x1 - 1, x2 + 1) and z in (z1 - 1, z2 + 1)
                    if edge and not corner and (x + z + y) % 2 == 0 and m.solid(x, y, z) and (x, y, z) not in m.open:
                        m.set(x, y, z, blk, axis='y')
    m.hanging_lights(spacing=6, soul=True, themes=['lobby'], min_h=9, chain=4, offset=2)
    m.hanging_lights(spacing=5, soul=False, themes=['shrine', 'yard'], min_h=8, chain=2, offset=1)
    m.glow(spacing=4, level=13, themes=['lobby', 'shrine', 'yard'], height=3)
    m.glow(spacing=6, level=13, themes=['lobby'], height=12)

    lobby = {'box': list(BOX), 'spawn': [CX + 0.5, GY, 79.5, 180], 'seal': [CX, FY + 1, CZ], 'signs': signs,
             'dummies': dummies}
    return m, lobby
