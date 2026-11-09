"""Скелет человекоподобного (кости и базовые объёмы) и живой набор движений под характер.

    r = humanoid(m, H(height=32, head=9, ...), mats={"skin": ..., "torso": ..., ...})
    r["chest"], r["head"], r["right_hand"] … — имена костей, на них модель вешает одежду, доспех, оружие.
    motions(m, r, Persona(weight=0.8, caster=True))  — idle/walk/attack/death/cast/slam + shoot/block/leap/hurt

Кости: root → body (таз) → chest → head, {right,left}_arm → _forearm → _hand; body → {right,left}_leg → _shin.
Имена совпадают с тем, что ждут общие анимации навыков хранителей (tools/boss_anims.py): body, head, right_arm, left_arm.
"""
from hy import kf, loop, once, sway, beats


class H:
    """Пропорции (в единицах модели, 16 = блок). Стиль Hytale: крупная голова, крепкие кисти, короткие ноги."""

    def __init__(self, height=32, head=9, shoulders=11, hips=8, depth=5.5, leg=12, arm=12, limb=3.4, hand=3.8,
                 neck=1.0, lean=0, belly=0.0):
        self.height, self.head, self.shoulders, self.hips, self.depth = height, head, shoulders, hips, depth
        self.leg, self.arm, self.limb, self.hand, self.neck, self.lean, self.belly = leg, arm, limb, hand, neck, lean, belly


def humanoid(m, h, mats, legs=True, skirt=False, arm_out=0):
    """Кости и базовые объёмы. mats: skin, torso, arm (рукав), hand, leg, boot, head (если голова не кожа).
    skirt — вместо ног длинное одеяние (ноги есть, но короче и спрятаны)."""
    g = lambda k, d=None: mats.get(k, d)
    r = {}
    hip_y = h.leg
    torso_h = h.height - h.leg - h.head - h.neck
    chest_y = hip_y + torso_h * 0.42
    top_y = hip_y + torso_h
    r["body"] = m.bone("body", [0, hip_y, 0], rot=[h.lean, 0, 0] if h.lean else None)
    m.box(r["body"], [0, hip_y + torso_h * 0.2, 0], [h.hips + 1, torso_h * 0.42, h.depth], g("pelvis", g("torso")))
    r["chest"] = m.bone("chest", [0, chest_y, 0], r["body"])
    ch = top_y - chest_y
    m.box(r["chest"], [0, chest_y + ch * 0.52, 0], [h.shoulders, ch * 1.0, h.depth + 0.6 + h.belly], g("torso"))
    m.box(r["chest"], [0, chest_y + ch * 0.12, -0.2 - h.belly * 0.4], [h.shoulders - 2, ch * 0.4, h.depth + h.belly], g("torso"))
    neck_y = top_y
    r["head"] = m.bone("head", [0, neck_y, 0], r["chest"])
    if h.neck > 0:
        m.box(r["head"], [0, neck_y + h.neck / 2, 0.3], [h.head * 0.42, h.neck + 0.6, h.head * 0.42], g("skin"))
    hy0 = neck_y + h.neck
    m.box(r["head"], [0, hy0 + h.head / 2, 0], [h.head, h.head, h.head * 0.95], g("head", g("skin")),
          decal=mats.get("face") and {"north": mats["face"]})
    r["head_top"] = hy0 + h.head
    r["face_z"] = -h.head * 0.475
    r["head_y"] = hy0
    # руки
    sh_y = top_y - 1.6
    for side, sx in (("right", -1), ("left", 1)):
        x = sx * (h.shoulders / 2 + h.limb / 2 - 0.2)
        arm = m.bone(f"{side}_arm", [x, sh_y, 0], r["chest"], rot=[0, 0, -arm_out * sx] if arm_out else None)
        up = h.arm * 0.48
        m.box(arm, [x, sh_y - up / 2 + 0.6, 0], [h.limb + 0.4, up + 1.2, h.limb + 0.4], g("arm", g("torso")))
        fore = m.bone(f"{side}_forearm", [x, sh_y - up, 0], arm)
        fl = h.arm * 0.42
        m.box(fore, [x, sh_y - up - fl / 2, 0], [h.limb, fl, h.limb], g("forearm", g("arm", g("torso"))))
        hand = m.bone(f"{side}_hand", [x, sh_y - up - fl, 0], fore)
        m.box(hand, [x, sh_y - up - fl - h.hand / 2 + 0.3, 0], [h.hand * 0.9, h.hand, h.hand * 0.9], g("hand", g("skin")))
        r[f"{side}_arm"], r[f"{side}_forearm"], r[f"{side}_hand"] = arm, fore, hand
        r[f"{side}_x"] = x
        r[f"{side}_hand_y"] = sh_y - up - fl - h.hand
    # ноги
    for side, sx in (("right", -1), ("left", 1)):
        x = sx * (h.hips / 4 + 0.6)
        leg = m.bone(f"{side}_leg", [x, hip_y, 0], r["body"])
        th = h.leg * 0.5
        lw = h.limb + 0.6
        if legs:
            m.box(leg, [x, hip_y - th / 2 + 0.4, 0], [lw, th + 0.8, lw], g("leg", g("torso")))
        shin = m.bone(f"{side}_shin", [x, hip_y - th, 0], leg)
        if legs:
            m.box(shin, [x, (hip_y - th) / 2 + 0.9, 0], [lw - 0.4, hip_y - th - 1.8, lw - 0.4], g("shin", g("leg", g("torso"))))
            m.box(shin, [x, 1.0, -0.6], [lw + 0.2, 2.0, lw + 1.6], g("boot", g("leg")))
        r[f"{side}_leg"], r[f"{side}_shin"] = leg, shin
    r["top_y"], r["hip_y"], r["chest_y"], r["shoulder_y"] = top_y, hip_y, chest_y, sh_y
    return r


