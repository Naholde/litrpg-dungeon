"""Этап 5 · Шпиль Бездны — страж пустоты, пожиратель света, осколочный, странник, искажённый лучник, бездновый колосс,
тень; хранители Двойник, Хронофаг, Ксалатар; песочные часы, глаз бездны."""
import math
import hy
from hy import Model, mat, kf, loop, once, sway, beats
from rig import H, humanoid, motions, Persona, cloth_sway, features
from crypt import rivets, tatter, plate_rows

mat("void", "#1E1428", "metal", dark="#0C0812", light="#4A3866", gloss=0.8)
mat("void_cloth", "#24183A", "cloth", dark="#100A1C", light="#463066")
mat("purpur", "#9A6AB0", "stone", dark="#5A3A6E", light="#C8A0DC")
mat("void_glow", "#C080FF", "glow", glow=True, light="#FFFFFF", dark="#7A2AD0")
mat("void_eye", "#F0E0FF", "glow", glow=True, light="#FFFFFF", dark="#B080FF")
mat("star_glow", "#8AF0FF", "glow", glow=True, light="#FFFFFF", dark="#3AA0D8")
mat("crystal_v", "#B48AF0", "crystal", glow="pattern", dark="#6A40A8", light="#F0DCFF")
mat("shade", "#0E0A14", "plain", dark="#050307", light="#2A2236")
mat("mirror", "#C8D4E0", "metal", dark="#7A8898", light="#FFFFFF", gloss=1.0)
mat("sand", "#E8C87A", "plain", dark="#B08A40", light="#FFF0C0")
mat("time_gold", "#D8B04A", "metal", dark="#7A5A1A", light="#FFF0A0", gloss=0.8)
mat("time_blue", "#2A3A6A", "cloth", dark="#121A34", light="#4A5E9A")
mat("flesh_v", "#6A4A7A", "skin", dark="#3A2246", light="#9A72AC")
mat("teeth", "#EDE6D8", "bone", dark="#A89E8A", light="#FFFFFF", scale=0.5)


