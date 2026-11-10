"""
Этап 5 · Шпиль Бездны (уровни 40–50). Пурпур, обсидиан, аметист, мосты и острова над пустотой.
Обновление 14: этапы 1–4 уходили вниз, а Шпиль — **растёт вверх**. Четыре яруса (ноги 30 → 52 → 74 → 96), каждый подъём
— свой приём, не как в этапах 1–4. Под всеми провалами — настоящая бездна: упавшего Система вытаскивает (дорого).

Ярус I · Подножие (30, на восток): Последний привал (в северной нише — **Колодец возврата**: купель, лестница наверх) →
  **Мост осколков** (площадки на пурпурных столбах из бездны; прямой мост рухнул — обход по северу, в мосту пролом в блок —
  прыжок; на юге ветка к тайнику через второй пролом) → Обсерватория (загадка памяти «Звёздная карта») → Зал отражений
  (Двойник, r 19, зеркальный пол) → **Сердцевина Шпиля**: круглый колодец над бездной, пандус-винт по стене на 22 вверх
  (1¾ оборота, перила со стороны колодца), посередине — Ось Шпиля; на пандусе — засады и вспышки пустоты.
Ярус II · Средний (52, на запад): Сад искажений (вспышки; божественная секретка: треснувшая стена → келья с загадкой →
  тайник) → Звёздная кладовая (Пожиратель Звёзд, запертый тайник) → Часовня времени (Хронофаг, r 18, циферблат) →
  **Парящие острова**: зал высотой 30 над бездной, четыре острова и пандусы между ними — вверх на 22; три плиты на трёх
  островах; с острова прыжки по осколкам к тайнику у стены.
Ярус III · Верхний (74, на восток): Архив Системы (полки, летопись, загадка «Кто я?»; запретная полка за треснувшей
  стеной; ниша **Колодца возврата** — прыжок на 45 блоков в купель у привала; печать ниши открывается, когда Архив
  очищен, иначе от привала залезли бы прямо сюда) → Зал созвездий (кристаллы по росту) → Колоннада ветров (порывы
  полосами) → **Подъём ветров**: лоза по стене шахты на 22 вверх, порывы бьют ярусами.
Ярус IV · Вершина (96): Преддверие пустоты → мост над бездной → **Вершина** (Ксалатар, r 20: круглая площадка — макушка
  башни, растущей из бездны; вокруг — шипы Шпиля) → мост → Сердце Бездны (второй зал) → последний разлом.

Арены — по `arenas.py` (замер приёмов): Двойник 19, Хронофаг 18, Ксалатар 20.
Помощники настилов (flat, disc, lay, rails) — из stage4.
"""
import math

from decor import brazier, column
from kit import Map, Stage, Theme, apply_gates, h3, jump_ok
from stage4 import disc, flat, lay, rails, run_cells

N = 5
OX = 1024 * N
STRICT = True                  # строгая проверка: обрывы, западни, арены — ошибки
A, B, C, D = 30, 52, 74, 96    # ярусы: уровень ног (снизу вверх)
ABYSS = 12                     # дно провалов (ниже пола этапа — бездна: Система вытаскивает)
RAIL = 'end_stone_brick_wall'


def X(x):
    return OX + x


# ---------------------------------------------------------------------- провалы, пандусы, винт
def abyss(m, cells, ytop, theme):
    """Провал до бездны под клетками (x, z): пустота без дна от ABYSS до ytop-1."""
    for (x, z) in cells:
        for y in range(ABYSS, ytop):
            m.open[(x, y, z)] = theme.name
            m.nowall.add((x, y, z))


def disc_cells(cx, cz, r):
    """Клетки круга той же меры, что у `round_room` (от угла клетки)."""
    return [(x, z) for x in range(int(cx - r) - 1, int(cx + r) + 2) for z in range(int(cz - r) - 1, int(cz + r) + 2)
            if math.hypot(x - cx, z - cz) <= r]


def ramp_up(feet, a, b, y_from, width=3, every=2):
    """Пандус вверх a → b: первая клетка на блок выше y_from (уровень ног площадки перед ним), дальше вверх на блок
    через каждые every клеток. Следующая площадка — на блок выше последней клетки (её и возвращает)."""
    y = y_from
    for i, cells in run_cells(a, b, width):
        y = y_from + 1 + i // every
        for c in cells:
            feet[c] = y
    return y + 1


