"""
Этап 3 · Затопленный храм (уровни 20–30). Призмарин, морские фонари, окна в море, водовороты.
Обновление 14: храм уходит под воду четырьмя ярусами, у каждого спуска свой приём — не как в этапах 1 и 2.

Ярус I · Верхний храм (ноги 84): Притвор (привал, окна в море) → Галерея окон (за стеклом — море: ламинария,
  кораллы, морские фонари) → Зал Прилива (затопленный зал: три плиты на островах — доплыть) → Арена Стража Прилива
  (квадратный зал r 18, шесть колонн-укрытий) → **Цистерна**: открытый колодец r 9 с пандусом-винтом по стене вниз
  на 18 блоков; посередине вода (сорвался — в воду) и фонтан пузырей в стеклянной трубе.
Ярус II · Затопленные галереи (66): Нижний зал Цистерны (засада; **пузырьковый колодец** — срезка наверх, к привалу)
  → Бассейн течений (струи полосами; **подводный лаз** под северной стеной — заплыв в грот с тайником) → Пещера
  корабля (корабль на мели: Капитан на палубе, по мачте в «клетке» — в «воронье гнездо», там его сундук) →
  Арена Левиафана (r 21, кольцо воды у края) → **Водоворот**: нырнуть в трубу — пузыри над магмой тянут на 19 блоков
  вниз сквозь стеклянную трубу в пруд нижнего зала.
Ярус III · Библиотека (50): Пруд водоворота (засада) → Библиотека прилива (две галереи по лестницам, вентили на
  галереях и в простенке, загадка «вода») → Затонувший неф (дорога по насыпи между двумя затопленными пролётами;
  паркур по обломкам колонн к нише с тайником) → **Лестница-аквариум** вниз на 16: по обе стороны — стекло и море,
  ступени бьют струи прорыва по очереди.
Ярус IV · Святилище (34): Преддверие → Зал Наяры (r 19, восемь колонн) → Купол на дне (второй зал: потолок — стекло,
  над ним море) → ниша разлома.

Арены — по `arenas.py` (замер приёмов): Страж Прилива 18, Левиафан 21, Наяра 19.
"""
import math

from decor import brazier, column
from kit import WATER, Map, Stage, Theme, apply_gates, h3, is_water, passable

N = 3
OX = 1024 * N
STRICT = True                  # строгая проверка: обрывы, западни, арены — ошибки
A, B, C, D = 84, 66, 50, 34    # ярусы: уровень ног

CORAL = ('tube_coral_block', 'brain_coral_block', 'bubble_coral_block', 'fire_coral_block', 'horn_coral_block')
SIDES = ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))
POST = dict(up=True, north='none', south='none', east='none', west='none', waterlogged=False)


def X(x):
    return OX + x


def sea(m, cells, out, deep=2, seed=0, glass='glass', life=True):
    """Окно в море: в клетках стены cells — стекло, за ним (по направлению out = (dx, dy, dz)) — вода на deep блоков,
    вокруг воды — камень, кораллы и морские фонари, у дна — ламинария, водоросли, морские огурцы.
    Вода проверяется сразу: течь ей некуда (иначе ошибка постройки)."""
    cells = list(cells)
    dx, dy, dz = out
    for c in cells:
        if c in m.open:
            raise ValueError(f'окно в море: {c} — не стена, а пустота')

    def build():
        glassy = set(cells)
        for c in cells:
            m.set(*c, glass)
        ws = {(x + dx * k, y + dy * k, z + dz * k) for (x, y, z) in cells for k in range(1, deep + 1)} - glassy
        for c in ws:
            if c in m.open:
                raise ValueError(f'окно в море: за стеклом {c} — пустота')
            m.blocks[c] = WATER
        for (x, y, z) in ws:
            for sx, sy, sz in SIDES:
                n = (x + sx, y + sy, z + sz)
                if n in ws:
                    continue
                if n in m.open:
                    raise ValueError(f'окно в море: вода {x, y, z} рядом с пустотой {n}')
                b = m.get(*n)
                if b is not None and (is_water(b) or not passable(b)):
                    continue
                k = h3(*n, seed)
                m.set(*n, CORAL[int(k * 50) % 5] if k < 0.3 else 'sea_lantern' if k > 0.9 else 'dark_prismarine')
        if life:
            cols = {}
            for (x, y, z) in ws:
                cols.setdefault((x, z), []).append(y)
            for (x, z), ys in cols.items():
                ys.sort()
                y0 = ys[0]
                if not m.solid(x, y0 - 1, z) or 'glass' in (m.get(x, y0 - 1, z) or '') and dy == 0:
                    continue
                run = 1
                while y0 + run in ys:
                    run += 1
                k = h3(x, y0, z, seed + 5)
                if k < 0.32 and run >= 2:
                    for y in range(y0, y0 + run - 1):
                        m.set(x, y, z, 'kelp_plant')
                    m.set(x, y0 + run - 1, z, 'kelp', age=25)
                elif k < 0.5:
                    m.set(x, y0, z, 'seagrass')
                elif k < 0.58:
                    m.set(x, y0, z, 'sea_pickle', pickles=1 + int(k * 97) % 4, waterlogged=True)
        m.check_water([c for c in ws if is_water(m.get(*c))])
    m.later(build)


