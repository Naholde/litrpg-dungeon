"""Объекты и призывы классов (тотемы, знамёна, древо, печать, плаха, капкан, турель, мина, светило, щупальце,
двойники, разлом, оберег купола; скелет, тень-волк, костяной голем), а также торговец, Испытатель Системы,
узел силы и тёмная свеча."""
import math
import hy
from hy import Model, mat, kf, loop, once, sway, beats
from rig import H, humanoid, motions, Persona, cloth_sway, features
from crypt import rivets, tatter, plate_rows, skull_face
from crypt_mobs import skeleton

mat("totem_wood", "#7A5A3A", "wood", dark="#3E2C1A", light="#AA8458")
mat("paint_red", "#B8302A", "plain", dark="#7A1A14", light="#E8604A")
mat("paint_white", "#E8E0D0", "plain", dark="#B0A898", light="#FFFFFF")
mat("heal_glow", "#7BF5A0", "glow", glow=True, light="#F0FFF4", dark="#2AB060")
mat("storm_glow", "#B8C6FF", "glow", glow=True, light="#FFFFFF", dark="#5A70E0")
mat("earth_glow", "#E8B860", "glow", glow=True, light="#FFF4D0", dark="#B07A20")
mat("stone_t", "#7A7470", "stone", dark="#45403C", light="#A8A29C")
mat("banner_red", "#A8262E", "cloth", dark="#5A0E14", light="#D8505A")
mat("banner_blue", "#2A4E9A", "cloth", dark="#12244E", light="#5A80CC")
mat("banner_teal", "#2A9A8A", "cloth", dark="#10504A", light="#5ACCBC")
mat("pole", "#5A3E26", "wood", dark="#2E1E10", light="#86603E")
mat("tree_bark", "#6A5034", "bark", dark="#36261A", light="#94744E")
mat("tree_leaf", "#5ACC6A", "moss", dark="#2A7A3A", light="#9AF0A8")
mat("seal_gold", "#E0B040", "metal", dark="#8A6418", light="#FFF0A0", gloss=0.8)
mat("seal_glow", "#FFD27A", "glow", glow=True, light="#FFFFFF", dark="#E09A20")
mat("block_wood", "#5A3A26", "wood", dark="#2E1C10", light="#86583A")
mat("blood", "#6A1214", "plain", dark="#3A0808", light="#9A2024")
mat("trap_iron", "#8A929E", "metal", dark="#4A5058", light="#D0D8E2", gloss=0.5)
mat("brass_t", "#B88A3A", "metal", dark="#6A4A18", light="#F0C878", gloss=0.6)
mat("blink", "#FF7A2E", "glow", glow=True, light="#FFE0C0", dark="#C03A08")
mat("star_core", "#BFF0FF", "glow", glow=True, light="#FFFFFF", dark="#5AC8F0")
mat("abyss", "#2A1A3E", "skin", dark="#120A1E", light="#5A3A80")
mat("abyss_glow", "#B07AFF", "glow", glow=True, light="#F4E8FF", dark="#6A2AD0")
mat("ghost", "#C8A6F0", "glow", glow=True, light="#F6EEFF", dark="#7A50B0")
mat("mirror_p", "#C8E8FF", "metal", dark="#6A90B0", light="#FFFFFF", gloss=1.0)
mat("dome_c", "#8FE3FF", "crystal", glow="pattern", dark="#3A90C0", light="#F0FCFF")
mat("wolf_shade", "#1A1424", "fur", dark="#08060C", light="#3E3250")
mat("wolf_eye", "#B07AFF", "glow", glow=True, light="#FFFFFF", dark="#6A2AD0")
mat("trader_robe", "#3A2E5A", "cloth", dark="#1C1630", light="#5E4C8A")
mat("trader_trim", "#C9A04A", "metal", dark="#7A5A18", light="#F0D088", gloss=0.6)
mat("trader_skin", "#C69C7A", "skin", dark="#8A6A50", light="#EAC4A0")
mat("lantern_t", "#FFD27A", "glow", glow=True, light="#FFFFFF", dark="#E09A20")
mat("sys_slate", "#3A4150", "runes", dark="#1E222A", light="#5A6478", glow="pattern", accent="#5FC8FF")
mat("sys_bronze", "#9C6B2E", "metal", dark="#5A3C18", light="#D8A060", gloss=0.5)
mat("sys_crystal", "#3FD8FF", "crystal", glow=True, dark="#1A88B0", light="#E8FCFF")
mat("candle_wax", "#3A3442", "plain", dark="#1C1822", light="#5A5266")
mat("candle_flame", "#9B5CFF", "glow", glow=True, light="#F0E0FF", dark="#5A20C0")


def carved_face(color):
    def f(face):
        w, h = face.w, face.h
        for sx in (int(w * 0.2), int(w * 0.6)):
            face.rect(sx, int(h * 0.3), sx + 3, int(h * 0.3) + 2, color, glow=True)
        face.rect(int(w * 0.3), int(h * 0.62), int(w * 0.7), int(h * 0.78), "#1A120A")
        face.rect(0, 0, w, 1, "#B8302A")
        face.rect(0, h - 1, w, h, "#E8E0D0")
    return f


