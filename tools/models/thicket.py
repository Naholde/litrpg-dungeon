"""Этап 2 · Паучья чаща — пауки, оса, лесная ведьма, древень-страж; хранители Аракна, Гнилой Древень, Морена;
коконы, кладки, гнилой плод."""
import math
import hy
from hy import Model, mat, kf, loop, once, sway, beats
from rig import H, humanoid, motions, Persona, cloth_sway, features
from crypt import rivets, tatter

mat("sp_chitin", "#2E2A2C", "chitin", dark="#141214", light="#5A5258", gloss=0.6)
mat("sp_chitin2", "#46403E", "chitin", dark="#221E1C", light="#6E6660", gloss=0.4)
mat("sp_fur", "#3A3430", "fur", dark="#1A1614", light="#5E544C")
mat("sp_purple", "#4A2A5A", "chitin", dark="#24142E", light="#7A4E90", gloss=0.7)
mat("sp_eye", "#FF5A4A", "glow", glow=True, light="#FFE0D0", dark="#B0201A")
mat("sp_eye_g", "#B4FF5A", "glow", glow=True, light="#F4FFE0", dark="#5AA020")
mat("sp_eye_v", "#E07AFF", "glow", glow=True, light="#FFE8FF", dark="#9A30C0")
mat("sp_fang", "#E4DCC8", "bone", dark="#A09884", light="#FFFFFF", scale=0.6)
mat("sp_silk", "#E8E8F0", "cloth", dark="#B0B0C0", light="#FFFFFF")
mat("sp_acid", "#9CF04A", "glow", glow=True, light="#EAFFC0", dark="#4AA018")
mat("sp_band", "#C8B080", "chitin", dark="#806A44", light="#F0DCA8")
mat("wasp_y", "#E8B428", "chitin", dark="#8A6410", light="#FFE27A", gloss=0.5)
mat("wasp_k", "#1E1A16", "chitin", dark="#0C0A08", light="#3E3830", gloss=0.5)
mat("wing", "#C8E0EC", "plain", dark="#90B0C4", light="#F0FAFF")
mat("bark", "#5E4630", "bark", dark="#2E2012", light="#86684A")
mat("bark_dark", "#3A2A1C", "bark", dark="#1C140C", light="#5A4430")
mat("moss", "#4E7A30", "moss", dark="#2A4418", light="#7AAA4A")
mat("leaf", "#3E6E2A", "moss", dark="#203A14", light="#6A9E44")
mat("amber", "#FFB43A", "glow", glow=True, light="#FFF0C0", dark="#C06A10")
mat("rot", "#5E3A6A", "skin", dark="#301C38", light="#8A5E98")
mat("spore", "#D88AFF", "glow", glow=True, light="#FFE8FF", dark="#8A3AC0")
mat("mush_red", "#B8302A", "plain", dark="#6A1612", light="#E8604A")
mat("mush_cap", "#E8DCC8", "plain", dark="#B0A48E", light="#FFFFFF")
mat("witch_skin", "#B8C4A8", "skin", dark="#788468", light="#DCE8CA")
mat("witch_cloak", "#2E3A22", "cloth", dark="#161E10", light="#4A5E36")
mat("feather", "#1E1A26", "fur", dark="#0C0A10", light="#3E3650")
mat("mor_violet", "#3A1E4A", "cloth", dark="#1C0E26", light="#62387A")
mat("mor_skin", "#C8B8D0", "skin", dark="#86769A", light="#ECE0F4")
mat("antler", "#6A5440", "bark", dark="#3A2C1E", light="#9A8064", scale=0.6)
mat("egg_shell", "#D8CCE0", "skin", dark="#9A8AA8", light="#F4ECFA")
mat("eye_big", "#FFE85A", "glow", glow=True, light="#FFFFFF", dark="#D89A10")


# ---------------------------------------------------------------------------- паук

