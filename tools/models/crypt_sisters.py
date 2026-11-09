"""Этап 1 · Сёстры Морра и Весса, Король-лич Карн, ледяная могила и филактерия."""
import hy
from hy import Model, mat, kf, loop, once, sway, beats, mul, rgb
from rig import H, humanoid, motions, Persona, cloth_sway, features
from crypt import rivets, tatter, skull

# ---------------------------------------------------------------------------- материалы
mat("pale", "#E6D2C4", "skin", dark="#A88E80", light="#FFF0E6")
mat("pale_green", "#C8D8C0", "skin", dark="#7E9478", light="#EAF6E2")
mat("m_dress", "#26202A", "cloth", dark="#120E14", light="#463A4C")
mat("m_red", "#8A1E26", "cloth", dark="#480C12", light="#C23A44")
mat("m_mask", "#EDE4CC", "bone", dark="#A09276", light="#FFFAEA")
mat("m_glove", "#2E2228", "leather", dark="#160F12", light="#4E3A42")
mat("m_steel", "#A8B2BE", "metal", dark="#5A6470", light="#F0F6FC", gloss=0.8)
mat("m_blood", "#FF4A50", "glow", glow=True, light="#FFD0C8", dark="#B01A24")
mat("v_dress", "#3E2A5A", "cloth", dark="#1E1230", light="#6A4C92")
mat("v_green", "#2E4A30", "cloth", dark="#16261A", light="#4E7A50")
mat("v_hair", "#1E1A22", "hair", dark="#0C0A0E", light="#4A4258")
mat("v_wood", "#4A3826", "bark", dark="#241A10", light="#7A6040")
mat("v_flame", "#7CFF8A", "glow", glow=True, light="#F0FFE8", dark="#20B040")
mat("v_book", "#4A2A1E", "leather", dark="#24140E", light="#7A4A34")
mat("v_page", "#E8DCB8", "plain", dark="#B8A880", light="#FFF6DC")
mat("v_wax", "#E4DCC8", "plain", dark="#B4A890", light="#FFFAEE")
mat("gold_old", "#C8963A", "metal", dark="#7A5418", light="#F6D27A", gloss=0.6)
mat("l_robe", "#1E2C58", "cloth", dark="#0E1430", light="#3A5090")
mat("l_robe_purple", "#3E2460", "cloth", dark="#1E1032", light="#6A44A0")
mat("l_fur", "#D8DCE4", "fur", dark="#8A90A0", light="#FFFFFF")
mat("l_bone", "#DCD4C0", "bone", dark="#8A8270", light="#FAF4E6")
mat("l_ice", "#9CDCF4", "crystal", glow="pattern", dark="#4A90C0", light="#E8FAFF")
mat("l_ice_glow", "#BFF4FF", "glow", glow=True, light="#FFFFFF", dark="#5AC8F0")
mat("l_gold", "#E0B040", "metal", dark="#8A6418", light="#FFF0A0", gloss=0.8)
mat("l_gem", "#7FE8FF", "crystal", glow=True, dark="#2A9AD0", light="#FFFFFF")


# ---------------------------------------------------------------------------- декали лиц

def morra_mask(f):
    """Костяная маска-череп: вытянутые глазницы с красным огнём, трещина, нарисованные зубы."""
    w, h = f.w, f.h
    for side, sx in ((0, int(w * 0.14)), (1, w - int(w * 0.14) - int(w * 0.28))):
        ew, eh = int(w * 0.28), int(h * 0.36)
        ey = int(h * 0.24)
        for y in range(eh):
            cut = int((eh - y) * 0.45) if y < eh // 2 else max(0, y - eh + 3)
            x0 = sx + (cut if side == 1 else 0)
            x1 = sx + ew - (cut if side == 0 else 0)
            f.rect(x0, ey + y, x1, ey + y + 1, "#100A0C")
        cx, cy = sx + ew // 2, ey + int(eh * 0.6)
        f.rect(cx - 1, cy, cx + 1, cy + 2, "#FF4A50", glow=True)
        f.set(cx, cy, "#FFE0D8", glow=True)
    f.line(int(w * 0.62), 0, int(w * 0.52), int(h * 0.5), "#8A7E64")       # трещина
    for x in range(int(w * 0.22), int(w * 0.80), 2):
        f.rect(x, int(h * 0.76), x + 1, h - 1, "#3A2C2A")                   # зубы
    f.shade(0, int(h * 0.7), w, h, 0.85)