def emblem(kind, col):
    def f(face):
        w, h = face.w, face.h
        cx, cy = w // 2, int(h * 0.42)
        if kind == "sword":
            face.rect(cx - 1, cy - 6, cx + 1, cy + 6, col)
            face.rect(cx - 4, cy + 2, cx + 4, cy + 3, col)
        elif kind == "shield":
            for y in range(10):
                half = 5 - max(0, y - 5)
                face.rect(cx - half, cy - 5 + y, cx + half, cy - 4 + y, col)
        else:
            for i in range(6):
                face.rect(cx - 6 + i, cy - 3 + i, cx - 4 + i, cy - 2 + i, col)
                face.rect(cx + 4 - i, cy - 3 + i, cx + 6 - i, cy - 2 + i, col)
        tatter(face)
    return f


def idle_only(m, bones, T=2.0):
    m.anim("idle", loop(T, bones))


# ---------------------------------------------------------------------------- тотемы

def totem(name, glow, top):
    m = Model(name)
    b = m.bone("pole", [0, 0, 0])
    m.box(b, [0, 1, 0], [6, 2, 6], "stone_t")
    for i in range(3):
        m.box(b, [0, 5 + i * 5.2, 0], [4.6 - i * 0.4, 5, 4.6 - i * 0.4], "totem_wood", decal={"north": carved_face(hy.MATS[glow] and {"heal_glow": "#7BF5A0", "storm_glow": "#B8C6FF", "earth_glow": "#E8B860"}[glow])})
    for sx in (-1, 1):
        m.box(b, [3.4 * sx, 15, 0], [2.4, 1.2, 1.6], "totem_wood", rot=[0, 0, -20 * sx])          # крылья-перекладины
        m.box(b, [4.6 * sx, 14, 0], [0.8, 3, 0.8], "paint_red")
    t = m.bone("crown", [0, 18, 0], b)
    top(m, t)
    for c in m.by["crown"].cubes:
        c.origin[1] -= 3.2
    idle_only(m, {"crown": {"rotation": kf([(0, [0, 0, 0]), (4, [0, 360, 0])], smooth=False), "position": sway(2, 0.3, 0, 1)}}, 4.0)
    return m


def totem_heal():
    """Тотем исцеления: резной столб с ликами, наверху — зелёный огонёк в перьях."""
    def top(m, t):
        m.box(t, [0, 21.4, 0], [2.4, 2.4, 2.4], "heal_glow")
        for a in range(0, 360, 90):
            r = math.radians(a)
            m.box(t, [math.cos(r) * 1.8, 22, math.sin(r) * 1.8], [0.6, 2.6, 1.2], "paint_white", rot=[0, -a, 20])
    return totem("totem_heal", "heal_glow", top)


def totem_storm():
    """Тотем бури: столб с синими ликами, наверху — кристалл с искрами, кольцо медных стержней."""
    def top(m, t):
        m.box(t, [0, 22, 0], [1.8, 4, 1.8], "storm_glow", rot=[0, 45, 0])
        for a in range(0, 360, 120):
            r = math.radians(a)
            m.box(t, [math.cos(r) * 2.2, 21.6, math.sin(r) * 2.2], [0.5, 3, 0.5], "brass_t")
    return totem("totem_storm", "storm_glow", top)


def totem_earth():
    """Тотем земли: каменный столб, наверху — валун с янтарной жилой."""
    def top(m, t):
        m.box(t, [0, 21.8, 0], [4, 3.4, 4], "stone_t")
        m.box(t, [0, 21.8, -2.05], [1.6, 2.4, 0.4], "earth_glow")
    return totem("totem_earth", "earth_glow", top)


# ---------------------------------------------------------------------------- знамёна

def banner(name, cloth, kind, col):
    m = Model(name)
    p = m.bone("pole", [0, 0, 0])
    m.box(p, [0, 15, 0], [1.2, 30, 1.2], "pole")
    m.box(p, [0, 30.6, 0], [1.8, 1.4, 1.8], "seal_gold")
    m.box(p, [0, 27.4, 0], [10, 0.8, 0.8], "pole")
    m.box(p, [0, 0.6, 0], [3, 1.2, 3], "stone_t")
    c = m.bone("cloth", [0, 27, 0.6], p)
    m.box(c, [0, 20, 0.7], [9, 14, 0.4], cloth, decal={"north": emblem(kind, col), "south": emblem(kind, col)})
    idle_only(m, {"cloth": {"rotation": sway(2.4, 6, 0, 0, [4, 0, 0])}}, 2.4)
    return m


def banner_power():
    return banner("banner_power", "banner_red", "sword", "#F0D088")


def banner_guard():
    return banner("banner_guard", "banner_blue", "shield", "#E8E8F0")


def banner_speed():
    return banner("banner_speed", "banner_teal", "wings", "#F0FFF8")


# ---------------------------------------------------------------------------- прочие объекты

def tree_life():
    """Живое древо друида: изогнутый ствол, светящаяся крона, плоды-огоньки, корни."""
    m = Model("tree_life")
    t = m.bone("trunk", [0, 0, 0])
    m.segments("st", t, [0, 0, 0], [(8, 3.4, [0, 0, 6]), (8, 3.0, [0, 0, -12]), (6, 2.6, [0, 0, 10])], "tree_bark")
    for a in range(0, 360, 72):
        r = math.radians(a)
        m.box(t, [math.cos(r) * 2.6, 0.8, math.sin(r) * 2.6], [3.4, 1.4, 1.2], "tree_bark", rot=[0, -a, -20])
    cr = m.bone("crown", [0, 22, 0], "st2")
    for i, (x, y, z, s) in enumerate(((0, 26, 0, 10), (-4, 24, 2, 6), (4, 24.6, -2, 6.4), (0, 29, -1, 6), (2, 23, 4, 5))):
        m.box(cr, [x, y, z], [s, s * 0.7, s], "tree_leaf")
    for i in range(5):
        m.box(cr, [-4 + i * 2, 21.6 + (i % 2), -3 + (i % 3) * 3], [1.2, 1.2, 1.2], "heal_glow")
    idle_only(m, {"crown": {"rotation": sway(3, 3, 0, 2), "scale": sway(3, 0.03, 0, 0, [1, 1, 1])}}, 3.0)
    return m


