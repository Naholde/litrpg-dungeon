"""Этап 3 · Затопленный храм — утопленник, жрец глубин, мурена, крабоид, медуза, гарпунщик;
хранители Страж Прилива, Левиафан, Жрица Глубин Наяра."""
import math
import hy
from hy import Model, mat, kf, loop, once, sway, beats
from rig import H, humanoid, motions, Persona, cloth_sway, features
from crypt import rivets, tatter, plate_rows

mat("pris", "#4E9A8C", "runes", dark="#22524A", light="#8AD4C4", glow="pattern", accent="#9FF0E8")
mat("pris_plate", "#3E8478", "metal", dark="#1E4440", light="#7CC8BA", gloss=0.5)
mat("pris_dark", "#24504A", "stone", dark="#102824", light="#3E7A70")
mat("drown", "#5E8A7C", "skin", dark="#34564C", light="#86B4A4")
mat("drown_rot", "#3E5E52", "skin", dark="#1E342C", light="#5E8476")
mat("kelp", "#3A6A2A", "cloth", dark="#1C3A12", light="#5E9444")
mat("coral", "#D8507A", "plain", dark="#8A2A48", light="#FF8AAA")
mat("coral_b", "#4A7AE0", "plain", dark="#203E90", light="#8AB0FF")
mat("pearl", "#EEF6FA", "glow", glow=True, light="#FFFFFF", dark="#A8C8D8")
mat("sea_glow", "#9FF0E8", "glow", glow=True, light="#FFFFFF", dark="#40B8A8")
mat("shell", "#C8603A", "chitin", dark="#7A3018", light="#F09060", gloss=0.5)
mat("shell_l", "#E8A070", "chitin", dark="#A0603A", light="#FFD0A8")
mat("scale_teal", "#2A8A8A", "scales", dark="#0E4444", light="#6ACACA")
mat("scale_deep", "#1E4A6A", "scales", dark="#0A1E32", light="#4A86AA")
mat("belly", "#C8C08A", "scales", dark="#8A8256", light="#F0E8B8")
mat("eel", "#5A6A30", "scales", dark="#2E3816", light="#8A9A52")
mat("jelly", "#C87FFF", "glow", glow=True, light="#FFE0FF", dark="#8A3AD0")
mat("jelly_body", "#B47AF0", "glow", glow=True, dark="#7A40C0", light="#F6E0FF")
mat("gold_sea", "#C8A040", "metal", dark="#7A5A18", light="#F6DC8A", gloss=0.7)
mat("priest_robe", "#1E3A4A", "cloth", dark="#0E1E26", light="#3A6478")
mat("naiad_skin", "#BFE4E8", "skin", dark="#7AA8B0", light="#ECFAFC")
mat("naiad_hair", "#2A7A9A", "hair", dark="#103A50", light="#6AC0E0")
mat("eye_sea", "#7FFFF0", "glow", glow=True, light="#FFFFFF", dark="#20B0A0")
mat("rope", "#B89A6A", "leather", dark="#7A6040", light="#E0C494")


def fish_face(f):
    """Шлем-раковина Стража: щель забрала, светящиеся глаза в глубине."""
    w, h = f.w, f.h
    f.rect(int(w * 0.15), int(h * 0.38), int(w * 0.85), int(h * 0.56), "#08181A")
    for sx in (int(w * 0.28), int(w * 0.62)):
        f.rect(sx, int(h * 0.42), sx + 2, int(h * 0.52), "#7FFFF0", glow=True)
    f.rect(int(w * 0.48), int(h * 0.56), int(w * 0.52), h, "#24504A")
    plate_rows(f)


def drowned_face(f):
    w, h = f.w, f.h
    for sx in (int(w * 0.2), int(w * 0.6)):
        f.rect(sx, int(h * 0.38), sx + 3, int(h * 0.38) + 3, "#0A1A16")
        f.rect(sx + 1, int(h * 0.38) + 1, sx + 2, int(h * 0.38) + 2, "#7FFFF0", glow=True)
    f.rect(int(w * 0.3), int(h * 0.72), int(w * 0.7), int(h * 0.82), "#102620")
    for x in range(int(w * 0.3), int(w * 0.7), 2):
        f.set(x, int(h * 0.72), "#C8D0B0")
    f.line(int(w * 0.1), int(h * 0.1), int(w * 0.3), int(h * 0.3), "#34564C")


