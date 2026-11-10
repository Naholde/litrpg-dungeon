"""
Этап 4 · Кузни Пепла (уровни 30–40). Базальт, чернокамень, лава, цепи, наковальни, горны.
Обновление 14: кузни уходят в гору четырьмя ярусами, и каждый ярус лежит прямо под предыдущим — путь идёт «змейкой»
(на восток, назад на запад под пройденным, снова на восток). Спуски — свои, не как в этапах 1–3.

Ярус I · Верхние кузни (ноги 84, на восток): Кузнечный двор (привал; в южной нише — устье Дымохода) → Литейный цех
  (лава в желобе; **мостки** под сводом по лестнице — арбалетчики и тайник) → Печати горнов (кнопки по цене металла) →
  Арена Плавильного голема (круглый цех r 17, шесть чанов воды по кругу) → **Плавильная шахта**: зал высотой 28 блоков,
  серпантин по стенам вниз на 18 — по северной стене, через угол, по восточной; на дне — лавовое озеро в ограде,
  над ним на цепях висит ковш. По серпантину бьют огненные струи.
Ярус II · Литейные недра (66, на запад — под ярусом I): Лавовое озеро (мост на цепях с провисом над лавовой ямой,
  огненные струи; с северного берега — прыжки по столбу на островок с тайником) → Арсенал (Мастер-кузнец Горн,
  запертый тайник; три скрытых рычага открывают божественную секретку) → Зал мехов (**Дымоход** — срезка наверх,
  прямо к привалу: багровая лоза в шахте 2×2, мехи дуют ярусами) → Ристалище Колесницы (круг r 20) → Закалочная.
Ярус III · Горнила (48, на восток): **Закалочный чан** — прыжок в дыру в полу Закалочной на 18 блоков в воду → Зал горнил
  (три острова в лаве — плиты, загадка «огонь») → **Висячие марши**: шахта 29 блоков над лавовым дном, четыре
  площадки на цепях и лестничные пролёты между ними — вниз на 18; по пролётам бьют молоты.
Ярус IV · Тронные недра (30): Преддверие → Тронный зал Владыки (r 18, восемь колонн) → Кальдера (второй зал, кольцо
  лавы) → ниша разлома.

Арены — по `arenas.py` (замер приёмов): Плавильный голем 17, Колесница 20, Владыка Пепла 18.
"""
import math

from decor import brazier, column
from kit import Map, Stage, Theme, apply_gates, h3

N = 4
OX = 1024 * N
STRICT = True                  # строгая проверка: обрывы, западни, арены — ошибки
A, B, C, D = 84, 66, 48, 30    # ярусы: уровень ног

DIRS4 = ((1, 0), (-1, 0), (0, 1), (0, -1))
WALL = 'polished_blackstone_brick_wall'


def X(x):
    return OX + x


# ---------------------------------------------------------------------- лава
def is_lava(m, c):
    return 'lava' in (m.get(*c) or '')


def lava_sheet(m, cells, under='magma_block'):
    """Лава вместо блоков пола (клетки (x, y, z) — сам блок пола; над ним пустота). Под лавой — магма.
    Течь лаве некуда проверяет `check_lava` — после того, как поверх лягут мосты и тропы."""
    cs = set(cells)
    for (x, y, z) in cs:
        if (x, y, z) in m.open:
            raise ValueError(f'лава: {x, y, z} — пустота, а не пол')
        m.set(x, y, z, 'lava', level=0)
        if (x, y - 1, z) not in cs:
            m.set(x, y - 1, z, under)
    return sorted(cs)


def check_lava(m, cells):
    """Лава стоит, только если по бокам и снизу — твёрдое или лава (иначе потечёт по залу)."""
    for (x, y, z) in cells:
        if not is_lava(m, (x, y, z)):
            continue
        for c in ((x + 1, y, z), (x - 1, y, z), (x, y, z + 1), (x, y, z - 1), (x, y - 1, z)):
            if is_lava(m, c):
                continue
            if not m.solid(*c):
                raise ValueError(f'лава в {x, y, z} утечёт в {c}')