def spider(m, size=1.0, abdomen=(9, 7, 10), body_mat="sp_chitin", leg_mat="sp_chitin2", fur="sp_fur",
           eyes="sp_eye", abd_mat=None, abd_decal=None, leg_len=1.0, stance=1.0):
    """Паук: головогрудь (head), брюшко (abdomen), 8 ног из трёх колен (свои кости), хелицеры с клыками.
    Кости: body → head, abdomen, leg_<сторона><1..4>_a/_b/_c, fang_r/fang_l."""
    k = size
    by = 5.2 * k
    body = m.bone("body", [0, by, 0])
    m.box(body, [0, by + 0.6 * k, 0], [6.4 * k, 3.8 * k, 7.0 * k], body_mat)
    m.box(body, [0, by + 2.6 * k, 0.4 * k], [4.8 * k, 1.0 * k, 5.4 * k], fur)
    hd = m.bone("head", [0, by + 0.6 * k, -3.4 * k], body)
    m.box(hd, [0, by + 0.8 * k, -4.8 * k], [4.8 * k, 3.2 * k, 3.4 * k], body_mat)
    # глаза: два крупных, четыре малых
    for sx in (-1, 1):
        m.box(hd, [1.1 * sx * k, by + 1.6 * k, -6.55 * k], [1.2 * k, 1.2 * k, 0.4], eyes)
        m.box(hd, [2.0 * sx * k, by + 2.2 * k, -6.3 * k], [0.7 * k, 0.7 * k, 0.4], eyes)
        m.box(hd, [0.5 * sx * k, by + 2.5 * k, -6.4 * k], [0.6 * k, 0.6 * k, 0.4], eyes)
    for side, sx in (("r", -1), ("l", 1)):
        fb = m.bone(f"fang_{side}", [0.9 * sx * k, by - 0.2 * k, -6.2 * k], hd)
        m.box(fb, [0.9 * sx * k, by - 0.8 * k, -6.4 * k], [1.2 * k, 1.6 * k, 1.2 * k], body_mat)
        m.box(fb, [0.8 * sx * k, by - 2.0 * k, -6.6 * k], [0.6 * k, 1.4 * k, 0.6 * k], "sp_fang", rot=[0, 0, 12 * sx])
    ab = m.bone("abdomen", [0, by + 0.8 * k, 3.2 * k], body, rot=[16, 0, 0])
    aw, ah, ad = (v * k for v in abdomen)
    c = [0, by + ah * 0.42, 3.0 * k + ad / 2]
    am = abd_mat or body_mat
    m.box(ab, c, [aw, ah * 0.74, ad * 0.78], am, decal={"up": abd_decal} if abd_decal else None)   # брюшко: три объёма —
    m.box(ab, c, [aw * 0.76, ah, ad * 0.82], am)                                                    # выходит округлым
    m.box(ab, c, [aw * 0.8, ah * 0.7, ad], am)
    m.box(ab, [0, c[1] + ah * 0.5, c[2]], [aw * 0.5, 0.6 * k, ad * 0.6], fur)                      # ворс по хребту
    m.box(ab, [0, c[1] - ah * 0.1, c[2] + ad / 2 + 0.3], [aw * 0.3, ah * 0.3, 1.0 * k], body_mat)  # прядильце
    legs = []
    for i in range(4):
        z = (-2.6 + i * 1.8) * k
        spread = [-50, -20, 15, 45][i]
        for side, sx in (("r", -1), ("l", 1)):
            name = f"leg_{side}{i + 1}"
            a = m.bone(f"{name}_a", [2.8 * sx * k, by + 0.4 * k, z], body, rot=[0, spread * sx, -30 * sx * stance])
            L1, L2, L3 = (v * k * leg_len for v in (4.6, 5.0, 4.2))
            m.cube(a, [2.8 * sx * k - (L1 if sx < 0 else 0), by - 0.5 * k, z - 0.6 * k], [L1, 1.2 * k, 1.2 * k], leg_mat)
            b = m.bone(f"{name}_b", [2.8 * sx * k + L1 * sx, by + 0.1 * k, z], a, rot=[0, 0, 62 * sx * stance])
            m.cube(b, [2.8 * sx * k + L1 * sx - (L2 if sx < 0 else 0), by - 0.45 * k, z - 0.5 * k], [L2, 1.0 * k, 1.0 * k], leg_mat)
            m.cube(b, [2.8 * sx * k + L1 * sx - (0.8 * k if sx < 0 else 0), by - 0.7 * k, z - 0.7 * k], [0.8 * k, 1.4 * k, 1.4 * k], body_mat)
            c = m.bone(f"{name}_c", [2.8 * sx * k + (L1 + L2) * sx, by + 0.05 * k, z], b, rot=[0, 0, 26 * sx * stance])
            m.cube(c, [2.8 * sx * k + (L1 + L2) * sx - (L3 if sx < 0 else 0), by - 0.35 * k, z - 0.35 * k], [L3, 0.7 * k, 0.7 * k], fur)
            legs.append((name, sx, i))
    return {"body": body, "head": hd, "abdomen": ab, "legs": legs, "by": by}


