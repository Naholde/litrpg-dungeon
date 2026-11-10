"""
Этап 2 · Паучья чаща (уровни 10–20). Подземный лес, который растёт вниз: мох, корни, гигантские грибы, паутина, споры.
Обновление 14: чаща уходит вниз четырьмя ярусами, каждый спуск — свой приём набора вертикали.

Ярус I · Кроны (ноги 84): Грибная поляна (привал) → Тропа ткачей → Паучьи ясли (секретка за осыпью) → Роща светлячков
  (загадка памяти; «Шляпочная тропа» — паркур по грибам к гнезду под сводом) → Логово Аракны (r 16, свод в 20 блоков:
  Аракна уходит под него на 9) → Обрыв Ткачей: корневой мост с провисом над Чёрной топью (споры по очереди) →
  Полый гриб — винтовая лестница внутри ножки гигантского гриба вниз на 18 блоков.
Ярус II · Подлесок (66): дно Обрыва (засада ткачей у топи) → Логово ведьм (хижина на сваях по лестнице — запертый
  тайник; загадка «ветер») → Поляна Гнилого Древня (неровная поляна r 18) → Корни Древня: «Дупло» — прыжок в полый
  ствол на 18 блоков в купель.
Ярус III · Грибница (48): Корневище (засада у купели) → Сад гнили (грибы по росту; Великан в пруду — паркур по
  трутовикам на шляпку, там тайник) → Мост над бездной (споры, провис).
Ярус IV · Изнанка (34): парадная корневая лестница вниз на 14 → Преддверие → Поляна Морены (r 18) → Изнанка (второй
  зал r 16) → ниша разлома.

Арены — по `arenas.py` (замер приёмов): Аракна 16, Древень 17 → 18, Морена 18.
"""
import math

from decor import brazier, cobwebs
from kit import Map, Stage, Theme, apply_gates, h3

N = 2
OX = 1024 * N
STRICT = True                  # строгая проверка: обрывы, западни, арены — ошибки
A, B, C, D = 84, 66, 48, 34    # ярусы: уровень ног

STEM = dict(up=False, down=False, north=True, south=True, east=True, west=True)
CAP_TOP = dict(up=True, down=False, north=True, south=True, east=True, west=True)
CAP_RIM = dict(up=False, down=True, north=True, south=True, east=True, west=True)


def X(x):
    return OX + x


def mushroom(m, x, y, z, h, cap='red_mushroom_block', r=2):
    """Гигантский гриб: ножка y..y+h-1, шляпка на y+h (край свисает на блок вниз)."""
    for yy in range(y, y + h):
        m.set(x, yy, z, 'mushroom_stem', **STEM)
    top = y + h
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            if abs(dx) == r and abs(dz) == r:
                continue
            m.set(x + dx, top, z + dz, cap, **CAP_TOP)
            if abs(dx) == r or abs(dz) == r:
                m.set(x + dx, top - 1, z + dz, cap, **CAP_RIM)
    m.set(x, top - 1, z, 'shroomlight')


def forest_floor(m, box, seed=0):
    """Ковры мха, папоротник, трава, азалии на полу пещеры."""
    x1, z1, x2, z2, y = box
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            if not m.is_air(x, y, z) or (x, y, z) not in m.open or not m.solid(x, y - 1, z):
                continue
            if (x, z) in m.keep:
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
            if not m.solid(x, top + 1, z) or not m.is_air(x, top, z) or top - y < 4:
                continue
            if (x, top + 1, z) in m.open:
                continue                      # над головой постройка (ступень, мост, шляпка), а не свод — не трогать
            k = h3(x, top, z, seed + 3)
            if k < 0.05:
                m.set(x, top, z, 'hanging_roots', waterlogged=False)
            elif k < 0.065:
                m.set(x, top, z, 'spore_blossom')
            elif k < 0.085:
                m.set(x, top + 1, z, 'shroomlight')


def hut(m, x1, z1, y, door):
    """Хижина 5×5 (стены в 2 блока, крыша из плит): door — клетка (x, z) проёма в стене."""
    for x in range(x1, x1 + 5):
        for z in range(z1, z1 + 5):
            edge = x in (x1, x1 + 4) or z in (z1, z1 + 4)
            if edge and (x, z) != door:
                corner = x in (x1, x1 + 4) and z in (z1, z1 + 4)
                for dy in range(2):
                    m.set(x, y + dy, z, 'dark_oak_log' if corner else 'spruce_planks', **({'axis': 'y'} if corner else {}))
            m.set(x, y + 2, z, 'dark_oak_slab', type='bottom', waterlogged=False)
    dx, dz = door
    m.clear(dx, y, dz)
    m.clear(dx, y + 1, dz)