def naiad_face(f):
    w, h = f.w, f.h
    for sx in (int(w * 0.22), int(w * 0.6)):
        f.rect(sx - 1, int(h * 0.38) - 1, sx + 4, int(h * 0.38), "#1E5A6A")
        f.rect(sx, int(h * 0.38) + 1, sx + 3, int(h * 0.38) + 3, "#7FFFF0", glow=True)
        f.set(sx + 1, int(h * 0.38) + 1, "#FFFFFF", glow=True)
    for i in range(4):
        f.set(int(w * 0.12) + i, int(h * 0.6) + i % 2, "#6AC0E0")
        f.set(int(w * 0.84) - i, int(h * 0.6) + i % 2, "#6AC0E0")                         # чешуйки на скулах
    f.rect(int(w * 0.42), int(h * 0.76), int(w * 0.58), int(h * 0.76) + 1, "#4A7A8A")


def scale_stripe(f):
    for y in range(0, f.h, 6):
        f.rect(0, y, f.w, y + 1, "#6ACACA")


# ---------------------------------------------------------------------------- Страж Прилива

def tide_warden():
    """Страж Прилива: рыцарь в призмариновых латах, шлем-раковина с гребнем-плавником, коралл растёт на наплечниках,
    щит-раковина и трезубец; на поясе — цепи с жемчугом, по латам светятся руны."""
    m = Model("tide_warden")
    h = H(height=44, head=8.6, shoulders=13, hips=9, depth=7, leg=15, arm=15, limb=3.8, hand=4.0, neck=0.4)
    r = humanoid(m, h, {"skin": "drown", "torso": "pris", "arm": "pris_plate", "forearm": "pris_plate", "hand": "drown",
                        "leg": "pris_plate", "boot": "pris_dark", "head": "pris_plate", "face": fish_face, "pelvis": "pris_dark"},
                 arm_out=8)
    hb, top, y0 = r["head"], r["head_top"], r["head_y"]
    m.box(hb, [0, top + 0.2, 0.4], [h.head + 1, 1.4, h.head + 2], "pris_plate")
    m.segments("fin", hb, [0, top + 0.8, -2], [(3.2, 1.0, [30, 0, 0]), (3.0, 0.9, [24, 0, 0]), (2.6, 0.8, [24, 0, 0])], "scale_teal")
    m.mirror(hb, [h.head / 2 - 0.2, y0 + 2, -1], [1.6, 5, 4], "shell", rot=[0, -20, 0])            # жабры-раковины
    ch = r["chest"]
    m.box(ch, [0, r["chest_y"] + 5, -h.depth / 2 - 0.4], [h.shoulders - 2, 6, 0.8], "pris", decal={"north": plate_rows})
    for sx in (-1, 1):
        x = sx * (h.shoulders / 2 + 1)
        arm = r["right_arm" if sx < 0 else "left_arm"]
        m.box(arm, [x, r["shoulder_y"] + 1.4, 0], [5.6, 3.4, 6.4], "pris_plate", decal={"north": plate_rows, "up": rivets})
        for i, (dx, dz, ln, rz) in enumerate(((0.6, -1.6, 4.4, 12), (1.8, 1.0, 3.4, 34), (-0.8, 1.6, 3.0, -14))):
            m.box(arm, [x + dx * sx, r["shoulder_y"] + 3.2 + ln / 2, dz], [1.2, ln, 1.2], "coral" if i != 1 else "coral_b",
                  rot=[0, 0, -rz * sx], pivot=[x + dx * sx, r["shoulder_y"] + 3, dz])
    body = r["body"]
    for i in range(5):
        a = (i - 2) * 2.2
        m.box(body, [a, r["hip_y"] - 1 - abs(i - 2) * 0.6, -h.depth / 2 - 0.4], [1.8, 6, 0.6], "kelp", decal={"north": tatter})
    for i, x in enumerate((-3, 0, 3)):
        m.box(body, [x, r["hip_y"] + 1, -h.depth / 2 - 0.6], [1.2, 1.2, 1.2], "pearl")
    # трезубец (правая рука)
    rh = r["right_hand"]
    x, y = r["right_x"], r["right_hand_y"] + 1.6
    tr = m.bone("trident", [x, y, 0], rh, rot=[14, 0, -6])
    m.box(tr, [x, y + 6, 0], [1.4, 38, 1.4], "gold_sea")
    m.box(tr, [x, y + 25, 0], [5.6, 1.2, 1.4], "gold_sea")
    for dx, ln in ((-2.4, 4.4), (0, 6.0), (2.4, 4.4)):
        m.box(tr, [x + dx, y + 25.6 + ln / 2, 0], [0.9, ln, 0.9], "pris_plate")
        m.box(tr, [x + dx, y + 26 + ln, 0], [0.6, 1.2, 0.6], "sea_glow")
    # щит-раковина (левая рука)
    lh = r["left_hand"]
    xl, yl = r["left_x"], r["left_hand_y"]
    sh = m.bone("shield", [xl, yl + 4, -1], lh)
    for i in range(4):
        w = 9 - i * 1.6
        m.box(sh, [xl + 1.2, yl + 6 - i * 1.2, -2.4 - i * 0.4], [w, 10 - i * 2.2, 0.8], "shell" if i % 2 == 0 else "shell_l")
    motions(m, r, Persona(weight=0.85, hunch=3, menace=1.3), extra={"fin0": cloth_sway(4, 0.2)})
    return m