def monk_seal():
    """Печать монаха: золотой диск с руной на земле, медленно вращается."""
    m = Model("monk_seal")
    s = m.bone("seal", [0, 0, 0])
    m.box(s, [0, 0.3, 0], [14, 0.4, 14], "seal_gold", rot=[0, 45, 0])
    m.box(s, [0, 0.5, 0], [12, 0.4, 12], "seal_glow", decal={"up": lambda f: f.rect(2, 2, f.w - 2, f.h - 2, "#B8862A")})
    m.box(s, [0, 0.8, 0], [4, 0.6, 4], "seal_glow")
    idle_only(m, {"seal": {"rotation": kf([(0, [0, 0, 0]), (6, [0, 360, 0])], smooth=False)}}, 6.0)
    return m


def headsman_block():
    """Плаха палача: дубовая колода в крови, топор вогнан в дерево."""
    m = Model("headsman_block")
    b = m.bone("block", [0, 0, 0])
    m.box(b, [0, 3, 0], [14, 6, 10], "block_wood", decal={"up": lambda f: [f.rect(int(f.w * 0.4), y, int(f.w * 0.6), y + 1, "#6A1214") for y in range(f.h)]})
    m.box(b, [0, 6.1, 0], [3, 0.2, 9], "blood")
    ax = m.bone("axe", [3, 6, 0], b, rot=[0, 0, 24])
    m.box(ax, [3, 6.4, 0], [1.2, 12, 1.2], "pole")
    m.box(ax, [3, 7.6, -2], [0.6, 4, 4], "trap_iron")
    idle_only(m, {"axe": {"rotation": sway(3, 1, 0, 2)}}, 3.0)
    return m


def hunter_trap():
    """Капкан: стальные челюсти с зубьями на пружине, цепь к колышку."""
    m = Model("hunter_trap")
    b = m.bone("base", [0, 0, 0])
    m.box(b, [0, 0.4, 0], [10, 0.8, 2], "trap_iron")
    m.box(b, [0, 0.6, 0], [2, 1.2, 2], "brass_t")
    for side, sz in (("jaw_a", -1), ("jaw_b", 1)):
        j = m.bone(side, [0, 0.8, 0], b, rot=[-60 * sz, 0, 0])
        m.box(j, [0, 0.8, 2.4 * sz], [10, 0.6, 0.6], "trap_iron")
        for i in range(5):
            m.box(j, [-4 + i * 2, 1.6, 2.4 * sz], [0.6, 1.4, 0.6], "trap_iron")
    m.box(b, [5.6, 0.3, 3], [0.6, 0.6, 3], "chain")
    idle_only(m, {"jaw_a": {"rotation": sway(1.2, 3, 0, 0)}, "jaw_b": {"rotation": sway(1.2, -3, 0, 0)}}, 1.2)
    return m


def turret():
    """Турель механика: латунная баллиста на треноге, шестерня сбоку, светится прицел."""
    m = Model("turret")
    b = m.bone("base", [0, 0, 0])
    for a in (0, 120, 240):
        r = math.radians(a)
        m.box(b, [math.cos(r) * 2.4, 2, math.sin(r) * 2.4], [0.8, 4.6, 0.8], "trap_iron", rot=[0, -a, 24])
    hd = m.bone("head", [0, 5, 0], b)
    m.box(hd, [0, 6, 0], [4, 3, 6], "brass_t", decal={"north": rivets})
    m.box(hd, [0, 6.4, -5], [1.2, 1.2, 6], "trap_iron")
    m.box(hd, [0, 6, -1], [9, 0.8, 0.8], "pole")
    m.box(hd, [2.4, 6, 1.6], [0.6, 2.6, 2.6], "brass_t")
    m.box(hd, [0, 7.8, -1.6], [0.8, 0.8, 0.8], "blink")
    idle_only(m, {"head": {"rotation": kf([(0, [0, 0, 0]), (1.5, [0, 30, 0]), (3, [0, -30, 0]), (4.5, [0, 0, 0])])}}, 4.5)
    m.anim("attack", once(0.4, {"head": {"position": beats(0.4, [(0, [0, 0, 0]), (0.3, [0, 0, 1.2]), (1, [0, 0, 0])])}}))
    return m


def mine():
    """Мина: шипастый шар, мигающий огонёк."""
    m = Model("mine")
    b = m.bone("body", [0, 0, 0])
    m.box(b, [0, 2.4, 0], [5, 4, 5], "trap_iron", decal={"north": rivets})
    m.box(b, [0, 2.4, 0], [4, 5, 4], "trap_iron")
    for dx, dz in ((3, 0), (-3, 0), (0, 3), (0, -3)):
        m.box(b, [dx, 2.4, dz], [1, 1, 1], "steel")
    lt = m.bone("light", [0, 5, 0], b)
    m.box(lt, [0, 5.2, 0], [1.2, 1.0, 1.2], "blink")
    idle_only(m, {"light": {"scale": kf([(0, [1, 1, 1]), (0.4, [1, 1, 1]), (0.5, [0.2, 0.2, 0.2]), (1.0, [0.2, 0.2, 0.2])], smooth=False)}}, 1.0)
    return m


