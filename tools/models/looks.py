"""Облик игрока по классу (обновление 14, пункт 7): слой поверх модели игрока — головной убор, наплечники,
накидка, пояс, наручи, сапоги, плащ и заплечные вещи. Каждый из 30 классов — свой силуэт и своя эмблема.

Кости верхнего уровня называются как части тела игрока и стоят в их опорных точках (как в ванильной модели):
head/body [0, 24, 0], right_arm [-5, 22, 0], left_arm [5, 22, 0], right_leg [-1.9, 12, 0], left_leg [1.9, 12, 0].
Игра (client/look/LookLayer) ведёт каждую кость за своей частью тела; кости cape* развеваются как плащ.
Тело игрока: голова x ±4, y 24..32, z ±4 (второй слой скина +0,5); туловище x ±4, y 12..24, z ±2;
руки x 4..8, ноги x 0..4 — поэтому оболочки облика начинаются чуть дальше второго слоя скина.

Превью на манекене: python -I tools/look_preview.py guardian [--size 520]
"""
import math

import hy
from hy import Model, mat, mix, mul, rgb
from crypt import rivets, tatter

# ============================================================================ материалы облика

M = {}


def lm(name, *a, **k):
    M[name] = mat("lk_" + name, *a, **k)
    return M[name]


lm("steel", "#A4AEBA", "metal", dark="#59636E", light="#EEF4FA", gloss=0.7)
lm("steel_dark", "#5A606A", "metal", dark="#2C3036", light="#9AA2AE", gloss=0.5)
lm("iron_black", "#3A3C44", "metal", dark="#1A1B20", light="#6C707C", gloss=0.4)
lm("gold", "#E0B040", "metal", dark="#8A6418", light="#FFF0A0", gloss=0.8)
lm("brass", "#B88A3A", "metal", dark="#6A4A18", light="#F0C878", gloss=0.6)
lm("copper", "#C0703A", "metal", dark="#6E3A1A", light="#F0A870", gloss=0.6)
lm("silver", "#D4DEE8", "metal", dark="#7A8896", light="#FFFFFF", gloss=1.0)
lm("leather", "#7A5234", "leather", dark="#45291A", light="#A8784E")
lm("leather_dark", "#4A3222", "leather", dark="#24180E", light="#6E4E36")
lm("leather_black", "#2A2426", "leather", dark="#120E10", light="#4A4044")
lm("fur", "#8A6E52", "fur", dark="#4A3626", light="#C8AA88")
lm("fur_white", "#E6E2DA", "fur", dark="#A8A298", light="#FFFFFF")
lm("fur_grey", "#6E6A66", "fur", dark="#3A3634", light="#A8A29C")
lm("bone", "#E4D8BA", "bone", dark="#9C8E70", light="#FFF6DE")
lm("wood", "#7A5532", "wood", dark="#45301A", light="#A87A4C")
lm("chain", "#6E747E", "chain", dark="#33373E", light="#AEB6C2")
lm("chain_dark", "#45474E", "chain", dark="#1E1F24", light="#7C808A")
lm("socket", "#121016", "plain", dark="#08070A", light="#1E1A24")
lm("rope", "#B89A6A", "leather", dark="#7A6040", light="#E0C898")
lm("glass", "#C8ECFF", "crystal", glow="pattern", dark="#6AA8D0", light="#FFFFFF")
lm("mirror", "#D8F0FF", "metal", dark="#7AA0C0", light="#FFFFFF", gloss=1.0)
lm("ice", "#A8E0F4", "crystal", glow="pattern", dark="#5A9AC0", light="#F0FCFF")
lm("moss", "#4E9A52", "moss", dark="#2A5A2E", light="#8AD890")
lm("bark", "#6A5034", "bark", dark="#36261A", light="#94744E")
lm("lava", "#5A1A10", "lava", dark="#2A0A06", light="#FF8A3A")
lm("void", "#2A1A3E", "skin", dark="#120A1E", light="#5A3A80")
lm("cloud", "#8A92A6", "plain", dark="#5A6276", light="#C8D0E0")


def cloth(name, base, dark=None, light=None):
    return lm(name, base, "cloth", dark=dark, light=light)


def glow(name, base, light="#FFFFFF", dark=None):
    return lm(name, base, "glow", glow=True, light=light, dark=dark)


cloth("blue", "#2E5490", "#142A50", "#5A84C8")
cloth("navy", "#1E2A4A", "#0E1426", "#3A4C7A")
cloth("red", "#9A2228", "#4E0E12", "#D04A50")
cloth("blood", "#6A1014", "#34060A", "#9A2428")
cloth("white", "#E4E0D6", "#A8A49A", "#FFFFFF")
cloth("cream", "#D8C8A0", "#9A8A66", "#F4E8C8")
cloth("black", "#24222A", "#100F14", "#424050")
cloth("grey", "#5E626A", "#33363C", "#8E949E")
cloth("ash", "#8A8C92", "#55575C", "#BCBEC4")
cloth("purple", "#4E2C78", "#26143C", "#7E54B0")
cloth("violet", "#3A2458", "#1C102C", "#6A48A0")
cloth("lilac", "#A47ACC", "#644488", "#D6B8F4")
cloth("teal", "#1E7468", "#0C3C36", "#46B0A0")
cloth("green", "#2E5E34", "#14301A", "#58965E")
cloth("olive", "#4E5A2A", "#262E12", "#7C8A48")
cloth("orange", "#C8661E", "#70340A", "#F0A050")
cloth("crimson", "#8A1A1E", "#44080C", "#C8343A")
cloth("frost", "#9CC8E0", "#5A8AA8", "#E4F6FF")
cloth("midnight", "#18204A", "#0A0E24", "#34407E")
cloth("sand", "#B89A6A", "#7A6040", "#E0C898")
cloth("rust", "#8A4A26", "#4A2410", "#BC7446")

glow("g_blue", "#7FB8FF", dark="#3A70D0")
glow("g_red", "#FF4A3A", "#FFE0C0", "#C01A10")
glow("g_gold", "#FFD860", dark="#D89A20")
glow("g_green", "#8AFF96", "#F0FFF0", "#3AC050")
glow("g_purple", "#C08AFF", "#F6EEFF", "#7A3AD0")
glow("g_ice", "#9AE8FF", dark="#3AB0E0")
glow("g_fire", "#FF8A2E", "#FFF0B0", "#E03A08")
glow("g_storm", "#B8C6FF", dark="#5A70E0")
glow("g_soul", "#BFE8FF", dark="#5AA8E0")
glow("g_teal", "#7FF0D0", dark="#2AB090")
glow("g_plague", "#B8E83A", "#F4FFC0", "#6A9A10")


# ============================================================================ эмблемы (декали)

GLYPHS = {
    "shield": ["  #####  ", " ####### ", " ##...## ", " ##...## ", " ##...## ", "  ##.##  ", "   ###   ", "    #    "],
    "drop": ["    #    ", "   ###   ", "  #####  ", " ####### ", " ####### ", " ####### ", "  #####  ", "   ###   "],
    "frame": ["#########", "#.......#", "#.#####.#", "#.#...#.#", "#.#...#.#", "#.#####.#", "#.......#", "#########"],
    "spiral": ["  #####  ", " #.....# ", "#..###..#", "#.#..#..#", "#.#.##..#", "#..#...# ", " #....#  ", "  ####   "],
    "snow": ["#   #   #", " #  #  # ", "  # # #  ", "#########", "  # # #  ", " #  #  # ", "#   #   #"],
    "sun": ["#   #   #", " #  #  # ", "   ###   ", "#########", "   ###   ", " #  #  # ", "#   #   #"],
    "axes": ["##     ##", "###   ###", " ### ### ", "  #####  ", "   ###   ", "  ## ##  ", " ##   ## ", "##     ##"],
    "cross": ["   ###   ", "   ###   ", "#########", "#########", "   ###   ", "   ###   ", "   ###   ", "   ###   "],
    "arrow": ["    #    ", "   ###   ", "  # # #  ", " #  #  # ", "    #    ", "    #    ", "   # #   ", "  #   #  "],
    "lotus": ["    #    ", "   ###   ", " # ### # ", "## ### ##", "#########", " ####### ", "  #####  "],
    "moon": ["   ####  ", "  ##     ", " ##      ", " ##      ", " ##      ", "  ##     ", "   ####  "],
    "skull": ["  #####  ", " ####### ", " #..#..# ", " ####### ", "  ## ##  ", "  #####  ", "  # # #  "],
    "anchor": ["    #    ", "   #.#   ", "    #    ", "  #####  ", "    #    ", "#   #   #", " ## # ## ", "   ###   "],
    "axe": ["  ####   ", " #####   ", " ######  ", "  ####   ", "    #    ", "    #    ", "    #    ", "    #    "],
    "target": ["  #####  ", " #     # ", "#  ###  #", "#  #.#  #", "#  ###  #", " #     # ", "  #####  "],
    "gear": [" # # # # ", "#########", " ##...## ", "##.....##", " ##...## ", "#########", " # # # # "],
    "flame": ["    #    ", "   ##    ", "   ###   ", "  ####   ", " ###### #", " ####### ", "  #####  ", "   ###   "],
    "bolt": ["     ### ", "    ###  ", "   ###   ", "  ###### ", "    ###  ", "   ###   ", "  ##     ", " #       "],
    "bio": ["   ###   ", "  #...#  ", "   ###   ", "##  #  ##", "#.# # #.#", " ## # ## ", "   ###   "],
    "star": ["    #    ", "    #    ", "   ###   ", "#########", "  #####  ", "  ## ##  ", " ##   ## "],
    "rune": ["#  ###  #", " # # # # ", "  #####  ", "#########", "  #####  ", " # # # # ", "#  ###  #"],
    "eye": ["  #####  ", " #.....# ", "#..###..#", "#..#.#..#", "#..###..#", " #.....# ", "  #####  "],
    "pentacle": ["    #    ", "   # #   ", "#########", " # # # # ", "  #   #  ", " #  #  # ", "#  # #  #"],
    "rift": ["    #    ", "   #.#   ", "  #...#  ", " #.....# ", "  #...#  ", "   #.#   ", "    #    "],
    "hourglass": ["#########", " #.....# ", "  #...#  ", "   #.#   ", "  # # #  ", " # ### # ", "#########"],
    "leaf": ["     ### ", "   ##### ", "  ###### ", " ####### ", " ###.##  ", " ##.##   ", " #.      ", "#        "],
    "totem": ["# ##### #", "##.#.#.##", "  #####  ", "  #...#  ", "  #####  ", " ### ### ", "  #####  "],
    "banner": ["#        ", "#######  ", "##....## ", "##....## ", "#######  ", "#  ##    ", "#   #    ", "#        "],
}


def glyph(name, col, glow_=False, cy=0.42, scale=1, edge=None, dark=None):
    """Эмблема из пиксельного рисунка: «#» — цвет, «.» — тёмная середина (или пропуск), по центру грани."""
    art = GLYPHS[name]
    c = rgb(col)
    d = rgb(dark) if dark else None

    def f(face):
        h, w = len(art) * scale, max(len(r) for r in art) * scale
        x0, y0 = (face.w - w) // 2, int(face.h * cy - h / 2)
        for j, row in enumerate(art):
            for i, ch in enumerate(row):
                for sy in range(scale):
                    for sx in range(scale):
                        x, y = x0 + i * scale + sx, y0 + j * scale + sy
                        if ch == "#":
                            face.set(x, y, c, glow=glow_)
                        elif ch == "." and d:
                            face.set(x, y, d)
        if edge:
            face.shade(0, 0, face.w, 1, 1.25)
            face.shade(0, face.h - 1, face.w, face.h, 0.6)
    return f


def both(*fns):
    def f(face):
        for fn in fns:
            fn(face)
    return f