# ---------------------------------------------------------------------------- Левиафан

def leviathan():
    """Левиафан: морской змей. Голова с рогами и челюстью-капканом, ряды светящихся пятен вдоль боков,
    гребень-плавник на каждом сегменте, плавники-крылья, хвост-лопасть."""
    m = Model("leviathan")
    hd = m.bone("head", [0, 6, -4])
    m.box(hd, [0, 7, -9], [8, 6, 10], "scale_deep", decal={"up": scale_stripe})
    m.box(hd, [0, 9.6, -8], [6, 1.6, 8], "scale_teal")
    for sx in (-1, 1):
        m.box(hd, [3.6 * sx, 8.6, -12.2], [1.6, 1.6, 1.2], "eye_sea")
        m.segments(f"horn_{'r' if sx < 0 else 'l'}", hd, [2.6 * sx, 10, -6], [(3, 1.6, [60, 0, 20 * sx]), (2.6, 1.2, [20, 0, 10 * sx]), (2.0, 0.9, [20, 0, 0])], "shell_l")
        m.box(hd, [4.8 * sx, 6.6, -6], [1.4, 4, 4], "scale_teal", rot=[0, 20 * sx, 0])                # жабры
    jaw = m.bone("jaw", [0, 5, -5], hd)
    m.box(jaw, [0, 4.4, -9.4], [7.4, 2.2, 9.6], "belly")
    for i in range(5):
        for sx in (-1, 1):
            m.box(jaw, [3.0 * sx, 5.9, -13 + i * 1.8], [0.6, 1.4, 0.6], "sp_fang" if "sp_fang" in hy.MATS else "bone")
    m.box(hd, [0, 5.4, -13.4], [6, 1.0, 1.0], "bone")
    parent, z0 = "head", -4
    for i in range(6):
        w = 7.6 * (1 - i * 0.1)
        seg = m.bone(f"seg{i}", [0, 6, z0], parent)
        m.box(seg, [0, 6.6, z0 + 3], [w, w * 0.8, 6.2], "scale_deep", decal={"up": scale_stripe})
        m.box(seg, [0, 6.6 - w * 0.36, z0 + 3], [w * 0.8, 1.2, 6.0], "belly")
        m.box(seg, [0, 6.6 + w * 0.48, z0 + 3], [0.8, 2.6 - i * 0.2, 5], "scale_teal")                # гребень
        for sx in (-1, 1):
            m.box(seg, [(w / 2 + 0.1) * sx, 6.8, z0 + 2 + (i % 2)], [0.4, 0.8, 0.8], "sea_glow")
        if i == 1:
            for sx in (-1, 1):
                fb = m.bone(f"flipper_{'r' if sx < 0 else 'l'}", [w / 2 * sx, 5.6, z0 + 2], seg)
                m.cube(fb, [w / 2 * sx - (8 if sx < 0 else 0), 5.4, z0], [8, 0.6, 5], "scale_teal")
        parent, z0 = seg, z0 + 6
    m.box("seg5", [0, 6.6, z0 + 2], [10, 0.8, 5], "scale_teal")                                        # лопасть
    wave = {f"seg{i}": {"rotation": sway(1.6, 10 + i * 2, i * 0.12, 1)} for i in range(6)}
    wave["head"] = {"rotation": sway(1.6, 6, -0.1, 1)}
    wave["flipper_r"] = {"rotation": sway(1.6, 20, 0, 2)}
    wave["flipper_l"] = {"rotation": sway(1.6, -20, 0, 2)}
    m.anim("walk", loop(1.6, wave))
    idle = {f"seg{i}": {"rotation": sway(3.2, 4 + i, i * 0.1, 1)} for i in range(6)}
    idle["jaw"] = {"rotation": sway(3.2, 4, 0, 0, [4, 0, 0])}
    idle["head"] = {"rotation": sway(3.2, 5, 0.2, 0)}
    m.anim("idle", loop(3.2, idle))
    z = [0, 0, 0]
    m.anim("attack", once(0.7, {"head": {"rotation": beats(0.7, [(0, z), (0.4, [-30, 0, 0]), (0.6, [24, 0, 0]), (1, z)])},
                                "jaw": {"rotation": beats(0.7, [(0, z), (0.4, [40, 0, 0]), (0.62, [0, 0, 0]), (1, z)])}}))
    m.anim("cast", loop(1.4, {"head": {"rotation": sway(1.4, 6, 0, 0, [-30, 0, 0])}, "jaw": {"rotation": [30, 0, 0]}}))
    m.anim("slam", once(1.0, {"head": {"rotation": beats(1.0, [(0, z), (0.4, [-50, 0, 0]), (0.6, [30, 0, 0]), (1, z)])},
                              "seg0": {"rotation": beats(1.0, [(0, z), (0.4, [-20, 0, 0]), (0.6, [10, 0, 0]), (1, z)])}}))
    m.anim("death", once(1.6, {"root": {"rotation": beats(1.6, [(0, z), (1, [0, 0, 160])]),
                                        "position": beats(1.6, [(0, z), (1, [0, -4, 0])])}}, hold=True))
    return m


