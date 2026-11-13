"""
Лобби «Цитадель Искателей» (обновление 15, заказ владельца 13.11.2026) — заново, вместо мрачного Преддверия.

Светлая цитадель над бездной (кальцит, диорит, кварц, золото — в противовес подземелью), где отряд собирается,
выбирает сложность, ждёт друзей, соревнуется и тренируется. Игрок появляется в **Зале прибытия** (юг) и выходит на:
  • **Площадь сбора** (центр, круг r 18 под куполом): Печать на возвышении — ПКМ открывает окно сбора (сложность,
    готовность, старт); золотой «Круг сбора», костры со скамьями, кристалл Системы над Печатью, живая доска
    «Сейчас в подземелье»;
  • **Зал славы** (север): доски рекордов — быстрейший забег по сложностям, рекорды «за забег», лучшее время с каждым
    хранителем, герои Цитадели, летопись последних забегов, паркур;
  • **Зал трофеев** (запад): 15 пьедесталов в пять рядов по этапам (пол ряда — в материалах этапа); после первой победы
    над хранителем на пьедестале встаёт его чучело и табличка; на западной стене — доска титулов;
  • **Полигон** (восток): круглая арена Испытателя Системы (учебный бой: урона нет, считаются увороты) и Двор манекенов;
  • **Башня ветров** (северо-восток, вход из Зала славы): паркур над бассейном — 5 сохранений, финиш над серединой;
    сорвался — в воду (игра вернёт на последнее сохранение).
Схема для игры: `boards` (доски — text_display), `trophies` (пьедесталы), `parkour` (старт, сохранения, финиш),
`polygon` (середина и радиус арены), `dummies`, `signs`.

Проверка строгая (`check.py`): от места появления пешком — Печать, каждая табличка, манекен, пьедестал, доска, полигон,
старт и финиш паркура; обрывы без перил и западни — ошибки (падения паркура — в воду).
"""
import math

from kit import Map, Theme, h3

FY = 64                       # уровень ног главного этажа
PCX, PCZ = 75, 66             # середина Площади сбора
PR = 18                       # её радиус
BOX = (0, 52, 0, 146, 104, 110)
STRICT = True

# Этапы (ряды Зала трофеев): номер, название, уровни, хранители по порядку, пол ряда, кайма, свет
STAGES = [
    (1, 'Склеп Забытых', '1–10', ['gatekeeper', 'morra', 'lich'], 'polished_deepslate', 'bone_block', 'soul'),
    (2, 'Паучья чаща', '10–20', ['arachna', 'rot_ent', 'morena'], 'moss_block', 'mossy_stone_bricks', 'soul'),
    (3, 'Затопленный храм', '20–30', ['tide_warden', 'leviathan', 'naiad'], 'prismarine_bricks', 'dark_prismarine', 'plain'),
    (4, 'Кузни Пепла', '30–40', ['smelter', 'chariot', 'ash_lord'], 'polished_blackstone_bricks', 'gilded_blackstone', 'plain'),
    (5, 'Шпиль Бездны', '40–50', ['doppel', 'chronophage', 'xalathar'], 'purpur_block', 'amethyst_block', 'soul'),
]
# Размер чучела (Attributes.SCALE): крупных хранителей уменьшаем, чтобы встали на пьедестал.
STATUE_SCALE = {'leviathan': 0.45, 'chariot': 0.5, 'arachna': 0.55, 'rot_ent': 0.5, 'smelter': 0.55, 'xalathar': 0.5,
                'ash_lord': 0.6, 'gatekeeper': 0.6, 'chronophage': 0.65}