def spider_motions(m, sp, speed=1.0, menace=1.0):
    z = [0, 0, 0]
    T = 2.4
    idle = {"body": {"position": sway(T, 0.25, 0, 1)}, "abdomen": {"rotation": sway(T, 3, 0.2, 0)},
            "fang_r": {"rotation": sway(T / 2, 8, 0, 2)}, "fang_l": {"rotation": sway(T / 2, 8, 0.5, 2)},
            "head": {"rotation": kf([(0, z), (T * 0.3, [0, 8, 0]), (T * 0.6, [4, -6, 0]), (T, z)])}}
    for name, sx, i in sp["legs"]:
        idle[f"{name}_a"] = {"rotation": sway(T, 2, i * 0.13 + (0.5 if sx > 0 else 0), 1)}
    L = 0.55 / speed
    walk = {"body": {"position": sway(L / 2, 0.35, 0, 1)}, "abdomen": {"rotation": sway(L, 4, 0.25, 1)}}
    for name, sx, i in sp["legs"]:
        ph = (i % 2) * 0.5 + (0.5 if sx > 0 else 0)
        walk[f"{name}_a"] = {"rotation": sway(L, 18, ph, 1, [0, 0, 0])}
        walk[f"{name}_b"] = {"rotation": sway(L, 12, ph + 0.25, 2)}
    A = 0.55
    attack = {"head": {"rotation": beats(A, [(0, z), (0.4, [-28, 0, 0]), (0.62, [26, 0, 0]), (1, z)])},
              "body": {"position": beats(A, [(0, z), (0.4, [0, 1, 2]), (0.62, [0, -0.5, -3]), (1, z)])},
              "fang_r": {"rotation": beats(A, [(0, z), (0.4, [0, 0, -30]), (0.62, [0, 0, 20]), (1, z)])},
              "fang_l": {"rotation": beats(A, [(0, z), (0.4, [0, 0, 30]), (0.62, [0, 0, -20]), (1, z)])}}
    for name, sx, i in sp["legs"]:
        if i == 0:
            attack[f"{name}_a"] = {"rotation": beats(A, [(0, z), (0.4, [0, 0, 50 * sx]), (0.62, [0, 0, -10 * sx]), (1, z)])}
    death = {"body": {"position": beats(1.2, [(0, z), (1, [0, -3, 0])])}, "abdomen": {"rotation": beats(1.2, [(0, z), (1, [10, 0, 0])])}}
    for name, sx, i in sp["legs"]:
        death[f"{name}_a"] = {"rotation": beats(1.2, [(0, z), (0.4, [0, 0, 40 * sx]), (1, [0, 0, 70 * sx])])}
        death[f"{name}_b"] = {"rotation": beats(1.2, [(0, z), (1, [0, 0, 80 * sx])])}
    shoot = {"abdomen": {"rotation": beats(0.6, [(0, z), (0.4, [-40, 0, 0]), (0.6, [10, 0, 0]), (1, z)])},
             "body": {"rotation": beats(0.6, [(0, z), (0.4, [12, 0, 0]), (1, z)])}}
    leap = {"body": {"position": beats(0.6, [(0, z), (0.25, [0, -1.5, 0]), (0.6, [0, 2, 0]), (1, z)])}}
    for name, sx, i in sp["legs"]:
        leap[f"{name}_a"] = {"rotation": beats(0.6, [(0, z), (0.25, [0, 0, -20 * sx]), (0.6, [0, 0, 30 * sx]), (1, z)])}
    hurt = {"body": {"rotation": beats(0.3, [(0, z), (0.3, [-10, 0, 6]), (1, z)])}, "head": {"rotation": beats(0.3, [(0, z), (0.3, [-15, 0, 0]), (1, z)])}}
    cast = {"abdomen": {"scale": beats(0.8, [(0, [1, 1, 1]), (0.5, [1.15, 0.92, 1.15]), (1, [1, 1, 1])])},
            "head": {"rotation": beats(0.8, [(0, z), (0.5, [-20, 0, 0]), (1, z)])}}
    block = {"body": {"position": beats(0.5, [(0, z), (0.3, [0, -1.4, 1]), (1, z)])}}
    for nm, kind, ln, bones in (("idle", loop, T, idle), ("walk", loop, L, walk), ("attack", once, A, attack),
                                ("shoot", once, 0.6, shoot), ("leap", once, 0.6, leap), ("hurt", once, 0.3, hurt),
                                ("cast", once, 0.8, cast), ("block", once, 0.5, block)):
        if nm not in m.anims:
            m.anim(nm, kind(ln, bones))
    if "death" not in m.anims:
        m.anim("death", once(1.2, death, hold=True))
    m.anims.setdefault("slam", once(0.9, {"body": {"position": beats(0.9, [(0, z), (0.4, [0, 2, 0]), (0.6, [0, -1.5, 0]), (1, z)])}}))
    m.anims.setdefault("cast", loop(1.2, cast["abdomen"] and {"abdomen": {"scale": sway(1.2, 0.08, 0, 0, [1.05, 1, 1.05])}}))


def stripe_abd(f):
    for y in range(0, f.h, 4):
        f.rect(0, y, f.w, y + 1, "#C8B080")


def skull_abd(f):
    """Узор-череп на брюшке королевы (светится)."""
    w, h = f.w, f.h
    cx = w // 2
    for y in range(int(h * 0.2), int(h * 0.7)):
        half = int(w * 0.28 * (1 - abs(y - h * 0.4) / (h * 0.4)) + 2)
        f.rect(cx - half, y, cx + half, y + 1, "#C060E0", glow=True)
    f.rect(cx - int(w * 0.16), int(h * 0.35), cx - 1, int(h * 0.45), "#1A0E20")
    f.rect(cx + 1, int(h * 0.35), cx + int(w * 0.16), int(h * 0.45), "#1A0E20")
    for x in range(cx - 4, cx + 5, 2):
        f.rect(x, int(h * 0.6), x + 1, int(h * 0.68), "#E8B0FF", glow=True)


def acid_spots(f):
    for i in range(6):
        x, y = int(hy.h32(i, 3) * (f.w - 3)), int(hy.h32(i, 7) * (f.h - 3))
        f.rect(x, y, x + 2, y + 2, "#9CF04A", glow=True)


def weaver():
    """Паук-ткач: брюшко в полосах, на спине — шёлковый мешок, нити на лапах."""
    m = Model("weaver")
    sp = spider(m, 1.0, abdomen=(9, 7, 10), abd_decal=stripe_abd)
    m.box(sp["abdomen"], [0, sp["by"] + 7.4, 8.4], [5, 3, 5], "sp_silk")
    spider_motions(m, sp)
    return m


def jumper():
    """Прыгун: поджарый, длинноногий, огромные передние глаза, мохнатый."""
    m = Model("jumper")
    sp = spider(m, 0.95, abdomen=(6.6, 5.4, 7.4), fur="sp_fur", eyes="sp_eye_g", leg_len=1.25, stance=1.2)
    m.box(sp["head"], [0, sp["by"] + 1.4, -6.6], [3.6, 2.0, 0.4], "sp_eye_g")
    spider_motions(m, sp, speed=1.6)
    return m