# ---------------------------------------------------------------------------- Наяра

def naiad():
    """Жрица Глубин Наяра: высокая, кожа в чешуйках, волосы-водоросли до пояса, коралловый венец с жемчугом,
    платье-плавник, ореол-кольцо за спиной, посох-раковина (левая рука)."""
    m = Model("naiad")
    h = H(height=44, head=8.4, shoulders=9.4, hips=7.4, depth=5, leg=16, arm=14, limb=2.6, hand=3.0)
    r = humanoid(m, h, {"skin": "naiad_skin", "torso": "scale_teal", "arm": "naiad_skin", "hand": "naiad_skin",
                        "leg": "scale_teal", "boot": "scale_deep", "head": "naiad_skin", "face": naiad_face}, arm_out=9)
    hb, top, y0 = r["head"], r["head_top"], r["head_y"]
    m.box(hb, [0, top - 0.6, 0.6], [h.head + 1, 2.4, h.head + 1.2], "naiad_hair")
    m.box(hb, [0, y0 + 2.4, h.head / 2 + 0.6], [h.head + 1, 8, 1.4], "naiad_hair")
    hair = m.bone("hair", [0, y0 + 1, h.head / 2 + 1], hb)
    m.box(hair, [0, y0 - 6, h.head / 2 + 1.2], [h.head, 14, 1.0], "naiad_hair", decal={"north": tatter, "south": tatter})
    for i, (x, ln, rz) in enumerate(((-3.6, 3.4, 26), (-1.6, 4.6, 10), (0, 5.6, 0), (1.6, 4.6, -10), (3.6, 3.4, -26))):
        m.box(hb, [x, top + ln / 2, -1], [1.0, ln, 1.0], "coral" if i % 2 else "coral_b", rot=[-10, 0, rz], pivot=[x, top, -1])
        m.box(hb, [x * 1.3, top + ln, -1.4], [1.2, 1.2, 1.2], "pearl")
    ch = r["chest"]
    for i in range(5):
        m.box(ch, [-2.8 + i * 1.4, r["top_y"] - 1.6 - abs(i - 2) * 0.8, -h.depth / 2 - 0.5], [1.0, 1.0, 1.0], "pearl")   # ожерелье
    halo = m.bone("halo", [0, r["top_y"] + 3, 5], ch)
    for i in range(16):
        a = i * math.pi / 8
        m.box(halo, [math.cos(a) * 7, r["top_y"] + 3 + math.sin(a) * 7, 5], [2.9, 1.0, 0.5], "gold_sea",
              rot=[0, 0, math.degrees(a) + 90])
        if i % 4 == 0:
            m.box(halo, [math.cos(a) * 7, r["top_y"] + 3 + math.sin(a) * 7, 4.6], [1.6, 1.6, 0.6], "sea_glow")
    body = r["body"]
    for i, (w, d, hg) in enumerate(((h.hips + 1.6, h.depth + 1.4, 6), (h.hips + 3.4, h.depth + 3, 7), (h.hips + 6, h.depth + 5, 8))):
        m.box(body, [0, r["hip_y"] + 1 - i * 6 - hg / 2 + 2.6, 0.4], [w, hg, d], "scale_teal" if i < 2 else "scale_deep",
              decal={"north": tatter, "south": tatter, "east": tatter, "west": tatter} if i == 2 else None)
    fin = m.bone("fin_back", [0, r["hip_y"], 3], body)
    m.box(fin, [0, r["hip_y"] - 9, 4.4], [h.hips + 3, 18, 0.6], "scale_teal", decal={"north": tatter, "south": tatter})
    lh = r["left_hand"]
    x, y = r["left_x"], r["left_hand_y"] + 1
    st = m.bone("staff", [x, y, 0], lh, rot=[12, 0, 6])
    m.box(st, [x, y + 8, 0], [1.2, 38, 1.2], "coral_b")
    sy = y + 28
    for i in range(3):
        m.box(st, [x, sy + i * 1.6, 0], [4.4 - i * 1.2, 1.6, 4.4 - i * 1.2], "shell" if i % 2 == 0 else "shell_l", rot=[0, i * 30, 0])
    m.box(st, [x, sy + 5.2, 0], [1.8, 1.8, 1.8], "pearl")
    features(m, r, h, "naiad_skin", nose=0.8, brow=False, ears=1.2)
    motions(m, r, Persona(weight=0.25, caster=True, left=True, float_=True),
            extra={"hair": cloth_sway(6, 0.2), "fin_back": cloth_sway(6, 0.4, 3),
                   "halo": {"idle": lambda T: {"rotation": kf([(0, [0, 0, 0]), (T, [0, 0, 360])], smooth=False)},
                            "cast": lambda C: {"rotation": kf([(0, [0, 0, 0]), (C, [0, 0, 720])], smooth=False)}}})
    return m


