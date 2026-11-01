"""
Этап 1 · Склеп Забытых (уровни 1–10). Обучение: разметка, агро, сбивать подготовку, разделение отряда.

Путь: Сторожка (привал) → Зал Предков (3 отряда, секретка) → Галерея саркофагов (рычаги в трёх нишах) →
Коридор шипов → Арена Привратника → спуск в Нижние катакомбы → Зал Трёх Стражей (плиты на разделение) +
Оссуарий (мини-хранитель Костолом, запертый тайник) → Склеп послушников → Коридор маятников → Капелла Сестёр →
Алтарь загадки → Усыпальница Карна → Тронный зал (второй зал лича, разлом).
"""
import math

from decor import banner, bones_pile, brazier, candles, carpet_line, cobwebs, column, floor_pattern, sarcophagus
from kit import Map, Stage, Theme, apply_gates

N = 1
OX = 1024 * N
UP, LOW = 64, 56   # уровни пола: верхний склеп и нижние катакомбы


def X(x):
    return OX + x


def build(_n=N):
    m = Map(seed=1001)
    crypt = m.theme(Theme('crypt', wall=[('stone_bricks', 6), ('cracked_stone_bricks', 2), ('mossy_stone_bricks', 2), ('andesite', 1)],
                          floor=[('polished_andesite', 4), ('stone_bricks', 3), ('cracked_stone_bricks', 1)],
                          ceil=[('stone_bricks', 3), ('cobblestone', 1)], seed=11))
    lower = m.theme(Theme('catacomb', wall=[('deepslate_bricks', 6), ('cracked_deepslate_bricks', 2), ('deepslate_tiles', 2)],
                          floor=[('polished_deepslate', 3), ('deepslate_tiles', 2), ('cobbled_deepslate', 1)],
                          ceil='deepslate_bricks', seed=12))
    chapel = m.theme(Theme('chapel', wall=[('polished_blackstone_bricks', 5), ('cracked_polished_blackstone_bricks', 2), ('blackstone', 1)],
                           floor=[('polished_blackstone', 3), ('blackstone', 1)], ceil='polished_blackstone_bricks', seed=13))
    tomb = m.theme(Theme('tomb', wall=[('deepslate_tiles', 4), ('packed_ice', 1), ('cracked_deepslate_tiles', 2)],
                         floor=[('polished_deepslate', 3), ('blue_ice', 1)], ceil='deepslate_tiles', seed=14))
    s = Stage(N, 'crypt', 'Склеп Забытых', (1, 10), 'crypt')

    # ------------------------------------------------------------------ пустоты
    m.room(X(0), 20, X(16), 34, UP, 6, crypt, vault=2)                          # Сторожка
    m.corridor([(X(16), 27), (X(22), 27)], 3, 4, crypt, y=UP)
    m.room(X(22), 12, X(46), 42, UP, 9, crypt, vault=4)                         # Зал Предков
    m.room(X(30), 43, X(36), 49, UP, 5, crypt)                                  # западная ниша (рычаг 1)
    m.room(X(26), 3, X(31), 8, UP, 4, crypt)                                    # секретка предков
    m.corridor([(X(28), 9), (X(28), 11)], 1, 3, crypt, y=UP)
    m.corridor([(X(46), 27), (X(52), 27)], 3, 4, crypt, y=UP)
    m.room(X(52), 20, X(66), 34, UP, 7, crypt, vault=2)                         # Галерея саркофагов
    m.room(X(55), 9, X(63), 19, UP, 5, crypt)                                   # северная ниша (рычаг 2)
    m.room(X(55), 35, X(63), 45, UP, 5, crypt)                                  # южная ниша (рычаг 3)
    m.corridor([(X(66), 27), (X(70), 27)], 3, 4, crypt, y=UP)
    m.room(X(70), 25, X(92), 29, UP, 5, crypt)                                  # Коридор шипов
    m.corridor([(X(92), 27), (X(96), 27)], 3, 4, crypt, y=UP)
    m.round_room(X(110), 27, 14, UP, 10, crypt, dome=4)                         # Арена Привратника
    m.corridor([(X(124), 27, UP), (X(128), 27, UP), (X(140), 27, LOW)], 3, 4, lower)   # спуск
    m.room(X(140), 14, X(164), 40, LOW, 8, lower, vault=3)                      # Зал Трёх Стражей
    m.room(X(148), 2, X(156), 9, LOW, 5, lower)                                 # северная ниша плиты
    m.corridor([(X(152), 10), (X(152), 13)], 3, 4, lower, y=LOW)
    m.room(X(166), 22, X(171), 32, LOW + 4, 4, lower)                           # балкон плиты (выше)
    m.corridor([(X(164), 35, LOW), (X(168), 35, LOW + 4), (X(168), 33, LOW + 4)], 2, 3, lower)
    m.room(X(146), 44, X(160), 56, LOW, 6, lower, vault=2)                      # Оссуарий
    m.corridor([(X(152), 40), (X(152), 44)], 3, 4, lower, y=LOW)
    m.room(X(162), 50, X(167), 55, LOW, 4, lower)                               # секретка оссуария
    m.corridor([(X(164), 18), (X(174), 18)], 3, 4, lower, y=LOW)
    m.room(X(174), 10, X(188), 24, LOW, 6, lower, vault=2)                      # Склеп послушников
    m.corridor([(X(188), 17), (X(192), 17)], 3, 4, lower, y=LOW)
    m.room(X(192), 15, X(206), 19, LOW, 5, chapel)                              # Коридор маятников
    m.corridor([(X(206), 17), (X(210), 17)], 3, 4, chapel, y=LOW)
    m.round_room(X(226), 17, 15, LOW, 10, chapel, dome=5)                       # Капелла Сестёр
    m.corridor([(X(241), 17), (X(248), 17)], 3, 4, chapel, y=LOW)
    m.room(X(248), 10, X(260), 24, LOW, 6, chapel, vault=2)                     # Алтарь загадки
    m.corridor([(X(260), 17), (X(266), 17)], 3, 4, tomb, y=LOW)
    m.round_room(X(282), 17, 15, LOW, 11, tomb, dome=5)                         # Усыпальница Карна
    m.corridor([(X(282), 32), (X(282), 44)], 3, 4, tomb, y=LOW)
    m.round_room(X(282), 58, 13, LOW, 12, tomb, dome=6)                         # Тронный зал
    m.room(X(279), 71, X(285), 76, LOW, 6, tomb)                                # ниша разлома
    m.shell(2)

    # ------------------------------------------------------------------ Сторожка (привал)
    floor_pattern(m, X(1), 21, X(15), 33, UP, ['polished_andesite', 'stone_bricks', 'chiseled_stone_bricks'], 3)
    m.set(X(8), UP, 27, 'campfire', facing='north', lit=True, signal_fire=False, waterlogged=False)
    for dx, dz in ((-2, 0), (2, 0), (0, -2), (0, 2)):
        m.stairs(X(8 + dx), UP, 27 + dz, 'spruce_stairs', {(-2, 0): 'east', (2, 0): 'west', (0, -2): 'south', (0, 2): 'north'}[(dx, dz)])
    for z in (22, 32):
        m.set(X(3), UP, z, 'barrel', facing='up', open=False)
        m.set(X(4), UP, z, 'barrel', facing='up', open=False)
    banner(m, X(1), UP + 3, 25, 'east', 'gray')
    banner(m, X(1), UP + 3, 29, 'east', 'gray')
    m.sign(X(16), UP + 1, 24, 'west')
    s.sign((X(16), UP + 1, 24), ['ЭТАП 1', 'Склеп', 'Забытых', 'уровни 1–10'])

    # ------------------------------------------------------------------ Зал Предков
    for x in range(X(25), X(44), 6):
        for z in (16, 38):
            column(m, x, z, UP, m.ceil_y(x, UP, z) - UP + 1)
    for x in (X(27), X(33), X(39)):
        sarcophagus(m, x, UP, 22, 'x')
        sarcophagus(m, x, UP, 32, 'x')
        candles(m, x - 1, UP, 24, None, 3)
    carpet_line(m, X(23), 27, X(45), 27, UP, 'red')
    bones_pile(m, X(44), UP, 14, seed=1)
    bones_pile(m, X(24), UP, 40, seed=2)
    cobwebs(m, (X(22), UP + 5, 12, X(46), UP + 12, 42), 0.05, 1)
    # секретка: треснувшая стена на севере зала (ломается) → «Тайник предков»
    for y in range(UP, UP + 3):
        m.set(X(28), y, 10, 'infested_cracked_stone_bricks')
        m.set(X(28), y, 11, 'infested_cracked_stone_bricks')
    s.cache('ancestors', (X(28), UP, 5), 3)
    m.set(X(28), UP, 5, 'litrpg:secret_cache', facing='south')
    candles(m, X(26), UP, 3, 'purple', 4)
    candles(m, X(31), UP, 8, 'purple', 4)
    s.say((X(22), UP - 1, 12, X(46), UP + 9, 42),
          'Система: «Здесь лежат те, кто вошёл до вас. Склеп помнит каждого — и не любит, когда его тревожат.»')

    lv = lambda k: k  # уровень монстров этапа 1 — как есть
    p1 = s.pack('p1', [('crypt_warrior', X(30), UP, 25, lv(1)), ('crypt_warrior', X(31), UP, 29, lv(1)),
                       ('crypt_archer', X(36), UP, 27, lv(1)), ('crypt_warrior', X(30), UP, 27, lv(1), None, 4)])
    p2 = s.pack('p2', [('bone_rat', X(40), UP, 18, lv(2)), ('bone_rat', X(41), UP, 19, lv(2)), ('bone_rat', X(42), UP, 18, lv(2)),
                       ('acolyte', X(43), UP, 20, lv(2)), ('bone_rat', X(40), UP, 20, lv(2), None, 5)])
    p3 = s.pack('p3', [('bone_guard', X(42), UP, 34, lv(3)), ('crypt_warrior', X(40), UP, 36, lv(2)),
                       ('crypt_archer', X(44), UP, 37, lv(2)), ('crypt_archer', X(43), UP, 39, lv(2), None, 4)])

    # ------------------------------------------------------------------ Рычаги трёх саркофагов
    levers = []
    for (lx, lz, f) in ((X(33), 48, 'north'), (X(59), 10, 'south'), (X(59), 44, 'north')):
        # рычаг на камне у стены, саркофаг — перед ним
        back = 1 if f == 'north' else -1
        sarcophagus(m, lx - 1, UP, lz - 3 * back, 'x')
        m.set(lx, UP + 1, lz + back, 'chiseled_stone_bricks')
        m.set(lx, UP + 1, lz, 'lever', face='wall', facing=f, powered=False)
        levers.append((lx, UP + 1, lz))
    mech_levers = s.mech('levers', 'levers', levers, text='Рычаги трёх саркофагов')
    for x in range(X(53), X(66), 4):
        for z in (21, 33):
            column(m, x, z, UP, m.ceil_y(x, UP, z) - UP + 1)
    for (bx, bz) in ((X(54), 25), (X(54), 29)):
        brazier(m, bx, UP, bz)
    p4 = s.pack('p4', [('ghoul', X(58), UP, 14, lv(3)), ('ghoul', X(60), UP, 15, lv(3)), ('crypt_archer', X(59), UP, 12, lv(3), None, 4)])
    p5 = s.pack('p5', [('crypt_warrior', X(58), UP, 39, lv(3)), ('acolyte', X(62), UP, 39, lv(3)),
                       ('bone_rat', X(57), UP, 38, lv(3)), ('bone_rat', X(61), UP, 38, lv(3), None, 4)])
    m.sign(X(66), UP + 1, 24, 'west')
    s.sign((X(66), UP + 1, 24), ['Три саркофага', 'три рычага.', 'Мёртвые спят —', 'буди их всех.'])
    g1 = s.gate('g1', (X(67), UP, 26, X(67), UP + 3, 28), open_when=[f'mech:{mech_levers}'])

    # ------------------------------------------------------------------ Коридор шипов (полосы через одну)
    s.mech('spikes', 'hazard', area=(X(71), UP, 25, X(91), UP, 29), pattern=2, stripe=2, period=2.2, warn=1.0, share=0.22)
    for x in range(X(71), X(92), 2):
        m.set(x, UP - 1, 27, 'chiseled_stone_bricks')
    s.say((X(70), UP - 1, 24, X(74), UP + 4, 30), 'Система: «Полосы на полу вспыхивают по очереди. Идите за погасшими.»')

    # ------------------------------------------------------------------ Арена Привратника
    for i in range(8):
        a = i * math.pi / 4
        bx, bz = round(X(110) + 12 * math.cos(a)), round(27 + 12 * math.sin(a))
        column(m, bx, bz, UP, m.ceil_y(bx, UP, bz) - UP + 1, 'stone_bricks', 'chiseled_stone_bricks', 'chiseled_stone_bricks')
    bones_pile(m, X(104), UP, 21, seed=4)
    bones_pile(m, X(116), UP, 34, seed=5)
    b1 = s.boss('gatekeeper', 'gatekeeper', (X(110), UP, 27), 14)
    s.gate('arena1', (X(95), UP, 26, X(95), UP + 3, 28), lock=b1)
    g2 = s.gate('g2', (X(126), UP, 26, X(126), UP + 3, 28), open_when=[f'boss:{b1}'])
    s.say((X(96), UP - 1, 13, X(124), UP + 10, 41), 'Система: «Привратник не пропускает живых. Стойте за спиной у того, кто держит его взгляд.»')

    # ------------------------------------------------------------------ Зал Трёх Стражей (плиты)
    for x in range(X(143), X(162), 6):
        for z in (17, 37):
            column(m, x, z, LOW, m.ceil_y(x, LOW, z) - LOW + 1, 'deepslate_bricks', 'chiseled_deepslate', 'chiseled_deepslate')
    plates = [(X(152), LOW, 4), (X(169), LOW + 4, 27), (X(142), LOW, 27)]
    for (px, py, pz) in plates:
        m.set(px, py, pz, 'polished_blackstone_pressure_plate', powered=False)
        m.set(px, py - 1, pz, 'chiseled_deepslate')
    mech_plates = s.mech('plates', 'plates', plates, text='Плиты трёх стражей', hold=3)
    for (px, py, pz) in plates:
        brazier(m, px + (1 if px < X(160) else -1), py, pz + 1)
    m.sign(X(140), LOW + 1, 24, 'east')
    s.sign((X(140), LOW + 1, 24), ['Три стража', 'стоят порознь.', 'Встаньте на все', 'плиты разом.'])
    s.say((X(140), LOW - 1, 14, X(164), LOW + 8, 40), 'Система: «Три плиты в трёх концах зала. Одному их не удержать — придётся разделиться.»')
    p6 = s.pack('p6', [('crypt_warrior', X(150), LOW, 24, lv(5)), ('crypt_warrior', X(152), LOW, 30, lv(5)),
                       ('acolyte', X(156), LOW, 27, lv(5)), ('crypt_archer', X(158), LOW, 22, lv(5), None, 4)])
    p7 = s.pack('p7', [('bone_guard', X(160), LOW, 33, lv(6)), ('ghoul', X(158), LOW, 36, lv(5)), ('ghoul', X(161), LOW, 31, lv(5), None, 5)])
    g3 = s.gate('g3', (X(165), LOW, 17, X(165), LOW + 3, 19), open_when=[f'mech:{mech_plates}'])

    # ------------------------------------------------------------------ Оссуарий: Костолом и запертый тайник
    for x in range(X(147), X(160), 2):
        bones_pile(m, x, LOW, 55, r=1.2, seed=x)
    s.pack('bonebreaker', [('crypt_warrior', X(153), LOW, 50, lv(6)), ('bone_rat', X(150), LOW, 48, lv(5)),
                           ('bone_rat', X(156), LOW, 48, lv(5))], title='Костолом')
    m.set(X(153), LOW, 55, 'litrpg:locked_cache', facing='north')
    s.cache('ossuary', (X(153), LOW, 55), 2)
    for y in range(LOW, LOW + 3):
        m.set(X(161), y, 52, 'infested_deepslate')
        m.set(X(161), y, 53, 'infested_deepslate')
    m.set(X(165), LOW, 53, 'litrpg:secret_cache', facing='west')
    s.cache('ossuary_secret', (X(165), LOW, 53), 3)
    s.say((X(146), LOW - 1, 44, X(160), LOW + 6, 56), 'Система: «Костолом собирает кости тех, кто сюда забрёл. Его ключ откроет его же сундук.»')

    # ------------------------------------------------------------------ Склеп послушников
    for z in (12, 22):
        for x in range(X(176), X(188), 3):
            sarcophagus(m, x, LOW, z, 'x', lid='polished_deepslate', body='deepslate_bricks', skull=False)
    for (cx, cz) in ((X(180), 17), (X(184), 17)):
        candles(m, cx, LOW, cz, 'green', 4)
    p8 = s.pack('p8', [('acolyte', X(179), LOW, 15, lv(6)), ('acolyte', X(183), LOW, 19, lv(6)),
                       ('ghoul', X(181), LOW, 17, lv(6)), ('ghoul', X(185), LOW, 15, lv(6)), ('crypt_warrior', X(177), LOW, 19, lv(6), None, 4)])

    # ------------------------------------------------------------------ Коридор маятников
    s.mech('blades', 'hazard', area=(X(193), LOW, 15, X(205), LOW, 19), pattern=1, stripe=1, period=1.6, warn=0.8, share=0.25, sound=2)
    for x in range(X(193), X(206), 2):
        m.chain_lantern(x, LOW + 4, 17, 1, soul=False)

    # ------------------------------------------------------------------ Капелла Сестёр
    for i in range(10):
        a = i * math.pi / 5
        bx, bz = round(X(226) + 13 * math.cos(a)), round(17 + 13 * math.sin(a))
        column(m, bx, bz, LOW, m.ceil_y(bx, LOW, bz) - LOW + 1, 'polished_blackstone_bricks', 'gilded_blackstone', 'chiseled_polished_blackstone')
    carpet_line(m, X(212), 17, X(240), 17, LOW, 'red')
    for (cx, cz) in ((X(222), 13), (X(222), 21), (X(230), 13), (X(230), 21)):
        candles(m, cx, LOW, cz, 'red', 4)
    b2 = s.boss('sisters', 'morra', (X(226), LOW, 17), 15)
    s.gate('arena2', (X(211), LOW, 16, X(211), LOW + 3, 18), lock=b2)
    g4 = s.gate('g4', (X(243), LOW, 16, X(243), LOW + 3, 18), open_when=[f'boss:{b2}'])
    s.say((X(211), LOW - 1, 2, X(241), LOW + 10, 32), 'Система: «Сёстры Морра и Весса. Пала одна — вторая её поднимет. Убейте их вместе.»')
    p9 = s.pack('p9', [('ghoul', X(201), LOW, 16, lv(7)), ('ghoul', X(203), LOW, 18, lv(7)), ('crypt_archer', X(197), LOW, 17, lv(7))])

    # ------------------------------------------------------------------ Алтарь загадки
    m.set(X(254), LOW, 17, 'lectern', facing='west', has_book=False, powered=False)
    for (cx, cz) in ((X(253), 15), (X(253), 19), (X(255), 15), (X(255), 19)):
        candles(m, cx, LOW, cz, 'white', 4)
    m.sign(X(260), LOW + 1, 15, 'west')
    s.sign((X(260), LOW + 1, 15), ['Чем больше из', 'неё берёшь,', 'тем больше она', 'становится.'])
    m.sign(X(260), LOW + 1, 19, 'west')
    s.sign((X(260), LOW + 1, 19), ['Ответ скажи', 'вслух у алтаря', '(напиши', 'в чат).'])
    mech_riddle = s.mech('riddle', 'riddle', area=(X(250), LOW, 12, X(258), LOW + 3, 22), answers=['яма', 'дыра', 'ямка'],
                         text='Загадка алтаря|Чем больше из неё берёшь, тем больше она становится. Что это?')
    s.cache('altar', (X(249), LOW, 11), 1)
    m.set(X(249), LOW, 11, 'litrpg:cache', facing='south')
    p10 = s.pack('p10', [('crypt_warrior', X(252), LOW, 13, lv(8)), ('bone_guard', X(256), LOW, 21, lv(8)),
                         ('acolyte', X(257), LOW, 13, lv(8)), ('crypt_archer', X(251), LOW, 22, lv(8), None, 4)])
    g5 = s.gate('g5', (X(263), LOW, 16, X(263), LOW + 3, 18), open_when=[f'mech:{mech_riddle}', f'pack:{p10}'])
    s.say((X(248), LOW - 1, 10, X(260), LOW + 6, 24), 'Система: «Алтарь задаёт вопрос. Ответ напишите в чат, стоя рядом.»')

    # ------------------------------------------------------------------ Усыпальница Карна и Тронный зал
    for i in range(10):
        a = i * math.pi / 5
        bx, bz = round(X(282) + 13 * math.cos(a)), round(17 + 13 * math.sin(a))
        column(m, bx, bz, LOW, m.ceil_y(bx, LOW, bz) - LOW + 1, 'deepslate_tiles', 'packed_ice', 'chiseled_deepslate')
    for i in range(8):
        a = i * math.pi / 4 + 0.3
        sx_, sz_ = round(X(282) + 9 * math.cos(a)), round(17 + 9 * math.sin(a))
        m.set(sx_, LOW, sz_, 'blue_ice')
    for dx in (-6, 6):
        for dz in (-6, 6):
            brazier(m, X(282 + dx), LOW, 58 + dz, soul=True)
    # трон на севере Тронного зала
    for dx in (-1, 0, 1):
        m.set(X(282 + dx), LOW, 46, 'polished_deepslate')
    m.stairs(X(282), LOW + 1, 46, 'polished_deepslate_stairs', 'south')
    m.set(X(281), LOW + 1, 46, 'polished_deepslate_wall', up=True, north='none', south='none', east='none', west='none', waterlogged=False)
    m.set(X(283), LOW + 1, 46, 'polished_deepslate_wall', up=True, north='none', south='none', east='none', west='none', waterlogged=False)
    b3 = s.boss('lich', 'lich', (X(282), LOW, 17), 15, final=True, arena2=(X(282) + 0.5, LOW, 58.5, 13))
    s.gate('arena3', (X(267), LOW, 16, X(267), LOW + 3, 18), lock=b3)
    s.gate('throne', (X(281), LOW, 40, X(283), LOW + 3, 40), open_when=[f'boss:{b3}'])
    s.say((X(267), LOW - 1, 2, X(297), LOW + 11, 32), 'Система: «Король-лич Карн. Когда погаснет свет — держитесь у душевных жаровен.»')

    # разлом в нише Тронного зала
    for dx in (-2, 2):
        for dy in range(0, 4):
            m.set(X(282 + dx), LOW + dy, 74, 'crying_obsidian')
    for dx in (-1, 0, 1):
        m.set(X(282 + dx), LOW + 3, 74, 'crying_obsidian')
        for dy in range(0, 3):
            m.set(X(282 + dx), LOW + dy, 74, 'litrpg:rift', active=False)
    apply_gates(m, s)

    # свет
    m.hanging_lights(spacing=6, soul=False, themes=['crypt'], min_h=6, chain=2)
    m.hanging_lights(spacing=6, soul=True, themes=['catacomb', 'tomb'], min_h=6, chain=2)
    m.hanging_lights(spacing=6, soul=False, themes=['chapel'], min_h=6, chain=3, offset=3)
    m.glow(spacing=4, level=9, height=3)

    s.box = (X(-4), LOW - 10, -4, X(304), UP + 24, 90)
    s.camp = (X(8.5), UP, 30.5, 180)
    s.trader = (X(3.5), UP, 27.5, -90)
    s.rift = (X(281), LOW, 74, X(283), LOW + 2, 74)
    return m, s
