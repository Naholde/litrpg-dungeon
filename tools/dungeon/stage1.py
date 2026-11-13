"""
Этап 1 · Склеп Забытых (уровни 1–10). Обучение: разметка, агро, сбивать подготовку, разделение отряда —
и (обновление 14) вертикаль: склеп уходит вниз четырьмя ярусами, каждый спуск — свой приём набора.

Ярус I · Верхний склеп (ноги 84): Сторожка (привал) → Зал Предков (3 отряда; лоза по стене в нишу под сводом,
  за трещиной — тайник) → Галерея саркофагов (рычаги: на хорах по лестнице, в Нижнем склепе по пандусу вниз,
  в нише) → Коридор шипов → Арена Привратника (r 18, колоннада по краю) → Край колодца.
Ярус II · Катакомбы (62): «Колодец Падших» — прыжок с 22 блоков в купель (дропер) → Чаша Падших (засада;
  отсюда шахта-срезка с лестницей и ловушкой ярусами наверх, к привалу) → Зал Трёх Стражей (плиты: в северной
  нише, на балконе по лестничному маршу, в яме по пандусу) → Оссуарий (верёвочный мост с провисом над пропастью,
  Костолом, запертый тайник; паркур по столбам к тайнику над бездной) → Склеп послушников.
Ярус III · Капелла (44): винтовая лестница вниз на 18 блоков → Притвор → Коридор маятников → Капелла Сестёр
  (вытянутый зал-овал 44×37, арена r 18) → Алтарь загадки (ступенчатое возвышение).
Ярус IV · Усыпальница (30): парадная лестница вниз → Усыпальница Карна (r 19) → Тронный зал (второй зал лича,
  душевные жаровни в ±6 от центра — их ищет «Во тьме») → ниша разлома.

Арены — по `arenas.py` (замер приёмов): Привратник 17 → 18, Сёстры 17 → 18, Лич 18 → 19.
"""
import math

from decor import banner, bones_pile, brazier, candles, carpet_line, cobwebs, column, floor_pattern, sarcophagus
from kit import DIRV, Map, Stage, Theme, apply_gates

N = 1
OX = 1024 * N
STRICT = True                  # строгая проверка: обрывы, западни, арены — ошибки
A, B, C, D = 84, 62, 44, 30    # ярусы: уровень ног


def X(x):
    return OX + x


def lever(m, x, y, z, facing, back='chiseled_stone_bricks'):
    """Рычаг на стене: facing — куда смотрит (от стены к игроку); за ним — резной камень."""
    dx, dz = DIRV[facing]
    m.set(x - dx, y, z - dz, back)
    m.set(x, y, z, 'lever', face='wall', facing=facing, powered=False)
    return (x, y, z)


def oval_room(m, cx, cz, rx, rz, y, h, theme, dome=0):
    """Вытянутый зал: эллипс с полуосями rx (вдоль x) и rz, купол dome."""
    for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
        for z in range(int(cz - rz) - 1, int(cz + rz) + 2):
            k = ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2
            if k <= 1:
                top = h + int(round(dome * math.sqrt(max(0.0, 1 - k))))
                m.carve(x, y, z, x, y + top - 1, z, theme)


def flight(m, x1, x2, zs, y, block):
    """Лестничный марш из полных блоков: k-я клетка списка zs поднимает ноги на k блоков над y."""
    for k, z in enumerate(zs, 1):
        m.fill(x1, y, z, x2, y + k - 1, z, block)


def colonnade(m, cx, cz, r, n, y, body, base_, cap, rz=None, phase=0.5):
    """Колонны по кругу (или эллипсу rz) в n местах; phase=0.5 — мимо осей, где входы."""
    for i in range(n):
        a = (i + phase) * 2 * math.pi / n
        bx, bz = round(cx + r * math.cos(a)), round(cz + (rz or r) * math.sin(a))
        column(m, bx, bz, y, m.ceil_y(bx, y, bz) - y + 1, body, base_, cap)


# Эмблемы ролей 7×7 (сверху вниз, слева направо — как видит игрок, стоя лицом к нише): a — цвет роли, b — второй, y — золото.
EMBLEMS = {
    'tank': ('blue', 'light_blue', ['.aaaaa.', 'abbbbba', 'abbybba', 'abyyyba', 'abbybba', '.abbba.', '..aaa..']),
    'damage': ('red', 'orange', ['......a', '.....ab', '....ab.', 'y..ab..', '.yab...', '.ay....', 'a..y...']),
    'support': ('lime', 'white', ['..aaa..', '..aba..', 'aaabaaa', 'abbbbba', 'aaabaaa', '..aba..', '..aaa..']),
}
ROLE_TEXT = {'tank': ['ТАНК', 'держит хранителя', 'и врагов на себе.', 'Провокация'],
             'damage': ['БОЕЦ', 'главный урон.', 'Не стой', 'в разметке!'],
             'support': ['ПОДДЕРЖКА', 'лечит отряд', 'и поднимает', 'павших']}