# ---------------------------------------------------------------------------- монстры храма

def drowned_guard():
    """Утопленник-страж: раздутый мертвец в ржавом призмариновом доспехе, водоросли, ракушки на плечах, тесак."""
    m = Model("drowned_guard")
    h = H(height=38, head=8, shoulders=11.6, hips=8.4, depth=6.4, leg=13, arm=14, limb=3.4, hand=3.6, belly=1.0)
    r = humanoid(m, h, {"skin": "drown", "torso": "drown_rot", "arm": "drown", "hand": "drown", "leg": "drown_rot",
                        "boot": "pris_dark", "head": "drown", "face": drowned_face, "pelvis": "kelp"}, arm_out=8)
    ch = r["chest"]
    m.box(ch, [0, r["chest_y"] + 4.6, 0], [h.shoulders + 0.6, 6, h.depth + 1.8], "pris_plate", decal={"north": plate_rows})
    for sx in (-1, 1):
        m.box(ch, [sx * 5.2, r["top_y"] + 0.6, 0], [2.6, 1.6, 2.6], "shell_l")
        m.box(ch, [sx * 3.6, r["top_y"] + 0.4, 1.6], [1.8, 1.2, 1.8], "shell")
    kb = m.bone("kelp_hang", [2, r["top_y"], -3.8], ch)
    m.box(kb, [2, r["top_y"] - 5, -4], [1.4, 10, 0.4], "kelp", decal={"north": tatter})
    rh = r["right_hand"]
    x, y = r["right_x"], r["right_hand_y"]
    bl = m.bone("blade", [x, y + 1, 0], rh, rot=[70, 0, 0])
    m.box(bl, [x, y + 1.6, 0], [1.2, 3, 1.2], "rope")
    m.box(bl, [x, y + 8, 0], [1.0, 10, 3.6], "pris_plate")
    features(m, r, h, "drown", nose=1.1, chin=0.6, ears=0.8)
    m.box(r["head"], [1.4, r["head_top"] + 0.4, 0.6], [5, 1.2, 6], "kelp")
    m.box(r["head"], [-2.6, r["head_y"] + 4, -r["face_z"] - 0.6], [2, 1.4, 1.4], "shell_l")
    motions(m, r, Persona(weight=0.7, hunch=8), extra={"kelp_hang": cloth_sway(8, 0.2)})
    return m