def vessa_face(f):
    """Бледное лицо, зелёные светящиеся глаза, тёмные губы, тень под капюшоном сверху."""
    w, h = f.w, f.h
    f.shade(0, 0, w, int(h * 0.3), 0.55)
    ey = int(h * 0.42)
    for sx in (int(w * 0.22), int(w * 0.62)):
        f.rect(sx - 1, ey - 1, sx + 4, ey, "#2A2230")                     # брови вниз к переносице
        f.rect(sx, ey + 1, sx + 3, ey + 3, "#1A1418")
        f.rect(sx + 1, ey + 1, sx + 3, ey + 3, "#7CFF8A", glow=True)
        f.set(sx + 1, ey + 1, "#F0FFE8", glow=True)
    f.rect(int(w * 0.40), int(h * 0.74), int(w * 0.60), int(h * 0.74) + 1, "#4A1E30")
    f.rect(int(w * 0.43), int(h * 0.74) + 1, int(w * 0.57), int(h * 0.74) + 2, "#2E1220")
    f.shade(int(w * 0.46), int(h * 0.55), int(w * 0.54), int(h * 0.68), 0.88)   # нос


def lich_face(f):
    """Череп лича: синий огонь в глазницах, иней, корона бросает тень."""
    w, h = f.w, f.h
    f.shade(0, 0, w, 2, 0.6)
    ey, eh, ew = int(h * 0.30), int(h * 0.30), int(w * 0.28)
    for side, sx in ((0, int(w * 0.14)), (1, w - int(w * 0.14) - ew)):
        for y in range(eh):
            cut = int((eh * 0.5 - y) * 0.6) if y < eh * 0.5 else 0
            x0 = sx + (cut if side == 1 else 0)
            x1 = sx + ew - (cut if side == 0 else 0)
            f.rect(x0, ey + y, x1, ey + y + 1, "#06080E")
        cx, cy = sx + ew // 2, ey + int(eh * 0.55)
        f.rect(cx - 1, cy - 1, cx + 2, cy + 2, "#5AD0FF", glow=True)
        f.set(cx, cy, "#FFFFFF", glow=True)
        f.set(cx, cy - 2, "#BFF4FF", glow=True)
        f.set(cx, cy - 3, "#5AC8F0", glow=True)
    nx = w // 2
    for i in range(3):
        f.rect(nx - 1 - (1 if i < 2 else 0), int(h * 0.64) + i, nx + 1 + (1 if i < 2 else 0), int(h * 0.64) + i + 1, "#100C10")
    for x in range(2, w - 2, 2):
        f.rect(x, int(h * 0.82), x + 1, h - 1, "#F6F0E2")
        f.set(x + 1, int(h * 0.82), "#2A2228")
    for i in range(6):
        f.set(int(hy.h32(i, 5) * w), int(hy.h32(i, 9) * h * 0.3), "#DFF6FF")   # иней


def dress_hem(f):
    """Подол: рваный край + алые руны у края."""
    tatter(f)
    for x in range(2, f.w - 2, 5):
        f.set(x, f.h - 8, "#C23A44")
        f.set(x + 1, f.h - 9, "#C23A44")


def hem_green(f):
    tatter(f)
    for x in range(1, f.w - 1, 4):
        f.set(x, f.h - 7, "#7CFF8A", glow=True)


def gold_trim(f):
    f.rect(0, 0, f.w, 1, "#E8C060")
    f.rect(0, f.h - 1, f.w, f.h, "#8A6418")


