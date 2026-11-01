"""
Этап 3 · Затопленный храм (уровни 20–30). Призмарин, морские фонари, вода по щиколотку, водопады.

Путь: Притвор (привал) → Залитая галерея → Зал колонн (плиты на разделение) → Арена Стража Прилива (колонны-укрытия) →
Бассейн течений (секретка за водопадом) → Обломки корабля (Капитан Утопленников) → Арена Левиафана →
Библиотека прилива (загадка «сон» и три вентиля) → Преддверие святилища → Зал Наяры → Святилище на дне (разлом).
"""
import math

from decor import column
from kit import Map, Stage, Theme, apply_gates

N = 3
OX = 1024 * N
FY = 64


def X(x):
    return OX + x


def water_channel(m, x1, z1, x2, z2, y):
    """Канал: вода по щиколотку (на уровне ног), дно — тёмный призмарин."""
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for z in range(min(z1, z2), max(z1, z2) + 1):
            m.set(x, y - 1, z, 'dark_prismarine')
            m.set(x, y, z, 'water', level=0)


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
                         floor=[('dark_prismarine', 3), ('sea_lantern', 1)], ceil='dark_prismarine', seed=34))
    s = Stage(N, 'temple', 'Затопленный храм', (20, 30), 'temple')

    # ------------------------------------------------------------------ пустоты
    m.room(X(0), 20, X(16), 34, FY, 7, temple, vault=2)                         # Притвор
    m.corridor([(X(16), 27), (X(22), 27)], 3, 4, temple, y=FY)
    m.room(X(22), 18, X(58), 36, FY, 8, temple, vault=4)                        # Залитая галерея
    m.corridor([(X(58), 27), (X(64), 27)], 3, 4, temple, y=FY)
    m.room(X(64), 14, X(88), 40, FY, 9, temple, vault=3)                        # Зал колонн
    m.room(X(73), 5, X(79), 13, FY, 5, temple)                                  # северная ниша плиты
    m.room(X(73), 41, X(79), 49, FY, 5, temple)                                 # южная ниша плиты
    m.corridor([(X(88), 27), (X(94), 27)], 3, 4, temple, y=FY)
    m.round_room(X(111), 27, 16, FY, 10, temple, dome=4)                        # Арена Стража Прилива
    m.corridor([(X(127), 27), (X(132), 27)], 3, 4, sunk, y=FY)
    m.room(X(132), 18, X(160), 36, FY, 8, sunk, vault=3)                        # Бассейн течений
    m.room(X(143), 9, X(149), 15, FY, 4, sunk)                                  # секретка за водопадом
    m.corridor([(X(146), 15), (X(146), 18)], 2, 3, sunk, y=FY)
    m.corridor([(X(160), 27), (X(166), 27), (X(166), 18)], 3, 4, sunk, y=FY)
    m.cave(X(177), FY + 6, 15, 12, 7, 9, sunk, rough=0.15, seed=9, floor=FY)    # Обломки корабля
    m.corridor([(X(188), 15), (X(192), 15), (X(192), 27), (X(194), 27)], 3, 4, sunk, y=FY)
    m.round_room(X(211), 27, 17, FY, 11, temple, dome=4)                        # Арена Левиафана
    m.corridor([(X(228), 27), (X(236), 27)], 3, 4, library, y=FY)
    m.room(X(236), 16, X(258), 38, FY, 8, library, vault=2)                     # Библиотека прилива
    m.room(X(240), 6, X(246), 15, FY, 5, library)                               # ниша вентиля
    m.room(X(250), 39, X(256), 47, FY, 5, library)                              # ниша вентиля
    m.corridor([(X(258), 27), (X(264), 27)], 3, 4, library, y=FY)
    m.room(X(264), 20, X(274), 34, FY, 7, deep, vault=2)                        # Преддверие святилища
    m.corridor([(X(274), 27), (X(278), 27)], 3, 4, deep, y=FY)
    m.round_room(X(294), 27, 15, FY, 10, deep, dome=5)                          # Зал Наяры
    m.corridor([(X(294), 42), (X(294), 52)], 3, 4, deep, y=FY)
    m.round_room(X(294), 66, 14, FY, 12, deep, dome=6)                          # Святилище на дне
    m.room(X(291), 79, X(297), 84, FY, 6, deep)                                 # ниша разлома
    m.shell(2)

    # ------------------------------------------------------------------ Притвор
    for (lx, lz) in ((X(2), 22), (X(2), 32), (X(14), 22), (X(14), 32)):
        m.set(lx, FY, lz, 'sea_lantern')
    m.set(X(8), FY, 27, 'soul_campfire', facing='north', lit=True, signal_fire=False, waterlogged=False)
    m.sign(X(0), FY + 1, 27, 'east')
    s.sign((X(0), FY + 1, 27), ['ЭТАП 3', 'Затопленный', 'храм', 'уровни 20–30'])
    s.say((X(0), FY - 1, 20, X(16), FY + 8, 34), 'Система: «Храм ушёл под воду вместе с теми, кто молился. Они до сих пор молятся.»')

    # ------------------------------------------------------------------ Залитая галерея
    water_channel(m, X(23), 26, X(57), 28, FY)
    for x in range(X(26), X(56), 6):
        for z in (21, 33):
            column(m, x, z, FY, m.ceil_y(x, FY, z) - FY + 1, 'prismarine_bricks', 'dark_prismarine', 'sea_lantern')
    s.pack('p1', [('drowned_guard', X(32), FY, 24, 21), ('harpooner', X(36), FY, 31, 21), ('drowned_guard', X(33), FY, 30, 21, None, 4)])
    s.pack('p2', [('jelly', X(44), FY, 22, 21), ('jelly', X(45), FY, 24, 21), ('jelly', X(46), FY, 22, 21), ('deep_priest', X(47), FY, 31, 21),
                  ('jelly', X(44), FY, 32, 21, None, 5)])
    s.pack('p3', [('crab', X(53), FY, 23, 22), ('crab', X(54), FY, 31, 22), ('harpooner', X(56), FY, 20, 22, None, 4)])
    s.say((X(22), FY - 1, 18, X(58), FY + 8, 36), 'Система: «Утопленники держат щиты перед собой. Их жрецы держат утопленников. Начинайте с жрецов.»')

    # ------------------------------------------------------------------ Зал колонн (плиты)
    for x in range(X(67), X(87), 5):
        for z in (17, 37):
            column(m, x, z, FY, m.ceil_y(x, FY, z) - FY + 1, 'prismarine', 'dark_prismarine', 'sea_lantern')
    plates = [(X(76), FY, 7), (X(76), FY, 47), (X(86), FY, 27)]
    for (px, py, pz) in plates:
        m.set(px, py, pz, 'heavy_weighted_pressure_plate', power=0)
        m.set(px, py - 1, pz, 'sea_lantern')
    mech_plates = s.mech('plates', 'plates', plates, text='Плиты трёх приливов', hold=3)
    m.sign(X(64), FY + 1, 24, 'east')
    s.sign((X(64), FY + 1, 24), ['Три прилива', 'приходят', 'вместе. Встаньте', 'на все плиты.'])
    s.pack('p4', [('drowned_guard', X(74), FY, 22, 22), ('deep_priest', X(80), FY, 30, 22), ('moray', X(77), FY, 33, 22),
                  ('moray', X(72), FY, 30, 22, None, 4)])
    s.gate('g1', (X(91), FY, 26, X(91), FY + 3, 28), open_when=[f'mech:{mech_plates}', 'pack:s3_p4'])

    # ------------------------------------------------------------------ Арена Стража Прилива: четыре колонны-укрытия
    for dx in (-7, 7):
        for dz in (-7, 7):
            for ox in range(-1, 2):
                for oz in range(-1, 2):
                    top = m.ceil_y(X(111 + dx + ox), FY, 27 + dz + oz)
                    for y in range(FY, top + 1):
                        m.set(X(111 + dx + ox), y, 27 + dz + oz, 'prismarine_bricks' if (y + ox + oz) % 4 else 'sea_lantern')
    b1 = s.boss('tide', 'tide_warden', (X(111), FY, 27), 16)
    s.gate('arena1', (X(93), FY, 26, X(93), FY + 3, 28), lock=b1)
    s.gate('g2', (X(130), FY, 26, X(130), FY + 3, 28), open_when=[f'boss:{b1}'])
    s.say((X(94), FY - 1, 11, X(127), FY + 10, 43), 'Система: «Страж Прилива. Когда пойдёт волна — встаньте так, чтобы колонна была между вами и водой.»')

    # ------------------------------------------------------------------ Бассейн течений
    s.mech('current', 'hazard', area=(X(134), FY, 19, X(158), FY, 35), pattern=1, stripe=2, period=2.5, warn=1.1, share=0.22, sound=0)
    water_channel(m, X(133), 19, X(159), 20, FY)
    water_channel(m, X(133), 34, X(159), 35, FY)
    # водопад перед секреткой
    for y in range(FY, FY + 3):
        for x in (X(145), X(146)):
            m.set(x, y, 17, 'water', level=0)
    m.set(X(146), FY, 10, 'litrpg:secret_cache', facing='south')
    s.cache('waterfall', (X(146), FY, 10), 3)
    s.pack('p5', [('moray', X(156), FY, 22, 25), ('moray', X(157), FY, 31, 25), ('jelly', X(154), FY, 27, 25), ('jelly', X(155), FY, 25, 25),
                  ('deep_priest', X(158), FY, 27, 25, None, 4)])
    s.say((X(132), FY - 1, 18, X(140), FY + 8, 36), 'Система: «Течения бьют полосами по очереди. А вода с севера шумит не так, как остальная.»')

    # ------------------------------------------------------------------ Обломки корабля: Капитан Утопленников
    for x in range(X(170), X(185)):
        for z in (10, 20):
            m.set(x, FY, z, 'spruce_planks')
            m.set(x, FY + 1, z, 'spruce_fence', north=False, south=False, east=True, west=True, waterlogged=False)
    for z in range(10, 21):
        m.set(X(170), FY, z, 'spruce_planks')
    m.set(X(183), FY + 1, 15, 'litrpg:locked_cache', facing='west')
    m.set(X(183), FY, 15, 'spruce_planks')
    s.cache('captain', (X(183), FY + 1, 15), 2)
    s.pack('captain', [('drowned_guard', X(178), FY, 15, 26), ('harpooner', X(175), FY, 12, 25), ('harpooner', X(175), FY, 18, 25)],
           title='Капитан Утопленников')
    s.say((X(165), FY - 1, 6, X(190), FY + 12, 25), 'Система: «Корабль, который так и не доплыл. Капитан не отдаёт ключ — его забирают.»')

    # ------------------------------------------------------------------ Арена Левиафана: кольцо воды у края
    for i in range(360):
        a = math.radians(i)
        for rr in (15, 16):
            x, z = round(X(211) + rr * math.cos(a)), round(27 + rr * math.sin(a))
            if (x, FY, z) in m.open:
                m.set(x, FY - 1, z, 'dark_prismarine')
                m.set(x, FY, z, 'water', level=0)
    b2 = s.boss('leviathan', 'leviathan', (X(211), FY, 27), 17)
    s.gate('arena2', (X(194), FY, 26, X(194), FY + 3, 28), lock=b2)
    s.gate('g3', (X(232), FY, 26, X(232), FY + 3, 28), open_when=[f'boss:{b2}'])
    s.say((X(194), FY - 1, 10, X(228), FY + 11, 44), 'Система: «Левиафан. Где пузыри — там он вынырнет. Где прилив — там стоять нельзя.»')

    # ------------------------------------------------------------------ Библиотека прилива
    for z in range(18, 37, 3):
        for x in (X(238), X(256)):
            for y in range(FY, FY + 3):
                m.set(x, y, z, 'bookshelf')
    valves = [(X(243), FY + 1, 7), (X(253), FY + 1, 46), (X(237), FY + 1, 27)]
    m.set(X(243), FY + 1, 6, 'dark_prismarine')
    m.set(X(243), FY + 1, 7, 'lever', face='wall', facing='south', powered=False)
    m.set(X(253), FY + 1, 47, 'dark_prismarine')
    m.set(X(253), FY + 1, 46, 'lever', face='wall', facing='north', powered=False)
    m.set(X(236), FY + 1, 27, 'dark_prismarine')
    m.set(X(237), FY + 1, 27, 'lever', face='wall', facing='east', powered=False)
    mech_valves = s.mech('valves', 'levers', valves, text='Три вентиля')
    m.set(X(247), FY, 27, 'lectern', facing='east', has_book=False, powered=False)
    mech_riddle = s.mech('riddle', 'riddle', area=(X(244), FY, 23, X(250), FY + 3, 31), answers=['сон', 'сны', 'сновидение'],
                         text='Загадка библиотеки|Что можно увидеть с закрытыми глазами?')
    m.sign(X(247), FY + 1, 32, 'north')
    s.sign((X(247), FY + 1, 32), ['Что можно', 'увидеть', 'с закрытыми', 'глазами?'])
    m.sign(X(258), FY + 1, 20, 'west')
    s.sign((X(258), FY + 1, 20), ['Три вентиля', 'спрятаны', 'в нишах. Откройте', 'все три.'])
    s.pack('p6', [('deep_priest', X(242), FY, 20, 28), ('drowned_guard', X(244), FY, 22, 28), ('crab', X(241), FY, 25, 28, None, 4)])
    s.pack('p7', [('harpooner', X(252), FY, 34, 28), ('harpooner', X(254), FY, 31, 28), ('moray', X(250), FY, 35, 28),
                  ('crab', X(253), FY, 22, 28)])
    s.cache('library', (X(255), FY, 37), 1)
    m.set(X(255), FY, 37, 'litrpg:cache', facing='north')
    s.gate('g4', (X(261), FY, 26, X(261), FY + 3, 28), open_when=[f'mech:{mech_riddle}', f'mech:{mech_valves}', 'pack:s3_p7'])
    s.say((X(236), FY - 1, 16, X(258), FY + 8, 38), 'Система: «Библиотека прилива. Загадку — в чат, у аналоя. Вентили — в нишах.»')

    # ------------------------------------------------------------------ Преддверие святилища и Зал Наяры
    s.pack('p8', [('drowned_guard', X(268), FY, 24, 29), ('drowned_guard', X(268), FY, 30, 29), ('deep_priest', X(272), FY, 27, 29),
                  ('jelly', X(266), FY, 27, 29), ('jelly', X(270), FY, 27, 29, None, 4)])
    for i in range(8):
        a = i * math.pi / 4
        bx, bz = round(X(294) + 12 * math.cos(a)), round(27 + 12 * math.sin(a))
        column(m, bx, bz, FY, m.ceil_y(bx, FY, bz) - FY + 1, 'dark_prismarine', 'sea_lantern', 'sea_lantern')
    for i in range(10):
        a = i * math.pi / 5
        bx, bz = round(X(294) + 11 * math.cos(a)), round(66 + 11 * math.sin(a))
        m.set(bx, FY, bz, 'sea_lantern')
        m.set(bx, FY + 1, bz, 'prismarine_wall', up=True, north='none', south='none', east='none', west='none', waterlogged=False)
    b3 = s.boss('naiad', 'naiad', (X(294), FY, 27), 15, final=True, arena2=(X(294) + 0.5, FY, 66.5, 14))
    s.gate('arena3', (X(277), FY, 26, X(277), FY + 3, 28), lock=b3)
    s.gate('sanct', (X(293), FY, 48, X(295), FY + 3, 48), open_when=[f'boss:{b3}'])
    s.say((X(278), FY - 1, 12, X(310), FY + 10, 42), 'Система: «Наяра, Жрица Глубин. Когда засияет свет — бейте её только из света. Песнь — сбить.»')
    for dx in (-2, 2):
        for dy in range(0, 4):
            m.set(X(294 + dx), FY + dy, 82, 'crying_obsidian')
    for dx in (-1, 0, 1):
        m.set(X(294 + dx), FY + 3, 82, 'crying_obsidian')
        for dy in range(0, 3):
            m.set(X(294 + dx), FY + dy, 82, 'litrpg:rift', active=False)
    apply_gates(m, s)

    m.hanging_lights(spacing=6, soul=False, themes=['temple', 'library'], min_h=6, chain=2)
    m.glow(spacing=4, level=9, height=3)

    s.box = (X(-4), FY - 10, -4, X(314), FY + 24, 92)
    s.camp = (X(8.5), FY, 30.5, -90)
    s.trader = (X(4.5), FY, 24.5, -90)
    s.rift = (X(293), FY, 82, X(295), FY + 2, 82)
    return m, s
