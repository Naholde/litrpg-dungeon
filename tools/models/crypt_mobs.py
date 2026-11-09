"""Этап 1 · нежить склепа: воин, лучник, костяной страж, послушник, гуль, костяная крыса."""
import hy
from hy import Model, mat, kf, loop, once, sway, beats
from rig import H, humanoid, motions, Persona, cloth_sway
from crypt import skull, rivets, tatter, plate_rows, shackle, skull_face

mat("sk_bone", "#DCD0B2", "bone", dark="#8A7C5E", light="#FAF2DC")
mat("sk_bone2", "#B8AA8A", "bone", dark="#6E6248", light="#D8CCAC")
mat("sk_rust", "#7A4A30", "rust", dark="#3E2418", light="#B0703E")
mat("sk_iron", "#5E646E", "metal", dark="#30343A", light="#A0AAB6", gloss=0.4)
mat("sk_cloth", "#3A2E2A", "cloth", dark="#1C1614", light="#5E4C44")
mat("sk_hood", "#262A2E", "cloth", dark="#121416", light="#444A52")
mat("sk_wood", "#6A4A2C", "wood", dark="#3A2816", light="#9A7046")
mat("sk_string", "#D8D0B8", "plain", dark="#A09880", light="#FFFFFF")
mat("sk_feather", "#5A2A2A", "fur", dark="#2A1212", light="#8A4A40")
mat("ac_robe", "#1E1E26", "cloth", dark="#0C0C12", light="#3A3A48")
mat("ac_green", "#2E5A36", "cloth", dark="#16301A", light="#4E8A56")
mat("ac_gem", "#6CFF86", "crystal", glow=True, dark="#1E9A3A", light="#E8FFEC")
mat("gh_skin", "#8A9A7A", "skin", dark="#525E48", light="#B4C2A2")
mat("gh_rot", "#5A4A5E", "skin", dark="#2E2232", light="#7E6A84")
mat("gh_claw", "#2A2622", "bone", dark="#141210", light="#4A443C")
mat("gh_rag", "#5A2A22", "cloth", dark="#2C1410", light="#84463A")


def blue_eyes_face(f):
    skull_face(f)


def ghoul_face(f):
    """Гуль: впалые щёки, широкая пасть с иглами-зубами, зелёные глаза без зрачков."""
    w, h = f.w, f.h
    for sx in (int(w * 0.18), int(w * 0.6)):
        f.rect(sx, int(h * 0.3), sx + int(w * 0.22), int(h * 0.3) + 3, "#1A1E14")
        f.rect(sx + 1, int(h * 0.3) + 1, sx + int(w * 0.22) - 1, int(h * 0.3) + 3, "#9CFF7A", glow=True)
    f.shade(0, int(h * 0.3) - 2, w, int(h * 0.3), 0.7)
    my = int(h * 0.62)
    f.rect(int(w * 0.12), my, int(w * 0.88), my + int(h * 0.26), "#1A0E10")
    for x in range(int(w * 0.14), int(w * 0.86), 2):
        f.set(x, my, "#E8E0C8")
        f.set(x + 1, my + int(h * 0.26) - 1, "#E8E0C8")
    f.shade(0, int(h * 0.45), int(w * 0.15), int(h * 0.6), 0.75)
    f.shade(int(w * 0.85), int(h * 0.45), w, int(h * 0.6), 0.75)


# ---------------------------------------------------------------------------- костяк скелета