def book_cover(f):
    f.rect(1, 1, f.w - 1, f.h - 1, "#5A3424")
    for cx, cy in ((1, 1), (f.w - 3, 1), (1, f.h - 3), (f.w - 3, f.h - 3)):
        f.rect(cx, cy, cx + 2, cy + 2, "#E0B040")
    f.rect(f.w // 2 - 1, f.h // 2 - 2, f.w // 2 + 1, f.h // 2 + 2, "#7CFF8A", glow=True)


def robe_ribs(f):
    """Разорванная мантия на груди: видны рёбра."""
    w, h = f.w, f.h
    f.rect(int(w * 0.3), 0, int(w * 0.7), h, "#0E0A12")
    for y in range(2, h - 1, 3):
        f.rect(int(w * 0.32), y, int(w * 0.68), y + 1, "#DCD4C0")
    f.rect(int(w * 0.48), 0, int(w * 0.52), h, "#C8C0AC")


# ---------------------------------------------------------------------------- общие части сестёр

def hood(m, r, cloth, trim, peak, h):
    """Капюшон: свод нависает над лбом, толстые бока выступают вперёд — лицо утоплено в тень;
    тонкая подкладка видна только по нижнему краю; peak — острый конец назад/вверх."""
    hb = r["head"]
    y0, top = r["head_y"], r["head_top"]
    s = h.head + 1.8
    m.box(hb, [0, top + 0.8, 0.0], [s, 1.8, s + 0.6], cloth)                                  # свод
    m.box(hb, [0, top - 0.3, -s / 2 - 0.1], [s, 1.6, 1.4], cloth)                             # козырёк над лбом
    m.mirror(hb, [h.head / 2 + 0.05, y0 - 1.0, -s / 2 - 0.4], [1.3, top - y0 + 1.6, s + 0.2], cloth)   # бока
    m.box(hb, [0, (y0 + top) / 2 + 0.2, s / 2], [s, top - y0 + 1.8, 1.2], cloth)              # спинка
    m.mirror(hb, [h.head / 2 + 0.05, y0 - 1.0, -s / 2 - 0.5], [1.3, 0.8, 1.2], trim)          # подкладка у края
    m.box(hb, [0, y0 - 0.6, 1.0], [s + 0.6, 1.6, s - 1.0], cloth)                             # складки у шеи
    if peak:
        m.segments("hood_tip", hb, [0, top + 1.4, 1.8], peak, cloth)


def skirt(m, r, mat_, hem, tiers, front=True):
    """Юбка ярусами (расширяется книзу) + передняя и задняя полы на своих костях (качаются)."""
    body = r["body"]
    y = r["hip_y"] + 1.5
    for i, (w, d, hgt) in enumerate(tiers):
        m.box(body, [0, y - hgt / 2, 0.3], [w, hgt, d], mat_, decal={"north": hem, "south": hem, "east": hem, "west": hem}
              if i == len(tiers) - 1 else None)
        y -= hgt * 0.8
    if front:
        fb = m.bone("skirt_front", [0, r["hip_y"], -tiers[-1][1] / 2], body)
        m.box(fb, [0, r["hip_y"] - 6, -tiers[-1][1] / 2 - 0.3], [tiers[-1][0] * 0.5, 12, 0.5], mat_,
              decal={"north": hem, "south": hem})
    bb = m.bone("skirt_back", [0, r["hip_y"] + 1, tiers[-1][1] / 2], body)
    m.box(bb, [0, r["hip_y"] - 6, tiers[-1][1] / 2 + 0.3], [tiers[-1][0] * 0.8, 13, 0.5], mat_,
          decal={"north": hem, "south": hem})


def bell_sleeves(m, r, mat_, hem, h):
    for side in ("right", "left"):
        x = r[f"{side}_x"]
        y = r[f"{side}_hand_y"] + h.hand + 1.2
        m.box(r[f"{side}_forearm"], [x, y + 1.6, 0], [h.limb + 2.2, 4.2, h.limb + 2.2], mat_,
              decal={"north": hem, "south": hem, "east": hem, "west": hem})


# ---------------------------------------------------------------------------- Морра

def morra():
    """Морра, старшая: бьёт косой. Чёрное платье с алым, костяная маска-череп, капюшон с острым концом,
    костяные наплечники, коса с лезвием-полумесяцем и алой кромкой."""
    m = Model("morra")
    h = H(height=40, head=8.6, shoulders=9.6, hips=7.6, depth=5, leg=14, arm=13.5, limb=2.8, hand=3.2)
    r = humanoid(m, h, {"skin": "pale", "torso": "m_dress", "arm": "m_red", "forearm": "m_glove", "hand": "m_mask",
                        "leg": "m_dress", "boot": "m_glove", "head": "pale", "pelvis": "m_red"}, arm_out=9)
    hb = r["head"]
    fz = r["face_z"]
    m.box(hb, [0, r["head_y"] + h.head * 0.56, fz - 0.45], [h.head - 2.0, h.head * 0.66, 0.8], "m_mask",
          decal={"north": morra_mask})
    m.box(hb, [0, r["head_y"] + h.head * 0.17, fz - 0.35], [h.head - 3.6, h.head * 0.2, 0.7], "m_mask")   # подбородок маски
    m.mirror(hb, [1.2, r["head_y"] + h.head * 0.9, fz - 0.6], [2.4, 0.8, 1.0], "m_mask")   # рожки маски
    hood(m, r, "m_dress", "m_red", [(2.6, 3.0, [-38, 0, 0]), (2.2, 2.0, [-30, 0, 0]), (1.8, 1.0, [-24, 0, 0])], h)
    ch = r["chest"]
    top = r["top_y"]
    m.box(ch, [0, top - 0.2, 0], [h.shoulders + 1.2, 2.2, h.depth + 2], "m_red")             # воротник
    for i, x in enumerate((-4.4, -2.2, 0, 2.2, 4.4)):
        m.box(ch, [x, top + 1.6 + (1 - abs(i - 2) * 0.4), 1.8], [0.9, 2.8 - abs(i - 2) * 0.5, 0.9], "m_red", rot=[-16, 0, x * 3])
    m.box(ch, [0, r["chest_y"] + 2.2, -h.depth / 2 - 0.35], [h.shoulders - 3, 5, 0.6], "m_red",
          decal={"north": gold_trim})                                                         # корсет
    m.box(r["body"], [0, r["hip_y"] + 3.6, 0], [h.hips + 2, 1.2, h.depth + 1.4], "gold_old", decal={"north": rivets})
    for side, sx in (("right", -1), ("left", 1)):
        arm = r[f"{side}_arm"]
        x = r[f"{side}_x"]
        for i in range(3):
            m.box(arm, [x + 0.4 * sx, r["shoulder_y"] + 1.4 - i * 1.3, 0], [h.limb + 2.4 - i * 0.6, 1.0, h.limb + 2.6 - i * 0.6],
                  "m_mask", rot=[0, 0, -22 * sx])                                              # костяной наплечник
    m.box(r["left_arm"], [r["left_x"] + 1.4, r["shoulder_y"] + 2.6, 0], [2.6, 2.6, 2.6], "m_mask",
          decal={"north": lambda f: (f.rect(0, 1, 1, 2, "#100A0C"), f.rect(f.w - 1, 1, f.w, 2, "#100A0C"))})
    bell_sleeves(m, r, "m_dress", dress_hem, h)
    for side in ("right", "left"):
        for i in range(2):
            m.box(r[f"{side}_forearm"], [r[f"{side}_x"], r[f"{side}_hand_y"] + h.hand + 4.6 + i * 1.6, 0],
                  [h.limb + 0.6, 0.7, h.limb + 0.6], "m_mask")                                 # костяные наручи
    skirt(m, r, "m_dress", dress_hem, [(h.hips + 1.4, h.depth + 1.6, 4), (h.hips + 3.6, h.depth + 3.4, 5), (h.hips + 6, h.depth + 5.4, 6)])
    # коса в правой руке
    rh = r["right_hand"]
    x, y = r["right_x"], r["right_hand_y"] + 1.6
    sc = m.bone("scythe", [x, y, 0], rh, rot=[14, 0, -8])
    m.box(sc, [x, y + 4, 0], [1.1, 34, 1.1], "wood_dark")
    for k in (-8, 6, 15):
        m.box(sc, [x, y + k, 0], [1.5, 0.8, 1.5], "gold_old")
    blade_root = [x, y + 20.5, 0]
    m.box(sc, blade_root, [2.4, 2.2, 2.4], "m_red")
    m.segments("blade", sc, [x, y + 20.6, -0.8],
               [(6.0, 2.2, [-90, 0, 0]), (5.2, 2.0, [-22, 0, 0]), (4.4, 1.7, [-24, 0, 0]), (3.6, 1.3, [-26, 0, 0]),
                (2.6, 0.9, [-30, 0, 0])], ["m_steel", "m_steel", "m_steel", "m_steel", "m_blood"])
    m.box(sc, [x, y + 19.6, -1.8], [0.5, 3.4, 0.5], "m_blood")
    m.box(sc, [x, y - 11.6, 0], [1.6, 1.6, 1.6], "m_mask")                                    # набалдашник
    motions(m, r, Persona(weight=0.35, hunch=4, menace=1.2),
            extra={"skirt_front": cloth_sway(5, 0.1), "skirt_back": cloth_sway(6, 0.35, 4), "hood_tip0": cloth_sway(5, 0.2)})
    z = [0, 0, 0]
    m.anim("attack", once(0.85, {
        "right_arm": {"rotation": beats(0.85, [(0, z), (0.4, [-130, 40, 50]), (0.58, [-40, -50, -20]), (0.8, [-20, -30, 0]), (1, z)])},
        "right_forearm": {"rotation": beats(0.85, [(0, z), (0.4, [-30, 0, 0]), (0.58, [-10, 0, 0]), (1, z)])},
        "chest": {"rotation": beats(0.85, [(0, z), (0.4, [-6, 45, 0]), (0.58, [12, -40, 0]), (1, z)])},
        "body": {"rotation": beats(0.85, [(0, z), (0.4, [0, 15, 0]), (0.58, [0, -20, 0]), (1, z)])},
        "scythe": {"rotation": beats(0.85, [(0, z), (0.4, [20, 0, 0]), (0.58, [-30, 0, 0]), (1, z)])},
        "skirt_back": {"rotation": beats(0.85, [(0, [4, 0, 0]), (0.6, [24, 0, 0]), (1, [4, 0, 0])])},
    }))
    return m


# ---------------------------------------------------------------------------- Весса

def vessa():
    """Весса, младшая: лечит сестру и проклинает. Фиолетово-зелёное платье, остроконечный капюшон с заплатами,
    чёрные волосы, зелёный огонь в фонаре на посохе (левая рука), гримуар на цепи, свечи на плече."""
    m = Model("vessa")
    h = H(height=39, head=8.6, shoulders=9.2, hips=7.4, depth=5, leg=14, arm=13, limb=2.7, hand=3.1)
    r = humanoid(m, h, {"skin": "pale_green", "torso": "v_dress", "arm": "v_dress", "forearm": "v_dress", "hand": "pale_green",
                        "leg": "v_dress", "boot": "m_glove", "head": "pale_green", "face": vessa_face, "arm": "v_green"}, arm_out=8)
    hb = r["head"]
    hood(m, r, "v_green", "gold_old", [(3.4, 4.0, [-18, 0, 0]), (3.2, 3.0, [-24, 0, 0]), (2.8, 2.0, [-30, 0, 6]),
                                       (2.2, 1.2, [-36, 0, 10])], h)
    m.box(hb, [-2.6, r["head_top"] + 0.8, -1.4], [2.4, 0.4, 2.4], "v_dress", inflate=0.1)      # заплата
    # волосы: две пряди спереди и копна сзади
    for sx in (-1, 1):
        m.segments(f"hair_{'r' if sx < 0 else 'l'}", hb, [3.2 * sx, r["head_y"] + 1.5, -3.2],
                   [(3.0, 1.6, [6, 0, 4 * sx]), (3.0, 1.4, [8, 0, 0]), (2.6, 1.1, [10, 0, -4 * sx])], "v_hair", up=False)
    m.box(hb, [0, r["head_y"] + 1.6, 3.4], [h.head - 0.6, 6, 2.4], "v_hair")
    ch = r["chest"]
    m.box(ch, [0, r["chest_y"] + 2.4, -h.depth / 2 - 0.3], [h.shoulders - 3.2, 4.6, 0.5], "v_green", decal={"north": gold_trim})
    m.box(r["body"], [0, r["hip_y"] + 3.6, 0], [h.hips + 2, 1.0, h.depth + 1.4], "leather_dark", decal={"north": rivets})
    bell_sleeves(m, r, "v_green", hem_green, h)
    skirt(m, r, "v_dress", hem_green, [(h.hips + 1.4, h.depth + 1.6, 4), (h.hips + 3.4, h.depth + 3.2, 5), (h.hips + 5.6, h.depth + 5, 6)])
    # гримуар на цепи у правого бедра
    bk = m.bone("book", [-4.8, r["hip_y"] + 3, -0.5], r["body"])
    m.box(bk, [-5.6, r["hip_y"] + 1.2, -0.5], [1.0, 1.6, 0.6], "chain")
    m.box(bk, [-5.8, r["hip_y"] - 2.2, -0.5], [1.6, 5, 4], "v_book", decal={"west": book_cover, "east": book_cover})
    m.box(bk, [-5.8, r["hip_y"] - 2.2, -0.5], [1.2, 4.6, 3.7], "v_page", inflate=0.05)
    # свечи на левом плече
    la = r["left_arm"]
    for i, (dx, hgt) in enumerate(((0.6, 2.6), (-0.6, 1.8), (1.4, 1.4))):
        x = r["left_x"] + dx
        y = r["shoulder_y"] + 1.6
        m.box(la, [x, y + hgt / 2, -0.6 + i * 0.7], [0.8, hgt, 0.8], "v_wax")
        m.box(la, [x, y + hgt + 0.5, -0.6 + i * 0.7], [0.5, 0.9, 0.5], "v_flame")
    # посох с фонарём в левой руке
    lh = r["left_hand"]
    x, y = r["left_x"], r["left_hand_y"] + 1.6
    st = m.bone("staff", [x, y, 0], lh, rot=[12, 0, 6])
    m.segments("staff_w", st, [x, y - 12, 0], [(9, 1.3, [0, 0, 3]), (9, 1.2, [0, 0, -5]), (8, 1.1, [0, 0, 4]),
                                                (5, 1.0, [0, 0, -6])], "v_wood")
    top = "staff_w3"
    ty = y - 12 + 31
    m.box(top, [x, ty + 0.4, 0], [2.6, 0.8, 2.6], "gold_old")
    lt = m.bone("lantern", [x, ty, 0], top)
    for dx, dz in ((-1.2, -1.2), (1.2, -1.2), (-1.2, 1.2), (1.2, 1.2)):
        m.box(lt, [x + dx, ty - 2.4, dz], [0.5, 3.6, 0.5], "gold_old")
    m.box(lt, [x, ty - 0.6, 0], [3.2, 0.6, 3.2], "gold_old")
    m.box(lt, [x, ty - 4.3, 0], [3.2, 0.6, 3.2], "gold_old")
    fl = m.bone("flame", [x, ty - 2.5, 0], lt)
    m.box(fl, [x, ty - 2.5, 0], [1.8, 2.8, 1.8], "v_flame")
    m.box(fl, [x, ty - 1.4, 0], [0.8, 1.6, 0.8], "v_flame")
    features(m, r, h, "pale_green", nose=1.1, ears=0)
    motions(m, r, Persona(weight=0.25, caster=True, left=True, hunch=6),
            extra={"skirt_front": cloth_sway(5, 0.15), "skirt_back": cloth_sway(6, 0.4, 4), "book": cloth_sway(8, 0.3),
                   "hood_tip0": cloth_sway(4, 0.1), "hood_tip2": cloth_sway(8, 0.3),
                   "lantern": {"idle": lambda T: {"rotation": sway(T, 6, 0, 0)}, "walk": lambda L: {"rotation": sway(L, 12, 0.3, 0)}},
                   "flame": {"idle": lambda T: {"scale": sway(T / 4, 0.12, 0, 1, [1, 1, 1])},
                             "cast": lambda C: {"scale": sway(C / 2, 0.3, 0, 1, [1.4, 1.5, 1.4])}}})
    return m


# ---------------------------------------------------------------------------- Король-лич Карн

def lich():
    """Король-лич Карн: высокий скелет-король. Мантия в синем и пурпуре с меховым воротником, разорвана на груди —
    видны рёбра; корона с камнями душ; ледяные кристаллы растут из плеч; посох с ледяным кристаллом; парит."""
    m = Model("lich")
    h = H(height=45, head=8.4, shoulders=11, hips=8, depth=5.4, leg=15, arm=15, limb=2.6, hand=3.4, neck=1.4)
    h.head = 0.01
    r = humanoid(m, h, {"skin": "l_bone", "torso": "l_robe", "arm": "l_robe_purple", "forearm": "l_bone", "hand": "l_bone",
                        "leg": "l_robe", "boot": "l_robe", "head": "l_bone"}, arm_out=10)
    h.head = 8.4
    hb = r["head"]
    y0 = r["head_y"] + 3.2
    skull(m, hb, y0, 10.6, "l_bone", "l_bone", face=lich_face)
    top = y0 + 10.6 * 0.72 + 0.6
    # корона: обод, зубцы, камни душ
    m.box(hb, [0, top + 0.6, 0], [h.head + 0.8, 1.4, h.head + 0.6], "l_gold", decal={"north": gold_trim})
    for i, (x, hgt) in enumerate(((-3.6, 2.4), (-1.8, 3.2), (0, 4.6), (1.8, 3.2), (3.6, 2.4))):
        m.box(hb, [x, top + 1.3 + hgt / 2, -h.head / 2], [1.2, hgt, 0.8], "l_gold")
    m.box(hb, [0, top + 3.0, -h.head / 2 - 0.4], [1.4, 1.6, 0.6], "l_gem")
    for sx in (-1, 1):
        m.box(hb, [3.6 * sx, top + 1.0, -h.head / 2 - 0.4], [1.0, 1.0, 0.5], "l_gem")
        for i in range(2):
            m.box(hb, [(h.head / 2 + 0.3) * sx, top + 1.3 + i * 1.8, -1 + i * 2], [0.8, 2.0, 0.8], "l_gold")
    ch = r["chest"]
    tp = r["top_y"]
    m.box(ch, [0, r["chest_y"] + 3, -h.depth / 2 - 0.9], [h.shoulders - 1, 7, 1.0], "l_robe", decal={"north": robe_ribs})
    m.box(ch, [0, tp + 0.4, 0.2], [h.shoulders + 3, 3.0, h.depth + 3.4], "l_fur")              # меховой воротник
    m.box(ch, [0, tp + 2.6, 2.4], [h.shoulders + 1, 3.6, 1.4], "l_fur")                       # высокий ворот сзади
    m.box(ch, [0, r["chest_y"] - 0.6, -h.depth / 2 - 1.2], [2.4, 2.4, 0.8], "l_gem")          # амулет-филактерия
    m.box(ch, [0, r["chest_y"] - 0.6, -h.depth / 2 - 1.0], [3.2, 3.2, 0.5], "l_gold")
    # плечи: ледяные кристаллы
    for side, sx in (("right", -1), ("left", 1)):
        arm = r[f"{side}_arm"]
        x = r[f"{side}_x"]
        sy = r["shoulder_y"]
        m.box(arm, [x + 1.2 * sx, sy + 1.0, 0], [h.limb + 5.0, 3.0, h.limb + 5.4], "l_robe_purple", decal={"north": gold_trim})
        m.box(arm, [x + 1.6 * sx, sy + 2.8, 0], [h.limb + 3.6, 1.0, h.limb + 4.4], "l_gold")
        for i, (dx, dz, ln, rz, rx) in enumerate(((1.0, -1.2, 7.0, 18, -8), (2.8, 0.6, 5.4, 38, 6), (0.0, 1.8, 4.2, 4, 18),
                                                   (3.6, -1.6, 3.6, 55, -14), (1.8, 2.0, 3.0, 26, 22))):
            m.box(arm, [x + dx * sx, sy + 3.4 + ln / 2, dz], [1.8 - i * 0.2, ln, 1.8 - i * 0.2], "l_ice" if i % 2 else "l_ice_glow",
                  rot=[rx, 0, -rz * sx], pivot=[x + dx * sx, sy + 3.2, dz])
        m.box(r[f"{side}_forearm"], [x, r[f"{side}_hand_y"] + h.hand + 1.4, 0], [h.limb + 2.6, 3.4, h.limb + 2.6],
              "l_robe", decal={"north": tatter, "south": tatter, "east": tatter, "west": tatter})
    # мантия до пола, плащ сзади
    skirt(m, r, "l_robe", tatter, [(h.hips + 3, h.depth + 3, 5), (h.hips + 5, h.depth + 4.6, 6), (h.hips + 7, h.depth + 6, 7)])
    cape = m.bone("cape", [0, tp, h.depth / 2 + 1.4], ch)
    m.box(cape, [0, tp - 13, h.depth / 2 + 1.8], [h.shoulders + 2, 26, 0.6], "l_robe_purple",
          decal={"north": tatter, "south": tatter})
    # посох с ледяным кристаллом (правая рука)
    rh = r["right_hand"]
    x, y = r["right_x"], r["right_hand_y"] + 1.6
    st = m.bone("staff", [x, y, 0], rh, rot=[14, 0, -10])
    m.box(st, [x, y + 3, 0], [1.3, 36, 1.3], "l_bone")
    for k in (-6, 2, 10):
        m.box(st, [x, y + k, 0], [1.7, 0.7, 1.7], "l_gold")
    m.box(st, [x, y + 20.2, 0], [2.6, 1.2, 2.6], "l_gold")
    m.box(st, [x, y - 15, 0], [1.8, 1.6, 1.8], "l_gold")
    cr = m.bone("crystal", [x, y + 21, 0], st)
    m.box(cr, [x, y + 24.6, 0], [3.0, 6, 3.0], "l_ice", rot=[0, 45, 0])
    m.box(cr, [x, y + 24.6, 0], [1.8, 7.8, 1.8], "l_ice_glow", rot=[0, 45, 0])
    for sx, rz in ((-1, 30), (1, -30)):
        m.box(cr, [x + 1.4 * sx, y + 23, 0], [1.0, 3.4, 1.0], "l_ice", rot=[0, 0, rz], pivot=[x, y + 21.6, 0])
    motions(m, r, Persona(weight=0.6, caster=True, float_=True, hunch=2, menace=1.3),
            extra={"skirt_front": cloth_sway(4, 0.1), "skirt_back": cloth_sway(5, 0.35, 3), "cape": cloth_sway(4, 0.3, 4),
                   "crystal": {"idle": lambda T: {"rotation": kf([(0, [0, 0, 0]), (T, [0, 360, 0])], smooth=False)},
                               "cast": lambda C: {"rotation": kf([(0, [0, 0, 0]), (C, [0, 720, 0])], smooth=False)}}})
    return m


# ---------------------------------------------------------------------------- ледяная могила и филактерия

def ice_tomb():
    """Ледяная могила: глыба льда с намёрзшими шипами, внутри — тень пойманного."""
    m = Model("ice_tomb")
    b = m.bone("block", [0, 0, 0])
    m.box(b, [0, 11, 0], [10, 22, 10], "l_ice")
    m.box(b, [0, 11, 0], [8.6, 20, 8.6], "l_ice_glow", inflate=-0.1)
    for i, (x, z, ln, rx, rz) in enumerate(((5, 2, 6, 10, -30), (-5, -2, 5, -12, 30), (2, -5, 5, -30, 6),
                                             (-2, 5, 6, 28, -8), (0, 0, 5, 0, 0))):
        m.box(b, [x, 22 + ln / 2 - 1, z], [2, ln, 2], "l_ice", rot=[rx, 0, rz], pivot=[x, 21, z])
    m.anim("idle", loop(2.0, {"block": {"scale": sway(2.0, 0.015, 0, 0, [1, 1, 1])}}))
    return m


def phylactery():
    """Филактерия: сердце лича — камень души в золотой клетке, парит и вращается."""
    m = Model("phylactery")
    core = m.bone("core", [0, 12, 0])
    m.box(core, [0, 12, 0], [3.6, 3.6, 3.6], "l_gem", rot=[45, 0, 45])
    m.box(core, [0, 12, 0], [2, 5.6, 2], "l_ice_glow")
    cage = m.bone("cage", [0, 12, 0])
    for a in (0, 90):
        m.box(cage, [0, 12, 0], [7, 0.6, 0.6], "l_gold", rot=[0, a, 0])
        m.box(cage, [0, 12, 0], [0.6, 7, 0.6], "l_gold", rot=[0, a + 45, 0])
    m.box(cage, [0, 15.8, 0], [2.6, 0.8, 2.6], "l_gold")
    m.box(cage, [0, 8.2, 0], [2.6, 0.8, 2.6], "l_gold")
    m.anim("idle", loop(4.0, {"core": {"rotation": kf([(0, [0, 0, 0]), (4, [0, 360, 0])], smooth=False),
                                       "position": sway(4, 0.8, 0, 1)},
                              "cage": {"rotation": kf([(0, [0, 0, 0]), (4, [0, -360, 0])], smooth=False),
                                       "position": sway(4, 0.8, 0, 1)}}))
    return m


MODELS = {"morra": morra, "vessa": vessa, "lich": lich, "ice_tomb": ice_tomb, "phylactery": phylactery}
