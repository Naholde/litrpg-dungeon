"""Этап 1 · Склеп Забытых — модели хранителей и нежити."""
from hy import Model, mat, kf, loop, once, sway, beats, mul, rgb, mix

# ---------------------------------------------------------------------------- материалы склепа
mat("gk_bone", "#E6DCC0", "bone", dark="#8E7F60", light="#FFF8E4")
mat("gk_bone_shadow", "#B4A685", "bone", dark="#6E6048", light="#D8CCAE")
mat("gk_iron", "#545A63", "metal", dark="#2A2D33", light="#9AA4B0", gloss=0.3)
mat("gk_rust", "#7E4A2C", "rust", dark="#42251A", light="#B8723E")
mat("gk_cloth", "#3A3236", "cloth", dark="#1C171A", light="#5E5056")
mat("gk_soul", "#6FE6FF", "glow", glow=True, light="#E8FFFF", dark="#1E9ACC")
mat("gk_horn", "#5A5048", "bone", dark="#2E2822", light="#8A7E70", scale=0.7)


# ---------------------------------------------------------------------------- декали

def skull_face(f):
    """Свод черепа спереди: глубокие угловатые глазницы в нижней половине, синий огонь, тяжёлое надбровье."""
    w, h = f.w, f.h
    ey, eh = int(h * 0.42), int(h * 0.50)
    ew = int(w * 0.31)
    f.shade(0, ey - 3, w, ey - 1, 1.12)                           # надбровный валик
    f.shade(0, ey - 1, w, ey, 0.7)
    for side, sx in ((0, int(w * 0.10)), (1, w - int(w * 0.10) - ew)):
        for y in range(eh):
            slant = int((eh * 0.55 - y) * 0.7) if y < eh * 0.55 else 0
            # скос у переносицы: внутренние верхние углы ниже — злой взгляд
            x0 = sx + (slant if side == 1 else 0)
            x1 = sx + ew - (slant if side == 0 else 0)
            r = 1 if y >= eh - 2 else 0
            f.rect(x0 + r, ey + y, x1 - r, ey + y + 1, "#0C080E")
        cx, cy = sx + ew // 2 + (1 if side == 0 else -1), ey + int(eh * 0.62)
        f.rect(cx - 1, cy - 1, cx + 2, cy + 1, "#3FC8F0", glow=True)
        f.set(cx, cy - 1, "#FFFFFF", glow=True)
        f.set(cx, cy - 2, "#8AF0FF", glow=True)
        f.set(cx + (1 if side == 0 else -1), cy - 3, "#2A9AD0", glow=True)
    f.line(w // 2, 1, w // 2 + 2, int(h * 0.3), "#8E7F60")         # трещина по своду


def maxilla_face(f):
    """Верхняя челюсть: провал носа и верхний ряд зубов."""
    w, h = f.w, f.h
    nx = w // 2
    for i in range(3):
        half = 2 - i // 2
        f.rect(nx - half, i, nx + half, i + 1, "#140E12")
    f.rect(0, h - 3, w, h, "#1A1216")
    for x in range(1, w - 1, 2):
        f.rect(x, h - 3, x + 1, h - 1, "#F2EAD0")
        f.set(x, h - 1, "#BFB08E")
    f.shade(0, 3, 2, h - 3, 0.8)
    f.shade(w - 2, 3, w, h - 3, 0.8)


def skull_side(f):
    f.shade(0, int(f.h * 0.6), f.w, f.h, 0.85)
    f.line(int(f.w * 0.2), int(f.h * 0.3), int(f.w * 0.7), int(f.h * 0.55), "#8E7F60")


def jaw_front(f):
    """Челюсть: ряд зубов, пара сломана."""
    w, h = f.w, f.h
    f.rect(0, 0, w, 2, "#1A1418")
    for x in range(1, w - 1, 2):
        if x in (5, 6):
            continue
        f.rect(x, 0, x + 1, 3, "#F4ECD2")
        f.set(x, 3, "#C8BA98")


def ribs_front(f):
    """Грудина с выщерблинами."""
    for y in range(2, f.h, 5):
        f.shade(0, y, f.w, y + 1, 0.7)


def rivets(f, step=6, col="#B8C2CE"):
    for x in range(2, f.w - 1, step):
        for y in (1, f.h - 2):
            f.set(x, y, col)
            f.set(x + 1, y + 1, mul(rgb(col), 0.55))


def plate_rows(f):
    for y in range(0, f.h, 5):
        f.shade(0, y, f.w, y + 1, 0.55)
        f.shade(0, y + 1, f.w, y + 2, 1.25)
    rivets(f)


def tatter(f):
    """Рваный нижний край ткани."""
    import hy
    for x in range(f.w):
        cut = int(hy.h32(x, 77) * 5) + (2 if x % 7 in (2, 3) else 0)
        for y in range(f.h - cut, f.h):
            f.cut(x, y)
    f.shade(0, 0, f.w, 3, 0.7)


def shackle(f):
    f.shade(0, 0, f.w, 1, 1.3)
    f.shade(0, f.h - 1, f.w, f.h, 0.6)
    rivets(f, 5)


# ---------------------------------------------------------------------------- Привратник Склепа

def gatekeeper():
    """Привратник Склепа: исполинский скелет-тюремщик. Ребра — клетка, в ней горит синий огонь души.
    Железный ошейник с обрывком цепи, ржавый наплечник, кандалы, цепь с шипастой гирей в правой руке."""
    m = Model("gatekeeper")
    B, S = "gk_bone", "gk_bone_shadow"

    # --- ноги (кость бедра, колено, голень, стопа, поножи)
    for side, sx in (("right", -1), ("left", 1)):
        leg = m.bone(f"{side}_leg", [3.2 * sx, 17, 0])
        x = 3.2 * sx
        m.box(leg, [x, 13, 0], [3.2, 8, 3.2], B)                          # бедро
        m.box(leg, [x, 17.2, 0], [4.2, 2.4, 4], S)                        # головка бедра
        knee = m.bone(f"{side}_shin", [x, 9, -0.4], leg)
        m.box(knee, [x, 9.3, -0.6], [4.2, 3, 4.2], B)                   # колено
        m.box(knee, [x - 0.5 * sx, 4.6, 0], [2.4, 8, 2.4], B)             # большая берцовая
        m.box(knee, [x + 1.1 * sx, 4.8, 0.4], [1.4, 7, 1.4], S)           # малая берцовая
        m.box(knee, [x, 3.4, 0], [4.8, 3.4, 4.8], "gk_iron", inflate=0.05, decal={"north": shackle, "south": shackle})
        m.box(knee, [x, 0.9, -1.4], [5, 1.8, 7], B)                   # стопа
        for i, dx in enumerate((-1.3, 0, 1.3)):
            m.box(knee, [x + dx * 1.2, 0.6, -5.4], [1.3, 1.2, 1.8], S)          # пальцы

    # --- таз и позвоночник
    body = m.bone("body", [0, 18, 0])
    m.box(body, [0, 18.6, 0.2], [11, 3.4, 6.4], B)                              # таз
    m.box(body, [0, 17.4, -0.2], [6, 1.6, 4], S)
    for i in range(4):
        m.box(body, [0, 20.8 + i * 1.6, 1.8], [3, 1.3, 3], B)          # позвонки поясницы
    # набедренная повязка спереди и сзади (свои кости — колышутся)
    cf = m.bone("cloth_front", [0, 19.5, -3.3], body)
    m.box(cf, [0, 15.2, -3.4], [7.5, 8.6, 0.5], "gk_cloth", decal={"north": tatter, "south": tatter})
    cb = m.bone("cloth_back", [0, 19.5, 3.6], body)
    m.box(cb, [0, 14.6, 3.7], [9.4, 10, 0.5], "gk_cloth", decal={"north": tatter, "south": tatter})
    m.box(body, [0, 19.8, 0.2], [12, 1.6, 7.4], "gk_iron", inflate=0.02, decal={"north": rivets, "south": rivets})  # пояс

    # --- грудная клетка
    chest = m.bone("chest", [0, 26, 1], body)
    m.box(chest, [0, 30, 3.2], [3.6, 11, 3], B)                            # грудной отдел позвоночника
    m.box(chest, [0, 30.5, -4.8], [2.6, 9, 1.6], B, decal={"north": ribs_front})  # грудина
    for i in range(5):
        y = 34 - i * 2.3
        half = 7.6 - abs(i - 1.2) * 0.6
        m.mirror(chest, [half - 1.6, y, -4.4], [1.6, 1.5, 7.6], B)          # бока ребра
        m.mirror(chest, [1.4, y, -5.4], [half - 2.6, 1.5, 1.6], S if i % 2 else B)  # передние дуги
        m.mirror(chest, [1.6, y, 3.2], [half - 3.0, 1.5, 1.2], S)          # задние дуги
    # душа в клетке
    soul = m.bone("soul", [0, 29, -0.6], chest)
    m.box(soul, [0, 29, -0.6], [5, 5, 5], "gk_soul")
    m.box(soul, [0, 29, -0.6], [3, 7.4, 3], "gk_soul")
    # ключицы и лопатки
    m.mirror(chest, [0.8, 35.4, -4.2], [7.6, 1.8, 2], B)
    m.mirror(chest, [2.2, 30.6, 3.6], [5, 5.4, 1], S)
    # ошейник и обрывок цепи
    m.box(chest, [0, 36.6, -0.4], [10, 2.8, 10], "gk_iron", inflate=0.1, decal={"north": plate_rows, "east": rivets, "west": rivets})
    for i in range(3):
        m.box(chest, [0.6 * (i % 2), 34.6 - i * 1.9, -5.8], [1.6, 2, 1] if i % 2 else [1, 2, 1.6], "gk_iron")
    m.box(chest, [-3, 34.6, -5.6], [2, 3, 0.8], "gk_iron")          # замок ошейника

    # --- голова: череп, рога, челюсть
    head = m.bone("head", [0, 37.5, -0.5], chest)
    m.box(head, [0, 44.4, -0.6], [11.4, 8, 10.8], B, decal={"north": skull_face, "east": skull_side, "west": skull_side})
    m.box(head, [0, 48.8, -0.2], [9.4, 1.4, 9.4], B)                       # свод
    m.box(head, [0, 41, -2.2], [8, 3.4, 7.6], S, decal={"north": maxilla_face})   # верхняя челюсть
    m.mirror(head, [3.6, 40.6, -5.6], [2.4, 2.8, 4], B)                    # скулы
    m.box(head, [0, 42.4, 4.9], [8.6, 5.4, 1.4], S)                        # затылок
    for side, sx in (("r", -1), ("l", 1)):
        m.box(head, [6.2 * sx, 46.6, 0.2], [2.4, 3.2, 3.2], "gk_horn")
        m.segments(f"horn_{side}", head, [7.0 * sx, 46.8, 0.4],
                   [(3.4, 3.0, [0, 0, 70 * sx]), (3.0, 2.6, [0, 0, -30 * sx]), (2.8, 2.1, [-16, 0, -30 * sx]),
                    (2.4, 1.6, [-20, 0, -22 * sx]), (1.8, 1.1, [-18, 0, -12 * sx])],
                   ["gk_horn", "gk_horn", "gk_horn", "gk_bone_shadow", "gk_bone"])
    jaw = m.bone("jaw", [0, 39.6, 2], head)
    m.box(jaw, [0, 38.2, -2.0], [8.4, 2.4, 7.6], B, decal={"north": jaw_front})
    m.mirror(jaw, [3.6, 38.4, 1.2], [1.2, 3.6, 2.4], S)

    # --- плечи и руки
    for side, sx in (("right", -1), ("left", 1)):
        arm = m.bone(f"{side}_arm", [8.6 * sx, 34.5, 0], chest)
        x = 9.8 * sx
        m.box(arm, [x, 34.2, 0], [4.6, 4.2, 4.6], S)                       # плечевой сустав
        m.box(arm, [x + 0.3 * sx, 28.6, 0], [3.2, 9, 3.2], B)              # плечевая кость
        fore = m.bone(f"{side}_forearm", [x + 0.3 * sx, 24.4, 0], arm)
        m.box(fore, [x + 0.3 * sx, 24.4, 0], [3.8, 3, 3.8], B)               # локоть
        m.box(fore, [x - 0.4 * sx, 19.8, -0.6], [2, 8, 2], B)          # лучевая
        m.box(fore, [x + 1.2 * sx, 19.8, 0.6], [1.6, 8, 1.6], S)           # локтевая
        m.box(fore, [x + 0.3 * sx, 17.4, 0], [5.4, 3, 5.4], "gk_iron", inflate=0.05,
              decal={"north": shackle, "south": shackle, "east": shackle, "west": shackle})   # кандалы
        hand = m.bone(f"{side}_hand", [x + 0.3 * sx, 15.6, 0], fore)
        m.box(hand, [x + 0.3 * sx, 14.2, 0], [4.6, 3.4, 4.6], B)
        for i, dz in enumerate((-1.3, 0, 1.3)):
            m.box(hand, [x + 0.3 * sx, 11.4, dz * 1.2], [1.4, 3.2, 1.4], S if i == 1 else B, rot=[0, 0, 8 * sx])
        m.box(hand, [x - 2.2 * sx, 13, -1.4], [1.4, 2.4, 1.4], B)              # большой палец
    # наплечник на левом плече: три ржавых пластины
    pa = m.bone("pauldron", [9.8, 37, 0], "left_arm")
    for i, (w, y, d) in enumerate(((8.2, 37.4, 8.0), (7.4, 35.4, 7.4), (6.4, 33.6, 6.8))):
        m.box(pa, [10.8 + i * 0.5, y + 0.4, 0], [w * 0.8, 2.4, d * 1.15], "gk_rust", inflate=0.02,
              decal={"north": plate_rows, "south": plate_rows, "up": rivets}, rot=[0, 0, -14 - i * 4])
    m.box(pa, [10.2, 40.4, 0], [2, 3.2, 2], "gk_iron")                 # шип

    # --- цепь с гирей в правой руке: звенья — свои кости, раскачиваются
    prev, y = "right_hand", 13
    for i in range(3):
        link = m.bone(f"chain{i}", [-9.5, y, 0], prev)
        size = [0.8, 2.6, 1.8] if i % 2 == 0 else [1.8, 2.6, 0.8]
        m.box(link, [-9.5, y - 1.3, 0], [v * 1.2 for v in size], "gk_iron")
        prev, y = link, y - 2.2
    ball = m.bone("weight", [-9.5, y, 0], prev)
    c = [-9.5, y - 3.8, 0]
    m.box(ball, c, [7.4, 5.2, 5.2], "gk_iron", decal={"north": rivets})
    m.box(ball, c, [5.2, 7.4, 5.2], "gk_iron")
    m.box(ball, c, [5.2, 5.2, 7.4], "gk_iron", decal={"north": rivets})
    m.box(ball, [c[0], c[1] + 0.2, c[2]], [7.8, 1.4, 7.8], "gk_rust")
    for dx, dy, dz in ((4.2, 0, 0), (-4.2, 0, 0), (0, 0, 4.2), (0, 0, -4.2), (0, -4.2, 0), (2.8, 2.8, 2.8), (-2.8, 2.8, -2.8)):
        m.box(ball, [c[0] + dx, c[1] + dy, c[2] + dz], [1.4, 1.4, 1.4], "steel")
    anims_gatekeeper(m)
    return m


def anims_gatekeeper(m):
    z = [0, 0, 0]
    # покой: тяжёлое дыхание грудной клетки, душа пульсирует, цепь едва качается
    m.anim("idle", loop(3.2, {
        "chest": {"rotation": sway(3.2, 2.5, 0, 0), "position": sway(3.2, 0.25, 0.25, 1)},
        "head": {"rotation": kf([(0, z), (0.8, [4, 10, 0]), (1.6, [-2, 0, 0]), (2.4, [4, -12, 0]), (3.2, z)])},
        "jaw": {"rotation": sway(3.2, 4, 0.1, 0, [6, 0, 0])},
        "soul": {"scale": kf([(0, [1, 1, 1]), (0.8, [1.15, 1.2, 1.15]), (1.6, [0.92, 0.9, 0.92]), (2.4, [1.1, 1.1, 1.1]), (3.2, [1, 1, 1])]),
                 "rotation": kf([(0, z), (1.6, [0, 180, 0]), (3.2, [0, 360, 0])])},
        "right_arm": {"rotation": sway(3.2, 3, 0.3, 2, [0, 0, 4])},
        "left_arm": {"rotation": sway(3.2, 3, 0.55, 2, [0, 0, -4])},
        "right_forearm": {"rotation": [-14, 0, 0]}, "left_forearm": {"rotation": [-10, 0, 0]},
        "chain0": {"rotation": sway(3.2, 5, 0.2, 0)}, "chain1": {"rotation": sway(3.2, 4, 0.35, 0)},
        "chain2": {"rotation": sway(3.2, 3, 0.5, 0)}, "weight": {"rotation": sway(3.2, 6, 0.6, 1)},
        "cloth_front": {"rotation": sway(3.2, 4, 0.1, 0, [-3, 0, 0])}, "cloth_back": {"rotation": sway(3.2, 4, 0.4, 0, [4, 0, 0])},
    }))
    # шаг: тяжёлый, с перекатом корпуса и мотанием цепи
    m.anim("walk", loop(1.4, {
        "right_leg": {"rotation": sway(1.4, 26, 0, 0)}, "left_leg": {"rotation": sway(1.4, 26, 0.5, 0)},
        "right_shin": {"rotation": kf([(0, [10, 0, 0]), (0.35, [32, 0, 0]), (0.7, [4, 0, 0]), (1.05, [6, 0, 0]), (1.4, [10, 0, 0])])},
        "left_shin": {"rotation": kf([(0, [4, 0, 0]), (0.35, [6, 0, 0]), (0.7, [10, 0, 0]), (1.05, [32, 0, 0]), (1.4, [4, 0, 0])])},
        "body": {"position": kf([(0, z), (0.35, [0, -0.8, 0]), (0.7, z), (1.05, [0, -0.8, 0]), (1.4, z)]),
                 "rotation": sway(1.4, 4, 0, 2)},
        "chest": {"rotation": sway(1.4, 5, 0.25, 1, [8, 0, 0])},
        "right_arm": {"rotation": sway(1.4, 18, 0.5, 0)}, "left_arm": {"rotation": sway(1.4, 18, 0, 0)},
        "right_forearm": {"rotation": [-20, 0, 0]}, "left_forearm": {"rotation": [-16, 0, 0]},
        "chain0": {"rotation": sway(1.4, 14, 0.7, 0)}, "chain2": {"rotation": sway(1.4, 12, 0.85, 0)},
        "weight": {"rotation": sway(1.4, 18, 0.9, 0)},
        "cloth_front": {"rotation": sway(1.4, 10, 0.1, 0)}, "cloth_back": {"rotation": sway(1.4, 10, 0.6, 0, [6, 0, 0])},
        "jaw": {"rotation": sway(1.4, 5, 0, 0, [8, 0, 0])},
    }))
    # удар рукой с цепью: замах через плечо и хлёст
    m.anim("attack", once(0.9, {
        "right_arm": {"rotation": beats(0.9, [(0, z), (0.35, [-150, 20, 30]), (0.55, [-40, -10, -10]), (0.75, [10, 0, 0]), (1, z)])},
        "right_forearm": {"rotation": beats(0.9, [(0, z), (0.35, [-50, 0, 0]), (0.55, [0, 0, 0]), (1, z)])},
        "chest": {"rotation": beats(0.9, [(0, z), (0.35, [-10, 30, 0]), (0.55, [14, -25, 0]), (1, z)])},
        "chain0": {"rotation": beats(0.9, [(0, z), (0.4, [60, 0, 0]), (0.6, [-50, 0, 0]), (0.8, [20, 0, 0]), (1, z)])},
        "chain1": {"rotation": beats(0.9, [(0, z), (0.45, [40, 0, 0]), (0.65, [-40, 0, 0]), (1, z)])},
        "weight": {"rotation": beats(0.9, [(0, z), (0.5, [30, 0, 0]), (0.7, [-30, 0, 0]), (1, z)])},
        "jaw": {"rotation": beats(0.9, [(0, z), (0.4, [25, 0, 0]), (0.7, [5, 0, 0]), (1, z)])},
    }))
    # подготовка приёма (общая, пока идёт полоса): руки разведены, цепь натянута, душа разгорается
    m.anim("cast", loop(1.6, {
        "right_arm": {"rotation": sway(1.6, 6, 0, 0, [-70, 0, 35])}, "left_arm": {"rotation": sway(1.6, 6, 0.5, 0, [-70, 0, -35])},
        "right_forearm": {"rotation": [-30, 0, 0]}, "left_forearm": {"rotation": [-30, 0, 0]},
        "chest": {"rotation": sway(1.6, 3, 0, 0, [-8, 0, 0])}, "head": {"rotation": sway(1.6, 4, 0.25, 0, [-14, 0, 0])},
        "jaw": {"rotation": sway(1.6, 8, 0, 0, [22, 0, 0])},
        "soul": {"scale": sway(1.6, 0.25, 0, 0, [1.25, 1.25, 1.25])},
        "chain0": {"rotation": sway(1.6, 10, 0.3, 2)}, "weight": {"rotation": sway(1.6, 20, 0.5, 1)},
    }))
    m.anim("slam", once(1.1, {
        "right_arm": {"rotation": beats(1.1, [(0, z), (0.4, [-175, 0, 12]), (0.58, [-25, 0, 0]), (0.8, [-30, 0, 0]), (1, z)])},
        "left_arm": {"rotation": beats(1.1, [(0, z), (0.4, [-175, 0, -12]), (0.58, [-25, 0, 0]), (0.8, [-30, 0, 0]), (1, z)])},
        "chest": {"rotation": beats(1.1, [(0, z), (0.4, [-20, 0, 0]), (0.58, [34, 0, 0]), (0.8, [28, 0, 0]), (1, z)])},
        "body": {"position": beats(1.1, [(0, z), (0.4, [0, 1.5, 0]), (0.58, [0, -2.5, 0]), (0.8, [0, -2, 0]), (1, z)])},
        "jaw": {"rotation": beats(1.1, [(0, z), (0.4, [10, 0, 0]), (0.6, [35, 0, 0]), (1, z)])},
        "weight": {"rotation": beats(1.1, [(0, z), (0.45, [-60, 0, 0]), (0.6, [40, 0, 0]), (1, z)])},
    }))
    # смерть: колени подгибаются, кости оседают, голова скатывается
    m.anim("death", once(1.6, {
        "body": {"position": beats(1.6, [(0, z), (0.4, [0, -6, 0]), (1, [0, -14, 0])]),
                 "rotation": beats(1.6, [(0, z), (0.4, [10, 0, 0]), (1, [70, 0, 8])])},
        "right_leg": {"rotation": beats(1.6, [(0, z), (0.4, [-60, 0, 10]), (1, [-80, 0, 20])])},
        "left_leg": {"rotation": beats(1.6, [(0, z), (0.4, [-50, 0, -10]), (1, [-80, 0, -25])])},
        "right_shin": {"rotation": beats(1.6, [(0, z), (0.4, [80, 0, 0]), (1, [100, 0, 0])])},
        "left_shin": {"rotation": beats(1.6, [(0, z), (0.4, [70, 0, 0]), (1, [100, 0, 0])])},
        "head": {"rotation": beats(1.6, [(0, z), (0.6, [20, 30, 0]), (1, [50, 60, 30])])},
        "jaw": {"rotation": beats(1.6, [(0, z), (0.5, [40, 0, 0]), (1, [35, 0, 0])])},
        "soul": {"scale": beats(1.6, [(0, [1, 1, 1]), (0.5, [1.6, 1.6, 1.6]), (1, [0.01, 0.01, 0.01])])},
        "right_arm": {"rotation": beats(1.6, [(0, z), (1, [-30, 0, 60])])},
        "left_arm": {"rotation": beats(1.6, [(0, z), (1, [-20, 0, -70])])},
    }, hold=True))


MODELS = {"gatekeeper": gatekeeper}