def build(_n=N):
    m = Map(seed=2002)
    forest = m.theme(Theme('forest', wall=[('moss_block', 3), ('mud', 2), ('rooted_dirt', 2), ('packed_mud', 1)],
                           floor=[('moss_block', 4), ('coarse_dirt', 2), ('rooted_dirt', 1), ('podzol', 1)],
                           ceil=[('dirt', 2), ('rooted_dirt', 2), ('moss_block', 1)], seed=21))
    lair = m.theme(Theme('lair', wall=[('deepslate', 2), ('tuff', 2), ('moss_block', 1)],
                         floor=[('tuff', 2), ('deepslate', 2), ('moss_block', 1)], ceil=[('deepslate', 2), ('tuff', 1)], seed=22))
    grove = m.theme(Theme('grove', wall=[('mud_bricks', 3), ('packed_mud', 2), ('moss_block', 1)],
                          floor=[('packed_mud', 2), ('mud_bricks', 2), ('moss_block', 1)], ceil='mud_bricks', seed=23))
    roots = m.theme(Theme('roots', wall=[('rooted_dirt', 3), ('mud', 2), ('dripstone_block', 1), ('mangrove_roots', 1)],
                          floor=[('rooted_dirt', 2), ('mud', 2), ('moss_block', 2)], ceil=[('rooted_dirt', 2), ('dirt', 1)], seed=25))
    under = m.theme(Theme('underside', wall=[('obsidian', 2), ('crying_obsidian', 1), ('purple_terracotta', 2)],
                          floor=[('obsidian', 2), ('amethyst_block', 1), ('purple_terracotta', 1)], ceil='crying_obsidian', seed=24))
    s = Stage(N, 'thicket', 'Паучья чаща', (10, 20), 'thicket')

    # ================================================================== пустоты
    # ---- Ярус I · Кроны (84)
    m.cave(X(8), A + 4, 30, 9, 5, 8, forest, rough=0.2, seed=1, floor=A)                       # Грибная поляна
    m.corridor([(X(16), 30), (X(26), 30), (X(34), 22), (X(46), 22)], 4, 5, forest, y=A)        # Тропа ткачей
    m.cave(X(30), A + 3, 25, 6, 4, 5, forest, rough=0.2, seed=2, floor=A)
    m.cave(X(60), A + 6, 22, 14, 7, 12, lair, rough=0.25, seed=3, floor=A)                     # Паучьи ясли
    m.cave(X(58), A + 2, 3, 4, 3, 3, lair, rough=0.2, seed=4, floor=A)                         # секретка за осыпью
    m.corridor([(X(58), 5), (X(58), 14)], 2, 3, lair, y=A)
    m.corridor([(X(74), 22), (X(80), 22)], 4, 5, forest, y=A)
    m.cave(X(88), A + 5, 22, 9, 5, 9, forest, rough=0.2, seed=5, floor=A)                      # Роща светлячков
    m.room(X(84), 8, X(90), 13, A + 5, 3, forest)                                              # гнездо светлячков
    m.corridor([(X(97), 22), (X(101), 22)], 3, 4, forest, y=A)
    m.round_room(X(117), 22, 16, A, 13, lair, dome=8)                                          # Логово Аракны
    m.corridor([(X(133), 22), (X(141), 22)], 3, 4, lair, y=A)
    m.cave(X(163), B + 12, 22, 24, 18, 18, forest, rough=0.2, seed=6, floor=B)                      # Обрыв Ткачей
    m.pool(X(150), 15, X(169), 29, B - 1, 3, forest)                                           # Чёрная топь
    m.spiral(X(180) + 0.5, 22.5, B, A, 5, forest, step='brown_mushroom_block', core='mushroom_stem', r_core=1.3,
             per_turn=12, start=0.0, underside='mangrove_stairs')                              # Полый гриб

    # ---- Ярус II · Подлесок (66)
    m.corridor([(X(186), 22), (X(198), 22)], 4, 5, grove, y=B)
    m.cave(X(210), B + 8, 22, 12, 8, 12, grove, rough=0.2, seed=7, floor=B)                    # Логово ведьм
    m.corridor([(X(222), 22), (X(233), 22)], 3, 4, grove, y=B)
    m.cave(X(253), B + 7, 22, 20, 7, 19, forest, rough=0.25, seed=8, floor=B)                  # Поляна Древня
    m.corridor([(X(273), 22), (X(282), 22)], 3, 4, roots, y=B)
    m.cave(X(291), B + 5, 22, 9, 5, 9, roots, rough=0.2, seed=9, floor=B)                      # Корни Древня
    m.dropper(X(290), 21, X(292), 23, B, C, roots, depth=3, pad=2)                                  # Дупло: 18 блоков в купель

    # ---- Ярус III · Грибница (48)
    m.cave(X(291), C + 5, 22, 10, 5, 10, roots, rough=0.2, seed=10, floor=C)                   # Корневище
    m.corridor([(X(301), 22), (X(308), 22)], 4, 5, grove, y=C)
    m.cave(X(323), C + 8, 22, 15, 8, 13, grove, rough=0.2, seed=11, floor=C)                   # Сад гнили
    m.pool(X(318), 17, X(328), 27, C - 1, 2, grove)                                            # пруд Великана
    m.corridor([(X(338), 22), (X(346), 22)], 3, 4, forest, y=C)
    m.cave(X(361), C + 6, 22, 16, 6, 9, forest, rough=0.2, seed=12, floor=C)                   # Мост над бездной
    m.pit(X(348), 14, X(374), 30, C, 30, forest)
    # ---- Ярус IV · Изнанка (34): парадная корневая лестница вниз на 14 блоков
    m.corridor([(X(377), 22, C), (X(380), 22, C), (X(394), 22, D), (X(398), 22, D)], 5, 5, roots)
    m.room(X(398), 14, X(408), 30, D, 7, roots, vault=2)                                      # Преддверие
    m.corridor([(X(408), 22), (X(414), 22)], 3, 4, grove, y=D)
    m.round_room(X(432), 22, 18, D, 12, grove, dome=6)                                         # Поляна Морены
    m.corridor([(X(432), 40), (X(432), 51)], 3, 4, under, y=D)
    m.round_room(X(432), 67, 16, D, 12, under, dome=6)                                         # Изнанка
    m.room(X(429), 83, X(435), 88, D, 6, under)                                                # ниша разлома

    # ---- вертикаль до оболочки (постройки откладываются до неё сами)
    m.bridge((X(148), 22), (X(174), 22), A, 'mangrove_planks', width=3, rail='mangrove_fence', posts='mangrove_log',
             post_every=6, sag=2, under='mangrove_roots', tall_posts=True)                                     # корневой мост Обрыва
    m.bridge((X(346), 22), (X(376), 22), C, 'mangrove_planks', width=3, rail='mangrove_fence', posts='mangrove_log',
             post_every=6, sag=1, under='mangrove_roots', tall_posts=True)                                     # мост над бездной
    # «Шляпочная тропа»: грибы-ступени к гнезду светлячков под сводом Рощи (с 5 блоков падать можно)
    m.parkour([(X(85), A + 1, 20), (X(85), A + 2, 17), (X(88), A + 3, 17), (X(88), A + 4, 14)],
              'brown_mushroom_block', pillar='mushroom_stem', theme=forest)
    # хижина ведьм на сваях: площадка на +5 с перилами, лестница с севера
    m.platform(X(212) + 0.5, 28.5, B + 5, 3, 'spruce_planks', rim='dark_oak_planks', round_=False, theme=grove,
               rail='dark_oak_fence', gaps={(X(212), 25)})
    m.ladder(X(212), 24, B, B + 4, 'north', support='dark_oak_log')
    # Великан в пруду: трутовики по кругу вокруг ножки, каждый на блок выше — до шляпки (падать — в воду)
    ring = [(319, 22), (319, 19), (322, 19), (325, 19), (325, 22), (325, 25), (322, 25), (319, 25)]
    shelves = [(X(ring[k % 8][0] + 1), C + 1 + k, ring[k % 8][1]) for k in range(11)]
    m.parkour(shelves, 'brown_mushroom_block', theme=grove)
    m.shell(2)

    # ================================================================== Грибная поляна (привал)
    mushroom(m, X(3), A, 25, 5)
    mushroom(m, X(13), A, 35, 4, 'brown_mushroom_block', r=2)
    m.set(X(8), A, 30, 'campfire', facing='north', lit=True, signal_fire=False, waterlogged=False)
    for dx, dz, f in ((-2, 0, 'east'), (2, 0, 'west')):
        m.stairs(X(8 + dx), A, 30 + dz, 'mangrove_stairs', f)
    m.sign(X(1), A + 1, 30, 'east')
    s.sign((X(1), A + 1, 30), ['ЭТАП 2', 'Паучья чаща', 'уровни 10–20', ''])
    s.say((X(0), A - 1, 22, X(17), A + 8, 38),
          'Система: «Здесь когда-то был лес. Теперь он растёт вниз — и ест тех, кто растёт вверх. '
          'Чаща уходит на четыре яруса: кроны, подлесок, грибница — и Изнанка под ними.»')

    # ================================================================== Тропа ткачей и Ясли
    s.pack('p1', [('weaver', X(30), A, 24, 10), ('weaver', X(32), A, 27, 10), ('jumper', X(28), A, 27, 10),
                  ('spitter', X(33), A, 24, 10, None, 4)])
    s.pack('p2', [('wasp', X(52), A, 21, 11), ('wasp', X(53), A, 23, 11), ('wasp', X(54), A, 21, 11),
                  ('spitter', X(55), A, 22, 11), ('wasp', X(53), A, 22, 11, None, 5)])
    s.pack('p3', [('weaver', X(58), A, 14, 11), ('weaver', X(60), A, 17, 11), ('spitter', X(56), A, 17, 11),
                  ('jumper', X(59), A, 15, 11, None, 4)])
    s.pack('p4', [('jumper', X(64), A, 28, 12), ('jumper', X(66), A, 26, 12), ('wasp', X(63), A, 30, 12),
                  ('wasp', X(65), A, 30, 12), ('wasp', X(67), A, 29, 12, None, 4)])
    s.pack('p5', [('forest_witch', X(68), A, 18, 12), ('treant_guard', X(66), A, 16, 12), ('weaver', X(70), A, 19, 12),
                  ('treant_guard', X(69), A, 15, 12, None, 4)])
    cobwebs(m, (X(46), A + 4, 10, X(74), A + 13, 34), 0.08, 2)
    for (cx, cz) in ((X(50), 14), (X(70), 30), (X(62), 34)):
        for dy in range(0, 3):
            m.set(cx, A + dy, cz, 'cobweb')
    for y in range(A, A + 2):                     # секретка: осыпь (заражённый камень) в северной стене яслей
        for z in (8, 9):
            m.set(X(58), y, z, 'infested_stone')
            m.set(X(57), y, z, 'infested_stone')
    m.set(X(58), A, 2, 'litrpg:secret_cache', facing='south')
    s.cache('nursery', (X(58), A, 2), 3)
    s.say((X(46), A - 1, 10, X(74), A + 13, 34),
          'Система: «Паучьи ясли. Не рвите паутину без нужды — по ней бегут вести к Королеве. '
          'А камень в северной стене здесь осыпается от одного удара.»')

    # ================================================================== Роща светлячков (загадка памяти, Шляпочная тропа)
    pillars = []
    for i in range(5):
        a = i * 2 * math.pi / 5 + 0.3
        px, pz = round(X(88) + 6 * math.cos(a)), round(22 + 6 * math.sin(a))
        m.set(px, A, pz, 'mushroom_stem', **STEM)
        m.set(px, A + 1, pz, 'shroomlight')
        pillars.append((px, A + 1, pz))
    m.set(X(88), A, 22, 'stripped_mangrove_log', axis='y')
    m.set(X(88), A + 1, 22, 'mangrove_button', face='floor', facing='north', powered=False)
    mech_mem = s.mech('memory', 'memory', [(X(88), A + 1, 22)] + pillars, text='Роща светлячков', rounds=3)
    m.sign(X(80), A + 1, 19, 'east')
    s.sign((X(80), A + 1, 19), ['Светлячки', 'помнят путь.', 'Кнопка в пне —', 'повтори их свет'])
    s.gate('g1', (X(99), A, 21, X(99), A + 3, 23), open_when=[f'mech:{mech_mem}'])
    s.say((X(79), A - 1, 13, X(97), A + 10, 31),
          'Система: «Нажмите кнопку в пне и запомните, какие грибы вспыхнут. Потом — по порядку. '
          'А грибы у северной стены растут ступенями — к гнезду под сводом.»')
    # гнездо светлячков: тайник в нише под сводом, край гнезда — корни над рощей (с края — 5 блоков вниз)
    m.fill(X(84), A + 4, 12, X(90), A + 4, 13, 'mangrove_roots')
    for (cx, cz) in ((X(85), 8), (X(89), 8)):
        m.candle(cx, A + 5, cz, 3, 'lime')
    m.set(X(87), A + 5, 8, 'litrpg:secret_cache', facing='south')
    s.cache('firefly_nest', (X(87), A + 5, 8), 3)
    for x in (X(84), X(90)):
        m.set(x, A + 8, 10, 'shroomlight')

    # ================================================================== Логово Аракны (r 16, высокий свод)
    cobwebs(m, (X(101), A + 7, 6, X(133), A + 20, 38), 0.10, 3)
    for i in range(6):
        a = i * math.pi / 3 + math.pi / 6
        bx, bz = round(X(117) + 13.5 * math.cos(a)), round(22 + 13.5 * math.sin(a))
        m.pillar(bx, bz, A, m.ceil_y(bx, A, bz) - A + 1, 'deepslate', cap='tuff', base_='mossy_cobblestone')
        for dy in (2, 3, 5, 6):                  # паутина обмотала колонны
            for ddx, ddz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if h3(bx + ddx, A + dy, bz + ddz, 31) < 0.45 and m.is_air(bx + ddx, A + dy, bz + ddz):
                    m.set(bx + ddx, A + dy, bz + ddz, 'cobweb')
    b1 = s.boss('arachna', 'arachna', (X(117), A, 22), 16)
    s.gate('arena1', (X(100), A, 21, X(100), A + 3, 23), lock=b1)
    s.gate('g2', (X(137), A, 21, X(137), A + 3, 23), open_when=[f'boss:{b1}'])
    s.say((X(101), A - 1, 6, X(133), A + 12, 38),
          'Система: «Королева Аракна. Яйца — разбить. Коконы — разрезать. Свод здесь высок: когда она уходит '
          'под него — не стойте под ней.»')

    # ================================================================== Обрыв Ткачей: балкон, корневой мост, Полый гриб
    m.fill(X(136), A - 1, 17, X(147), A - 1, 27, 'mossy_cobblestone')
    for z in range(17, 28):
        m.stairs(X(147), A - 2, z, 'mossy_cobblestone_stairs', 'west', half='top')
    m.railing([(x, A, z) for x in range(X(136), X(148)) for z in (17, 27)]
              + [(X(147), A, z) for z in range(18, 27) if z not in (21, 22, 23)], 'mossy_cobblestone_wall')
    for i, x0 in enumerate((X(151), X(158), X(165))):
        s.mech(f'spores{i}', 'hazard', area=(x0, A - 2, 21, x0 + 4, A, 23), period=3.0, warn=1.2, share=0.2,
               offset=i * 1.0, sound=1)
    # Полый гриб: ножка-труба вокруг винтовой лестницы, шляпка над ней, двери наверху (мост) и внизу (на юг и восток)
    gx, gz = X(180) + 0.5, 22.5
    for x in range(X(172), X(190)):
        for z in range(12, 34):
            d = math.hypot(x + 0.5 - gx, z + 0.5 - gz)
            if 5.5 < d <= 6.8:
                for y in range(B - 1, A + 4):
                    m.set(x, y, z, 'shroomlight' if (y - B) % 6 == 3 and h3(x, y, z, 5) < 0.35 else 'mushroom_stem', **(
                        {} if (y - B) % 6 == 3 and h3(x, y, z, 5) < 0.35 else STEM))
            if d <= 9.5:
                m.set(x, A + 4, z, 'red_mushroom_block', **CAP_TOP)
                if d > 8.5 and not m.solid(x, A + 3, z):
                    m.set(x, A + 3, z, 'red_mushroom_block', **CAP_RIM)
                elif 6.8 < d <= 8.0 and h3(x, A, z, 6) < 0.4 and not m.solid(x, A + 3, z):
                    m.set(x, A + 3, z, 'shroomlight')
    for z in (21, 22, 23):
        for y in range(A, A + 3):
            m.clear(X(174), y, z)                # дверь наверху — с моста
        for y in range(B, B + 3):
            m.clear(X(186), y, z)                # дверь внизу — на восток, к ведьмам
    for x in (X(179), X(180), X(181)):
        for y in range(B, B + 2):
            m.clear(x, y, 28)                    # дверь внизу — на юг, к топи
    # дно Обрыва: топь, кладки, засада
    for (cx, cz) in ((X(146), 8), (X(156), 36), (X(170), 6), (X(150), 34)):
        mushroom(m, cx, B, cz, 4 + int(h3(cx, B, cz, 2) * 4), 'brown_mushroom_block' if cx % 2 else 'red_mushroom_block', r=2)
    cobwebs(m, (X(140), B + 4, 4, X(186), A + 10, 40), 0.03, 4)
    s.pack('p6', [('weaver', X(160), B, 34, 13), ('weaver', X(165), B, 33, 13), ('spitter', X(170), B, 35, 13),
                  ('jumper', X(162), B, 36, 13), ('wasp', X(168), B, 32, 13, None, 4)])
    s.pack('p7', [('jumper', X(184), B, 30, 13), ('spitter', X(182), B, 33, 13), ('treant_guard', X(178), B, 33, 13),
                  ('weaver', X(176), B, 31, 13, None, 5)])
    m.sign(X(146), A + 1, 20, 'east', wood='mangrove')
    s.sign((X(146), A + 1, 20), ['Обрыв Ткачей', 'Мост держит.', 'Споры — нет.', 'Вниз — в гриб.'])
    s.say((X(136), A - 1, 14, X(150), A + 8, 30),
          'Система: «Обрыв Ткачей. Корневой мост провис над Чёрной топью; споры на нём вспыхивают по очереди. '
          'На том берегу — Полый гриб: внутри его ножки лестница уходит на восемнадцать блоков вниз.»')
    s.say((X(174), B - 1, 15, X(187), A + 3, 30),
          'Система: «Внутри гриба темно и сыро. Внизу две двери: на юг — к топи, где ткачи стерегут свои кладки, '
          'на восток — к ведьмам.»')

    # ================================================================== Логово ведьм: хижина на сваях, загадка
    hut(m, X(201), 12, B, (X(203), 16))                                       # хижина на земле
    m.set(X(203), B, 14, 'cauldron')
    m.set(X(202), B, 13, 'brewing_stand', has_bottle_0=False, has_bottle_1=False, has_bottle_2=False)
    hut(m, X(210), 26, B + 5, (X(212), 26))                                   # хижина на сваях
    for (px, pz) in ((X(209), 25), (X(215), 25), (X(209), 31), (X(215), 31)):
        for y in range(B, B + 4):
            m.set(px, y, pz, 'dark_oak_log', axis='y')
    m.set(X(212), B + 5, 29, 'litrpg:locked_cache', facing='north')
    s.cache('hut', (X(212), B + 5, 29), 2)
    m.set(X(211), B + 5, 28, 'cauldron')
    m.lantern(X(213), B + 5, 28)
    m.set(X(208), B, 22, 'lectern', facing='west', has_book=False, powered=False)
    for (cx, cz) in ((X(207), 21), (X(207), 23)):
        m.candle(cx, B, cz, 3, 'green')
    mech_riddle = s.mech('riddle', 'riddle', area=(X(204), B, 18, X(212), B + 3, 26), answers=['ветер', 'ветерок'],
                         text='Загадка ведьм|Без рук, без ног, а ворота отворяет. Что это?')
    m.sign(X(208), B + 1, 25, 'north', wood='spruce')
    s.sign((X(208), B + 1, 25), ['Без рук,', 'без ног, а', 'ворота', 'отворяет.'])
    s.pack('crone', [('forest_witch', X(214), B, 22, 16), ('wasp', X(212), B, 20, 15), ('wasp', X(216), B, 20, 15)],
           title='Старая Ягиня')
    p8 = s.pack('p8', [('treant_guard', X(203), B, 22, 15), ('spitter', X(201), B, 25, 15), ('spitter', X(205), B, 25, 15),
                       ('weaver', X(203), B, 19, 15, None, 4)])
    p9 = s.pack('p9', [('forest_witch', X(214), B, 15, 16), ('jumper', X(212), B, 17, 16), ('jumper', X(216), B, 17, 16),
                       ('treant_guard', X(216), B, 15, 16, None, 4)])
    s.gate('g3', (X(225), B, 21, X(225), B + 3, 23), open_when=[f'mech:{mech_riddle}', f'pack:{p8}', f'pack:{p9}'])
    s.say((X(198), B - 1, 10, X(222), B + 12, 34),
          'Система: «Логово ведьм. Книга на аналое загадывает загадку — ответ напишите в чат, стоя рядом. '
          'Ягиня прячет добро в хижине на сваях.»')

    # ================================================================== Поляна Гнилого Древня (r 18, неровная)
    for i, (a, r) in enumerate(((0.55, 15), (1.35, 14), (2.2, 15.5), (2.75, 13), (3.75, 15), (4.6, 14.5), (5.5, 13.5))):
        bx, bz = round(X(253) + r * math.cos(a)), round(22 + r * math.sin(a))
        m.pillar(bx, bz, B, m.ceil_y(bx, B, bz) - B + 1, 'dark_oak_log', cap='mangrove_roots', base_='rooted_dirt')
    b2 = s.boss('ent', 'rot_ent', (X(253), B, 22), 18)
    s.gate('arena2', (X(230), B, 21, X(230), B + 3, 23), lock=b2)
    s.gate('g4', (X(277), B, 21, X(277), B + 3, 23), open_when=[f'boss:{b2}'])
    s.say((X(233), B - 1, 3, X(273), B + 14, 41),
          'Система: «Гнилой Древень. Споры растут — уводите его от них: поляна большая. Сердцевину — сбить.»')

    # ================================================================== Корни Древня: Дупло (дропер в полый ствол)
    for x in range(X(289), X(294)):
        for z in range(20, 25):
            if x in (X(289), X(293)) or z in (20, 24):
                m.set(x, B, z, 'dark_oak_wood', axis='y')            # край дупла — порожек в блок
    # стенки ствола — кора до трёх блоков над водой: ствол свисает в Корневище, и с разбега не вылететь вбок
    # (проверено вживую: без нижней части ствола разбег уносил на край купели — 70 урона из 486)
    for x in range(X(289), X(294)):
        for z in range(20, 25):
            if x in (X(289), X(293)) or z in (20, 24):
                for y in range(C + 3, B - 1):
                    m.set(x, y, z, 'dark_oak_wood', axis='y')
    m.sign(X(286), B + 1, 22, 'east', wood='dark_oak')
    s.sign((X(286), B + 1, 22), ['Дупло Древня.', 'Внизу купель.', 'Прыгайте —', 'назад пути нет.'])
    s.say((X(282), B - 1, 13, X(300), B + 10, 31),
          'Система: «Корни Древня. Его ствол был полым — дупло уходит на восемнадцать блоков вниз, в купель. Прыгайте.»')

    # ================================================================== Корневище (засада у купели)
    s.pack('p10', [('treant_guard', X(286), C, 15, 17), ('spitter', X(296), C, 15, 17), ('spitter', X(296), C, 29, 17),
                   ('forest_witch', X(286), C, 29, 17), ('jumper', X(298), C, 22, 17, None, 4)])
    s.say((X(281), C - 1, 12, X(301), C + 10, 32),
          'Система: «Грибница. Корни пьют отсюда всё, что падает сверху, — и тех, кто падает, тоже.»')

    # ================================================================== Сад гнили (грибы по росту) и Великан в пруду
    spots = [(X(314), 15), (X(332), 29), (X(314), 29), (X(332), 15)]
    heights = [3, 5, 7, 9]
    order_ = [1, 3, 0, 2]                         # рост по местам: 5, 9, 3, 7 — жать 3 → 5 → 7 → 9
    btns = []
    for k, (gx_, gz_) in enumerate(spots):
        hgt = heights[order_[k]]
        mushroom(m, gx_, C, gz_, hgt, 'brown_mushroom_block', r=1)
        bx = gx_ + (1 if gx_ < X(323) else -1)
        m.set(bx, C, gz_, 'polished_blackstone_bricks')
        m.set(bx, C + 1, gz_, 'polished_blackstone_button', face='floor', facing='north', powered=False)
        btns.append((hgt, (bx, C + 1, gz_)))
    mech_seq = s.mech('garden', 'sequence', [p for _, p in sorted(btns)], text='Сад гнили')
    m.sign(X(309), C + 1, 19, 'east', wood='mangrove')
    s.sign((X(309), C + 1, 19), ['Грибы растут', 'от малого', 'к великому.', 'Жми по росту.'])
    mushroom(m, X(323), C - 2, 22, 13, 'red_mushroom_block', r=2)              # Великан: шляпка на C+11
    m.set(X(323), C + 12, 22, 'litrpg:secret_cache', facing='west')
    # огородка из корней по краю шляпки: шаг с края — на нижний трутовик (7 блоков); вход — с последнего трутовика
    m.railing([(X(323 + dx), C + 12, 22 + dz) for dx in range(-2, 3) for dz in range(-2, 3)
               if (abs(dx) == 2 or abs(dz) == 2) and not (abs(dx) == 2 and abs(dz) == 2) and (dx, dz) != (0, -2)],
              'mangrove_fence')
    s.cache('giant_cap', (X(323), C + 12, 22), 3)
    for x, y, z in shelves:                       # трутовики держатся на свисающих корнях
        if m.is_air(x, y - 2, z):
            m.set(x, y - 2, z, 'hanging_roots', waterlogged=False)
    p11 = s.pack('p11', [('treant_guard', X(323), C, 32, 18), ('spitter', X(320), C, 33, 18), ('spitter', X(326), C, 33, 18),
                         ('forest_witch', X(323), C, 12, 18), ('jumper', X(320), C, 11, 18, None, 4)])
    s.gate('g5', (X(341), C, 21, X(341), C + 3, 23), open_when=[f'mech:{mech_seq}', f'pack:{p11}'])
    s.cache('garden', (X(336), C, 30), 1)
    m.set(X(336), C, 30, 'litrpg:cache', facing='west')
    s.say((X(308), C - 1, 9, X(338), C + 16, 35),
          'Система: «Сад гнили. Четыре гриба — четыре кнопки: жмите от малого к великому. '
          'Великан в пруду не считается — но на его шляпке кто-то оставил сундук.»')

    # ================================================================== Мост над бездной
    for i, x0 in enumerate((X(350), X(359), X(368))):
        s.mech(f'spores{i + 3}', 'hazard', area=(x0, C - 1, 21, x0 + 4, C, 23), period=2.6, warn=1.1, share=0.22,
               offset=i * 0.9, sound=1)
    cobwebs(m, (X(346), C + 4, 13, X(376), C + 12, 31), 0.04, 5)
    s.say((X(345), C - 1, 13, X(350), C + 8, 31),
          'Система: «Мост над бездной. Внизу — ничего. Совсем ничего. Споры вспыхивают по очереди.»')

    # ================================================================== Преддверие и Поляна Морены
    s.pack('p12', [('forest_witch', X(403), D, 18, 19), ('treant_guard', X(405), D, 22, 19), ('treant_guard', X(401), D, 26, 19),
                   ('spitter', X(406), D, 27, 19), ('wasp', X(404), D, 16, 19, None, 4)])
    for (bx, bz) in ((X(399), 15), (X(407), 15), (X(399), 29), (X(407), 29)):
        brazier(m, bx, D, bz, soul=True)
    s.say((X(377), D - 1, 14, X(398), C + 5, 30), 'Система: «Корни сплелись в лестницу. Ниже чащи — только Изнанка.»')
    for i in range(8):
        a = i * math.pi / 4 + math.pi / 8
        bx, bz = round(X(432) + 16.5 * math.cos(a)), round(22 + 16.5 * math.sin(a))
        m.pillar(bx, bz, D, m.ceil_y(bx, D, bz) - D + 1, 'mud_bricks', cap='shroomlight', base_='packed_mud')
    for i in range(8):
        a = i * math.pi / 4 + 0.4
        bx, bz = round(X(432) + 12 * math.cos(a)), round(67 + 12 * math.sin(a))
        top = m.ceil_y(bx, D, bz)
        for yy in range(top, top - 5, -1):
            m.set(bx, yy, bz, 'crying_obsidian' if yy == top - 4 else 'obsidian')
    brazier(m, X(432), D, 52, soul=True)
    b3 = s.boss('morena', 'morena', (X(432), D, 22), 18, final=True, arena2=(X(432) + 0.5, D, 67.5, 16))
    s.gate('arena3', (X(411), D, 21, X(411), D + 3, 23), lock=b3)
    s.gate('under', (X(431), D, 46, X(433), D + 3, 46), open_when=[f'boss:{b3}'])
    s.say((X(414), D - 1, 4, X(450), D + 11, 40),
          'Система: «Морена. Испуганного — не оставляйте одного. На Изнанке земля выворачивается: к ней — от неё — к ней.»')
    for dx in (-2, 2):
        for dy in range(0, 4):
            m.set(X(432 + dx), D + dy, 87, 'crying_obsidian')
    for dx in (-1, 0, 1):
        m.set(X(432 + dx), D + 3, 87, 'crying_obsidian')
        for dy in range(0, 3):
            m.set(X(432 + dx), D + dy, 87, 'litrpg:rift', active=False)
    apply_gates(m, s)

    # ================================================================== жизнь и свет
    for y, seed in ((A, 7), (B, 17), (C, 27), (D, 37)):
        forest_floor(m, (X(-2), 0, X(452), 92, y), seed=seed)
        ceiling_life(m, (X(-2), 0, X(452), 92, y), seed=seed + 1)
    m.glow(spacing=5, level=9, height=3)
    m.hanging_lights(spacing=7, soul=True, themes=['grove', 'roots'], min_h=6, chain=2)

    s.box = (X(-6), 14, -6, X(456), A + 24, 94)
    s.camp = (X(8.5), A, 32.5, -90)
    s.trader = (X(5.5), A, 27.5, 0)
    s.rift = (X(431), D, 87, X(433), D + 2, 87)
    return m, s
