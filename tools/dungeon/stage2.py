"""
Этап 2 · Паучья чаща (уровни 10–20). Подземный лес: мох, корни, гигантские грибы, паутина, светящиеся споры.

Путь: Грибная поляна (привал) → Тропа ткачей → Паучьи ясли (секретка за осыпью) → Роща светлячков (загадка памяти) →
Логово Аракны → Корневой мост над пропастью (споровые вспышки) → Логово ведьм (Старая Ягиня, загадка «ветер») →
Арена Гнилого Древня → Сад гнили (грибы по росту) → Поляна Морены → Изнанка (второй зал, разлом).
"""
import math

from decor import brazier, cobwebs
from kit import Map, Stage, Theme, apply_gates, h3

N = 2
OX = 1024 * N
FY = 64


def X(x):
    return OX + x


def mushroom(m, x, y, z, h, cap='red_mushroom_block', r=2):
    """Гигантский гриб: ножка и шляпка (вниз по краю)."""
    for yy in range(y, y + h):
        m.set(x, yy, z, 'mushroom_stem', up=False, down=False, north=True, south=True, east=True, west=True)
    top = y + h
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            if abs(dx) == r and abs(dz) == r:
                continue
            m.set(x + dx, top, z + dz, cap, up=True, down=False, north=True, south=True, east=True, west=True)
            if abs(dx) == r or abs(dz) == r:
                m.set(x + dx, top - 1, z + dz, cap, up=False, down=True, north=True, south=True, east=True, west=True)
    m.set(x, top - 1, z, 'shroomlight')


def forest_floor(m, box, seed=0):
    """Ковры мха, папоротник, трава, азалии на полу пещеры."""
    x1, z1, x2, z2, y = box
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            if not m.is_air(x, y, z) or (x, y, z) not in m.open or not m.solid(x, y - 1, z):
                continue
            k = h3(x, y, z, seed)
            if k < 0.10:
                m.set(x, y, z, 'moss_carpet')
            elif k < 0.14:
                m.set(x, y, z, 'fern')
            elif k < 0.17:
                m.set(x, y, z, 'short_grass')
            elif k < 0.18:
                m.set(x, y, z, 'azalea')


def ceiling_life(m, box, seed=0):
    """Под сводами: свисающие корни, цветы спор, светящиеся грибы в потолке."""
    x1, z1, x2, z2, y = box
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            if (x, y, z) not in m.open:
                continue
            top = m.ceil_y(x, y, z)
            if not m.solid(x, top + 1, z) or not m.is_air(x, top, z):
                continue
            k = h3(x, top, z, seed + 3)
            if k < 0.05:
                m.set(x, top, z, 'hanging_roots', waterlogged=False)
            elif k < 0.065:
                m.set(x, top, z, 'spore_blossom')
            elif k < 0.085:
                m.set(x, top + 1, z, 'shroomlight')


