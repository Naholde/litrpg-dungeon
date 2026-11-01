"""
Этап 5 · Шпиль Бездны (уровни 40–50). Пурпур, обсидиан, аметист, мосты над пустотой.

Путь: Последний привал → Мост осколков над бездной → Обсерватория (загадка памяти) → Зал отражений (Двойник) →
Сад искажений (вспышки пустоты; божественная секретка: загадка за треснувшей стеной) → Звёздная кладовая (Пожиратель
Звёзд) → Часовня времени (Хронофаг) → Лестница в небо (плиты на трёх балконах) → Преддверие пустоты →
Вершина (Ксалатар) → Сердце Бездны (последний разлом — конец подземелья).
"""
import math

from decor import brazier, column
from kit import Map, Stage, Theme, apply_gates

N = 5
OX = 1024 * N
FY = 64


def X(x):
    return OX + x


def bridge(m, a, b, y, w=3, block='purpur_block', edge='purpur_pillar'):
    """Мост над пустотой из точки a в b (x, z) шириной w; края — другим блоком."""
    (ax, az), (bx, bz) = a, b
    n = max(abs(bx - ax), abs(bz - az), 1)
    for i in range(n + 1):
        x, z = round(ax + (bx - ax) * i / n), round(az + (bz - az) * i / n)
        for dx in range(-(w // 2), w - w // 2):
            for dz in range(-(w // 2), w - w // 2):
                edge_cell = (abs(dx) == w // 2 or abs(dz) == w // 2) and w > 2
                m.set(x + dx, y - 1, z + dz, edge if edge_cell else block)


def platform(m, cx, cz, r, y, block='end_stone_bricks', rim='purpur_block'):
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            d = math.hypot(dx, dz)
            if d <= r + 0.3:
                m.set(cx + dx, y - 1, cz + dz, rim if d > r - 1 else block)


def build(_n=N):
    m = Map(seed=5005)
    spire = m.theme(Theme('spire', wall=[('purpur_block', 3), ('end_stone_bricks', 3), ('obsidian', 1)],
                          floor=[('end_stone_bricks', 3), ('purpur_block', 2)], ceil=[('purpur_block', 2), ('obsidian', 1)], seed=51))
    void = m.theme(Theme('void', wall=[('obsidian', 3), ('crying_obsidian', 1), ('purpur_block', 1)],
                         floor='obsidian', ceil=[('obsidian', 2), ('crying_obsidian', 1)], seed=52))
    crystal = m.theme(Theme('crystal', wall=[('amethyst_block', 2), ('calcite', 2), ('purpur_block', 1)],
                            floor=[('calcite', 2), ('amethyst_block', 1), ('end_stone_bricks', 2)], ceil='amethyst_block', seed=53))
    s = Stage(N, 'spire', 'Шпиль Бездны', (40, 50), 'spire')

    # ------------------------------------------------------------------ пустоты
    m.room(X(0), 20, X(16), 34, FY, 7, spire, vault=2)                          # Последний привал
    m.corridor([(X(16), 27), (X(22), 27)], 3, 4, spire, y=FY)
    m.room(X(22), 10, X(64), 44, FY, 12, void, vault=4)                         # Мост осколков
    m.pit(X(23), 11, X(63), 43, FY, 16, void)
    m.corridor([(X(64), 27), (X(70), 27)], 3, 4, crystal, y=FY)
    m.round_room(X(82), 27, 12, FY, 9, crystal, dome=4)                         # Обсерватория
    m.corridor([(X(94), 27), (X(100), 27)], 3, 4, spire, y=FY)
    m.round_room(X(116), 27, 15, FY, 10, spire, dome=5)                         # Зал отражений
    m.corridor([(X(131), 27), (X(137), 27)], 3, 4, void, y=FY)
    m.room(X(137), 14, X(165), 40, FY, 9, void, vault=3)                        # Сад искажений
    m.room(X(146), 2, X(156), 10, FY, 5, crystal)                               # тайная келья с загадкой
    m.corridor([(X(151), 10), (X(151), 13)], 2, 3, crystal, y=FY)
    m.room(X(160), 2, X(166), 8, FY, 4, crystal)                                # божественный тайник
    m.corridor([(X(156), 5), (X(160), 5)], 2, 3, crystal, y=FY)
    m.corridor([(X(165), 27), (X(170), 27)], 3, 4, spire, y=FY)
    m.room(X(170), 16, X(190), 38, FY, 8, crystal, vault=3)                     # Звёздная кладовая
    m.corridor([(X(190), 27), (X(196), 27)], 3, 4, spire, y=FY)
    m.round_room(X(213), 27, 16, FY, 11, spire, dome=5)                         # Часовня времени
    m.corridor([(X(229), 27), (X(236), 27)], 3, 4, spire, y=FY)
    m.room(X(236), 12, X(262), 42, FY, 16, void, vault=3)                       # Лестница в небо
    m.corridor([(X(262), 27), (X(268), 27)], 3, 4, void, y=FY)
    m.room(X(268), 20, X(280), 34, FY, 7, void, vault=2)                        # Преддверие пустоты
    m.corridor([(X(280), 27), (X(284), 27)], 3, 4, void, y=FY)
    m.round_room(X(302), 27, 18, FY, 12, void, dome=6)                          # Вершина
    m.corridor([(X(302), 45), (X(302), 55)], 3, 4, void, y=FY)
    m.round_room(X(302), 70, 14, FY, 14, crystal, dome=7)                       # Сердце Бездны
    m.room(X(299), 84, X(305), 89, FY, 6, void)                                 # последний разлом
    m.shell(2)

    # ------------------------------------------------------------------ Последний привал
    m.set(X(8), FY, 27, 'soul_campfire', facing='north', lit=True, signal_fire=False, waterlogged=False)
    for (lx, lz) in ((X(2), 22), (X(2), 32), (X(14), 22), (X(14), 32)):
        m.set(lx, FY, lz, 'end_rod', facing='up')
    m.sign(X(16), FY + 1, 22, 'west')
    s.sign((X(16), FY + 1, 22), ['ЭТАП 5', 'Шпиль Бездны', 'уровни 40–50', ''])
    s.say((X(0), FY - 1, 20, X(16), FY + 8, 34), 'Система: «Последний этап. Дальше Система не видит. Дальше — то, что её создало.»')

    # ------------------------------------------------------------------ Мост осколков: платформы над пустотой
    plats = [(X(32), 18), (X(44), 36), (X(56), 20)]
    bridge(m, (X(22), 27), plats[0], FY)
    bridge(m, plats[0], plats[1], FY)
    bridge(m, plats[1], plats[2], FY)
    bridge(m, plats[2], (X(64), 27), FY)
    for (px, pz) in plats:
        platform(m, px, pz, 4, FY)
    s.pack('p1', [('void_sentinel', X(32), FY, 18, 41), ('warped_archer', X(33), FY, 20, 41), ('shard', X(31), FY, 17, 41),
                  ('shard', X(30), FY, 19, 41, None, 4)])
    s.pack('p2', [('wanderer', X(44), FY, 36, 42), ('wanderer', X(45), FY, 34, 42), ('light_eater', X(43), FY, 37, 42),
                  ('warped_archer', X(46), FY, 37, 42, None, 4)])
    s.pack('p3', [('colossus', X(56), FY, 20, 42), ('shard', X(55), FY, 22, 42), ('shard', X(57), FY, 22, 42), ('shard', X(56), FY, 18, 42),
                  ('light_eater', X(57), FY, 19, 42, None, 5)])
    s.say((X(22), FY - 1, 10, X(30), FY + 12, 44), 'Система: «Мост над бездной. Упавших Система ещё вытаскивает. Пока.»')

    # ------------------------------------------------------------------ Обсерватория (память)
    pillars = []
    for i in range(6):
        a = i * math.pi / 3
        px, pz = round(X(82) + 8 * math.cos(a)), round(27 + 8 * math.sin(a))
        m.set(px, FY, pz, 'calcite')
        m.set(px, FY + 1, pz, 'amethyst_block')
        m.set(px, FY + 2, pz, 'amethyst_cluster', facing='up', waterlogged=False)
        pillars.append((px, FY + 1, pz))
    m.set(X(82), FY, 27, 'end_stone_bricks')
    m.set(X(82), FY + 1, 27, 'polished_blackstone_button', face='floor', facing='north', powered=False)
    mech_mem = s.mech('stars', 'memory', [(X(82), FY + 1, 27)] + pillars, text='Звёздная карта', rounds=3)
    s.pack('p4', [('void_sentinel', X(76), FY, 24, 43), ('light_eater', X(86), FY, 32, 43), ('wanderer', X(80), FY, 34, 43, None, 4)])
    s.gate('g1', (X(97), FY, 26, X(97), FY + 3, 28), open_when=[f'mech:{mech_mem}', 'pack:s5_p4'])
    s.say((X(70), FY - 1, 15, X(94), FY + 9, 39), 'Система: «Звёзды вспыхнут по очереди. Кнопка — в центре. Три раунда, и каждый длиннее.»')

    # ------------------------------------------------------------------ Зал отражений (Двойник)
    for i in range(10):
        a = i * math.pi / 5
        bx, bz = round(X(116) + 13 * math.cos(a)), round(27 + 13 * math.sin(a))
        column(m, bx, bz, FY, m.ceil_y(bx, FY, bz) - FY + 1, 'purpur_pillar', 'obsidian', 'end_rod')
    b1 = s.boss('doppel', 'doppel', (X(116), FY, 27), 15)
    s.gate('arena1', (X(99), FY, 26, X(99), FY + 3, 28), lock=b1)
    s.gate('g2', (X(134), FY, 26, X(134), FY + 3, 28), open_when=[f'boss:{b1}'])
    s.say((X(100), FY - 1, 12, X(131), FY + 10, 42), 'Система: «Двойник. Каждому — своя тень. Бейте свою. Когда он поднимет зеркало — опустите оружие.»')

    # ------------------------------------------------------------------ Сад искажений
    for i, (hx, hz) in enumerate(((X(142), 18), (X(150), 32), (X(158), 18), (X(146), 26), (X(156), 30))):
        s.mech(f'burst{i}', 'hazard', area=(hx - 2, FY, hz - 2, hx + 2, FY, hz + 2), period=4.0, warn=1.4, share=0.25, offset=i * 0.8, sound=1)
    s.pack('p5', [('colossus', X(150), FY, 22, 44), ('warped_archer', X(160), FY, 36, 44), ('warped_archer', X(162), FY, 20, 44),
                  ('light_eater', X(155), FY, 24, 44, None, 4)])
    s.pack('p6', [('shard', X(144), FY, 36, 44), ('shard', X(146), FY, 37, 44), ('shard', X(142), FY, 37, 44), ('wanderer', X(145), FY, 34, 44),
                  ('shard', X(143), FY, 34, 44, None, 5)])
    # божественная секретка: треснувшая стена → келья с загадкой → тайник
    for y in range(FY, FY + 2):
        for x in (X(150), X(151)):
            m.set(x, y, 12, 'infested_stone')
            m.set(x, y, 13, 'infested_stone')
    m.sign(X(151), FY + 1, 2, 'south')
    s.sign((X(151), FY + 1, 2), ['Я всегда', 'впереди, но меня', 'никогда не', 'догнать.'])
    mech_secret = s.mech('vault', 'riddle', area=(X(147), FY, 3, X(155), FY + 3, 9), answers=['будущее', 'завтра', 'горизонт'],
                         text='Загадка кельи|Я всегда впереди, но меня никогда не догнать. Что я?')
    s.gate('vault', (X(158), FY, 4, X(158), FY + 2, 5), open_when=[f'mech:{mech_secret}'])
    m.set(X(164), FY, 5, 'litrpg:divine_cache', facing='west')
    s.cache('divine_spire', (X(164), FY, 5), 4)
    s.say((X(137), FY - 1, 14, X(165), FY + 9, 40), 'Система: «Сад искажений. Вспышки пустоты — по кругу. И где-то здесь стена дышит.»')

    # ------------------------------------------------------------------ Звёздная кладовая
    for z in range(18, 38, 4):
        for x in (X(172), X(188)):
            m.set(x, FY, z, 'amethyst_block')
            m.set(x, FY + 1, z, 'amethyst_cluster', facing='up', waterlogged=False)
    s.pack('eater', [('colossus', X(180), FY, 27, 46), ('shard', X(177), FY, 24, 45), ('shard', X(183), FY, 30, 45),
                     ('light_eater', X(180), FY, 33, 45)], title='Пожиратель Звёзд')
    m.set(X(186), FY, 36, 'litrpg:locked_cache', facing='north')
    s.cache('stars', (X(186), FY, 36), 2)

    # ------------------------------------------------------------------ Часовня времени (Хронофаг)
    for i in range(12):
        a = i * math.pi / 6
        bx, bz = round(X(213) + 14 * math.cos(a)), round(27 + 14 * math.sin(a))
        m.set(bx, FY, bz, 'gold_block' if i % 3 == 0 else 'end_stone_bricks')
        m.set(bx, FY + 1, bz, 'end_rod', facing='up')
    b2 = s.boss('chrono', 'chronophage', (X(213), FY, 27), 16)
    s.gate('arena2', (X(196), FY, 26, X(196), FY + 3, 28), lock=b2)
    s.gate('g3', (X(233), FY, 26, X(233), FY + 3, 28), open_when=[f'boss:{b2}'])
    s.say((X(196), FY - 1, 10, X(229), FY + 11, 44), 'Система: «Хронофаг. Часы — разбить. После отката — сразу с метки.»')

    # ------------------------------------------------------------------ Лестница в небо: три балкона с плитами
    balconies = [(X(240), 16, FY + 4), (X(258), 16, FY + 8), (X(249), 38, FY + 12)]
    plates = []
    for (bx, bz, by) in balconies:
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                for y in range(FY, by):
                    m.set(bx + dx, y, bz + dz, 'purpur_block' if y < by - 1 else 'end_stone_bricks')
        m.set(bx, by, bz, 'polished_blackstone_pressure_plate', powered=False)
        plates.append((bx, by, bz))
    # лестницы к балконам: ступень высотой 1 на каждый блок
    def stairs_up(cells):
        for i, (x, z) in enumerate(cells):
            for w in (-1, 0, 1):
                xx, zz = (x, z + w) if cells[0][1] == cells[-1][1] else (x + w, z)
                for y in range(FY, FY + i + 1):
                    m.set(xx, y, zz, 'purpur_block' if y < FY + i else 'end_stone_bricks')
    stairs_up([(X(247 - k), 16) for k in range(4)])          # к балкону 1 (верх FY+4), с востока
    stairs_up([(X(258), 27 - k) for k in range(8)])          # к балкону 2 (верх FY+8), с юга
    stairs_up([(X(249), 23 + k) for k in range(12)])         # к балкону 3 (верх FY+12), с севера
    mech_plates = s.mech('balconies', 'plates', plates, text='Три балкона', hold=3)
    s.pack('p7', [('void_sentinel', X(244), FY, 26, 47), ('colossus', X(252), FY, 30, 47), ('light_eater', X(246), FY, 28, 47),
                  ('warped_archer', X(256), FY, 32, 47, None, 4)])
    s.gate('g4', (X(265), FY, 26, X(265), FY + 3, 28), open_when=[f'mech:{mech_plates}', 'pack:s5_p7'])
    s.cache('stairs', (X(260), FY, 40), 1)
    m.set(X(260), FY, 40, 'litrpg:cache', facing='north')
    s.say((X(236), FY - 1, 12, X(262), FY + 15, 42), 'Система: «Три балкона на разной высоте. Наверх по пандусам — и на плиты разом.»')

    # ------------------------------------------------------------------ Преддверие пустоты и Вершина
    s.pack('p8', [('void_sentinel', X(272), FY, 24, 48), ('void_sentinel', X(272), FY, 30, 48), ('light_eater', X(277), FY, 27, 48),
                  ('wanderer', X(270), FY, 27, 48), ('warped_archer', X(278), FY, 23, 48, None, 4)])
    for i in range(12):
        a = i * math.pi / 6
        bx, bz = round(X(302) + 16 * math.cos(a)), round(27 + 16 * math.sin(a))
        column(m, bx, bz, FY, m.ceil_y(bx, FY, bz) - FY + 1, 'obsidian', 'crying_obsidian', 'end_rod')
    for i in range(8):
        a = i * math.pi / 4 + 0.2
        bx, bz = round(X(302) + 11 * math.cos(a)), round(70 + 11 * math.sin(a))
        m.set(bx, FY, bz, 'amethyst_block')
        m.set(bx, FY + 1, bz, 'amethyst_cluster', facing='up', waterlogged=False)
    brazier(m, X(302), FY, 58, soul=True)
    b3 = s.boss('xalathar', 'xalathar', (X(302), FY, 27), 18, final=True, arena2=(X(302) + 0.5, FY, 70.5, 13))
    s.gate('arena3', (X(283), FY, 26, X(283), FY + 3, 28), lock=b3)
    s.gate('heart', (X(301), FY, 50, X(303), FY + 3, 50), open_when=[f'boss:{b3}'])
    s.say((X(284), FY - 1, 9, X(320), FY + 12, 45), 'Система: «Ксалатар, Голод Бездны. Самого раненого он съест — держите всех полными. Край рушится — к центру.»')
    for dx in (-2, 2):
        for dy in range(0, 4):
            m.set(X(302 + dx), FY + dy, 87, 'crying_obsidian')
    for dx in (-1, 0, 1):
        m.set(X(302 + dx), FY + 3, 87, 'crying_obsidian')
        for dy in range(0, 3):
            m.set(X(302 + dx), FY + dy, 87, 'litrpg:rift', active=False)
    apply_gates(m, s)

    m.hanging_lights(spacing=7, soul=True, themes=['spire', 'crystal'], min_h=6, chain=2)
    m.glow(spacing=4, level=9, height=3)

    s.box = (X(-4), FY - 20, -4, X(324), FY + 28, 94)
    s.camp = (X(8.5), FY, 30.5, -90)
    s.trader = (X(5.5), FY, 24.5, -90)
    s.rift = (X(301), FY, 87, X(303), FY + 2, 87)
    return m, s