def wall_strip(x1, x2, z, y1, y2):
    """Клетки стены z от x1 до x2 на высотах y1..y2 (под окно в море)."""
    return [(x, y, z) for x in range(x1, x2 + 1) for y in range(y1, y2 + 1)]


def water_disc(m, cx, cz, r, y_top, depth, theme):
    """Круглая купель в полу: вода от y_top-depth+1 до y_top в круге r (центр — середина клетки). Вырезает пустоту."""
    cells = []
    for x in range(int(cx - r) - 1, int(cx + r) + 2):
        for z in range(int(cz - r) - 1, int(cz + r) + 2):
            if math.hypot(x + 0.5 - cx, z + 0.5 - cz) <= r:
                for y in range(y_top - depth + 1, y_top + 1):
                    m.open[(x, y, z)] = theme.name
                    cells.append((x, y, z))
    return cells


def build(_n=N):
    m = Map(seed=3003)
    temple = m.theme(Theme('temple', wall=[('prismarine_bricks', 5), ('prismarine', 3), ('dark_prismarine', 1)],
                           floor=[('prismarine_bricks', 3), ('dark_prismarine', 2), ('prismarine', 1)],
                           ceil=[('prismarine_bricks', 3), ('dark_prismarine', 1)], seed=31))
    sunk = m.theme(Theme('sunk', wall=[('prismarine', 3), ('mossy_stone_bricks', 2), ('sandstone', 1)],
                         floor=[('sand', 2), ('gravel', 1), ('prismarine', 2)], ceil=[('prismarine', 2), ('mossy_stone_bricks', 1)], seed=32))
    library = m.theme(Theme('library', wall=[('dark_prismarine', 3), ('prismarine_bricks', 2)],
                            floor=[('dark_prismarine', 2), ('prismarine_bricks', 2)], ceil='dark_prismarine', seed=33))
    deep = m.theme(Theme('deep', wall=[('dark_prismarine', 3), ('obsidian', 1), ('prismarine', 1)],
                         floor=[('dark_prismarine', 3), ('prismarine_bricks', 1)], ceil='dark_prismarine', seed=34))
    s = Stage(N, 'temple', 'Затопленный храм', (20, 30), 'temple')

    # ================================================================== пустоты
    # ---- Ярус I · Верхний храм (84)
    m.room(X(0), 20, X(16), 34, A, 7, temple, vault=2)                          # Притвор (привал)
    m.corridor([(X(16), 27), (X(22), 27)], 3, 4, temple, y=A)
    m.room(X(22), 18, X(58), 36, A, 8, temple, vault=4)                         # Галерея окон
    m.corridor([(X(58), 27), (X(64), 27)], 3, 4, temple, y=A)
    m.room(X(64), 12, X(92), 42, A, 9, temple, vault=3)                         # Зал Прилива
    m.pool(X(66), 14, X(90), 25, A - 1, 3, temple)                              # затопленные пролёты
    m.pool(X(66), 29, X(90), 40, A - 1, 3, temple)
    m.corridor([(X(92), 27), (X(97), 27)], 3, 4, temple, y=A)
    m.room(X(97), 8, X(135), 46, A, 11, temple, vault=4)                        # Арена Стража Прилива (квадрат 39×39)
    m.corridor([(X(135), 27), (X(143), 27)], 3, 4, temple, y=A)
    # Цистерна: пандус-винт по стене (r 9), посередине открытый колодец с водой, над ним купол
    m.spiral(X(152) + 0.5, 27.5, B, A, 9, temple, step='prismarine_bricks', core=None, r_core=5.5, per_turn=14.4,
             start=math.pi / 2, underside='prismarine_brick_stairs')
    m.round_room(X(152) + 0.5, 27.5, 9.6, A + 4, 3, temple, dome=5)
    well = water_disc(m, X(152) + 0.5, 27.5, 5.5, B - 1, 3, temple)

    # ---- Ярус II · Затопленные галереи (66)
    m.corridor([(X(152), 36), (X(152), 42)], 3, 4, sunk, y=B)
    m.room(X(136), 41, X(168), 57, B, 8, sunk, vault=2)                         # Нижний зал Цистерны
    m.room(X(136), 32, X(140), 40, B, 4, sunk)                                  # ниша пузырькового колодца
    m.bubble_lift(X(138), 30, B, A - 1, 'south', 'north', sunk, glass='glass', basin='prismarine_bricks')
    m.corridor([(X(168), 49), (X(174), 49)], 3, 4, sunk, y=B)
    m.room(X(174), 40, X(206), 58, B, 8, sunk, vault=3)                         # Бассейн течений
    # подводный лаз: купель у северной стены → ход под стеной (8 клеток) → грот с тайником
    m.room(X(194), 27, X(201), 32, B, 4, sunk)                                  # грот
    lair = [(x, y, z) for x in (X(197), X(198)) for y in (B - 4, B - 3) for z in range(33, 41)]
    for c in lair:
        m.open[c] = sunk.name

    def lair_water():
        for c in lair:
            m.blocks[c] = WATER
    m.later(lair_water)                                                         # до проверки купелей — они на него опираются
    m.pool(X(196), 41, X(199), 43, B - 1, 4, sunk)
    m.pool(X(196), 30, X(199), 32, B - 1, 4, sunk)
    m.later(lambda: m.check_water(lair))
    m.corridor([(X(206), 49), (X(214), 49)], 3, 4, sunk, y=B)
    m.cave(X(236), B + 9, 49, 22, 13, 15, sunk, rough=0.15, seed=9, floor=B)    # Пещера корабля
    m.corridor([(X(257), 49), (X(267), 49)], 3, 4, temple, y=B)
    m.round_room(X(288) + 0.5, 49.5, 21.5, B, 12, temple, dome=6)               # Арена Левиафана
    moat = []
    for x in range(X(265), X(312)):
        for z in range(26, 74):
            d = math.hypot(x + 0.5 - (X(288) + 0.5), z + 0.5 - 49.5)
            if 18.0 <= d <= 20.6 and abs(z - 49) > 2:
                for y in (B - 2, B - 1):
                    m.open[(x, y, z)] = temple.name
                    moat.append((x, y, z))
    m.corridor([(X(309), 49), (X(318), 49)], 3, 4, temple, y=B)
    m.room(X(318), 41, X(334), 57, B, 7, deep, vault=2)                         # Водоворот (верх)
    m.whirlpool(X(325), 48, X(326), 49, B, C, deep, pad=2, depth=3, glass='glass', basin='dark_prismarine')

    # ---- Ярус III · Библиотека (50)
    m.room(X(314), 36, X(338), 62, C, 8, deep, vault=2)                         # Пруд водоворота (свод ≤ C+9 — над ним купель)
    m.corridor([(X(338), 49), (X(344), 49)], 3, 4, library, y=C)
    m.room(X(344), 34, X(378), 64, C, 12, library, vault=3)                     # Библиотека прилива (две галереи)
    m.corridor([(X(378), 49), (X(384), 49)], 3, 4, sunk, y=C)
    m.room(X(384), 36, X(416), 62, C, 12, sunk, vault=3)                        # Затонувший неф
    m.room(X(396), 32, X(400), 35, C + 5, 3, sunk)                              # ниша с тайником над пролётом
    m.pool(X(386), 38, X(414), 45, C - 1, 3, sunk)                              # северный пролёт
    m.pool(X(386), 53, X(414), 60, C - 1, 3, sunk)                              # южный пролёт
    # ---- Ярус IV · Святилище (34): лестница-аквариум вниз на 16
    m.corridor([(X(416), 49, C), (X(419), 49, C), (X(435), 49, D), (X(440), 49, D)], 5, 5, deep)
    m.room(X(440), 39, X(456), 59, D, 7, deep, vault=2)                         # Преддверие святилища
    m.corridor([(X(456), 49), (X(463), 49)], 3, 4, deep, y=D)
    m.round_room(X(482) + 0.5, 49.5, 19.5, D, 12, deep, dome=6)                 # Зал Наяры
    m.corridor([(X(501), 49), (X(509), 49)], 3, 4, deep, y=D)
    m.round_room(X(525) + 0.5, 49.5, 16.5, D, 11, deep)                         # Купол на дне (потолок — стекло)
    m.room(X(522), 66, X(528), 72, D, 6, deep)                                  # ниша разлома

    # ---- вертикаль до оболочки
    # корабль: «воронье гнездо» на мачте, лестница в клетке
    m.platform(X(238) + 0.5, 49.5, B + 16, 2.5, 'spruce_planks', rim='stripped_spruce_wood', theme=sunk, rail='spruce_fence')
    m.ladder(X(237), 49, B + 4, B + 15, 'west')
    m.sleeve(X(237), 49, X(237), 49, B + 7, B + 14, 'spruce_fence')
    # затонувший неф: обломки колонн — паркур к нише (сорвался — в воду)
    m.parkour([(X(392), C + 1, 44), (X(392), C + 2, 41), (X(395), C + 3, 41), (X(398), C + 4, 41), (X(398), C + 5, 38)],
              'chiseled_stone_bricks', pillar='prismarine_bricks', theme=sunk)
    m.shell(2)
    # дальше всё отложенное (`later`) выполняется сразу — оболочка уже стоит

    # ================================================================== Притвор (привал)
    for (lx, lz) in ((X(2), 22), (X(2), 32), (X(14), 22), (X(14), 32)):
        m.set(lx, A - 1, lz, 'sea_lantern')
    m.set(X(8), A, 27, 'soul_campfire', facing='north', lit=True, signal_fire=False, waterlogged=False)
    for dx in (-2, 2):
        m.stairs(X(8 + dx), A, 27, 'dark_prismarine_stairs', 'east' if dx < 0 else 'west')
    sea(m, wall_strip(X(3), X(5), 19, A + 1, A + 3) + wall_strip(X(11), X(13), 19, A + 1, A + 3), (0, 0, -1), seed=1)
    m.sign(X(0), A + 1, 27, 'east')
    s.sign((X(0), A + 1, 27), ['ЭТАП 3', 'Затопленный', 'храм', 'уровни 20–30'])
    s.say((X(0), A - 1, 20, X(16), A + 8, 34),
          'Система: «Храм ушёл под воду вместе с теми, кто молился. Они до сих пор молятся. '
          'Храм уходит вниз на четыре яруса — и чем ниже, тем ближе море.»')

    # ================================================================== Галерея окон
    for x in range(X(26), X(57), 6):
        for z in (21, 33):
            column(m, x, z, A, m.ceil_y(x, A, z) - A + 1, 'prismarine_bricks', 'dark_prismarine', 'sea_lantern')
        if x + 4 <= X(57):
            sea(m, wall_strip(x + 2, x + 4, 17, A + 1, A + 4), (0, 0, -1), seed=x)
            sea(m, wall_strip(x + 2, x + 4, 37, A + 1, A + 4), (0, 0, 1), seed=x + 7)
    m.pool(X(26), 27, X(54), 27, A - 1, 1, temple)                              # ручей по оси галереи
    s.pack('p1', [('drowned_guard', X(32), A, 24, 21), ('harpooner', X(36), A, 31, 21), ('drowned_guard', X(33), A, 30, 21, None, 4)])
    s.pack('p2', [('jelly', X(43), A, 22, 21), ('jelly', X(45), A, 24, 21), ('jelly', X(46), A, 22, 21), ('deep_priest', X(47), A, 31, 21),
                  ('jelly', X(44), A, 32, 21, None, 5)])
    s.pack('p3', [('crab', X(52), A, 23, 22), ('crab', X(53), A, 31, 22), ('harpooner', X(56), A, 20, 22, None, 4)])
    s.say((X(22), A - 1, 18, X(58), A + 8, 36),
          'Система: «За стеклом — море. Оно ждёт. Утопленники держат щиты перед собой, их жрецы держат утопленников — '
          'начинайте с жрецов.»')

    # ================================================================== Зал Прилива: три плиты на островах
    plates = []
    for (ix, iz) in ((X(72), 18), (X(86), 20), (X(79), 35)):
        for x in range(ix - 2, ix + 3):
            for z in range(iz - 2, iz + 3):
                corner = abs(x - ix) == 2 and abs(z - iz) == 2
                for y in range(A - 4, A):
                    m.set(x, y, z, 'dark_prismarine' if corner or y < A - 1 else 'prismarine_bricks')
                if corner:
                    m.set(x, A, z, 'prismarine_wall', **POST)
                    m.set(x, A + 1, z, 'sea_lantern')
        m.set(ix, A - 1, iz, 'sea_lantern')
        m.set(ix, A, iz, 'heavy_weighted_pressure_plate', power=0)
        plates.append((ix, A, iz))
    mech_plates = s.mech('plates', 'plates', plates, text='Плиты трёх приливов', hold=3)
    for x in (X(65), X(91)):
        for z in (13, 41):
            column(m, x, z, A, m.ceil_y(x, A, z) - A + 1, 'prismarine', 'dark_prismarine', 'sea_lantern')
    m.sign(X(64), A + 1, 25, 'east')
    s.sign((X(64), A + 1, 25), ['Три прилива', 'приходят вместе.', 'Доплывите до', 'плит на островах'])
    p4 = s.pack('p4', [('drowned_guard', X(74), A, 27, 22), ('deep_priest', X(82), A, 26, 22), ('moray', X(78), A, 28, 22),
                       ('jelly', X(72), A, 16, 22), ('moray', X(86), A, 21, 22, None, 4)])
    s.gate('g1', (X(93), A, 26, X(93), A + 3, 28), open_when=[f'mech:{mech_plates}', f'pack:{p4}'])
    s.say((X(64), A - 1, 12, X(92), A + 10, 42),
          'Система: «Зал Прилива. Пол ушёл под воду — остались дорога посередине и три острова. '
          'Плиты на островах держат дверь, если на них стоят все трое разом.»')

    # ================================================================== Арена Стража Прилива: шесть колонн-укрытий
    for (dx, dz) in ((-8, -8), (8, -8), (-8, 8), (8, 8), (0, -13), (0, 13)):
        for ox in range(-1, 2):
            for oz in range(-1, 2):
                x, z = X(116 + dx + ox), 27 + dz + oz
                top = m.ceil_y(x, A, z)
                for y in range(A, top + 1):
                    m.set(x, y, z, 'sea_lantern' if (y - A) % 4 == 3 and ox == 0 and oz == 0 else
                          'dark_prismarine' if abs(ox) + abs(oz) == 2 else 'prismarine_bricks')
    for x0 in (X(101), X(127)):                                                  # море за северной и южной стенами
        sea(m, wall_strip(x0, x0 + 4, 7, A + 2, A + 6), (0, 0, -1), seed=x0)
        sea(m, wall_strip(x0, x0 + 4, 47, A + 2, A + 6), (0, 0, 1), seed=x0 + 3)
    b1 = s.boss('tide', 'tide_warden', (X(116), A, 27), 18)
    s.gate('arena1', (X(95), A, 26, X(95), A + 3, 28), lock=b1)
    s.gate('g2', (X(137), A, 26, X(137), A + 3, 28), open_when=[f'boss:{b1}'])
    s.say((X(97), A - 1, 8, X(135), A + 14, 46),
          'Система: «Страж Прилива. Когда пойдёт волна — встаньте так, чтобы колонна была между вами и водой. '
          'Стены зала не спасут: волна приходит изнутри.»')

    # ================================================================== Цистерна: пандус по стене, колодец с водой
    for c in well:
        m.blocks[c] = WATER
    for (x, y, z) in well:                                                       # огни на дне
        if y == B - 3 and h3(x, y, z, 9) < 0.18:
            m.set(x, y - 1, z, 'sea_lantern')
    # фонтан посередине: пузыри вверх в стеклянной трубе до купола (руками не достать — для красоты)
    fx, fz = X(152), 27
    m.set(fx, B - 4, fz, 'soul_sand')
    top = B - 1
    while (fx, top + 1, fz) in m.open and m.is_air(fx, top + 1, fz):
        top += 1
    fountain = [(fx, y, fz) for y in range(B - 3, top + 1)]
    for c in fountain:
        m.set(*c, 'bubble_column', drag=False)
    for y in range(B - 3, top + 1):                                              # труба закрыта и в купели: иначе
        for ddx, ddz in ((1, 0), (-1, 0), (0, 1), (0, -1)):                      # пловца унесёт под купол насовсем
            m.set(fx + ddx, y, fz + ddz, 'glass')
    well = [c for c in well if is_water(m.get(*c))]
    m.set(fx, top + 1, fz, 'sea_lantern')
    m.check_water(well + fountain)
    # свет в стене цистерны: кольца морских фонарей через 4 блока (свет набора сюда не ставится — здесь `keep`)
    for y in range(B + 1, A + 9, 4):
        for i in range(14):
            a = (i + (y // 4) * 0.5) * 2 * math.pi / 14
            for rr in (10.0, 10.5, 11.0):
                wx, wz = math.floor(X(152) + 0.5 + rr * math.cos(a)), math.floor(27.5 + rr * math.sin(a))
                if (wx, y, wz) not in m.open and m.solid(wx, y, wz) and any(
                        (wx + ddx, y, wz + ddz) in m.open for ddx, ddz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    m.set(wx, y, wz, 'sea_lantern')
                    break
    m.sign(X(141), A + 1, 26, 'south')
    s.sign((X(141), A + 1, 26), ['Цистерна.', 'Пандус ведёт', 'вниз по стене.', 'Упал — в воду.'])
    s.say((X(138), A - 1, 18, X(162), A + 9, 37),
          'Система: «Цистерна. Храм пил отсюда, пока не захлебнулся. Пандус идёт по стене вниз на восемнадцать блоков; '
          'кто сорвётся — упадёт в воду, это не смертельно. Только стыдно.»')

    # ================================================================== Нижний зал Цистерны: засада и пузырьковый колодец
    s.pack('p5', [('drowned_guard', X(148), B, 47, 24), ('drowned_guard', X(156), B, 47, 24), ('harpooner', X(146), B, 53, 24),
                  ('deep_priest', X(158), B, 53, 24), ('crab', X(152), B, 51, 24, None, 4)])
    m.sign(X(136), B + 1, 37, 'east')
    s.sign((X(136), B + 1, 37), ['Пузырьковый', 'колодец.', 'Нырни под стекло —', 'вода унесёт наверх'])
    s.say((X(136), B - 1, 30, X(168), B + 9, 57),
          'Система: «Нижний зал Цистерны. В северной нише — пузырьковый колодец: нырните под стекло, и вода сама '
          'поднимет вас к привалу. Обратно — только пандусом.»')

    # ================================================================== Бассейн течений и подводный лаз
    s.mech('current', 'hazard', area=(X(176), B, 44, X(204), B, 57), pattern=1, stripe=2, period=2.5, warn=1.1, share=0.2, sound=0)
    m.set(X(197), B, 28, 'litrpg:secret_cache', facing='south')
    s.cache('lair', (X(197), B, 28), 3)
    for (cx, cz) in ((X(195), 28), (X(200), 28)):
        m.set(cx, B, cz, 'sea_pickle', pickles=4, waterlogged=False)
    s.pack('p6', [('moray', X(186), B, 48, 25), ('moray', X(192), B, 54, 25), ('jelly', X(198), B, 50, 25),
                  ('jelly', X(196), B, 47, 25), ('deep_priest', X(202), B, 52, 25, None, 4)])
    s.say((X(174), B - 1, 40, X(182), B + 8, 58),
          'Система: «Течения бьют полосами по очереди — идите в такт. А купель у северной стены глубже, чем кажется: '
          'вода под стеной куда-то уходит. Воздуха хватит.»')

    # ================================================================== Пещера корабля: Капитан Утопленников
    ship = {}
    for x in range(X(222), X(251)):
        t = (x - X(222)) / 28.0
        half = 4.0 if t < 0.6 else 4.0 * math.sqrt(max(0.0, 1 - ((t - 0.6) / 0.4) ** 2))    # корма прямая, нос острый
        for z in range(44, 55):
            if abs(z + 0.5 - 49.5) <= half + 0.01:
                ship[(x, z)] = half
    for (x, z), half in ship.items():
        for y in range(B - 1, B + 4):
            w = half - (B + 3 - y) * 0.6                                         # корпус сужается книзу
            if abs(z + 0.5 - 49.5) <= w + 0.01 or y == B + 3:
                edge = abs(z + 0.5 - 49.5) > w - 1
                if y == B + 3 and not edge:
                    m.set(x, y, z, 'stripped_spruce_wood', axis='x')
                else:
                    m.set(x, y, z, 'spruce_planks' if edge else 'dark_oak_planks')
    rail = [(x, B + 4, z) for (x, z) in ship
            if any((x + ddx, z + ddz) not in ship for ddx, ddz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            and not (X(235) <= x <= X(237) and z > 49)]
    for c in rail:
        m.set(*c, 'spruce_fence')
    m.connect(rail)
    for (x, y, z) in rail:                                                       # фонари на бортах
        if (x + z) % 6 == 0:
            m.lantern(x, y + 1, z)
    for (y, z) in ((B, 56), (B + 1, 55), (B + 2, 54)):                           # сходни (полные блоки — ступенью)
        for xx in (X(235), X(236), X(237)):
            for yy in range(B - 1, y + 1):
                m.set(xx, yy, z, 'spruce_planks')
    for y in range(B + 4, B + 21):                                               # мачта
        if (X(238), y, 49) in m.open:
            m.set(X(238), y, 49, 'spruce_log', axis='y')
    for x in range(X(234), X(243)):                                              # рея
        if (x, B + 12, 49) in m.open and x not in (X(237), X(238)):
            m.set(x, B + 12, 49, 'stripped_spruce_log', axis='x')
    for x in (X(234), X(242)):                                                   # фонари на концах реи
        m.chain_lantern(x, B + 11, 49, 1)
    for (bx, bz) in ((X(226), 46), (X(226), 52), (X(244), 49), (X(230), 52)):
        m.set(bx, B + 4, bz, 'barrel', facing='up', open=False)
    m.set(X(239), B + 16, 48, 'litrpg:locked_cache', facing='west')
    s.cache('captain', (X(239), B + 16, 48), 2)
    s.pack('captain', [('drowned_guard', X(232), B + 4, 49, 26), ('harpooner', X(228), B + 4, 47, 25), ('harpooner', X(228), B + 4, 51, 25)],
           title='Капитан Утопленников')
    p7 = s.pack('p7', [('crab', X(222), B, 40, 25), ('crab', X(226), B, 60, 25), ('moray', X(218), B, 54, 25),
                       ('jelly', X(220), B, 44, 25, None, 4)])
    p8 = s.pack('p8', [('drowned_guard', X(252), B, 44, 26), ('harpooner', X(254), B, 54, 26), ('deep_priest', X(250), B, 58, 26),
                       ('moray', X(253), B, 49, 26, None, 4)])
    s.gate('g3', (X(260), B, 48, X(260), B + 3, 50), open_when=[f'pack:{p7}', f'pack:{p8}'])
    for yy in (B, B + 1):
        m.set(X(232), yy, 59, 'stripped_spruce_log', axis='y')
    m.sign(X(232), B + 1, 58, 'north', wood='spruce')
    s.sign((X(232), B + 1, 58), ['«Последняя', 'надежда».', 'Ключ — у капитана.', 'Сундук — на мачте.'])
    s.say((X(206), B - 1, 34, X(258), B + 22, 64),
          'Система: «Корабль, который так и не доплыл. Капитан не отдаёт ключ — его забирают. '
          'Свой сундук он держит в «вороньем гнезде»: лестница по мачте в клетке, не сорвётесь.»')

    # ================================================================== Арена Левиафана: кольцо воды у края
    for c in moat:
        m.blocks[c] = WATER
    m.check_water(moat)
    for i in range(10):
        a = (i + 0.5) * math.pi / 5
        bx, bz = round(X(288) + 21 * math.cos(a)), round(49 + 21 * math.sin(a))
        column(m, bx, bz, B, m.ceil_y(bx, B, bz) - B + 1, 'dark_prismarine', 'prismarine_bricks', 'sea_lantern')
    b2 = s.boss('leviathan', 'leviathan', (X(288), B, 49), 21)
    s.gate('arena2', (X(263), B, 48, X(263), B + 3, 50), lock=b2)
    s.gate('g4', (X(313), B, 48, X(313), B + 3, 50), open_when=[f'boss:{b2}'])
    s.say((X(266), B - 1, 27, X(310), B + 18, 71),
          'Система: «Левиафан. Где пузыри — там он вынырнет. Где прилив — там стоять нельзя. '
          'Хвостом он бьёт широко: держитесь от него дальше, чем кажется нужным.»')

    # ================================================================== Водоворот
    m.sign(X(318), B + 1, 47, 'east')
    s.sign((X(318), B + 1, 47), ['Водоворот.', 'Нырни в середину —', 'унесёт на дно.', 'Назад пути нет.'])
    s.say((X(318), B - 1, 41, X(334), B + 9, 57),
          'Система: «Водоворот. Храм глотает воду — и тех, кто в ней. Нырните в середину: пузыри утянут вниз, '
          'на дно нижнего пруда. Отплывите вбок и всплывайте.»')

    # ================================================================== Пруд водоворота (засада)
    s.pack('p9', [('drowned_guard', X(318), C, 40, 27), ('harpooner', X(334), C, 40, 27), ('deep_priest', X(334), C, 58, 27),
                  ('moray', X(318), C, 58, 27), ('crab', X(318), C, 49, 27, None, 4)])
    s.say((X(314), C - 1, 36, X(338), C + 9, 62),
          'Система: «Библиотечный ярус. Здесь хранили то, что не тонет. Тонет всё.»')

    # ================================================================== Библиотека прилива: две галереи, вентили, загадка
    for (z1, z2, rz, pz) in ((34, 38, 38, 38), (60, 64, 60, 60)):
        for x in range(X(344), X(379)):
            for z in range(z1, z2 + 1):
                m.set(x, C + 5, z, 'dark_prismarine' if x % 4 else 'prismarine_bricks')
        m.railing([(x, C + 6, rz) for x in range(X(344), X(379))], 'prismarine_wall')
        m.ladder(X(344), (z1 + z2) // 2, C, C + 5, 'east')
        for x in range(X(348), X(377), 6):                                       # опоры галереи
            for y in range(C, C + 5):
                m.set(x, y, pz, 'prismarine_bricks')
    for z in (34, 64):                                                           # шкафы под галереями у стен
        for x in range(X(346), X(377)):
            if x % 6 == 2:
                continue
            for y in range(C, C + 3):
                if m.is_air(x, y, z):
                    m.set(x, y, z, 'bookshelf')
        for x in range(X(356), X(368)):                                          # шкафы на галереях
            if x % 5:
                for y in (C + 6, C + 7):
                    m.set(x, y, z, 'bookshelf')
    valves = []
    for (x, y, z, facing, back) in ((X(378), C + 7, 35, 'west', (X(379), C + 7, 35)),      # северная галерея, торец
                                    (X(378), C + 7, 63, 'west', (X(379), C + 7, 63)),      # южная галерея, торец
                                    (X(366), C + 1, 35, 'south', (X(366), C + 1, 34))):    # простенок под северной
        m.set(*back, 'chiseled_stone_bricks')
        m.set(x, y, z, 'lever', face='wall', facing=facing, powered=False)
        valves.append((x, y, z))
    mech_valves = s.mech('valves', 'levers', valves, text='Три вентиля')
    m.set(X(361), C, 49, 'lectern', facing='east', has_book=False, powered=False)
    for (cx, cz) in ((X(359), 47), (X(359), 51), (X(363), 47), (X(363), 51)):
        m.set(cx, C - 1, cz, 'sea_lantern')
    mech_riddle = s.mech('riddle', 'riddle', area=(X(357), C, 45, X(365), C + 3, 53),
                         answers=['вода', 'капля', 'капли', 'вода точит камень'],
                         text='Загадка библиотеки|Без рук, без ног, а камень точит. Что это?')
    for yy in (C, C + 1):
        m.set(X(361), yy, 55, 'dark_prismarine')
    m.sign(X(361), C + 1, 54, 'north')
    s.sign((X(361), C + 1, 54), ['Без рук,', 'без ног,', 'а камень', 'точит.'])
    m.sign(X(344), C + 1, 47, 'east')
    s.sign((X(344), C + 1, 47), ['Три вентиля:', 'на двух галереях', 'и под северной.', 'Откройте все.'])
    s.pack('p10', [('deep_priest', X(352), C, 44, 27), ('drowned_guard', X(354), C, 46, 27), ('harpooner', X(360), C + 6, 36, 27),
                   ('crab', X(351), C, 54, 27, None, 4)])
    p11 = s.pack('p11', [('harpooner', X(372), C, 56, 28), ('harpooner', X(370), C + 6, 62, 28), ('moray', X(368), C, 52, 28),
                         ('drowned_guard', X(372), C, 44, 28), ('crab', X(374), C, 49, 28, None, 4)])
    s.gate('g5', (X(381), C, 48, X(381), C + 3, 50), open_when=[f'mech:{mech_riddle}', f'mech:{mech_valves}', f'pack:{p11}'])
    s.say((X(344), C - 1, 34, X(378), C + 14, 64),
          'Система: «Библиотека прилива. Загадку — в чат, стоя у аналоя. Вентили — на галереях (лестницы у западной '
          'стены) и в простенке под северной галереей.»')

    # ================================================================== Затонувший неф: насыпь, пролёты, паркур к нише
    for x in range(X(388), X(414), 6):
        for z in (47, 51):
            column(m, x, z, C, m.ceil_y(x, C, z) - C + 1, 'prismarine', 'mossy_stone_bricks', 'sea_lantern')
    m.set(X(398), C + 5, 33, 'litrpg:cache', facing='south')
    s.cache('nave', (X(398), C + 5, 33), 2)
    for (cx, cz) in ((X(397), 33), (X(399), 33)):
        m.set(cx, C + 5, cz, 'sea_pickle', pickles=3, waterlogged=False)
    s.pack('p12', [('drowned_guard', X(394), C, 49, 28), ('drowned_guard', X(404), C, 49, 28), ('deep_priest', X(410), C, 48, 28),
                   ('jelly', X(400), C, 50, 28), ('harpooner', X(412), C, 50, 28, None, 4)])
    s.say((X(384), C - 1, 36, X(416), C + 14, 62),
          'Система: «Затонувший неф. Колонны рухнули в воду — их обломки торчат над ней ступенями. '
          'Кто-то по ним уже прыгал: в нише под сводом — сундук.»')

    # ================================================================== Лестница-аквариум: море по обе стороны, струи прорыва
    for z, out in ((46, -1), (52, 1)):
        cells = []
        for x in range(X(419), X(437)):
            ys = sorted(y for y in range(D - 2, C + 8) if (x, y, z - out) in m.open)
            if len(ys) >= 4:
                cells += [(x, y, z) for y in ys[1:-1] if (x, y, z) not in m.open]
        sea(m, cells, (0, 0, out), seed=z)
    k = 0
    for x in range(X(420), X(435), 2):
        fy = m.floor_y(x, 49, C + 2)
        s.mech(f'burst{k}', 'hazard', area=(x, fy, 47, x + 1, fy + 1, 51), period=2.6, warn=1.1, share=0.15,
               offset=(k % 2) * 1.3, sound=0)
        k += 1
    s.say((X(416), D - 1, 44, X(440), C + 6, 54),
          'Система: «Лестница-аквариум. Стекло держит море — пока. Из щелей бьют струи: ступени через одну, по очереди.»')

    # ================================================================== Преддверие и Зал Наяры
    s.pack('p13', [('drowned_guard', X(446), D, 45, 29), ('drowned_guard', X(446), D, 53, 29), ('deep_priest', X(452), D, 49, 29),
                   ('jelly', X(444), D, 49, 29), ('harpooner', X(452), D, 43, 29, None, 4)])
    for (bx, bz) in ((X(441), 40), (X(455), 40), (X(441), 58), (X(455), 58)):
        brazier(m, bx, D, bz, soul=True)
    for i in range(8):
        a = i * math.pi / 4 + math.pi / 8
        bx, bz = round(X(482) + 16.5 * math.cos(a)), round(49 + 16.5 * math.sin(a))
        column(m, bx, bz, D, m.ceil_y(bx, D, bz) - D + 1, 'dark_prismarine', 'sea_lantern', 'sea_lantern')
    b3 = s.boss('naiad', 'naiad', (X(482), D, 49), 19, final=True, arena2=(X(525) + 0.5, D, 49.5, 16))
    s.gate('arena3', (X(459), D, 48, X(459), D + 3, 50), lock=b3)
    s.gate('sanct', (X(505), D, 48, X(505), D + 3, 50), open_when=[f'boss:{b3}'])
    s.say((X(463), D - 1, 30, X(501), D + 18, 69),
          'Система: «Наяра, Жрица Глубин. Когда засияет свет — бейте её только из света. Песнь — сбить. '
          'А на половине сил она уйдёт под купол — туда, где над головой море.»')

    # ================================================================== Купол на дне: потолок — стекло, над ним море
    sky = [(x, D + 11, z) for x in range(X(508), X(544)) for z in range(32, 68)
           if (x, D + 10, z) in m.open and (x, D + 11, z) not in m.open]
    sea(m, sky, (0, 1, 0), deep=2, seed=77)
    for i in range(10):
        a = i * math.pi / 5
        bx, bz = round(X(525) + 12 * math.cos(a)), round(49 + 12 * math.sin(a))
        m.set(bx, D - 1, bz, 'sea_lantern')
        m.set(bx, D, bz, 'prismarine_wall', **POST)
    for dx in (-2, 2):
        for dy in range(0, 4):
            m.set(X(525 + dx), D + dy, 71, 'crying_obsidian')
    for dx in (-1, 0, 1):
        m.set(X(525 + dx), D + 3, 71, 'crying_obsidian')
        for dy in range(0, 3):
            m.set(X(525 + dx), D + dy, 71, 'litrpg:rift', active=False)
    apply_gates(m, s)

    m.hanging_lights(spacing=6, soul=False, themes=['temple', 'library'], min_h=7, chain=2)
    m.glow(spacing=4, level=9, height=3)

    s.box = (X(-6), D - 12, -6, X(556), A + 24, 80)
    s.camp = (X(8.5), A, 30.5, -90)
    s.trader = (X(4.5), A, 24.5, -90)
    s.rift = (X(524), D, 71, X(526), D + 2, 71)
    return m, s