# Паркур Башни ветров: площадки-сохранения 3×3 (середина, уровень ног) и стороны (клетки ног по порядку).
CORNERS = [((108, 30), FY + 1), ((108, 18), FY + 5), ((120, 18), FY + 9), ((120, 30), FY + 13)]
FINISH = ((114, 24), FY + 20)
SIDES = [
    [(105, FY, 30), (107, FY + 1, 30)],                                                        # с дорожки — на старт
    [(108, FY + 1, 29), (108, FY + 2, 26), (108, FY + 3, 23), (108, FY + 4, 20), (108, FY + 5, 19)],
    [(109, FY + 5, 18), (112, FY + 6, 18), (115, FY + 7, 18), (118, FY + 8, 18), (119, FY + 9, 18)],
    [(120, FY + 9, 19), (120, FY + 10, 22), (120, FY + 11, 25), (120, FY + 12, 28), (120, FY + 13, 29)],
    [(119, FY + 13, 30), (116, FY + 14, 30), (113, FY + 15, 30), (111, FY + 16, 30)],
    [(111, FY + 16, 30), (111, FY + 16, 27), (111, FY + 16, 23), (111, FY + 17, 21),               # внутренний круг —
     (114, FY + 17, 21), (117, FY + 18, 21), (117, FY + 18, 25), (117, FY + 19, 27),               # прыжки длиннее
     (114, FY + 19, 27), (114, FY + 20, 25)],
]
CP_INNER = (111, FY + 17, 21)       # пятое сохранение — на внутреннем круге