def skeleton(m, height=32, B="sk_bone", S="sk_bone2", skull_w=8.4, wide=1.0, eyes=None):
    """Скелет с теми же костями, что у rig.humanoid (body, chest, head, руки с предплечьями и кистями, ноги
    с голенями) — подходят общие движения. Рёбра, позвоночник, суставы, череп из частей."""
    k = height / 32
    r = {}
    hip = 13 * k
    r["body"] = m.bone("body", [0, hip, 0])
    m.box(r["body"], [0, hip + 0.4, 0.2], [7.2 * wide, 2.6, 4.4], B)                      # таз
    for i in range(3):
        m.box(r["body"], [0, hip + 2.4 + i * 1.3 * k, 1.2], [2.2, 1.0, 2.2], S)            # поясница
    cy = hip + 5.6 * k
    r["chest"] = m.bone("chest", [0, cy, 0.6], r["body"])
    m.box(r["chest"], [0, cy + 4.4 * k, 2.0], [2.4, 9 * k, 2.2], B)                         # позвоночник
    m.box(r["chest"], [0, cy + 4.6 * k, -2.6], [1.6, 6.4 * k, 1.2], B)                      # грудина
    for i in range(4):
        y = cy + 7.6 * k - i * 1.9 * k
        half = (4.8 - abs(i - 1) * 0.4) * wide
        m.mirror(r["chest"], [half - 1.2, y, -2.8], [1.2, 1.1, 5.4], B)
        m.mirror(r["chest"], [0.8, y, -3.3], [half - 1.8, 1.1, 1.0], S if i % 2 else B)
        m.mirror(r["chest"], [1.0, y, 2.4], [half - 2.0, 1.1, 0.9], S)
    m.mirror(r["chest"], [0.6, cy + 9.2 * k, -2.6], [5.2 * wide, 1.3, 1.4], B)            # ключицы
    top = cy + 10 * k
    r["head"] = m.bone("head", [0, top, 0], r["chest"])
    m.box(r["head"], [0, top + 0.9, 0.6], [2.2, 2.0, 2.2], S)                               # шея
    hy0 = top + 4.0
    r["jaw"], r["head_top"] = skull(m, r["head"], hy0, skull_w, B, S, face=eyes, jaw_bone="jaw")
    r["head_y"], r["face_z"] = hy0, -0.4 - skull_w * 0.47
    sh = top - 0.8
    for side, sx in (("right", -1), ("left", 1)):
        x = sx * (5.0 * wide + 0.6)
        arm = m.bone(f"{side}_arm", [x, sh, 0], r["chest"])
        m.box(arm, [x, sh, 0], [2.8, 2.6, 2.8], S)                                          # плечо
        m.box(arm, [x, sh - 3.4 * k, 0], [1.8, 6 * k, 1.8], B)
        ey = sh - 6.6 * k
        fore = m.bone(f"{side}_forearm", [x, ey, 0], arm)
        m.box(fore, [x, ey, 0], [2.2, 1.8, 2.2], S)
        m.box(fore, [x - 0.3 * sx, ey - 2.9 * k, -0.4], [1.1, 5.4 * k, 1.1], B)
        m.box(fore, [x + 0.5 * sx, ey - 2.9 * k, 0.4], [0.9, 5.2 * k, 0.9], S)
        hy_ = ey - 5.8 * k
        hand = m.bone(f"{side}_hand", [x, hy_, 0], fore)
        m.box(hand, [x, hy_ - 1.0, 0], [2.6, 2.0, 2.6], B)
        for dz in (-0.8, 0.0, 0.8):
            m.box(hand, [x, hy_ - 2.6, dz], [0.7, 1.8, 0.7], S)
        r[f"{side}_arm"], r[f"{side}_forearm"], r[f"{side}_hand"], r[f"{side}_x"] = arm, fore, hand, x
        r[f"{side}_hand_y"] = hy_ - 2.4
    for side, sx in (("right", -1), ("left", 1)):
        x = sx * 2.4 * wide
        leg = m.bone(f"{side}_leg", [x, hip, 0])
        m.box(leg, [x, hip - 0.4, 0], [2.6, 1.8, 2.6], S)
        m.box(leg, [x, hip - 3.6 * k, 0], [1.9, 6 * k, 1.9], B)
        ky = hip - 6.6 * k
        shin = m.bone(f"{side}_shin", [x, ky, 0], leg)
        m.box(shin, [x, ky, -0.2], [2.4, 1.8, 2.4], S)
        m.box(shin, [x, ky / 2 + 0.6, 0], [1.6, ky - 2.2, 1.6], B)
        m.box(shin, [x, 0.6, -0.8], [2.8, 1.2, 4.2], B)
        r[f"{side}_leg"], r[f"{side}_shin"] = leg, shin
    r["hip_y"], r["top_y"], r["shoulder_y"], r["chest_y"] = hip, top, sh, cy
    return r