def spitter():
    """Кислотный плевун: брюшко задрано, светящиеся кислотные мешки, сопло."""
    m = Model("spitter")
    sp = spider(m, 1.0, abdomen=(8.6, 8, 8.6), abd_mat="sp_chitin2", abd_decal=acid_spots, eyes="sp_eye_g")
    ab = sp["abdomen"]
    m.bone("abd_tilt", [0, sp["by"], 3], ab)
    for sx in (-1, 1):
        m.box(ab, [3.8 * sx, sp["by"] + 4, 7], [2.4, 3.2, 3.2], "sp_acid")
    m.box(ab, [0, sp["by"] + 8.6, 10], [2, 2.4, 2], "sp_chitin")
    m.box(ab, [0, sp["by"] + 9.9, 10], [1.2, 0.6, 1.2], "sp_acid")
    spider_motions(m, sp)
    return m


def arachna():
    """Королева пауков Аракна: огромная, пурпурный хитин, светящийся череп на брюшке, корона из шипов,
    кладка яиц под брюшком, свисающий шёлк."""
    m = Model("arachna")
    sp = spider(m, 1.12, abdomen=(11, 9, 12), body_mat="sp_purple", leg_mat="sp_chitin", fur="sp_fur",
                eyes="sp_eye_v", abd_mat="sp_purple", abd_decal=skull_abd, leg_len=1.02)
    hd = sp["head"]
    by = sp["by"]
    for i, (x, ln, rz) in enumerate(((-2, 3.2, 18), (-0.8, 4.2, 6), (0.8, 4.2, -6), (2, 3.2, -18))):
        m.box(hd, [x, by + 3.6 + ln / 2, -4.6], [0.8, ln, 0.8], "sp_fang", rot=[-14, 0, rz], pivot=[x, by + 3.4, -4.6])
    m.box(hd, [0, by + 3.4, -4.6], [5.2, 0.8, 3.2], "gold")
    ab = sp["abdomen"]
    for i in range(5):
        m.box(ab, [(-2 + i) * 1.6, by - 0.6, 7 + (i % 2) * 1.6], [1.6, 1.6, 1.6], "sp_eye_v")   # кладка
    for i, x in enumerate((-3, 0, 3)):
        s = m.bone(f"silk{i}", [x, by + 1, 14], ab)
        m.box(s, [x, by - 2, 14.2], [0.4, 6, 0.4], "sp_silk")
    spider_motions(m, sp, speed=0.8)
    return m


def wasp():
    """Трутень-оса: полосатое брюшко с жалом, сетчатые глаза, две пары крыльев (быстро машут)."""
    m = Model("wasp")
    body = m.bone("body", [0, 8, 0])
    m.box(body, [0, 8, 0], [3.4, 3.4, 4], "wasp_k")
    hd = m.bone("head", [0, 8.4, -2], body)
    m.box(hd, [0, 8.6, -3.6], [3.4, 3.0, 2.8], "wasp_y")
    for sx in (-1, 1):
        m.box(hd, [1.6 * sx, 9.0, -3.8], [1.0, 2.0, 2.2], "sp_eye")
        m.segments(f"ant_{'r' if sx < 0 else 'l'}", hd, [0.6 * sx, 10, -4.4], [(2, 0.4, [-40, 0, 10 * sx]), (2, 0.35, [-30, 0, 0])], "wasp_k")
    m.box(hd, [0, 7.2, -5.0], [1.6, 0.8, 0.8], "wasp_k")
    ab = m.bone("abdomen", [0, 8, 2], body, rot=[-18, 0, 0])
    for i in range(4):
        w = 4.2 - abs(i - 1.2) * 0.6
        m.box(ab, [0, 8, 3.2 + i * 1.6], [w, w * 0.9, 1.6], "wasp_y" if i % 2 == 0 else "wasp_k")
    m.box(ab, [0, 8, 10.2], [0.6, 0.6, 2], "sp_fang")
    for side, sx in (("r", -1), ("l", 1)):
        for j, (z, ln) in enumerate(((-0.6, 7), (0.8, 5))):
            wb = m.bone(f"wing_{side}{j}", [1.4 * sx, 9.8, z], body)
            m.cube(wb, [1.4 * sx - (ln if sx < 0 else 0), 9.8, z - 1.2], [ln, 0.1, 2.4 if j == 0 else 1.8], "wing")
        for j in range(3):
            lb = m.bone(f"leg_{side}{j}", [1.2 * sx, 6.6, -1 + j * 1.2], body, rot=[0, 0, -40 * sx])
            m.cube(lb, [1.2 * sx - (0.4 if sx < 0 else 0), 3.6, -1.2 + j * 1.2], [0.4, 3.0, 0.4], "wasp_k")
    z = [0, 0, 0]
    flap = {}
    for side, sx in (("r", -1), ("l", 1)):
        for j in range(2):
            flap[f"wing_{side}{j}"] = {"rotation": kf([(0, [0, 0, 30 * sx]), (0.05, [0, 0, -30 * sx]), (0.1, [0, 0, 30 * sx])], smooth=False)}
    m.anim("idle", loop(0.1 * 12, dict(flap, body={"position": sway(1.2, 0.6, 0, 1)}, abdomen={"rotation": sway(1.2, 6, 0.2, 0)})) if False else
           loop(0.1, dict(flap)))
    m.anims["idle"] = {"loop": True, "animation_length": 0.1, "bones": flap}
    walk = dict(flap)
    walk["body"] = {"rotation": [14, 0, 0]}
    m.anim("walk", {"loop": True, "animation_length": 0.1, "bones": walk})
    m.anim("attack", once(0.5, {"abdomen": {"rotation": beats(0.5, [(0, z), (0.4, [-60, 0, 0]), (0.6, [20, 0, 0]), (1, z)])},
                                "body": {"position": beats(0.5, [(0, z), (0.5, [0, 0, -2]), (1, z)])}}))
    m.anim("death", once(1.0, {"body": {"rotation": beats(1.0, [(0, z), (1, [0, 0, 180])]),
                                        "position": beats(1.0, [(0, z), (1, [0, -6, 0])])}}, hold=True))
    for nm, ln in (("shoot", 0.6), ("cast", 0.8), ("block", 0.5), ("leap", 0.6), ("hurt", 0.3)):
        m.anim(nm, once(ln, {"abdomen": {"rotation": beats(ln, [(0, z), (0.4, [-40, 0, 0]), (1, z)])}}))
    return m


