"""Этап 4 · Кузни Пепла — бес, кузнечный конструкт, пепельный арбалетчик, раскалённый голем, шаман огня, саламандра;
хранители Плавильный голем, Колесница Пламени, Владыка Пепла."""
import math
import hy
from hy import Model, mat, kf, loop, once, sway, beats
from rig import H, humanoid, motions, Persona, cloth_sway, features
from crypt import rivets, tatter, plate_rows

mat("basalt", "#3A3638", "stone", dark="#1C1A1C", light="#5E585A")
mat("ash_iron", "#4A4440", "metal", dark="#221E1C", light="#847A72", gloss=0.3)
mat("hot_iron", "#8A3A1E", "metal", dark="#4A1A0C", light="#FF8A3A", gloss=0.6)
mat("lava", "#3A2018", "lava", dark="#1C0E0A", light="#FFB040")
mat("ember", "#FF7A1E", "glow", glow=True, light="#FFF0A0", dark="#C03A08")
mat("ember_y", "#FFD25A", "glow", glow=True, light="#FFFFFF", dark="#FF8A1E")
mat("ash_cloth", "#3E3632", "cloth", dark="#1E1A18", light="#5E544E")
mat("ash_red", "#7A2A18", "cloth", dark="#3E140A", light="#A84A2E")
mat("imp_skin", "#B83A22", "skin", dark="#6A1A0E", light="#E8684A")
mat("imp_horn", "#2A2420", "bone", dark="#141010", light="#4A403A", scale=0.6)
mat("sala", "#C8501E", "scales", dark="#6A2410", light="#F0904A")
mat("sala_belly", "#E8A84A", "scales", dark="#A06A20", light="#FFD890")
mat("brass", "#B88A3A", "metal", dark="#6A4A18", light="#F0C878", gloss=0.6)
mat("chain_hot", "#6A3A2A", "chain", dark="#2E1A12", light="#C86A3A")
mat("obsidian", "#2A2232", "metal", dark="#120E16", light="#6A5A80", gloss=0.9)
mat("crown_fire", "#FF9A2E", "glow", glow=True, light="#FFF6C0", dark="#E0400A")
mat("horse_skin", "#2A1E1A", "fur", dark="#120C0A", light="#4A3A32")


def ember_face(f):
    """Лицо из пустоты с горящими глазами-щелями и раскалённым ртом."""
    w, h = f.w, f.h
    f.rect(0, 0, w, h, "#1A1210")
    for sx in (int(w * 0.16), int(w * 0.58)):
        f.rect(sx, int(h * 0.38), sx + int(w * 0.26), int(h * 0.38) + 2, "#FFD25A", glow=True)
        f.set(sx, int(h * 0.38) - 1, "#FF7A1E", glow=True)
    for x in range(int(w * 0.24), int(w * 0.76)):
        f.set(x, int(h * 0.72) + (x % 3 == 0), "#FF7A1E", glow=True)


def imp_face(f):
    w, h = f.w, f.h
    for sx in (int(w * 0.18), int(w * 0.6)):
        f.rect(sx, int(h * 0.38), sx + 3, int(h * 0.38) + 2, "#FFE27A", glow=True)
        f.rect(sx - 1, int(h * 0.34), sx + 4, int(h * 0.36), "#4A0E06")
    f.rect(int(w * 0.2), int(h * 0.68), int(w * 0.8), int(h * 0.82), "#2A0806")
    for x in range(int(w * 0.22), int(w * 0.78), 3):
        f.set(x, int(h * 0.68), "#F0E8D0")
        f.set(x + 1, int(h * 0.81), "#F0E8D0")


def visor(f):
    w, h = f.w, f.h
    f.rect(int(w * 0.1), int(h * 0.4), int(w * 0.9), int(h * 0.58), "#140C08")
    f.rect(int(w * 0.18), int(h * 0.44), int(w * 0.82), int(h * 0.54), "#FF7A1E", glow=True)
    plate_rows(f)