def deep_priest():
    """Жрец глубин: лекарь культа — ряса с жемчугом, капюшон-раковина, фонарь с морским светом на цепи."""
    m = Model("deep_priest")
    h = H(height=38, head=8, shoulders=9.4, hips=7.4, depth=5, leg=13, arm=13, limb=2.6, hand=2.8)
    r = humanoid(m, h, {"skin": "drown", "torso": "priest_robe", "arm": "priest_robe", "hand": "drown", "leg": "priest_robe",
                        "boot": "priest_robe", "head": "drown", "face": drowned_face}, arm_out=7)
    hb, top = r["head"], r["head_top"]
    for i in range(4):
        m.box(hb, [0, top - 1 + i * 1.2, 0.6 + i * 0.6], [h.head + 1.6 - i * 1.6, 1.4, h.head + 1.4 - i * 1.2], "shell" if i % 2 == 0 else "shell_l")
    m.mirror(hb, [h.head / 2 + 0.05, r["head_y"] - 0.4, -h.head / 2 - 0.2], [1.0, top - r["head_y"], h.head + 0.6], "shell")
    body = r["body"]
    m.box(body, [0, r["hip_y"] - 4, 0.3], [h.hips + 3, 9, h.depth + 2.4], "priest_robe", decal={"north": tatter, "south": tatter, "east": tatter, "west": tatter})
    ch = r["chest"]
    for i in range(5):
        m.box(ch, [-2.4 + i * 1.2, r["top_y"] - 1.2 - abs(i - 2) * 0.7, -h.depth / 2 - 0.5], [0.9, 0.9, 0.9], "pearl")
    rh = r["right_hand"]
    x, y = r["right_x"], r["right_hand_y"]
    lt = m.bone("lantern", [x, y, 0], rh)
    m.box(lt, [x, y - 1.6, 0], [0.5, 3, 0.5], "chain")
    m.box(lt, [x, y - 4.6, 0], [3, 3.4, 3], "gold_sea")
    m.box(lt, [x, y - 4.6, 0], [2.2, 2.6, 3.2], "sea_glow")
    features(m, r, h, "drown", nose=1.0, chin=0.3)
    motions(m, r, Persona(weight=0.3, caster=True, hunch=6), extra={"lantern": {"idle": lambda T: {"rotation": sway(T, 8, 0, 0)},
                                                                              "walk": lambda L: {"rotation": sway(L, 18, 0.3, 0)}}})
    return m


def moray():
    """Мурена: длинная угорь с пятнистой кожей, пасть с иглами, плавник вдоль спины."""
    m = Model("moray")
    hd = m.bone("head", [0, 3, -3])
    m.box(hd, [0, 3, -6], [3.4, 3.2, 6], "eel")
    m.box(hd, [0, 3.4, -9.4], [2.6, 2.2, 1.6], "eel")
    for sx in (-1, 1):
        m.box(hd, [1.5 * sx, 4.2, -7.6], [0.6, 0.8, 0.8], "eye_gold")
    jaw = m.bone("jaw", [0, 2, -4], hd)
    m.box(jaw, [0, 1.6, -6.6], [3.0, 1.0, 6.4], "belly")
    for i in range(4):
        m.box(jaw, [0, 2.4, -9 + i * 1.4], [2.4, 0.6, 0.4], "bone")
    parent, z0 = "head", -3
    for i in range(5):
        w = 3.4 * (1 - i * 0.1)
        seg = m.bone(f"seg{i}", [0, 3, z0], parent)
        m.box(seg, [0, 3, z0 + 2], [w, w * 0.95, 4.2], "eel")
        m.box(seg, [0, 3 + w * 0.6, z0 + 2], [0.4, 1.4, 4], "scale_teal")
        parent, z0 = seg, z0 + 4
    wave = {f"seg{i}": {"rotation": sway(0.8, 16 + i * 4, i * 0.14, 1)} for i in range(5)}
    wave["head"] = {"rotation": sway(0.8, 10, -0.1, 1)}
    m.anim("walk", loop(0.8, wave))
    m.anim("idle", loop(2.0, {f"seg{i}": {"rotation": sway(2.0, 5 + i * 2, i * 0.15, 1)} for i in range(5)} | {"jaw": {"rotation": sway(2, 8, 0, 0, [8, 0, 0])}}))
    z = [0, 0, 0]
    m.anim("attack", once(0.45, {"head": {"rotation": beats(0.45, [(0, z), (0.4, [-30, 0, 0]), (0.6, [20, 0, 0]), (1, z)]),
                                          "position": beats(0.45, [(0, z), (0.6, [0, 0, -3]), (1, z)])},
                                 "jaw": {"rotation": beats(0.45, [(0, z), (0.4, [45, 0, 0]), (0.6, [0, 0, 0]), (1, z)])}}))
    m.anim("death", once(1.0, {"root": {"rotation": beats(1.0, [(0, z), (1, [0, 0, 170])])}}, hold=True))
    for nm, ln in (("shoot", 0.6), ("cast", 0.8), ("block", 0.5), ("leap", 0.6), ("hurt", 0.3)):
        m.anim(nm, once(ln, {"head": {"rotation": beats(ln, [(0, z), (0.4, [-25, 0, 0]), (1, z)])}}))
    return m