# ---------------------------------------------------------------------------- лесная ведьма, древень-страж

def witch_face(f):
    w, h = f.w, f.h
    f.shade(0, 0, w, int(h * 0.25), 0.6)
    for sx in (int(w * 0.2), int(w * 0.6)):
        f.rect(sx, int(h * 0.4), sx + 3, int(h * 0.4) + 2, "#1A2010")
        f.rect(sx + 1, int(h * 0.4), sx + 3, int(h * 0.4) + 2, "#B4FF5A", glow=True)
    f.rect(int(w * 0.44), int(h * 0.5), int(w * 0.56), int(h * 0.72), "#9AA88A")              # длинный нос
    f.set(int(w * 0.5), int(h * 0.72), "#6A7858")
    f.rect(int(w * 0.36), int(h * 0.8), int(w * 0.64), int(h * 0.8) + 1, "#3A2A2A")


def bark_face(f):
    """Лицо древня: дупло-рот, янтарные глаза в трещинах коры."""
    w, h = f.w, f.h
    for sx in (int(w * 0.18), int(w * 0.6)):
        f.rect(sx, int(h * 0.32), sx + int(w * 0.22), int(h * 0.32) + 3, "#1A120A")
        f.rect(sx + 1, int(h * 0.32) + 1, sx + int(w * 0.22) - 1, int(h * 0.32) + 3, "#FFB43A", glow=True)
    f.rect(int(w * 0.3), int(h * 0.62), int(w * 0.7), int(h * 0.86), "#140C06")
    f.rect(int(w * 0.34), int(h * 0.64), int(w * 0.66), int(h * 0.68), "#3A2412")


def forest_witch():
    """Лесная ведьма: лохматый плащ из мха и перьев, рога-ветви, гриб на плече, посох-корень с янтарём."""
    m = Model("forest_witch")
    h = H(height=36, head=8, shoulders=9, hips=7.4, depth=5, leg=12.4, arm=12.5, limb=2.6, hand=2.8)
    r = humanoid(m, h, {"skin": "witch_skin", "torso": "witch_cloak", "arm": "witch_cloak", "forearm": "witch_skin",
                        "hand": "witch_skin", "leg": "witch_cloak", "boot": "bark_dark", "head": "witch_skin",
                        "face": witch_face}, arm_out=8)
    hb, top, y0 = r["head"], r["head_top"], r["head_y"]
    m.box(hb, [0, top + 0.4, 0.4], [h.head + 1.4, 1.8, h.head + 1.4], "moss")                  # шапка мха
    m.box(hb, [0, y0 + 2, h.head / 2 + 0.4], [h.head + 1, 7, 1.6], "feather")                   # волосы-перья
    for sx in (-1, 1):
        s = 'r' if sx < 0 else 'l'
        m.segments(f"antler_{s}", hb, [3.0 * sx, top + 0.6, -0.4], [(3.2, 1.2, [0, 0, 30 * sx]), (2.6, 1.0, [-10, 0, -20 * sx]),
                                                                    (2.0, 0.8, [-18, 0, 20 * sx])], "antler")
        m.box(hb, [4.4 * sx, top + 3.4, -0.4], [2.2, 0.7, 0.7], "antler", rot=[0, 0, -30 * sx])
    ch = r["chest"]
    cl = m.bone("cloak", [0, r["top_y"], 1.8], ch)
    m.box(cl, [0, r["top_y"] - 10, 3.2], [h.shoulders + 3, 22, 1.0], "feather", decal={"north": tatter, "south": tatter})
    m.box(ch, [0, r["top_y"] + 0.2, 0], [h.shoulders + 3.4, 2.4, h.depth + 2.6], "moss")
    m.box(r["left_arm"], [r["left_x"] + 0.6, r["shoulder_y"] + 2.2, 0], [1.4, 2.0, 1.4], "mush_cap")
    m.box(r["left_arm"], [r["left_x"] + 0.6, r["shoulder_y"] + 3.8, 0], [3.6, 1.2, 3.6], "mush_red",
          decal={"up": lambda f: [f.set(int(hy.h32(i, 1) * f.w), int(hy.h32(i, 2) * f.h), "#FFFFFF") for i in range(6)]})
    body = r["body"]
    m.box(body, [0, r["hip_y"] - 3, 0.4], [h.hips + 3.4, 8, h.depth + 3], "witch_cloak", decal={"north": tatter, "south": tatter, "east": tatter, "west": tatter})
    rh = r["right_hand"]
    x, y = r["right_x"], r["right_hand_y"] + 1
    st = m.bone("staff", [x, y, 0], rh, rot=[12, 0, -6])
    m.segments("root", st, [x, y - 10, 0], [(8, 1.4, [0, 0, 4]), (8, 1.3, [0, 0, -6]), (7, 1.2, [0, 0, 6]), (3, 1.0, [20, 0, -10]),
                                            (2.4, 2.4, [0, 0, 0])], ["bark", "bark", "bark", "bark", "amber"])
    features(m, r, h, "witch_skin", nose=1.6, chin=0.8)
    motions(m, r, Persona(weight=0.3, caster=True, hunch=10), extra={"cloak": cloth_sway(6, 0.3, 4)})
    return m