# ---------------------------------------------------------------------- настилы, марши, перила
def run_cells(a, b, width=3):
    """Прямой пролёт a → b ((x, z), вдоль оси): [(i, [клетки поперёк])] по порядку."""
    (ax, az), (bx, bz) = a, b
    n = max(abs(bx - ax), abs(bz - az))
    sx, sz = (bx > ax) - (bx < ax), (bz > az) - (bz < az)
    px, pz = (0, 1) if sx else (1, 0)
    lo = -(width // 2)
    out = []
    for i in range(n + 1):
        x, z = ax + sx * i, az + sz * i
        out.append((i, [(x + px * k, z + pz * k) for k in range(lo, lo + width)]))
    return out


def ramp(feet, a, b, y_from, width=3, every=2):
    """Лестничный пролёт a → b: первая клетка на блок ниже y_from (уровень ног площадки перед ним), дальше вниз на блок
    через каждые every клеток. Ступени — полные блоки (вниз — шагом, вверх — шагом на блок). Уровень последней клетки."""
    y = y_from
    for i, cells in run_cells(a, b, width):
        y = y_from - 1 - i // every
        for c in cells:
            feet[c] = y
    return y


def flat(feet, x1, z1, x2, z2, y):
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for z in range(min(z1, z2), max(z1, z2) + 1):
            feet[(x, z)] = y


def disc(feet, cx, cz, r, y):
    for x in range(int(cx - r) - 1, int(cx + r) + 2):
        for z in range(int(cz - r) - 1, int(cz + r) + 2):
            if math.hypot(x + 0.5 - cx, z + 0.5 - cz) <= r:
                feet[(x, z)] = y


FACE = {(1, 0): 'west', (-1, 0): 'east', (0, 1): 'north', (0, -1): 'south'}


def lay(m, feet, deck, rim=None, fill_gap=3, corbel=None):
    """Настил: блок deck под ногами каждой клетки (y-1). Где под настилом до пола не больше fill_gap — засыпать
    (у самого пола марш становится сплошной лестницей, а не полкой с щелью). corbel — ступени вверх ногами под
    открытым краем настила (через клетку), для красоты снизу."""
    for (x, z), y in feet.items():
        edge = any((x + dx, z + dz) not in feet for dx, dz in DIRS4)
        m.set(x, y - 1, z, rim if rim and edge else deck)
        gap, hit = [], False
        for yy in range(y - 2, y - 3 - fill_gap, -1):
            if m.solid(x, yy, z):
                hit = True
                break
            gap.append(yy)
        if hit:
            for yy in gap:
                m.set(x, yy, z, deck)
        elif corbel and edge and (x + z) % 2 == 0:
            for dx, dz in DIRS4:
                if (x + dx, z + dz) not in feet and not m.solid(x + dx, y - 1, z + dz):
                    m.stairs(x, y - 2, z, corbel, FACE[(dx, dz)], half='top')
                    break
    for (x, z) in feet:
        m.keep.add((x, z))


def rails(m, feet, block=WALL, skip=(), double=True, down=3):
    """Перила вокруг настила: в каждой соседней клетке, где пустота, а под ней обрыв (нет пола или там лава), — стенка
    от нижнего до верхнего уровня соседних клеток настила (+1 у маршей: с верхней ступени на перила нижней не шагнуть).
    Низ перил спускается до пола, если до него не больше down блоков (без щели под перилами). skip — проходы.
    Возвращает клетки перил."""
    span = {}
    for (x, z), y in feet.items():
        for dx, dz in DIRS4:
            n = (x + dx, z + dz)
            if n in feet or n in skip:
                continue
            if (n[0], y, n[1]) not in m.open or m.solid(n[0], y, n[1]):
                continue
            g = y - 1
            while g > y - 40 and not m.solid(n[0], g, n[1]):
                g -= 1
            if y - (g + 1) <= 1 and not is_lava(m, (n[0], g, n[1])):
                continue                                      # там пол вровень или на ступень ниже — проход
            lo, hi = span.get(n, (y, y))
            span[n] = (min(lo, y), max(hi, y))
    cells = []
    for (x, z), (lo, hi) in span.items():
        top = hi + (1 if double else 0)
        bottom = lo
        for _ in range(down):
            if m.solid(x, bottom - 1, z) or (x, bottom - 1, z) not in m.open:
                break
            bottom -= 1
        if not m.solid(x, bottom - 1, z):
            bottom = lo                                       # пол далеко — перила держатся на настиле
        for y in range(bottom, top + 1):
            if not m.solid(x, y, z):
                m.set(x, y, z, block)
                cells.append((x, y, z))
        m.keep.add((x, z))
    m.connect(cells)
    return cells


def chains_up(m, x, y, z, limit=40):
    """Цепь от (x, y, z) вверх до потолка (если он не дальше limit)."""
    top = y - 1
    while (x, top + 1, z) in m.open and not m.solid(x, top + 1, z) and top - y < limit:
        top += 1
    if top < y or top - y >= limit or (x, top + 1, z) in m.open:
        return
    for yy in range(y, top + 1):
        m.set(x, yy, z, 'chain', axis='y', waterlogged=False)


def vats_at():
    """Шесть чанов 2×2 по кругу r 13 вокруг середины арены голема (угол чана с меньшими x, z)."""
    out = []
    for i in range(6):
        a = i * math.pi / 3
        out.append((math.floor(X(103) + 0.5 + 13 * math.cos(a) - 1), math.floor(27.5 + 13 * math.sin(a) - 1)))
    return out


def vat(m, x, z, y, theme):
    """Чан воды 2×2 в полу (вровень с полом), кайма из меди."""
    m.pool(x, z, x + 1, z + 1, y - 1, 2, theme)

    def rim():
        for dx in range(-1, 3):
            for dz in range(-1, 3):
                if dx in (-1, 2) or dz in (-1, 2):
                    m.set(x + dx, y - 1, z + dz, 'cut_copper')
    m.later(rim)


def build(_n=N):
    m = Map(seed=4004)
    forge = m.theme(Theme('forge', wall=[('polished_blackstone_bricks', 4), ('basalt', 2), ('blackstone', 2), ('cracked_polished_blackstone_bricks', 1)],
                          floor=[('polished_blackstone', 3), ('smooth_basalt', 2), ('blackstone', 1)],
                          ceil=[('basalt', 2), ('blackstone', 2)], seed=41))
    lake = m.theme(Theme('lake', wall=[('basalt', 3), ('blackstone', 2), ('magma_block', 1)],
                         floor=[('basalt', 2), ('blackstone', 2)], ceil=[('basalt', 2), ('blackstone', 1)], seed=42))
    arsenal = m.theme(Theme('arsenal', wall=[('deepslate_tiles', 3), ('polished_blackstone_bricks', 2)],
                            floor=[('polished_deepslate', 2), ('iron_block', 1), ('deepslate_tiles', 2)], ceil='deepslate_tiles', seed=43))
    throne = m.theme(Theme('ashthrone', wall=[('polished_blackstone_bricks', 3), ('gilded_blackstone', 1), ('nether_bricks', 2)],
                           floor=[('polished_blackstone', 3), ('gilded_blackstone', 1)], ceil='nether_bricks', seed=44))
    s = Stage(N, 'forge', 'Кузни Пепла', (30, 40), 'forge')

    # ================================================================== пустоты
    # ---- Ярус I · Верхние кузни (84), на восток
    m.room(X(0), 20, X(16), 34, A, 7, forge, vault=2)                           # Кузнечный двор (привал)
    m.room(X(4), 35, X(10), 40, A, 4, forge)                                    # ниша Дымохода
    m.corridor([(X(16), 27), (X(22), 27)], 3, 4, forge, y=A)
    m.room(X(22), 16, X(58), 38, A, 10, forge, vault=3)                         # Литейный цех (мостки на A+6)
    m.carve(X(23), A + 6, 15, X(23), A + 7, 15, forge)                          # ниша тайника над мостками
    m.corridor([(X(58), 27), (X(64), 27)], 3, 4, forge, y=A)
    m.room(X(64), 18, X(80), 36, A, 7, forge, vault=2)                          # Печати горнов
    m.corridor([(X(80), 27), (X(87), 27)], 3, 4, forge, y=A)
    m.round_room(X(103) + 0.5, 27.5, 17.5, A, 12, forge, dome=4)                # Арена Плавильного голема (r 17)
    for (vx, vz) in vats_at():
        vat(m, vx, vz, A, forge)
    m.corridor([(X(121), 27), (X(127), 27)], 3, 4, forge, y=A)
    m.carve(X(127), B, 12, X(157), A + 9, 42, lake)                             # Плавильная шахта (28 в высоту)

    # ---- Ярус II · Литейные недра (66), на запад под ярусом I
    m.corridor([(X(127), 27), (X(120), 27)], 3, 4, lake, y=B)
    m.room(X(84), 12, X(120), 42, B, 12, lake, vault=2)                         # Лавовое озеро
    m.carve(X(88), B - 4, 15, X(116), B - 1, 39, lake)                          # лавовая яма (лава — на дне, B-5)
    m.corridor([(X(84), 27), (X(78), 27)], 3, 4, arsenal, y=B)
    m.room(X(50), 14, X(78), 40, B, 9, arsenal, vault=2)                        # Арсенал
    m.corridor([(X(55), 14), (X(55), 8)], 2, 3, arsenal, y=B)                   # ход к божественной секретке
    m.room(X(50), 1, X(60), 7, B, 5, arsenal, vault=1)                          # божественная секретка
    m.room(X(62), 42, X(68), 45, B, 4, arsenal)                                 # тайная ниша рычага
    m.corridor([(X(50), 27), (X(36), 27)], 3, 4, forge, y=B)
    m.room(X(0), 24, X(36), 44, B, 6, forge)                                    # Зал мехов (h 6 — с лозы соскочить ≤ 5)
    m.carve(X(6), B, 38, X(7), A + 2, 39, forge)                                # Дымоход: шахта 2×2 до ниши привала
    m.corridor([(X(20), 44), (X(20), 51)], 3, 4, forge, y=B)
    m.round_room(X(20) + 0.5, 71.5, 20.5, B, 12, forge, dome=3)                 # Ристалище Колесницы (r 20)
    m.corridor([(X(41), 71), (X(47), 71)], 3, 4, forge, y=B)
    m.room(X(46), 60, X(70), 80, B, 8, forge, vault=2)                          # Закалочная
    m.dropper(X(57), 69, X(59), 71, B, C, forge, depth=3, pad=2)                # Закалочный чан: 18 блоков в воду

    # ---- Ярус III · Горнила (48), на восток
    m.room(X(44), 58, X(72), 82, C, 9, forge)                                   # Закалочный чан (низ)
    m.corridor([(X(72), 70), (X(80), 70)], 3, 4, lake, y=C)
    m.cave(X(99), C + 7, 70, 19, 9, 16, lake, rough=0.15, seed=11, floor=C)     # Зал горнил
    m.corridor([(X(117), 70), (X(124), 70)], 3, 4, lake, y=C)
    m.carve(X(124), D, 54, X(156), C + 10, 86, lake)                            # Висячие марши (29 в высоту)

    # ---- Ярус IV · Тронные недра (30)
    m.corridor([(X(156), 76), (X(160), 76)], 3, 4, throne, y=D)
    m.room(X(160), 68, X(176), 84, D, 8, throne, vault=2)                       # Преддверие
    m.corridor([(X(176), 76), (X(188), 76)], 3, 4, throne, y=D)
    m.round_room(X(205) + 0.5, 76.5, 18.5, D, 12, throne, dome=5)               # Тронный зал Владыки (r 18)
    m.corridor([(X(205), 94), (X(205), 102)], 3, 4, throne, y=D)
    m.round_room(X(205) + 0.5, 117.5, 15.5, D, 12, lake, dome=6)                # Кальдера
    m.room(X(202), 132, X(208), 137, D, 6, throne)                              # ниша разлома

    # ---- вертикаль до оболочки
    m.vines(X(6), 38, B, A - 1, kind='twisting', to=(X(7), 39))                 # багровая лоза Дымохода
    m.ladder(X(58), 17, A, A + 5, 'west')                                       # лестница на мостки цеха
    m.sleeve(X(57), 69, X(59), 71, C + 3, C + 8, 'polished_blackstone_bricks')  # рукав чана: до 3 блоков над водой
    bridge = m.bridge((X(88), 27), (X(116), 27), B, 'polished_blackstone_bricks', width=3, rail=WALL,
                      posts='polished_blackstone_bricks', post_every=7, sag=2, tall_posts=True)
    m.parkour([(X(92), B, 14), (X(92), B, 17)], 'basalt', theme=lake)
    m.shell(2)
    # дальше всё отложенное (`later`) выполняется сразу — оболочка уже стоит
    # магма из палитры стен попадает и в пол у стен: на ней жжёт и монстру не встать — в полу только камень
    for (x, y, z) in list(m.open):
        b = m.get(x, y - 1, z)
        if b and 'magma' in b and (x, y - 1, z) not in m.open and m.is_air(x, y, z):
            m.set(x, y - 1, z, 'blackstone')

    # ================================================================== Кузнечный двор (привал)
    for (ax, az) in ((X(3), 22), (X(3), 32)):
        m.set(ax, A, az, 'anvil', facing='north')
        m.set(ax + 1, A, az, 'blast_furnace', facing='east', lit=True)
    m.set(X(8), A, 27, 'campfire', facing='north', lit=True, signal_fire=False, waterlogged=False)
    for dx in (-2, 2):
        m.stairs(X(8 + dx), A, 27, 'polished_blackstone_brick_stairs', 'east' if dx < 0 else 'west')
    m.sign(X(16), A + 1, 22, 'west')
    s.sign((X(16), A + 1, 22), ['ЭТАП 4', 'Кузни Пепла', 'уровни 30–40', ''])
    # ниша Дымохода: бортик вокруг устья, подход с севера
    m.railing([(X(5), A, z) for z in (37, 38, 39, 40)] + [(X(8), A, z) for z in (37, 38, 39, 40)] +
              [(X(6), A, 40), (X(7), A, 40)], WALL)
    m.sign(X(4), A + 1, 36, 'east')
    s.sign((X(4), A + 1, 36), ['Дымоход.', 'Срезка снизу —', 'от Зала мехов.', 'Лоза держит.'])
    s.say((X(0), A - 1, 20, X(16), A + 8, 40),
          'Система: «Здесь ковали оружие для армии, которой не стало. Горны так и не погасли. '
          'Кузни уходят в гору на четыре яруса — каждый под предыдущим. Дымоход в южной нише — '
          'срезка с нижнего яруса: по нему потом можно вернуться к торговцу.»')

    # ================================================================== Литейный цех: желоб лавы и мостки
    for x in range(X(26), X(57), 6):
        for z in (19, 35):
            column(m, x, z, A, m.ceil_y(x, A, z) - A + 1, 'polished_blackstone_bricks', 'basalt', 'magma_block')
    channel = []
    for x in range(X(23), X(58)):
        for z in range(26, 29):
            if x in (X(30), X(40), X(50)):
                m.set(x, A - 1, z, 'iron_block')                                  # переходы через желоб
            else:
                channel.append((x, A - 1, z))
    channel = lava_sheet(m, channel)
    walk = {}
    flat(walk, X(22), 16, X(57), 18, A + 6)                                     # мостки вдоль северной стены
    lay(m, walk, 'polished_blackstone_bricks', rim='polished_blackstone', corbel='polished_blackstone_brick_stairs')
    rails(m, walk, 'iron_bars', skip=[(X(58), 17)])                          # в два ряда: на решётку запрыгивают
    m.set(X(23), A + 6, 15, 'litrpg:cache', facing='south')                    # в нише стены: с крышки — не на перила
    s.cache('catwalk', (X(23), A + 6, 15), 1)
    for x in range(X(28), X(56), 9):
        m.chain_lantern(x, m.ceil_y(x, A + 6, 17), 17, 2)
    s.pack('p1', [('forge_construct', X(32), A, 21, 31), ('ash_crossbow', X(34), A, 33, 31), ('imp', X(30), A, 32, 31),
                  ('imp', X(31), A, 33, 31, None, 4)])
    s.pack('p2', [('molten_golem', X(44), A, 22, 32), ('fire_shaman', X(46), A, 34, 32), ('salamander', X(42), A, 33, 32),
                  ('imp', X(45), A, 31, 32, None, 4)])
    s.pack('p3', [('ash_crossbow', X(30), A + 6, 17, 32), ('ash_crossbow', X(42), A + 6, 17, 32), ('imp', X(50), A + 6, 17, 32),
                  ('ash_crossbow', X(54), A + 6, 17, 32, None, 4)])
    s.pack('p4', [('imp', X(53), A, 21, 32), ('imp', X(54), A, 23, 32), ('imp', X(55), A, 21, 32), ('imp', X(54), A, 33, 32),
                  ('fire_shaman', X(56), A, 34, 32), ('imp', X(53), A, 35, 32, None, 5)])
    m.sign(X(58), A + 1, 20, 'west')
    s.sign((X(58), A + 1, 20), ['Мостки.', 'Лестница —', 'в северо-восточном', 'углу.'])
    s.say((X(22), A - 1, 16, X(58), A + 12, 38),
          'Система: «Литейный цех. В желобе — живой металл, наступать не советую. '
          'Арбалетчики бьют с мостков под сводом: лестница наверх — в северо-восточном углу.»')

    # ================================================================== Печати горнов (по цене металла)
    metals = [('gold_block', X(68), 21), ('copper_block', X(76), 33), ('diamond_block', X(76), 21), ('iron_block', X(68), 33)]
    btn = {}
    for (blk, mx, mz) in metals:
        m.set(mx, A, mz, blk)
        m.set(mx, A + 1, mz, 'anvil', facing='north')
        bz = mz + (1 if mz < 27 else -1)
        m.set(mx, A, bz, 'polished_blackstone_bricks')
        m.set(mx, A + 1, bz, 'polished_blackstone_button', face='floor', facing='north', powered=False)
        btn[blk] = (mx, A + 1, bz)
    order = [btn[b] for b in ('copper_block', 'iron_block', 'gold_block', 'diamond_block')]
    mech_seq = s.mech('metals', 'sequence', order, text='Печати горнов')
    m.sign(X(64), A + 1, 24, 'east')
    s.sign((X(64), A + 1, 24), ['Куй от дешёвого', 'к дорогому:', 'жми кнопки у', 'наковален.'])
    p5 = s.pack('p5', [('forge_construct', X(72), A, 27, 33), ('ash_crossbow', X(78), A, 29, 33), ('ash_crossbow', X(78), A, 25, 33, None, 4)])
    s.gate('g1', (X(82), A, 26, X(82), A + 3, 28), open_when=[f'mech:{mech_seq}', f'pack:{p5}'])

    # ================================================================== Арена Плавильного голема: шесть чанов
    for i in range(10):
        a = (i + 0.5) * math.pi / 5
        bx, bz = math.floor(X(103) + 0.5 + 16.3 * math.cos(a)), math.floor(27.5 + 16.3 * math.sin(a))
        column(m, bx, bz, A, m.ceil_y(bx, A, bz) - A + 1, 'polished_blackstone_bricks', 'basalt', 'magma_block')
    b1 = s.boss('smelter', 'smelter', (X(103), A, 27), 17)
    s.gate('arena1', (X(84), A, 26, X(84), A + 3, 28), lock=b1)
    s.gate('g2', (X(124), A, 26, X(124), A + 3, 28), open_when=[f'boss:{b1}'])
    s.say((X(86), A - 1, 10, X(121), A + 15, 45),
          'Система: «Плавильный голем. У него жарко — когда шкала жара полна, прыгайте в чан с водой. '
          'Чаны стоят кругом: до ближайшего — не больше рывка.»')

    # ================================================================== Плавильная шахта: серпантин по стенам вниз на 18
    path = {}
    flat(path, X(127), 12, X(130), 42, A)                                       # верхняя площадка у западной стены
    y1 = ramp(path, (X(131), 13), (X(154), 13), A + 1)                          # по северной стене: 84 → 73
    flat(path, X(155), 12, X(157), 14, y1)                                      # угол
    y2 = ramp(path, (X(156), 15), (X(156), 25), y1)                             # по восточной стене: 72 → 67
    assert y2 == B + 1, y2
    lay(m, path, 'polished_blackstone_bricks', rim='polished_blackstone', corbel='polished_blackstone_brick_stairs')
    rails(m, path, WALL)
    # лавовое озеро на дне в ограде
    pool = lava_sheet(m, [(x, B - 1, z) for x in range(X(134), X(151)) for z in range(20, 36)])
    m.railing([(x, B, z) for x in range(X(133), X(152)) for z in (19, 36)] +
              [(x, B, z) for x in (X(133), X(151)) for z in range(20, 36)], WALL)
    # над озером на цепях висит ковш с живым металлом
    cx, cz = X(142), 27
    ladle = []
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if abs(dx) == 2 and abs(dz) == 2:
                continue
            m.set(cx + dx, A, cz + dz, 'polished_blackstone')
            if max(abs(dx), abs(dz)) == 2:
                m.set(cx + dx, A + 1, cz + dz, 'polished_blackstone_bricks')
            else:
                ladle.append((cx + dx, A + 1, cz + dz))
    for c in ladle:
        m.set(*c, 'lava', level=0)
    for (dx, dz) in ((-2, 0), (2, 0), (0, -2), (0, 2)):
        chains_up(m, cx + dx, A + 2, cz + dz)
    jets = [((X(137), path[(X(137), 13)], 12), (X(138), path[(X(137), 13)], 14)),
            ((X(145), path[(X(145), 13)], 12), (X(146), path[(X(145), 13)], 14)),
            ((X(153), path[(X(153), 13)], 12), (X(154), path[(X(153), 13)], 14)),
            ((X(155), path[(X(156), 19)], 19), (X(157), path[(X(156), 19)], 20))]
    for k, (lo_, hi_) in enumerate(jets):
        s.mech(f'jets{k}', 'hazard', area=lo_ + hi_, period=2.8, warn=1.0, share=0.18, offset=k * 0.7, sound=1)
    m.sign(X(127), A + 1, 24, 'east')
    s.sign((X(127), A + 1, 24), ['Плавильная шахта.', 'Серпантин вниз', 'по стенам.', 'Струи — в такт.'])
    s.say((X(127), A - 1, 12, X(157), A + 9, 42),
          'Система: «Плавильная шахта. Сюда сливали металл со всех кузен — вниз на восемнадцать блоков. '
          'Серпантин идёт по стенам, перила держат. Из-под ступеней бьёт огонь — по очереди, идите в такт.»')
    s.pack('p6', [('forge_construct', X(129), B, 24, 33), ('salamander', X(130), B, 31, 33), ('fire_shaman', X(129), B, 38, 33),
                  ('salamander', X(140), B, 39, 33), ('ash_crossbow', X(130), B, 15, 33, None, 4)])
    s.say((X(127), B - 1, 12, X(157), B + 8, 42),
          'Система: «Дно шахты. Наверх отсюда — только серпантином. К привалу короче через Дымоход: он впереди, за Арсеналом.»')

    # ================================================================== Ярус II · Лавовое озеро: мост на цепях
    pillars = {(X(92), 17)} | {(x, z) for x in range(X(91), X(94)) for z in range(20, 23)}
    for (x, z) in pillars:                                                      # столб и островок — из лавы
        for y in range(B - 5, B - 1):
            m.set(x, y, z, 'basalt', axis='y')
    pit = lava_sheet(m, [(x, B - 5, z) for x in range(X(88), X(117)) for z in range(15, 40) if (x, z) not in pillars])
    isle = {}
    flat(isle, X(91), 20, X(93), 22, B)
    lay(m, isle, 'polished_basalt', rim='basalt')
    m.set(X(92), B, 21, 'litrpg:cache', facing='north')
    s.cache('lake_isle', (X(92), B, 21), 2)
    # ограда ямы по берегу (проходы — на мост и к прыжку на столб)
    fence = [(x, B, z) for x in range(X(87), X(118)) for z in (14, 40)] + [(x, B, z) for x in (X(87), X(117)) for z in range(15, 40)]
    fence = [c for c in fence if not (25 <= c[2] <= 29 and c[0] in (X(87), X(117))) and (c[0], c[2]) != (X(92), 14)]
    m.railing(fence, WALL)
    for (i, x, z, yy) in bridge:                                                # цепи от столбов к своду
        if i % 7 == 0:
            for zz in (25, 29):
                chains_up(m, x, yy + 3, zz)
    ys = {i: yy for (i, x, z, yy) in bridge}
    k = 0
    for (i, x, z, yy) in bridge:
        if i in (6, 13, 20):
            lo_, hi_ = min(ys[i], ys[i + 1]), max(ys[i], ys[i + 1])
            s.mech(f'bjets{k}', 'hazard', area=(x, lo_, 26, x + 1, hi_, 28), period=2.6, warn=1.0, share=0.2, offset=k * 0.8, sound=1)
            k += 1
    p7 = s.pack('p7', [('salamander', X(86), B, 22, 34), ('salamander', X(86), B, 32, 34), ('imp', X(85), B, 27, 34),
                       ('fire_shaman', X(85), B, 37, 34), ('molten_golem', X(86), B, 17, 34, None, 4)])
    m.sign(X(120), B + 1, 23, 'west')
    s.sign((X(120), B + 1, 23), ['Мост на цепях.', 'Внизу — лава.', 'Огонь бьёт', 'по очереди.'])
    s.say((X(84), B - 1, 12, X(120), B + 12, 42),
          'Система: «Лавовое озеро. Мост держится на цепях и честном слове. Огонь бьёт из-под досок по очереди. '
          'Внизу лава: Система вытащит, но дорого. А с северного берега кто-то прыгал на столб — и дальше, на островок.»')

    # ================================================================== Арсенал: Мастер-кузнец Горн и божественная секретка
    for z in range(16, 40, 4):
        for x in (X(52), X(76)):
            m.set(x, B, z, 'smithing_table')
            m.set(x, B + 1, z, 'iron_bars', north=False, south=False, east=False, west=False, waterlogged=False)
    for x in range(X(58), X(72), 4):                                            # точила посередине
        m.set(x, B, 20, 'grindstone', face='floor', facing='north')
        m.set(x, B, 34, 'grindstone', face='floor', facing='north')
    master = s.pack('master', [('forge_construct', X(64), B, 27, 36), ('ash_crossbow', X(60), B, 22, 35), ('ash_crossbow', X(68), B, 32, 35),
                               ('molten_golem', X(62), B, 31, 35, None, 4)], title='Мастер-кузнец Горн')
    m.set(X(74), B, 38, 'litrpg:locked_cache', facing='north')
    s.cache('arsenal', (X(74), B, 38), 2)
    # три скрытых рычага: в нише за ложной стеной, высоко на западной стене, в юго-восточном углу за верстаком
    hidden = [(X(65), B + 1, 45), (X(50), B + 2, 15), (X(78), B + 1, 39)]
    m.set(X(65), B + 1, 46, 'deepslate_tiles')
    m.set(X(65), B + 1, 45, 'lever', face='wall', facing='north', powered=False)
    m.set(X(49), B + 2, 15, 'deepslate_tiles')
    m.set(X(50), B + 2, 15, 'lever', face='wall', facing='east', powered=False)
    m.set(X(79), B + 1, 39, 'deepslate_tiles')
    m.set(X(78), B + 1, 39, 'lever', face='wall', facing='west', powered=False)
    mech_hidden = s.mech('hidden', 'levers', hidden, text='Скрытые рычаги арсенала')
    for y in range(B, B + 2):
        m.set(X(65), y, 41, 'infested_cobblestone')                             # ложная стена ниши
    s.gate('vault', (X(54), B, 10, X(55), B + 2, 10), open_when=[f'mech:{mech_hidden}'])
    for y in range(B, B + 2):
        for x in (X(54), X(55)):
            for z in (12, 13):
                m.set(x, y, z, 'infested_cobblestone')                          # завал в ходе к секретке
    m.set(X(55), B, 3, 'litrpg:divine_cache', facing='south')
    s.cache('divine_forge', (X(55), B, 3), 4)
    for (bx, bz) in ((X(51), 2), (X(59), 2)):
        brazier(m, bx, B, bz, soul=True)
    s.gate('g3', (X(42), B, 26, X(42), B + 3, 28), open_when=[f'pack:{master}', f'pack:{p7}'])
    s.say((X(50), B - 1, 14, X(78), B + 10, 40),
          'Система: «Арсенал. Мастер Горн держит ключ. А в стенах здесь кое-что спрятано — '
          'ищите булыжник среди кирпича и рычаги там, где их не ждёшь.»')

    # ================================================================== Зал мехов и Дымоход
    for x in range(X(10), X(34), 6):                                            # мехи у северной стены
        for dx in (0, 1):
            m.set(x + dx, B, 25, 'stripped_dark_oak_log', axis='x')
            m.set(x + dx, B + 1, 25, 'dark_oak_trapdoor', facing='south', half='bottom', open=False, powered=False, waterlogged=False)
        m.set(x + 2, B, 25, 'blast_furnace', facing='south', lit=True)
    s.climb_trap('chimney', (X(6), B + 1, 38, X(7), A - 2, 39), period=3.0, warn=1.0, share=0.15, layer=2, sound=1)
    m.sign(X(8), B + 1, 24, 'south')
    s.sign((X(8), B + 1, 24), ['Дымоход', 'к привалу —', 'в юго-западном', 'углу. Мехи дуют.'])
    p8 = s.pack('p8', [('forge_construct', X(24), B, 34, 35), ('imp', X(18), B, 30, 35), ('imp', X(20), B, 38, 35),
                       ('fire_shaman', X(28), B, 40, 35), ('salamander', X(14), B, 36, 35, None, 4)])
    # Печать над устьем у привала: пока Зал мехов не очищен снизу, от привала в Дымоход не спрыгнуть (иначе — мимо
    # голема, шахты и Арсенала прямо к Колеснице). Снизу лезут до печати и ждут; печать — на уровне пола ниши.
    s.gate('chimney_seal', (X(6), A, 38, X(7), A, 39), open_when=[f'pack:{p8}'])
    s.say((X(0), B - 1, 24, X(36), B + 6, 44),
          'Система: «Зал мехов. Отсюда Дымоход ведёт прямо к привалу и торговцу — по лозе вверх, восемнадцать блоков. '
          'Мехи дуют ярусами по очереди: лезьте в такт, пережидая вспышку над головой.»')

    # ================================================================== Ристалище Колесницы
    for i in range(12):
        a = i * math.pi / 6 + math.pi / 12
        bx, bz = math.floor(X(20) + 0.5 + 19.2 * math.cos(a)), math.floor(71.5 + 19.2 * math.sin(a))
        m.set(bx, B, bz, 'polished_blackstone_wall', up=True, north='none', south='none', east='none', west='none', waterlogged=False)
        m.set(bx, B + 1, bz, 'campfire', facing='north', lit=True, signal_fire=False, waterlogged=False)
    b2 = s.boss('chariot', 'chariot', (X(20), B, 71), 20)
    s.gate('arena2', (X(19), B, 48, X(21), B + 3, 48), lock=b2)
    s.gate('g4', (X(44), B, 70, X(44), B + 3, 72), open_when=[f'boss:{b2}'])
    s.say((X(0), B - 1, 50, X(41), B + 15, 92),
          'Система: «Колесница Пламени. Уходите с полосы разгона — ристалище большое, места хватит. '
          'На пит-стопе — бейте все.»')

    # ================================================================== Закалочная: дыра в полу — в чан
    troughs = []
    for (tx, tz) in ((X(48), 62), (X(48), 78), (X(67), 62), (X(67), 78)):
        for dx in (0, 1, 2):
            m.set(tx + dx, B - 1, tz, 'water', level=0)
            troughs.append((tx + dx, B - 1, tz))
    m.check_water(troughs)
    s.pack('p9', [('molten_golem', X(52), B, 66, 36), ('ash_crossbow', X(64), B, 76, 36), ('fire_shaman', X(64), B, 64, 36),
                  ('imp', X(52), B, 75, 36, None, 4)])
    m.sign(X(56), B + 1, 60, 'south')
    s.sign((X(56), B + 1, 60), ['Закалочный чан.', 'Внизу — вода.', 'Прыгайте', 'в дыру.'])
    s.say((X(46), B - 1, 60, X(70), B + 9, 80),
          'Система: «Закалочная. Клинки отсюда бросали в чан ярусом ниже — и вы туда же. '
          'Восемнадцать блоков, внизу вода. Назад — только через Дымоход.»')

    # ================================================================== Ярус III · Закалочный чан (низ)
    s.pack('p10', [('forge_construct', X(48), C, 62, 36), ('salamander', X(68), C, 62, 36), ('ash_crossbow', X(48), C, 78, 36),
                   ('fire_shaman', X(68), C, 78, 36), ('imp', X(65), C, 70, 36, None, 4)])
    for (bx, bz) in ((X(45), 59), (X(71), 59), (X(45), 81), (X(71), 81)):
        brazier(m, bx, C, bz, soul=False)
    s.say((X(44), C - 1, 58, X(72), C + 9, 82),
          'Система: «Чан закалки. Вода шипит до сих пор. Засада — классика жанра.»')

    # ================================================================== Зал горнил: три острова в лаве
    hall = lava_sheet(m, {(x, C - 1, z) for (x, y, z) in m.open if y == C and X(84) <= x <= X(114) and (x, C - 1, z) not in m.open})
    islands = [(X(91), 61), (X(107), 61), (X(99), 80)]
    paths = [((X(82), 70), (X(91), 61)), ((X(91), 61), (X(107), 61)), ((X(91), 61), (X(99), 80)), ((X(107), 61), (X(116), 70)),
             ((X(99), 80), (X(116), 70)), ((X(82), 70), (X(116), 70))]
    for (ax, az), (bx, bz) in paths:
        n = max(abs(bx - ax), abs(bz - az))
        for i in range(n + 1):
            x, z = round(ax + (bx - ax) * i / n), round(az + (bz - az) * i / n)
            for dx in (0, 1):
                for dz in (0, 1):
                    m.set(x + dx, C - 1, z + dz, 'smooth_basalt')
    plates = []
    for (ix, iz) in islands:
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                if dx * dx + dz * dz <= 10:
                    m.set(ix + dx, C - 1, iz + dz, 'polished_blackstone')
        m.set(ix, C, iz, 'light_weighted_pressure_plate', power=0)
        plates.append((ix, C, iz))
        brazier(m, ix + 2, C, iz + 2, soul=False)
    for dx in range(-3, 4):
        for dz in range(-3, 5):
            m.set(X(99 + dx), C - 1, 70 + dz, 'polished_blackstone')
    mech_plates = s.mech('islands', 'plates', plates, text='Три горнила', hold=3)
    m.set(X(99), C, 70, 'lectern', facing='west', has_book=False, powered=False)
    mech_riddle = s.mech('riddle', 'riddle', area=(X(96), C, 67, X(102), C + 3, 73), answers=['огонь', 'пламя', 'костер', 'костёр'],
                         text='Загадка горнила|Чем больше её кормишь, тем она сильнее; дашь ей пить — умрёт.')
    for yy in (C, C + 1):
        m.set(X(99), yy, 74, 'polished_blackstone_bricks')
    m.sign(X(99), C + 1, 73, 'north')
    s.sign((X(99), C + 1, 73), ['Чем больше', 'кормишь — тем', 'сильнее; дашь', 'пить — умрёт.'])
    p11 = s.pack('p11', [('molten_golem', X(107), C, 63, 37), ('fire_shaman', X(109), C, 60, 37), ('imp', X(106), C, 59, 37)])
    p12 = s.pack('p12', [('forge_construct', X(99), C, 78, 38), ('salamander', X(97), C, 81, 38), ('salamander', X(101), C, 81, 38),
                         ('ash_crossbow', X(99), C, 82, 38, None, 4)])
    s.gate('g5', (X(121), C, 69, X(121), C + 3, 71), open_when=[f'mech:{mech_plates}', f'mech:{mech_riddle}', f'pack:{p11}', f'pack:{p12}'])
    m.set(X(91), C, 64, 'litrpg:cache', facing='south')
    s.cache('crucible', (X(91), C, 64), 1)
    s.say((X(80), C - 1, 54, X(118), C + 16, 86),
          'Система: «Зал горнил. Три горнила на островах — встаньте на все три разом. '
          'И загадка у аналоя в середине. Лава под ногами по щиколотку — но вам хватит.»')

    # ================================================================== Висячие марши: четыре площадки на цепях над лавой
    marsh = lava_sheet(m, [(x, D - 1, z) for x in range(X(124), X(157)) for z in range(54, 87) if not (x >= X(146) and z >= 64)])
    feet = {}
    flat(feet, X(124), 64, X(128), 76, C)                                       # P0 у входа (48)
    y = ramp(feet, (X(129), 70), (X(139), 70), C)                               # B1 на восток: 47 → 42
    disc(feet, X(142) + 0.5, 70.5, 2.5, y)                                      # P1 (42)
    y = ramp(feet, (X(142), 67), (X(142), 59), y)                               # B2 на север: 41 → 37
    disc(feet, X(142) + 0.5, 56.5, 2.5, y)                                      # P2 (37)
    y = ramp(feet, (X(145), 56), (X(151), 56), y)                               # B3 на восток: 36 → 33
    disc(feet, X(154) + 0.5, 56.5, 2.5, y)                                      # P3 (33)
    y = ramp(feet, (X(154), 59), (X(154), 63), y)                               # B4 на юг: 32 → 30 — на площадку дна
    assert y == D, y
    lay(m, feet, 'polished_blackstone_bricks', rim='polished_blackstone', corbel='polished_blackstone_brick_stairs')
    railed = rails(m, feet, WALL, down=0)
    # площадка дна: бортик над лавой (вход — с пролёта)
    m.railing([(X(146), D, z) for z in range(64, 87)] + [(x, D, 64) for x in range(X(147), X(157)) if not X(153) <= x <= X(155)], WALL)
    tops = {}
    for (x, yy, z) in railed:
        tops[(x, z)] = max(tops.get((x, z), yy), yy)
    for (x, z), yy in sorted(tops.items()):                                     # цепи от перил к своду
        if h3(x, 0, z, 47) < 0.3:
            chains_up(m, x, yy + 1, z)
    for (x, z) in ((X(142), 70), (X(142), 56), (X(154), 56)):                   # фонари под площадками
        ytop = feet[(x, z)] - 2
        m.chain_lantern(x, ytop, z, max(0, min(2, ytop - D - 1)))               # фонарь не ниже D+1 (там лава)
    hammers = [((X(141), 64), (X(143), 65)), ((X(141), 60), (X(143), 61)), ((X(153), 61), (X(155), 62))]
    for k, ((ax, az), (bx, bz)) in enumerate(hammers):
        yy = feet[(ax + 1, az)]
        assert feet[(bx - 1, bz)] == yy, (k, yy)
        s.mech(f'hammer{k}', 'hazard', area=(ax, yy, az, bx, yy, bz), period=3.0, warn=1.1, share=0.2, offset=k * 1.0, sound=1)
    m.sign(X(124), C + 1, 68, 'east')
    s.sign((X(124), C + 1, 68), ['Висячие марши.', 'Вниз по цепям', 'на дно.', 'Молоты — в такт.'])
    p13 = s.pack('p13', [('molten_golem', X(150), D, 70, 38), ('ash_crossbow', X(155), D, 82, 38), ('fire_shaman', X(148), D, 80, 38),
                         ('salamander', X(152), D, 74, 38, None, 4)])
    s.say((X(124), D - 1, 54, X(156), C + 10, 86),
          'Система: «Висячие марши. Кузнецы спускались к Владыке по цепям — и не все поднимались обратно. '
          'Четыре площадки, пролёты между ними; молоты бьют по пролётам по очереди. Под вами лава. Не смотрите вниз. '
          'Или смотрите — красиво же.»')

    # ================================================================== Ярус IV · Преддверие, Тронный зал, Кальдера
    p14 = s.pack('p14', [('forge_construct', X(166), D, 72, 39), ('forge_construct', X(166), D, 80, 39), ('fire_shaman', X(172), D, 76, 39),
                         ('ash_crossbow', X(174), D, 70, 39), ('imp', X(170), D, 82, 39, None, 4)])
    for (bx, bz) in ((X(161), 69), (X(175), 69), (X(161), 83), (X(175), 83)):
        brazier(m, bx, D, bz, soul=False)
    s.gate('g6', (X(180), D, 75, X(180), D + 3, 77), open_when=[f'pack:{p13}', f'pack:{p14}'])
    for i in range(8):
        a = i * math.pi / 4 + math.pi / 8
        bx, bz = math.floor(X(205) + 0.5 + 15 * math.cos(a)), math.floor(76.5 + 15 * math.sin(a))
        column(m, bx, bz, D, m.ceil_y(bx, D, bz) - D + 1, 'nether_bricks', 'gilded_blackstone', 'magma_block')
    ring = set()
    for i in range(360):
        if 75 <= i <= 105 or 255 <= i <= 285:
            continue  # проходы: с севера (из тронного зала) и на юг (к разлому)
        a = math.radians(i)
        x, z = math.floor(X(205) + 0.5 + 13.5 * math.cos(a)), math.floor(117.5 + 13.5 * math.sin(a))
        if (x, D, z) in m.open and (x, D - 1, z) not in m.open:
            ring.add((x, D - 1, z))
    ring = lava_sheet(m, ring)
    b3 = s.boss('ashlord', 'ash_lord', (X(205), D, 76), 18, final=True, arena2=(X(205) + 0.5, D, 117.5, 12))
    s.gate('arena3', (X(183), D, 75, X(183), D + 3, 77), lock=b3)
    s.gate('caldera', (X(204), D, 98, X(206), D + 3, 98), open_when=[f'boss:{b3}'])
    s.say((X(176), D - 1, 58, X(224), D + 17, 95),
          'Система: «Владыка Пепла. Вызванный на дуэль — бейтесь один. Остальным — не мешать и лечить. '
          'Пол здесь обрушается в лаву кругами: с каждым обвалом места меньше.»')
    s.say((X(190), D - 1, 102, X(221), D + 17, 133),
          'Система: «Кальдера. Сердце горы. Здесь Владыка ковал пепел — теперь куёт вас.»')
    for dx in (-2, 2):
        for dy in range(0, 4):
            m.set(X(205 + dx), D + dy, 135, 'crying_obsidian')
    for dx in (-1, 0, 1):
        m.set(X(205 + dx), D + 3, 135, 'crying_obsidian')
        for dy in range(0, 3):
            m.set(X(205 + dx), D + dy, 135, 'litrpg:rift', active=False)
    apply_gates(m, s)

    check_lava(m, channel + pool + ladle + pit + hall + marsh + ring)
    m.hanging_lights(spacing=6, soul=False, themes=['forge', 'arsenal', 'ashthrone'], min_h=6, chain=2)
    m.glow(spacing=5, level=8, height=3)

    s.box = (X(-6), D - 12, -6, X(232), A + 24, 145)
    s.camp = (X(8.5), A, 30.5, -90)
    s.trader = (X(5.5), A, 24.5, -90)
    s.rift = (X(204), D, 135, X(206), D + 2, 135)
    return m, s
