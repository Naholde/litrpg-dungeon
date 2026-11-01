"""
Черновые этапы для проверки прохождения (фаза 7): привал с торговцем → зал с двумя отрядами → печать →
арена хранителя (двери запираются на время боя) → разлом. Настоящие карты этапов заменяют их по одному (фаза 8).
"""
from kit import Map, Stage, Theme, apply_gates

FY = 64
STYLE = {
    1: ('crypt', 'Склеп Забытых', [('stone_bricks', 5), ('cracked_stone_bricks', 2), ('mossy_stone_bricks', 1)], 'polished_andesite'),
    2: ('thicket', 'Паучья чаща', [('mud_bricks', 4), ('packed_mud', 2), ('moss_block', 1)], 'moss_block'),
    3: ('temple', 'Затопленный храм', [('prismarine_bricks', 4), ('prismarine', 2), ('dark_prismarine', 1)], 'dark_prismarine'),
    4: ('forge', 'Кузни Пепла', [('polished_blackstone_bricks', 4), ('basalt', 2), ('blackstone', 1)], 'smooth_basalt'),
    5: ('spire', 'Шпиль Бездны', [('purpur_block', 3), ('end_stone_bricks', 3), ('obsidian', 1)], 'end_stone_bricks'),
}


def build(n):
    key, name, wall, floor = STYLE[n]
    ox = 1024 * n
    lv = (n - 1) * 10
    m = Map(seed=100 + n)
    t = m.theme(Theme(key, wall=wall, floor=floor, ceil=wall, seed=n))
    s = Stage(n, key, name, (max(1, lv), lv + 10), key)

    m.room(ox + 6, 6, ox + 20, 20, FY, 7, t, vault=2)                 # привал
    m.corridor([(ox + 20, 13), (ox + 34, 13)], 3, 4, t, y=FY)
    m.room(ox + 34, 1, ox + 58, 25, FY, 8, t, vault=3)                # зал отрядов
    m.corridor([(ox + 58, 13), (ox + 79, 13)], 3, 4, t, y=FY)
    m.round_room(ox + 92, 13, 14, FY, 10, t, dome=4)                  # арена
    m.shell(2)
    m.hanging_lights(spacing=6, soul=n % 2 == 1, min_h=6)
    m.glow(spacing=4, level=10, height=3)

    a = lambda lvl: max(1, lv + lvl)
    p1 = s.pack('p1', [('crypt_warrior', ox + 44, FY, 8, a(2)), ('crypt_warrior', ox + 46, FY, 10, a(2)),
                       ('crypt_archer', ox + 50, FY, 6, a(2)), ('bone_guard', ox + 45, FY, 6, a(3), 'elite', 4)])
    p2 = s.pack('p2', [('bone_rat', ox + 44, FY, 19, a(3)), ('bone_rat', ox + 45, FY, 20, a(3)),
                       ('bone_rat', ox + 46, FY, 18, a(3)), ('acolyte', ox + 50, FY, 20, a(3)),
                       ('bone_rat', ox + 47, FY, 21, a(3), None, 5)])
    s.gate('g1', (ox + 62, FY, 12, ox + 62, FY + 3, 14), open_when=[f'pack:{p1}', f'pack:{p2}'])
    b1 = s.boss('b1', 'trial_warden', (ox + 92, FY, 13), 14, final=True)
    s.gate('arena', (ox + 76, FY, 12, ox + 76, FY + 3, 14), lock=b1)
    apply_gates(m, s)

    m.set(ox + 8, FY - 1, 13, 'chiseled_stone_bricks')
    for dz in (-1, 0, 1):
        m.set(ox + 103, FY - 1, 13 + dz, 'crying_obsidian')
        m.set(ox + 103, FY + 3, 13 + dz, 'crying_obsidian')
    for dy in range(0, 3):
        m.set(ox + 103, FY + dy, 11, 'crying_obsidian')
        m.set(ox + 103, FY + dy, 15, 'crying_obsidian')
        for dz in (-1, 0, 1):
            m.set(ox + 103, FY + dy, 13 + dz, 'litrpg:rift', active=False)

    s.box = (ox, FY - 8, -4, ox + 112, FY + 24, 32)
    s.camp = (ox + 12.5, FY, 13.5, -90)
    s.trader = (ox + 8.5, FY, 13.5, -90)
    s.rift = (ox + 103, FY, 12, ox + 103, FY + 2, 14)
    return m, s