def build():
    m = Map(seed=15)
    cit = m.theme(Theme('citadel', wall=[('calcite', 5), ('polished_diorite', 3), ('smooth_quartz', 1)],
                        floor=[('polished_diorite', 4), ('calcite', 2), ('smooth_stone', 1)], ceil='calcite', seed=3))
    fame = m.theme(Theme('fame', wall=[('polished_andesite', 4), ('stone_bricks', 2), ('chiseled_stone_bricks', 1)],
                         floor=[('polished_andesite', 3), ('smooth_stone', 1)], ceil='smooth_quartz', seed=5))
    hall = m.theme(Theme('trophy', wall=[('deepslate_tiles', 4), ('polished_deepslate', 2), ('deepslate_bricks', 1)],
                         floor=[('polished_deepslate', 3), ('deepslate_tiles', 1)], ceil='deepslate_tiles', seed=7))
    arena = m.theme(Theme('arena', wall=[('tuff_bricks', 5), ('polished_tuff', 2), ('chiseled_tuff_bricks', 1)],
                          floor=[('polished_tuff', 3), ('tuff_bricks', 1)], ceil='tuff_bricks', seed=9))
    tower = m.theme(Theme('tower', wall=[('prismarine_bricks', 3), ('white_terracotta', 2), ('cyan_terracotta', 1)],
                          floor='prismarine_bricks', ceil='white_terracotta', seed=11))
    signs, boards, trophies, dummies = [], [], [], []

    def sign(x, y, z, facing, lines):
        m.sign(x, y, z, facing)
        signs.append({'at': [x, y, z], 'lines': lines})

    def board(bid, x, y, z, yaw, scale=1.0, width=220):
        boards.append({'id': bid, 'at': [x, y, z], 'yaw': yaw, 'scale': scale, 'width': width})

    def deco(x, y, z, block, **props):
        """Блок украшения в стене — только не в пустоте (проходы не перекрыть)."""
        if (x, y, z) not in m.open:
            m.set(x, y, z, block, **props)

    # ================================================================== пустоты
    m.round_room(PCX, PCZ, PR, FY, 14, cit, dome=9)                              # Площадь сбора
    m.corridor([(PCX, PCZ + PR - 1), (PCX, 92)], 5, 6, cit, y=FY)              # к Залу прибытия
    m.room(67, 92, 83, 104, FY, 7, cit, vault=3)                                 # Зал прибытия
    m.corridor([(PCX, 40), (PCX, PCZ - PR + 1)], 5, 7, fame, y=FY)              # к Залу славы
    m.room(55, 18, 95, 40, FY, 11, fame, vault=4)                                # Зал славы
    m.corridor([(44, PCZ), (PCX - PR + 1, PCZ)], 5, 7, hall, y=FY)              # к Залу трофеев
    m.room(6, 48, 44, 84, FY, 11, hall, vault=3)                                 # Зал трофеев
    m.corridor([(PCX + PR - 1, PCZ), (110, PCZ)], 5, 6, arena, y=FY)            # к Полигону
    m.round_room(124, PCZ, 15, FY, 11, arena, dome=5)                            # Полигон
    m.corridor([(124, PCZ + 14), (124, 87)], 3, 4, arena, y=FY)                 # к Двору манекенов
    m.room(112, 87, 136, 100, FY, 7, arena, vault=2)                             # Двор манекенов
    m.corridor([(95, 26), (104, 26)], 3, 4, tower, y=FY)                        # к Башне ветров
    m.room(104, 14, 124, 34, FY, 30, tower)                                      # Башня ветров
    m.pool(106, 16, 122, 32, FY - 1, 3, tower)                                   # бассейн под паркуром

    # ================================================================== паркур (проверка прыжков — kit.parkour)
    platform = {}
    for (cx, cz), y in CORNERS + [FINISH]:
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                platform[(cx + dx, cz + dz)] = y
    for (x, z), y in platform.items():
        for dy in range(3):
            m.open.setdefault((x, y + dy, z), 'tower')
    for seg in SIDES:
        m.parkour(seg, 'white_concrete', theme=tower)
    m.reserve(105, 15, 123, 33, pad=0)

    m.shell(2)

    # ================================================================== Площадь сбора
    for x in range(PCX - PR, PCX + PR + 1):
        for z in range(PCZ - PR, PCZ + PR + 1):
            d = math.hypot(x - PCX, z - PCZ)
            if d > PR or (x, FY, z) not in m.open:
                continue
            if 6.5 <= d <= 7.5:
                blk = 'gold_block' if (x + z) % 3 else 'yellow_glazed_terracotta'      # Круг сбора
            elif d < 4.5:
                blk = 'chiseled_quartz_block'
            elif int(d) % 4 == 0:
                blk = 'smooth_quartz'
            else:
                blk = 'polished_diorite' if h3(x, 1, z, 4) < 0.7 else 'calcite'
            m.set(x, FY - 1, z, blk)
    # возвышение Печати: круг на блок выше, ступени-плиты по краю
    for x in range(PCX - 4, PCX + 5):
        for z in range(PCZ - 4, PCZ + 5):
            d = math.hypot(x - PCX, z - PCZ)
            if d <= 2.5:
                m.set(x, FY, z, 'chiseled_quartz_block')
            elif d <= 3.6:
                m.slab(x, FY, z, 'smooth_quartz_slab')
    m.set(PCX, FY + 1, PCZ, 'litrpg:seal')
    # кристалл Системы над Печатью: аметист и стержни света на цепи из-под купола
    top = m.ceil_y(PCX, FY + 9, PCZ)
    for y in range(FY + 9, top + 1):
        m.set(PCX, y, PCZ, 'chain', axis='y')
    for dy, r in ((8, 0), (7, 1), (6, 1), (5, 0)):
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if abs(dx) + abs(dz) <= r:
                    m.set(PCX + dx, FY + dy, PCZ + dz, 'amethyst_block' if (dx or dz) else 'purple_stained_glass')
    for dx, dz in ((2, 0), (-2, 0), (0, 2), (0, -2)):
        m.set(PCX + dx, FY + 6, PCZ + dz, 'end_rod', facing='down')
    board('now', PCX + 0.5, FY + 3.4, PCZ + 0.5, 0, 1.0, 240)              # «Сейчас в подземелье» (лицом к игроку)
    # колонны по краю площади (не в проходах)
    for i in range(12):
        a = 2 * math.pi * i / 12 + math.pi / 12
        x, z = round(PCX + (PR - 1.2) * math.cos(a)), round(PCZ + (PR - 1.2) * math.sin(a))
        if (x, FY, z) not in m.open or abs(x - PCX) <= 4 or abs(z - PCZ) <= 4:
            continue
        for y in range(FY, FY + 10):
            m.set(x, y, z, 'quartz_pillar', axis='y')
        m.set(x, FY + 10, z, 'chiseled_quartz_block')
        if (x, FY + 11, z) in m.open:
            m.lantern(x, FY + 11, z)
    # костры со скамьями — ждать друзей
    for sx, sz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        fx, fz = PCX + sx * 10, PCZ + sz * 10
        m.set(fx, FY, fz, 'campfire', facing='north', lit=True, signal_fire=False, waterlogged=False)
        for dx, dz, f in ((-2, 0, 'east'), (2, 0, 'west'), (0, -2, 'south'), (0, 2, 'north')):
            m.stairs(fx + dx, FY, fz + dz, 'birch_stairs', f)
        m.set(fx + sx * 2, FY, fz + sz * 2, 'potted_azalea_bush')
    # таблички у проходов и у Печати
    sign(PCX - 3, FY + 1, PCZ - PR + 2, 'south', ['ЗАЛ СЛАВЫ', 'рекорды и', 'летопись. Там же', '— Башня ветров'])
    sign(PCX - PR + 2, FY + 1, PCZ - 3, 'east', ['ЗАЛ ТРОФЕЕВ', 'чучела', 'побеждённых', 'хранителей'])
    sign(PCX + PR - 2, FY + 1, PCZ - 3, 'west', ['ПОЛИГОН', 'учитесь', 'уворачиваться', 'от разметки'])
    sign(PCX, FY + 1, PCZ + 2, 'south', ['ПЕЧАТЬ', 'ПКМ — окно', 'сбора отряда', '(или клавиша P)'])

    # ================================================================== Зал прибытия
    for x in range(68, 83):
        for z in range(93, 104):
            r2 = (x - PCX) ** 2 + (z - 99) ** 2
            if r2 <= 5:
                m.set(x, FY - 1, z, 'gold_block' if r2 >= 4 else 'chiseled_quartz_block')
    for z in range(84, 97):
        if m.is_air(PCX, FY, z) and (PCX, FY, z) in m.open:
            m.set(PCX, FY, z, 'light_blue_carpet')
    arrival = [
        (67, 95, 'east', ['ЦИТАДЕЛЬ', 'ИСКАТЕЛЕЙ', 'Отсюда отряды', 'уходят вниз']),
        (67, 98, 'east', ['1. СОБЕРИТЕСЬ', 'на Площади:', 'ПКМ по Печати', 'или клавиша P']),
        (67, 101, 'east', ['2. СЛОЖНОСТЬ', 'Обычная или', 'Героическая —', 'в окне сбора']),
        (83, 95, 'west', ['3. ГОТОВЫ?', 'Все в лобби', 'жмут «Готов» —', 'старт через 5 с']),
        (83, 98, 'west', ['ЗАБЕГ С НУЛЯ', 'Каждый забег —', 'с 1 уровня.', 'Рекорды вечны']),
        (83, 101, 'west', ['РОЛЬ', 'выбирают уже', 'в подземелье,', 'в Зале ролей']),
    ]
    for x, z, f, lines in arrival:
        sign(x, FY + 1, z, f, lines)

    # ================================================================== Зал славы
    for x in range(56, 95):
        for z in range(19, 40):
            if (x, FY, z) in m.open and (x - PCX) % 8 == 0:
                m.set(x, FY - 1, z, 'smooth_quartz')
    for z in range(19, PCZ - PR + 1):
        if (PCX, FY, z) in m.open and m.is_air(PCX, FY, z):
            m.set(PCX, FY, z, 'red_carpet')
    for x in (59, 67, 83, 91):                                                   # колонны с золотыми капителями
        for z in (24, 34):
            for y in range(FY, FY + 10):
                m.set(x, y, z, 'gold_block' if y == FY + 9 else 'polished_andesite')
    board('best_normal', 63.5, FY + 4.5, 18.9, 0, 0.9, 200)
    board('best_heroic', 71.5, FY + 4.5, 18.9, 0, 0.9, 200)
    board('single_normal', 79.5, FY + 4.5, 18.9, 0, 0.9, 200)
    board('single_heroic', 87.5, FY + 4.5, 18.9, 0, 0.9, 200)
    board('bosses_normal', 55.1, FY + 4.0, 26.5, -90, 0.75, 230)
    board('bosses_heroic', 55.1, FY + 4.0, 34.5, -90, 0.75, 230)
    board('heroes', 94.9, FY + 4.0, 35.0, 90, 0.85, 220)
    board('history', 64.5, FY + 4.2, 39.9, 180, 0.8, 240)
    board('parkour', 86.5, FY + 4.2, 39.9, 180, 0.9, 200)
    sign(94, FY + 1, 29, 'west', ['БАШНЯ ВЕТРОВ', 'паркур над', 'бассейном.', 'Рекорд — титул'])
    sign(56, FY + 1, 30, 'east', ['ЗАЛ СЛАВЫ', 'Рекорды ставят', 'только в', 'настоящем забеге'])

    # ================================================================== Зал трофеев
    for i, (n, name, levels, bosses, floor, rim, light) in enumerate(STAGES):
        z = 52 + 7 * i
        for x in range(8, 43):
            for dz in (-2, -1, 0, 1, 2):
                if (x, FY, z + dz) in m.open:
                    m.set(x, FY - 1, z + dz, rim if abs(dz) == 2 else floor)
        sign(8, FY + 1, z, 'east', [f'ЭТАП {n}', name, f'уровни {levels}', ''])
        for j, boss in enumerate(bosses):
            x = 16 + 10 * j
            m.set(x, FY, z, 'chiseled_quartz_block')                            # пьедестал
            for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                m.slab(x + dx, FY, z + dz, 'smooth_quartz_slab')
            trophies.append({'boss': boss, 'stage': n, 'at': [x + 0.5, FY + 1, z + 0.5], 'yaw': 90,
                             'scale': STATUE_SCALE.get(boss, 0.75)})
            m.chain_lantern(x + 3, m.ceil_y(x + 3, FY + 1, z), z, 3, soul=light == 'soul')
    board('titles', 6.1, FY + 4.0, 66.5, -90, 0.8, 260)

    # ================================================================== Полигон и Двор манекенов
    for x in range(109, 140):
        for z in range(PCZ - 15, PCZ + 16):
            d = math.hypot(x - 124, z - PCZ)
            if d <= 15 and (x, FY, z) in m.open:
                circle = abs(d - 9) < 0.6 or abs(d - 13) < 0.6
                m.set(x, FY - 1, z, 'cyan_glazed_terracotta' if circle else ('polished_tuff' if h3(x, 2, z, 6) < 0.8 else 'tuff_bricks'))
    m.set(124, FY - 1, PCZ, 'lodestone')
    for i in range(8):
        a = 2 * math.pi * i / 8 + math.pi / 8
        x, z = round(124 + 14.2 * math.cos(a)), round(PCZ + 14.2 * math.sin(a))
        if (abs(z - PCZ) <= 3 and x < 124) or (abs(x - 124) <= 2 and z > PCZ) or (x, FY, z) not in m.open:
            continue
        for y in range(FY, FY + 6):
            m.set(x, y, z, 'polished_blackstone_bricks')
        m.set(x, FY + 6, z, 'sea_lantern')
    board('polygon', 109.1, FY + 3.6, PCZ - 5.0, -90, 0.8, 200)
    sign(110, FY + 1, PCZ + 3, 'east', ['ПОЛИГОН', 'Испытатель бьёт', 'понарошку:', 'уворачивайтесь'])
    sign(110, FY + 1, PCZ + 4, 'east', ['СЕРИЯ', 'Увернулся 20 раз', 'подряд — титул', '«Неуловимый»'])
    for x, angry in ((117, False), (124, True), (131, False)):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                m.set(x + dx, FY - 1, 95 + dz, 'red_terracotta' if angry else 'chiseled_tuff')
        dummies.append({'at': [x + 0.5, FY, 95.5], 'yaw': 0, 'aggressive': angry})
    for x in range(114, 136, 4):
        m.set(x, FY + 2, 88, 'target')
        m.set(x, FY, 88, 'hay_block', axis='y')
    sign(113, FY + 1, 92, 'east', ['ДВОР', 'МАНЕКЕНОВ', 'Над манекеном —', 'урон в секунду'])
    sign(113, FY + 1, 97, 'east', ['ЗЛОЙ МАНЕКЕН', 'бьёт того, кто', 'ему опаснее.', 'Танк, проверь!'])

    # ================================================================== Башня ветров (паркур)
    golden = {c for c, _ in CORNERS} | {FINISH[0]}
    for (x, z), y in platform.items():
        m.set(x, y - 1, z, 'gold_block' if (x, z) in golden else 'white_concrete')
    m.set(CP_INNER[0], CP_INNER[1] - 1, CP_INNER[2], 'gold_block')
    for z in range(15, 34):                                                     # дорожка вдоль стен — светлая
        for x in range(105, 124):
            if (x, FY, z) in m.open and not (106 <= x <= 122 and 16 <= z <= 32):
                m.set(x, FY - 1, z, 'smooth_quartz' if (x + z) % 2 else 'prismarine_bricks')
    sign(105, FY + 1, 31, 'east', ['СТАРТ', 'Шаг на золотую', 'площадку —', 'пошёл отсчёт'])
    sign(105, FY + 1, 27, 'east', ['СОХРАНЕНИЯ', 'на золотых', 'площадках. Упал —', 'вернёт туда'])
    sign(105, FY + 1, 23, 'east', ['ФИНИШ', 'над серединой.', 'Вниз — прыжком', 'в бассейн'])
    for y in range(FY + 4, FY + 28, 6):                                         # светящиеся окна в стенах башни
        for x in (103, 125):
            for z in (19, 24, 29):
                deco(x, y, z, 'sea_lantern')
        for z in (13, 35):
            for x in (109, 114, 119):
                deco(x, y, z, 'sea_lantern')
    checkpoints = [[x + 0.5, y, z + 0.5] for (x, z), y in CORNERS[1:]] + [[CP_INNER[0] + 0.5, CP_INNER[1], CP_INNER[2] + 0.5]]
    parkour = {'start': [CORNERS[0][0][0] + 0.5, CORNERS[0][1], CORNERS[0][0][1] + 0.5], 'checkpoints': checkpoints,
               'finish': [FINISH[0][0] + 0.5, FINISH[1], FINISH[0][1] + 0.5], 'area': [104, FY - 4, 14, 124, FY + 29, 34]}

    # ================================================================== свет
    m.hanging_lights(spacing=7, soul=False, themes=['citadel', 'fame'], min_h=9, chain=3, offset=3)
    m.hanging_lights(spacing=6, soul=True, themes=['trophy'], min_h=8, chain=2, offset=1)
    m.glow(spacing=4, level=15, themes=['citadel', 'fame', 'arena', 'tower'], height=3)
    m.glow(spacing=4, level=13, themes=['trophy'], height=3)
    m.glow(spacing=6, level=14, themes=['citadel'], height=10)

    lobby = {'box': list(BOX), 'spawn': [PCX + 0.5, FY, 99.5, 180], 'seal': [PCX, FY + 1, PCZ], 'signs': signs,
             'dummies': dummies, 'boards': boards, 'trophies': trophies, 'parkour': parkour,
             'polygon': {'center': [124.5, FY, PCZ + 0.5], 'radius': 14}}
    return m, lobby