def crab():
    """Крабоид-щит: широкий панцирь с наростами кораллов, одна клешня огромная (щит), глаза на стебельках."""
    m = Model("crab")
    body = m.bone("body", [0, 5, 0])
    m.box(body, [0, 6, 0], [13, 4.4, 10], "shell", decal={"up": rivets})
    m.box(body, [0, 8.6, 0], [9, 1.6, 7], "shell_l")
    for x, z in ((-3, -1), (2, 2), (4, -2)):
        m.box(body, [x, 9.8, z], [1, 2, 1], "coral")
    hd = m.bone("head", [0, 7, -5], body)
    for sx in (-1, 1):
        m.box(hd, [1.6 * sx, 9.6, -5.2], [0.6, 3, 0.6], "shell_l")
        m.box(hd, [1.6 * sx, 11.4, -5.2], [1.2, 1.2, 1.2], "eye_red")
    m.box(hd, [0, 6, -5.4], [4, 1.6, 1.2], "shell_l")
    for side, sx, big in (("r", -1, True), ("l", 1, False)):
        cl = m.bone(f"claw_{side}", [5.6 * sx, 6, -4], body)
        s = 1.6 if big else 1.0
        m.box(cl, [7.2 * sx, 6, -5.4], [2.4 * s, 2.4 * s, 3.4 * s], "shell")
        m.box(cl, [7.6 * sx, 6.4 + 0.6 * s, -6.4 - 2.6 * s], [3.4 * s, 2.0 * s, 4.6 * s], "shell_l")
        pin = m.bone(f"pincer_{side}", [7.6 * sx, 5.8, -5.6 - 1.4 * s], cl)
        m.box(pin, [7.6 * sx, 5.6 - 0.4 * s, -6.6 - 2.4 * s], [3.0 * s, 1.4 * s, 4.2 * s], "shell")
        for i in range(3):
            lg = m.bone(f"leg_{side}{i}", [6 * sx, 5, -1 + i * 2.4], body, rot=[0, 0, 40 * sx])
            m.cube(lg, [6 * sx - (5 if sx < 0 else 0), 4.4, -1.4 + i * 2.4], [5, 0.9, 0.9], "shell_l")
    z = [0, 0, 0]
    walk = {f"leg_{s}{i}": {"rotation": sway(0.5, 18, i * 0.33 + (0.5 if s == "l" else 0), 1)} for s in "rl" for i in range(3)}
    walk["body"] = {"position": sway(0.25, 0.3, 0, 1)}
    m.anim("walk", loop(0.5, walk))
    m.anim("idle", loop(2.0, {"pincer_r": {"rotation": sway(1, 12, 0, 0)}, "pincer_l": {"rotation": sway(1, 10, 0.4, 0)},
                              "head": {"rotation": sway(2, 6, 0, 1)}, "body": {"position": sway(2, 0.2, 0, 1)}}))
    m.anim("attack", once(0.55, {"claw_r": {"rotation": beats(0.55, [(0, z), (0.4, [-40, 0, 0]), (0.6, [20, 0, 0]), (1, z)])},
                                 "pincer_r": {"rotation": beats(0.55, [(0, z), (0.4, [30, 0, 0]), (0.6, [0, 0, 0]), (1, z)])}}))
    m.anim("block", once(0.6, {"claw_r": {"rotation": beats(0.6, [(0, z), (0.2, [-30, 30, 0]), (0.8, [-30, 30, 0]), (1, z)])},
                               "body": {"position": beats(0.6, [(0, z), (0.2, [0, -1, 0]), (0.8, [0, -1, 0]), (1, z)])}}))
    m.anim("death", once(1.0, {"body": {"rotation": beats(1.0, [(0, z), (1, [0, 0, 180])]), "position": beats(1, [(0, z), (1, [0, 4, 0])])}}, hold=True))
    for nm, ln in (("shoot", 0.6), ("cast", 0.8), ("leap", 0.6), ("hurt", 0.3)):
        m.anim(nm, once(ln, {"body": {"rotation": beats(ln, [(0, z), (0.4, [-12, 0, 0]), (1, z)])}}))
    return m