# ---------------------------------------------------------------------------- воин склепа

def crypt_warrior():
    """Воин склепа: скелет в ржавом шлеме с гребнем, кольчужный нагрудник, рваный табард, щербатый меч и баклер."""
    m = Model("crypt_warrior")
    r = skeleton(m, 32)
    hb, top = r["head"], r["head_top"]
    m.box(hb, [0, top - 1.6, -0.4], [9.4, 4.0, 8.8], "sk_rust", decal={"north": plate_rows})   # шлем
    m.box(hb, [0, top + 1.0, -0.2], [1.4, 2.2, 8.0], "sk_iron")                                  # гребень
    m.box(hb, [0, top - 3.0, -4.8], [1.4, 3.0, 0.8], "sk_iron")                                  # наносник
    m.mirror(hb, [4.0, top - 6.0, -2.6], [0.9, 4.0, 4.8], "sk_rust")                              # нащёчники
    ch = r["chest"]
    m.box(ch, [0, r["chest_y"] + 5.6, -0.2], [10.6, 5.0, 6.4], "chain")                         # кольчуга
    m.mirror(ch, [4.4, r["shoulder_y"] - 0.4, -2.0], [2.6, 2.4, 5.0], "sk_rust", rot=[0, 0, 0])
    tb = m.bone("tabard", [0, r["chest_y"] + 2.6, -3.4], ch)
    m.box(tb, [0, r["chest_y"] - 2.2, -3.6], [4.6, 11, 0.4], "cloth_red", decal={"north": tatter, "south": tatter})
    m.box(r["body"], [0, r["hip_y"] + 1.6, 0.2], [8.4, 1.2, 5.2], "leather_dark", decal={"north": rivets})
    rh = r["right_hand"]
    x, y = r["right_x"], r["right_hand_y"]
    sw = m.bone("sword", [x, y + 1.2, 0], rh, rot=[70, 0, 0])
    m.box(sw, [x, y + 1.2, 0], [1.2, 3.2, 1.2], "leather_dark")
    m.box(sw, [x, y + 3.2, 0], [4.2, 0.9, 1.4], "sk_rust")
    m.box(sw, [x, y + 9.6, 0], [1.8, 12, 0.6], "sk_iron", decal={"east": lambda f: f.cut(f.w - 1, f.h // 3), "west": lambda f: f.cut(0, f.h // 2)})
    m.box(sw, [x, y + 16, 0], [1.0, 1.0, 0.5], "sk_iron")
    lh = r["left_hand"]
    xl, yl = r["left_x"], r["left_hand_y"]
    m.box(lh, [xl + 1.6, yl + 2.6, -0.6], [0.8, 5.6, 5.6], "sk_rust", decal={"east": shackle, "west": shackle})   # баклер
    m.box(lh, [xl + 2.1, yl + 2.6, -0.6], [0.5, 1.8, 1.8], "sk_iron")
    motions(m, r, Persona(weight=0.45, hunch=4), extra={"tabard": cloth_sway(8, 0.2)})
    return m


def crypt_archer():
    """Лучник склепа: скелет в рваном капюшоне и шарфе, колчан со стрелами за спиной, длинный кривой лук."""
    m = Model("crypt_archer")
    r = skeleton(m, 32)
    hb, top, y0 = r["head"], r["head_top"], r["head_y"]
    m.box(hb, [0, top - 0.2, 0.2], [10, 2.0, 9.6], "sk_hood")
    m.mirror(hb, [4.4, y0 - 2.6, -4.2], [1.0, top - y0 + 2.4, 9.0], "sk_hood")
    m.box(hb, [0, (y0 + top) / 2 - 1, 4.6], [10, top - y0 + 3, 1.0], "sk_hood")
    m.box(hb, [0, top - 1.0, -4.7], [10, 1.6, 1.2], "sk_hood")
    m.segments("hood_tail", hb, [0, top - 1, 5], [(3, 2.6, [60, 0, 0]), (3, 2.0, [20, 0, 0]), (2.4, 1.4, [20, 0, 0])], "sk_hood", up=False)
    ch = r["chest"]
    m.box(ch, [0, r["top_y"] + 0.2, -0.4], [8.4, 2.4, 6.6], "cloth_red")                        # шарф
    sc = m.bone("scarf", [-2, r["top_y"], -3.4], ch)
    m.box(sc, [-2, r["top_y"] - 4, -3.6], [2.2, 7.6, 0.4], "cloth_red", decal={"north": tatter, "south": tatter})
    qv = m.bone("quiver", [2, r["chest_y"] + 4, 3.6], ch, rot=[0, 0, -24])
    m.box(qv, [2, r["chest_y"] + 4, 3.8], [3.0, 10, 3.0], "leather", decal={"north": rivets})
    for i, dx in enumerate((-0.8, 0.2, 1.0)):
        m.box(qv, [2 + dx, r["chest_y"] + 10.2, 3.8 + (i - 1) * 0.6], [0.4, 3.4, 0.4], "sk_wood")
        m.box(qv, [2 + dx, r["chest_y"] + 11.6, 3.8 + (i - 1) * 0.6], [0.9, 1.4, 0.3], "sk_feather")
    lh = r["left_hand"]
    xl, yl = r["left_x"], r["left_hand_y"] + 1.2
    bw = m.bone("bow", [xl, yl, -0.8], lh)
    m.box(bw, [xl, yl, -0.8], [1.4, 2.6, 1.4], "leather_dark")
    m.segments("bow_up", bw, [xl, yl + 1.2, -0.8], [(4, 1.1, [-16, 0, 0]), (4, 0.9, [-16, 0, 0]), (3, 0.8, [-14, 0, 0])], "sk_wood")
    m.segments("bow_dn", bw, [xl, yl - 1.2, -0.8], [(4, 1.1, [16, 0, 0]), (4, 0.9, [16, 0, 0]), (3, 0.8, [14, 0, 0])], "sk_wood", up=False)
    m.box(bw, [xl, yl, 1.8], [0.25, 18, 0.25], "sk_string")
    motions(m, r, Persona(weight=0.3, hunch=6, left=False), extra={"scarf": cloth_sway(10, 0.2), "hood_tail0": cloth_sway(6, 0.3)})
    return m


def bone_guard():
    """Костяной страж: широкий скелет в стальной кирасе и рогатом шлеме, башенный щит с черепом, тяжёлая булава."""
    m = Model("bone_guard")
    r = skeleton(m, 34, wide=1.2, skull_w=8.8)
    hb, top = r["head"], r["head_top"]
    m.box(hb, [0, top - 1.8, -0.4], [9.8, 4.4, 9.2], "sk_iron", decal={"north": plate_rows})
    m.box(hb, [0, top - 4.6, -4.9], [8.0, 1.0, 0.8], "sk_iron")                                 # забрало-щель
    for sx in (-1, 1):
        m.segments(f"horn_{'r' if sx < 0 else 'l'}", hb, [4.6 * sx, top - 1.2, 0],
                   [(2.4, 2.0, [0, 0, 70 * sx]), (2.2, 1.6, [0, 0, -40 * sx]), (2.0, 1.2, [-10, 0, -30 * sx])], "sk_bone2")
    ch = r["chest"]
    m.box(ch, [0, r["chest_y"] + 5.0, -0.2], [11.8, 8.2, 7.0], "sk_iron", decal={"north": plate_rows, "south": plate_rows})
    m.mirror(ch, [5.2, r["shoulder_y"], -2.6], [3.6, 3.0, 5.8], "sk_iron", decal={"up": rivets})
    m.box(r["body"], [0, r["hip_y"] + 0.4, -0.2], [9.6, 3.4, 6.0], "chain")
    lh = r["left_hand"]
    xl, yl = r["left_x"], r["left_hand_y"]
    sh = m.bone("shield", [xl, yl + 3, -2], lh)
    m.box(sh, [xl + 0.4, yl + 4, -3.6], [9, 16, 1.4], "sk_rust", decal={"north": plate_rows})
    m.box(sh, [xl + 0.4, yl + 4, -4.4], [10, 1.2, 0.6], "sk_iron")
    m.box(sh, [xl + 0.4, yl + 6.6, -4.5], [3.4, 3.4, 0.6], "sk_bone", decal={"north": lambda f: (f.rect(1, 2, 3, 4, "#140E10"), f.rect(f.w - 3, 2, f.w - 1, 4, "#140E10"))})
    rh = r["right_hand"]
    x, y = r["right_x"], r["right_hand_y"]
    mc = m.bone("mace", [x, y + 1, 0], rh, rot=[60, 0, 0])
    m.box(mc, [x, y + 3, 0], [1.2, 9, 1.2], "sk_wood")
    m.box(mc, [x, y + 8.6, 0], [3.6, 3.6, 3.6], "sk_iron", decal={"north": rivets, "south": rivets})
    for dx, dz in ((2.1, 0), (-2.1, 0), (0, 2.1), (0, -2.1)):
        m.box(mc, [x + dx, y + 8.6, dz], [0.9, 1.4, 0.9], "steel")
    motions(m, r, Persona(weight=0.8, hunch=2))
    return m


def acolyte():
    """Послушник склепа: скелет-культист в рясе с зелёной вышивкой, капюшон, свечной посох с зелёным камнем, чётки из позвонков."""
    m = Model("acolyte")
    h = H(height=33, head=8, shoulders=9, hips=7.4, depth=4.8, leg=12, arm=12, limb=2.4, hand=2.6)
    r = humanoid(m, h, {"skin": "sk_bone", "torso": "ac_robe", "arm": "ac_robe", "forearm": "sk_bone", "hand": "sk_bone",
                        "leg": "ac_robe", "boot": "ac_robe", "head": "sk_bone", "face": blue_eyes_face}, arm_out=7)
    hb, top, y0 = r["head"], r["head_top"], r["head_y"]
    s = h.head + 1.6
    m.box(hb, [0, top + 0.6, 0], [s, 1.6, s + 0.4], "ac_robe")
    m.box(hb, [0, top - 0.3, -s / 2], [s, 1.4, 1.2], "ac_robe")
    m.mirror(hb, [h.head / 2 + 0.05, y0 - 0.6, -s / 2 - 0.2], [1.1, top - y0 + 1.4, s], "ac_robe")
    m.box(hb, [0, (y0 + top) / 2, s / 2], [s, top - y0 + 1.6, 1.0], "ac_robe")
    ch = r["chest"]
    m.box(ch, [0, r["chest_y"] + 2, -h.depth / 2 - 0.4], [2.6, 8, 0.5], "ac_green", decal={"north": lambda f: [f.set(f.w // 2, y, "#6CFF86", True) for y in range(1, f.h, 3)]})
    for i in range(7):
        a = i / 6
        m.box(ch, [-3.4 + a * 6.8, r["top_y"] - 1 - abs(a - 0.5) * 6, -h.depth / 2 - 0.7], [0.9, 0.9, 0.9], "sk_bone2")   # чётки
    body = r["body"]
    m.box(body, [0, r["hip_y"] - 2, 0.2], [h.hips + 2.6, 6, h.depth + 2.2], "ac_robe")
    fr = m.bone("robe_front", [0, r["hip_y"], -2.6], body)
    m.box(fr, [0, r["hip_y"] - 7, -2.9], [6.4, 10, 0.5], "ac_robe", decal={"north": tatter, "south": tatter})
    bk = m.bone("robe_back", [0, r["hip_y"], 2.8], body)
    m.box(bk, [0, r["hip_y"] - 7.4, 3.0], [8.6, 11, 0.5], "ac_robe", decal={"north": tatter, "south": tatter})
    rh = r["right_hand"]
    x, y = r["right_x"], r["right_hand_y"] + 1
    st = m.bone("staff", [x, y, 0], rh, rot=[10, 0, -4])
    m.box(st, [x, y + 2, 0], [1.0, 26, 1.0], "sk_wood")
    m.box(st, [x, y + 15.6, 0], [2.4, 2.4, 2.4], "ac_gem", rot=[45, 0, 45])
    for dx in (-1.6, 1.6):
        m.box(st, [x + dx, y + 14.6, 0], [0.5, 2.2, 0.5], "sk_bone2")
    motions(m, r, Persona(weight=0.3, caster=True, hunch=8),
            extra={"robe_front": cloth_sway(6, 0.2), "robe_back": cloth_sway(7, 0.45, 4)})
    return m


def ghoul():
    """Гуль: сгорбленный трупоед, длинные руки с чёрными когтями, рёбра проступают, лохмотья, пасть до ушей."""
    m = Model("ghoul")
    h = H(height=30, head=7.6, shoulders=10, hips=7, depth=6, leg=11, arm=15, limb=2.8, hand=3.6, lean=26)
    r = humanoid(m, h, {"skin": "gh_skin", "torso": "gh_skin", "arm": "gh_skin", "hand": "gh_skin", "leg": "gh_rot",
                        "boot": "gh_skin", "head": "gh_skin", "face": ghoul_face, "pelvis": "gh_rag"}, arm_out=12)
    ch = r["chest"]
    for i in range(4):
        m.box(ch, [0, r["chest_y"] + 1.2 + i * 1.6, -h.depth / 2 - 0.4], [h.shoulders - 2.2 - i * 0.2, 0.8, 0.6], "gh_rot")   # рёбра
    m.box(ch, [0, r["top_y"] - 1, 2.8], [5, 4, 2.2], "gh_rot")                                     # горб
    for i in range(4):
        m.box(ch, [0, r["top_y"] - 0.4 - i * 2.2, 4.0], [1.2, 1.2, 1.4], "gh_skin")                 # позвонки
    hb = r["head"]
    m.mirror(hb, [3.4, r["head_y"] + 6.6, 0.4], [1.6, 3.6, 1.2], "gh_skin", rot=[0, 0, -30], pivot=[3.6, r["head_y"] + 6.2, 0.4])  # уши
    for side, sx in (("right", -1), ("left", 1)):
        hand = r[f"{side}_hand"]
        x, y = r[f"{side}_x"], r[f"{side}_hand_y"]
        for dz in (-1.2, 0, 1.2):
            m.box(hand, [x, y - 1.2, dz - 0.6], [0.7, 3.2, 0.7], "gh_claw", rot=[-24, 0, 0], pivot=[x, y + 0.2, dz])
    rag = m.bone("rag", [0, r["hip_y"] + 1, -2.6], r["body"])
    m.box(rag, [0, r["hip_y"] - 3, -3.2], [6, 7, 0.4], "gh_rag", decal={"north": tatter, "south": tatter})
    motions(m, r, Persona(weight=0.35, hunch=14, speed=1.3), extra={"rag": cloth_sway(9, 0.2)})
    z = [0, 0, 0]
    m.anim("attack", once(0.6, {
        "right_arm": {"rotation": beats(0.6, [(0, z), (0.35, [-150, 30, 40]), (0.55, [-40, -20, -10]), (1, z)])},
        "left_arm": {"rotation": beats(0.6, [(0, z), (0.45, [-140, -30, -40]), (0.7, [-30, 20, 10]), (1, z)])},
        "chest": {"rotation": beats(0.6, [(0, [14, 0, 0]), (0.35, [-10, 0, 0]), (0.6, [30, 0, 0]), (1, [14, 0, 0])])},
        "head": {"rotation": beats(0.6, [(0, z), (0.4, [-30, 0, 0]), (0.65, [20, 0, 0]), (1, z)])},
    }))
    return m


def bone_rat():
    """Костяная крыса: скелет крысы с длинным позвонковым хвостом, красные глаза, жёлтые резцы."""
    m = Model("bone_rat")
    body = m.bone("body", [0, 4, 0])
    m.box(body, [0, 4.6, 0.6], [4.4, 3.6, 8], "sk_bone")
    for i in range(4):
        m.box(body, [0, 4.6, -2.4 + i * 2], [5.0, 0.7, 0.7], "sk_bone2")
    m.box(body, [0, 6.6, 0.8], [1.2, 0.8, 8.4], "sk_bone2")
    hd = m.bone("head", [0, 5, -3.4], body)
    m.box(hd, [0, 5.2, -5.4], [3.6, 3.2, 4], "sk_bone", decal={"north": lambda f: (f.rect(0, 1, 2, 3, "#140C0C"), f.rect(f.w - 2, 1, f.w, 3, "#140C0C"), f.set(0, 2, "#FF4A3A", True), f.set(f.w - 1, 2, "#FF4A3A", True))})
    m.box(hd, [0, 4.6, -8.2], [2.0, 2.0, 2.2], "sk_bone")
    m.box(hd, [0, 3.4, -8.8], [1.2, 1.0, 0.6], "eye_gold")
    m.mirror(hd, [1.0, 7.2, -4.8], [1.6, 1.8, 0.6], "sk_bone2")
    m.segments("tail", body, [0, 5, 4.6], [(2.4, 1.0, [-80, 0, 0]), (2.4, 0.8, [-10, 0, 0]), (2.4, 0.6, [-10, 0, 0]), (2.0, 0.5, [-10, 0, 0])], "sk_bone2")
    legs = {}
    for name, x, z in (("leg_fl", -1.8, -2.4), ("leg_fr", 1.8, -2.4), ("leg_bl", -1.8, 3.0), ("leg_br", 1.8, 3.0)):
        lb = m.bone(name, [x, 3.6, z], body)
        m.box(lb, [x, 1.8, z], [0.9, 3.6, 0.9], "sk_bone")
        m.box(lb, [x, 0.3, z - 0.6], [1.4, 0.6, 1.8], "sk_bone2")
        legs[name] = lb
    z0 = [0, 0, 0]
    m.anim("idle", loop(1.6, {"tail0": {"rotation": sway(1.6, 10, 0, 1)}, "tail2": {"rotation": sway(1.6, 14, 0.3, 1)},
                              "head": {"rotation": kf([(0, z0), (0.4, [8, 10, 0]), (0.8, z0), (1.2, [8, -10, 0]), (1.6, z0)])},
                              "body": {"position": sway(0.8, 0.15, 0, 1)}}))
    m.anim("walk", loop(0.4, {"leg_fl": {"rotation": sway(0.4, 40, 0, 0)}, "leg_br": {"rotation": sway(0.4, 40, 0, 0)},
                              "leg_fr": {"rotation": sway(0.4, 40, 0.5, 0)}, "leg_bl": {"rotation": sway(0.4, 40, 0.5, 0)},
                              "tail0": {"rotation": sway(0.4, 20, 0.2, 1)}, "tail2": {"rotation": sway(0.4, 24, 0.5, 1)},
                              "body": {"position": sway(0.2, 0.3, 0, 1)}}))
    m.anim("attack", once(0.45, {"head": {"rotation": beats(0.45, [(0, z0), (0.35, [-35, 0, 0]), (0.6, [25, 0, 0]), (1, z0)])},
                                 "body": {"position": beats(0.45, [(0, z0), (0.35, [0, 0.6, 1.2]), (0.6, [0, 0, -2]), (1, z0)])}}))
    m.anim("death", once(0.8, {"body": {"rotation": beats(0.8, [(0, z0), (1, [0, 0, 180])]),
                                        "position": beats(0.8, [(0, z0), (0.5, [0, 2, 0]), (1, [0, 2.4, 0])])}}, hold=True))
    for nm, ln, core in (("shoot", 0.6, "head"), ("cast", 0.8, "body"), ("block", 0.5, "body"), ("leap", 0.6, "body"), ("hurt", 0.3, "body")):
        m.anim(nm, once(ln, {core: {"rotation": beats(ln, [(0, z0), (0.4, [-20, 0, 0]), (1, z0)])}}))
    return m


MODELS = {"crypt_warrior": crypt_warrior, "crypt_archer": crypt_archer, "bone_guard": bone_guard, "acolyte": acolyte,
          "ghoul": ghoul, "bone_rat": bone_rat}