def star():
    """Светило астроманта: сияющее ядро и вращающиеся лучи."""
    m = Model("star")
    c = m.bone("core", [0, 9, 0])
    m.box(c, [0, 9, 0], [3.4, 3.4, 3.4], "star_core", rot=[45, 0, 45])
    rays = m.bone("rays", [0, 9, 0], c)
    for a in range(0, 180, 45):
        m.box(rays, [0, 9, 0], [8, 0.6, 0.6], "star_core", rot=[0, a, 0])
    idle_only(m, {"core": {"position": sway(2, 0.8, 0, 1)}, "rays": {"rotation": kf([(0, [0, 0, 0]), (2, [0, 360, 0])], smooth=False)}}, 2.0)
    return m


def tentacle():
    """Щупальце бездны: выросло из земли, извивается, присоски светятся."""
    m = Model("tentacle")
    names = m.segments("t", "root", [0, 0, 0], [(7, 5, [0, 0, 0]), (7, 4.2, [8, 0, 0]), (7, 3.4, [8, 0, 0]), (6, 2.6, [10, 0, 0]), (5, 1.8, [10, 0, 0])], "abyss")
    for i, n in enumerate(names):
        m.box(n, [0, 3 + i * 7, -(2.6 - i * 0.4)], [1, 1, 0.4], "abyss_glow")
    m.box("root", [0, 0.4, 0], [9, 0.8, 9], "abyss")
    idle_only(m, {n: {"rotation": sway(2.4, 6 + i * 3, i * 0.12, 0)} for i, n in enumerate(names)}, 2.4)
    return m