def void_face(f):
    """Пустота вместо лица: тёмный провал, два белых огня и сеть фиолетовых трещин."""
    w, h = f.w, f.h
    f.rect(1, 1, w - 1, h - 1, "#08050C")
    for sx in (int(w * 0.24), int(w * 0.62)):
        f.rect(sx, int(h * 0.4), sx + 2, int(h * 0.4) + 3, "#F0E0FF", glow=True)
    for i in range(5):
        x = int(hy.h32(i, 3) * w)
        f.line(x, 0, w // 2, h // 2, "#7A2AD0", glow=True)


def mirror_face(f):
    """Двойник: зеркальная маска — отражение, блики, трещина."""
    w, h = f.w, f.h
    for y in range(h):
        for x in range(w):
            if (x + y) % 7 == 0:
                f.set(x, y, "#FFFFFF")
    f.line(int(w * 0.7), 0, int(w * 0.3), h, "#5A6A7A")
    for sx in (int(w * 0.24), int(w * 0.6)):
        f.rect(sx, int(h * 0.42), sx + 3, int(h * 0.42) + 1, "#1A1E26")


def clock_face(f):
    """Циферблат вместо лица: метки часов, две стрелки, светящийся центр."""
    w, h = f.w, f.h
    cx, cy = w // 2, h // 2
    f.rect(1, 1, w - 1, h - 1, "#E8DCC0")
    for i in range(12):
        a = i * math.pi / 6
        f.set(cx + round(math.cos(a) * (w / 2 - 2)), cy + round(math.sin(a) * (h / 2 - 2)), "#2A1E10")
    f.line(cx, cy, cx, cy - int(h * 0.36), "#2A1E10")
    f.line(cx, cy, cx + int(w * 0.24), cy + 1, "#2A1E10")
    f.rect(cx - 1, cy - 1, cx + 1, cy + 1, "#8AF0FF", glow=True)


def maw_face(f):
    """Ксалатар: пасть во всё лицо — кольцо зубов вокруг сияющей глотки, над ней россыпь глаз."""
    w, h = f.w, f.h
    cx, cy = w // 2, int(h * 0.62)
    for y in range(h):
        for x in range(w):
            d = math.hypot((x - cx) / (w * 0.38), (y - cy) / (h * 0.3))
            if d < 0.55:
                f.set(x, y, "#C080FF" if d < 0.3 else "#3A1A50", glow=d < 0.3)
            elif d < 0.8 and (x + y) % 2 == 0:
                f.set(x, y, "#EDE6D8")
    for i in range(6):
        x, y = int(w * (0.12 + 0.15 * i)), int(h * (0.14 + 0.08 * (i % 2)))
        f.rect(x, y, x + 2, y + 2, "#F0E0FF", glow=True)


def rune_lines(f):
    for y in range(2, f.h, 5):
        f.rect(2, y, f.w - 2, y + 1, "#C080FF", glow=True)


# ---------------------------------------------------------------------------- монстры

def void_sentinel():
    """Страж пустоты: латы из пурпурного камня, вместо лица — провал, щит-зеркало пустоты, копьё-луч."""
    m = Model("void_sentinel")
    h = H(height=40, head=8, shoulders=12, hips=8.6, depth=6.4, leg=14, arm=14, limb=3.4, hand=3.4)
    r = humanoid(m, h, {"skin": "void", "torso": "purpur", "arm": "void", "forearm": "purpur", "hand": "void",
                        "leg": "purpur", "boot": "void", "head": "void", "face": void_face, "pelvis": "void"}, arm_out=7)
    hb, top = r["head"], r["head_top"]
    m.box(hb, [0, top + 0.4, 0], [h.head + 1, 1.4, h.head + 1], "purpur")
    m.box(hb, [0, top + 2.6, 0.6], [1.0, 3.4, 5], "void_glow")
    ch = r["chest"]
    m.box(ch, [0, r["chest_y"] + 4, -h.depth / 2 - 0.4], [h.shoulders - 2, 6, 0.6], "purpur", decal={"north": rune_lines})
    for sx in (-1, 1):
        arm = r["right_arm" if sx < 0 else "left_arm"]
        x = r["right_x" if sx < 0 else "left_x"]
        m.box(arm, [x + 0.8 * sx, r["shoulder_y"] + 1.4, 0], [5, 3, 6], "purpur", decal={"up": rivets})
        m.box(arm, [x + 1.6 * sx, r["shoulder_y"] + 3.6, 0], [1.4, 3, 1.4], "crystal_v", rot=[0, 0, -20 * sx])
    lh = r["left_hand"]
    xl, yl = r["left_x"], r["left_hand_y"]
    m.box(lh, [xl + 1.4, yl + 5, -2], [1.4, 14, 9], "void", decal={"east": rune_lines, "west": rune_lines})
    m.box(lh, [xl + 2.2, yl + 5, -2], [0.4, 6, 5], "void_glow")
    rh = r["right_hand"]
    x, y = r["right_x"], r["right_hand_y"] + 1
    sp = m.bone("spear", [x, y, 0], rh, rot=[14, 0, -6])
    m.box(sp, [x, y + 6, 0], [1.1, 34, 1.1], "void")
    m.box(sp, [x, y + 24, 0], [2.2, 4.6, 0.8], "crystal_v")
    m.box(sp, [x, y + 27.4, 0], [1.0, 2.2, 0.5], "void_glow")
    motions(m, r, Persona(weight=0.7, hunch=2))
    return m


def light_eater():
    """Пожиратель света: лекарь-вампир — вытянутый, без глаз, огромная пасть, руки-щупы втягивают свет."""
    m = Model("light_eater")
    h = H(height=36, head=8, shoulders=9, hips=7, depth=5, leg=13, arm=15, limb=2.6, hand=3.0, lean=10)
    r = humanoid(m, h, {"skin": "flesh_v", "torso": "void_cloth", "arm": "flesh_v", "hand": "flesh_v", "leg": "void_cloth",
                        "boot": "shade", "head": "flesh_v", "face": maw_face}, arm_out=12)
    hb, top = r["head"], r["head_top"]
    for i in range(3):
        m.box(hb, [0, top + 0.6 + i, 0.6 + i * 1.2], [h.head - i * 2, 1, h.head - i * 2], "flesh_v")
    ch = r["chest"]
    for i, (x, rz) in enumerate(((-3, 30), (-1, 10), (1, -10), (3, -30))):
        m.box(ch, [x, r["top_y"] + 2, 2.6], [0.9, 4, 0.9], "void_glow", rot=[-30, 0, rz], pivot=[x, r["top_y"], 2.6])
    body = r["body"]
    m.box(body, [0, r["hip_y"] - 4, 0.3], [h.hips + 3, 9, h.depth + 2.4], "void_cloth", decal={"north": tatter, "south": tatter, "east": tatter, "west": tatter})
    for side, sx in (("right", -1), ("left", 1)):
        hand = r[f"{side}_hand"]
        x, y = r[f"{side}_x"], r[f"{side}_hand_y"]
        m.segments(f"feel_{side}", hand, [x, y, 0], [(2, 0.8, [10, 0, 0]), (2, 0.6, [10, 0, 0]), (1.6, 0.5, [10, 0, 0])], ["flesh_v", "flesh_v", "void_glow"], up=False)
    motions(m, r, Persona(weight=0.3, caster=True, hunch=14))
    return m


def wanderer():
    """Странник: мерцающий беглец в рваном плаще с капюшоном, без лица — только звёзды, кинжалы-осколки."""
    m = Model("wanderer")
    h = H(height=34, head=7.6, shoulders=9, hips=7, depth=5, leg=12, arm=12, limb=2.6, hand=2.8)
    r = humanoid(m, h, {"skin": "shade", "torso": "void_cloth", "arm": "void_cloth", "hand": "shade", "leg": "void_cloth",
                        "boot": "shade", "head": "shade", "face": void_face}, arm_out=6)
    hb, top, y0 = r["head"], r["head_top"], r["head_y"]
    s = h.head + 1.6
    m.box(hb, [0, top + 0.6, 0], [s, 1.6, s + 0.4], "void_cloth")
    m.box(hb, [0, top - 0.4, -s / 2], [s, 1.4, 1.4], "void_cloth")
    m.mirror(hb, [h.head / 2 + 0.05, y0 - 0.6, -s / 2 - 0.4], [1.1, top - y0 + 1.4, s], "void_cloth")
    m.box(hb, [0, (y0 + top) / 2, s / 2], [s, top - y0 + 1.6, 1.0], "void_cloth")
    cl = m.bone("cloak", [0, r["top_y"], 2.4], r["chest"])
    m.box(cl, [0, r["top_y"] - 10, 3], [h.shoulders + 3, 20, 0.6], "void_cloth", decal={"north": tatter, "south": tatter})
    for side in ("right", "left"):
        hand = r[f"{side}_hand"]
        x, y = r[f"{side}_x"], r[f"{side}_hand_y"]
        d = m.bone(f"dagger_{side}", [x, y + 1, 0], hand, rot=[70, 0, 0])
        m.box(d, [x, y + 4.6, 0], [0.6, 6, 1.6], "crystal_v")
    motions(m, r, Persona(weight=0.15, hunch=10, speed=1.5), extra={"cloak": cloth_sway(10, 0.2, 6)})
    return m


def warped_archer():
    """Искажённый лучник: лучник, чьё тело треснуло от пустоты — светящиеся разломы, лук из кристалла."""
    m = Model("warped_archer")
    h = H(height=34, head=7.6, shoulders=9.6, hips=7.4, depth=5, leg=12, arm=12.6, limb=2.6, hand=2.8)
    r = humanoid(m, h, {"skin": "flesh_v", "torso": "void_cloth", "arm": "flesh_v", "hand": "flesh_v", "leg": "void_cloth",
                        "boot": "shade", "head": "flesh_v", "face": void_face}, arm_out=6)
    features(m, r, h, "flesh_v", nose=0.8)
    ch = r["chest"]
    for i in range(3):
        m.box(ch, [-2 + i * 2, r["chest_y"] + 2 + i * 2, -h.depth / 2 - 0.3], [0.6, 3, 0.4], "void_glow", rot=[0, 0, 30 - i * 20])
    m.box(r["head"], [2.4, r["head_top"] + 1.2, 0], [1.2, 3, 1.2], "crystal_v", rot=[0, 0, -20])
    lh = r["left_hand"]
    xl, yl = r["left_x"], r["left_hand_y"] + 1.2
    bw = m.bone("bow", [xl, yl, -0.8], lh)
    m.segments("bow_up", bw, [xl, yl + 1, -0.8], [(4, 1.2, [-18, 0, 0]), (4, 1.0, [-18, 0, 0]), (3, 0.8, [-14, 0, 0])], "crystal_v")
    m.segments("bow_dn", bw, [xl, yl - 1, -0.8], [(4, 1.2, [18, 0, 0]), (4, 1.0, [18, 0, 0]), (3, 0.8, [14, 0, 0])], "crystal_v", up=False)
    m.box(bw, [xl, yl, 1.8], [0.25, 18, 0.25], "void_glow")
    motions(m, r, Persona(weight=0.3, hunch=6))
    return m


def colossus():
    """Бездновый колосс: громила из пурпурных блоков, кристаллы растут из спины, кулаки-глыбы, единственный глаз."""
    m = Model("colossus")
    h = H(height=34, head=6.6, shoulders=16, hips=10, depth=9, leg=11, arm=16, limb=5, hand=6, neck=0)
    r = humanoid(m, h, {"skin": "purpur", "torso": "purpur", "arm": "purpur", "hand": "void", "leg": "purpur", "boot": "void",
                        "head": "purpur", "face": lambda f: (f.rect(f.w // 2 - 2, f.h // 2 - 1, f.w // 2 + 2, f.h // 2 + 2, "#F0E0FF", True))}, arm_out=12)
    ch = r["chest"]
    for i, (x, z, ln, rx, rz) in enumerate(((-4, 3, 6, -30, 20), (0, 4, 8, -40, 0), (4, 3, 6, -30, -20), (-2, 5, 4, -50, 10), (2.4, 5, 4.4, -50, -12))):
        m.box(ch, [x, r["top_y"] + ln / 2, z], [2.2, ln, 2.2], "crystal_v", rot=[rx, 0, rz], pivot=[x, r["top_y"], z])
    m.box(ch, [0, r["chest_y"] + 3, -h.depth / 2 - 0.3], [6, 6, 0.6], "void", decal={"north": rune_lines})
    for side in ("right", "left"):
        hand = r[f"{side}_hand"]
        m.box(hand, [r[f"{side}_x"], r[f"{side}_hand_y"] + 1, 0], [7, 6, 7], "purpur", decal={"north": plate_rows})
    motions(m, r, Persona(weight=1.0, hunch=10))
    return m


def shadow():
    """Тень Двойника: силуэт игрока из чистой тьмы с лиловым контуром и белыми глазами."""
    m = Model("shadow")
    h = H(height=32, head=8, shoulders=9, hips=7.4, depth=4.6, leg=12, arm=12, limb=3, hand=3)
    r = humanoid(m, h, {"skin": "shade", "torso": "shade", "hand": "shade", "leg": "shade", "boot": "shade", "head": "shade",
                        "face": lambda f: [f.rect(sx, f.h // 2 - 1, sx + 2, f.h // 2 + 1, "#F0E0FF", True) for sx in (f.w // 4, f.w * 5 // 8)]}, arm_out=6)
    for side in ("right", "left"):
        m.box(r[f"{side}_hand"], [r[f"{side}_x"], r[f"{side}_hand_y"] + 1, 0], [3.4, 1.0, 3.4], "void_glow")
    m.box(r["chest"], [0, r["top_y"] - 0.4, 0], [h.shoulders + 0.4, 0.6, h.depth + 0.4], "void_glow")
    motions(m, r, Persona(weight=0.3))
    return m


def shard():
    """Осколочный: рой — парящий кристалл с ядром, вокруг кружат три обломка."""
    m = Model("shard")
    core = m.bone("core", [0, 9, 0])
    m.box(core, [0, 9, 0], [3, 6, 3], "crystal_v", rot=[0, 45, 0])
    m.box(core, [0, 9, 0], [1.6, 7.4, 1.6], "void_glow", rot=[0, 45, 0])
    orbit = m.bone("orbit", [0, 9, 0], core)
    for i in range(3):
        a = i * math.pi * 2 / 3
        m.box(orbit, [math.cos(a) * 3.4, 9 + (i - 1), math.sin(a) * 3.4], [1, 2, 1], "crystal_v", rot=[20, i * 40, 30])
    z = [0, 0, 0]
    idle = {"core": {"position": sway(1.6, 0.8, 0, 1), "rotation": kf([(0, z), (1.6, [0, 90, 0])], smooth=False)},
            "orbit": {"rotation": kf([(0, z), (1.6, [0, -360, 0])], smooth=False)}}
    m.anim("idle", loop(1.6, idle))
    m.anim("walk", loop(1.6, idle))
    m.anim("attack", once(0.5, {"core": {"position": beats(0.5, [(0, z), (0.5, [0, 0, -3]), (1, z)])}}))
    m.anim("death", once(0.8, {"core": {"scale": beats(0.8, [(0, [1, 1, 1]), (1, [0.05, 0.05, 0.05])])}}, hold=True))
    for nm, ln in (("shoot", 0.6), ("cast", 0.8), ("block", 0.5), ("leap", 0.6), ("hurt", 0.3)):
        m.anim(nm, once(ln, {"core": {"scale": beats(ln, [(0, [1, 1, 1]), (0.4, [1.2, 1.2, 1.2]), (1, [1, 1, 1])])}}))
    return m


# ---------------------------------------------------------------------------- хранители

def doppel():
    """Двойник: дуэлянт в зеркальных латах, маска-зеркало, плащ из осколков, два клинка-отражения."""
    m = Model("doppel")
    h = H(height=36, head=8, shoulders=10, hips=7.6, depth=5.4, leg=13, arm=13, limb=2.8, hand=3.0)
    r = humanoid(m, h, {"skin": "mirror", "torso": "void_cloth", "arm": "mirror", "forearm": "void_cloth", "hand": "mirror",
                        "leg": "void_cloth", "boot": "mirror", "head": "mirror", "face": mirror_face}, arm_out=8)
    hb, top = r["head"], r["head_top"]
    m.box(hb, [0, top + 0.8, 0.6], [h.head - 2, 2, h.head - 1], "crystal_v")
    ch = r["chest"]
    m.box(ch, [0, r["chest_y"] + 4, -h.depth / 2 - 0.4], [h.shoulders - 2, 6, 0.6], "mirror", decal={"north": plate_rows})
    cape = m.bone("cape", [0, r["top_y"], h.depth / 2 + 0.8], ch)
    for i in range(6):
        m.box(cape, [-4 + i * 1.6, r["top_y"] - 5 - (i % 3) * 2, h.depth / 2 + 1.2], [1.4, 10 + (i % 3) * 3, 0.4], "mirror" if i % 2 else "crystal_v")
    for side, rz in (("right", -6), ("left", 6)):
        hand = r[f"{side}_hand"]
        x, y = r[f"{side}_x"], r[f"{side}_hand_y"] + 1
        b = m.bone(f"blade_{side}", [x, y, 0], hand, rot=[70, 0, rz])
        m.box(b, [x, y + 1, 0], [1.0, 2.6, 1.0], "void")
        m.box(b, [x, y + 2.6, 0], [3.4, 0.8, 1.0], "mirror")
        m.box(b, [x, y + 9, 0], [1.6, 12, 0.5], "mirror")
        m.box(b, [x, y + 9, 0], [0.4, 11, 0.6], "void_glow")
    motions(m, r, Persona(weight=0.3, hunch=4, speed=1.2), extra={"cape": cloth_sway(6, 0.3, 4)})
    return m


def chronophage():
    """Хронофаг: пожиратель времени — высокий, в синей мантии, вместо лица циферблат, за спиной кольцо шестерён,
    в руках огромные часовые стрелки-клинки, на поясе песочные часы."""
    m = Model("chronophage")
    h = H(height=46, head=8.4, shoulders=11, hips=8, depth=5.6, leg=16, arm=15, limb=2.8, hand=3.2)
    r = humanoid(m, h, {"skin": "time_gold", "torso": "time_blue", "arm": "time_blue", "hand": "time_gold", "leg": "time_blue",
                        "boot": "time_gold", "head": "time_gold", "face": clock_face}, arm_out=9)
    hb, top = r["head"], r["head_top"]
    m.box(hb, [0, top + 0.6, 0], [h.head + 1.4, 1.2, h.head + 1.4], "time_gold")
    m.box(hb, [0, top + 2.0, 0], [2.4, 2.4, 2.4], "time_gold")
    ch = r["chest"]
    gear = m.bone("gears", [0, r["top_y"] + 1, h.depth / 2 + 1.6], ch)
    for i in range(10):
        a = i * math.pi / 5
        m.box(gear, [math.cos(a) * 8, r["top_y"] + 1 + math.sin(a) * 8, h.depth / 2 + 1.6], [5.4, 1.4, 0.6], "time_gold",
              rot=[0, 0, math.degrees(a) + 90])
        m.box(gear, [math.cos(a) * 9.4, r["top_y"] + 1 + math.sin(a) * 9.4, h.depth / 2 + 1.6], [1.4, 1.4, 0.8], "time_gold")
    for i in range(4):
        a = i * math.pi / 2
        m.box(gear, [math.cos(a) * 4, r["top_y"] + 1 + math.sin(a) * 4, h.depth / 2 + 1.4], [8, 0.8, 0.5], "time_gold", rot=[0, 0, math.degrees(a)])
    m.box(gear, [0, r["top_y"] + 1, h.depth / 2 + 1.2], [2.4, 2.4, 0.8], "star_glow")
    body = r["body"]
    for i, (w, d, hg) in enumerate(((h.hips + 2, h.depth + 2, 6), (h.hips + 4, h.depth + 3.6, 7), (h.hips + 6, h.depth + 5, 7))):
        m.box(body, [0, r["hip_y"] + 1 - i * 6 - hg / 2 + 2.6, 0.4], [w, hg, d], "time_blue",
              decal={"north": tatter, "south": tatter, "east": tatter, "west": tatter} if i == 2 else None)
    m.box(body, [h.hips / 2 + 1.6, r["hip_y"] + 0.4, -1], [2.4, 4, 2.4], "time_gold")
    m.box(body, [h.hips / 2 + 1.6, r["hip_y"] + 0.4, -1], [1.6, 3.2, 1.6], "sand")
    for side, rz in (("right", -8), ("left", 8)):
        hand = r[f"{side}_hand"]
        x, y = r[f"{side}_x"], r[f"{side}_hand_y"] + 1
        b = m.bone(f"hand_{side}", [x, y, 0], hand, rot=[60, 0, rz])
        m.box(b, [x, y + 9, 0], [1.4 if side == "right" else 1.0, 18 if side == "right" else 13, 0.6], "time_gold")
        m.box(b, [x, y + (18.6 if side == "right" else 16), 0], [2.6, 2.6, 0.6], "time_gold", rot=[0, 0, 45])
    motions(m, r, Persona(weight=0.55, caster=True, float_=True, hunch=4, menace=1.3),
            extra={"gears": {"idle": lambda T: {"rotation": kf([(0, [0, 0, 0]), (T, [0, 0, 90])], smooth=False)},
                             "cast": lambda C: {"rotation": kf([(0, [0, 0, 0]), (C, [0, 0, -360])], smooth=False)}}})
    return m


def xalathar():
    """Ксалатар, Голод Бездны: исполинский пожиратель — горбатое тело из пустоты, пасть во всё лицо,
    россыпь глаз, рога-кристаллы, щупальца из спины, длинные когтистые руки; за спиной — кольцо разлома."""
    m = Model("xalathar")
    h = H(height=40, head=10, shoulders=18, hips=11, depth=10, leg=12, arm=19, limb=5, hand=6, neck=0, lean=10, belly=1.4)
    r = humanoid(m, h, {"skin": "void", "torso": "void", "arm": "flesh_v", "forearm": "void", "hand": "void", "leg": "void",
                        "boot": "void", "head": "flesh_v", "face": maw_face}, arm_out=14)
    hb, top, y0 = r["head"], r["head_top"], r["head_y"]
    jaw = m.bone("jaw", [0, y0 + 1, -1], hb)
    m.box(jaw, [0, y0 - 1.2, -2], [h.head - 1, 3, h.head - 1.6], "flesh_v", decal={"north": lambda f: [f.set(x, 0, "#EDE6D8") for x in range(0, f.w, 2)]})
    for sx in (-1, 1):
        m.segments(f"horn_{'r' if sx < 0 else 'l'}", hb, [4.4 * sx, top - 1, 0], [(4, 2.4, [-20, 0, 40 * sx]), (3.6, 2.0, [-20, 0, -20 * sx]),
                                                                                   (3.0, 1.4, [-20, 0, -20 * sx]), (2.2, 1.0, [-10, 0, -10 * sx])], "crystal_v")
    ch = r["chest"]
    m.box(ch, [0, r["top_y"] + 1, 3], [h.shoulders - 2, 6, 7], "void")                              # горб
    for i in range(6):
        m.box(ch, [(i - 2.5) * 2.4, r["chest_y"] + 3 + (i % 2), -h.depth / 2 - 1.2], [1.2, 1.2, 0.6], "void_eye")   # глаза на груди
    for i, (x, rz) in enumerate(((-5, 30), (-2, 12), (2, -12), (5, -30))):
        m.segments(f"tent{i}_", ch, [x, r["top_y"] + 3, 5], [(4, 2.2, [-40, 0, rz]), (4, 1.8, [-20, 0, rz * 0.3]), (3.4, 1.4, [-20, 0, 0]),
                                                             (2.6, 1.0, [-20, 0, 0])], ["void", "void", "flesh_v", "void_glow"])
    ring = m.bone("rift", [0, r["top_y"] + 4, 9], ch)
    for i in range(14):
        a = i * math.pi / 7
        m.box(ring, [math.cos(a) * 11, r["top_y"] + 4 + math.sin(a) * 11, 9], [5.2, 1.2, 0.6], "void_glow", rot=[0, 0, math.degrees(a) + 90])
    for side, sx in (("right", -1), ("left", 1)):
        hand = r[f"{side}_hand"]
        x, y = r[f"{side}_x"], r[f"{side}_hand_y"]
        for dz, rz in ((-2.2, 12), (0, 0), (2.2, -12)):
            m.box(hand, [x, y - 2.6, dz], [1.4, 6, 1.4], "teeth", rot=[-20, 0, rz * sx], pivot=[x, y + 0.6, dz])
    tent = {}
    for i in range(4):
        for j in range(4):
            tent[f"tent{i}_{j}"] = {"idle": (lambda ii, jj: lambda T: {"rotation": sway(T, 8 + jj * 4, ii * 0.2 + jj * 0.15, 0)})(i, j),
                                    "walk": (lambda ii, jj: lambda L: {"rotation": sway(L, 12 + jj * 4, ii * 0.2 + jj * 0.15, 0)})(i, j)}
    tent["rift"] = {"idle": lambda T: {"rotation": kf([(0, [0, 0, 0]), (T, [0, 0, 120])], smooth=False)},
                    "cast": lambda C: {"rotation": kf([(0, [0, 0, 0]), (C, [0, 0, 360])], smooth=False), "scale": sway(C, 0.1, 0, 0, [1.1, 1.1, 1.1])}}
    tent["jaw"] = {"idle": lambda T: {"rotation": sway(T, 8, 0, 0, [10, 0, 0])}, "cast": lambda C: {"rotation": [30, 0, 0]}}
    motions(m, r, Persona(weight=1.0, hunch=14, menace=1.6), extra=tent)
    return m


def hourglass():
    """Песочные часы Хронофага: золотая рама, светящийся песок сыплется, вращаются."""
    m = Model("hourglass")
    b = m.bone("glass", [0, 10, 0])
    m.box(b, [0, 17.4, 0], [8, 1.2, 8], "time_gold", decal={"up": rivets})
    m.box(b, [0, 2.6, 0], [8, 1.2, 8], "time_gold")
    for dx, dz in ((-3.4, -3.4), (3.4, -3.4), (-3.4, 3.4), (3.4, 3.4)):
        m.box(b, [dx, 10, dz], [0.8, 14, 0.8], "time_gold")
    for i, w in enumerate((5.6, 4.4, 3, 1.4, 3, 4.4, 5.6)):
        m.box(b, [0, 4.4 + i * 1.8, 0], [w, 1.8, w], "mirror" if i != 3 else "sand")
    m.box(b, [0, 6.4, 0], [4.2, 2.6, 4.2], "sand")
    m.box(b, [0, 10, 0], [0.6, 5, 0.6], "star_glow")
    m.anim("idle", loop(4.0, {"glass": {"rotation": kf([(0, [0, 0, 0]), (4, [0, 360, 0])], smooth=False), "position": sway(4, 0.6, 0, 1)}}))
    return m


def void_eye():
    """Глаз бездны: парящее око в кольце кристаллов, зрачок следит."""
    m = Model("void_eye")
    eye = m.bone("eye", [0, 10, 0])
    m.box(eye, [0, 10, 0], [8, 8, 8], "flesh_v")
    m.box(eye, [0, 10, -4.1], [6, 6, 0.4], "void_eye")
    m.box(eye, [0, 10, -4.4], [2.4, 3.4, 0.4], "shade")
    ring = m.bone("ring", [0, 10, 0])
    for i in range(6):
        a = i * math.pi / 3
        m.box(ring, [math.cos(a) * 6.4, 10 + math.sin(a) * 6.4, 1], [1.4, 3, 1.4], "crystal_v", rot=[0, 0, math.degrees(a) + 90])
    z = [0, 0, 0]
    m.anim("idle", loop(3.0, {"eye": {"rotation": kf([(0, z), (0.8, [0, 20, 0]), (1.6, [10, -20, 0]), (2.4, [-8, 0, 0]), (3, z)]),
                                      "position": sway(3, 0.8, 0, 1)},
                              "ring": {"rotation": kf([(0, z), (3, [0, 0, 360])], smooth=False)}}))
    return m


MODELS = {"void_sentinel": void_sentinel, "light_eater": light_eater, "wanderer": wanderer, "warped_archer": warped_archer,
          "colossus": colossus, "shadow": shadow, "shard": shard, "doppel": doppel, "chronophage": chronophage,
          "xalathar": xalathar, "hourglass": hourglass, "void_eye": void_eye}