def jelly():
    """Медуза: светящийся купол с прожилками, бахрома, длинные щупальца волнами."""
    m = Model("jelly")
    bell = m.bone("bell", [0, 14, 0])
    m.box(bell, [0, 14, 0], [10, 5, 10], "jelly_body")
    m.box(bell, [0, 16.6, 0], [7, 2, 7], "jelly_body")
    m.box(bell, [0, 13.4, 0], [4, 3, 4], "jelly")
    m.box(bell, [0, 11.4, 0], [10.6, 0.8, 10.6], "jelly_body", decal={f: tatter for f in ("north", "south", "east", "west")})
    for i in range(6):
        a = i * math.pi / 3
        x, zz = math.cos(a) * 3.4, math.sin(a) * 3.4
        m.segments(f"t{i}_", bell, [x, 11.4, zz], [(3, 0.6, [0, 0, 0]), (3, 0.5, [0, 0, 0]), (2.6, 0.4, [0, 0, 0])], "jelly", up=False)
    idle = {"bell": {"scale": kf([(0, [1, 1, 1]), (0.6, [1.12, 0.86, 1.12]), (1.4, [0.94, 1.08, 0.94]), (2.0, [1, 1, 1])]),
                     "position": sway(2.0, 1.2, 0, 1)}}
    for i in range(6):
        for j in range(3):
            idle[f"t{i}_{j}"] = {"rotation": sway(2.0, 10 + j * 6, i * 0.16 + j * 0.15, 0)}
    m.anim("idle", loop(2.0, idle))
    m.anim("walk", loop(2.0, idle))
    z = [0, 0, 0]
    m.anim("attack", once(0.6, {"bell": {"scale": beats(0.6, [(0, [1, 1, 1]), (0.4, [1.25, 0.8, 1.25]), (1, [1, 1, 1])])}}))
    m.anim("death", once(1.2, {"bell": {"scale": beats(1.2, [(0, [1, 1, 1]), (1, [0.1, 0.1, 0.1])])}}, hold=True))
    for nm, ln in (("shoot", 0.6), ("cast", 0.8), ("block", 0.5), ("leap", 0.6), ("hurt", 0.3)):
        m.anim(nm, once(ln, {"bell": {"scale": beats(ln, [(0, [1, 1, 1]), (0.4, [1.15, 0.9, 1.15]), (1, [1, 1, 1])])}}))
    return m


def harpooner():
    """Гарпунщик: рыбак-утопленник в просмолённой куртке и шляпе, моток верёвки на плече, гарпун с зазубринами."""
    m = Model("harpooner")
    h = H(height=36, head=8, shoulders=10.4, hips=8, depth=5.6, leg=12.4, arm=13, limb=3.0, hand=3.2)
    r = humanoid(m, h, {"skin": "drown", "torso": "leather_dark", "arm": "leather_dark", "hand": "drown", "leg": "priest_robe",
                        "boot": "leather_dark", "head": "drown", "face": drowned_face}, arm_out=7)
    hb, top = r["head"], r["head_top"]
    m.box(hb, [0, top + 0.4, 0], [h.head + 4, 0.8, h.head + 4], "leather")
    m.box(hb, [0, top + 1.8, 0], [h.head - 0.6, 2.4, h.head - 0.6], "leather")
    ch = r["chest"]
    for i in range(4):
        m.box(ch, [3.2, r["chest_y"] + 2 + i * 1.6, -1 + i * 0.8], [5.4, 1.2, 1.2], "rope", rot=[0, 0, -40], pivot=[3.2, r["chest_y"] + 4, 0])
    rh = r["right_hand"]
    x, y = r["right_x"], r["right_hand_y"] + 1
    hp = m.bone("harpoon", [x, y, 0], rh, rot=[80, 0, 0])
    m.box(hp, [x, y + 4, 0], [1.1, 26, 1.1], "wood")
    m.box(hp, [x, y + 17.6, 0], [1.6, 3, 1.6], "iron")
    m.box(hp, [x, y + 20, 0], [0.8, 2, 0.8], "steel")
    for a in (-1, 1):
        m.box(hp, [x + 0.8 * a, y + 17, 0], [0.5, 1.6, 0.5], "steel", rot=[0, 0, 30 * a])
    features(m, r, h, "drown", nose=1.2, chin=0.8, ears=0.7)
    m.box(r["head"], [0, r["head_y"] + 0.6, r["face_z"] - 0.3], [5.4, 2.2, 0.8], "leather")
    motions(m, r, Persona(weight=0.45, hunch=6))
    return m


MODELS = {"tide_warden": tide_warden, "leviathan": leviathan, "naiad": naiad, "drowned_guard": drowned_guard,
          "deep_priest": deep_priest, "moray": moray, "crab": crab, "jelly": jelly, "harpooner": harpooner}
