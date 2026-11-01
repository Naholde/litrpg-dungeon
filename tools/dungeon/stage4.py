"""
Этап 4 · Кузни Пепла (уровни 30–40). Базальт, чернокамень, лава в желобах, цепи, наковальни, горны.

Путь: Кузнечный двор (привал) → Литейный цех → Печати горнов (по цене металла) → Арена Плавильного голема (чаны) →
Цепной мост над лавовым озером (огненные струи) → Арсенал (Мастер-кузнец Горн; божественная секретка за тремя
скрытыми рычагами) → Ристалище Колесницы → Зал горнил (плиты на трёх островах, загадка «огонь») → Тронный зал Владыки →
Кальдера (второй зал, разлом).
"""
import math

from decor import brazier, column
from kit import Map, Stage, Theme, apply_gates

N = 4
OX = 1024 * N
FY = 64


def X(x):
    return OX + x


def lava_channel(m, x1, z1, x2, z2, y):
    """Желоб лавы на уровне пола (лава на уровне ног, ниже — магма)."""
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for z in range(min(z1, z2), max(z1, z2) + 1):
            m.set(x, y - 1, z, 'magma_block')
            m.set(x, y, z, 'lava', level=0)


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

    # ------------------------------------------------------------------ пустоты
    m.room(X(0), 20, X(16), 34, FY, 7, forge, vault=2)                          # Кузнечный двор
    m.corridor([(X(16), 27), (X(22), 27)], 3, 4, forge, y=FY)
    m.room(X(22), 16, X(58), 38, FY, 9, forge, vault=3)                         # Литейный цех
    m.corridor([(X(58), 27), (X(64), 27)], 3, 4, forge, y=FY)
    m.room(X(64), 18, X(80), 36, FY, 7, forge, vault=2)                         # Печати горнов
    m.corridor([(X(80), 27), (X(86), 27)], 3, 4, forge, y=FY)
    m.round_room(X(103), 27, 16, FY, 11, forge, dome=4)                         # Арена Плавильного голема
    m.corridor([(X(119), 27), (X(124), 27)], 3, 4, lake, y=FY)
    m.cave(X(142), FY + 7, 27, 18, 8, 12, lake, rough=0.15, seed=10, floor=FY)  # Лавовое озеро
    m.corridor([(X(160), 27), (X(166), 27)], 3, 4, arsenal, y=FY)
    m.room(X(166), 14, X(190), 40, FY, 8, arsenal, vault=2)                     # Арсенал
    m.room(X(192), 3, X(200), 11, FY, 5, arsenal)                               # божественная секретка
    m.corridor([(X(186), 16), (X(186), 7), (X(192), 7)], 2, 3, arsenal, y=FY)
    m.room(X(176), 42, X(182), 48, FY, 4, arsenal)                              # тайная ниша рычага
    m.corridor([(X(190), 27), (X(196), 27)], 3, 4, forge, y=FY)
    m.round_room(X(215), 27, 19, FY, 10, forge, dome=3)                         # Ристалище
    m.corridor([(X(234), 27), (X(240), 27)], 3, 4, forge, y=FY)
    m.cave(X(258), FY + 6, 27, 16, 7, 14, lake, rough=0.12, seed=11, floor=FY)  # Зал горнил
    m.corridor([(X(274), 27), (X(280), 27)], 3, 4, throne, y=FY)
    m.round_room(X(296), 27, 16, FY, 11, throne, dome=5)                        # Тронный зал Владыки
    m.corridor([(X(296), 43), (X(296), 52)], 3, 4, throne, y=FY)
    m.round_room(X(296), 68, 15, FY, 12, lake, dome=6)                          # Кальдера
    m.room(X(293), 82, X(299), 87, FY, 6, throne)                               # ниша разлома
    m.shell(2)

    # ------------------------------------------------------------------ Кузнечный двор
    for (ax, az) in ((X(3), 22), (X(3), 32)):
        m.set(ax, FY, az, 'anvil', facing='north')
        m.set(ax + 1, FY, az, 'blast_furnace', facing='east', lit=True)
    m.set(X(8), FY, 27, 'campfire', facing='north', lit=True, signal_fire=False, waterlogged=False)
    m.sign(X(16), FY + 1, 22, 'west')
    s.sign((X(16), FY + 1, 22), ['ЭТАП 4', 'Кузни Пепла', 'уровни 30–40', ''])
    s.say((X(0), FY - 1, 20, X(16), FY + 8, 34), 'Система: «Здесь ковали оружие для армии, которой не стало. Горны так и не погасли.»')

    # ------------------------------------------------------------------ Литейный цех
    lava_channel(m, X(23), 26, X(57), 28, FY)
    for x in (X(30), X(40), X(50)):
        for z in range(26, 29):
            m.set(x, FY - 1, z, 'iron_block')
            m.clear(x, FY, z)
            m.set(x, FY, z, 'iron_trapdoor', facing='north', half='bottom', open=False, powered=False, waterlogged=False)
    for x in range(X(26), X(56), 6):
        for z in (19, 35):
            column(m, x, z, FY, m.ceil_y(x, FY, z) - FY + 1, 'polished_blackstone_bricks', 'basalt', 'magma_block')
    s.pack('p1', [('forge_construct', X(32), FY, 21, 31), ('ash_crossbow', X(34), FY, 33, 31), ('imp', X(30), FY, 32, 31),
                  ('imp', X(31), FY, 33, 31, None, 4)])
    s.pack('p2', [('molten_golem', X(44), FY, 22, 32), ('fire_shaman', X(46), FY, 34, 32), ('salamander', X(42), FY, 33, 32),
                  ('ash_crossbow', X(45), FY, 20, 32, None, 4)])
    s.pack('p3', [('imp', X(53), FY, 20, 32), ('imp', X(54), FY, 22, 32), ('imp', X(55), FY, 20, 32), ('imp', X(54), FY, 33, 32),
                  ('fire_shaman', X(56), FY, 34, 32), ('imp', X(53), FY, 34, 32, None, 5)])

    # ------------------------------------------------------------------ Печати горнов (по цене металла)
    metals = [('gold_block', X(68), 21), ('copper_block', X(76), 33), ('diamond_block', X(76), 21), ('iron_block', X(68), 33)]
    btn = {}
    for (blk, mx, mz) in metals:
        m.set(mx, FY, mz, blk)
        m.set(mx, FY + 1, mz, 'anvil', facing='north')
        bz = mz + (1 if mz < 27 else -1)
        m.set(mx, FY, bz, 'polished_blackstone_bricks')
        m.set(mx, FY + 1, bz, 'polished_blackstone_button', face='floor', facing='north', powered=False)
        btn[blk] = (mx, FY + 1, bz)
    order = [btn[b] for b in ('copper_block', 'iron_block', 'gold_block', 'diamond_block')]
    mech_seq = s.mech('metals', 'sequence', order, text='Печати горнов')
    m.sign(X(64), FY + 1, 24, 'east')
    s.sign((X(64), FY + 1, 24), ['Куй от дешёвого', 'к дорогому:', 'жми кнопки у', 'наковален.'])
    s.pack('p4', [('forge_construct', X(72), FY, 27, 33), ('ash_crossbow', X(78), FY, 29, 33), ('ash_crossbow', X(78), FY, 25, 33, None, 4)])
    s.gate('g1', (X(83), FY, 26, X(83), FY + 3, 28), open_when=[f'mech:{mech_seq}', 'pack:s4_p4'])

    # ------------------------------------------------------------------ Арена Плавильного голема: чаны с водой
    for (dx, dz) in ((9, 0), (-9, 0), (0, 9), (0, -9)):
        for ox in (0, 1):
            for oz in (0, 1):
                x, z = X(103 + dx + ox), 27 + dz + oz
                m.set(x, FY - 1, z, 'iron_block')
                m.set(x, FY, z, 'water', level=0)
        for ox in range(-1, 3):
            for oz in range(-1, 3):
                if ox in (-1, 2) or oz in (-1, 2):
                    m.set(X(103 + dx + ox), FY - 1, 27 + dz + oz, 'cut_copper')
    b1 = s.boss('smelter', 'smelter', (X(103), FY, 27), 16)
    s.gate('arena1', (X(85), FY, 26, X(85), FY + 3, 28), lock=b1)
    s.gate('g2', (X(122), FY, 26, X(122), FY + 3, 28), open_when=[f'boss:{b1}'])
    s.say((X(86), FY - 1, 11, X(119), FY + 10, 43), 'Система: «Плавильный голем. У него жарко — когда шкала жара полна, прыгайте в чан с водой.»')

    # ------------------------------------------------------------------ Цепной мост над лавовым озером
    for x in range(X(124), X(161)):
        for z in range(13, 42):
            if (x, FY, z) in m.open and not (25 <= z <= 29):
                m.set(x, FY - 1, z, 'magma_block')
                m.set(x, FY, z, 'lava', level=0)
    for x in range(X(124), X(161)):
        for z in range(25, 30):
            m.set(x, FY - 1, z, 'polished_blackstone_bricks' if z in (25, 29) else 'smooth_basalt')
        if x % 4 == 0:
            for z in (25, 29):
                m.set(x, FY, z, 'chain', axis='y', waterlogged=False)
                top = m.ceil_y(x, FY, z)
                for y in range(FY, min(top + 1, FY + 10)):
                    m.set(x, y, z, 'chain', axis='y', waterlogged=False)
    for i, x0 in enumerate((X(130), X(140), X(150))):
        s.mech(f'jets{i}', 'hazard', area=(x0, FY, 26, x0 + 5, FY, 28), period=2.4, warn=1.0, share=0.24, offset=i * 0.8, sound=1)
    s.pack('p5', [('salamander', X(158), FY, 26, 35), ('salamander', X(158), FY, 28, 35), ('imp', X(157), FY, 27, 35),
                  ('fire_shaman', X(159), FY, 27, 35, None, 4)])
    s.say((X(124), FY - 1, 20, X(130), FY + 10, 34), 'Система: «Цепной мост. Огонь бьёт из-под досок по очереди. Внизу — лава: Система вытащит, но дорого.»')

    # ------------------------------------------------------------------ Арсенал: Мастер-кузнец Горн и божественная секретка
    for z in range(16, 40, 4):
        for x in (X(168), X(188)):
            m.set(x, FY, z, 'smithing_table')
            m.set(x, FY + 1, z, 'iron_bars', north=False, south=False, east=False, west=False, waterlogged=False)
    s.pack('master', [('forge_construct', X(178), FY, 27, 36), ('ash_crossbow', X(174), FY, 22, 35), ('ash_crossbow', X(182), FY, 32, 35),
                      ('molten_golem', X(176), FY, 32, 35, None, 4)], title='Мастер-кузнец Горн')
    m.set(X(186), FY, 38, 'litrpg:locked_cache', facing='north')
    s.cache('arsenal', (X(186), FY, 38), 2)
    # три скрытых рычага открывают проход к божественному тайнику
    hidden = [(X(179), FY + 1, 47), (X(167), FY + 2, 15), (X(189), FY + 1, 39)]
    m.set(X(179), FY + 1, 48, 'deepslate_tiles')
    m.set(X(179), FY + 1, 47, 'lever', face='wall', facing='north', powered=False)
    m.set(X(166), FY + 2, 15, 'deepslate_tiles')
    m.set(X(167), FY + 2, 15, 'lever', face='wall', facing='east', powered=False)
    m.set(X(190), FY + 1, 39, 'deepslate_tiles')
    m.set(X(189), FY + 1, 39, 'lever', face='wall', facing='west', powered=False)
    mech_hidden = s.mech('hidden', 'levers', hidden, text='Скрытые рычаги арсенала')
    for y in range(FY, FY + 2):
        m.set(X(179), y, 41, 'infested_cobblestone')
    s.gate('vault', (X(191), FY, 6, X(191), FY + 2, 8), open_when=[f'mech:{mech_hidden}'])
    for y in range(FY, FY + 2):
        for x in (X(185), X(186)):
            m.set(x, y, 12, 'infested_cobblestone')
            m.set(x, y, 13, 'infested_cobblestone')
    m.set(X(196), FY, 7, 'litrpg:divine_cache', facing='west')
    s.cache('divine_forge', (X(196), FY, 7), 4)
    s.say((X(166), FY - 1, 14, X(190), FY + 8, 40), 'Система: «Арсенал. Мастер Горн держит ключ. А в стенах здесь кое-что спрятано — ищите булыжник среди кирпича.»')

    # ------------------------------------------------------------------ Ристалище Колесницы
    for i in range(12):
        a = i * math.pi / 6
        bx, bz = round(X(215) + 17 * math.cos(a)), round(27 + 17 * math.sin(a))
        m.set(bx, FY, bz, 'polished_blackstone_wall', up=True, north='none', south='none', east='none', west='none', waterlogged=False)
        m.set(bx, FY + 1, bz, 'campfire', facing='north', lit=True, signal_fire=False, waterlogged=False)
    b2 = s.boss('chariot', 'chariot', (X(215), FY, 27), 18)
    s.gate('arena2', (X(196), FY, 26, X(196), FY + 3, 28), lock=b2)
    s.gate('g3', (X(237), FY, 26, X(237), FY + 3, 28), open_when=[f'boss:{b2}'])
    s.say((X(196), FY - 1, 8, X(234), FY + 10, 46), 'Система: «Колесница Пламени. Уходите с полосы разгона. На пит-стопе — бейте все.»')

    # ------------------------------------------------------------------ Зал горнил: три острова в лаве
    for x in range(X(242), X(275)):
        for z in range(13, 42):
            if (x, FY, z) in m.open:
                m.set(x, FY - 1, z, 'magma_block')
                m.set(x, FY, z, 'lava', level=0)
    islands = [(X(250), 18), (X(266), 18), (X(258), 37)]
    paths = [((X(242), 27), (X(250), 18)), ((X(250), 18), (X(266), 18)), ((X(250), 18), (X(258), 37)), ((X(266), 18), (X(274), 27)),
             ((X(258), 37), (X(274), 27)), ((X(242), 27), (X(274), 27))]
    for (ax, az), (bx, bz) in paths:
        n = max(abs(bx - ax), abs(bz - az))
        for i in range(n + 1):
            x, z = round(ax + (bx - ax) * i / n), round(az + (bz - az) * i / n)
            for dx in (0, 1):
                for dz in (0, 1):
                    m.set(x + dx, FY - 1, z + dz, 'smooth_basalt')
                    if m.get(x + dx, FY, z + dz) and 'lava' in m.get(x + dx, FY, z + dz):
                        m.clear(x + dx, FY, z + dz)
    plates = []
    for (ix, iz) in islands:
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                if dx * dx + dz * dz <= 10:
                    m.set(ix + dx, FY - 1, iz + dz, 'polished_blackstone')
                    if m.get(ix + dx, FY, iz + dz) and 'lava' in m.get(ix + dx, FY, iz + dz):
                        m.clear(ix + dx, FY, iz + dz)
        m.set(ix, FY, iz, 'light_weighted_pressure_plate', power=0)
        plates.append((ix, FY, iz))
        brazier(m, ix + 2, FY, iz + 2, soul=False)
    mech_plates = s.mech('islands', 'plates', plates, text='Три горнила', hold=3)
    m.set(X(258), FY, 27, 'lectern', facing='west', has_book=False, powered=False)
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            m.set(X(258 + dx), FY - 1, 27 + dz, 'polished_blackstone')
            if m.get(X(258 + dx), FY, 27 + dz) and 'lava' in m.get(X(258 + dx), FY, 27 + dz):
                m.clear(X(258 + dx), FY, 27 + dz)
    mech_riddle = s.mech('riddle', 'riddle', area=(X(255), FY, 24, X(261), FY + 3, 30), answers=['огонь', 'пламя', 'костер', 'костёр'],
                         text='Загадка горнила|Чем больше её кормишь, тем она сильнее; дашь ей пить — умрёт.')
    m.sign(X(258), FY + 1, 30, 'north')
    s.sign((X(258), FY + 1, 30), ['Чем больше', 'кормишь — тем', 'сильнее; дашь', 'пить — умрёт.'])
    s.pack('p6', [('molten_golem', X(266), FY, 20, 37), ('fire_shaman', X(268), FY, 17, 37), ('imp', X(265), FY, 16, 37)])
    s.pack('p7', [('forge_construct', X(258), FY, 35, 38), ('salamander', X(256), FY, 38, 38), ('salamander', X(260), FY, 38, 38),
                  ('ash_crossbow', X(258), FY, 39, 38, None, 4)])
    s.gate('g4', (X(277), FY, 26, X(277), FY + 3, 28), open_when=[f'mech:{mech_plates}', f'mech:{mech_riddle}', 'pack:s4_p6', 'pack:s4_p7'])
    s.cache('crucible', (X(250), FY, 21), 1)
    m.set(X(250), FY, 21, 'litrpg:cache', facing='south')
    s.say((X(242), FY - 1, 12, X(274), FY + 10, 42), 'Система: «Три горнила на островах — по одному на каждого. И загадка у аналоя в середине.»')

    # ------------------------------------------------------------------ Тронный зал Владыки и Кальдера
    for i in range(8):
        a = i * math.pi / 4
        bx, bz = round(X(296) + 13 * math.cos(a)), round(27 + 13 * math.sin(a))
        column(m, bx, bz, FY, m.ceil_y(bx, FY, bz) - FY + 1, 'nether_bricks', 'gilded_blackstone', 'magma_block')
    for i in range(360):
        if 75 <= i <= 105 or 255 <= i <= 285:
            continue  # проходы: с севера (из тронного зала) и на юг (к разлому)
        a = math.radians(i)
        x, z = round(X(296) + 13.5 * math.cos(a)), round(68 + 13.5 * math.sin(a))
        if (x, FY, z) in m.open:
            m.set(x, FY - 1, z, 'magma_block')
            m.set(x, FY, z, 'lava', level=0)
    b3 = s.boss('ashlord', 'ash_lord', (X(296), FY, 27), 16, final=True, arena2=(X(296) + 0.5, FY, 68.5, 12))
    s.gate('arena3', (X(279), FY, 26, X(279), FY + 3, 28), lock=b3)
    s.gate('caldera', (X(295), FY, 48, X(297), FY + 3, 48), open_when=[f'boss:{b3}'])
    s.say((X(280), FY - 1, 11, X(312), FY + 11, 43), 'Система: «Владыка Пепла. Вызванный на дуэль — бейтесь один. Остальным — не мешать и лечить.»')
    for dx in (-2, 2):
        for dy in range(0, 4):
            m.set(X(296 + dx), FY + dy, 85, 'crying_obsidian')
    for dx in (-1, 0, 1):
        m.set(X(296 + dx), FY + 3, 85, 'crying_obsidian')
        for dy in range(0, 3):
            m.set(X(296 + dx), FY + dy, 85, 'litrpg:rift', active=False)
    apply_gates(m, s)

    m.hanging_lights(spacing=6, soul=False, themes=['forge', 'arsenal', 'ashthrone'], min_h=6, chain=2)
    m.glow(spacing=5, level=8, height=3)

    s.box = (X(-4), FY - 10, -4, X(316), FY + 26, 92)
    s.camp = (X(8.5), FY, 30.5, -90)
    s.trader = (X(5.5), FY, 24.5, -90)
    s.rift = (X(295), FY, 85, X(297), FY + 2, 85)
    return m, s