class Helix:
    """Пандус-винт по стене круглого колодца: клетки кольца r_in…r_out, ноги от y0 до y0 + rise за угол sweep
    (рад, от theta0, против часовой при взгляде сверху: x = cos, z = sin). Одна клетка (x, z) лежит на нескольких
    витках, поэтому настил разбит на полвитка — у каждого свой словарь {(x, z): y}."""

    def __init__(self, cx, cz, r_in, r_out, y0, rise, theta0, sweep):
        self.cx, self.cz, self.y0, self.rise, self.theta0, self.sweep = cx, cz, y0, rise, theta0, sweep
        self.segs = {}
        self.cells = set()
        for (x, z) in disc_cells(cx, cz, r_out):
            if math.hypot(x - cx, z - cz) < r_in:
                continue
            self.cells.add((x, z))
            u = self.u(x, z)
            k = 0
            while u + 2 * math.pi * k <= sweep:
                s = u + 2 * math.pi * k
                self.segs.setdefault(int(s // math.pi), {})[(x, z)] = self.y_at(s)
                k += 1

    def u(self, x, z):
        a = math.atan2(z + 0.5 - self.cz, x + 0.5 - self.cx)
        return (a - self.theta0) % (2 * math.pi)

    def y_at(self, s):
        return self.y0 + int(round(self.rise * s / self.sweep))

    def dicts(self):
        return [self.segs[k] for k in sorted(self.segs)]

    def at(self, s, r):
        """Клетка ног на пандусе: путь s (рад от начала), радиус r."""
        a = self.theta0 + s
        x = math.floor(self.cx + r * math.cos(a))
        z = math.floor(self.cz + r * math.sin(a))
        seg = self.segs[int(s // math.pi)]
        if (x, z) not in seg:
            raise ValueError(f'винт: клетки {(x, z)} нет на полувитке {int(s // math.pi)}')
        return x, seg[(x, z)], z

    def joins(self, seg):
        """Соседние клетки полувитка seg, где пандус продолжается (на другом полувитке вровень или на блок выше/ниже):
        там перил не ставить. Остальные соседи — колодец или виток ниже/выше: туда перила."""
        out = set()
        for (x, z), y in seg.items():
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (x + dx, z + dz)
                if n in seg:
                    continue
                if any(n in o and abs(o[n] - y) <= 1 for o in self.segs.values()):
                    out.add(n)
        return out

    def box(self, s1, s2):
        """Область (x1, y1, z1, x2, y2, z2) клеток пандуса на отрезке пути s1…s2 (для ловушки)."""
        pts = []
        for (x, z) in self.cells:
            u = self.u(x, z)
            for k in range(3):
                s = u + 2 * math.pi * k
                if s1 <= s <= s2:
                    pts.append((x, self.y_at(s), z))
        xs, ys, zs = zip(*pts)
        return (min(xs), min(ys), min(zs), max(xs), max(ys), max(zs))


def island_rock(m, feet, cx, cz, r, y, seed):
    """Скала под парящим островом: перевёрнутый конус из эндерняка и пурпура, снизу — аметист."""
    depth = int(r * 1.6) + 2
    for k in range(1, depth + 1):
        rr = r * (1 - k / (depth + 1)) + 0.4
        for x in range(int(cx - rr) - 1, int(cx + rr) + 2):
            for z in range(int(cz - rr) - 1, int(cz + rr) + 2):
                if (x, z) not in feet or math.hypot(x + 0.5 - cx, z + 0.5 - cz) > rr + (h3(x, k, z, seed) - 0.5) * 0.8:
                    continue
                yy = y - 1 - k
                if not m.solid(x, yy, z):
                    m.set(x, yy, z, 'end_stone' if h3(x, yy, z, seed + 1) < 0.7 else 'purpur_block')
    m.set(math.floor(cx), y - 2 - depth, math.floor(cz), 'amethyst_cluster', facing='down', waterlogged=False)


def deck_light(m, feet, seed, share=0.12, level=11):
    """Невидимый свет над настилом построек (их столбцы — в keep, общая подсветка `glow` их обходит):
    винт и острова над бездной иначе тонут во тьме."""
    for (x, z), y in feet.items():
        if h3(x, y, z, seed) < share and (x, y + 2, z) in m.open and m.is_air(x, y + 2, z) and m.is_air(x, y + 1, z):
            m.set(x, y + 2, z, 'light', level=level, waterlogged=False)


def spike(m, x, z, top, seed):
    """Шип Шпиля: столб из бездны (обсидиан, полосы плачущего), наверху — друза и стержень."""
    for y in range(ABYSS, top):
        band = (y + int(h3(x, 0, z, seed) * 5)) % 7 == 0
        m.set(x, y, z, 'crying_obsidian' if band else 'obsidian')
    m.set(x, top, z, 'amethyst_block')
    m.set(x, top + 1, z, 'end_rod', facing='up')


def build(_n=N):
    m = Map(seed=5005)
    spire = m.theme(Theme('spire', wall=[('purpur_block', 3), ('end_stone_bricks', 3), ('purpur_pillar', 1), ('obsidian', 1)],
                          floor=[('end_stone_bricks', 3), ('purpur_block', 2)], ceil=[('purpur_block', 2), ('obsidian', 1)], seed=51))
    void = m.theme(Theme('void', wall=[('obsidian', 3), ('crying_obsidian', 1), ('purpur_block', 1)],
                         floor=[('obsidian', 3), ('polished_blackstone', 1)], ceil=[('obsidian', 2), ('crying_obsidian', 1)], seed=52))
    crystal = m.theme(Theme('crystal', wall=[('amethyst_block', 2), ('calcite', 2), ('purpur_block', 1)],
                            floor=[('calcite', 2), ('amethyst_block', 1), ('end_stone_bricks', 2)], ceil='amethyst_block', seed=53))
    archive = m.theme(Theme('archive', wall=[('purpur_block', 3), ('end_stone_bricks', 2), ('calcite', 1)],
                            floor=[('polished_deepslate', 2), ('purpur_block', 1)], ceil=[('purpur_block', 2), ('end_stone_bricks', 1)], seed=54))
    s = Stage(N, 'spire', 'Шпиль Бездны', (40, 50), 'spire')

    # ================================================================== пустоты
    # ---- Ярус I · Подножие (30), на восток
    m.room(X(0), 20, X(16), 34, A, 7, spire, vault=2)                           # Последний привал
    m.room(X(2), 9, X(12), 19, A, 6, spire)                                     # ниша Колодца возврата (купель)
    m.corridor([(X(16), 27), (X(22), 27)], 3, 4, spire, y=A)
    m.room(X(22), 6, X(70), 48, A, 12, void, vault=4)                           # Мост осколков
    abyss(m, [(x, z) for x in range(X(24), X(69)) for z in range(7, 48)], A, void)
    m.corridor([(X(70), 27), (X(77), 27)], 3, 4, crystal, y=A)
    m.round_room(X(88) + 0.5, 27.5, 12.5, A, 9, crystal, dome=4)                # Обсерватория
    m.corridor([(X(100), 27), (X(108), 27)], 3, 4, spire, y=A)
    m.round_room(X(127) + 0.5, 27.5, 19.5, A, 12, spire, dome=5)                # Зал отражений (Двойник, r 19)
    m.corridor([(X(147), 27), (X(158), 27)], 3, 4, void, y=A)
    core_c = (X(173) + 0.5, 27.5)
    m.round_room(core_c[0], core_c[1], 14.6, A, B - A + 7, void)               # Сердцевина: A … B+6
    abyss(m, disc_cells(core_c[0], core_c[1], 14.6), A, void)
    m.corridor([(X(173), 43), (X(173), 58)], 3, 4, void, y=B)

    # ---- Ярус II · Средний (52), на запад
    m.room(X(152), 58, X(192), 88, B, 9, crystal, vault=3)                      # Сад искажений
    m.corridor([(X(172), 88), (X(172), 92)], 2, 3, crystal, y=B)                # ход за треснувшей стеной
    m.room(X(166), 92, X(178), 98, B, 5, crystal)                               # келья с загадкой
    m.corridor([(X(178), 95), (X(184), 95)], 2, 3, crystal, y=B)
    m.room(X(184), 92, X(190), 98, B, 4, void)                                  # божественный тайник
    m.corridor([(X(146), 72), (X(152), 72)], 3, 4, crystal, y=B)
    m.room(X(124), 62, X(146), 82, B, 8, crystal, vault=3)                      # Звёздная кладовая
    m.corridor([(X(117), 72), (X(124), 72)], 3, 4, spire, y=B)
    m.round_room(X(98) + 0.5, 72.5, 18.5, B, 11, spire, dome=5)                 # Часовня времени (Хронофаг, r 18)
    m.corridor([(X(74), 72), (X(80), 72)], 3, 4, void, y=B)
    m.carve(X(26), B, 52, X(74), C + 8, 96, void)                               # Парящие острова (30 в высоту)
    ledge0 = {(x, z) for x in range(X(70), X(75)) for z in range(68, 81)}
    abyss(m, [(x, z) for x in range(X(26), X(75)) for z in range(52, 97) if (x, z) not in ledge0], B, void)
    m.corridor([(X(27), 51), (X(27), 43)], 3, 4, archive, y=C)

    # ---- Ярус III · Верхний (74), на восток
    m.room(X(2), 8, X(12), 18, C, 5, archive)                                   # ниша Колодца возврата (верх)
    m.corridor([(X(12), 13), (X(16), 13)], 3, 4, archive, y=C)
    m.room(X(16), 4, X(48), 42, C, 10, archive, vault=3)                        # Архив Системы
    m.room(X(49), 6, X(51), 8, C, 3, archive)                                   # запретная полка
    m.corridor([(X(48), 22), (X(54), 22)], 3, 4, archive, y=C)
    m.room(X(54), 10, X(80), 36, C, 9, crystal, vault=3)                        # Зал созвездий
    m.corridor([(X(80), 23), (X(86), 23)], 3, 4, spire, y=C)
    m.room(X(86), 12, X(126), 34, C, 8, spire, vault=2)                         # Колоннада ветров
    m.corridor([(X(126), 23), (X(130), 23)], 3, 4, spire, y=C)
    m.room(X(130), 16, X(142), 30, C, 6, crystal)                               # Подножие подъёма

    # ---- Ярус IV · Вершина (96)
    m.room(X(128), 6, X(152), 30, D, 9, void, vault=2)                          # Преддверие пустоты
    m.corridor([(X(152), 18), (X(198), 18)], 3, 5, crystal, y=D)
    top_c = (X(226) + 0.5, 18.5)
    m.round_room(top_c[0], top_c[1], 27.6, D, 18, void, dome=8)                 # Вершина (Ксалатар, r 20)
    ring = [(x, z) for (x, z) in disc_cells(top_c[0], top_c[1], 27.6) if math.hypot(x - top_c[0], z - top_c[1]) > 20.6]
    abyss(m, ring, D, void)
    m.corridor([(X(226), 47), (X(226), 52)], 3, 4, crystal, y=D)
    heart_c = (X(226) + 0.5, 66.5)
    m.round_room(heart_c[0], heart_c[1], 14.5, D, 14, crystal, dome=7)          # Сердце Бездны
    m.room(X(223), 81, X(229), 86, D, 6, void)                                  # последний разлом

    # ---- вертикаль до оболочки
    m.dropper(X(6), 12, X(8), 14, C, A, spire, depth=3, pad=2)                  # Колодец возврата: 45 блоков в купель
    m.ladder(X(6), 12, A, C - 1, 'south', support='purpur_block', width=3)      # …и лестница наверх по северной стене
    m.sleeve(X(6), 12, X(8), 14, A + 3, A + 5, 'purpur_block')                  # рукав в нише: до 3 блоков над водой
    m.vines(X(135), 16, C, D - 1, kind='vine', facing='south', to=(X(137), 16), theme=crystal)   # Подъём ветров
    # Мост осколков: площадки на столбах из бездны, мосты
    plat = dict(P1=(X(33) + 0.5, 27.5, 4), P2=(X(33) + 0.5, 13.5, 3), P3=(X(59) + 0.5, 13.5, 3), P4=(X(59) + 0.5, 27.5, 4),
                P5=(X(33) + 0.5, 41.5, 2.5))
    for (px, pz, pr) in plat.values():
        m.platform(px, pz, A, pr, 'end_stone_bricks', rim='purpur_block', support='pillar', support_block='purpur_pillar')
    bk = dict(deck='purpur_block', width=3, rail=RAIL, posts='purpur_pillar', post_every=5)
    m.bridge((X(24), 27), (X(28), 27), A, **bk)                                 # вход → P1
    m.bridge((X(33), 22), (X(33), 17), A, **bk)                                 # P1 → P2 (север)
    m.bridge((X(37), 13), (X(55), 13), A, gaps=(9,), **bk)                      # P2 → P3: пролом (основной путь — в блок)
    m.bridge((X(59), 17), (X(59), 22), A, **bk)                                 # P3 → P4
    m.bridge((X(64), 27), (X(68), 27), A, **bk)                                 # P4 → выход
    m.bridge((X(38), 27), (X(44), 27), A, **bk)                                 # прямой мост рухнул: обломки
    m.bridge((X(49), 27), (X(54), 27), A, **bk)
    m.bridge((X(33), 32), (X(33), 38), A, gaps=(3, 4), **bk)                    # P1 → P5 (тайник): пролом в два блока
    m.shell(2)
    # дальше всё отложенное (`later`) выполняется сразу — оболочка уже стоит

    # ================================================================== Последний привал и Колодец возврата
    m.set(X(8), A, 27, 'soul_campfire', facing='north', lit=True, signal_fire=False, waterlogged=False)
    for dx in (-2, 2):
        m.stairs(X(8 + dx), A, 27, 'purpur_stairs', 'east' if dx < 0 else 'west')
    for (lx, lz) in ((X(1), 21), (X(1), 33), (X(15), 21), (X(15), 33)):
        m.set(lx, A, lz, 'end_rod', facing='up')
    m.sign(X(16), A + 1, 22, 'west')
    s.sign((X(16), A + 1, 22), ['ЭТАП 5', 'Шпиль Бездны', 'уровни 40–50', ''])
    m.sign(X(2), A + 1, 19, 'east')
    s.sign((X(2), A + 1, 19), ['Колодец', 'возврата.', 'Сверху — из', 'Архива.'])
    s.say((X(0), A - 1, 9, X(16), A + 8, 34),
          'Система: «Последний этап. Шпиль растёт из бездны вверх — четыре яруса до самой вершины. '
          'Дальше Система видит хуже. Дальше — то, что её создало. Колодец в северной нише — путь назад, '
          'когда вы будете наверху: прыгайте, купель поймает.»')

    # ================================================================== Мост осколков
    s.pack('p1', [('void_sentinel', X(33), A, 27, 40), ('warped_archer', X(34), A, 29, 40), ('shard', X(32), A, 25, 40),
                  ('shard', X(35), A, 26, 40, None, 4)])
    s.pack('p2', [('wanderer', X(59), A, 13, 41), ('warped_archer', X(60), A, 14, 41), ('light_eater', X(58), A, 12, 41),
                  ('shard', X(59), A, 15, 41, None, 4)])
    p3 = s.pack('p3', [('colossus', X(59), A, 27, 41), ('shard', X(57), A, 29, 41), ('shard', X(61), A, 29, 41),
                       ('light_eater', X(60), A, 25, 41, None, 5)])
    m.set(X(33), A, 42, 'litrpg:cache', facing='north')
    s.cache('shards', (X(33), A, 42), 1)
    m.sign(X(22), A + 1, 25, 'east')
    s.sign((X(22), A + 1, 25), ['Прямой мост', 'рухнул.', 'Обход —', 'по северу.'])
    s.say((X(22), A - 1, 6, X(30), A + 14, 48),
          'Система: «Мост осколков. Прямой путь рухнул в бездну — обход по северу, и там пролом: прыгайте с разбега. '
          'Упавших Система ещё вытаскивает. Пока.»')

    # ================================================================== Обсерватория (память)
    pillars = []
    for i in range(6):
        a = i * math.pi / 3
        px, pz = math.floor(X(88) + 0.5 + 8 * math.cos(a)), math.floor(27.5 + 8 * math.sin(a))
        m.set(px, A, pz, 'calcite')
        m.set(px, A + 1, pz, 'amethyst_block')
        m.set(px, A + 2, pz, 'amethyst_cluster', facing='up', waterlogged=False)
        pillars.append((px, A + 1, pz))
    m.set(X(88), A, 27, 'end_stone_bricks')
    m.set(X(88), A + 1, 27, 'polished_blackstone_button', face='floor', facing='north', powered=False)
    mech_mem = s.mech('stars', 'memory', [(X(88), A + 1, 27)] + pillars, text='Звёздная карта', rounds=3)
    for i in range(24):                                                         # звёздный круг на полу
        a = i * math.pi / 12
        m.set(math.floor(X(88) + 0.5 + 11 * math.cos(a)), A - 1, math.floor(27.5 + 11 * math.sin(a)), 'crying_obsidian')
    p4 = s.pack('p4', [('void_sentinel', X(82), A, 24, 42), ('light_eater', X(92), A, 32, 42), ('wanderer', X(86), A, 34, 42, None, 4)])
    s.gate('g1', (X(103), A, 26, X(103), A + 3, 28), open_when=[f'mech:{mech_mem}', f'pack:{p4}', f'pack:{p3}'])
    s.say((X(76), A - 1, 15, X(100), A + 13, 40),
          'Система: «Обсерватория. Звёзды вспыхнут по очереди — повторите. Кнопка — в центре. Три раунда, и каждый длиннее.»')

    # ================================================================== Зал отражений (Двойник): зеркальный пол
    cx, cz = X(127) + 0.5, 27.5
    for (x, z) in disc_cells(cx, cz, 19.5):
        d = math.hypot(x + 0.5 - cx, z + 0.5 - cz)
        a = math.atan2(z + 0.5 - cz, x + 0.5 - cx)
        ray = abs(((a / (math.pi / 8)) + 0.5) % 1 - 0.5) < 0.09 and d > 4
        if 7.2 < d < 8.2 or 14.2 < d < 15.2:
            m.set(x, A - 1, z, 'crying_obsidian')
        elif ray:
            m.set(x, A - 1, z, 'calcite')
        elif d <= 3:
            m.set(x, A - 1, z, 'obsidian')
    for i in range(10):
        a = i * math.pi / 5
        bx, bz = math.floor(cx + 18.3 * math.cos(a)), math.floor(cz + 18.3 * math.sin(a))
        column(m, bx, bz, A, m.ceil_y(bx, A, bz) - A + 1, 'purpur_pillar', 'obsidian', 'end_rod')
    b1 = s.boss('doppel', 'doppel', (X(127), A, 27), 19)
    s.gate('arena1', (X(106), A, 26, X(106), A + 3, 28), lock=b1)
    s.gate('g2', (X(150), A, 26, X(150), A + 3, 28), open_when=[f'boss:{b1}'])
    s.say((X(107), A - 1, 7, X(148), A + 18, 48),
          'Система: «Двойник. Каждому — своя тень. Бейте свою. Когда он поднимет зеркало — опустите оружие.»')

    # ================================================================== Сердцевина Шпиля: винт над бездной
    hx = Helix(core_c[0], core_c[1], 9.5, 14.6, A, B - A, math.pi - 0.13, 3.5 * math.pi + 0.3)
    hdicts = hx.dicts()
    for seg in hdicts:
        lay(m, seg, 'purpur_block', corbel='purpur_stairs', fill_gap=0)
    for (x, z) in hx.cells:                                                     # кайма у колодца
        if math.hypot(x - core_c[0], z - core_c[1]) < 10.5:
            for seg in hdicts:
                if (x, z) in seg:
                    m.set(x, seg[(x, z)] - 1, z, 'end_stone_bricks')
    for seg in hdicts:
        rails(m, seg, RAIL, skip=hx.joins(seg))
    # Ось Шпиля: столб посередине колодца — от бездны до свода
    for (x, z) in disc_cells(core_c[0], core_c[1], 2.2):
        d = math.hypot(x - core_c[0], z - core_c[1])
        for y in range(ABYSS, B + 7):
            m.set(x, y, z, 'crying_obsidian' if y % 6 == 0 else ('amethyst_block' if d < 1 else 'obsidian'))
    for k, seg in enumerate(hdicts):
        deck_light(m, seg, 60 + k)
    for y in range(A + 2, B + 6, 5):                                            # огни на Оси
        for (dx, dz, f) in ((3, 0, 'east'), (-3, 0, 'west'), (0, 3, 'south'), (0, -3, 'north')):
            xx, zz = math.floor(core_c[0]) + dx, math.floor(core_c[1]) + dz
            bx, bz = xx - (dx > 0) + (dx < 0), zz - (dz > 0) + (dz < 0)
            if m.solid(bx, y, bz) and not m.solid(xx, y, zz):
                m.set(xx, y, zz, 'end_rod', facing=f)
    p5 = s.pack('p5', [('void_sentinel', *hx.at(1.1 * math.pi, 12), 42), ('warped_archer', *hx.at(1.25 * math.pi, 13), 42),
                       ('shard', *hx.at(1.05 * math.pi, 11), 42), ('shard', *hx.at(1.15 * math.pi, 11), 42, None, 4)])
    p6 = s.pack('p6', [('colossus', *hx.at(2.55 * math.pi, 12), 43), ('light_eater', *hx.at(2.7 * math.pi, 12), 43),
                       ('wanderer', *hx.at(2.45 * math.pi, 11), 43), ('warped_archer', *hx.at(2.8 * math.pi, 13), 43, None, 4)])
    for k, (s1, s2) in enumerate(((0.55 * math.pi, 0.8 * math.pi), (1.7 * math.pi, 1.95 * math.pi), (2.95 * math.pi, 3.2 * math.pi))):
        s.mech(f'core{k}', 'hazard', area=hx.box(s1, s2), period=3.2, warn=1.2, share=0.18, offset=k * 0.9, sound=1)
    s.gate('g3', (X(172), B, 49, X(174), B + 3, 49), open_when=[f'pack:{p5}', f'pack:{p6}'])
    m.sign(X(157), A + 1, 26, 'south')
    s.sign((X(157), A + 1, 26), ['Сердцевина.', 'Вверх — по', 'пандусу. Внизу —', 'бездна.'])
    s.say((X(158), A - 1, 12, X(189), B + 7, 43),
          'Система: «Сердцевина Шпиля. Колодец до самой бездны, а пандус идёт по стене вверх — почти два оборота. '
          'Перила со стороны пустоты есть, но порывы пустоты бьют по пандусу. Посередине — Ось: на ней всё и держится.»')

    # ================================================================== Ярус II · Сад искажений
    for i, (bx, bz) in enumerate(((X(160), 64), (X(170), 80), (X(180), 64), (X(164), 74), (X(184), 78))):
        s.mech(f'burst{i}', 'hazard', area=(bx - 2, B, bz - 2, bx + 2, B, bz + 2), period=4.0, warn=1.4, share=0.25,
               offset=i * 0.8, sound=1)
        m.set(bx, B - 1, bz, 'crying_obsidian')
    for (gx, gz) in ((X(156), 61), (X(188), 61), (X(156), 85), (X(188), 85), (X(172), 72)):
        m.set(gx, B, gz, 'amethyst_block')
        m.set(gx, B + 1, gz, 'amethyst_cluster', facing='up', waterlogged=False)
    p7 = s.pack('p7', [('colossus', X(168), B, 66, 43), ('warped_archer', X(186), B, 84, 43), ('warped_archer', X(186), B, 62, 43),
                       ('light_eater', X(176), B, 70, 43, None, 4)])
    p8 = s.pack('p8', [('shard', X(158), B, 80, 44), ('shard', X(160), B, 82, 44), ('shard', X(156), B, 82, 44),
                       ('wanderer', X(159), B, 78, 44), ('shard', X(157), B, 78, 44, None, 5)])
    # божественная секретка: треснувшая стена → келья с загадкой → тайник
    for y in range(B, B + 3):
        for x in (X(171), X(172)):
            for z in (89, 90):
                m.set(x, y, z, 'infested_deepslate')
    m.sign(X(172), B + 1, 98, 'north')
    s.sign((X(172), B + 1, 98), ['Я всегда', 'впереди, но меня', 'никогда не', 'догнать.'])
    mech_secret = s.mech('vault', 'riddle', area=(X(167), B, 93, X(177), B + 3, 97), answers=['будущее', 'завтра', 'горизонт'],
                         text='Загадка кельи|Я всегда впереди, но меня никогда не догнать. Что я?')
    s.gate('vault', (X(181), B, 94, X(181), B + 2, 95), open_when=[f'mech:{mech_secret}'])
    m.set(X(188), B, 95, 'litrpg:divine_cache', facing='west')
    s.cache('divine_spire', (X(188), B, 95), 4)
    for (bx, bz) in ((X(185), 93), (X(185), 97)):
        brazier(m, bx, B, bz, soul=True)
    s.gate('g4', (X(149), B, 71, X(149), B + 3, 73), open_when=[f'pack:{p7}', f'pack:{p8}'])
    s.say((X(152), B - 1, 56, X(192), B + 12, 88),
          'Система: «Сад искажений. Вспышки пустоты — по кругу, читайте разметку. А южная стена здесь дышит — '
          'темнее прочих. Ярус второй, до вершины ещё два.»')

    # ================================================================== Звёздная кладовая
    for z in range(64, 82, 4):
        for x in (X(126), X(144)):
            m.set(x, B, z, 'amethyst_block')
            m.set(x, B + 1, z, 'amethyst_cluster', facing='up', waterlogged=False)
    eater = s.pack('eater', [('colossus', X(135), B, 72, 46), ('shard', X(132), B, 69, 45), ('shard', X(138), B, 75, 45),
                             ('light_eater', X(135), B, 78, 45), ('warped_archer', X(135), B, 65, 45, None, 4)], title='Пожиратель Звёзд')
    m.set(X(142), B, 80, 'litrpg:locked_cache', facing='north')
    s.cache('stars', (X(142), B, 80), 2)
    s.say((X(124), B - 1, 62, X(146), B + 11, 82),
          'Система: «Звёздная кладовая. Здесь Пожиратель Звёзд копил то, что откусил от неба. Отберите.»')

    # ================================================================== Часовня времени (Хронофаг): циферблат
    cx, cz = X(98) + 0.5, 72.5
    for (x, z) in disc_cells(cx, cz, 18.5):
        d = math.hypot(x + 0.5 - cx, z + 0.5 - cz)
        if 15.6 < d < 16.4:
            m.set(x, B - 1, z, 'purpur_block')
    for (ang, ln) in ((-math.pi / 2, 12), (math.pi / 6, 8)):                    # стрелки на полу
        for t in range(1, ln + 1):
            m.set(math.floor(cx + t * math.cos(ang)), B - 1, math.floor(cz + t * math.sin(ang)), 'polished_blackstone')
    for i in range(12):
        a = i * math.pi / 6
        bx, bz = math.floor(cx + 17 * math.cos(a)), math.floor(cz + 17 * math.sin(a))
        m.set(bx, B - 1, bz, 'gold_block' if i % 3 == 0 else 'end_stone_bricks')
        m.set(bx, B, bz, 'end_rod', facing='up')
    m.set(math.floor(cx), B - 1, math.floor(cz), 'gold_block')
    b2 = s.boss('chrono', 'chronophage', (X(98), B, 72), 18)
    s.gate('g5', (X(122), B, 71, X(122), B + 3, 73), open_when=[f'pack:{eater}'])
    s.gate('arena2', (X(119), B, 71, X(119), B + 3, 73), lock=b2)
    s.gate('g6', (X(77), B, 71, X(77), B + 3, 73), open_when=[f'boss:{b2}'])
    s.say((X(80), B - 1, 54, X(117), B + 16, 91),
          'Система: «Хронофаг. Часы — разбить. После отката — сразу с метки. Циферблат большой: '
          'до песочных часов бегите, а не идите.»')

    # ================================================================== Парящие острова: вверх на 22 над бездной
    isl = {}
    y1 = ramp_up(isl, (X(69), 74), (X(61), 74), B)                              # R1 на запад
    disc(isl, X(56) + 0.5, 74.5, 4.5, y1)                                       # I1
    y2 = ramp_up(isl, (X(56), 69), (X(56), 62), y1)                             # R2 на север
    disc(isl, X(56) + 0.5, 58.5, 3.5, y2)                                       # I2
    y3 = ramp_up(isl, (X(52), 58), (X(40), 58), y2)                             # R3 на запад
    disc(isl, X(36) + 0.5, 58.5, 3.5, y3)                                       # I3
    y4 = ramp_up(isl, (X(32), 58), (X(29), 58), y3)                             # R4 на запад — к выходу
    assert y4 == C, y4
    flat(isl, X(26), 52, X(28), 62, C)                                          # балкон выхода у западной стены
    ys = ramp_up(isl, (X(56), 79), (X(56), 85), y1)                             # к боковому острову
    disc(isl, X(56) + 0.5, 89.5, 3.5, ys)                                       # S1
    lay(m, isl, 'end_stone_bricks', rim='purpur_block', corbel='purpur_stairs', fill_gap=0)
    # прыжки по осколкам с I2 к тайнику у восточной стены (туда и обратно)
    edge = (X(59), y2, 58)
    pads = [(X(61), y2, 58), (X(63), y2 + 1, 58), (X(66), y2 + 1, 58), (X(68), y2 + 2, 58)]
    nook = {}
    flat(nook, X(70), 56, X(73), 60, y2 + 2)
    for a_, b_ in zip([edge] + pads, pads + [(X(70), y2 + 2, 58)]):
        assert jump_ok(a_, b_) and jump_ok(b_, a_), (a_, b_)
    m.parkour(pads, 'amethyst_block')
    lay(m, nook, 'end_stone_bricks', rim='purpur_block', corbel='purpur_stairs', fill_gap=0)
    rails(m, isl, RAIL, skip=[(X(60), 58)])
    rails(m, nook, RAIL, skip=[(X(69), 58)])
    for (icx, icz, ir, iy, sd) in ((X(56) + 0.5, 74.5, 4.5, y1, 1), (X(56) + 0.5, 58.5, 3.5, y2, 2),
                                   (X(36) + 0.5, 58.5, 3.5, y3, 3), (X(56) + 0.5, 89.5, 3.5, ys, 4)):
        island_rock(m, isl, icx, icz, ir, iy, sd)
    island_rock(m, nook, X(71) + 0.5, 58.5, 2.5, y2 + 2, 5)
    deck_light(m, isl, 70, share=0.1)
    deck_light(m, nook, 71, share=0.3)
    m.railing([(x, B, z) for x in range(X(70), X(75)) for z in (67, 81)] +
              [(X(69), B, z) for z in range(67, 82) if not 73 <= z <= 75], RAIL)
    m.set(X(72), y2 + 2, 58, 'litrpg:cache', facing='west')
    s.cache('islands', (X(72), y2 + 2, 58), 2)
    plates = [(X(56), y2, 58), (X(36), y3, 58), (X(56), ys, 89)]
    for (x, y, z) in plates:
        m.set(x, y, z, 'polished_blackstone_pressure_plate', powered=False)
    mech_plates = s.mech('islands', 'plates', plates, text='Три острова', hold=3)
    p9 = s.pack('p9', [('void_sentinel', X(55), y1, 73, 44), ('warped_archer', X(58), y1, 76, 44), ('shard', X(54), y1, 76, 44),
                       ('shard', X(57), y1, 72, 44, None, 4)])
    p10 = s.pack('p10', [('colossus', X(35), y3, 58, 45), ('light_eater', X(37), y3, 60, 45), ('warped_archer', X(37), y3, 56, 45),
                         ('wanderer', X(34), y3, 59, 45, None, 4)])
    s.mech('isl0', 'hazard', area=(X(61), B + 1, 73, X(66), y1 - 1, 75), period=3.4, warn=1.2, share=0.18, sound=1)
    s.mech('isl1', 'hazard', area=(X(41), y2 + 3, 57, X(47), y3 - 1, 59), period=3.4, warn=1.2, share=0.18, offset=1.2, sound=1)
    s.gate('g7', (X(26), C, 47, X(28), C + 3, 47), open_when=[f'mech:{mech_plates}', f'pack:{p9}', f'pack:{p10}'])
    m.sign(X(74), B + 1, 69, 'west')
    s.sign((X(74), B + 1, 69), ['Парящие', 'острова.', 'Три плиты —', 'на трёх островах.'])
    s.say((X(26), B - 1, 52, X(74), C + 8, 96),
          'Система: «Парящие острова. Шпиль крошится, и куски его висят над бездной. Наверх — по пандусам, '
          'с острова на остров. Три плиты на трёх островах — встаньте разом, иначе печать наверху не откроется. '
          'И осколки у восточной стены висят подозрительно ровно.»')

    # ================================================================== Ярус III · Колодец возврата (верх) и Архив
    m.sign(X(10), C + 1, 8, 'south')
    s.sign((X(10), C + 1, 8), ['Колодец', 'возврата.', 'Прыгайте —', 'купель поймает.'])
    p11 = s.pack('p11', [('void_sentinel', X(32), C, 25, 46), ('wanderer', X(24), C, 20, 46), ('light_eater', X(40), C, 25, 46),
                         ('warped_archer', X(46), C, 40, 46), ('warped_archer', X(18), C, 6, 46, None, 4)])
    s.gate('well_seal', (X(14), C, 12, X(14), C + 3, 14), open_when=[f'pack:{p11}'])
    shelves = []
    for z in (7, 10, 13, 16, 29, 32, 35, 38):
        for x in range(X(20), X(45)):
            if X(31) <= x <= X(33):
                continue                                                        # средний проход
            shelves.append((x, z))
            m.set(x, C, z, 'bookshelf')
            m.set(x, C + 1, z, 'bookshelf')
            if h3(x, C, z, 54) < 0.25:
                m.set(x, C + 2, z, 'candle', candles=1 + int(h3(x, 1, z, 5) * 4) % 4, lit=True, waterlogged=False)
    m.keep.update(shelves)
    m.set(X(32), C, 22, 'lectern', facing='south', has_book=False, powered=False)
    mech_riddle = s.mech('archive', 'riddle', area=(X(29), C, 19, X(35), C + 3, 25), answers=['система'],
                         text='Загадка Архива|Я считаю ваши шаги, раны и победы. Знаю ваш уровень, но не знаю имени. Кто я?')
    lore = [((X(20), C + 1, 17), 'south', ['Летопись I.', 'Сначала была', 'Бездна. Потом —', 'счёт.']),
            ((X(44), C + 1, 17), 'south', ['Летопись II.', 'Счёт стал', 'Системой. Она', 'дала имена.']),
            ((X(20), C + 1, 28), 'north', ['Летопись III.', 'Бездна голодна.', 'Система кормит', 'её героями.']),
            ((X(44), C + 1, 28), 'north', ['Летопись IV.', 'Кто дойдёт до', 'вершины — тот', 'решит, чья вина.'])]
    for (at, face, lines) in lore:
        m.sign(*at, face)
        s.sign(at, lines)
    # запретная полка: треснувшая стена в северо-восточном углу
    for y in range(C, C + 3):
        for z in (6, 7, 8):
            m.set(X(49), y, z, 'infested_deepslate')
    m.set(X(51), C, 7, 'litrpg:cache', facing='west')
    s.cache('archive', (X(51), C, 7), 2)
    s.gate('g8', (X(51), C, 21, X(51), C + 3, 23), open_when=[f'mech:{mech_riddle}', f'pack:{p11}'])
    s.say((X(16), C - 1, 4, X(48), C + 13, 42),
          'Система: «Архив Системы. Здесь записано всё — и про вас тоже. Загадка у аналоя — обо мне, '
          'догадаетесь. Летопись на полках — читайте, пока можно. В западной нише — Колодец возврата к привалу: '
          'печать снимется, когда в Архиве станет тихо. А в одном углу полки стоят прямо на стене, которой нет.»')

    # ================================================================== Зал созвездий (кристаллы по росту)
    buds = [('small_amethyst_bud', X(60), 16), ('large_amethyst_bud', X(74), 16),
            ('amethyst_cluster', X(60), 30), ('medium_amethyst_bud', X(74), 30)]
    btn = {}
    for (bud, bx, bz) in buds:
        m.set(bx, C, bz, 'calcite')
        m.set(bx, C + 1, bz, bud, facing='up', waterlogged=False)
        nz = bz + (1 if bz < 23 else -1)
        m.set(bx, C, nz, 'end_stone_bricks')
        m.set(bx, C + 1, nz, 'polished_blackstone_button', face='floor', facing='north', powered=False)
        btn[bud] = (bx, C + 1, nz)
    order = [btn[b] for b in ('small_amethyst_bud', 'medium_amethyst_bud', 'large_amethyst_bud', 'amethyst_cluster')]
    mech_seq = s.mech('crystals', 'sequence', order, text='Созвездие кристаллов')
    for k in range(40):                                                         # звёзды на полу
        x, z = X(55 + int(h3(k, 1, 0, 9) * 25)), 11 + int(h3(k, 2, 0, 9) * 25)
        if m.solid(x, C - 1, z) and not m.solid(x, C, z):
            m.set(x, C - 1, z, 'crying_obsidian' if k % 3 else 'amethyst_block')
    m.sign(X(54), C + 1, 19, 'east')
    s.sign((X(54), C + 1, 19), ['От ростка', 'к друзе:', 'жми у кристаллов', 'по росту.'])
    p12 = s.pack('p12', [('void_sentinel', X(67), C, 23, 46), ('shard', X(64), C, 20, 46), ('shard', X(70), C, 26, 46),
                         ('light_eater', X(67), C, 33, 46), ('warped_archer', X(77), C, 13, 46, None, 4)])
    s.gate('g9', (X(83), C, 22, X(83), C + 3, 24), open_when=[f'mech:{mech_seq}', f'pack:{p12}'])
    s.say((X(54), C - 1, 10, X(80), C + 12, 36),
          'Система: «Зал созвездий. Кристаллы растут, как звёзды: сначала росток, потом — друза. Жмите по порядку.»')

    # ================================================================== Колоннада ветров
    for x in range(X(90), X(124), 6):
        for z in (17, 29):
            column(m, x, z, C, m.ceil_y(x, C, z) - C + 1, 'purpur_pillar', 'end_stone_bricks', 'end_rod')
    for k, x0 in enumerate((X(95), X(105), X(115))):
        s.mech(f'gust{k}', 'hazard', area=(x0, C, 13, x0 + 2, C, 33), period=3.0, warn=1.1, share=0.2, offset=k * 1.0, sound=1)
        for z in range(13, 34, 4):
            m.set(x0 + 1, C - 1, z, 'crying_obsidian')
    p13 = s.pack('p13', [('colossus', X(100), C, 23, 47), ('warped_archer', X(98), C, 14, 47), ('warped_archer', X(98), C, 32, 47),
                         ('shard', X(101), C, 20, 47, None, 4)])
    p14 = s.pack('p14', [('void_sentinel', X(119), C, 23, 47), ('light_eater', X(122), C, 26, 47), ('wanderer', X(117), C, 19, 47),
                         ('wanderer', X(117), C, 27, 47), ('shard', X(121), C, 20, 47, None, 5)])
    s.gate('g10', (X(128), C, 22, X(128), C + 3, 24), open_when=[f'pack:{p13}', f'pack:{p14}'])
    s.say((X(86), C - 1, 12, X(126), C + 10, 34),
          'Система: «Колоннада ветров. Пустота дует полосами поперёк зала — по очереди. Идите в такт, '
          'пережидайте за колонной.»')

    # ================================================================== Подъём ветров: лоза на 22 вверх
    s.climb_trap('winds', (X(135), C + 1, 16, X(137), D - 2, 16), period=3.0, warn=1.0, share=0.15, layer=2, sound=1)
    for (bx, bz) in ((X(131), 29), (X(141), 29)):
        brazier(m, bx, C, bz, soul=True)
    m.sign(X(139), C + 1, 16, 'south')
    s.sign((X(139), C + 1, 16), ['Подъём ветров.', 'Лоза держит.', 'Порывы бьют', 'ярусами.'])
    s.say((X(130), C - 1, 16, X(142), C + 6, 30),
          'Система: «Подъём ветров. Лоза по северной стене — до самой вершины, двадцать два блока. '
          'Порывы бьют ярусами по очереди: лезьте в такт.»')

    # ================================================================== Ярус IV · Преддверие пустоты
    m.railing([(x, D, 15) for x in range(X(134), X(139))] + [(x, D, z) for x in (X(134), X(138)) for z in (16, 17)], RAIL)
    p15 = s.pack('p15', [('void_sentinel', X(144), D, 14, 48), ('void_sentinel', X(144), D, 22, 48), ('light_eater', X(148), D, 18, 48),
                         ('wanderer', X(140), D, 24, 48), ('warped_archer', X(149), D, 9, 48, None, 4),
                         ('colossus', X(140), D, 10, 48, None, 5)])
    for (bx, bz) in ((X(129), 7), (X(151), 7), (X(129), 29), (X(151), 29)):
        brazier(m, bx, D, bz, soul=True)
    s.gate('g11', (X(156), D, 17, X(156), D + 4, 19), open_when=[f'pack:{p15}'])
    s.say((X(128), D - 1, 6, X(152), D + 11, 30),
          'Система: «Преддверие пустоты. Вершина. Дальше Система не видит — дальше только Голод. Последний шанс '
          'выпить зелья и переглянуться.»')

    # ================================================================== Вершина (Ксалатар) и Сердце Бездны
    cx, cz = top_c
    m.bridge((X(199), 18), (X(205), 18), D, 'obsidian', width=3, rail=RAIL, posts='crying_obsidian', post_every=6)
    m.bridge((X(226), 40), (X(226), 46), D, 'obsidian', width=3, rail=RAIL, posts='crying_obsidian', post_every=6)
    for (x, z) in disc_cells(cx, cz, 20.6):                                     # пол-печать
        d = math.hypot(x + 0.5 - cx, z + 0.5 - cz)
        a = math.atan2(z + 0.5 - cz, x + 0.5 - cx)
        if not m.solid(x, D - 1, z):
            continue
        if 9.4 < d < 10.4 or 17.6 < d < 18.6:
            m.set(x, D - 1, z, 'crying_obsidian')
        elif d > 19.6:
            m.set(x, D - 1, z, 'purpur_block')
        elif abs(((a / (math.pi / 6)) + 0.5) % 1 - 0.5) < 0.07 and 10.4 < d < 17.6:
            m.set(x, D - 1, z, 'amethyst_block')
        elif d < 2.5:
            m.set(x, D - 1, z, 'obsidian')
    for i in range(14):                                                         # шипы Шпиля вокруг площадки
        a = i * 2 * math.pi / 14 + 0.12
        if any(abs((a - b + math.pi) % (2 * math.pi) - math.pi) < 0.3 for b in (math.pi, math.pi / 2)):
            continue                                                            # не на мостах
        sx, sz = math.floor(cx + 24.2 * math.cos(a)), math.floor(cz + 24.2 * math.sin(a))
        spike(m, sx, sz, D + 3 + int(h3(i, 0, 0, 7) * 9), i)
    b3 = s.boss('xalathar', 'xalathar', (X(226), D, 18), 20, final=True, arena2=(heart_c[0], D, heart_c[1], 13))
    s.gate('arena3', (X(195), D, 17, X(195), D + 4, 19), lock=b3)
    s.gate('heart', (X(225), D, 49, X(227), D + 3, 49), open_when=[f'boss:{b3}'])
    s.say((X(199), D - 1, -9, X(254), D + 20, 46),
          'Система: «Ксалатар, Голод Бездны. Самого раненого он съест — держите всех полными. Площадка — макушка '
          'Шпиля, вокруг — пустота. Край рушится — к центру.»')
    for i in range(8):
        a = i * math.pi / 4 + 0.2
        bx, bz = math.floor(heart_c[0] + 11 * math.cos(a)), math.floor(heart_c[1] + 11 * math.sin(a))
        m.set(bx, D, bz, 'amethyst_block')
        m.set(bx, D + 1, bz, 'amethyst_cluster', facing='up', waterlogged=False)
    brazier(m, X(223), D, 54, soul=True)
    brazier(m, X(229), D, 54, soul=True)
    s.say((X(211), D - 1, 51, X(241), D + 15, 86),
          'Система: «Сердце Бездны. Отсюда всё началось. Здесь и кончится — так или иначе.»')
    for dx in (-2, 2):
        for dy in range(0, 4):
            m.set(X(226 + dx), D + dy, 85, 'crying_obsidian')
    for dx in (-1, 0, 1):
        m.set(X(226 + dx), D + 3, 85, 'crying_obsidian')
        for dy in range(0, 3):
            m.set(X(226 + dx), D + dy, 85, 'litrpg:rift', active=False)
    apply_gates(m, s)

    m.hanging_lights(spacing=7, soul=True, themes=['spire', 'crystal', 'archive'], min_h=6, chain=2)
    m.glow(spacing=4, level=9, height=3)

    s.box = (X(-6), 16, -12, X(260), D + 30, 102)
    s.camp = (X(8.5), A, 30.5, -90)
    s.trader = (X(5.5), A, 24.5, -90)
    s.rift = (X(225), D, 85, X(227), D + 2, 85)
    return m, s