def trim_edges(col, top=True, bottom=True):
    """Кайма по верхнему/нижнему краю грани (вышивка, окантовка)."""
    c = rgb(col)

    def f(face):
        for x in range(face.w):
            if top:
                face.set(x, 0, c)
                face.set(x, 1, mul(c, 0.7))
            if bottom:
                face.set(x, face.h - 1, mul(c, 0.7))
                face.set(x, face.h - 2, c)
    return f


def side_trim(col):
    c = rgb(col)

    def f(face):
        for y in range(face.h):
            face.set(0, y, c)
            face.set(face.w - 1, y, c)
    return f


def buttons(col, n=4):
    c = rgb(col)

    def f(face):
        x = face.w // 2
        for i in range(n):
            y = 3 + i * max(3, (face.h - 6) // max(1, n - 1))
            face.rect(x - 1, y, x + 1, y + 2, c)
            face.set(x - 1, y, mix(c, (255, 255, 255), 0.5))
    return f


def stars(col, n=14, seed=3):
    c = rgb(col)

    def f(face):
        for i in range(n):
            x = int(hy.h32(i, seed, face.w) * face.w)
            y = int(hy.h32(i, seed + 1, face.h) * face.h)
            face.set(x, y, c, glow=True)
            if i % 3 == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    face.set(x + dx, y + dy, mul(c, 0.6), glow=True)
    return f


def cracks(col, n=4, seed=5):
    """Светящиеся трещины (разлом, лава, руны)."""
    c = rgb(col)

    def f(face):
        for i in range(n):
            x = hy.h32(i, seed) * face.w
            y = hy.h32(i, seed + 7) * face.h * 0.4
            for s in range(int(face.h * 0.7)):
                face.set(x, y + s, c, glow=True)
                x += (hy.h32(i, s, seed) - 0.5) * 2.2
    return f


def visor(eye=None, kind="slit"):
    """Забрало: прорезь (и щель для дыхания) — тёмные, глаза могут светиться."""
    def f(face):
        w, h = face.w, face.h
        dark = rgb("#0E0C12")
        ey = int(h * 0.44)
        if kind == "slit":
            face.rect(2, ey, w - 2, ey + 2, dark)
            face.rect(w // 2 - 1, ey + 2, w // 2 + 1, int(h * 0.8), dark)
            for x in range(3, w - 3, 2):
                face.set(x, ey - 1, mul(face.get(x, ey - 1), 1.25))
        elif kind == "grill":
            for x in range(3, w - 3, 3):
                face.rect(x, ey - 1, x + 1, int(h * 0.82), dark)
            face.rect(2, ey - 2, w - 2, ey - 1, dark)
        elif kind == "holes":
            for sx in (int(w * 0.2), int(w * 0.58)):
                for y in range(ey - 1, ey + 2):
                    for x in range(sx, sx + int(w * 0.22)):
                        face.cut(x, y)
            return
        if eye:
            c = rgb(eye)
            for sx in (int(w * 0.26), int(w * 0.62)):
                face.rect(sx, ey, sx + 3, ey + 2, c, glow=True)
    return f


def face_shadow(eye=None):
    """Глубокий капюшон без лица: тьма и два светящихся глаза."""
    def f(face):
        w, h = face.w, face.h
        for y in range(h):
            for x in range(w):
                face.set(x, y, mix(rgb("#141018"), rgb("#050407"), y / h))
        if eye:
            c = rgb(eye)
            ey = int(h * 0.42)
            for sx in (int(w * 0.24), int(w * 0.6)):
                face.rect(sx, ey, sx + 3, ey + 2, c, glow=True)
                face.set(sx + 1, ey - 1, mul(c, 0.6), glow=True)
    return f


# ============================================================================ основа и детали

HEAD, BODY, RA, LA, RL, LL = "head", "body", "right_arm", "left_arm", "right_leg", "left_leg"
SIDES = ((RA, -1), (LA, 1))
LEGS = ((RL, -1), (LL, 1))


def base(cls):
    m = Model(f"look_{cls}")
    m.bone(HEAD, [0, 24, 0])
    m.bone(BODY, [0, 24, 0])
    m.bone(RA, [-5, 22, 0])
    m.bone(LA, [5, 22, 0])
    m.bone(RL, [-1.9, 12, 0])
    m.bone(LL, [1.9, 12, 0])
    return m


# ---------------------------------------------------------------- голова

def hood(m, mat_, rim, tip=True, low=False, shadow=None, mantle=None, tip_parts=None):
    """Капюшон: оболочка вокруг головы, открытая спереди, толстая кайма вокруг лица, острый кончик назад.
    shadow — лицо скрыто тьмой (с глазами цвета shadow); low — капюшон надвинут на брови."""
    h = HEAD
    m.box(h, [0, 28.45, 0.3], [9.8, 9.3, 9.4], mat_, skip=() if shadow else ("north", "down"),
          decal={"north": face_shadow(shadow)} if shadow else None)
    m.box(h, [0, 31.7 if low else 32.3, -4.3], [10.1, 2.4 if low else 1.8, 1.4], rim)
    for sx in (-1, 1):
        m.box(h, [sx * 4.45, 27.6, -4.3], [1.2, 7.6, 1.4], rim)
    m.box(h, [0, 24.3, 1.0], [9.2, 1.2, 7.2], rim)                                 # ворот вокруг шеи
    if tip:
        parts = tip_parts or [(2.8, 3.6, [58, 0, 0]), (2.6, 2.6, [28, 0, 0]), (2.2, 1.6, [22, 0, 0])]
        m.segments("hood_tip", h, [0, 31.6, 4.4], parts, mat_)
    if mantle:
        mantle_on(m, mantle)


def mantle_on(m, mat_, deco=None):
    """Пелерина на плечи: ворот и мысок на груди и спине."""
    b = BODY
    m.box(b, [0, 23.3, 0], [10.4, 2.0, 5.8], mat_, decal={"north": deco} if deco else None)
    m.box(b, [0, 21.6, -2.55], [4.2, 4.2, 0.7], mat_, rot=[0, 0, 45], pivot=[0, 21.6, -2.55])
    m.box(b, [0, 21.2, 2.55], [5.0, 5.0, 0.7], mat_, rot=[0, 0, 45], pivot=[0, 21.2, 2.55])


def helm(m, metal, trim, face="slit", eye=None, crest=None, crest_mat=None, trim_deco=None):
    """Шлем, закрывающий голову. face: slit / grill / open (лицо видно, с наносником)."""
    h = HEAD
    if face == "open":
        m.box(h, [0, 28.6, 0.2], [9.8, 9.0, 9.6], metal, skip=("north", "down"))
        m.box(h, [0, 32.0, -4.45], [10.0, 2.2, 1.2], trim, decal={"north": trim_deco or rivets})
        m.box(h, [0, 29.4, -4.6], [1.4, 4.6, 0.9], metal)                           # наносник
        for sx in (-1, 1):
            m.box(h, [sx * 4.25, 26.8, -4.0], [1.6, 6.0, 2.2], metal)               # нащёчники
    else:
        m.box(h, [0, 28.5, 0], [9.8, 9.2, 9.8], metal, skip=("down",), decal={"north": visor(eye, face)})
        m.box(h, [0, 31.9, 0], [10.2, 1.2, 10.2], trim, decal={"north": trim_deco or rivets, "south": rivets})
    m.box(h, [0, 24.4, 0], [10.0, 1.4, 10.0], trim, skip=("up",))                  # обод у шеи
    if crest == "plume":
        m.box(h, [0, 33.6, 0.5], [1.4, 1.6, 6.4], crest_mat or trim)
        m.segments("plume", h, [0, 33.6, 2.6], [(3.4, 2.2, [40, 0, 0]), (3.4, 2.0, [40, 0, 0]),
                                                 (3.0, 1.6, [35, 0, 0]), (2.6, 1.2, [20, 0, 0])], crest_mat)
    elif crest == "fin":
        m.box(h, [0, 34.2, 0.2], [1.2, 2.8, 8.4], crest_mat or trim)
    elif crest == "horns":
        horns(m, crest_mat or M["bone"], y=31.0, x=4.6)
    elif crest == "wings":
        for sx in (-1, 1):
            for i, (dy, ln) in enumerate(((0, 6.4), (1.6, 5.2), (3.0, 3.8))):
                m.box(h, [sx * (5.4 + i * 0.3), 30.2 + dy, 1.2 + i * 0.8], [0.8, 1.4, ln], crest_mat or trim,
                      rot=[28 + i * 10, 0, 0], pivot=[sx * 5.4, 30.2 + dy, -1])
    elif crest == "spikes":
        for i, x in enumerate((-3, 0, 3)):
            m.box(h, [x, 34.4 - abs(x) * 0.3, 0.5], [1.4, 3.4 - abs(x) * 0.3, 1.4], crest_mat or trim,
                  rot=[0, 45, 0], pivot=[x, 33, 0.5])


def horns(m, mat_, y=31.0, x=4.4, bone=HEAD, big=1.0, back=True):
    """Пара рогов: от висков вбок, затем вверх и назад (гладкий изгиб из повёрнутых частей)."""
    for sx, nm in ((-1, "horn_r"), (1, "horn_l")):
        parts = [(2.4 * big, 2.4 * big, [0, 0, sx * 70]), (2.4 * big, 2.0 * big, [20 if back else 0, 0, -sx * 35]),
                 (2.2 * big, 1.5 * big, [20 if back else 0, 0, -sx * 25]), (1.8 * big, 0.9 * big, [15, 0, -sx * 15])]
        m.segments(nm, bone, [sx * x, y, 0.4], parts, mat_)


def wide_hat(m, mat_, band, brim=14.0, crown=4.0, tilt=0.0, band_deco=None, point=False, cone=None):
    """Шляпа с полями: поля, тулья, лента; point/cone — остроконечная (маг)."""
    h = HEAD
    m.box(h, [0, 33.0, 0.2], [brim, 0.9, brim], mat_, rot=[tilt, 0, 0] if tilt else None, pivot=[0, 33, 0.2])
    m.box(h, [0, 33.4 + crown / 2, 0.3], [8.8, crown, 8.8], mat_)
    m.box(h, [0, 34.0, 0.3], [9.1, 1.2, 9.1], band, decal={"north": band_deco} if band_deco else None)
    if point:
        parts = cone or [(3.0, 7.0, [8, 0, 0]), (3.0, 5.0, [14, 0, 0]), (2.8, 3.2, [22, 0, 0]), (2.4, 1.6, [30, 0, 6])]
        m.segments("hat", h, [0, 33.4 + crown, 0.5], parts, mat_)


def circlet(m, mat_, gem=None, y=30.6, spikes=0, spike_mat=None):
    """Обруч поверх капюшона/волос: с камнем во лбу и (по желанию) зубцами короны."""
    h = HEAD
    m.box(h, [0, y, 0], [9.9, 1.1, 9.9], mat_, skip=("up", "down"))
    if gem:
        m.box(h, [0, y + 0.2, -5.05], [1.6, 1.8, 0.6], gem, rot=[0, 0, 45], pivot=[0, y + 0.2, -5.05])
    for i in range(spikes):
        x = -3.6 + 7.2 * i / max(1, spikes - 1)
        ln = 3.2 if i == spikes // 2 else 2.2
        m.box(h, [x, y + 0.5 + ln / 2, -4.9], [1.0, ln, 0.9], spike_mat or mat_)


def scarf_mask(m, mat_, deco=None):
    """Повязка на нижнюю половину лица."""
    m.box(HEAD, [0, 25.9, -0.1], [9.5, 3.8, 9.3], mat_, skip=("up", "down"), decal={"north": deco} if deco else None)


def goggles(m, frame, lens, y=30.4, on_eyes=False):
    h = HEAD
    yy = 28.3 if on_eyes else y
    m.box(h, [0, yy, 0], [9.7, 1.0, 9.7], M["leather_dark"], skip=("up", "down"))
    for sx in (-1, 1):
        m.box(h, [sx * 2.1, yy, -4.95], [3.0, 2.6, 1.2], frame)
        m.box(h, [sx * 2.1, yy, -5.6], [2.2, 1.8, 0.3], lens)


# ---------------------------------------------------------------- туловище и руки

def tunic(m, mat_, deco=None, back=None, long=False):
    """Накидка поверх туловища (вместо ванильной брони); long — до колен (полы на ногах)."""
    m.box(BODY, [0, 18.2, 0], [8.9, 11.6, 4.9], mat_, skip=("up", "down"),
          decal={k: v for k, v in (("north", deco), ("south", back)) if v})


def belt(m, mat_, buckle=None, deco=None, pouches=None):
    b = BODY
    m.box(b, [0, 12.9, 0], [9.3, 1.7, 5.3], mat_, decal={"north": deco} if deco else None)
    if buckle:
        m.box(b, [0, 12.9, -2.85], [2.2, 2.2, 0.6], buckle)
    for x, mt in (pouches or []):
        m.box(b, [x, 11.6, -2.6], [2.0, 2.4, 1.4], mt)


def straps(m, mat_, cross=True, buckle=None):
    """Ремни через грудь (одна перевязь или крест)."""
    b = BODY
    for sx in ((-1, 1) if cross else (1,)):
        m.box(b, [0, 18.6, -2.55], [1.4, 14.0, 0.5], mat_, rot=[0, 0, sx * 33], pivot=[0, 18.6, -2.55])
        m.box(b, [0, 18.6, 2.55], [1.4, 14.0, 0.5], mat_, rot=[0, 0, -sx * 33], pivot=[0, 18.6, 2.55])
    if buckle:
        m.box(b, [0, 18.6, -2.9], [1.8, 1.8, 0.4], buckle)


def pauldrons(m, metal, trim=None, size=1.0, layers=2, spikes=0, spike_mat=None, deco=None, round_=False, only=None,
              tilt=14):
    """Наплечники: несколько пластин внахлёст, наклонены наружу; spikes — шипы сверху."""
    for bone, sx in SIDES:
        if only and bone != only:
            continue
        cx = sx * 6.4
        w = 5.6 * size
        top = 24.6 + 0.4 * size
        rot = [0, 0, sx * tilt]
        pv = [sx * 4.2, 24.4, 0]
        m.box(bone, [cx, top, 0], [w, 2.6 * size, w * 0.98], metal, rot=rot, pivot=pv,
              decal={"north": deco or (rivets if trim else None), "south": rivets if trim else None}
              if (deco or trim) else None)
        if round_:
            m.box(bone, [cx + sx * 0.3, top + 1.3 * size, 0], [w * 0.7, 1.0 * size, w * 0.7], metal, rot=rot, pivot=pv)
        for i in range(1, layers):
            m.box(bone, [cx + sx * 0.4 * i, top - 2.0 * i * size, 0], [w - 0.8 * i, 1.8 * size, w * 0.98 - 0.6 * i],
                  trim or metal, rot=[0, 0, sx * (tilt + 6 * i)], pivot=pv)
        for k in range(spikes):
            z = (k - (spikes - 1) / 2) * 2.0
            m.box(bone, [cx + sx * 0.6, top + 1.4 * size + 1.3, z], [1.1, 2.8, 1.1], spike_mat or metal,
                  rot=[0, 0, sx * 30], pivot=[cx, top + 1.2, z])


def fur_mantle(m, mat_):
    """Меховой ворот на плечах — пышный, поверх рук и спины."""
    b = BODY
    m.box(b, [0, 23.6, 0.2], [11.6, 3.0, 6.8], mat_)
    m.box(b, [0, 21.8, 2.4], [9.6, 3.4, 2.2], mat_)
    for bone, sx in SIDES:
        m.box(bone, [sx * 6.3, 24.4, 0.1], [5.4, 2.4, 5.6], mat_)


def bracers(m, mat_, trim=None, long=False, blade=None, spikes=0):
    """Наручи на предплечьях; blade — скрытый клинок вдоль правого предплечья."""
    for bone, sx in SIDES:
        ln = 6.0 if long else 4.6
        m.box(bone, [sx * 6, 12.4 + ln / 2, 0], [4.7, ln, 4.7], mat_)
        if trim:
            m.box(bone, [sx * 6, 12.6 + ln, 0], [5.0, 0.9, 5.0], trim)
        for k in range(spikes):
            m.box(bone, [sx * 8.5, 14.4 + k * 2.2, 0], [1.2, 1.0, 1.0], trim or mat_)
    if blade:
        m.box(RA, [-6, 11.0, -2.9], [0.6, 9.0, 1.0], blade)


def boots(m, mat_, cuff=None, toe=True, tall=False, fur=None):
    for bone, sx in LEGS:
        hgt = 6.4 if tall else 4.8
        m.box(bone, [sx * 1.9, hgt / 2 - 0.05, -0.05], [4.5, hgt, 4.7], mat_)
        if toe:
            m.box(bone, [sx * 1.9, 0.85, -2.6], [4.3, 1.7, 1.0], mat_)
        if cuff:
            m.box(bone, [sx * 1.9, hgt + 0.2, 0], [4.8, 1.1, 5.0], cuff)
        if fur:
            m.box(bone, [sx * 1.9, hgt + 0.3, 0], [5.0, 1.6, 5.2], fur)


def leg_flaps(m, mat_, length=6.0, deco=None, top=12.2, split_front=False):
    """Полы одеяния на ногах (идут за шагом, поэтому одеяние не протыкается ногами)."""
    for bone, sx in LEGS:
        m.box(bone, [sx * 2.0, top - length / 2, 0.05], [4.6, length, 4.9], mat_, skip=("up",),
              decal={"north": deco or tatter, "south": deco or tatter, "east": tatter, "west": tatter})


def greaves(m, mat_, trim=None):
    """Наколенники/набедренники."""
    for bone, sx in LEGS:
        m.box(bone, [sx * 1.9, 8.4, -0.1], [4.6, 4.2, 4.8], mat_)
        if trim:
            m.box(bone, [sx * 1.9, 6.6, -2.55], [3.0, 2.0, 0.7], trim)



def sleeves(m, mat_, cuff=None, flare=False, upper=False):
    """Рукава поверх рук: облик заменяет броню, без них видна рубаха скина. upper — только плечо (кольчуга под латами);
    flare — широкий раструб у запястья (маг)."""
    for bone, sx in SIDES:
        if upper:
            m.box(bone, [sx * 6, 20.2, 0], [4.6, 6.6, 4.6], mat_, skip=("up",))
            continue
        m.box(bone, [sx * 6, 18.2, 0], [4.55, 11.4, 4.55], mat_, skip=("up",))
        if flare:
            m.box(bone, [sx * 6.1, 12.6, 0.3], [5.4, 3.0, 5.6], mat_, decal={"north": tatter, "south": tatter,
                                                                           "east": tatter, "west": tatter})
        if cuff:
            m.box(bone, [sx * 6, 14.6, 0], [4.9, 1.0, 4.9], cuff)


def pants(m, mat_):
    for bone, sx in LEGS:
        m.box(bone, [sx * 1.9, 8.6, 0], [4.45, 7.0, 4.45], mat_, skip=("up",))


# ---------------------------------------------------------------- спина

def cape(m, mat_, length=18.0, width=9.2, deco=None, inner=None, clasp=None, tattered=True, collar=None):
    """Плащ из двух полотнищ: верхнее и нижнее (своя кость — на бегу плащ изгибается)."""
    top = 23.6
    up = min(10.0, length * 0.55)
    m.bone("cape", [0, top, 2.6], BODY)
    m.box("cape", [0, top - up / 2, 2.95], [width, up, 0.6], mat_, faces={"north": inner} if inner else None,
          decal={"south": deco} if deco else None)
    m.bone("cape_lower", [0, top - up, 2.95], "cape")
    lo = length - up
    m.box("cape_lower", [0, top - up - lo / 2, 2.95], [width, lo + 0.02, 0.6], mat_,
          faces={"north": inner} if inner else None,
          decal={"south": tatter, "north": tatter} if tattered else None)
    if clasp:
        for sx in (-1, 1):
            m.box(BODY, [sx * 3.6, 23.0, -2.6], [1.6, 1.6, 0.7], clasp)
    if collar:
        m.box(BODY, [0, 24.2, 2.3], [width + 0.6, 2.2, 1.8], collar)


def back_shield(m, face_mat, rim, deco=None):
    b = BODY
    m.box(b, [0, 17.4, 3.4], [8.4, 11.0, 1.0], face_mat, decal={"south": deco} if deco else None)
    m.box(b, [0, 17.4, 3.4], [9.2, 11.8, 0.6], rim)
    m.box(b, [0, 11.2, 3.4], [4.0, 1.6, 1.0], rim, rot=[0, 0, 45], pivot=[0, 11.2, 3.4])


def quiver(m, mat_, trim, fletch):
    b = BODY
    m.box(b, [1.8, 17.0, 3.6], [3.2, 10.0, 3.0], mat_, rot=[0, 0, -25], pivot=[1.8, 17, 3.6])
    m.box(b, [1.8, 21.6, 3.6], [3.5, 1.0, 3.3], trim, rot=[0, 0, -25], pivot=[1.8, 17, 3.6])
    for i, (dx, dz) in enumerate(((-0.6, -0.5), (0.6, 0.4), (0, 0.9), (-0.5, 0.6))):
        m.box(b, [1.8 + dx, 23.6 + i * 0.3, 3.6 + dz], [0.5, 3.4, 0.5], M["wood"], rot=[0, 0, -25],
              pivot=[1.8, 17, 3.6])
        m.box(b, [1.8 + dx, 24.8 + i * 0.3, 3.6 + dz], [1.2, 1.6, 0.25], fletch, rot=[0, 0, -25],
              pivot=[1.8, 17, 3.6])


# ============================================================================ классы

def guardian():
    """Стражник: глухой шлем с синим плюмажем, тяжёлые наплечники, синий налатник со щитом, щит за спиной."""
    m = base("guardian")
    sleeves(m, M["chain"], upper=True)
    pants(m, M["chain"])
    helm(m, M["steel"], M["steel_dark"], face="slit", crest="plume", crest_mat=M["blue"])
    pauldrons(m, M["steel"], M["blue"], size=1.15, layers=3)
    tunic(m, M["blue"], deco=glyph("shield", "#E8EEF6", dark="#2E5490"))
    belt(m, M["leather_dark"], buckle=M["steel"])
    bracers(m, M["steel"], M["steel_dark"], long=True)
    greaves(m, M["steel"], M["steel_dark"])
    boots(m, M["steel_dark"], cuff=M["steel"])
    back_shield(m, M["blue"], M["steel"], deco=glyph("shield", "#E8EEF6", cy=0.45, scale=2, dark="#2E5490"))
    return m


def blood_knight():
    """Кровавый рыцарь: чёрный шлем с рогами и багровыми глазами, шипастые наплечники, рваный кровавый плащ."""
    m = base("blood_knight")
    sleeves(m, M["chain_dark"], upper=True)
    pants(m, M["chain_dark"])
    helm(m, M["iron_black"], M["blood"], face="slit", eye="#FF3A2A", crest="horns", crest_mat=M["bone"])
    pauldrons(m, M["iron_black"], M["blood"], size=1.1, layers=2, spikes=3, spike_mat=M["bone"])
    tunic(m, M["blood"], deco=glyph("drop", "#FF4A3A", glow_=True))
    belt(m, M["iron_black"], buckle=M["g_red"])
    bracers(m, M["iron_black"], M["blood"], long=True, spikes=2)
    greaves(m, M["iron_black"])
    boots(m, M["iron_black"], cuff=M["blood"])
    cape(m, M["blood"], length=19, inner=M["black"], clasp=M["g_red"])
    return m


def mirror():
    """Зерцало: зеркальный шлем со стеклянным забралом, наплечники-осколки, стеклянные пластины за спиной."""
    m = base("mirror")
    sleeves(m, M["white"], cuff=M["mirror"])
    h = HEAD
    m.box(h, [0, 28.5, 0], [9.8, 9.2, 9.8], M["mirror"], skip=("down",), decal={"north": visor("#C8F4FF", "slit")})
    m.box(h, [0, 28.6, -5.05], [7.2, 4.0, 0.4], M["glass"])
    m.box(h, [0, 24.4, 0], [10.0, 1.4, 10.0], M["steel"], skip=("up",))
    for i, (x, ln, r) in enumerate(((-3.0, 3.0, 12), (0, 4.4, 0), (3.0, 3.0, -12))):
        m.box(h, [x, 33.2 + ln / 2, 0], [1.6, ln, 1.6], M["glass"], rot=[0, 45, r], pivot=[x, 33.2, 0])
    for bone, sx in SIDES:
        for k, (dy, dz, r) in enumerate(((0, -1.2, 20), (1.0, 1.0, 35), (-0.6, 0.2, 10))):
            m.box(bone, [sx * (6.6 + k * 0.5), 25.2 + dy, dz], [2.0, 3.6 - k * 0.6, 2.0], M["glass"],
                  rot=[r * 0.4, 45, sx * r], pivot=[sx * 6.4, 24.6, dz])
        m.box(bone, [sx * 6.3, 24.4, 0], [5.4, 1.8, 5.4], M["mirror"])
    tunic(m, M["white"], deco=glyph("frame", "#9ADCF4", glow_=True))
    belt(m, M["steel"], buckle=M["glass"])
    bracers(m, M["mirror"], M["glass"])
    boots(m, M["steel"], cuff=M["mirror"])
    for sx in (-1, 1):
        m.box(BODY, [sx * 3.2, 18.0, 4.2], [4.4, 9.0, 0.5], M["mirror"], rot=[0, -sx * 28, sx * 8], pivot=[sx * 1, 18, 3])
        m.box(BODY, [sx * 3.2, 18.0, 4.5], [3.6, 8.0, 0.3], M["glass"], rot=[0, -sx * 28, sx * 8], pivot=[sx * 1, 18, 3])
    return m


def graviton():
    """Гравитон: тяжёлый тёмно-фиолетовый шлем, кольцо-орбита вокруг головы со светящимся ядром, круглые латы."""
    m = base("graviton")
    sleeves(m, M["chain_dark"], upper=True)
    pants(m, M["chain_dark"])
    vio = lm("grav_metal", "#4A3E7A", "metal", dark="#221C3E", light="#8A7AD0", gloss=0.6)
    helm(m, vio, M["steel_dark"], face="grill", eye="#B8A8FF")
    h = HEAD
    ring = M["g_purple"]
    for i in range(8):
        a = math.radians(i * 45)
        x, z = math.sin(a) * 6.4, math.cos(a) * 6.4
        m.box(h, [x, 29.6 + math.sin(a) * 1.6, z], [2.4, 0.7, 0.7], ring, rot=[0, i * 45, 0],
              pivot=[x, 29.6 + math.sin(a) * 1.6, z])
    m.box(h, [0, 35.4, 0], [2.4, 2.4, 2.4], M["g_purple"], rot=[45, 45, 0], pivot=[0, 35.4, 0])
    m.box(h, [0, 35.4, 0], [3.2, 3.2, 3.2], M["void"], rot=[0, 45, 45], pivot=[0, 35.4, 0])
    pauldrons(m, vio, M["steel_dark"], size=1.25, layers=2, round_=True)
    tunic(m, M["violet"], deco=glyph("spiral", "#C08AFF", glow_=True))
    belt(m, M["steel_dark"], buckle=M["g_purple"])
    bracers(m, vio, M["steel_dark"], long=True)
    greaves(m, vio)
    boots(m, vio, cuff=M["steel_dark"])
    return m


def bastion():
    """Ледяной бастион: тяжёлый шлем в инее с ледяными зубцами, белый мех на плечах, ледяные друзы, меховой плащ."""
    m = base("bastion")
    sleeves(m, M["chain"], upper=True)
    pants(m, M["chain"])
    frost_metal = lm("frost_metal", "#8AA6BC", "metal", dark="#4A6478", light="#E4F4FF", gloss=0.7)
    helm(m, frost_metal, M["steel_dark"], face="slit", eye="#9AE8FF")
    h = HEAD
    for i, (x, ln, rz) in enumerate(((-3.4, 2.6, 16), (-1.2, 4.0, 6), (1.4, 3.4, -8), (3.4, 2.4, -18))):
        m.box(h, [x, 33.0 + ln / 2, -1 + i * 0.6], [1.5, ln, 1.5], M["ice"], rot=[0, 45, rz], pivot=[x, 33, 0])
    fur_mantle(m, M["fur_white"])
    for bone, sx in SIDES:
        for k, (dx, dz, ln) in enumerate(((0.6, -1.4, 3.6), (1.6, 0.8, 2.8), (-0.4, 1.6, 2.2))):
            m.box(bone, [sx * (7.0 + dx), 26.6 + ln / 2 - 1, dz], [1.6, ln, 1.6], M["ice"],
                  rot=[10, 45, sx * (25 + k * 10)], pivot=[sx * 7, 25.8, dz])
    tunic(m, M["frost"], deco=glyph("snow", "#E8FAFF", glow_=True))
    belt(m, M["steel_dark"], buckle=M["ice"])
    bracers(m, frost_metal, M["fur_white"], long=True)
    greaves(m, frost_metal)
    boots(m, frost_metal, fur=M["fur_white"])
    cape(m, M["fur_white"], length=17, tattered=False)
    return m


def paladin():
    """Паладин клятвы: золотой шлем с крыльями и сияющим нимбом, белый налатник с солнцем, белый плащ."""
    m = base("paladin")
    sleeves(m, M["chain"], upper=True)
    pants(m, M["chain"])
    helm(m, M["silver"], M["gold"], face="slit", eye="#FFF0B0", crest="wings", crest_mat=M["gold"])
    h = HEAD
    for i in range(12):
        a = math.radians(i * 30)
        x, z = math.sin(a) * 4.0, math.cos(a) * 4.0 + 1.2
        m.box(h, [x, 36.2, z], [2.3, 0.5, 0.6], M["g_gold"], rot=[0, i * 30, 0], pivot=[x, 36.2, z])
    pauldrons(m, M["gold"], M["silver"], size=1.2, layers=2, round_=True,
              deco=glyph("sun", "#FFF4C0", cy=0.5))
    tunic(m, M["white"], deco=glyph("sun", "#E0B040", dark="#FFFFFF"), back=trim_edges("#E0B040", top=False))
    belt(m, M["gold"], buckle=M["g_gold"])
    bracers(m, M["silver"], M["gold"], long=True)
    greaves(m, M["silver"], M["gold"])
    boots(m, M["silver"], cuff=M["gold"])
    cape(m, M["white"], length=19, inner=M["cream"], clasp=M["gold"], tattered=False,
         deco=both(trim_edges("#E0B040", top=False), glyph("sun", "#E0B040", cy=0.55, scale=2)))
    return m


def berserker():
    """Берсерк: волчья шкура на голове (морда, уши, красные глаза), меховые плечи, ремни крест-накрест."""
    m = base("berserker")
    h = HEAD
    m.box(h, [0, 29.4, 0.6], [9.8, 7.4, 9.4], M["fur_grey"], skip=("north", "down"))
    m.box(h, [0, 32.6, -3.0], [8.6, 2.4, 5.4], M["fur_grey"])                                    # лоб зверя
    m.box(h, [0, 32.2, -6.6], [4.6, 2.6, 4.0], M["fur_grey"], decal={"north": visor("#FF3A2A", "slit")})  # морда
    m.box(h, [0, 31.2, -7.2], [3.6, 1.0, 3.0], M["bone"])                                         # клыки
    m.box(h, [0, 33.4, -8.4], [1.6, 1.0, 0.8], M["socket"])                                        # нос
    for sx in (-1, 1):
        m.box(h, [sx * 2.6, 32.6, -4.4], [1.2, 1.0, 0.6], M["g_red"])
        m.box(h, [sx * 3.0, 34.8, -1.0], [1.8, 2.8, 1.0], M["fur_grey"], rot=[-15, 0, sx * 12], pivot=[sx * 3, 33.6, -1])
    m.bone("pelt", [0, 30, 4.6], h)
    m.box("pelt", [0, 26.6, 4.9], [8.0, 8.0, 0.8], M["fur_grey"], decal={"south": tatter})
    fur_mantle(m, M["fur"])
    pauldrons(m, M["steel_dark"], M["leather_dark"], size=1.0, layers=1, spikes=2, only=RA)
    straps(m, M["leather_dark"], cross=True, buckle=M["bone"])
    belt(m, M["leather_dark"], buckle=M["bone"], deco=glyph("axes", "#C8C0B0", cy=0.5))
    bracers(m, M["leather_dark"], M["steel_dark"], spikes=2)
    leg_flaps(m, M["fur"], length=5)
    boots(m, M["leather_dark"], fur=M["fur"])
    return m


def swift():
    """Стриж: лёгкий бирюзовый капюшон и повязка на лицо, длинные концы шарфа летят за спиной."""
    m = base("swift")
    sleeves(m, M["teal"], upper=True)
    hood(m, M["teal"], M["leather"], tip=False)
    scarf_mask(m, M["teal"], deco=trim_edges("#7FF0D0", bottom=False))
    m.box(BODY, [0, 23.6, 0.2], [9.6, 1.8, 5.8], M["teal"])
    m.bone("cape", [1.2, 23.4, 2.6], BODY)
    for sx, w in ((-1, 2.2), (1, 1.8)):
        m.box("cape", [sx * 1.4 + 1.2, 19.2, 2.95], [w, 8.4, 0.5], M["teal"])
    m.bone("cape_lower", [1.2, 15.0, 2.95], "cape")
    for sx, w, ln in ((-1, 2.2, 7.0), (1, 1.8, 9.0)):
        m.box("cape_lower", [sx * 1.4 + 1.2, 15.0 - ln / 2, 2.95], [w, ln, 0.5], M["teal"], decal={"south": tatter})
    pauldrons(m, M["leather"], None, size=0.85, layers=1, only=LA)
    tunic(m, M["leather_dark"], deco=glyph("arrow", "#7FF0D0", glow_=True))
    straps(m, M["leather"], cross=False, buckle=M["steel"])
    belt(m, M["leather"], buckle=M["steel"], pouches=[(-3.0, M["leather_dark"])])
    bracers(m, M["leather"], M["teal"])
    boots(m, M["leather"], cuff=M["teal"])
    for bone, sx in LEGS:
        m.box(bone, [sx * 4.2, 3.6, 1.2], [0.4, 2.4, 3.0], M["g_teal"], rot=[-20, 0, 0], pivot=[sx * 4.2, 3, 0])
    return m


def monk():
    """Монах: повязка с длинными хвостами, крупные чётки на груди, оранжевое одеяние с широким кушаком, обмотки."""
    m = base("monk")
    h = HEAD
    m.box(h, [0, 30.0, 0], [9.6, 1.6, 9.6], M["red"], skip=("up", "down"))
    m.box(h, [0, 30.0, 4.95], [1.8, 1.8, 0.6], M["gold"])
    m.segments("tail_a", h, [-0.6, 30.0, 5.0], [(3.0, 1.0, [160, 0, 10]), (3.0, 0.9, [15, 0, 8]), (2.6, 0.8, [10, 0, 5])],
               M["red"])
    m.segments("tail_b", h, [0.6, 30.0, 5.0], [(3.4, 1.0, [160, 0, -12]), (3.0, 0.9, [12, 0, -6]), (2.4, 0.8, [8, 0, -4])],
               M["red"])
    tunic(m, M["orange"], deco=glyph("lotus", "#FFE0A0", cy=0.36))
    m.box(BODY, [-0.5, 19.0, -2.55], [9.4, 1.6, 0.5], M["orange"], rot=[0, 0, 38], pivot=[0, 19, -2.55])
    for i in range(9):
        a = math.radians(-70 + i * 17.5)
        x, y = math.sin(a) * 3.8, 22.6 - math.cos(a) * 4.4
        m.box(BODY, [x, y, -2.75], [1.3, 1.3, 1.3], M["wood"] if i != 4 else M["g_gold"], rot=[0, 0, 45], pivot=[x, y, -2.75])
    m.box(BODY, [0, 13.0, 0], [9.4, 2.6, 5.4], M["red"])
    m.box(BODY, [2.6, 10.0, -2.8], [1.6, 4.6, 0.5], M["red"], decal={"north": tatter})
    m.box(BODY, [3.8, 10.4, -2.8], [1.4, 3.8, 0.5], M["red"], decal={"north": tatter})
    for bone, sx in SIDES:
        for k in range(3):
            m.box(bone, [sx * 6, 13.2 + k * 1.6, 0], [4.6, 1.2, 4.6], M["cream"])
    leg_flaps(m, M["orange"], length=7, top=12.4)
    for bone, sx in LEGS:
        m.box(bone, [sx * 1.9, 1.4, -0.05], [4.5, 2.8, 4.6], M["cream"])
        m.box(bone, [sx * 1.9, 0.25, -0.1], [4.7, 0.5, 4.9], M["wood"])
    return m


def shadow():
    """Тень: глубокий чёрный капюшон с фиолетовой каймой, маска на лицо, короткий рваный плащ, клинок в наруче."""
    m = base("shadow")
    sleeves(m, M["leather_black"])
    pants(m, M["black"])
    hood(m, M["black"], M["violet"], low=True, tip_parts=[(2.6, 3.4, [62, 0, 0]), (2.4, 2.2, [30, 0, 0])])
    scarf_mask(m, M["black"], deco=trim_edges("#9B5CFF", bottom=False))
    pauldrons(m, M["leather_black"], None, size=0.85, layers=2)
    tunic(m, M["leather_black"], deco=glyph("moon", "#9B5CFF", glow_=True, cy=0.36))
    straps(m, M["leather_dark"], cross=True)
    belt(m, M["leather_dark"], buckle=M["steel_dark"], pouches=[(3.0, M["leather_black"])])
    for x in (-2.2, 2.6):
        m.box(BODY, [x, 12.2, -3.2], [0.6, 4.0, 0.6], M["steel_dark"], rot=[0, 0, 25 if x < 0 else -25], pivot=[x, 13, -3])
    bracers(m, M["leather_black"], M["violet"], blade=M["steel"])
    boots(m, M["leather_black"], cuff=M["violet"], tall=True)
    cape(m, M["black"], length=13, inner=M["violet"])
    return m


def reaper():
    """Жнец: глубокий капюшон без лица — тьма и два бледных огня, череп на плече, цепь на поясе, длинный рваный плащ."""
    m = base("reaper")
    sleeves(m, M["ash"], flare=True)
    hood(m, M["ash"], M["black"], shadow="#BFE8FF", tip_parts=[(3.4, 3.6, [50, 0, 0]), (3.0, 2.6, [30, 0, 0]),
                                                                (2.6, 1.6, [25, 0, 0])])
    mantle_on(m, M["black"])
    sk = lm("rp_skull", "#E4D8BA", "bone", dark="#9C8E70", light="#FFF6DE")
    from crypt import skull_face
    m.box(RA, [-6.6, 26.2, 0], [4.2, 4.0, 4.2], sk, decal={"north": skull_face, "east": skull_face}, rot=[0, -30, 0],
          pivot=[-6.6, 26.2, 0])
    m.box(RA, [-6.4, 24.3, 0], [5.4, 1.6, 5.4], M["iron_black"])
    pauldrons(m, M["iron_black"], None, size=0.9, layers=2, only=LA)
    tunic(m, M["grey"], deco=glyph("skull", "#BFE8FF", glow_=True, cy=0.38))
    belt(m, lm("rp_chain", "#5E636B", "chain", dark="#2E3136", light="#9AA2AC"), buckle=M["bone"])
    m.box(BODY, [-3.2, 9.8, -2.7], [0.8, 5.2, 0.8], M["iron_black"])
    bracers(m, M["leather_black"], M["iron_black"])
    leg_flaps(m, M["ash"], length=8)
    boots(m, M["leather_black"], toe=False)
    cape(m, M["ash"], length=21, inner=M["black"], clasp=M["bone"])
    return m


def harpooner():
    """Гарпунёр: зюйдвестка, синий бушлат с медными пуговицами, цепь через плечо, гарпун за спиной."""
    m = base("harpooner")
    sleeves(m, M["navy"])
    pants(m, M["leather_dark"])
    oil = lm("oilskin", "#C8A24A", "leather", dark="#7A5E20", light="#F0D07A")
    h = HEAD
    m.box(h, [0, 31.4, 0.4], [9.8, 3.4, 9.6], oil, skip=("north", "down"))
    m.box(h, [0, 33.4, 0.4], [9.0, 1.4, 8.8], oil)
    m.box(h, [0, 32.4, -4.8], [10.4, 0.8, 2.6], oil, rot=[-14, 0, 0], pivot=[0, 32.6, -4])
    m.box(h, [0, 30.0, 5.6], [10.0, 0.8, 4.2], oil, rot=[24, 0, 0], pivot=[0, 30.6, 4.4])
    for sx in (-1, 1):
        m.box(h, [sx * 4.6, 28.8, 0.6], [1.0, 4.6, 6.0], oil)
    tunic(m, M["navy"], deco=buttons("#E0B040", 4), back=glyph("anchor", "#C8A24A"))
    m.box(BODY, [0, 23.4, -0.2], [9.6, 2.0, 5.6], M["navy"])
    chain = lm("hp_chain", "#6E7682", "chain", dark="#3A3F48", light="#B8C2CE")
    m.box(BODY, [0, 18.4, -2.7], [1.6, 15.6, 0.7], chain, rot=[0, 0, 36], pivot=[0, 18.4, -2.7])
    m.box(BODY, [0, 18.4, 2.7], [1.6, 15.6, 0.7], chain, rot=[0, 0, -36], pivot=[0, 18.4, 2.7])
    m.box(LA, [6.2, 24.2, 0], [5.4, 2.0, 5.4], chain)
    belt(m, M["leather_dark"], buckle=M["brass"], deco=None, pouches=[(-3.0, M["leather"])])
    bracers(m, M["navy"], M["brass"])
    boots(m, M["leather_black"], cuff=M["leather_dark"], tall=True)
    b = BODY
    m.box(b, [-1.4, 19.0, 3.4], [0.9, 22.0, 0.9], M["wood"], rot=[0, 0, 24], pivot=[-1.4, 19, 3.4])
    m.box(b, [-1.4, 31.0, 3.4], [1.6, 4.0, 0.6], M["steel"], rot=[0, 0, 24], pivot=[-1.4, 19, 3.4])
    for sx in (-1, 1):
        m.box(b, [-1.4 + sx * 1.0, 29.6, 3.4], [0.6, 2.4, 0.5], M["steel"], rot=[0, 0, 24 - sx * 30], pivot=[-1.4, 19, 3.4])
    return m


def headsman():
    """Палач: чёрный мешок-колпак с прорезями (видны глаза), голые руки, кожаный фартук, тяжёлый пояс."""
    m = base("headsman")
    h = HEAD
    m.box(h, [0, 28.3, 0.1], [9.6, 9.0, 9.4], M["black"], skip=("down",), decal={"north": visor(kind="holes")})
    m.segments("hood_tip", h, [0, 32.6, 0.6], [(2.6, 4.0, [10, 0, 0]), (2.4, 2.6, [30, 0, 0]), (2.0, 1.4, [40, 0, 0])],
               M["black"])
    m.box(h, [0, 24.2, 0.4], [10.6, 1.8, 10.2], M["black"], decal={"north": tatter, "south": tatter})
    apron = lm("apron", "#5A3A26", "leather", dark="#2E1C10", light="#86583A")
    m.box(BODY, [0, 17.2, -2.55], [7.6, 13.0, 0.6], apron,
          decal={"north": both(cracks("#5A0A0A", 3, 11), rivets)})
    m.box(BODY, [0, 4.0 + 10.0, -2.55], [7.0, 3.0, 0.6], apron)
    straps(m, M["leather_dark"], cross=False)
    belt(m, M["leather_black"], buckle=M["iron_black"], deco=glyph("skull", "#8A2A2A", cy=0.5))
    m.box(BODY, [0, 10.2, -2.9], [7.0, 4.6, 0.6], apron, decal={"north": tatter})
    for bone, sx in SIDES:
        m.box(bone, [sx * 6, 14.0, 0], [4.8, 3.8, 4.8], M["leather_black"])
        m.box(bone, [sx * 6, 16.2, 0], [5.0, 0.8, 5.0], M["iron_black"])
    m.box(LA, [6.4, 24.4, 0], [5.4, 2.2, 5.4], M["leather_black"])
    boots(m, M["leather_black"], cuff=M["iron_black"], tall=True)
    return m


def sniper():
    """Снайпер: серый капюшон, стеклянный прицел на правом глазу, колчан за спиной, наруч лучника, короткий плащ."""
    m = base("sniper")
    sleeves(m, M["leather_dark"])
    pants(m, M["grey"])
    hood(m, M["ash"], M["grey"], low=True, tip=False)
    h = HEAD
    m.box(h, [-2.0, 28.4, -4.9], [3.2, 3.2, 1.2], M["brass"])
    m.box(h, [-2.0, 28.4, -5.6], [2.4, 2.4, 0.3], M["g_ice"])
    m.box(h, [-4.2, 29.6, -4.2], [0.8, 0.8, 1.6], M["brass"])
    scarf_mask(m, M["grey"])
    pauldrons(m, M["leather"], None, size=0.9, layers=2, only=LA)
    tunic(m, M["leather_dark"], deco=glyph("target", "#C9D6E8", cy=0.38))
    straps(m, M["leather"], cross=False, buckle=M["brass"])
    belt(m, M["leather"], buckle=M["brass"], pouches=[(3.0, M["leather_dark"]), (-3.0, M["leather_dark"])])
    bracers(m, M["leather"], M["brass"])
    m.box(LA, [6.0, 15.2, -2.6], [3.0, 5.2, 0.8], M["leather_dark"])
    boots(m, M["leather_dark"], cuff=M["leather"], tall=True)
    quiver(m, M["leather_dark"], M["brass"], M["white"])
    cape(m, M["ash"], length=12, width=8.2)
    return m


def mechanic():
    """Механик: кожаная кепка с очками на лбу, рюкзак-котёл с шестернёй и дымящей трубой, медный наплечник."""
    m = base("mechanic")
    sleeves(m, M["rust"], upper=True)
    pants(m, M["leather_dark"])
    h = HEAD
    m.box(h, [0, 31.4, 0.2], [9.6, 2.6, 9.4], M["leather"], skip=("down",))
    m.box(h, [0, 31.0, -6.0], [8.4, 0.8, 3.0], M["leather"])
    goggles(m, M["brass"], M["g_fire"], y=31.0)
    b = BODY
    m.box(b, [0, 18.0, 4.2], [7.4, 9.6, 3.6], M["copper"], decal={"south": rivets, "east": rivets, "west": rivets})
    m.box(b, [0, 18.0, 6.1], [3.0, 3.0, 0.6], M["g_fire"])
    gear = M["brass"]
    for k in range(2):
        m.box(b, [-3.6, 20.4, 4.4], [1.0, 5.4, 5.4], gear, rot=[k * 45, 0, 0], pivot=[-3.6, 20.4, 4.4])
    m.box(b, [-3.9, 20.4, 4.4], [0.6, 2.4, 2.4], M["iron_black"])
    m.box(b, [2.4, 25.8, 4.6], [1.6, 4.2, 1.6], M["iron_black"])
    m.box(b, [2.4, 28.1, 4.6], [2.2, 0.8, 2.2], M["iron_black"])
    m.box(b, [2.4, 28.6, 4.6], [1.2, 0.4, 1.2], M["g_fire"])
    pauldrons(m, M["copper"], M["brass"], size=1.0, layers=2, only=RA,
              deco=glyph("gear", "#F0C878", cy=0.5))
    tunic(m, M["rust"], deco=buttons("#F0C878", 3))
    straps(m, M["leather_dark"], cross=True)
    belt(m, M["leather_dark"], buckle=M["brass"], pouches=[(-3.0, M["leather"]), (3.0, M["leather"])])
    m.box(b, [-4.6, 10.6, -1.0], [0.8, 4.4, 0.8], M["steel"])
    m.box(b, [-4.6, 8.6, -1.0], [1.6, 1.0, 1.6], M["steel"])
    for bone, sx in SIDES:
        m.box(bone, [sx * 6, 13.6, 0], [5.0, 3.4, 5.0], M["leather_dark"])
        m.box(bone, [sx * 6, 15.6, 0], [5.2, 0.8, 5.2], M["brass"])
    boots(m, M["leather_black"], cuff=M["brass"])
    return m


def pyromancer():
    """Пиромант: багровый капюшон с огненным гребнем, обугленная пелерина, руны жара, плащ, тлеющий снизу."""
    m = base("pyromancer")
    sleeves(m, M["crimson"], flare=True)
    hood(m, M["crimson"], M["black"], tip=False, mantle=M["black"])
    h = HEAD
    for i, (x, ln, rx) in enumerate(((0, 8.0, -15), (-2.4, 6.0, -25), (2.4, 6.0, -25), (0, 5.6, -50))):
        m.segments(f"flame{i}", h, [x, 32.6, -1.6 + i * 1.6], [(ln * 0.45, 3.0, [-rx, 0, x * 7]),
                                                                (ln * 0.35, 2.0, [20, 0, x * 5]),
                                                                (ln * 0.2, 1.0, [20, 0, 0])],
                   [M["g_red"], M["g_fire"], M["g_gold"]])
    tunic(m, M["crimson"], deco=glyph("flame", "#FF8A2E", glow_=True))
    belt(m, M["leather_black"], buckle=M["g_fire"])
    bracers(m, M["leather_black"], M["g_fire"])
    leg_flaps(m, M["crimson"], length=7)
    boots(m, M["leather_black"], cuff=M["black"])
    lava = M["lava"]
    m.bone("cape", [0, 23.6, 2.6], BODY)
    m.box("cape", [0, 18.6, 2.95], [9.0, 10.0, 0.6], M["crimson"], faces={"north": M["black"]})
    m.bone("cape_lower", [0, 13.6, 2.95], "cape")
    m.box("cape_lower", [0, 9.6, 2.95], [9.0, 8.0, 0.6], lava, decal={"south": tatter, "north": tatter})
    return m


def cryomancer():
    """Криомант: светлый капюшон с короной ледяных шипов, наплечники-льдины, бело-голубое одеяние со снежинкой."""
    m = base("cryomancer")
    sleeves(m, M["white"], flare=True)
    hood(m, M["frost"], M["white"], tip=True, mantle=M["white"])
    h = HEAD
    for i, (x, ln, rz) in enumerate(((-3.6, 2.6, 22), (-1.8, 3.6, 10), (0, 4.8, 0), (1.8, 3.6, -10), (3.6, 2.6, -22))):
        m.box(h, [x, 32.6 + ln / 2, -4.2], [1.3, ln, 1.3], M["ice"], rot=[0, 45, rz], pivot=[x, 32.6, -4.2])
    for bone, sx in SIDES:
        m.box(bone, [sx * 6.4, 24.6, 0], [5.6, 2.2, 5.6], M["ice"], rot=[0, 0, sx * 12], pivot=[sx * 4.2, 24.4, 0])
        m.box(bone, [sx * 7.4, 26.6, -0.6], [1.6, 3.4, 1.6], M["ice"], rot=[0, 45, sx * 30], pivot=[sx * 7, 25.4, 0])
    tunic(m, M["white"], deco=glyph("snow", "#7FD0F0", glow_=True))
    belt(m, M["frost"], buckle=M["g_ice"])
    bracers(m, M["frost"], M["ice"])
    leg_flaps(m, M["frost"], length=8, deco=both(trim_edges("#E4F6FF", top=False), tatter))
    boots(m, M["white"], cuff=M["frost"])
    cape(m, M["frost"], length=17, inner=M["white"], deco=glyph("snow", "#E4F6FF", cy=0.5, scale=2), clasp=M["ice"])
    return m


def stormcaller():
    """Громовержец: медная корона с молниями-зубцами, грозовая туча над головой, медные катушки на плечах."""
    m = base("stormcaller")
    sleeves(m, M["blue"])
    hood(m, M["navy"], M["copper"], tip=False)
    circlet(m, M["copper"], gem=M["g_storm"], y=31.4, spikes=5, spike_mat=M["g_storm"])
    h = HEAD
    for i, (x, z, s) in enumerate(((-2.4, 0.4, 3.4), (0.6, -0.4, 4.2), (3.0, 0.8, 3.0), (0.4, 1.8, 3.2))):
        m.box(h, [x, 38.2, z], [s, s * 0.7, s], M["cloud"])
    m.box(h, [1.2, 36.0, 0.2], [0.6, 2.0, 0.6], M["g_storm"], rot=[0, 0, 25], pivot=[1.2, 36.8, 0.2])
    m.box(h, [-1.6, 35.8, 0.6], [0.5, 1.6, 0.5], M["g_storm"], rot=[0, 0, -30], pivot=[-1.6, 36.4, 0.6])
    for bone, sx in SIDES:
        m.box(bone, [sx * 6.3, 24.4, 0], [5.4, 2.0, 5.4], M["copper"])
        for k in range(3):
            m.box(bone, [sx * 6.3, 25.8 + k * 0.9, 0], [3.6 - k * 0.8, 0.6, 3.6 - k * 0.8], M["g_storm"] if k == 2 else M["copper"])
    tunic(m, M["blue"], deco=glyph("bolt", "#B8C6FF", glow_=True))
    belt(m, M["leather_dark"], buckle=M["copper"])
    bracers(m, M["copper"], M["g_storm"])
    leg_flaps(m, M["navy"], length=7)
    boots(m, M["leather_dark"], cuff=M["copper"])
    cape(m, M["navy"], length=17, inner=M["blue"], deco=glyph("bolt", "#8FA6FF", cy=0.5, scale=2), clasp=M["copper"])
    return m


def plague_lord():
    """Чумной владыка: широкополая шляпа, маска-клюв со светящимися окулярами, длинный плащ, кадило заразы."""
    m = base("plague_lord")
    sleeves(m, M["black"])
    wide_hat(m, M["black"], M["olive"], brim=14.6, crown=4.2)
    h = HEAD
    mask = lm("pl_mask", "#8A7A5A", "leather", dark="#4A3E2A", light="#BCAA80")
    m.box(h, [0, 28.2, -0.2], [9.5, 8.4, 9.3], mask, skip=("up", "down"))
    for sx in (-1, 1):
        m.box(h, [sx * 2.1, 29.2, -4.95], [2.6, 2.6, 0.8], M["brass"])
        m.box(h, [sx * 2.1, 29.2, -5.4], [1.8, 1.8, 0.3], M["g_plague"])
    m.segments("beak", h, [0, 27.4, -4.6], [(3.0, 3.2, [-100, 0, 0]), (2.6, 2.2, [-12, 0, 0]), (2.0, 1.2, [-14, 0, 0])],
               mask, up=True)
    m.box(BODY, [0, 23.4, 0], [10.2, 2.4, 6.0], M["black"])
    tunic(m, M["olive"], deco=glyph("bio", "#B8E83A", glow_=True), back=glyph("bio", "#6A7A2A"))
    straps(m, M["leather_black"], cross=False, buckle=M["brass"])
    belt(m, M["leather_black"], buckle=M["brass"])
    m.box(BODY, [3.4, 10.4, -2.6], [2.0, 2.4, 2.0], M["brass"])
    m.box(BODY, [3.4, 10.4, -2.6], [1.4, 1.8, 2.2], M["g_plague"])
    bracers(m, M["leather_black"], M["olive"], long=True)
    leg_flaps(m, M["black"], length=9)
    boots(m, M["leather_black"], toe=True, tall=True)
    cape(m, M["black"], length=16, inner=M["olive"])
    return m


def astromancer():
    """Астромант: золотой обруч со звездой и три светила над головой, тёмно-синее звёздное одеяние и плащ."""
    m = base("astromancer")
    sleeves(m, M["midnight"], flare=True)
    hood(m, M["midnight"], M["gold"], tip=False)
    circlet(m, M["gold"], gem=M["g_ice"], y=31.6)
    h = HEAD
    for i, (x, y, z, s) in enumerate(((-4.0, 36.4, 1.0, 1.4), (0.4, 38.0, -0.4, 1.9), (4.2, 36.0, 0.6, 1.2))):
        m.box(h, [x, y, z], [s, s, s], M["g_soul"], rot=[45, 45, 0], pivot=[x, y, z])
    m.box(BODY, [0, 23.4, 0], [10.2, 2.0, 5.8], M["gold"])
    tunic(m, M["midnight"], deco=both(stars("#BFF0FF", 10), glyph("star", "#FFE68A", glow_=True, cy=0.34)))
    belt(m, M["gold"], buckle=M["g_ice"])
    bracers(m, M["midnight"], M["gold"])
    leg_flaps(m, M["midnight"], length=9, deco=both(stars("#BFF0FF", 6, 9), trim_edges("#E0B040", top=False)))
    boots(m, M["leather_dark"], cuff=M["gold"])
    cape(m, M["midnight"], length=19, inner=M["navy"], deco=stars("#BFF0FF", 22, 4), clasp=M["gold"], tattered=False)
    return m


def summoner():
    """Призыватель: корона из костей поверх бледного капюшона, черепа на плечах, фонарь душ на поясе."""
    m = base("summoner")
    sleeves(m, M["grey"], flare=True)
    hood(m, M["ash"], M["grey"], tip=True)
    h = HEAD
    for i in range(7):
        x = -3.9 + i * 1.3
        ln = 2.6 + (1.6 if i == 3 else 0.8 if i in (2, 4) else 0)
        m.box(h, [x, 32.6 + ln / 2, -4.6], [0.9, ln, 0.9], M["bone"], rot=[-8, 0, -x * 4], pivot=[x, 32.6, -4.6])
    m.box(h, [0, 32.6, -4.6], [9.6, 1.0, 1.0], M["bone"])
    from crypt import skull_face
    sk = lm("sm_skull", "#DCD2BA", "bone", dark="#968A70", light="#FFF6DE")
    for bone, sx in SIDES:
        m.box(bone, [sx * 6.4, 24.4, 0], [5.4, 1.6, 5.4], M["leather_black"])
        m.box(bone, [sx * 6.6, 26.6, -0.2], [3.6, 3.4, 3.6], sk, decal={"north": skull_face}, rot=[0, sx * 20, 0],
              pivot=[sx * 6.6, 26.6, 0])
    tunic(m, M["grey"], deco=glyph("skull", "#BFE8FF", glow_=True, cy=0.36))
    for k in range(3):
        m.box(BODY, [0, 20.6 - k * 1.8, -2.65], [6.6 - k, 0.7, 0.4], M["bone"])
    belt(m, M["leather_black"], buckle=M["bone"])
    m.box(BODY, [-3.4, 10.0, -2.6], [2.2, 3.0, 2.2], M["iron_black"])
    m.box(BODY, [-3.4, 10.0, -2.6], [1.6, 2.4, 2.4], M["g_soul"])
    bracers(m, M["leather_black"], M["bone"])
    leg_flaps(m, M["ash"], length=8)
    boots(m, M["leather_black"], cuff=M["bone"])
    cape(m, M["grey"], length=18, inner=M["black"], clasp=M["bone"])
    return m


def illusionist():
    """Иллюзионист: шляпа с двумя загнутыми концами, маска с сиянием, высокий ворот, двухцветное одеяние и плащ."""
    m = base("illusionist")
    h = HEAD
    m.box(h, [-2.4, 32.8, 0.2], [4.9, 2.4, 9.6], M["lilac"])
    m.box(h, [2.4, 32.8, 0.2], [4.9, 2.4, 9.6], M["violet"])
    m.box(h, [0, 31.6, 0.2], [10.0, 1.2, 9.9], M["gold"])
    m.segments("hat_r", h, [-3.0, 33.6, 0.4], [(3.0, 3.0, [0, 0, -40]), (2.6, 2.2, [0, 0, -40]), (2.2, 1.4, [0, 0, -50])],
               M["lilac"])
    m.segments("hat_l", h, [3.0, 33.6, 0.4], [(3.0, 3.0, [0, 0, 40]), (2.6, 2.2, [0, 0, 40]), (2.2, 1.4, [0, 0, 50])],
               M["violet"])
    m.box(h, [-6.6, 37.6, 0.4], [1.4, 1.4, 1.4], M["g_gold"])
    m.box(h, [6.6, 37.6, 0.4], [1.4, 1.4, 1.4], M["g_gold"])
    mask = lm("il_mask", "#F0ECF6", "plain", dark="#B0A8C0", light="#FFFFFF")
    m.box(h, [0, 28.6, -4.75], [8.8, 2.8, 0.6], mask, decal={"north": visor("#D6A6FF", "slit")})
    b = BODY
    m.box(b, [0, 25.2, 0.6], [9.8, 3.6, 5.6], M["violet"], skip=("up", "down"))
    m.box(b, [-2.25, 18.2, 0], [4.5, 11.6, 4.9], M["lilac"], skip=("up", "down"))
    m.box(b, [2.25, 18.2, 0], [4.5, 11.6, 4.9], M["violet"], skip=("up", "down"),
          decal={"north": glyph("eye", "#D6A6FF", glow_=True, cy=0.36)})
    belt(m, M["gold"], buckle=M["g_purple"])
    m.box(RA, [-6, 18.2, 0], [4.55, 11.4, 4.55], M["violet"], skip=("up",))
    m.box(LA, [6, 18.2, 0], [4.55, 11.4, 4.55], M["lilac"], skip=("up",))
    m.box(RA, [-6, 14.6, 0], [4.7, 4.6, 4.7], M["violet"])
    m.box(LA, [6, 14.6, 0], [4.7, 4.6, 4.7], M["lilac"])
    m.box(RA, [-6.3, 24.4, 0], [5.4, 1.8, 5.4], M["gold"])
    m.box(LA, [6.3, 24.4, 0], [5.4, 1.8, 5.4], M["gold"])
    m.box(RL, [-1.95, 9.0, 0.05], [4.6, 6.4, 4.9], M["violet"], skip=("up",), decal={"north": tatter, "south": tatter})
    m.box(LL, [1.95, 9.0, 0.05], [4.6, 6.4, 4.9], M["lilac"], skip=("up",), decal={"north": tatter, "south": tatter})
    boots(m, M["leather_black"], cuff=M["gold"])
    cape(m, M["violet"], length=17, inner=M["lilac"], deco=glyph("eye", "#D6A6FF", cy=0.5, scale=2), clasp=M["gold"])
    return m


def warlock():
    """Чернокнижник: чёрный капюшон с рогами, багровые руны пакта, книга на поясе, шипастые наплечники, рваный плащ."""
    m = base("warlock")
    sleeves(m, M["black"], flare=True)
    hood(m, M["black"], M["crimson"], tip=False, low=True)
    horns(m, lm("wl_horn", "#3A2A2A", "bone", dark="#1A1010", light="#6A5050"), y=31.6, x=4.6, big=1.1)
    pauldrons(m, M["iron_black"], M["crimson"], size=1.0, layers=2, spikes=2, spike_mat=M["bone"])
    tunic(m, M["black"], deco=both(cracks("#FF3A3A", 3, 21), glyph("pentacle", "#FF4A3A", glow_=True)))
    belt(m, M["leather_black"], buckle=M["g_red"])
    m.box(BODY, [3.4, 10.4, -2.4], [2.6, 3.4, 1.6], M["leather_black"])
    m.box(BODY, [3.4, 10.4, -3.25], [1.4, 1.6, 0.2], M["g_red"])
    bracers(m, M["iron_black"], M["g_red"])
    leg_flaps(m, M["crimson"], length=7)
    boots(m, M["leather_black"], cuff=M["iron_black"])
    cape(m, M["crimson"], length=18, inner=M["black"], clasp=M["g_red"])
    return m


def rifter():
    """Разломщик: фиолетовый капюшон, светящиеся осколки вокруг головы, трещины пустоты на одеянии и плаще."""
    m = base("rifter")
    sleeves(m, M["violet"], flare=True)
    hood(m, M["purple"], M["void"], tip=True)
    h = HEAD
    for i, (x, y, z, ln, r) in enumerate(((-6.0, 31.0, -1.0, 3.0, 25), (6.2, 32.0, 0.4, 2.6, -30), (-3.6, 36.0, 1.6, 2.2, 10),
                                         (3.0, 35.4, -1.8, 2.0, -15))):
        m.box(h, [x, y, z], [1.2, ln, 1.2], M["g_purple"], rot=[r, 45, r], pivot=[x, y, z])
        m.box(h, [x, y, z], [1.7, ln * 0.6, 1.7], M["void"], rot=[r, 45, r], pivot=[x, y, z])
    for bone, sx in SIDES:
        m.box(bone, [sx * 6.3, 24.4, 0], [5.4, 1.8, 5.4], M["void"], decal={"north": cracks("#C08AFF", 2, 3)})
        m.box(bone, [sx * 7.6, 26.4, 0.4], [1.2, 3.2, 1.2], M["g_purple"], rot=[0, 45, sx * 25], pivot=[sx * 7, 25, 0])
    tunic(m, M["violet"], deco=both(cracks("#C08AFF", 4, 7), glyph("rift", "#E0C0FF", glow_=True)))
    belt(m, M["void"], buckle=M["g_purple"])
    bracers(m, M["void"], M["g_purple"])
    leg_flaps(m, M["purple"], length=8, deco=both(cracks("#C08AFF", 2, 13), tatter))
    boots(m, M["leather_black"], cuff=M["void"])
    cape(m, M["void"], length=18, inner=M["purple"], deco=cracks("#C08AFF", 5, 17))
    return m


def cleric():
    """Лекарь: белый капюшон с золотым крестом, золотая пелерина, белое одеяние, сумка с бинтами."""
    m = base("cleric")
    sleeves(m, M["white"], flare=True)
    hood(m, M["white"], M["gold"], tip=False, mantle=M["cream"])
    h = HEAD
    m.box(h, [0, 33.4, -4.6], [1.0, 2.8, 0.6], M["g_gold"])
    m.box(h, [0, 33.8, -4.6], [2.8, 0.9, 0.6], M["g_gold"])
    tunic(m, M["white"], deco=glyph("cross", "#E0B040"), back=glyph("cross", "#E0B040"))
    belt(m, M["leather"], buckle=M["gold"])
    m.box(BODY, [-3.4, 11.2, -1.0], [2.4, 3.2, 3.2], M["leather"])
    m.box(BODY, [-3.4, 11.4, -2.65], [1.4, 1.4, 0.2], M["red"])
    m.box(BODY, [0, 18.6, -2.55], [1.2, 14.4, 0.5], M["leather"], rot=[0, 0, -33], pivot=[0, 18.6, -2.55])
    bracers(m, M["white"], M["gold"])
    leg_flaps(m, M["white"], length=9, deco=both(trim_edges("#E0B040", top=False), tatter))
    boots(m, M["leather"], cuff=M["gold"])
    return m


def chronolog():
    """Хронолог: шляпа с часовой лентой, золотой монокль, часы с горящими стрелками за спиной, песочные часы на поясе."""
    m = base("chronolog")
    sleeves(m, M["teal"], cuff=M["gold"])
    wide_hat(m, M["teal"], M["gold"], brim=12.4, crown=5.0, band_deco=glyph("hourglass", "#FFE68A", cy=0.5))
    h = HEAD
    m.box(h, [2.0, 28.4, -4.7], [2.6, 2.6, 0.6], M["gold"])
    m.box(h, [2.0, 28.4, -5.05], [1.8, 1.8, 0.2], M["g_gold"])
    m.box(h, [3.6, 26.4, -4.6], [0.4, 3.6, 0.4], M["gold"])
    b = BODY
    m.box(b, [0, 19.0, 3.4], [10.0, 10.0, 0.9], M["gold"], rot=[0, 0, 45], pivot=[0, 19, 3.4])
    m.box(b, [0, 19.0, 3.5], [11.2, 11.2, 0.6], M["brass"])
    m.box(b, [0, 19.0, 3.95], [9.0, 9.0, 0.4], M["cream"], decal={"south": glyph("sun", "#6A5020", cy=0.5)})
    m.box(b, [0, 21.0, 4.3], [0.6, 4.0, 0.4], M["g_gold"])
    m.box(b, [1.2, 19.0, 4.3], [2.6, 0.6, 0.4], M["g_gold"])
    tunic(m, M["teal"], deco=both(buttons("#E0B040", 4), trim_edges("#E0B040")))
    m.box(BODY, [0, 23.4, 0], [10.2, 2.0, 5.8], M["gold"])
    belt(m, M["leather_dark"], buckle=M["gold"])
    m.box(b, [3.4, 10.4, -2.6], [2.0, 0.6, 2.0], M["gold"])
    m.box(b, [3.4, 8.0, -2.6], [2.0, 0.6, 2.0], M["gold"])
    m.box(b, [3.4, 9.2, -2.6], [1.4, 1.8, 1.4], lm("sand_glow", "#F0D27A", "crystal", glow="pattern",
                                                    dark="#B08A30", light="#FFF4C0"))
    bracers(m, M["teal"], M["gold"])
    leg_flaps(m, M["teal"], length=8, deco=both(trim_edges("#E0B040", top=False), tatter))
    boots(m, M["leather_dark"], cuff=M["gold"])
    return m


def shadow_priest():
    """Теневой жрец: глубокий тёмно-фиолетовый капюшон с серебряным полумесяцем, вуаль, плащ сумерек."""
    m = base("shadow_priest")
    sleeves(m, M["violet"], flare=True)
    hood(m, M["violet"], M["silver"], low=True, mantle=M["black"])
    h = HEAD
    m.box(h, [0, 34.6, -4.4], [4.4, 4.4, 0.6], M["g_purple"], decal={"north": glyph("moon", "#F4EEFF", glow_=True, cy=0.5)})
    m.box(h, [0, 34.6, -4.2], [5.0, 5.0, 0.4], M["silver"], rot=[0, 0, 45], pivot=[0, 34.6, -4.2])
    veil = lm("veil", "#2A1A3E", "cloth", dark="#120A1E", light="#4E3A70")
    m.box(h, [0, 25.8, -0.1], [9.5, 3.4, 9.3], veil, skip=("up", "down"), decal={"north": tatter})
    tunic(m, M["violet"], deco=glyph("moon", "#C08AFF", glow_=True, cy=0.38), back=glyph("eye", "#5A3A80"))
    belt(m, M["silver"], buckle=M["g_purple"])
    bracers(m, M["black"], M["silver"])
    leg_flaps(m, M["violet"], length=9)
    boots(m, M["leather_black"], cuff=M["silver"])
    cape(m, M["black"], length=18, inner=M["violet"], deco=stars("#C08AFF", 10, 7), clasp=M["silver"])
    return m


def druid():
    """Друид: ветвистые рога из дерева и венок из листьев с цветами, моховые наплечники, одеяние цвета коры."""
    m = base("druid")
    sleeves(m, M["olive"])
    hood(m, M["green"], M["bark"], tip=False)
    h = HEAD
    for sx, nm in ((-1, "antler_r"), (1, "antler_l")):
        bones = m.segments(nm, h, [sx * 3.2, 32.0, 0.4], [(2.6, 1.4, [10, 0, sx * 30]), (2.6, 1.2, [10, 0, sx * 10]),
                                                          (2.4, 1.0, [15, 0, -sx * 20]), (2.0, 0.8, [10, 0, -sx * 15])],
                           M["bark"])
        m.box(bones[1], [sx * 3.2, 34.4, 0.4], [3.0, 0.9, 0.9], M["bark"], rot=[0, 0, sx * 60],
              pivot=[sx * 3.2, 34.2, 0.4])
        m.box(bones[2], [sx * 3.2, 36.6, 0.4], [2.4, 0.8, 0.8], M["bark"], rot=[0, 0, -sx * 50], pivot=[sx * 3.2, 36.4, 0.4])
    for i in range(9):
        a = math.radians(i * 40)
        x, z = math.sin(a) * 4.9, math.cos(a) * 4.9
        m.box(h, [x, 31.8, z], [2.2, 1.6, 1.0], M["moss"], rot=[0, i * 40, 0], pivot=[x, 31.8, z])
        if i % 3 == 0:
            m.box(h, [x * 1.04, 32.4, z * 1.04], [0.9, 0.9, 0.9], glow("flower", "#FF9AD8", "#FFF0FA", "#D04A90"))
    for bone, sx in SIDES:
        m.box(bone, [sx * 6.4, 24.6, 0], [5.6, 2.4, 5.6], M["moss"])
        m.box(bone, [sx * 6.4, 23.0, 0], [5.2, 1.6, 5.2], M["bark"])
        m.box(bone, [sx * 7.0, 26.0, -1.2], [1.0, 1.0, 1.0], M["g_green"])
    tunic(m, M["olive"], deco=glyph("leaf", "#8AFF96", glow_=True))
    m.box(BODY, [0, 18.6, -2.55], [1.0, 14.4, 0.5], M["moss"], rot=[0, 0, 33], pivot=[0, 18.6, -2.55])
    belt(m, M["bark"], buckle=M["g_green"])
    bracers(m, M["bark"], M["moss"])
    leg_flaps(m, M["green"], length=8)
    boots(m, M["bark"], cuff=M["moss"])
    cape(m, M["moss"], length=16, inner=M["green"])
    return m


def shaman():
    """Шаман: череп зверя на голове с перьями, меховой ворот, ожерелье из клыков, костяные наручи, полы из шкур."""
    m = base("shaman")
    h = HEAD
    m.box(h, [0, 29.0, 0.6], [9.8, 8.0, 9.4], M["fur"], skip=("north", "down"))
    sk = lm("sh_skull", "#E8DCC0", "bone", dark="#A0927A", light="#FFF8E8")
    m.box(h, [0, 32.8, -2.4], [7.0, 2.8, 6.2], sk)
    m.box(h, [0, 32.2, -6.0], [4.0, 2.4, 3.0], sk, decal={"north": visor("#7BF5A0", "slit")})
    for sx in (-1, 1):
        m.segments(f"skull_horn{sx + 1}", h, [sx * 3.4, 33.8, -1.6], [(2.2, 1.4, [20, 0, sx * 60]),
                                                                       (2.0, 1.0, [25, 0, -sx * 30]),
                                                                       (1.6, 0.7, [20, 0, -sx * 20])], sk)
    for i, (x, col) in enumerate(((-2.4, "red"), (0, "teal"), (2.4, "cream"))):
        m.segments(f"feather{i}", h, [x, 33.6, 3.2], [(2.6, 1.0, [25, 0, -x * 5]), (2.6, 0.8, [15, 0, 0]),
                                                      (2.0, 0.6, [10, 0, 0])], M[col])
    fur_mantle(m, M["fur"])
    for i in range(7):
        a = math.radians(-60 + i * 20)
        x, y = math.sin(a) * 3.6, 22.2 - math.cos(a) * 3.6
        m.box(BODY, [x, y - 0.6, -2.8], [0.8, 1.8, 0.6], M["bone"] if i != 3 else M["g_green"], rot=[0, 0, -i * 20 + 60],
              pivot=[x, y, -2.8])
    tunic(m, M["leather"], deco=glyph("totem", "#7BF5A0", glow_=True, cy=0.5))
    belt(m, M["leather_dark"], buckle=M["bone"], pouches=[(-3.0, M["fur"])])
    for bone, sx in SIDES:
        for k in range(2):
            m.box(bone, [sx * 6, 13.4 + k * 2.4, 0], [4.8, 1.4, 4.8], M["bone"])
    leg_flaps(m, M["fur"], length=7)
    boots(m, M["leather"], fur=M["fur"])
    return m


def bannerman():
    """Знаменосец: открытый шлем с красным плюмажем, налатник со знаменем, древко со стягом за спиной."""
    m = base("bannerman")
    sleeves(m, M["chain"], upper=True)
    pants(m, M["chain"])
    helm(m, M["steel"], M["steel_dark"], face="open", crest="plume", crest_mat=M["red"])
    pauldrons(m, M["steel"], M["red"], size=1.05, layers=2)
    tunic(m, M["red"], deco=glyph("banner", "#FFE0A0"), back=trim_edges("#E0B040"))
    belt(m, M["leather_dark"], buckle=M["gold"])
    bracers(m, M["steel"], M["red"])
    greaves(m, M["steel"])
    boots(m, M["steel_dark"], cuff=M["red"])
    b = BODY
    m.box(b, [-2.6, 26.0, 3.4], [1.0, 30.0, 1.0], M["wood"])
    m.box(b, [-2.6, 41.4, 3.4], [1.8, 1.8, 1.8], M["gold"], rot=[45, 45, 0], pivot=[-2.6, 41.4, 3.4])
    m.box(b, [0.6, 38.8, 3.4], [7.4, 0.8, 0.8], M["wood"])
    m.box(b, [-2.6, 13.0, 3.4], [2.2, 2.6, 1.6], M["leather_dark"])
    m.bone("cape_flag", [0.6, 38.4, 3.4], b)
    m.box("cape_flag", [0.6, 33.6, 3.4], [7.0, 9.6, 0.4], M["red"],
          decal={"south": both(glyph("banner", "#FFE0A0", cy=0.42), tatter), "north": tatter})
    return m


# ============================================================================ облик по роли (до выбора класса, ур. 1–9)
# Скромнее классовых: снаряжение новичка — кожа, простое железо, без свечения. Облик класса на 10 уровне — награда.

def role_tank():
    """Новобранец-танк: открытый железный шлем, один наплечник, стёганка с синим щитом, круглый щит за спиной."""
    m = base("role_tank")
    sleeves(m, M["chain"], upper=True)
    pants(m, M["leather_dark"])
    helm(m, M["steel"], M["steel_dark"], face="open")
    pauldrons(m, M["steel"], M["blue"], size=0.95, layers=2, only=LA)
    tunic(m, M["cream"], deco=glyph("shield", "#3E6CB8"), back=trim_edges("#3E6CB8"))
    belt(m, M["leather_dark"], buckle=M["steel"])
    bracers(m, M["leather"], M["steel"])
    boots(m, M["leather_dark"], cuff=M["steel"])
    b = BODY
    for i in range(8):
        a = math.radians(i * 45)
        x, y = math.cos(a) * 3.4, 17.4 + math.sin(a) * 3.4
        m.box(b, [x, y, 3.4], [2.8, 2.8, 0.8], M["wood"], rot=[0, 0, i * 45], pivot=[x, y, 3.4])
    m.box(b, [0, 17.4, 3.45], [6.6, 6.6, 0.85], M["wood"], decal={"south": glyph("cross", "#3E6CB8", cy=0.5)})
    m.box(b, [0, 17.4, 4.0], [2.0, 2.0, 0.6], M["steel"])
    return m


def role_damage():
    """Новобранец-боец: красная бандана с хвостами, кожаная куртка, кушак, наручи, короткий клинок за спиной."""
    m = base("role_damage")
    sleeves(m, M["leather"])
    pants(m, M["leather_dark"])
    h = HEAD
    m.box(h, [0, 30.4, 0], [8.8, 2.4, 8.8], M["red"], skip=("down",))
    for i, (dx, r) in enumerate(((0.9, 18), (2.0, 34))):
        m.box(h, [dx, 28.6, 4.75], [1.2, 3.4, 0.4], M["red"], rot=[r, 0, 8 + i * 12], pivot=[dx, 30.2, 4.55])
    tunic(m, M["leather"], deco=glyph("axes", "#C8A070"))
    belt(m, M["red"], buckle=M["brass"], pouches=[(3.0, M["leather_dark"])])
    straps(m, M["leather_dark"], cross=False, buckle=M["brass"])
    bracers(m, M["leather_dark"], M["brass"])
    boots(m, M["leather_dark"], cuff=M["leather"])
    b = BODY
    pv = [-0.4, 18.0, 3.2]
    m.box(b, [-0.4, 20.4, 3.2], [1.2, 9.0, 0.5], M["steel"], rot=[0, 0, 35], pivot=pv)
    m.box(b, [-0.4, 15.6, 3.3], [3.4, 0.8, 0.8], M["brass"], rot=[0, 0, 35], pivot=pv)
    m.box(b, [-0.4, 13.8, 3.3], [1.0, 3.0, 1.0], M["leather_dark"], rot=[0, 0, 35], pivot=pv)
    return m


def role_support():
    """Послушник-поддержка: светлый капюшон с зелёной каймой, роба до колен, сумка с травами на боку."""
    m = base("role_support")
    sleeves(m, M["cream"], cuff=M["green"])
    hood(m, M["cream"], M["green"], tip=True)
    tunic(m, M["cream"], deco=glyph("cross", "#4FB060"), long=True)
    belt(m, M["leather"], buckle=M["brass"], pouches=[(-3.0, M["leather_dark"])])
    straps(m, M["leather"], cross=False)
    leg_flaps(m, M["cream"], length=6.5, deco=trim_edges("#4FB060", top=False))
    boots(m, M["leather"])
    b = BODY
    m.box(b, [2.6, 13.6, 3.3], [4.4, 4.0, 2.0], M["leather"])
    m.box(b, [2.6, 15.8, 3.3], [4.6, 0.8, 2.2], M["leather_dark"])
    for i, (dx, col) in enumerate(((-0.9, "green"), (0.5, "moss"), (1.6, "olive"))):
        m.box(b, [2.6 + dx, 16.9 + i * 0.3, 3.3], [0.8, 2.0, 0.8], M[col], rot=[0, 0, -15 + i * 15],
              pivot=[2.6 + dx, 16.2, 3.3])
    return m


MODELS = {f"look_{fn.__name__}": fn for fn in (
    role_tank, role_damage, role_support,
    guardian, blood_knight, mirror, graviton, bastion, paladin, berserker, swift, monk, shadow, reaper, harpooner,
    headsman, sniper, mechanic, pyromancer, cryomancer, stormcaller, plague_lord, astromancer, summoner, illusionist,
    warlock, rifter, cleric, chronolog, shadow_priest, druid, shaman, bannerman)}