def features(m, r, h, skin, nose=1.0, brow=True, ears=0, chin=0.0, cheek=None):
    """Объём лица поверх плоской декали: нос, надбровье, уши, подбородок — голова перестаёт быть «коробкой с лицом»."""
    hb, y0, fz = r["head"], r["head_y"], r["face_z"]
    H_ = h.head
    if nose:
        m.box(hb, [0, y0 + H_ * 0.38, fz - 0.45 * nose], [1.2 * nose, 1.8 * nose, 0.9 * nose], skin)
    if brow:
        m.box(hb, [0, y0 + H_ * 0.62, fz - 0.25], [H_ * 0.78, 0.7, 0.5], skin, light=0.9)
    if chin:
        m.box(hb, [0, y0 + 0.4, fz + 0.3], [H_ * 0.5, 1.2, 1.0 + chin], skin, light=0.95)
    if ears:
        for sx in (-1, 1):
            m.box(hb, [(H_ / 2 + 0.3) * sx, y0 + H_ * 0.48, 0.6], [0.6, 1.8 * ears, 1.4], skin,
                  rot=[0, 0, -18 * sx * ears], pivot=[(H_ / 2) * sx, y0 + H_ * 0.48, 0.6])
    if cheek:
        m.mirror(hb, [H_ * 0.26, y0 + H_ * 0.26, fz - 0.15], [H_ * 0.18, 1.0, 0.3], cheek)


class Persona:
    """Характер движений: weight (0 лёгкий … 1 тяжёлый), caster (колдует, а не рубит), hunch (сутулость),
    left (главная рука левая), float_ (парит — ноги почти не шагают), menace (размах угроз)."""

    def __init__(self, weight=0.5, caster=False, hunch=0, left=False, float_=False, menace=1.0, speed=1.0):
        self.weight, self.caster, self.hunch, self.left = weight, caster, hunch, left
        self.float_, self.menace, self.speed = float_, menace, speed


