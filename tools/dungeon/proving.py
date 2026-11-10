"""
Полигон вертикали — испытательная карта набора (обновление 14, пункт 9). В игру не входит: её строят
`tools/dungeon/test_vert.py` (проверка проходимости) и `python tools/gen_dungeon.py --proving` (вставить в map.bin
для осмотра в dev-клиенте: `execute in litrpg:dungeon run tp Dev 7174 64 26`).

Круг: Лагерь (ноги 64) → винтовая башня на 24 блока → Верхний зал над пропастью: верёвочный мост с провисом →
площадка на столбе из бездны → паркур (разрывы 1–2, прыжок вверх) → либо уступ вдоль южной стены →
шахта-спуск в бассейн Нижнего зала (64) → пузырьковый подъём (нырнуть под стекло) на антресоль (78) →
лестница в шахте с ловушкой ярусами → Галерея (92) → лоза-ягоды вниз до Зала разлома (64);
там же вьющаяся лоза на балкон и лоза по стене в нишу.
"""
from kit import Map, Stage, Theme

N = 7
OX = 1024 * N
G0 = 64


def X(x):
    return OX + x


def build(_n=N, ladder=True):
    m = Map(seed=7007)
    hall = m.theme(Theme('proving', wall=[('stone_bricks', 5), ('cracked_stone_bricks', 1), ('tuff_bricks', 2)],
                         floor=[('polished_andesite', 3), ('stone_bricks', 2)], ceil='stone_bricks', seed=71))
    deep = m.theme(Theme('proving_deep', wall=[('deepslate_bricks', 4), ('cracked_deepslate_bricks', 1)],
                         floor=[('polished_deepslate', 3), ('deepslate_tiles', 1)], ceil='deepslate_bricks', seed=72))
    s = Stage(N, 'proving', 'Полигон вертикали', (1, 50), 'crypt')

    # ---- Лагерь и винтовая башня (64 → 88)
    m.room(X(0), 20, X(12), 32, G0, 6, hall, vault=2)
    m.corridor([(X(12), 26), (X(16), 26)], 3, 4, hall, y=G0)
    top = m.spiral(X(20) + 0.5, 26.5, G0, G0 + 24, 4, hall, step='polished_andesite', core='chiseled_stone_bricks',
                   per_turn=12, start=0.0, rail='andesite_wall', underside='stone_brick_stairs')
    m.corridor([(X(24), 26, G0 + 24), (X(32), 26, G0 + 24)], 3, 4, hall)

    # ---- Верхний зал над пропастью (88)
    UY = G0 + 24
    m.room(X(32), 14, X(80), 40, UY, 8, deep, vault=3)
    m.pit(X(40), 14, X(72), 40, UY, 40, deep)
    m.bridge((X(39), 20), (X(57), 20), UY, 'spruce_planks', width=3, rail='spruce_fence', posts='stripped_spruce_log',
             post_every=6, sag=1, under='spruce_log')
    m.platform(X(60) + 0.5, 20.5, UY, 3, 'polished_deepslate', rim='deepslate_tiles', support='pillar',
               support_block='deepslate_bricks', chains=True)
    m.parkour([(X(65), UY, 20), (X(68), UY, 20), (X(71), UY + 1, 20)], 'deepslate_tiles', pillar='deepslate_brick_wall')
    m.ledge([(X(39), 40), (X(52), 40)], UY, 'stone_bricks', width=2, corbel='stone_brick_stairs')
    m.ledge([(X(56), 40), (X(73), 40)], UY, 'stone_bricks', width=2, corbel='stone_brick_stairs')  # разрыв 3 — прыжок

    # ---- Шахта-спуск в Нижний зал (88 → 64)
    m.room(X(74), 8, X(90), 44, G0, 7, deep, vault=2)
    m.dropper(X(76), 25, X(77), 27, UY, G0, deep, depth=3)

    # ---- Пузырьковый подъём на антресоль (64 → 78)
    MY = G0 + 14
    m.room(X(92), 20, X(104), 32, MY, 5, hall)
    m.bubble_lift(X(91), 26, G0, MY - 1, 'west', 'east', hall, glass='glass')

    # ---- Лестница в шахте с ловушкой ярусами (78 → 92), Галерея
    RY = G0 + 28
    m.carve(X(99), MY, 33, X(100), RY + 2, 34, hall)
    if ladder:
        m.ladder(X(99), 34, MY, RY - 1, 'north')
    m.room(X(94), 35, X(106), 44, RY, 5, hall)
    s.climb_trap('ladder_trap', (X(99), MY + 1, 33, X(100), RY - 2, 34), period=3.0, warn=1.0, share=0.15, layer=2)

    # ---- Лоза-ягоды вниз в Зал разлома (92 → 64)
    m.room(X(107), 30, X(124), 48, G0, 10, deep, vault=2)
    m.carve(X(107), G0, 38, X(108), RY + 3, 39, deep)
    m.vines(X(107), 38, G0 + 1, RY + 3, kind='cave', berries=0.5)
    # вьющаяся лоза на балкон
    m.platform(X(120) + 0.5, 44.5, G0 + 6, 2, 'polished_deepslate', rim='deepslate_tiles', chains=True)
    m.vines(X(117), 44, G0, G0 + 5, kind='twisting')
    # лоза по стене в нишу
    m.room(X(110), 49, X(114), 52, G0 + 6, 3, deep)
    m.vines(X(112), 48, G0, G0 + 5, kind='vine', facing='north')

    m.shell(2)
    m.hanging_lights(spacing=8, soul=False, min_h=6)
    m.glow(spacing=5, level=11)

    # ---- схема
    s.box = (X(-4), G0 - 4, 0, X(130), G0 + 40, 60)
    s.camp = (X(6.5), G0, 26.5, -90)
    s.trader = (X(3.5), G0, 22.5, 0)
    s.rift = (X(115), G0, 36, X(116), G0 + 2, 36)
    s.lore.append({'box': [X(0), G0, 20, X(12), G0 + 5, 32], 'text': 'Система: «Полигон вертикали.»'})
    marks = {'spiral_top': top['top'], 'balcony': (X(120), G0 + 6, 44), 'niche': (X(112), G0 + 6, 50),
             'mezzanine': (X(98), MY, 26), 'gallery': (X(100), RY, 40), 'lower': (X(84), G0, 30),
             'platform': (X(60), UY, 20), 'far_side': (X(76), UY, 20), 'ledge_end': (X(70), UY, 39)}
    return m, s, marks