def treant_guard():
    """Древень-страж: ходячее дерево — кора, мох на плечах, ветви вместо волос, янтарные глаза, руки-сучья."""
    m = Model("treant_guard")
    h = H(height=36, head=8.4, shoulders=12, hips=8.6, depth=7, leg=12, arm=15, limb=3.8, hand=4.6, neck=0.4)
    r = humanoid(m, h, {"skin": "bark", "torso": "bark", "arm": "bark", "hand": "bark_dark", "leg": "bark_dark",
                        "boot": "bark_dark", "head": "bark", "face": bark_face}, arm_out=10)
    hb, top = r["head"], r["head_top"]
    for i, (x, z, ln, rx, rz) in enumerate(((-3, 0, 5, -10, 24), (0, -1, 6, -20, 0), (3, 0, 5, -8, -26), (-1.6, 2, 4.4, 20, 12), (1.8, 2, 4.6, 24, -10))):
        m.box(hb, [x, top + ln / 2 - 0.4, z], [1.4, ln, 1.4], "bark_dark", rot=[rx, 0, rz], pivot=[x, top - 0.4, z])
        m.box(hb, [x * 1.5, top + ln - 0.6, z - 0.4], [2.6, 2.0, 2.6], "leaf")
    ch = r["chest"]
    m.mirror(ch, [h.shoulders / 2 - 1.6, r["top_y"] - 0.6, -h.depth / 2 - 0.4], [4.0, 2.6, h.depth + 0.8], "moss")
    m.box(ch, [2, r["chest_y"] + 3, -h.depth / 2 - 0.3], [3, 3, 0.5], "moss")
    for side, sx in (("right", -1), ("left", 1)):
        hand = r[f"{side}_hand"]
        x, y = r[f"{side}_x"], r[f"{side}_hand_y"]
        for dz, rz in ((-1.6, 10), (0, 0), (1.6, -10)):
            m.box(hand, [x, y - 1.4, dz], [1.2, 3.6, 1.2], "bark_dark", rot=[-10, 0, rz * sx], pivot=[x, y + 0.2, dz])
    motions(m, r, Persona(weight=0.85, hunch=4))
    return m


# ---------------------------------------------------------------------------- Гнилой Древень

def rot_ent():
    """Гнилой Древень: исполинское больное дерево. Грудь — дупло, в нём пульсирует сердцевина со спорами;
    грибы и гнилые плоды на плечах, корни вместо ног, ветви-руки с шипами."""
    m = Model("rot_ent")
    h = H(height=40, head=9, shoulders=15, hips=10, depth=9, leg=12, arm=17, limb=4.6, hand=5.4, neck=0.2, lean=6)
    r = humanoid(m, h, {"skin": "bark", "torso": "bark_dark", "arm": "bark", "hand": "bark_dark", "leg": "bark_dark",
                        "boot": "bark_dark", "head": "bark", "face": bark_face}, arm_out=12)
    ch = r["chest"]
    cy = r["chest_y"]
    m.box(ch, [0, cy + 3.4, -h.depth / 2 - 0.2], [7, 7, 1.2], "rot")                            # дупло
    core = m.bone("heart", [0, cy + 3.4, -2.6], ch)
    m.box(core, [0, cy + 3.4, -3.4], [3.6, 4.2, 3.0], "spore")
    m.box(core, [0, cy + 3.4, -3.4], [2.2, 5.4, 2.0], "spore")
    for i, (x, y, ln, rz) in enumerate(((-6, 0, 7, 30), (6, 0, 6, -34), (-3, 1, 9, 8), (3.6, 1, 8, -12), (0, 1, 10, 0))):
        m.box(ch, [x, r["top_y"] + ln / 2 + y, 1.4], [2.0, ln, 2.0], "bark_dark", rot=[-14, 0, rz], pivot=[x, r["top_y"], 1.4])
        m.box(ch, [x * 1.4, r["top_y"] + ln + y, 0.6], [4.2, 3.0, 4.2], "rot")
    for sx in (-1, 1):
        x = sx * (h.shoulders / 2 - 1)
        m.box(ch, [x, r["top_y"] + 0.8, -1], [1.4, 2.6, 1.4], "mush_cap")
        m.box(ch, [x, r["top_y"] + 2.6, -1], [4.6, 1.4, 4.6], "mush_red")
        m.box(ch, [x * 0.6, r["top_y"] + 0.6, 2.6], [3, 3, 3], "rot", decal={"north": lambda f: f.rect(1, 1, f.w - 1, 2, "#D88AFF", True)})
    for side, sx in (("right", -1), ("left", 1)):
        for i in range(3):
            m.box(r[f"{side}_forearm"], [r[f"{side}_x"] + 2 * sx, r[f"{side}_hand_y"] + 9 - i * 2.6, 0], [2.4, 0.9, 0.9], "bark_dark", rot=[0, 0, -30 * sx])
        hand = r[f"{side}_hand"]
        x, y = r[f"{side}_x"], r[f"{side}_hand_y"]
        for dz, rz in ((-2, 14), (0, 0), (2, -14)):
            m.box(hand, [x, y - 2, dz], [1.6, 5, 1.6], "bark_dark", rot=[-14, 0, rz * sx], pivot=[x, y + 0.4, dz])
        sh = r[f"{side}_shin"]
        lx = sx * (h.hips / 4 + 0.6)
        for dx, dz, rz, rx in ((2.4, -1.6, -40, -10), (-0.6, -3, 0, -40), (1.6, 2.4, -30, 30)):
            m.box(sh, [lx + dx * sx * 0.6, 0.8, dz], [1.8, 1.6, 4.6], "bark_dark", rot=[rx * 0.3, rz * sx, 0])
    motions(m, r, Persona(weight=1.0, hunch=8, menace=1.4),
            extra={"heart": {"idle": lambda T: {"scale": sway(T / 2, 0.08, 0, 0, [1, 1, 1])},
                             "cast": lambda C: {"scale": sway(C / 2, 0.18, 0, 0, [1.15, 1.15, 1.15])}}})
    return m