def role_hall(m, s):
    """Зал ролей (обновление 15): роль выбирают здесь, на привале этапа 1, — окно роли открывается само (клавиша K).
    Три ниши с витражами-эмблемами: север — танк, запад — боец, восток — поддержка. Выход с привала закрыт,
    пока роль не выбрал каждый из отряда (ворота `roles`)."""
    floor_pattern(m, X(2), 7, X(14), 17, A, ['polished_andesite', 'chiseled_stone_bricks', 'polished_andesite'], 4)
    carpet_line(m, X(8), 8, X(8), 18, A, 'gray')
    carpet_line(m, X(3), 12, X(13), 12, A, 'gray')
    for name, (side, (gx, gz)) in {'tank': ('north', (X(5), 2)), 'damage': ('west', (X(-2), 15)),
                                   'support': ('east', (X(18), 9))}.items():
        col, col2, rows = EMBLEMS[name]
        for j, row in enumerate(rows):
            y = A + 6 - j
            for i, ch in enumerate(row):
                if side == 'north':     # игрок смотрит на север: слева — запад
                    g, lt, bk = (gx + i, y, gz), (gx + i, y, gz - 1), (gx + i, y, gz - 2)
                elif side == 'west':    # смотрит на запад: слева — юг
                    g, lt, bk = (gx, y, gz - i), (gx - 1, y, gz - i), (gx - 2, y, gz - i)
                else:                   # смотрит на восток: слева — север
                    g, lt, bk = (gx, y, gz + i), (gx + 1, y, gz + i), (gx + 2, y, gz + i)
                if ch == '.':
                    m.set(*g, 'chiseled_stone_bricks')
                    continue
                c = {'a': col, 'b': col2, 'y': 'yellow'}[ch]
                m.set(*g, f'{c}_stained_glass')
                m.set(*lt, 'light', level=15, waterlogged=False)
                m.set(*bk, 'white_concrete')
        # постамент со свечами и табличка перед нишей
        px, pz, f = {'north': (X(8), 6, 'south'), 'west': (X(1), 12, 'east'), 'east': (X(15), 12, 'west')}[side]
        m.set(px, A, pz, 'chiseled_stone_bricks')
        m.set(px, A + 1, pz, f'{col}_candle', candles=4, lit=True, waterlogged=False)
        dx, dz = {'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}[f]
        m.sign(px + dx, A, pz + dz, f)
        s.sign((px + dx, A, pz + dz), ROLE_TEXT[name])
    m.sign(X(10), A + 1, 18, 'north')
    s.sign((X(10), A + 1, 18), ['ЗАЛ РОЛЕЙ', 'Выберите роль:', 'клавиша K.', 'Нужны все три'])
    s.say((X(0), A - 1, 2, X(17), A + 9, 34),
          'Система: «Здесь каждый выбирает, кем идти: танк держит, боец бьёт, поддержка лечит. Окно роли — клавиша K. '
          'Путь вниз откроется, когда выберут все.»')


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

    # ================================================================== пустоты
    # ---- Ярус I · Верхний склеп (84)
    m.room(X(0), 20, X(16), 34, A, 6, crypt, vault=2)                          # Сторожка
    m.room(X(1), 6, X(15), 18, A, 9, crypt, vault=3)                            # Зал ролей (обновление 15)
    m.carve(X(7), A, 19, X(9), A + 3, 19, crypt)                                # проход из Сторожки
    m.carve(X(5), A, 3, X(11), A + 7, 5, crypt)                                 # ниша танка (север)
    m.carve(X(-1), A, 9, X(0), A + 7, 15, crypt)                                # ниша бойца (запад)
    m.carve(X(16), A, 9, X(17), A + 7, 15, crypt)                               # ниша поддержки (восток)
    m.corridor([(X(16), 27), (X(22), 27)], 3, 4, crypt, y=A)
    m.room(X(22), 12, X(46), 42, A, 10, crypt, vault=4)                         # Зал Предков
    m.room(X(26), 7, X(31), 11, A + 6, 4, crypt)                                # ниша под сводом (лоза)
    m.room(X(27), 3, X(30), 5, A + 6, 3, crypt)                                 # тайник предков за трещиной
    m.corridor([(X(46), 27), (X(52), 27)], 3, 4, crypt, y=A)
    m.room(X(52), 16, X(72), 38, A, 11, crypt, vault=3)                         # Галерея саркофагов
    m.room(X(56), 10, X(68), 15, A + 6, 4, crypt)                               # хоры (рычаг 2)
    m.corridor([(X(62), 38, A), (X(62), 41, A), (X(62), 47, A - 5)], 3, 4, crypt)   # пандус вниз
    m.room(X(57), 47, X(67), 54, A - 5, 5, crypt, vault=1)                      # Нижний склеп (рычаг 1)
    m.room(X(52), 39, X(56), 44, A, 4, crypt)                                   # юго-западная ниша (рычаг 3)
    m.corridor([(X(72), 27), (X(78), 27)], 3, 4, crypt, y=A)
    m.room(X(78), 25, X(100), 29, A, 5, crypt)                                  # Коридор шипов
    m.corridor([(X(100), 27), (X(104), 27)], 3, 4, crypt, y=A)
    m.round_room(X(122), 27, 18, A, 12, crypt, dome=5)                          # Арена Привратника
    m.corridor([(X(140), 27), (X(148), 27)], 3, 4, crypt, y=A)
    m.room(X(148), 18, X(166), 36, A, 8, crypt, vault=3)                        # Край колодца
    m.corridor([(X(152), 36), (X(152), 40)], 3, 4, crypt, y=A)
    m.room(X(150), 39, X(170), 44, A, 5, crypt)                                 # Верхняя площадка срезки

    # ---- Ярус II · Катакомбы (62)
    m.room(X(146), 18, X(170), 36, B, 8, lower, vault=3)                        # Чаша Падших
    m.dropper(X(156), 26, X(158), 28, A, B, lower, depth=3)                     # Колодец Падших: 22 блока в купель
    # срезка наверх: шахта 2×2, лестница во всю ширину у южной стены, у входа — плачущая лоза с потолка
    m.carve(X(164), B, 37, X(165), A + 2, 38, lower)
    m.ladder(X(164), 38, B, A - 1, 'north', width=2)
    m.vines(X(164), 37, B, A - 1, kind='weeping', to=(X(165), 37))
    m.sleeve(X(164), 37, X(165), 38, B + 5, B + 8, 'polished_deepslate')        # стенка между шахтой и Чашей выше 5 блоков
    m.corridor([(X(170), 27), (X(176), 27)], 3, 4, lower, y=B)
    m.room(X(176), 12, X(202), 42, B, 10, lower, vault=3)                       # Зал Трёх Стражей
    m.room(X(185), 5, X(193), 11, B, 5, lower)                                  # северная ниша (плита 1)
    m.room(X(203), 22, X(208), 34, B + 5, 4, lower)                             # балкон (плита 2)
    m.corridor([(X(182), 42, B), (X(182), 43, B), (X(182), 47, B - 4)], 3, 4, lower)   # пандус в яму
    m.room(X(177), 47, X(187), 53, B - 4, 5, lower)                             # Яма стража (плита 3)
    m.corridor([(X(202), 16), (X(212), 16)], 3, 4, lower, y=B)
    m.room(X(212), 8, X(256), 30, B, 12, lower, vault=3)                        # Оссуарий
    m.pit(X(220), 8, X(236), 30, B, B - 12, lower)                              # пропасть до бездны
    m.corridor([(X(256), 19), (X(262), 19)], 3, 4, lower, y=B)
    m.room(X(262), 12, X(276), 26, B, 6, lower, vault=2)                        # Склеп послушников
    m.corridor([(X(276), 19), (X(282), 19)], 3, 4, lower, y=B)

    # ---- Ярус III · Капелла (44): винтовая лестница вниз
    m.spiral(X(286) + 0.5, 19.5, C, B, 4, chapel, step='polished_blackstone_bricks', core='chiseled_polished_blackstone',
             per_turn=12, start=0.0, rail='polished_blackstone_wall', underside='polished_blackstone_brick_stairs')
    m.room(X(291), 13, X(297), 25, C, 6, chapel, vault=1)                       # Притвор
    m.room(X(298), 17, X(314), 21, C, 5, chapel)                                # Коридор маятников
    m.corridor([(X(314), 19), (X(320), 19)], 3, 4, chapel, y=C)
    oval_room(m, X(341), 19, 22, 18.5, C, 11, chapel, dome=5)                  # Капелла Сестёр
    m.corridor([(X(363), 19), (X(370), 19)], 3, 4, chapel, y=C)
    m.room(X(370), 8, X(390), 30, C, 8, chapel, vault=3)                        # Алтарь загадки
    # ---- Ярус IV · Усыпальница (30): парадная лестница вниз на 14 блоков
    m.corridor([(X(390), 19, C), (X(392), 19, C), (X(406), 19, D), (X(414), 19, D)], 5, 5, tomb)
    m.round_room(X(432), 19, 19, D, 12, tomb, dome=5)                           # Усыпальница Карна
    m.corridor([(X(432), 37), (X(432), 47)], 3, 4, tomb, y=D)
    m.round_room(X(432), 62, 16, D, 12, tomb, dome=6)                           # Тронный зал
    m.room(X(429), 77, X(435), 84, D, 6, tomb)                                  # ниша разлома

    # ---- вертикаль до оболочки (постройки откладываются до неё сами)
    m.bridge((X(219), 19), (X(237), 19), B, 'spruce_planks', width=3, rail='spruce_fence', posts='stripped_spruce_log',
             post_every=6, sag=1, under='spruce_log')
    m.parkour([(X(222), B, 10), (X(225), B, 10), (X(228), B + 1, 10)], 'polished_deepslate', pillar='deepslate_brick_wall')
    m.platform(X(231) + 0.5, 10.5, B + 1, 2.2, 'polished_deepslate', rim='deepslate_tiles', support='pillar',
               support_block='deepslate_bricks', chains=True)
    m.vines(X(28), 12, A, A + 5, kind='vine', facing='south')                   # лоза в нишу Зала Предков
    m.ladder(X(62), 16, A, A + 5, 'south')                                      # лестница на хоры
    m.shell(2)

    # ================================================================== Сторожка (привал)
    floor_pattern(m, X(1), 21, X(15), 33, A, ['polished_andesite', 'stone_bricks', 'chiseled_stone_bricks'], 3)
    m.set(X(8), A, 27, 'campfire', facing='north', lit=True, signal_fire=False, waterlogged=False)
    for dx, dz in ((-2, 0), (2, 0), (0, -2), (0, 2)):
        m.stairs(X(8 + dx), A, 27 + dz, 'spruce_stairs', {(-2, 0): 'east', (2, 0): 'west', (0, -2): 'south', (0, 2): 'north'}[(dx, dz)])
    for z in (22, 32):
        m.set(X(3), A, z, 'barrel', facing='up', open=False)
        m.set(X(4), A, z, 'barrel', facing='up', open=False)
    banner(m, X(1), A + 3, 25, 'east', 'gray')
    banner(m, X(1), A + 3, 29, 'east', 'gray')
    m.sign(X(16), A + 1, 24, 'west')
    s.sign((X(16), A + 1, 24), ['ЭТАП 1', 'Склеп', 'Забытых', 'уровни 1–10'])

    role_hall(m, s)

    # ================================================================== Зал Предков
    for x in range(X(25), X(44), 6):
        for z in (16, 38):
            column(m, x, z, A, m.ceil_y(x, A, z) - A + 1)
    for x in (X(33), X(39)):
        sarcophagus(m, x, A, 22, 'x')
        sarcophagus(m, x, A, 32, 'x')
        candles(m, x - 1, A, 24, None, 3)
    sarcophagus(m, X(27), A, 32, 'x')
    carpet_line(m, X(23), 27, X(45), 27, A, 'red')
    bones_pile(m, X(44), A, 14, seed=1)
    bones_pile(m, X(24), A, 40, seed=2)
    cobwebs(m, (X(22), A + 5, 12, X(46), A + 13, 42), 0.05, 1)
    # ниша под сводом: лоза по северной стене, бортик по краю ниши (вход — только с лозы)
    m.railing([(X(x), A + 6, 11) for x in (26, 27, 29, 30, 31)], 'stone_brick_wall')
    candles(m, X(26), A + 6, 8, 'purple', 4)
    candles(m, X(31), A + 6, 8, 'purple', 4)
    for y in range(A + 6, A + 8):                                               # трещина в задней стене ниши
        m.set(X(28), y, 6, 'infested_cracked_stone_bricks')
        m.set(X(29), y, 6, 'infested_cracked_stone_bricks')
    m.set(X(28), A + 6, 3, 'litrpg:secret_cache', facing='south')
    s.cache('ancestors', (X(28), A + 6, 3), 3)
    bones_pile(m, X(30), A + 6, 4, r=1, seed=9)
    s.say((X(22), A - 1, 12, X(46), A + 9, 42),
          'Система: «Здесь лежат те, кто вошёл до вас. Склеп помнит каждого — и не любит, когда его тревожат. '
          'Смотрите не только под ноги: по стенам склепа можно забраться.»')

    lv = lambda k: k  # уровень монстров этапа 1 — как есть
    s.pack('p1', [('crypt_warrior', X(30), A, 25, lv(1)), ('crypt_warrior', X(31), A, 29, lv(1)),
                  ('crypt_archer', X(36), A, 27, lv(1)), ('crypt_warrior', X(30), A, 27, lv(1), None, 4)])
    s.pack('p2', [('bone_rat', X(40), A, 18, lv(2)), ('bone_rat', X(41), A, 19, lv(2)), ('bone_rat', X(42), A, 18, lv(2)),
                  ('acolyte', X(43), A, 20, lv(2)), ('bone_rat', X(40), A, 20, lv(2), None, 5)])
    s.pack('p3', [('bone_guard', X(42), A, 34, lv(3)), ('crypt_warrior', X(40), A, 36, lv(2)),
                  ('crypt_archer', X(44), A, 37, lv(2)), ('crypt_archer', X(43), A, 39, lv(2), None, 4)])

    # ================================================================== Галерея саркофагов: три рычага на трёх высотах
    levers = [lever(m, X(62), A - 4, 54, 'north'),        # Нижний склеп — по пандусу вниз
              lever(m, X(62), A + 7, 10, 'south'),        # хоры — по лестнице вверх
              lever(m, X(54), A + 1, 44, 'north')]        # ниша на полу галереи
    mech_levers = s.mech('levers', 'levers', levers, text='Рычаги трёх саркофагов')
    sarcophagus(m, X(61), A - 5, 51, 'x')
    sarcophagus(m, X(60), A + 6, 12, 'x')
    sarcophagus(m, X(53), A, 41, 'x')
    for (bx, bz) in ((X(58), 50), (X(66), 50)):
        brazier(m, bx, A - 5, bz, soul=True)
    m.railing([(X(x), A + 6, 15) for x in range(56, 69) if x != 62], 'stone_brick_wall')
    for x in (X(56), X(68)):
        m.chain_lantern(x, A + 9, 12, 1)
    for x in (X(55), X(60), X(65), X(70)):
        for z in (21, 33):
            column(m, x, z, A, m.ceil_y(x, A, z) - A + 1)
    for (bx, bz) in ((X(54), 25), (X(54), 29)):
        brazier(m, bx, A, bz)
    s.pack('p4', [('ghoul', X(59), A, 20, lv(3)), ('ghoul', X(65), A, 20, lv(3)),
                  ('crypt_archer', X(58), A + 6, 12, lv(3)), ('crypt_archer', X(66), A + 6, 12, lv(3), None, 4)])
    s.pack('p5', [('crypt_warrior', X(60), A - 5, 49, lv(3)), ('acolyte', X(64), A - 5, 52, lv(3)),
                  ('bone_rat', X(59), A - 5, 53, lv(3)), ('bone_rat', X(65), A - 5, 48, lv(3), None, 4)])
    m.sign(X(72), A + 1, 24, 'west')
    s.sign((X(72), A + 1, 24), ['Три рычага:', 'над головой,', 'под ногами', 'и у стены.'])
    s.say((X(52), A - 1, 16, X(72), A + 10, 38),
          'Система: «Три саркофага, три рычага — один на хорах, другой в склепе под полом, третий в нише. '
          'Разделитесь, так быстрее.»')
    s.gate('roles', (X(19), A, 26, X(19), A + 3, 28), open_when=['roles'])     # роль у всех — путь открыт
    s.gate('g1', (X(75), A, 26, X(75), A + 3, 28), open_when=[f'mech:{mech_levers}'])

    # ================================================================== Коридор шипов (полосы через одну)
    s.mech('spikes', 'hazard', area=(X(79), A, 25, X(99), A, 29), pattern=2, stripe=2, period=2.2, warn=1.0, share=0.22)
    for x in range(X(79), X(100), 2):
        m.set(x, A - 1, 27, 'chiseled_stone_bricks')
    s.say((X(78), A - 1, 24, X(82), A + 4, 30), 'Система: «Полосы на полу вспыхивают по очереди. Идите за погасшими.»')

    # ================================================================== Арена Привратника (r 18)
    colonnade(m, X(122), 27, 17.3, 12, A, 'stone_bricks', 'chiseled_stone_bricks', 'chiseled_stone_bricks')
    bones_pile(m, X(114), A, 19, seed=4)
    bones_pile(m, X(130), A, 36, seed=5)
    bones_pile(m, X(126), A, 15, seed=6)
    b1 = s.boss('gatekeeper', 'gatekeeper', (X(122), A, 27), 18)
    s.gate('arena1', (X(102), A, 26, X(102), A + 3, 28), lock=b1)
    s.gate('g2', (X(144), A, 26, X(144), A + 3, 28), open_when=[f'boss:{b1}'])
    s.say((X(104), A - 1, 9, X(140), A + 12, 45),
          'Система: «Привратник не пропускает живых. Стойте за спиной у того, кто держит его взгляд.»')

    # ================================================================== Край колодца и Чаша Падших
    for (bx, bz) in ((X(153), 23), (X(161), 23), (X(153), 31), (X(161), 31)):
        brazier(m, bx, A, bz, soul=True)
    for (cx, cz) in ((X(155), 25), (X(159), 25), (X(155), 29), (X(159), 29)):
        candles(m, cx, A, cz, 'white', 4)
    bones_pile(m, X(150), A, 20, seed=11)
    m.sign(X(148), A + 1, 24, 'east')
    s.sign((X(148), A + 1, 24), ['Колодец Падших', 'Дальше путь —', 'только вниз.', 'Прыгайте.'])
    s.say((X(148), A - 1, 18, X(166), A + 8, 36),
          'Система: «Колодец Падших. Внизу — купель: вода примет любого, кто прыгнет. Назад наверх ведёт шахта у южной стены.»')
    m.sign(X(163), B + 1, 36, 'north', wood='spruce')
    s.sign((X(163), B + 1, 36), ['Подъём', 'к привалу.', 'Ярусы бьют', 'по очереди.'])
    s.climb_trap('shaft_trap', (X(164), B + 1, 37, X(165), A - 2, 38), period=3.0, warn=1.0, share=0.15, layer=2)
    s.say((X(160), B - 1, 30, X(170), B + 6, 38),
          'Система: «Шахта наверх — к привалу и торговцу. Ярусы вспыхивают по очереди: лезьте в такт, пережидая вспышку над головой.»')
    for x in range(X(147), X(170), 4):
        bones_pile(m, x, B, 35, r=1.2, seed=x)
    s.pack('p6', [('crypt_warrior', X(150), B, 22, lv(4)), ('crypt_warrior', X(152), B, 32, lv(4)),
                  ('acolyte', X(166), B, 22, lv(4)), ('crypt_archer', X(168), B, 30, lv(4)), ('ghoul', X(148), B, 27, lv(4), None, 4)])

    # ================================================================== Зал Трёх Стражей: плиты на трёх высотах
    for x in range(X(179), X(200), 6):
        for z in (16, 40):
            column(m, x, z, B, m.ceil_y(x, B, z) - B + 1, 'deepslate_bricks', 'chiseled_deepslate', 'chiseled_deepslate')
    flight(m, X(200), X(202), [38, 37, 36, 35, 34], B, 'deepslate_bricks')    # марш на балкон
    m.railing([(X(203), B + 5, z) for z in range(22, 33)], 'deepslate_brick_wall')
    plates = [(X(189), B, 7), (X(206), B + 5, 28), (X(182), B - 4, 51)]
    for (px, py, pz) in plates:
        m.set(px, py, pz, 'polished_blackstone_pressure_plate', powered=False)
        m.set(px, py - 1, pz, 'chiseled_deepslate')
    mech_plates = s.mech('plates', 'plates', plates, text='Плиты трёх стражей', hold=3)
    brazier(m, X(187), B, 6)
    brazier(m, X(207), B + 5, 25)
    brazier(m, X(180), B - 4, 52)
    m.sign(X(176), B + 1, 24, 'east')
    s.sign((X(176), B + 1, 24), ['Три стража:', 'в нише, наверху', 'и в яме.', 'Все плиты разом.'])
    s.say((X(176), B - 1, 12, X(202), B + 10, 42),
          'Система: «Три плиты: в северной нише, на балконе и в яме под полом. Одному их не удержать — придётся разделиться.»')
    s.pack('p7', [('bone_guard', X(190), B, 30, lv(5)), ('ghoul', X(186), B, 33, lv(5)), ('ghoul', X(193), B, 26, lv(5), None, 5)])
    s.pack('p7b', [('crypt_archer', X(205), B + 5, 24, lv(5)), ('crypt_archer', X(206), B + 5, 31, lv(5)),
                   ('acolyte', X(205), B + 5, 27, lv(5), None, 4)])
    s.pack('p7c', [('crypt_warrior', X(180), B - 4, 49, lv(5)), ('bone_rat', X(184), B - 4, 49, lv(5)),
                   ('bone_rat', X(183), B - 4, 52, lv(5))])
    s.gate('g3', (X(207), B, 15, X(207), B + 3, 17), open_when=[f'mech:{mech_plates}'])

    # ================================================================== Оссуарий: мост над пропастью, Костолом, паркур
    for x in range(X(240), X(256), 2):
        bones_pile(m, x, B, 29, r=1.2, seed=x)
        bones_pile(m, x, B, 9, r=1.2, seed=x + 1)
    for z in (11, 27):
        for x in (X(214), X(217)):
            column(m, x, z, B, m.ceil_y(x, B, z) - B + 1, 'deepslate_bricks', 'chiseled_deepslate', 'chiseled_deepslate')
    s.pack('bonebreaker', [('crypt_warrior', X(247), B, 19, lv(6)), ('bone_rat', X(244), B, 16, lv(5)),
                           ('bone_rat', X(244), B, 22, lv(5))], title='Костолом')
    m.set(X(255), B, 26, 'litrpg:locked_cache', facing='west')
    s.cache('ossuary', (X(255), B, 26), 2)
    m.set(X(231), B + 1, 10, 'litrpg:secret_cache', facing='west')
    s.cache('ossuary_secret', (X(231), B + 1, 10), 3)
    s.say((X(212), B - 1, 8, X(219), B + 8, 30),
          'Система: «Оссуарий. Мост держится на честном слове — но держится. Кто сорвётся, того Система вытащит, '
          'заплатив его здоровьем. Над бездной кто-то оставил тропу из столбов.»')
    s.say((X(238), B - 1, 8, X(256), B + 8, 30),
          'Система: «Костолом собирает кости тех, кто сюда забрёл. Его ключ откроет его же сундук.»')

    # ================================================================== Склеп послушников и винтовая лестница
    for z in (14, 24):
        for x in range(X(264), X(276), 3):
            sarcophagus(m, x, B, z, 'x', lid='polished_deepslate', body='deepslate_bricks', skull=False)
    for (cx, cz) in ((X(268), 19), (X(272), 19)):
        candles(m, cx, B, cz, 'green', 4)
    s.pack('p8', [('acolyte', X(267), B, 17, lv(6)), ('acolyte', X(271), B, 21, lv(6)),
                  ('ghoul', X(269), B, 19, lv(6)), ('ghoul', X(273), B, 17, lv(6)), ('crypt_warrior', X(265), B, 21, lv(6), None, 4)])
    s.say((X(276), B - 1, 14, X(282), B + 4, 24), 'Система: «Винтовая лестница уходит на восемнадцать блоков вниз — в Капеллу.»')

    # ================================================================== Притвор и Коридор маятников
    for (cx, cz) in ((X(292), 14), (X(296), 14), (X(292), 24), (X(296), 24)):
        candles(m, cx, C, cz, 'red', 3)
    s.pack('p9', [('ghoul', X(294), C, 16, lv(7)), ('ghoul', X(295), C, 22, lv(7)), ('crypt_archer', X(296), C, 19, lv(7))])
    s.mech('blades', 'hazard', area=(X(299), C, 17, X(313), C, 21), pattern=1, stripe=1, period=1.6, warn=0.8, share=0.25, sound=2)
    for x in range(X(299), X(314), 2):
        m.chain_lantern(x, C + 4, 19, 1, soul=False)

    # ================================================================== Капелла Сестёр (овал 44×37, арена r 18)
    colonnade(m, X(341), 19, 20.4, 14, C, 'polished_blackstone_bricks', 'gilded_blackstone', 'chiseled_polished_blackstone',
              rz=17.0)
    carpet_line(m, X(321), 19, X(361), 19, C, 'red')
    for (cx, cz) in ((X(333), 13), (X(333), 25), (X(349), 13), (X(349), 25)):
        candles(m, cx, C, cz, 'red', 4)
    for bz in (1, 37):
        banner(m, X(341), C + 4, bz, 'south' if bz == 1 else 'north', 'purple')
    b2 = s.boss('sisters', 'morra', (X(341), C, 19), 18)
    s.gate('arena2', (X(317), C, 18, X(317), C + 3, 20), lock=b2)
    s.gate('g4', (X(366), C, 18, X(366), C + 3, 20), open_when=[f'boss:{b2}'])
    s.say((X(319), C - 1, 0, X(363), C + 11, 38),
          'Система: «Сёстры Морра и Весса. Пала одна — вторая её поднимет. Убейте их вместе.»')

    # ================================================================== Алтарь загадки (ступенчатое возвышение)
    m.fill(X(378), C, 17, X(382), C, 21, 'polished_blackstone_bricks')
    m.fill(X(379), C + 1, 18, X(381), C + 1, 20, 'chiseled_polished_blackstone')
    m.set(X(380), C + 2, 18, 'lectern', facing='south', has_book=False, powered=False)
    for (cx, cz) in ((X(377), 16), (X(377), 22), (X(383), 16), (X(383), 22)):
        candles(m, cx, C, cz, 'white', 4)
    m.sign(X(390), C + 1, 13, 'west')
    s.sign((X(390), C + 1, 13), ['Чем больше из', 'неё берёшь,', 'тем больше она', 'становится.'])
    m.sign(X(390), C + 1, 25, 'west')
    s.sign((X(390), C + 1, 25), ['Ответ скажи', 'вслух у алтаря', '(напиши', 'в чат).'])
    mech_riddle = s.mech('riddle', 'riddle', area=(X(372), C, 10, X(388), C + 4, 28), answers=['яма', 'дыра', 'ямка'],
                         text='Загадка алтаря|Чем больше из неё берёшь, тем больше она становится. Что это?')
    s.cache('altar', (X(371), C, 9), 1)
    m.set(X(371), C, 9, 'litrpg:cache', facing='south')
    p10 = s.pack('p10', [('crypt_warrior', X(374), C, 12, lv(8)), ('bone_guard', X(386), C, 26, lv(8)),
                         ('acolyte', X(386), C, 12, lv(8)), ('crypt_archer', X(374), C, 26, lv(8), None, 4)])
    s.gate('g5', (X(391), C, 17, X(391), C + 4, 21), open_when=[f'mech:{mech_riddle}', f'pack:{p10}'])
    s.say((X(370), C - 1, 8, X(390), C + 6, 30), 'Система: «Алтарь задаёт вопрос. Ответ напишите в чат, стоя рядом.»')

    # ================================================================== Усыпальница Карна и Тронный зал
    colonnade(m, X(432), 19, 18.3, 12, D, 'deepslate_tiles', 'packed_ice', 'chiseled_deepslate')
    for i in range(8):
        a = i * math.pi / 4 + 0.3
        m.set(round(X(432) + 9 * math.cos(a)), D - 1, round(19 + 9 * math.sin(a)), 'blue_ice')
    for dx in (-6, 6):                       # душевные жаровни: «Во тьме» видно только у них (LichKing.BRAZIER = 6)
        for dz in (-6, 6):
            brazier(m, X(432 + dx), D, 62 + dz, soul=True)
    for dx in (-1, 0, 1):                    # трон на юге Тронного зала
        m.set(X(432 + dx), D, 75, 'polished_deepslate')
    m.stairs(X(432), D + 1, 75, 'polished_deepslate_stairs', 'north')
    for dx in (-1, 1):
        m.set(X(432 + dx), D + 1, 75, 'polished_deepslate_wall', up=True, north='none', south='none', east='none', west='none',
              waterlogged=False)
    b3 = s.boss('lich', 'lich', (X(432), D, 19), 19, final=True, arena2=(X(432) + 0.5, D, 62.5, 16))
    s.gate('arena3', (X(409), D, 17, X(409), D + 4, 21), lock=b3)
    s.gate('throne', (X(431), D, 42, X(433), D + 3, 42), open_when=[f'boss:{b3}'])
    s.say((X(392), D - 1, 14, X(408), C + 5, 24), 'Система: «Парадная лестница в Усыпальницу. Ниже склепа нет.»')
    s.say((X(413), D - 1, 0, X(451), D + 12, 38),
          'Система: «Король-лич Карн. Когда погаснет свет — держитесь у душевных жаровен.»')

    # разлом в нише за троном
    for dx in (-2, 2):
        for dy in range(0, 4):
            m.set(X(432 + dx), D + dy, 83, 'crying_obsidian')
    for dx in (-1, 0, 1):
        m.set(X(432 + dx), D + 3, 83, 'crying_obsidian')
        for dy in range(0, 3):
            m.set(X(432 + dx), D + dy, 83, 'litrpg:rift', active=False)
    apply_gates(m, s)

    # свет
    m.hanging_lights(spacing=6, soul=False, themes=['crypt'], min_h=6, chain=2)
    m.hanging_lights(spacing=6, soul=True, themes=['catacomb', 'tomb'], min_h=6, chain=2)
    m.hanging_lights(spacing=6, soul=False, themes=['chapel'], min_h=6, chain=3, offset=3)
    m.glow(spacing=4, level=9, height=3)

    s.box = (X(-4), 22, -4, X(456), A + 24, 92)
    s.camp = (X(8.5), A, 30.5, 180)
    s.trader = (X(3.5), A, 27.5, -90)
    s.rift = (X(431), D, 83, X(433), D + 2, 83)
    return m, s
