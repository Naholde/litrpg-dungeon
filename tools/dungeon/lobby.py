"""Преддверие: круглый зал с Печатью в центре, ниши ролей, зал прибытия с правилами и запертые Врата."""
import math

from kit import Map, Theme

CX, CZ, FY = 32, 32, 64


def build():
    m = Map(seed=11)
    hall = m.theme(Theme('lobby', wall=[('deepslate_bricks', 6), ('cracked_deepslate_bricks', 1), ('deepslate_tiles', 2)],
                         floor=[('polished_deepslate', 4), ('deepslate_tiles', 2)], ceil='deepslate_bricks', seed=3))
    shrine = m.theme(Theme('shrine', wall=[('polished_blackstone_bricks', 5), ('cracked_polished_blackstone_bricks', 1)],
                           floor='polished_blackstone', ceil='blackstone', seed=5))

    # главный зал, зал прибытия (юг), ниша Врат (север), четыре ниши по диагоналям
    m.round_room(CX, CZ, 15, FY, 9, hall, dome=6)
    m.room(26, 48, 38, 58, FY, 6, hall, vault=2)
    m.corridor([(CX, 44), (CX, 49)], 5, 5, hall, y=FY)
    m.carve(27, FY, 9, 37, FY + 11, 18, hall)
    niches = {}
    for name, (sx, sz) in {'tank': (1, -1), 'damage': (-1, -1), 'support': (1, 1), 'rules': (-1, 1)}.items():
        nx, nz = round(CX + sx * 12.5), round(CZ + sz * 12.5)
        m.carve(nx - 2, FY, nz - 2, nx + 2, FY + 4, nz + 2, shrine)
        m.carve(nx - 2 + sx * 2, FY, nz - 2 + sz * 2, nx + 2 + sx * 2, FY + 4, nz + 2 + sz * 2, shrine)
        niches[name] = (nx + sx * 2, nz + sz * 2, sx, sz)
    m.shell(2)

    # пол: круг-узор вокруг Печати
    for x in range(CX - 15, CX + 16):
        for z in range(CZ - 15, CZ + 16):
            d = math.hypot(x - CX, z - CZ)
            if 6.5 <= d < 7.5:
                m.set(x, FY - 1, z, 'polished_blackstone_bricks')
            elif d < 6.5 and (x == CX or z == CZ):
                m.set(x, FY - 1, z, 'chiseled_polished_blackstone')
            elif 10.5 <= d < 11.2:
                m.set(x, FY - 1, z, 'gilded_blackstone' if (x + z) % 4 == 0 else 'polished_blackstone')

    # возвышение и Печать
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if abs(dx) == 2 or abs(dz) == 2:
                m.slab(CX + dx, FY, CZ + dz, 'polished_blackstone_brick_slab')
            elif (dx, dz) != (0, 0):
                m.set(CX + dx, FY, CZ + dz, 'polished_blackstone_bricks')
    m.set(CX, FY, CZ, 'chiseled_polished_blackstone')
    m.set(CX, FY + 1, CZ, 'litrpg:seal')
    for dx, dz in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
        m.set(CX + dx, FY, CZ + dz, 'polished_blackstone_brick_wall', up=True, north='none', south='none', east='none',
              west='none', waterlogged=False)
        m.candle(CX + dx, FY + 1, CZ + dz, 4, 'purple')

    # колонны по кругу
    for i in range(8):
        a = i * math.pi / 4 + math.pi / 8
        px, pz = round(CX + 12.5 * math.sin(a)), round(CZ + 12.5 * math.cos(a))
        top = m.ceil_y(px, FY, pz)
        m.pillar(px, pz, FY, top - FY + 1, 'deepslate_tiles', cap='chiseled_deepslate', base_='polished_deepslate')

    # Врата подземелья — арка и печать, которая не открывается (в подземелье ведёт Печать)
    for x in range(27, 38):
        for y in range(FY, FY + 12):
            if x in (27, 37) or y >= FY + 10:
                m.set(x, y, 10, 'chiseled_deepslate' if (y - FY) % 3 == 0 else 'polished_deepslate')
            else:
                m.set(x, y, 10, 'litrpg:sealed_gate')
    for x in (28, 36):
        m.set(x, FY, 12, 'soul_campfire', facing='south', lit=True, signal_fire=False, waterlogged=False)

    # ниши ролей: цвет стекла и свечей, знамя
    colors = {'tank': 'blue', 'damage': 'red', 'support': 'lime', 'rules': 'white'}
    signs = []
    texts = {'tank': ['ТАНК', 'держит врагов', 'на себе:', 'провокация'],
             'damage': ['БОЕЦ', 'наносит урон', 'и не стоит', 'в разметке'],
             'support': ['ПОДДЕРЖКА', 'лечит отряд:', 'Ctrl+1…5 —', 'выбор цели'],
             'rules': ['ПРАВИЛА', 'Пал — дух.', 'Все пали —', 'этап заново']}
    for name, (nx, nz, sx, sz) in niches.items():
        col = colors[name]
        bx, bz = nx + sx * 3, nz + sz * 3
        for dy in range(0, 4):
            for k in range(-1, 2):
                m.set(bx, FY + dy, nz + k, f'{col}_stained_glass')
                m.set(nx + k, FY + dy, bz, f'{col}_stained_glass')
        m.set(nx + sx * 2, FY, nz + sz * 2, 'polished_blackstone_bricks')
        m.candle(nx + sx * 2, FY + 1, nz + sz * 2, 3, col)
        m.set(nx, FY + 4, nz, 'light', level=12, waterlogged=False)
        # табличка на стене ниши, обращена к центру зала
        sxp, szp = nx + sx * 2, nz
        facing = 'west' if sx > 0 else 'east'
        m.sign(sxp, FY + 1, szp, facing)
        signs.append({'at': [sxp, FY + 1, szp], 'lines': texts[name]})

    # зал прибытия: правила на стенах
    rules = [
        (26, 50, 'east', ['ПРЕДДВЕРИЕ', 'Выберите роль', 'и соберите', 'отряд 3–5']),
        (26, 54, 'east', ['ПЕЧАТЬ', 'в центре зала:', 'ПКМ — начать', 'прохождение']),
        (38, 50, 'west', ['ДУХ', 'Павшего воскре-', 'шает союзник:', 'Shift рядом']),
        (38, 54, 'west', ['ВАЙП', 'Все пали — этап', 'заново. Опыт и', 'вещи остаются']),
    ]
    for x, z, f, lines in rules:
        m.sign(x, FY + 1, z, f)
        signs.append({'at': [x, FY + 1, z], 'lines': lines})
    for x in (26, 38):
        for z in (52, 56):
            m.wall_torch(x, FY + 3, z, 'east' if x == 26 else 'west', soul=True)

    m.hanging_lights(spacing=5, soul=True, themes=['lobby'], min_h=7, chain=3)
    m.glow(spacing=4, level=11, themes=['lobby', 'shrine'], height=3)
    # над Печатью — столб света
    for y in range(FY + 3, FY + 9):
        if (CX, y, CZ) in m.open:
            m.set(CX, y, CZ, 'light', level=15, waterlogged=False)

    lobby = {'box': [0, FY - 4, 0, 64, FY + 24, 64], 'spawn': [CX + 0.5, FY, 55.5, 180], 'seal': [CX, FY + 1, CZ],
             'signs': signs}
    return m, lobby