def build(_n=N):
    m = Map(seed=2002)
    forest = m.theme(Theme('forest', wall=[('moss_block', 3), ('mud', 2), ('rooted_dirt', 2), ('packed_mud', 1)],
                           floor=[('moss_block', 4), ('coarse_dirt', 2), ('rooted_dirt', 1), ('podzol', 1)],
                           ceil=[('dirt', 2), ('rooted_dirt', 2), ('moss_block', 1)], seed=21))
    lair = m.theme(Theme('lair', wall=[('deepslate', 2), ('tuff', 2), ('moss_block', 1)],
                         floor=[('tuff', 2), ('deepslate', 2), ('moss_block', 1)], ceil=[('deepslate', 2), ('tuff', 1)], seed=22))
    grove = m.theme(Theme('grove', wall=[('mud_bricks', 3), ('packed_mud', 2), ('moss_block', 1)],
                          floor=[('packed_mud', 2), ('mud_bricks', 2), ('moss_block', 1)], ceil='mud_bricks', seed=23))
    under = m.theme(Theme('underside', wall=[('obsidian', 2), ('crying_obsidian', 1), ('purple_terracotta', 2)],
                          floor=[('obsidian', 2), ('amethyst_block', 1), ('purple_terracotta', 1)], ceil='crying_obsidian', seed=24))
    s = Stage(N, 'thicket', 'Паучья чаща', (10, 20), 'thicket')

    # ------------------------------------------------------------------ пустоты
    m.cave(X(8), FY + 4, 30, 9, 5, 8, forest, rough=0.2, seed=1, floor=FY)                      # Грибная поляна
    m.corridor([(X(16), 30), (X(26), 30), (X(34), 22), (X(46), 22)], 4, 5, forest, y=FY)        # Тропа ткачей
    m.cave(X(30), FY + 3, 25, 6, 4, 5, forest, rough=0.2, seed=2, floor=FY)
    m.cave(X(60), FY + 6, 22, 14, 7, 12, lair, rough=0.25, seed=3, floor=FY)                    # Паучьи ясли
    m.cave(X(58), FY + 2, 3, 4, 3, 3, lair, rough=0.2, seed=4, floor=FY)                        # секретка
    m.corridor([(X(58), 5), (X(58), 14)], 2, 3, lair, y=FY)
    m.corridor([(X(74), 22), (X(80), 22)], 4, 5, forest, y=FY)
    m.cave(X(88), FY + 5, 22, 9, 5, 9, forest, rough=0.2, seed=5, floor=FY)                     # Роща светлячков
    m.corridor([(X(97), 22), (X(99), 22)], 3, 4, forest, y=FY)
    m.round_room(X(114), 22, 15, FY, 12, lair, dome=6)                                          # Логово Аракны
    m.corridor([(X(129), 22), (X(134), 22)], 3, 4, forest, y=FY)
    m.cave(X(148), FY + 6, 22, 14, 7, 10, forest, rough=0.2, seed=6, floor=FY)                 # Корневой мост
    m.pit(X(137), 15, X(159), 29, FY, 14, forest)
    m.corridor([(X(162), 22), (X(166), 22)], 4, 5, grove, y=FY)
    m.cave(X(178), FY + 6, 22, 12, 6, 12, grove, rough=0.2, seed=7, floor=FY)                  # Логово ведьм
    m.corridor([(X(190), 22), (X(194), 22)], 3, 4, grove, y=FY)
    m.round_room(X(210), 22, 16, FY, 12, forest, dome=5)                                        # Арена Древня
    m.corridor([(X(226), 22), (X(230), 22)], 3, 4, forest, y=FY)
    m.cave(X(240), FY + 5, 22, 10, 5, 10, grove, rough=0.2, seed=8, floor=FY)                  # Сад гнили
    m.corridor([(X(250), 22), (X(254), 22)], 3, 4, grove, y=FY)
    m.round_room(X(270), 22, 15, FY, 11, grove, dome=5)                                         # Поляна Морены
    m.corridor([(X(270), 37), (X(270), 48)], 3, 4, under, y=FY)
    m.round_room(X(270), 62, 14, FY, 12, under, dome=6)                                         # Изнанка
    m.room(X(267), 75, X(273), 80, FY, 6, under)                                                # ниша разлома
    m.shell(2)

    # ------------------------------------------------------------------ мост над пропастью: корни поперёк
    for x in range(X(137), X(160)):
        for z in range(21, 24):
            m.set(x, FY - 1, z, 'mangrove_roots' if (x + z) % 3 else 'muddy_mangrove_roots')
    for x in range(X(137), X(160), 4):
        m.set(x, FY, 20, 'mangrove_roots')
        m.set(x, FY, 24, 'mangrove_roots')

    # ------------------------------------------------------------------ Грибная поляна (привал)
    mushroom(m, X(3), FY, 25, 5)
    mushroom(m, X(13), FY, 35, 4, 'brown_mushroom_block', r=2)
    m.set(X(8), FY, 30, 'campfire', facing='north', lit=True, signal_fire=False, waterlogged=False)
    for dx, dz, f in ((-2, 0, 'east'), (2, 0, 'west')):
        m.stairs(X(8 + dx), FY, 30 + dz, 'mangrove_stairs', f)
    m.sign(X(1), FY + 1, 30, 'east')
    s.sign((X(1), FY + 1, 30), ['ЭТАП 2', 'Паучья чаща', 'уровни 10–20', ''])
    s.say((X(0), FY - 1, 22, X(17), FY + 8, 38), 'Система: «Здесь когда-то был лес. Теперь он растёт вниз — и ест тех, кто растёт вверх.»')

    # ------------------------------------------------------------------ Тропа ткачей и Ясли
    s.pack('p1', [('weaver', X(30), FY, 24, 10), ('weaver', X(32), FY, 27, 10), ('jumper', X(28), FY, 27, 10),
                  ('spitter', X(33), FY, 24, 10, None, 4)])
    s.pack('p2', [('wasp', X(42), FY, 21, 11), ('wasp', X(43), FY, 23, 11), ('wasp', X(44), FY, 21, 11),
                  ('spitter', X(45), FY, 22, 11), ('wasp', X(43), FY, 22, 11, None, 5)])
    s.pack('p3', [('weaver', X(54), FY, 15, 11), ('weaver', X(56), FY, 18, 11), ('spitter', X(52), FY, 18, 11),
                  ('jumper', X(55), FY, 16, 11, None, 4)])
    s.pack('p4', [('jumper', X(64), FY, 28, 12), ('jumper', X(66), FY, 26, 12), ('wasp', X(63), FY, 30, 12),
                  ('wasp', X(65), FY, 30, 12), ('wasp', X(67), FY, 29, 12, None, 4)])
    s.pack('p5', [('forest_witch', X(68), FY, 18, 12), ('treant_guard', X(66), FY, 16, 12), ('weaver', X(70), FY, 19, 12),
                  ('treant_guard', X(69), FY, 15, 12, None, 4)])
    cobwebs(m, (X(46), FY + 3, 10, X(74), FY + 13, 34), 0.08, 2)
    for (cx, cz) in ((X(50), 14), (X(70), 30), (X(62), 34)):
        for dy in range(0, 3):
            m.set(cx, FY + dy, cz, 'cobweb')
    # секретка: осыпь (заражённый камень) в северной стене яслей
    for y in range(FY, FY + 2):
        for z in (8, 9):
            m.set(X(58), y, z, 'infested_stone')
            m.set(X(57), y, z, 'infested_stone')
    m.set(X(58), FY, 2, 'litrpg:secret_cache', facing='south')
    s.cache('nursery', (X(58), FY, 2), 3)
    s.say((X(46), FY - 1, 10, X(74), FY + 13, 34), 'Система: «Паучьи ясли. Не рвите паутину без нужды — по ней бегут вести к Королеве.»')

    # ------------------------------------------------------------------ Роща светлячков (загадка памяти)
    pillars = []
    for i in range(5):
        a = i * 2 * math.pi / 5 + 0.3
        px, pz = round(X(88) + 6 * math.cos(a)), round(22 + 6 * math.sin(a))
        m.set(px, FY, pz, 'mushroom_stem', up=False, down=False, north=True, south=True, east=True, west=True)
        m.set(px, FY + 1, pz, 'shroomlight')
        pillars.append((px, FY + 1, pz))
    m.set(X(88), FY, 22, 'stripped_mangrove_log', axis='y')
    m.set(X(88), FY + 1, 22, 'mangrove_button', face='floor', facing='north', powered=False)
    mech_mem = s.mech('memory', 'memory', [(X(88), FY + 1, 22)] + pillars, text='Роща светлячков', rounds=3)
    m.sign(X(80), FY + 1, 19, 'east')
    s.sign((X(80), FY + 1, 19), ['Светлячки', 'помнят путь.', 'Кнопка в пне —', 'повтори их свет'])
    s.gate('g1', (X(98), FY, 21, X(98), FY + 3, 23), open_when=[f'mech:{mech_mem}'])
    s.say((X(79), FY - 1, 13, X(97), FY + 10, 31), 'Система: «Нажмите кнопку в пне и запомните, какие грибы вспыхнут. Потом — по порядку.»')

    # ------------------------------------------------------------------ Логово Аракны
    cobwebs(m, (X(99), FY + 4, 7, X(129), FY + 16, 37), 0.10, 3)
    for i in range(6):
        a = i * math.pi / 3
        bx, bz = round(X(114) + 13 * math.cos(a)), round(22 + 13 * math.sin(a))
        m.pillar(bx, bz, FY, m.ceil_y(bx, FY, bz) - FY + 1, 'deepslate', cap='tuff', base_='mossy_cobblestone')
    b1 = s.boss('arachna', 'arachna', (X(114), FY, 22), 15)
    s.gate('arena1', (X(100), FY, 21, X(100), FY + 3, 23), lock=b1)
    s.gate('g2', (X(131), FY, 21, X(131), FY + 3, 23), open_when=[f'boss:{b1}'])
    s.say((X(100), FY - 1, 7, X(129), FY + 12, 37), 'Система: «Королева Аракна. Яйца — разбить. Коконы — разрезать. Когда она уходит под своды — не стойте под ней.»')

    # ------------------------------------------------------------------ Корневой мост
    for i, x0 in enumerate((X(139), X(146), X(153))):
        s.mech(f'spores{i}', 'hazard', area=(x0, FY, 21, x0 + 4, FY, 23), period=3.0, warn=1.2, share=0.2, offset=i * 1.0, sound=1)
    s.pack('p6', [('forest_witch', X(161), FY, 20, 15), ('wasp', X(160), FY, 24, 15), ('wasp', X(163), FY, 22, 15),
                  ('jumper', X(160), FY, 18, 15), ('wasp', X(162), FY, 23, 15, None, 5)])
    s.say((X(134), FY - 1, 12, X(140), FY + 8, 32), 'Система: «Мост из корней. Споры вспыхивают по очереди. Упадёте — Система вытащит, но не бесплатно.»')

    # ------------------------------------------------------------------ Логово ведьм
    for (hx, hz) in ((X(172), 14), (X(184), 30)):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                for dy in range(0, 3):
                    if abs(dx) == 2 or abs(dz) == 2:
                        if not (dz == 2 and dx == 0 and dy < 2):
                            m.set(hx + dx, FY + dy, hz + dz, 'spruce_planks' if dy < 2 else 'dark_oak_planks')
                m.set(hx + dx, FY + 3, hz + dz, 'dark_oak_slab', type='bottom', waterlogged=False)
        m.set(hx, FY, hz, 'cauldron')
    m.set(X(184), FY, 29, 'litrpg:locked_cache', facing='south')
    m.clear(X(184), FY, 30)
    s.cache('hut', (X(184), FY, 29), 2)
    s.pack('crone', [('forest_witch', X(182), FY, 25, 16), ('wasp', X(180), FY, 24, 15), ('wasp', X(185), FY, 24, 15)], title='Старая Ягиня')
    s.pack('p7', [('treant_guard', X(172), FY, 22, 15), ('spitter', X(170), FY, 25, 15), ('spitter', X(174), FY, 25, 15),
                  ('weaver', X(172), FY, 19, 15, None, 4)])
    s.pack('p8', [('forest_witch', X(182), FY, 16, 16), ('jumper', X(180), FY, 18, 16), ('jumper', X(184), FY, 18, 16),
                  ('treant_guard', X(186), FY, 15, 16, None, 4)])
    m.set(X(178), FY, 22, 'lectern', facing='west', has_book=False, powered=False)
    for (cx, cz) in ((X(177), 21), (X(177), 23)):
        m.candle(cx, FY, cz, 3, 'green')
    mech_riddle = s.mech('riddle', 'riddle', area=(X(175), FY, 19, X(181), FY + 3, 25), answers=['ветер', 'ветерок'],
                         text='Загадка ведьм|Без рук, без ног, а ворота отворяет. Что это?')
    m.sign(X(178), FY + 1, 26, 'north')
    s.sign((X(178), FY + 1, 26), ['Без рук,', 'без ног, а', 'ворота', 'отворяет.'])
    s.gate('g3', (X(192), FY, 21, X(192), FY + 3, 23), open_when=[f'mech:{mech_riddle}', 'pack:s2_p7', 'pack:s2_p8'])
    s.say((X(166), FY - 1, 10, X(190), FY + 10, 34), 'Система: «Логово ведьм. Книга на аналое загадывает загадку — ответ напишите в чат, стоя рядом.»')

    # ------------------------------------------------------------------ Арена Гнилого Древня
    for i in range(7):
        a = i * 2 * math.pi / 7
        bx, bz = round(X(210) + 13 * math.cos(a)), round(22 + 13 * math.sin(a))
        m.pillar(bx, bz, FY, m.ceil_y(bx, FY, bz) - FY + 1, 'dark_oak_log', cap='mangrove_roots', base_='rooted_dirt')
    b2 = s.boss('ent', 'rot_ent', (X(210), FY, 22), 16)
    s.gate('arena2', (X(195), FY, 21, X(195), FY + 3, 23), lock=b2)
    s.gate('g4', (X(228), FY, 21, X(228), FY + 3, 23), open_when=[f'boss:{b2}'])
    s.say((X(195), FY - 1, 6, X(226), FY + 12, 38), 'Система: «Гнилой Древень. Споры растут — уводите его от них. Сердцевину — сбить.»')

    # ------------------------------------------------------------------ Сад гнили (грибы по росту)
    order = []
    spots = [(X(236), 16), (X(244), 28), (X(236), 28), (X(244), 16)]
    heights = [3, 1, 4, 2]
    for (gx, gz), hgt in zip(spots, heights):
        mushroom(m, gx, FY, gz, hgt + 1, 'brown_mushroom_block', r=1)
        m.set(gx + (1 if gx < X(240) else -1), FY, gz, 'polished_blackstone_bricks')
        m.set(gx + (1 if gx < X(240) else -1), FY + 1, gz, 'polished_blackstone_button', face='floor', facing='north', powered=False)
    for hgt in sorted(heights):
        i = heights.index(hgt)
        gx, gz = spots[i]
        order.append((gx + (1 if gx < X(240) else -1), FY + 1, gz))
    mech_seq = s.mech('garden', 'sequence', order, text='Сад гнили')
    m.sign(X(231), FY + 1, 19, 'east')
    s.sign((X(231), FY + 1, 19), ['Грибы растут', 'от малого', 'к великому.', 'Жми по росту.'])
    s.pack('p9', [('treant_guard', X(240), FY, 21, 18), ('spitter', X(238), FY, 24, 18), ('spitter', X(242), FY, 24, 18),
                  ('forest_witch', X(240), FY, 26, 18), ('jumper', X(241), FY, 19, 18, None, 4)])
    s.gate('g5', (X(252), FY, 21, X(252), FY + 3, 23), open_when=[f'mech:{mech_seq}', 'pack:s2_p9'])
    s.cache('garden', (X(246), FY, 30), 1)
    m.set(X(246), FY, 30, 'litrpg:cache', facing='north')

    # ------------------------------------------------------------------ Поляна Морены и Изнанка
    for i in range(8):
        a = i * math.pi / 4
        bx, bz = round(X(270) + 12 * math.cos(a)), round(22 + 12 * math.sin(a))
        m.pillar(bx, bz, FY, m.ceil_y(bx, FY, bz) - FY + 1, 'mud_bricks', cap='shroomlight', base_='packed_mud')
    for i in range(8):
        a = i * math.pi / 4 + 0.4
        bx, bz = round(X(270) + 11 * math.cos(a)), round(62 + 11 * math.sin(a))
        top = m.ceil_y(bx, FY, bz)
        for yy in range(top, top - 5, -1):
            m.set(bx, yy, bz, 'crying_obsidian' if yy == top - 4 else 'obsidian')
    brazier(m, X(270), FY, 50, soul=True)
    b3 = s.boss('morena', 'morena', (X(270), FY, 22), 15, final=True, arena2=(X(270) + 0.5, FY, 62.5, 14))
    s.gate('arena3', (X(255), FY, 21, X(255), FY + 3, 23), lock=b3)
    s.gate('under', (X(269), FY, 44, X(271), FY + 3, 44), open_when=[f'boss:{b3}'])
    s.say((X(255), FY - 1, 7, X(285), FY + 11, 37), 'Система: «Морена. Испуганного — не оставляйте одного. На Изнанке земля выворачивается: к ней — от неё — к ней.»')
    for dx in (-2, 2):
        for dy in range(0, 4):
            m.set(X(270 + dx), FY + dy, 78, 'crying_obsidian')
    for dx in (-1, 0, 1):
        m.set(X(270 + dx), FY + 3, 78, 'crying_obsidian')
        for dy in range(0, 3):
            m.set(X(270 + dx), FY + dy, 78, 'litrpg:rift', active=False)
    apply_gates(m, s)

    # ------------------------------------------------------------------ жизнь и свет
    forest_floor(m, (X(-2), 12, X(300), 90, FY), seed=7)
    ceiling_life(m, (X(-2), 0, X(300), 90, FY), seed=8)
    m.glow(spacing=5, level=9, height=3)
    m.hanging_lights(spacing=7, soul=True, themes=['grove'], min_h=6, chain=2)

    s.box = (X(-6), FY - 18, -6, X(300), FY + 26, 90)
    s.camp = (X(8.5), FY, 32.5, -90)
    s.trader = (X(5.5), FY, 27.5, 0)
    s.rift = (X(269), FY, 78, X(271), FY + 2, 78)
    return m, s