# ---------------------------------------------------------------------------- Морена

def morena_face(f):
    """Ведьма Изнанки: половина лица — белая маска с трещиной, половина — живая, фиолетовый глаз."""
    w, h = f.w, f.h
    f.rect(0, 0, w // 2, h, "#E8E4EC")
    f.line(w // 2, 0, w // 2 - 2, h, "#3A2A44")
    for sx, col in ((int(w * 0.18), "#120A16"), (int(w * 0.62), "#2A1A30")):
        f.rect(sx, int(h * 0.38), sx + 3, int(h * 0.38) + 2, col)
    f.rect(int(w * 0.18) + 1, int(h * 0.38), int(w * 0.18) + 3, int(h * 0.38) + 2, "#E07AFF", glow=True)
    f.rect(int(w * 0.62) + 1, int(h * 0.38), int(w * 0.62) + 3, int(h * 0.38) + 2, "#E07AFF", glow=True)
    f.rect(int(w * 0.38), int(h * 0.76), int(w * 0.66), int(h * 0.76) + 1, "#2A0E2A")
    f.shade(0, 0, w, 2, 0.6)


def morena():
    """Морена, ведьма Изнанки: высокая, в фиолетовом, плащ из вороньих перьев, рога-ветви, половина лица — маска,
    посох с огромным глазом (левая рука), висящие обереги."""
    m = Model("morena")
    h = H(height=42, head=8.4, shoulders=9.6, hips=7.6, depth=5, leg=15, arm=14, limb=2.6, hand=3.0)
    r = humanoid(m, h, {"skin": "mor_skin", "torso": "mor_violet", "arm": "mor_violet", "forearm": "mor_skin",
                        "hand": "mor_skin", "leg": "mor_violet", "boot": "feather", "head": "mor_skin",
                        "face": morena_face}, arm_out=9)
    hb, top, y0 = r["head"], r["head_top"], r["head_y"]
    m.box(hb, [0, y0 + 3, h.head / 2 + 0.6], [h.head + 1.2, 9, 1.8], "feather")                 # волосы
    for sx in (-1, 1):
        s = 'r' if sx < 0 else 'l'
        m.segments(f"horn_{s}", hb, [3.2 * sx, top - 0.2, 0.6], [(3.4, 1.4, [-14, 0, 24 * sx]), (3.4, 1.1, [-18, 0, -18 * sx]),
                                                                  (2.6, 0.9, [-20, 0, 22 * sx]), (2.0, 0.7, [-14, 0, -16 * sx])], "antler")
    ch = r["chest"]
    m.box(ch, [0, r["top_y"] + 0.4, 0.4], [h.shoulders + 4, 2.6, h.depth + 3], "feather")
    cl = m.bone("cloak", [0, r["top_y"], 2], ch)
    m.box(cl, [0, r["top_y"] - 13, 3.2], [h.shoulders + 5, 28, 1.0], "feather", decal={"north": tatter, "south": tatter})
    for i in range(3):
        m.box(ch, [-2 + i * 2, r["chest_y"] + 1 - i % 2, -h.depth / 2 - 0.5], [1.0, 1.4, 0.4], "bone")   # обереги
    body = r["body"]
    for i, (w, d, hg) in enumerate(((h.hips + 1.6, h.depth + 1.6, 5), (h.hips + 3.6, h.depth + 3.4, 6), (h.hips + 6, h.depth + 5.4, 7))):
        m.box(body, [0, r["hip_y"] + 1 - i * 5 - hg / 2 + 2, 0.4], [w, hg, d], "mor_violet",
              decal={"north": tatter, "south": tatter, "east": tatter, "west": tatter} if i == 2 else None)
    lh = r["left_hand"]
    x, y = r["left_x"], r["left_hand_y"] + 1
    st = m.bone("staff", [x, y, 0], lh, rot=[12, 0, 6])
    m.box(st, [x, y + 8, 0], [1.2, 38, 1.2], "bark_dark")
    ey = y + 29.5
    eye = m.bone("eye", [x, ey, 0], st)
    m.box(eye, [x, ey, 0], [4.4, 4.4, 4.4], "mor_skin")
    m.box(eye, [x, ey, -2.25], [3.0, 3.0, 0.4], "eye_big")
    m.box(eye, [x, ey, -2.5], [1.2, 1.8, 0.3], "socket")
    for a in (-1, 1):
        m.box(st, [x + 2.6 * a, ey, 0], [1.0, 5, 1.0], "antler", rot=[0, 0, 30 * a], pivot=[x, ey - 2, 0])
    features(m, r, h, "mor_skin", nose=0.9)
    motions(m, r, Persona(weight=0.3, caster=True, left=True, hunch=4, menace=1.3),
            extra={"cloak": cloth_sway(5, 0.3, 4),
                   "eye": {"idle": lambda T: {"rotation": kf([(0, [0, 0, 0]), (T * 0.3, [0, 30, 0]), (T * 0.6, [10, -30, 0]), (T, [0, 0, 0])])}}})
    return m


# ---------------------------------------------------------------------------- коконы, кладка, гнилой плод

def cocoon():
    """Кокон: обмотанный шёлком силуэт, слои нитей, внутри дёргается."""
    m = Model("cocoon")
    b = m.bone("body", [0, 0, 0])
    for i, (w, y, hg) in enumerate(((7, 2, 5), (9, 7, 6), (9.4, 13, 6), (8, 19, 5), (5.6, 23.4, 4))):
        m.box(b, [0, y + hg / 2 - 2, 0], [w, hg, w * 0.9], "sp_silk", rot=[0, i * 17, 0])
    for i in range(5):
        m.box(b, [0, 3 + i * 4.6, 0], [9.6 - abs(i - 2) * 1.2, 0.6, 0.6], "sp_band", rot=[0, i * 36, 0])
    m.anim("idle", loop(2.2, {"body": {"rotation": kf([(0, [0, 0, 0]), (0.2, [0, 0, 3]), (0.4, [0, 0, -2]), (0.6, [0, 0, 0]), (2.2, [0, 0, 0])]),
                                       "scale": sway(2.2, 0.02, 0, 0, [1, 1, 1])}}))
    return m


def egg_veins(f):
    """Прожилки скорлупы и тень зародыша внутри (светится)."""
    cx, cy = f.w // 2, f.h // 2
    f.rect(cx - 1, cy - 1, cx + 2, cy + 2, "#B050E0", glow=True)
    f.set(cx, cy, "#F0C0FF", glow=True)
    for i in range(3):
        x = int(hy.h32(i, f.w) * f.w)
        f.line(x, 0, cx, cy, "#C890E8", glow=False)


def egg():
    """Кладка: гроздь полупрозрачных яиц, внутри светятся зародыши, вылупление — дрожь."""
    m = Model("egg")
    b = m.bone("body", [0, 0, 0])
    for i, (x, z, s) in enumerate(((0, 0, 6), (-3, 2, 4.2), (3, 1.6, 4.6), (1, -3, 3.8), (-2.4, -2.2, 3.4))):
        m.box(b, [x, s / 2 + 0.4, z], [s, s * 1.1, s * 0.8], "egg_shell", decal={f: egg_veins for f in ("north", "south", "east", "west")})
        m.box(b, [x, s / 2 + 0.4, z], [s * 0.8, s * 1.2, s], "egg_shell", decal={f: egg_veins for f in ("north", "south", "east", "west")})
    m.box(b, [0, 0.4, 0], [10, 0.8, 9], "sp_silk")
    for i in range(4):
        m.box(b, [-4 + i * 2.6, 3, -4 + (i % 2) * 8], [0.3, 6, 0.3], "sp_silk", rot=[0, 0, 20 - i * 12])
    m.anim("idle", loop(1.6, {"body": {"scale": sway(1.6, 0.04, 0, 1, [1, 1, 1])}}))
    return m


def rot_fruit():
    """Гнилой плод Древня: раздутый плод с прожилками, споры светятся, лопается."""
    m = Model("rot_fruit")
    b = m.bone("body", [0, 0, 0])
    m.box(b, [0, 6, 0], [9, 9, 9], "rot")
    m.box(b, [0, 6, 0], [7, 11, 7], "rot")
    m.box(b, [0, 6, 0], [11, 7, 7], "rot")
    for i in range(6):
        a = i * math.pi / 3
        m.box(b, [math.cos(a) * 4.6, 6 + (i % 2) * 2, math.sin(a) * 4.6], [1.6, 1.6, 1.6], "spore")
    m.box(b, [0, 11.6, 0], [1.2, 2.4, 1.2], "bark_dark")
    m.box(b, [1.4, 12.4, 0], [2.4, 0.4, 1.4], "leaf")
    m.anim("idle", loop(1.2, {"body": {"scale": sway(1.2, 0.06, 0, 0, [1, 1, 1])}}))
    return m


MODELS = {"weaver": weaver, "jumper": jumper, "spitter": spitter, "arachna": arachna, "wasp": wasp,
          "forest_witch": forest_witch, "treant_guard": treant_guard, "rot_ent": rot_ent, "morena": morena,
          "cocoon": cocoon, "egg": egg, "rot_fruit": rot_fruit}