def ghost_double(name, mat_):
    """Двойник (иллюзиониста или зеркальный): силуэт героя, полупрозрачный по виду, со своим свечением."""
    m = Model(name)
    h = H(height=31, head=8, shoulders=9, hips=7.4, depth=4.6, leg=12, arm=12, limb=3, hand=3)
    r = humanoid(m, h, {"skin": mat_, "torso": mat_, "hand": mat_, "leg": mat_, "boot": mat_, "head": mat_,
                        "face": lambda f: [f.rect(sx, f.h // 2 - 1, sx + 2, f.h // 2 + 1, "#FFFFFF", True) for sx in (f.w // 4, f.w * 5 // 8)]}, arm_out=6)
    m.box(r["chest"], [0, r["chest_y"] + 3, -h.depth / 2 - 0.3], [3, 3, 0.4], "abyss_glow" if mat_ == "ghost" else "star_core")
    motions(m, r, Persona(weight=0.2, float_=True))
    return m


def illusion():
    return ghost_double("illusion", "ghost")


def mirror_double():
    return ghost_double("mirror_double", "mirror_p")


def void_rift():
    """Разлом пустоты: вертикальная трещина-овал, края светятся, внутри кружатся искры."""
    m = Model("void_rift")
    r_ = m.bone("rift", [0, 7, 0])
    for i in range(12):
        a = i * math.pi / 6
        m.box(r_, [math.cos(a) * 5, 7 + math.sin(a) * 6, 0], [2.8, 1.2, 1.2], "abyss_glow", rot=[0, 0, math.degrees(a) + 90])
    m.box(r_, [0, 7, 0], [7, 9, 0.4], "abyss")
    idle_only(m, {"rift": {"rotation": sway(2, 4, 0, 2), "scale": sway(2, 0.06, 0, 0, [1, 1, 1])}}, 2.0)
    return m


def dome_core():
    """Оберег купола: кристалл на каменной подставке, вокруг — кольцо рун."""
    m = Model("dome_core")
    b = m.bone("base", [0, 0, 0])
    m.box(b, [0, 1.4, 0], [8, 2.8, 8], "stone_t", decal={"north": rivets})
    c = m.bone("crystal", [0, 3, 0], b)
    m.box(c, [0, 8, 0], [3.4, 9, 3.4], "dome_c", rot=[0, 45, 0])
    ring = m.bone("ring", [0, 8, 0], b)
    for a in range(0, 360, 60):
        r = math.radians(a)
        m.box(ring, [math.cos(r) * 4.6, 8, math.sin(r) * 4.6], [1, 1, 1], "star_core")
    idle_only(m, {"crystal": {"position": sway(2.4, 0.6, 0, 1)}, "ring": {"rotation": kf([(0, [0, 0, 0]), (2.4, [0, 360, 0])], smooth=False)}}, 2.4)
    return m


# ---------------------------------------------------------------------------- призывы

def minion_skeleton():
    """Поднятый скелет некроманта: скелет с фиолетовым огнём в глазницах и ржавым мечом."""
    m = Model("minion_skeleton")
    r = skeleton(m, 31)
    rh = r["right_hand"]
    x, y = r["right_x"], r["right_hand_y"]
    sw = m.bone("sword", [x, y + 1, 0], rh, rot=[70, 0, 0])
    m.box(sw, [x, y + 7, 0], [1.4, 10, 0.5], "rust")
    m.box(sw, [x, y + 2, 0], [3.4, 0.8, 1.0], "iron")
    motions(m, r, Persona(weight=0.4, hunch=4))
    return m


def minion_wolf():
    """Тень-волк: волк из сгустка тьмы, лиловые глаза, рваный контур шерсти."""
    m = Model("minion_wolf")
    body = m.bone("body", [0, 7, 0])
    m.box(body, [0, 7.4, 0.4], [5.4, 5, 11], "wolf_shade")
    m.box(body, [0, 8.4, -3.6], [6.4, 6, 4], "wolf_shade")
    hd = m.bone("head", [0, 9, -6], body)
    m.box(hd, [0, 9.4, -8], [4.6, 4.4, 4.4], "wolf_shade")
    m.box(hd, [0, 8.4, -11], [2.6, 2.4, 3], "wolf_shade")
    for sx in (-1, 1):
        m.box(hd, [1.4 * sx, 10, -10.3], [0.8, 0.6, 0.4], "wolf_eye")
        m.box(hd, [1.6 * sx, 12.2, -7.4], [1.2, 2, 0.8], "wolf_shade")
    m.segments("tail", body, [0, 9, 5.6], [(3, 1.6, [-110, 0, 0]), (3, 1.4, [20, 0, 0]), (2.4, 1.0, [20, 0, 0])], ["wolf_shade", "wolf_shade", "wolf_eye"])
    for name, x, z in (("leg_fl", -1.6, -3.4), ("leg_fr", 1.6, -3.4), ("leg_bl", -1.6, 4), ("leg_br", 1.6, 4)):
        lb = m.bone(name, [x, 6, z], body)
        m.box(lb, [x, 3, z], [1.6, 6, 1.6], "wolf_shade")
    z = [0, 0, 0]
    m.anim("idle", loop(2, {"tail0": {"rotation": sway(2, 10, 0, 1)}, "head": {"rotation": sway(2, 6, 0, 1)}, "body": {"position": sway(1, 0.2, 0, 1)}}))
    m.anim("walk", loop(0.5, {"leg_fl": {"rotation": sway(0.5, 36, 0, 0)}, "leg_br": {"rotation": sway(0.5, 36, 0, 0)},
                              "leg_fr": {"rotation": sway(0.5, 36, 0.5, 0)}, "leg_bl": {"rotation": sway(0.5, 36, 0.5, 0)},
                              "tail0": {"rotation": sway(0.5, 14, 0.2, 1)}, "body": {"position": sway(0.25, 0.4, 0, 1)}}))
    m.anim("attack", once(0.45, {"head": {"rotation": beats(0.45, [(0, z), (0.4, [-30, 0, 0]), (0.6, [20, 0, 0]), (1, z)])},
                                 "body": {"position": beats(0.45, [(0, z), (0.6, [0, 1, -3]), (1, z)])}}))
    return m


def minion_golem():
    """Костяной голем: громада из сросшихся костей и черепов, грудь — клетка с фиолетовым огнём."""
    m = Model("minion_golem")
    h = H(height=40, head=7, shoulders=18, hips=11, depth=10, leg=13, arm=18, limb=5.4, hand=6.4, neck=0, lean=8)
    r = humanoid(m, h, {"skin": "bone", "torso": "bone_old", "arm": "bone", "hand": "bone_old", "leg": "bone_old", "boot": "bone",
                        "head": "bone", "face": skull_face}, arm_out=12)
    ch = r["chest"]
    for i in range(4):
        m.box(ch, [0, r["chest_y"] + 1 + i * 2.2, -h.depth / 2 - 0.4], [h.shoulders - 4 - i, 1.0, 0.8], "bone")
    m.box(ch, [0, r["chest_y"] + 4, -h.depth / 2 + 1], [5, 5, 3], "abyss_glow")
    for sx in (-1, 1):
        for i in range(3):
            m.box(ch, [sx * (6 + i * 1.6), r["top_y"] + 1 + i, 0], [3, 3, 3], "bone", decal={"north": skull_face})
    motions(m, r, Persona(weight=1.0, hunch=8))
    return m


# ---------------------------------------------------------------------------- торговец, Испытатель, узел силы, свеча

def trader_face(f):
    w, h = f.w, f.h
    f.shade(0, 0, w, int(h * 0.25), 0.6)
    for sx in (int(w * 0.22), int(w * 0.6)):
        f.rect(sx, int(h * 0.42), sx + 2, int(h * 0.42) + 2, "#2A1A10")
        f.set(sx + 1, int(h * 0.42), "#7FF0FF", glow=True)
    f.rect(int(w * 0.25), int(h * 0.66), int(w * 0.75), h, "#E8E0D0")                         # борода
    for x in range(int(w * 0.25), int(w * 0.75), 2):
        f.set(x, h - 1, "#B0A898")
    f.rect(int(w * 0.4), int(h * 0.62), int(w * 0.6), int(h * 0.65), "#6A3A2A")


def trader():
    """Торговец привала: старик-странник в фиолетовом плаще, белая борода, огромный заплечный короб с товаром,
    фонарь на шесте, монеты на поясе."""
    m = Model("trader")
    h = H(height=32, head=8.4, shoulders=10, hips=8, depth=6, leg=11, arm=12, limb=3, hand=3, belly=1.4)
    r = humanoid(m, h, {"skin": "trader_skin", "torso": "trader_robe", "arm": "trader_robe", "hand": "trader_skin",
                        "leg": "trader_robe", "boot": "leather_dark", "head": "trader_skin", "face": trader_face}, arm_out=6)
    features(m, r, h, "trader_skin", nose=1.4, ears=0.8)
    hb, top = r["head"], r["head_top"]
    m.box(hb, [0, top + 0.4, 0], [h.head + 4, 0.8, h.head + 4], "leather")
    m.box(hb, [0, top + 2.4, 0.4], [h.head - 1, 3.6, h.head - 1], "leather")
    m.box(hb, [0, top + 1.2, 0.4], [h.head - 0.6, 0.8, h.head - 0.6], "trader_trim")
    ch = r["chest"]
    pack = m.bone("pack", [0, r["chest_y"] + 3, h.depth / 2 + 1], ch)
    m.box(pack, [0, r["chest_y"] + 4, h.depth / 2 + 3.6], [10, 12, 5], "wood", decal={"south": plate_rows})
    for i, (x, y, s, mt) in enumerate(((-2.6, 11.6, 3, "cloth_red"), (2, 11, 2.4, "trader_trim"), (0, 12.6, 2, "leather"))):
        m.box(pack, [x, r["chest_y"] + y, h.depth / 2 + 3.6], [s, s, s], mt)
    m.box(pack, [4.8, r["chest_y"] + 2, h.depth / 2 + 3.6], [1, 6, 1], "pole")
    body = r["body"]
    m.box(body, [0, r["hip_y"] + 1, 0], [h.hips + 2, 1.2, h.depth + 2.4], "leather_dark", decal={"north": rivets})
    for x in (-3, 3):
        m.box(body, [x, r["hip_y"] - 0.6, -h.depth / 2 - 1.2], [1.8, 2.4, 1.2], "trader_trim")
    m.box(body, [0, r["hip_y"] - 3.4, 0.3], [h.hips + 3, 6, h.depth + 2.6], "trader_robe", decal={"north": tatter, "south": tatter, "east": tatter, "west": tatter})
    lh = r["left_hand"]
    xl, yl = r["left_x"], r["left_hand_y"] + 1
    st = m.bone("staff", [xl, yl, 0], lh, rot=[10, 0, 6])
    m.box(st, [xl, yl + 6, 0], [1.1, 30, 1.1], "pole")
    m.box(st, [xl + 1.6, yl + 20.6, 0], [3, 0.6, 0.6], "pole")
    lt = m.bone("lantern", [xl + 2.8, yl + 20.4, 0], st)
    m.box(lt, [xl + 2.8, yl + 18.6, 0], [2.4, 3, 2.4], "trader_trim")
    m.box(lt, [xl + 2.8, yl + 18.6, 0], [1.8, 2.4, 2.6], "lantern_t")
    motions(m, r, Persona(weight=0.5, hunch=10), extra={"lantern": {"idle": lambda T: {"rotation": sway(T, 8, 0, 2)}}})
    return m


def warden_face(f):
    w, h = f.w, f.h
    f.rect(int(w * 0.1), int(h * 0.36), int(w * 0.9), int(h * 0.56), "#0A1620")
    f.rect(int(w * 0.16), int(h * 0.42), int(w * 0.84), int(h * 0.5), "#7FF0FF", glow=True)
    plate_rows(f)


def trial_warden():
    """Испытатель Системы: страж из сланца и бронзы с синими рунами, вместо лица — визор Системы,
    над плечами парят кристаллы, в руке — бронзовый клинок-указка."""
    m = Model("trial_warden")
    h = H(height=40, head=8, shoulders=14, hips=9, depth=7, leg=14, arm=15, limb=4, hand=4.4)
    r = humanoid(m, h, {"skin": "sys_slate", "torso": "sys_slate", "arm": "sys_bronze", "forearm": "sys_slate", "hand": "sys_bronze",
                        "leg": "sys_slate", "boot": "sys_bronze", "head": "sys_bronze", "face": warden_face}, arm_out=8)
    ch = r["chest"]
    m.box(ch, [0, r["chest_y"] + 4, -h.depth / 2 - 0.5], [4, 4, 0.8], "sys_crystal")
    for sx in (-1, 1):
        orb = m.bone(f"orb_{'r' if sx < 0 else 'l'}", [sx * 10, r["top_y"] + 4, 0], ch)
        m.box(orb, [sx * 10, r["top_y"] + 4, 0], [2.6, 4.4, 2.6], "sys_crystal", rot=[0, 45, 0])
        arm = r["right_arm" if sx < 0 else "left_arm"]
        m.box(arm, [r["right_x" if sx < 0 else "left_x"] + 0.8 * sx, r["shoulder_y"] + 1.4, 0], [5.4, 3, 6], "sys_bronze", decal={"up": rivets})
    rh = r["right_hand"]
    x, y = r["right_x"], r["right_hand_y"] + 1
    sw = m.bone("blade", [x, y, 0], rh, rot=[16, 0, -6])
    m.box(sw, [x, y + 10, 0], [2, 18, 0.8], "sys_bronze", decal={"north": rivets})
    m.box(sw, [x, y + 10, 0], [0.6, 17, 1.0], "sys_crystal")
    motions(m, r, Persona(weight=0.8, hunch=2),
            extra={"orb_r": {"idle": lambda T: {"position": sway(T, 1, 0, 1)}}, "orb_l": {"idle": lambda T: {"position": sway(T, 1, 0.5, 1)}}})
    return m


def power_node():
    """Узел силы хранителя: бронзовый столб с кольцами, кристалл парит и вращается."""
    m = Model("power_node")
    b = m.bone("base", [0, 0, 0])
    m.box(b, [0, 1, 0], [10, 2, 10], "sys_slate")
    m.box(b, [0, 6, 0], [4, 8, 4], "sys_bronze", decal={"north": rivets, "south": rivets})
    core = m.bone("core", [0, 16, 0], b)
    m.box(core, [0, 16, 0], [4, 6, 4], "sys_crystal", rot=[0, 45, 0])
    ring = m.bone("ring", [0, 16, 0], b)
    for a in range(0, 360, 45):
        rr = math.radians(a)
        m.box(ring, [math.cos(rr) * 5, 16, math.sin(rr) * 5], [2.2, 0.8, 0.8], "sys_bronze", rot=[0, -a + 90, 0])
    idle_only(m, {"core": {"rotation": kf([(0, [0, 0, 0]), (3, [0, 360, 0])], smooth=False), "position": sway(3, 0.6, 0, 1)},
                  "ring": {"rotation": kf([(0, [0, 0, 0]), (3, [0, -360, 0])], smooth=False)}}, 3.0)
    return m


def dark_candle():
    """Тёмная свеча Вессы: оплывшая чёрная свеча на черепе, фиолетовое пламя."""
    m = Model("dark_candle")
    b = m.bone("base", [0, 0, 0])
    m.box(b, [0, 2, 0], [5, 4, 5], "bone", decal={"north": skull_face})
    m.box(b, [0, 9, 0], [2.6, 10, 2.6], "candle_wax")
    for x, z, ln in ((1.4, 0, 4), (-1.4, 1, 6), (0, -1.4, 3)):
        m.box(b, [x, 8 - ln / 2, z], [0.8, ln, 0.8], "candle_wax")
    fl = m.bone("flame", [0, 14, 0], b)
    m.box(fl, [0, 15, 0], [1.6, 2.4, 1.6], "candle_flame")
    m.box(fl, [0, 16.6, 0], [0.8, 1.4, 0.8], "candle_flame")
    idle_only(m, {"flame": {"scale": sway(0.6, 0.18, 0, 1, [1, 1, 1]), "rotation": sway(1.2, 6, 0, 2)}}, 1.2)
    return m


# ---------------------------------------------------------------------------- боевой манекен (Двор испытаний)
mat("burlap", "#B49A6C", "cloth", dark="#7A6440", light="#DCC594")
mat("burlap_dark", "#8A7250", "cloth", dark="#5A4830", light="#B09670")
mat("straw", "#D8B858", "hair", dark="#9A7A2A", light="#F6E294")
mat("rope", "#9A7646", "leather", dark="#5E4626", light="#C89E68")
mat("bucket", "#7E8690", "rust", dark="#454B52", light="#B8C0C8")


def dummy_face(f):
    """Мешок-голова: глаза крестиками и зашитый рот — стежки тёмной нитью."""
    w, h = f.w, f.h
    thread = "#3A2A18"
    for ex in (int(w * 0.28), int(w * 0.62)):
        ey = int(h * 0.36)
        for k in range(3):
            f.set(ex + k, ey + k, thread)
            f.set(ex + 2 - k, ey + k, thread)
    my = int(h * 0.7)
    f.rect(int(w * 0.3), my, int(w * 0.72), my + 1, thread)
    for x in range(int(w * 0.32), int(w * 0.72), 2):
        f.set(x, my - 1, thread)
        f.set(x, my + 1, thread)


def dummy_target(f):
    """Мишень на груди: красно-белые кольца с золотым яблочком."""
    w, h = f.w, f.h
    cx, cy = (w - 1) / 2, (h - 1) / 2
    for y in range(h):
        for x in range(w):
            r = math.hypot(x - cx, y - cy) / (min(w, h) / 2)
            if r > 1.02:
                continue
            col = "#FFD860" if r < 0.25 else ("#B8302A" if int(r * 4) % 2 else "#EAE2D0")
            f.set(x, y, col, glow=r < 0.25)                 # яблочко светится — видно и в полутьме
    f.rect(0, 0, w, 1, "#5A3A26")


def patch(f):
    w, h = f.w, f.h
    f.rect(int(w * 0.15), int(h * 0.55), int(w * 0.4), int(h * 0.8), "#8A7250")
    f.line(int(w * 0.15), int(h * 0.55), int(w * 0.4), int(h * 0.55), "#3A2A18")


def training_dummy():
    """Боевой манекен Двора испытаний: соломенное чучело в мешковине на крестовине, на груди — мишень,
    на голове — мятое ведро набекрень, из рукавов и ворота торчит солома. Бьют — качается на столбе."""
    m = Model("training_dummy")
    base = m.bone("base", [0, 0, 0])
    for rot in (0, 90):
        m.box(base, [0, 0.8, 0], [12, 1.6, 2.4], "wood_dark", rot=[0, rot, 0])
    m.box(base, [0, 1.8, 0], [3.4, 1.2, 3.4], "wood")
    for a in (45, 135, 225, 315):
        r = math.radians(a)
        m.box(base, [math.cos(r) * 2.6, 2.6, math.sin(r) * 2.6], [0.8, 3.4, 0.8], "wood_dark", rot=[0, -a, 35])
    m.box(base, [0, 6, 0], [2, 9, 2], "wood")                              # столб до пояса
    body = m.bone("body", [0, 9, 0], base)
    m.box(body, [0, 12, 0], [1.6, 22, 1.6], "wood")                        # столб внутри чучела
    m.box(body, [0, 15.5, 0], [9, 10, 6], "burlap", decal={"south": patch})  # туловище-мешок
    m.box(body, [0, 11.2, 0], [8, 2, 5.4], "burlap_dark")
    m.box(body, [0, 10.4, 0], [9.6, 1, 6.6], "rope")                       # пояс
    m.box(body, [2.6, 9.4, -3.1], [1, 2.2, 0.6], "rope", rot=[0, 0, 15])
    for i, x in enumerate((-3.2, -1.2, 1, 3)):
        m.box(body, [x, 9.2 - (i % 2) * 0.6, 0], [1.4, 1.6 + (i % 2), 5.2], "straw", rot=[0, 0, (i - 1.5) * 9])
    m.box(body, [0, 16, -3.15], [6, 6, 0.4], "wood", decal={"north": dummy_target})  # мишень
    m.box(body, [0, 20.8, 0], [10, 1.2, 6.4], "burlap_dark")               # плечи
    arms = m.bone("arms", [0, 19.5, 0], body)
    m.box(arms, [0, 19.5, 0], [22, 1.6, 1.6], "wood")                      # перекладина рук
    for sx in (-1, 1):
        m.box(arms, [7.2 * sx, 19.5, 0], [6, 3, 3], "burlap", rot=[0, 0, -6 * sx])
        m.box(arms, [10.6 * sx, 19.6, 0], [1.4, 2.4, 2.4], "rope")
        for k in range(3):
            m.box(arms, [11.8 * sx, 19.2 + k * 0.8, -0.8 + k * 0.8], [1.8, 0.8, 0.8], "straw",
                  rot=[0, 0, (k - 1) * 25 * sx])
    head = m.bone("head", [0, 22, 0], body)
    m.box(head, [0, 22.4, 0], [3.4, 1.4, 3.4], "rope")                     # перевязь шеи
    for k, (x, z) in enumerate(((-1.6, -1.4), (1.5, -1.2), (0, 1.7), (-1.2, 1.4))):
        m.box(head, [x, 22.2, z], [0.8, 1.6, 0.8], "straw", rot=[20 * (k % 2) - 10, 0, 15 - 10 * k])
    m.box(head, [0, 26, 0], [6.6, 6.4, 6.2], "burlap", decal={"north": dummy_face})
    m.box(head, [0.3, 29.4, 0], [5.4, 0.8, 5.2], "burlap_dark")
    helm = m.bone("bucket", [0, 29.4, 0], head, rot=[0, 0, -14])
    m.box(helm, [0.6, 31, 0], [7, 3.6, 7], "bucket")                       # мятое ведро набекрень
    m.box(helm, [0.6, 32.9, 0], [6, 0.4, 6], "iron_dark")
    m.box(helm, [0.6, 29.4, 0], [7.6, 0.6, 7.6], "iron_dark")
    m.box(helm, [4.4, 30.8, 0], [0.4, 3, 0.4], "iron", rot=[0, 0, 30])     # дужка
    m.anim("idle", loop(3.0, {
        "body": {"rotation": sway(3.0, 1.2, 0, 2)},
        "arms": {"rotation": sway(3.0, 1.5, 0.25, 0)},
        "bucket": {"rotation": sway(3.0, 2.0, 0.5, 2)},
    }))
    m.anim("hit", once(0.9, {
        "body": {"rotation": kf([(0, [0, 0, 0]), (0.08, [-14, 0, 6]), (0.25, [9, 0, -4]), (0.45, [-5, 0, 2]),
                                 (0.65, [2, 0, -1]), (0.9, [0, 0, 0])])},
        "head": {"rotation": kf([(0, [0, 0, 0]), (0.1, [-10, 8, 0]), (0.3, [8, -5, 0]), (0.55, [-3, 2, 0]), (0.9, [0, 0, 0])])},
        "arms": {"rotation": kf([(0, [0, 0, 0]), (0.1, [0, 12, 0]), (0.32, [0, -8, 0]), (0.6, [0, 3, 0]), (0.9, [0, 0, 0])])},
        "bucket": {"rotation": kf([(0, [0, 0, 0]), (0.1, [0, 0, -12]), (0.3, [0, 0, 10]), (0.6, [0, 0, -4]), (0.9, [0, 0, 0])])},
    }))
    return m


MODELS = {"totem_heal": totem_heal, "totem_storm": totem_storm, "totem_earth": totem_earth, "banner_power": banner_power,
          "banner_guard": banner_guard, "banner_speed": banner_speed, "tree_life": tree_life, "monk_seal": monk_seal,
          "headsman_block": headsman_block, "hunter_trap": hunter_trap, "turret": turret, "mine": mine, "star": star,
          "tentacle": tentacle, "illusion": illusion, "mirror_double": mirror_double, "void_rift": void_rift,
          "dome_core": dome_core, "minion_skeleton": minion_skeleton, "minion_wolf": minion_wolf, "minion_golem": minion_golem,
          "trader": trader, "trial_warden": trial_warden, "power_node": power_node, "dark_candle": dark_candle,
          "training_dummy": training_dummy}