def motions(m, r, p, extra=None):
    """Полный набор живых движений. extra — {кость: {анимация: дорожки}} для одежды/хвостов (покачивание)."""
    z = [0, 0, 0]
    W = p.weight
    main, off = ("left", "right") if p.left else ("right", "left")
    ms = -1 if p.left else 1                  # знак для зеркальных поворотов главной руки
    T = 2.8 + W * 1.2                         # дыхание медленнее у тяжёлых
    hunch = [p.hunch, 0, 0]
    idle = {
        "chest": {"rotation": sway(T, 2 + W, 0, 0, hunch), "position": sway(T, 0.18 + 0.12 * W, 0.25, 1)},
        "head": {"rotation": kf([(0, z), (T * 0.3, [3, 9, 0]), (T * 0.55, [-2, 0, 0]), (T * 0.8, [3, -9, 0]), (T, z)])},
        f"{main}_arm": {"rotation": sway(T, 3, 0.2, 2, [0, 0, 5 * ms])},
        f"{off}_arm": {"rotation": sway(T, 3, 0.45, 2, [0, 0, -5 * ms])},
        f"{main}_forearm": {"rotation": [-12, 0, 0]}, f"{off}_forearm": {"rotation": [-10, 0, 0]},
    }
    if p.float_:
        idle["root"] = {"position": sway(T, 1.0, 0, 1, [0, 1.2, 0])}
        idle["right_leg"] = {"rotation": [8, 0, 3]}
        idle["left_leg"] = {"rotation": [4, 0, -3]}
    L = (1.25 - 0.35 * (1 - W)) / p.speed     # шаг
    amp = 30 - 8 * W
    walk = {
        "right_leg": {"rotation": sway(L, amp, 0, 0)}, "left_leg": {"rotation": sway(L, amp, 0.5, 0)},
        "right_shin": {"rotation": kf([(0, [6, 0, 0]), (L * 0.25, [34, 0, 0]), (L * 0.5, [4, 0, 0]), (L * 0.75, [6, 0, 0]), (L, [6, 0, 0])])},
        "left_shin": {"rotation": kf([(0, [4, 0, 0]), (L * 0.25, [6, 0, 0]), (L * 0.5, [6, 0, 0]), (L * 0.75, [34, 0, 0]), (L, [4, 0, 0])])},
        "body": {"position": kf([(0, z), (L * 0.25, [0, -0.6 - W * 0.6, 0]), (L * 0.5, z), (L * 0.75, [0, -0.6 - W * 0.6, 0]), (L, z)]),
                 "rotation": sway(L, 3 + 2 * W, 0, 1)},
        "chest": {"rotation": sway(L, 4 + 2 * W, 0.25, 1, [4 + p.hunch, 0, 0])},
        "right_arm": {"rotation": sway(L, amp * 0.7, 0.5, 0)}, "left_arm": {"rotation": sway(L, amp * 0.7, 0, 0)},
        "right_forearm": {"rotation": [-18, 0, 0]}, "left_forearm": {"rotation": [-18, 0, 0]},
        "head": {"rotation": sway(L, 3, 0.5, 1)},
    }
    if p.float_:
        walk = {"root": {"position": sway(L * 1.4, 1.0, 0, 1, [0, 1.2, 0])},
                "body": {"rotation": [10, 0, 0]}, "chest": {"rotation": sway(L * 1.4, 3, 0, 2, [6, 0, 0])},
                "right_leg": {"rotation": [20, 0, 3]}, "left_leg": {"rotation": [14, 0, -3]},
                "right_arm": {"rotation": [18, 0, 8]}, "left_arm": {"rotation": [18, 0, -8]},
                "right_forearm": {"rotation": [-20, 0, 0]}, "left_forearm": {"rotation": [-20, 0, 0]}}
    A = 0.6 + 0.35 * W
    if p.caster:
        attack = {
            f"{main}_arm": {"rotation": beats(A, [(0, z), (0.4, [-110, 25 * ms, 20 * ms]), (0.6, [-70, -10 * ms, 0]), (1, z)])},
            f"{main}_forearm": {"rotation": beats(A, [(0, z), (0.4, [-40, 0, 0]), (0.6, [0, 0, 0]), (1, z)])},
            "chest": {"rotation": beats(A, [(0, z), (0.4, [-6, 18 * ms, 0]), (0.6, [8, -14 * ms, 0]), (1, z)])},
            f"{off}_arm": {"rotation": beats(A, [(0, z), (0.4, [-30, 0, -20 * ms]), (1, z)])},
        }
    else:
        attack = {
            f"{main}_arm": {"rotation": beats(A, [(0, z), (0.38, [-150, 15 * ms, 25 * ms]), (0.56, [-30, -15 * ms, -5 * ms]), (0.75, [5, 0, 0]), (1, z)])},
            f"{main}_forearm": {"rotation": beats(A, [(0, z), (0.38, [-45, 0, 0]), (0.56, [-5, 0, 0]), (1, z)])},
            "chest": {"rotation": beats(A, [(0, z), (0.38, [-10, 28 * ms, 0]), (0.56, [14, -24 * ms, 0]), (1, z)])},
            "body": {"rotation": beats(A, [(0, z), (0.38, [0, 8 * ms, 0]), (0.56, [0, -8 * ms, 0]), (1, z)])},
            f"{off}_arm": {"rotation": beats(A, [(0, z), (0.38, [20, 0, -10 * ms]), (0.56, [-20, 0, -10 * ms]), (1, z)])},
        }
    C = 1.6
    cast = {
        "right_arm": {"rotation": sway(C, 5, 0, 0, [-80, 0, 30])}, "left_arm": {"rotation": sway(C, 5, 0.5, 0, [-80, 0, -30])},
        "right_forearm": {"rotation": [-25, 0, 0]}, "left_forearm": {"rotation": [-25, 0, 0]},
        "chest": {"rotation": sway(C, 3, 0, 0, [-8, 0, 0])}, "head": {"rotation": sway(C, 4, 0.25, 0, [-12, 0, 0])},
    }
    if p.float_:
        cast["root"] = {"position": sway(C, 1.2, 0, 1, [0, 2, 0])}
    slam = {
        "right_arm": {"rotation": beats(1.1, [(0, z), (0.4, [-175, 0, 12]), (0.58, [-25, 0, 0]), (0.8, [-30, 0, 0]), (1, z)])},
        "left_arm": {"rotation": beats(1.1, [(0, z), (0.4, [-175, 0, -12]), (0.58, [-25, 0, 0]), (0.8, [-30, 0, 0]), (1, z)])},
        "chest": {"rotation": beats(1.1, [(0, z), (0.4, [-20, 0, 0]), (0.58, [32, 0, 0]), (0.8, [26, 0, 0]), (1, z)])},
        "body": {"position": beats(1.1, [(0, z), (0.4, [0, 1.2, 0]), (0.58, [0, -2, 0]), (0.8, [0, -1.6, 0]), (1, z)])},
        "right_shin": {"rotation": beats(1.1, [(0, z), (0.58, [22, 0, 0]), (1, z)])}, "left_shin": {"rotation": beats(1.1, [(0, z), (0.58, [22, 0, 0]), (1, z)])},
        "right_leg": {"rotation": beats(1.1, [(0, z), (0.58, [-14, 0, 0]), (1, z)])}, "left_leg": {"rotation": beats(1.1, [(0, z), (0.58, [-14, 0, 0]), (1, z)])},
    }
    D = 1.4 + W * 0.4
    death = {
        "body": {"position": beats(D, [(0, z), (0.35, [0, -3, 0]), (1, [0, -r["hip_y"] + 2, 0])]),
                 "rotation": beats(D, [(0, z), (0.35, [12, 0, 4]), (1, [82, 0, 10])])},
        "right_leg": {"rotation": beats(D, [(0, z), (0.35, [-50, 0, 8]), (1, [-86, 0, 12])])},
        "left_leg": {"rotation": beats(D, [(0, z), (0.35, [-40, 0, -8]), (1, [-86, 0, -14])])},
        "right_shin": {"rotation": beats(D, [(0, z), (0.35, [70, 0, 0]), (1, [90, 0, 0])])},
        "left_shin": {"rotation": beats(D, [(0, z), (0.35, [60, 0, 0]), (1, [90, 0, 0])])},
        "head": {"rotation": beats(D, [(0, z), (0.5, [24, 20, 0]), (1, [30, 40, 18])])},
        "right_arm": {"rotation": beats(D, [(0, z), (1, [-40, 0, 50])])}, "left_arm": {"rotation": beats(D, [(0, z), (1, [-30, 0, -60])])},
    }
    shoot = {
        f"{off}_arm": {"rotation": beats(0.7, [(0, z), (0.3, [-90, 10 * ms, 0]), (0.8, [-88, 10 * ms, 0]), (1, z)])},
        f"{main}_arm": {"rotation": beats(0.7, [(0, z), (0.3, [-90, -30 * ms, 0]), (0.65, [-82, -60 * ms, 0]), (0.75, [-90, -10 * ms, 0]), (1, z)])},
        f"{main}_forearm": {"rotation": beats(0.7, [(0, z), (0.3, [-10, 0, 0]), (0.65, [-60, 0, 0]), (0.75, [0, 0, 0]), (1, z)])},
        "chest": {"rotation": beats(0.7, [(0, z), (0.3, [0, 22 * ms, 0]), (1, z)])},
    }
    block = {
        f"{off}_arm": {"rotation": beats(0.5, [(0, z), (0.2, [-80, 30 * ms, 0]), (0.8, [-80, 30 * ms, 0]), (1, z)])},
        f"{off}_forearm": {"rotation": beats(0.5, [(0, z), (0.2, [-40, 0, 0]), (0.8, [-40, 0, 0]), (1, z)])},
        "chest": {"rotation": beats(0.5, [(0, z), (0.2, [-8, -10 * ms, 0]), (1, z)])},
        "right_leg": {"rotation": beats(0.5, [(0, z), (0.2, [-10, 0, 0]), (1, z)])},
    }
    leap = {
        "body": {"rotation": beats(0.6, [(0, z), (0.25, [28, 0, 0]), (0.6, [-12, 0, 0]), (1, z)]),
                 "position": beats(0.6, [(0, z), (0.25, [0, -2, 0]), (0.6, [0, 1, 0]), (1, z)])},
        "right_arm": {"rotation": beats(0.6, [(0, z), (0.25, [40, 0, 0]), (0.6, [-150, 0, 10]), (1, z)])},
        "left_arm": {"rotation": beats(0.6, [(0, z), (0.25, [40, 0, 0]), (0.6, [-150, 0, -10]), (1, z)])},
        "right_shin": {"rotation": beats(0.6, [(0, z), (0.25, [50, 0, 0]), (0.6, [10, 0, 0]), (1, z)])},
        "left_shin": {"rotation": beats(0.6, [(0, z), (0.25, [50, 0, 0]), (0.6, [10, 0, 0]), (1, z)])},
    }
    hurt = {
        "chest": {"rotation": beats(0.35, [(0, z), (0.25, [-14, 0, 6]), (1, z)])},
        "head": {"rotation": beats(0.35, [(0, z), (0.25, [-20, 0, 0]), (1, z)])},
        "right_arm": {"rotation": beats(0.35, [(0, z), (0.25, [-20, 0, 20]), (1, z)])},
        "left_arm": {"rotation": beats(0.35, [(0, z), (0.25, [-20, 0, -20]), (1, z)])},
    }
    sets = {"idle": (loop, T, idle), "walk": (loop, L if not p.float_ else L * 1.4, walk), "attack": (once, A, attack),
            "cast": (loop, C, cast), "slam": (once, 1.1, slam), "death": (once, D, death), "shoot": (once, 0.7, shoot),
            "block": (once, 0.5, block), "leap": (once, 0.6, leap), "hurt": (once, 0.35, hurt)}
    for name, (kind, ln, bones) in sets.items():
        if extra:
            for bone, tracks in extra.items():
                if name in tracks:
                    bones.setdefault(bone, {}).update(tracks[name](ln) if callable(tracks[name]) else tracks[name])
        if name in m.anims:
            continue      # своя анимация модели важнее
        m.anim(name, kind(ln, bones) if kind is loop else once(ln, bones, hold=(name == "death")))


def cloth_sway(amp=6, phase=0.2, base=0):
    """Покачивание ткани для extra: на покое — мягко, при ходьбе — сильнее, при смерти — висит."""
    return {"idle": lambda T: {"rotation": sway(T, amp * 0.6, phase, 0, [base, 0, 0])},
            "walk": lambda L: {"rotation": sway(L, amp * 1.6, phase + 0.25, 0, [base + amp, 0, 0])},
            "cast": lambda C: {"rotation": sway(C, amp, phase, 0, [base - amp * 0.5, 0, 0])},
            "leap": lambda L: {"rotation": beats(L, [(0, [base, 0, 0]), (0.5, [base + amp * 4, 0, 0]), (1, [base, 0, 0])])}}