def cracks_hot(f):
    """Трещины раскалённого металла."""
    for i in range(4):
        x0, y0 = int(hy.h32(i, f.w) * f.w), int(hy.h32(i, f.h) * f.h)
        for j in range(6):
            f.set(x0 + j * (1 if i % 2 else -1), y0 + j // 2, "#FF9A2E", glow=True)


# ---------------------------------------------------------------------------- монстры

def imp():
    """Огненный бес: маленький рогатый чертёнок с крыльями-перепонками и горящим хвостом, хихикает."""
    m = Model("imp")
    h = H(height=24, head=8.4, shoulders=8, hips=6, depth=4.6, leg=8, arm=9, limb=2.4, hand=2.8)
    r = humanoid(m, h, {"skin": "imp_skin", "torso": "imp_skin", "hand": "imp_skin", "leg": "imp_skin", "boot": "imp_horn",
                        "head": "imp_skin", "face": imp_face}, arm_out=14)
    features(m, r, h, "imp_skin", nose=1.0, ears=1.6)
    hb, top = r["head"], r["head_top"]
    for sx in (-1, 1):
        m.segments(f"horn_{'r' if sx < 0 else 'l'}", hb, [2.6 * sx, top - 0.4, 0], [(2.2, 1.6, [-10, 0, 30 * sx]), (2.0, 1.2, [-20, 0, -20 * sx]), (1.6, 0.8, [-20, 0, -20 * sx])], "imp_horn")
        wb = m.bone(f"wing_{'r' if sx < 0 else 'l'}", [1.6 * sx, r["top_y"] - 1, 2.4], r["chest"], rot=[0, -30 * sx, 0])
        m.cube(wb, [1.6 * sx - (8 if sx < 0 else 0), r["top_y"] - 6, 2.4], [8, 7, 0.4], "ash_red", decal={"north": tatter, "south": tatter})
    m.segments("tail", r["body"], [0, r["hip_y"], 2.4], [(3, 1.0, [-60, 0, 0]), (3, 0.8, [-20, 0, 0]), (2.4, 0.7, [-20, 0, 0]), (1.6, 1.4, [-10, 0, 0])],
               ["imp_skin", "imp_skin", "imp_skin", "ember"])
    motions(m, r, Persona(weight=0.15, hunch=10, speed=1.4),
            extra={"wing_r": {"idle": lambda T: {"rotation": sway(T / 3, 14, 0, 1)}, "walk": lambda L: {"rotation": sway(L / 2, 24, 0, 1)}},
                   "wing_l": {"idle": lambda T: {"rotation": sway(T / 3, -14, 0, 1)}, "walk": lambda L: {"rotation": sway(L / 2, -24, 0, 1)}},
                   "tail0": cloth_sway(10, 0.2)})
    return m


def forge_construct():
    """Кузнечный конструкт: латунно-железный голем, топка в груди, руки-молоты, трубы на плечах дымят."""
    m = Model("forge_construct")
    h = H(height=34, head=7, shoulders=13, hips=9, depth=8, leg=12, arm=14, limb=4, hand=5, neck=0)
    r = humanoid(m, h, {"skin": "ash_iron", "torso": "ash_iron", "arm": "brass", "forearm": "ash_iron", "hand": "ash_iron",
                        "leg": "ash_iron", "boot": "basalt", "head": "brass", "face": visor}, arm_out=10)
    ch = r["chest"]
    m.box(ch, [0, r["chest_y"] + 3.4, -h.depth / 2 - 0.6], [6, 5, 1.2], "ash_iron", decal={"north": rivets})
    m.box(ch, [0, r["chest_y"] + 3.4, -h.depth / 2 - 0.9], [4, 3, 0.6], "ember")
    for sx in (-1, 1):
        m.box(ch, [sx * 4, r["top_y"] + 2.6, 2], [2, 5, 2], "ash_iron")
        m.box(ch, [sx * 4, r["top_y"] + 5.4, 2], [2.6, 1, 2.6], "brass")
        hand = r["right_hand" if sx < 0 else "left_hand"]
        x, y = r["right_x" if sx < 0 else "left_x"], r["right_hand_y" if sx < 0 else "left_hand_y"]
        m.box(hand, [x, y - 0.6, 0], [6, 4, 6], "ash_iron", decal={"north": plate_rows, "south": plate_rows})
    motions(m, r, Persona(weight=0.95, hunch=2))
    return m


def ash_crossbow():
    """Пепельный арбалетчик: в закопчённом плаще и маске-респираторе, тяжёлый арбалет, болты в колчане на бедре."""
    m = Model("ash_crossbow")
    h = H(height=34, head=8, shoulders=10, hips=7.6, depth=5.4, leg=12, arm=12.6, limb=2.8, hand=3.0)
    r = humanoid(m, h, {"skin": "skin", "torso": "ash_cloth", "arm": "ash_cloth", "hand": "leather_dark", "leg": "ash_cloth",
                        "boot": "leather_dark", "head": "ash_cloth", "face": ember_face}, arm_out=6)
    hb, top = r["head"], r["head_top"]
    m.box(hb, [0, r["head_y"] + 2.4, r["face_z"] - 0.6], [5, 4, 1.4], "ash_iron", decal={"north": rivets})   # респиратор
    for sx in (-1, 1):
        m.box(hb, [2.8 * sx, r["head_y"] + 2.2, r["face_z"] - 0.6], [1.8, 2.6, 1.8], "brass")
    m.box(hb, [0, top + 0.4, 0], [h.head + 1.4, 1.4, h.head + 1.4], "ash_cloth")
    cl = m.bone("cloak", [0, r["top_y"], 2.6], r["chest"])
    m.box(cl, [0, r["top_y"] - 9, 3.2], [h.shoulders + 2, 18, 0.6], "ash_cloth", decal={"north": tatter, "south": tatter})
    m.box(r["body"], [3.6, r["hip_y"] - 1, 0.6], [2.4, 6, 2.4], "leather")
    lh = r["left_hand"]
    xl, yl = r["left_x"], r["left_hand_y"]
    xb = m.bone("crossbow", [xl, yl + 1, -1], lh, rot=[80, 0, 0])
    m.box(xb, [xl, yl + 5, -1], [1.6, 10, 1.6], "wood_dark")
    m.box(xb, [xl, yl + 8, -1], [11, 1.2, 1.4], "ash_iron")
    m.box(xb, [xl, yl + 6.4, -1], [0.3, 0.3, 0.3], "steel")
    m.box(xb, [xl, yl + 9.4, -1], [0.8, 3.4, 0.8], "hot_iron")
    motions(m, r, Persona(weight=0.4, hunch=4), extra={"cloak": cloth_sway(6, 0.3, 4)})
    return m


def molten_golem():
    """Раскалённый голем-щит: глыба базальта, сквозь трещины течёт лава, щит-плита из обсидиана."""
    m = Model("molten_golem")
    h = H(height=36, head=7.4, shoulders=14, hips=10, depth=8.6, leg=12, arm=15, limb=4.6, hand=5.4, neck=0)
    r = humanoid(m, h, {"skin": "basalt", "torso": "basalt", "arm": "basalt", "hand": "lava", "leg": "basalt",
                        "boot": "lava", "head": "basalt", "face": ember_face, "pelvis": "lava"}, arm_out=10)
    ch = r["chest"]
    m.box(ch, [0, r["chest_y"] + 3, -h.depth / 2 - 0.3], [h.shoulders - 3, 7, 0.6], "lava", decal={"north": cracks_hot})
    for sx in (-1, 1):
        m.box(ch, [sx * 6, r["top_y"] + 1, 0], [5, 4, 7], "basalt", rot=[0, 0, -10 * sx])
        m.box(ch, [sx * 6, r["top_y"] + 3, 0], [2, 3, 2], "ember", rot=[0, 0, -10 * sx])
    lh = r["left_hand"]
    xl, yl = r["left_x"], r["left_hand_y"]
    m.box(lh, [xl + 1.4, yl + 6, -3], [2, 16, 11], "obsidian", decal={"east": cracks_hot, "west": cracks_hot})
    motions(m, r, Persona(weight=1.0, hunch=4))
    return m


def fire_shaman():
    """Шаман огня: лекарь — маска из черепа саламандры, бусы-угли, накидка из кожи, посох-факел."""
    m = Model("fire_shaman")
    h = H(height=36, head=8, shoulders=9.6, hips=7.6, depth=5, leg=12.6, arm=13, limb=2.6, hand=2.8)
    r = humanoid(m, h, {"skin": "imp_skin", "torso": "ash_red", "arm": "imp_skin", "hand": "imp_skin", "leg": "ash_cloth",
                        "boot": "leather_dark", "head": "imp_skin", "face": imp_face}, arm_out=8)
    hb, top = r["head"], r["head_top"]
    m.box(hb, [0, top - 1, r["face_z"] - 0.6], [h.head + 1, 4, 3], "sala", decal={"north": lambda f: (f.rect(1, f.h - 3, 3, f.h - 1, "#140806"), f.rect(f.w - 3, f.h - 3, f.w - 1, f.h - 1, "#140806"))})
    for sx in (-1, 1):
        m.box(hb, [3 * sx, top + 1.4, 0], [1.4, 3.4, 1.4], "imp_horn", rot=[-20, 0, 20 * sx])
    ch = r["chest"]
    for i in range(7):
        a = i / 6
        m.box(ch, [-3.6 + a * 7.2, r["top_y"] - 1 - abs(a - 0.5) * 5, -h.depth / 2 - 0.5], [0.9, 0.9, 0.9], "ember" if i % 2 else "bone")
    body = r["body"]
    m.box(body, [0, r["hip_y"] - 3, 0.3], [h.hips + 2.6, 7, h.depth + 2], "ash_red", decal={"north": tatter, "south": tatter, "east": tatter, "west": tatter})
    rh = r["right_hand"]
    x, y = r["right_x"], r["right_hand_y"] + 1
    st = m.bone("staff", [x, y, 0], rh, rot=[12, 0, -6])
    m.box(st, [x, y + 4, 0], [1.1, 28, 1.1], "wood_dark")
    m.box(st, [x, y + 18.6, 0], [2.4, 1.8, 2.4], "ash_iron")
    fl = m.bone("torch", [x, y + 20, 0], st)
    m.box(fl, [x, y + 21.4, 0], [2.0, 2.6, 2.0], "ember")
    m.box(fl, [x, y + 23.2, 0], [1.0, 1.6, 1.0], "ember_y")
    motions(m, r, Persona(weight=0.35, caster=True, hunch=8),
            extra={"torch": {"idle": lambda T: {"scale": sway(T / 4, 0.15, 0, 1, [1, 1, 1])}}})
    return m


def salamander():
    """Саламандра: длинная огненная ящерица — чешуя, светящийся гребень, огонь на кончике хвоста, быстрая."""
    m = Model("salamander")
    body = m.bone("body", [0, 3, 0])
    m.box(body, [0, 3.2, 0.6], [4.4, 3.0, 9], "sala")
    m.box(body, [0, 1.8, 0.6], [3.6, 0.8, 8.6], "sala_belly")
    for i in range(4):
        m.box(body, [0, 5.0, -2.4 + i * 2.2], [0.6, 1.4 - i * 0.1, 1.2], "ember")
    hd = m.bone("head", [0, 3.6, -3.8], body)
    m.box(hd, [0, 3.8, -6.2], [3.6, 2.6, 4.4], "sala")
    m.box(hd, [0, 3.0, -8.8], [2.8, 1.6, 1.6], "sala")
    for sx in (-1, 1):
        m.box(hd, [1.8 * sx, 4.6, -6.8], [0.4, 0.8, 0.8], "ember_y")
    m.segments("tail", body, [0, 3.4, 5], [(3, 2.4, [-90, 0, 0]), (3, 1.8, [-2, 0, 0]), (3, 1.3, [-2, 0, 0]), (2.4, 1.0, [-2, 0, 0]), (1.8, 1.6, [0, 0, 0])],
               ["sala", "sala", "sala", "sala", "ember"])
    for name, x, z in (("leg_fl", -2.2, -2.4), ("leg_fr", 2.2, -2.4), ("leg_bl", -2.2, 3.2), ("leg_br", 2.2, 3.2)):
        sx = -1 if x < 0 else 1
        lb = m.bone(name, [x, 2.6, z], body, rot=[0, 0, -20 * sx])
        m.cube(lb, [x - (2.4 if sx < 0 else 0), 1.8, z - 0.6], [2.4, 1.2, 1.2], "sala")
        m.cube(lb, [x + 2.0 * sx - 0.6, 0, z - 1.0], [1.2, 2.4, 2.0], "sala")
    z = [0, 0, 0]
    walk = {"leg_fl": {"rotation": sway(0.35, 30, 0, 1)}, "leg_br": {"rotation": sway(0.35, 30, 0, 1)},
            "leg_fr": {"rotation": sway(0.35, 30, 0.5, 1)}, "leg_bl": {"rotation": sway(0.35, 30, 0.5, 1)},
            "body": {"rotation": sway(0.35, 8, 0, 1)}}
    for i in range(5):
        walk[f"tail{i}"] = {"rotation": sway(0.7, 14 + i * 3, 0.1 * i, 1)}
    m.anim("walk", loop(0.7, walk))
    idle = {f"tail{i}": {"rotation": sway(2.4, 5 + i * 2, 0.1 * i, 1)} for i in range(5)}
    idle["head"] = {"rotation": kf([(0, z), (0.6, [0, 14, 0]), (1.2, z), (1.8, [-6, -12, 0]), (2.4, z)])}
    m.anim("idle", loop(2.4, idle))
    m.anim("attack", once(0.45, {"head": {"rotation": beats(0.45, [(0, z), (0.4, [-30, 0, 0]), (0.6, [20, 0, 0]), (1, z)])},
                                 "body": {"position": beats(0.45, [(0, z), (0.6, [0, 0, -2]), (1, z)])}}))
    m.anim("death", once(1.0, {"body": {"rotation": beats(1.0, [(0, z), (1, [0, 0, 170])]), "position": beats(1, [(0, z), (1, [0, 2.6, 0])])}}, hold=True))
    for nm, ln in (("shoot", 0.6), ("cast", 0.8), ("block", 0.5), ("leap", 0.6), ("hurt", 0.3)):
        m.anim(nm, once(ln, {"head": {"rotation": beats(ln, [(0, z), (0.4, [-25, 0, 0]), (1, z)])}}))
    return m


# ---------------------------------------------------------------------------- хранители

def smelter():
    """Плавильный голем: исполинский котёл на ногах. Плавильная чаша вместо головы кипит, руки — ковш и молот,
    броневые пластины (сбиваются), цепи, жар из щелей."""
    m = Model("smelter")
    h = H(height=36, head=0.01, shoulders=17, hips=11, depth=10, leg=12, arm=16, limb=5, hand=6, neck=0, belly=2)
    r = humanoid(m, h, {"skin": "ash_iron", "torso": "ash_iron", "arm": "ash_iron", "forearm": "hot_iron", "hand": "ash_iron",
                        "leg": "ash_iron", "boot": "basalt", "head": "ash_iron"}, arm_out=12)
    ch = r["chest"]
    top = r["top_y"]
    m.box(ch, [0, top + 2.4, 0], [13, 5, 11], "ash_iron", decal={"north": plate_rows, "south": plate_rows, "east": rivets, "west": rivets})
    m.box(ch, [0, top + 5.2, 0], [14.4, 1.2, 12.4], "brass")
    m.box(ch, [0, top + 4.4, 0], [11, 0.8, 9], "lava", decal={"up": lambda f: None})
    m.box(ch, [0, top + 4.9, 0], [10.6, 0.4, 8.6], "ember")
    hd = r["head"]
    m.box(hd, [0, top - 1.6, -5.6], [10, 3.4, 0.8], "ash_iron", decal={"north": visor})
    for i, (y, w) in enumerate(((r["chest_y"] + 6, 15), (r["chest_y"] + 2.4, 14), (r["hip_y"] + 2, 12))):
        m.box(ch if i < 2 else r["body"], [0, y, -h.depth / 2 - 1.4], [w, 3.0, 1.2], "hot_iron" if i == 1 else "ash_iron", decal={"north": rivets})
    m.box(ch, [0, r["chest_y"] + 4.2, -h.depth / 2 - 2.1], [6, 1.6, 0.6], "ember")
    for sx in (-1, 1):
        m.box(ch, [sx * 8.8, top + 0.4, 0], [5, 4.4, 9], "ash_iron", decal={"up": rivets}, rot=[0, 0, -8 * sx])
        for i in range(3):
            m.box(ch, [sx * (4 + i * 2), r["chest_y"] - i * 1.6, h.depth / 2 + 1], [1.0, 2.2, 0.8], "chain_hot")
    # ковш (правая рука) и молот (левая)
    rh, lh = r["right_hand"], r["left_hand"]
    x, y = r["right_x"], r["right_hand_y"]
    m.box(rh, [x, y - 2, -1], [7, 4, 7], "ash_iron", decal={"north": rivets})
    m.box(rh, [x, y - 0.2, -1], [5.4, 0.6, 5.4], "ember")
    m.box(rh, [x + 3.8 * -1, y - 2, -1], [1.4, 1.4, 3], "ash_iron")
    xl, yl = r["left_x"], r["left_hand_y"]
    ham = m.bone("hammer", [xl, yl + 1, 0], lh, rot=[20, 0, 0])
    m.box(ham, [xl, yl - 3, 0], [1.8, 12, 1.8], "wood_dark")
    m.box(ham, [xl, yl - 10, 0], [6, 5, 9], "ash_iron", decal={"north": plate_rows, "south": plate_rows})
    m.box(ham, [xl, yl - 10, 0], [6.4, 1.4, 9.4], "hot_iron")
    motions(m, r, Persona(weight=1.0, hunch=3, menace=1.5))
    return m


def chariot():
    """Колесница Пламени: огненный конь из обсидиана с гривой пламени тянет колесницу с шипастыми колёсами;
    возница — пустой доспех с горящим шлемом."""
    m = Model("chariot")
    horse = m.bone("horse", [0, 10, -8])
    m.box(horse, [0, 11, -12], [7, 7, 14], "obsidian", decal={"east": cracks_hot, "west": cracks_hot})
    m.box(horse, [0, 15, -18], [4.4, 7, 4.4], "obsidian", rot=[30, 0, 0])
    hd = m.bone("horse_head", [0, 17, -19], horse)
    m.box(hd, [0, 19, -23], [4, 4, 7], "obsidian")
    for sx in (-1, 1):
        m.box(hd, [1.6 * sx, 20, -25.8], [0.8, 0.8, 0.8], "ember_y")
        m.box(hd, [1.6 * sx, 21.4, -21], [0.8, 2.4, 0.8], "obsidian")
    mane = m.bone("mane", [0, 19, -18], horse)
    for i in range(5):
        m.box(mane, [0, 19.4 + i * 0.3, -19.6 + i * 2.2], [1.6, 3.4 - i * 0.3, 2.2], "ember" if i % 2 else "ember_y")
    legs_f = m.bone("legs_f", [0, 9, -16], horse)
    legs_b = m.bone("legs_b", [0, 9, -7], horse)
    for lb, z in ((legs_f, -16), (legs_b, -7)):
        for sx in (-1, 1):
            m.box(lb, [2.4 * sx, 4.6, z], [2, 9.4, 2], "obsidian")
            m.box(lb, [2.4 * sx, 0.6, z - 0.4], [2.6, 1.4, 2.8], "ember")
    m.segments("tail", horse, [0, 13, -5.4], [(3, 1.6, [-130, 0, 0]), (3, 1.4, [20, 0, 0]), (2.4, 1.2, [20, 0, 0])], ["ember", "ember_y", "ember"])
    cart = m.bone("cart", [0, 8, 6])
    m.box(cart, [0, 9, 6], [12, 6, 10], "ash_iron", decal={"north": plate_rows, "east": plate_rows, "west": plate_rows})
    m.box(cart, [0, 12.4, 6], [13, 1, 11], "brass")
    for sx in (-1, 1):
        m.box(cart, [sx * 2.2, 8, -2], [0.8, 0.8, 10], "ash_iron")                              # оглобли
    for side, sx in (("wheel_r", -1), ("wheel_l", 1)):
        w = m.bone(side, [7 * sx, 7, 6], cart)
        for a in range(0, 180, 45):
            m.box(w, [7 * sx, 7, 6], [1.2, 13, 2.2], "ash_iron", rot=[a, 0, 0])
        m.box(w, [7 * sx, 7, 6], [1.6, 3, 3], "hot_iron")
        for a in range(0, 360, 60):
            rr = math.radians(a)
            m.box(w, [7.6 * sx, 7 + math.sin(rr) * 6.4, 6 + math.cos(rr) * 6.4], [1.6, 1.0, 1.0], "steel")
    drv = m.bone("driver", [0, 13, 7], cart)
    m.box(drv, [0, 17, 7], [6.6, 8, 4.6], "ash_iron", decal={"north": plate_rows})
    m.box(drv, [0, 23, 7], [5, 5, 5], "ash_iron", decal={"north": visor})
    m.box(drv, [0, 26.6, 7.4], [2.6, 3.4, 3.4], "crown_fire")
    m.mirror(drv, [3.4, 15, 4], [2, 6, 2], "ash_iron", rot=[60, 0, 0], pivot=[4.4, 20, 7])
    z = [0, 0, 0]
    gallop = {"legs_f": {"rotation": sway(0.5, 40, 0, 0)}, "legs_b": {"rotation": sway(0.5, 40, 0.5, 0)},
              "horse": {"position": sway(0.25, 0.6, 0, 1), "rotation": sway(0.5, 4, 0.25, 0)},
              "wheel_r": {"rotation": kf([(0, z), (0.5, [-360, 0, 0])], smooth=False)}, "wheel_l": {"rotation": kf([(0, z), (0.5, [-360, 0, 0])], smooth=False)},
              "mane": {"scale": sway(0.25, 0.1, 0, 1, [1, 1, 1])}, "tail0": {"rotation": sway(0.5, 10, 0, 0)}}
    m.anim("walk", loop(0.5, gallop))
    m.anim("idle", loop(2.4, {"horse_head": {"rotation": sway(2.4, 8, 0, 0)}, "mane": {"scale": sway(0.6, 0.12, 0, 1, [1, 1, 1])},
                              "tail0": {"rotation": sway(1.2, 8, 0, 1)}, "legs_f": {"rotation": kf([(0, z), (1.6, z), (1.8, [-30, 0, 0]), (2.0, z), (2.4, z)])}}))
    m.anim("attack", once(0.8, {"horse": {"rotation": beats(0.8, [(0, z), (0.4, [-30, 0, 0]), (0.6, [6, 0, 0]), (1, z)])},
                                "legs_f": {"rotation": beats(0.8, [(0, z), (0.4, [-70, 0, 0]), (0.6, [20, 0, 0]), (1, z)])}}))
    m.anim("cast", loop(1.2, {"horse": {"rotation": sway(1.2, 6, 0, 0, [-18, 0, 0])}, "legs_f": {"rotation": sway(0.6, 30, 0, 0, [-40, 0, 0])}}))
    m.anim("slam", once(1.0, {"horse": {"rotation": beats(1.0, [(0, z), (0.4, [-40, 0, 0]), (0.6, [8, 0, 0]), (1, z)])},
                              "legs_f": {"rotation": beats(1.0, [(0, z), (0.4, [-80, 0, 0]), (0.6, [30, 0, 0]), (1, z)])}}))
    m.anim("death", once(1.8, {"horse": {"rotation": beats(1.8, [(0, z), (1, [0, 0, 90])]), "position": beats(1.8, [(0, z), (1, [0, -4, 0])])},
                               "cart": {"rotation": beats(1.8, [(0, z), (1, [0, 0, -20])])}}, hold=True))
    return m


def ash_lord():
    """Владыка Пепла: рыцарь-король из обсидиана и тлеющего железа, корона из пламени, рогатый шлем,
    плащ из пепла, двуручный меч с раскалённой кромкой; из трещин доспеха сыплется жар."""
    m = Model("ash_lord")
    h = H(height=46, head=8, shoulders=13, hips=9, depth=6.6, leg=16, arm=16, limb=3.8, hand=3.8, neck=0.6)
    r = humanoid(m, h, {"skin": "ash_iron", "torso": "obsidian", "arm": "ash_iron", "forearm": "obsidian", "hand": "ash_iron",
                        "leg": "obsidian", "boot": "ash_iron", "head": "ash_iron", "face": visor, "pelvis": "ash_iron"}, arm_out=8)
    hb, top, y0 = r["head"], r["head_top"], r["head_y"]
    m.box(hb, [0, y0 + 2, r["face_z"] - 0.5], [h.head - 1, 3, 1], "ash_iron", decal={"north": rivets})
    for sx in (-1, 1):
        m.segments(f"horn_{'r' if sx < 0 else 'l'}", hb, [3.6 * sx, top - 1.4, 0], [(3.4, 2.0, [-10, 0, 60 * sx]), (3.2, 1.6, [-10, 0, -40 * sx]),
                                                                                     (2.8, 1.2, [-20, 0, -30 * sx]), (2.0, 0.8, [-20, 0, -20 * sx])], "obsidian")
    crown = m.bone("crown", [0, top, 0], hb)
    for i in range(7):
        a = i * math.pi * 2 / 7
        ln = 3.4 + (i % 2) * 1.6
        m.box(crown, [math.cos(a) * 3.4, top + ln / 2, math.sin(a) * 3.4], [1.2, ln, 1.2], "crown_fire" if i % 2 else "ember")
    ch = r["chest"]
    m.box(ch, [0, r["chest_y"] + 4, -h.depth / 2 - 0.4], [h.shoulders - 2, 8, 0.8], "obsidian", decal={"north": cracks_hot})
    m.box(ch, [0, r["chest_y"] + 4, -h.depth / 2 - 0.8], [2.6, 2.6, 0.5], "ember_y")
    for sx in (-1, 1):
        arm = r["right_arm" if sx < 0 else "left_arm"]
        x = r["right_x" if sx < 0 else "left_x"]
        for i in range(3):
            m.box(arm, [x + 0.8 * sx, r["shoulder_y"] + 1.6 - i * 1.6, 0], [6 - i, 1.6, 7 - i], "ash_iron" if i != 1 else "hot_iron",
                  decal={"up": rivets}, rot=[0, 0, -16 * sx])
        m.box(arm, [x + 2.4 * sx, r["shoulder_y"] + 3.6, 0], [1.4, 3.4, 1.4], "obsidian", rot=[0, 0, -30 * sx])
    cape = m.bone("cape", [0, r["top_y"], h.depth / 2 + 1], ch)
    m.box(cape, [0, r["top_y"] - 14, h.depth / 2 + 1.6], [h.shoulders + 2, 28, 0.6], "ash_cloth", decal={"north": tatter, "south": tatter})
    body = r["body"]
    for i in range(3):
        m.box(body, [(i - 1) * 3.4, r["hip_y"] - 2.6, -h.depth / 2 - 0.4], [3, 7, 0.6], "ash_iron", decal={"north": rivets})
    rh = r["right_hand"]
    x, y = r["right_x"], r["right_hand_y"] + 1.4
    sw = m.bone("sword", [x, y, 0], rh, rot=[16, 0, -6])
    m.box(sw, [x, y, 0], [1.4, 4.4, 1.4], "leather_dark")
    m.box(sw, [x, y + 2.6, 0], [6.4, 1.2, 1.8], "obsidian")
    m.box(sw, [x, y + 14, 0], [2.6, 22, 0.8], "obsidian", decal={"north": cracks_hot, "south": cracks_hot})
    m.box(sw, [x, y + 14, 0], [3.2, 21, 0.4], "ember")
    m.box(sw, [x, y - 2.6, 0], [1.8, 1.8, 1.8], "ember_y")
    motions(m, r, Persona(weight=0.75, hunch=3, menace=1.4), extra={"cape": cloth_sway(5, 0.3, 4),
            "crown": {"idle": lambda T: {"scale": sway(T / 3, 0.1, 0, 1, [1, 1, 1])}, "cast": lambda C: {"scale": sway(C / 2, 0.25, 0, 1, [1.2, 1.3, 1.2])}}})
    return m


MODELS = {"imp": imp, "forge_construct": forge_construct, "ash_crossbow": ash_crossbow, "molten_golem": molten_golem,
          "fire_shaman": fire_shaman, "salamander": salamander, "smelter": smelter, "chariot": chariot, "ash_lord": ash_lord}
