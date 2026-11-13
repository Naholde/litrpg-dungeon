"""
Личные анимации хранителей (обновление 15). Ставится ПОСЛЕ `boss_anims.add` (тот пишет общие a_* по стилю навыка).

Три слоя, от общего к личному:
  1. `body_mech` — живое тело для всех человекоподобных: локти сгибаются на замахе и распрямляются в удар, ноги делают
     шаг или присед, корпус проседает — вместо «деревянных рук» общей анимации.
  2. `FLAVOR` — манера хранителя на всех его приёмах: челюсть Привратника лязгает на ударе, шестерни Хронофага
     раскручиваются, щупальца Ксалатара извиваются, колёса Колесницы крутятся, у Лича вспыхивает кристалл посоха.
  3. `SIGNATURE` — коронные приёмы целиком свои (лич взлетает в шторм, Владыка Пепла вонзает меч в пол, Хронофаг
     откатывает время, Арахна встаёт на дыбы…).
Длина каждой анимации = подготовка навыка (BossRegistry), удар приходится на 0.85–0.93 длины — как и у общих.

Знаки (GeckoLib, проверено превью): rotation x < 0 — рука вперёд/вверх, корпус x > 0 — наклон вперёд, голова x < 0 — взгляд
вверх; челюсть x > 0 — рот открыт; position y > 0 — вверх.
"""
import math

from boss_anims import STYLES, kf

Z = [0, 0, 0]
ONE = [1, 1, 1]
W, H = 0.85, 0.93   # замах и удар в долях длины


def tr(L, keys, rest=Z):
    """Дорожка: покой → ключи (доля длины, значение) → покой."""
    pts = [(0, rest)] + [(L * f, v) for f, v in keys] + [(L, rest)]
    out, seen = [], set()
    for t, v in pts:
        t = round(t, 3)
        if t in seen:
            continue
        seen.add(t)
        out.append((t, v))
    return kf(out)


def hold(L, a, v, b=0.97):
    """Войти в позу к доле a, держать до b."""
    return tr(L, [(a, v), (b, v)])


def wave(L, n, amp, axis=2, phase=0.0, a=0.05, b=0.95):
    """Колебание по оси: n полупериодов от a до b."""
    keys = []
    steps = max(2, n * 2)
    for i in range(steps + 1):
        f = a + (b - a) * i / steps
        v = [0, 0, 0]
        v[axis] = amp * math.sin(math.pi * i / 2 + phase)
        keys.append((f, v))
    return tr(L, keys)


def spin(L, deg, axis=1, a=0.0, b=1.0):
    v0, v1 = [0, 0, 0], [0, 0, 0]
    v1[axis] = deg
    return kf([(round(L * a, 3), v0), (round(L * b, 3), v1)] + ([(round(L, 3), v1)] if b < 1 else []))


def pulse(L, k, a=0.3, b=W, c=H):
    return tr(L, [(a, [k, k, k]), (b, [k * 1.05, k * 1.05, k * 1.05]), (c, [k * 1.25, k * 1.25, k * 1.25])], rest=ONE)


def put(anim, bones, override=True):
    """Влить кости в анимацию; override=False — не трогать уже заданные каналы."""
    dst = anim.setdefault("bones", {})
    for b, ch in bones.items():
        d = dst.setdefault(b, {})
        for c, v in ch.items():
            if override or c not in d:
                d[c] = v


# ---------------------------------------------------------------------------------------------------------------------
# 1. Живое тело человекоподобных

def body_mech(style, L, left):
    a, b = ("left", "right") if left else ("right", "left")
    s = -1 if left else 1
    if style == "cleave":
        return {f"{a}_forearm": {"rotation": tr(L, [(W, [-55, 0, 0]), (H, [-5, 0, 0])])},
                f"{a}_leg": {"rotation": tr(L, [(W, [12, 0, 0]), (H, [-28, 0, 0])])},
                f"{a}_shin": {"rotation": tr(L, [(H, [22, 0, 0])])},
                f"{b}_leg": {"rotation": tr(L, [(W, [-8, 0, 0]), (H, [18, 0, 0])])},
                "root": {"position": tr(L, [(W, [0, 0.3, 0]), (H, [0, -0.9, 0])])}}
    if style == "overhead":
        return {"right_forearm": {"rotation": tr(L, [(W, [-60, 0, 0]), (H, [-8, 0, 0])])},
                "left_forearm": {"rotation": tr(L, [(W, [-60, 0, 0]), (H, [-8, 0, 0])])},
                "right_leg": {"rotation": tr(L, [(W, [5, 0, 4]), (H, [-30, 0, 8])])},
                "left_leg": {"rotation": tr(L, [(W, [5, 0, -4]), (H, [-30, 0, -8])])},
                "right_shin": {"rotation": tr(L, [(H, [42, 0, 0])])},
                "left_shin": {"rotation": tr(L, [(H, [42, 0, 0])])}}
    if style == "thrust":
        return {f"{a}_forearm": {"rotation": tr(L, [(W, [-75, 0, 0]), (H, [0, 0, 0])])},
                f"{a}_leg": {"rotation": tr(L, [(H, [-32, 0, 0])])},
                f"{a}_shin": {"rotation": tr(L, [(H, [28, 0, 0])])},
                f"{b}_leg": {"rotation": tr(L, [(W, [-10, 0, 0]), (H, [24, 0, 0])])}}
    if style == "spin":
        return {"right_leg": {"rotation": tr(L, [(W, [-6, 0, 14]), (H, [0, 0, 18])])},
                "left_leg": {"rotation": tr(L, [(W, [-6, 0, -14]), (H, [0, 0, -18])])},
                "right_forearm": {"rotation": tr(L, [(W, [-25, 0, 0]), (H, [0, 0, 0])])},
                "left_forearm": {"rotation": tr(L, [(W, [-25, 0, 0]), (H, [0, 0, 0])])}}
    if style == "raise":
        return {"right_forearm": {"rotation": tr(L, [(0.4, [-25, 0, 0]), (0.8, [-15, 0, 0]), (H, [-40, 0, 0])])},
                "left_forearm": {"rotation": tr(L, [(0.4, [-25, 0, 0]), (0.8, [-15, 0, 0]), (H, [-40, 0, 0])])},
                "right_hand": {"rotation": tr(L, [(0.4, [0, 0, 20]), (H, [0, 0, -10])])},
                "left_hand": {"rotation": tr(L, [(0.4, [0, 0, -20]), (H, [0, 0, 10])])},
                "root": {"position": tr(L, [(0.8, [0, 0.6, 0]), (H, [0, -0.4, 0])])}}
    if style == "summon":
        return {"right_forearm": {"rotation": tr(L, [(W, [-30, 0, 0]), (H, [-60, 0, 0])])},
                "left_forearm": {"rotation": tr(L, [(W, [-30, 0, 0]), (H, [-60, 0, 0])])},
                "right_hand": {"rotation": tr(L, [(W, [40, 0, 0]), (H, [-30, 0, 0])])},
                "left_hand": {"rotation": tr(L, [(W, [40, 0, 0]), (H, [-30, 0, 0])])},
                "right_leg": {"rotation": tr(L, [(W, [-20, 0, 6]), (H, [-6, 0, 6])])},
                "right_shin": {"rotation": tr(L, [(W, [35, 0, 0]), (H, [10, 0, 0])])},
                "left_leg": {"rotation": tr(L, [(W, [-20, 0, -6]), (H, [-6, 0, -6])])},
                "left_shin": {"rotation": tr(L, [(W, [35, 0, 0]), (H, [10, 0, 0])])}}
    if style == "channel":
        shake = [(0.2 + 0.7 * i / 10, [-40 + (4 if i % 2 else -4), 0, 0]) for i in range(11)]
        return {"right_forearm": {"rotation": tr(L, shake)},
                "left_forearm": {"rotation": tr(L, shake)},
                "right_hand": {"rotation": tr(L, [(0.2, [0, 0, 25]), (0.9, [0, 0, 30])])},
                "left_hand": {"rotation": tr(L, [(0.2, [0, 0, -25]), (0.9, [0, 0, -30])])}}
    # point
    return {f"{a}_forearm": {"rotation": tr(L, [(0.3, [-10, 0, 0]), (H, [0, 0, 0])])},
            f"{a}_hand": {"rotation": tr(L, [(0.3, [0, 0, 15 * s]), (H, [0, 0, 0])])},
            f"{b}_forearm": {"rotation": tr(L, [(0.3, [-35, 0, 0])])}}


# ---------------------------------------------------------------------------------------------------------------------
# 2. Манера хранителя (на всех приёмах)

def jaw(L, deg=28):
    return {"jaw": {"rotation": tr(L, [(0.3, [6, 0, 0]), (W, [10, 0, 0]), (H, [deg, 0, 0]), (0.98, [deg * 0.6, 0, 0])])}}


def chain_lag(prefix, n, L, amp):
    return {f"{prefix}{i}": {"rotation": tr(L, [(W, [amp * (i + 1) * 0.4, 0, 0]), (H, [-amp * (i + 1) * 0.5, 0, 0]),
                                                (0.97, [amp * 0.15, 0, 0])])} for i in range(n)}


def tentacles(L, amp, slam=False):
    out = {}
    for t in range(4):
        side = 1 if t % 2 else -1
        for j in range(4):
            ph = t * 0.9 + j * 0.6
            keys = [(0.05 + 0.8 * i / 8, [amp * (j + 1) * 0.35 * math.sin(i + ph), 0, side * amp * 0.5 * math.cos(i * 0.8 + ph)])
                    for i in range(9)]
            if slam:
                keys.append((H, [-amp * 1.2 if j == 0 else amp * 0.6, 0, side * amp * 1.4]))
            out[f"tent{t}_{j}"] = {"rotation": tr(L, keys)}
    return out


def flavor(name, style, L):
    f = {}
    if name == "gatekeeper":
        f.update(jaw(L, 30))
        f["soul"] = {"scale": pulse(L, 1.15)}
        if style in ("cleave", "spin", "overhead", "charge", "thrust"):
            f.update(chain_lag("chain", 3, L, 22))
            f["weight"] = {"rotation": tr(L, [(W, [30, 0, 0]), (H, [-60, 0, 0])])}
        f["cloth_front"] = {"rotation": tr(L, [(W, [-12, 0, 0]), (H, [14, 0, 0])])}
        f["cloth_back"] = {"rotation": tr(L, [(W, [10, 0, 0]), (H, [-16, 0, 0])])}
    elif name == "morra":
        f.update({f"blade{i}": {"rotation": tr(L, [(W, [0, 0, -6 * (i + 1)]), (H, [0, 0, 4 * (i + 1)])])} for i in range(5)})
        f.update(chain_lag("hood_tip", 3, L, 10))
        f["skirt_front"] = {"rotation": tr(L, [(H, [-14, 0, 0])])}
    elif name == "vessa":
        f["flame"] = {"scale": pulse(L, 1.4, a=0.2)}
        f["lantern"] = {"rotation": wave(L, 4, 12, axis=2)}
        f.update(chain_lag("hair_r", 3, L, 8))
        f.update(chain_lag("hair_l", 3, L, 8))
        f.update(chain_lag("hood_tip", 4, L, 6))
    elif name == "lich":
        f.update(jaw(L, 24))
        f["crystal"] = {"scale": pulse(L, 1.35, a=0.2)}
        f["cape"] = {"rotation": tr(L, [(W, [-18, 0, 0]), (H, [-30, 0, 0])])}
        f["skirt_back"] = {"rotation": tr(L, [(H, [-12, 0, 0])])}
    elif name == "arachna":
        f["fang_r"] = {"rotation": tr(L, [(W, [0, -25, 0]), (H, [0, 15, 0])])}
        f["fang_l"] = {"rotation": tr(L, [(W, [0, 25, 0]), (H, [0, -15, 0])])}
        f["leg_r1_a"] = {"rotation": tr(L, [(W, [0, 0, 35]), (H, [0, 0, -10])])}
        f["leg_l1_a"] = {"rotation": tr(L, [(W, [0, 0, -35]), (H, [0, 0, 10])])}
        f["leg_r1_b"] = {"rotation": tr(L, [(W, [0, 0, -30]), (H, [0, 0, 20])])}
        f["leg_l1_b"] = {"rotation": tr(L, [(W, [0, 0, 30]), (H, [0, 0, -20])])}
        for i in range(3):
            f[f"silk{i}"] = {"rotation": wave(L, 3, 12, axis=0, phase=i)}
    elif name == "rot_ent":
        f["heart"] = {"scale": pulse(L, 1.3, a=0.25)}
        f["head"] = {"rotation": tr(L, [(W, [-15, 0, 0]), (H, [12, 0, 0])])} if style not in ("raise", "summon", "channel") else {}
    elif name == "morena":
        f["eye"] = {"scale": pulse(L, 1.5, a=0.2)}
        f["cloak"] = {"rotation": tr(L, [(W, [-14, 0, 0]), (H, [-26, 0, 0])])}
        f.update(chain_lag("horn_r", 4, L, 3))
        f.update(chain_lag("horn_l", 4, L, 3))
    elif name == "tide_warden":
        f["fin0"] = {"rotation": tr(L, [(W, [-14, 0, 0]), (H, [10, 0, 0])])}
        f["fin1"] = {"rotation": tr(L, [(W, [-10, 0, 0]), (H, [8, 0, 0])])}
        f["fin2"] = {"rotation": tr(L, [(W, [-8, 0, 0]), (H, [6, 0, 0])])}
        f["shield"] = {"rotation": tr(L, [(W, [0, -20, 0]), (H, [0, 10, 0])])}
    elif name == "leviathan":
        f.update(jaw(L, 34))
        f["flipper_r"] = {"rotation": wave(L, 4, 30, axis=2)}
        f["flipper_l"] = {"rotation": wave(L, 4, -30, axis=2)}
    elif name == "naiad":
        f["halo"] = {"rotation": spin(L, 360, axis=1), "scale": pulse(L, 1.2, a=0.2)}
        f["hair"] = {"rotation": tr(L, [(W, [-14, 0, 0]), (H, [10, 0, 0])])}
        f["fin_back"] = {"rotation": wave(L, 3, 10, axis=1)}
    elif name == "smelter":
        f["hammer"] = {"rotation": tr(L, [(W, [-30, 0, 0]), (H, [25, 0, 0])])}
        f["chest"] = {"scale": tr(L, [(0.4, [1.03, 1.03, 1.03]), (W, [1.06, 1.04, 1.06]), (H, [0.97, 0.98, 0.97])], rest=ONE)}
    elif name == "chariot":
        f["wheel_r"] = {"rotation": spin(L, -540, axis=0)}
        f["wheel_l"] = {"rotation": spin(L, -540, axis=0)}
        f["horse_head"] = {"rotation": tr(L, [(W, [-30, 0, 0]), (H, [20, 0, 0])])}
        f["mane"] = {"rotation": wave(L, 4, 10, axis=0)}
        f.update(chain_lag("tail", 3, L, 18))
        f["driver"] = {"rotation": tr(L, [(W, [-15, 0, 0]), (H, [20, 0, 0])])}
    elif name == "ash_lord":
        f["crown"] = {"scale": pulse(L, 1.12, a=0.3)}
        f["cape"] = {"rotation": tr(L, [(W, [-20, 0, 0]), (H, [-32, 0, 0])])}
        f["sword"] = {"rotation": tr(L, [(W, [-15, 0, 0]), (H, [10, 0, 0])])}
    elif name == "doppel":
        f["cape"] = {"rotation": tr(L, [(W, [-16, 0, 0]), (H, [-28, 0, 0])])}
        f["blade_left"] = {"rotation": tr(L, [(W, [0, 0, -25]), (H, [0, 0, 15])])}
        f["blade_right"] = {"rotation": tr(L, [(W, [0, 0, 25]), (H, [0, 0, -15])])}
    elif name == "chronophage":
        f["gears"] = {"rotation": spin(L, 720, axis=2)}
        f["hand_right"] = {"rotation": spin(L, -1080, axis=2)}
        f["hand_left"] = {"rotation": spin(L, -90 * 4, axis=2)}
    elif name == "xalathar":
        f.update(jaw(L, 36))
        f.update(tentacles(L, 22, slam=style in ("overhead", "spin")))
        f["rift"] = {"rotation": spin(L, 360, axis=2), "scale": pulse(L, 1.3, a=0.2)}
    elif name == "trial_warden":
        f["orb_r"] = {"rotation": spin(L, 360, axis=1), "scale": pulse(L, 1.2)}
        f["orb_l"] = {"rotation": spin(L, -360, axis=1), "scale": pulse(L, 1.2)}
    return f


# ---------------------------------------------------------------------------------------------------------------------
# 3. Коронные приёмы

def sig_lich_storm(L):
    """Ледяной шторм: лич отрывается от пола, руки в стороны, голова запрокинута, плащ бьётся."""
    up = [(0.35, [0, 2.5, 0]), (W, [0, 3.2, 0]), (H, [0, 1.0, 0])]
    return {"root": {"position": tr(L, up)},
            "right_arm": {"rotation": tr(L, [(0.35, [-20, 0, 80]), (W, [-30, 0, 100]), (H, [-90, 0, 40])])},
            "left_arm": {"rotation": tr(L, [(0.35, [-20, 0, -80]), (W, [-30, 0, -100]), (H, [-90, 0, -40])])},
            "head": {"rotation": tr(L, [(0.35, [-35, 0, 0]), (W, [-40, 0, 0]), (H, [10, 0, 0])])},
            "body": {"rotation": tr(L, [(0.35, [-10, 0, 0]), (H, [12, 0, 0])])},
            "right_leg": {"rotation": tr(L, [(0.35, [15, 0, 6]), (H, [0, 0, 0])])},
            "left_leg": {"rotation": tr(L, [(0.35, [8, 0, -6]), (H, [0, 0, 0])])},
            "cape": {"rotation": wave(L, 6, 18, axis=0)},
            "skirt_front": {"rotation": wave(L, 6, 12, axis=0, phase=1)},
            "crystal": {"scale": tr(L, [(0.35, [1.4, 1.4, 1.4]), (W, [1.6, 1.6, 1.6]), (H, [2.0, 2.0, 2.0])], rest=ONE)}}


def sig_lich_raise(L):
    """Подъём мёртвых: посох в пол, лич склоняется, челюсть раскрыта."""
    return {"right_arm": {"rotation": tr(L, [(W, [-60, 0, 10]), (H, [-20, 0, 5])])},
            "right_forearm": {"rotation": tr(L, [(W, [-40, 0, 0]), (H, [0, 0, 0])])},
            "body": {"rotation": tr(L, [(W, [-10, 0, 0]), (H, [30, 0, 0])])},
            "left_arm": {"rotation": tr(L, [(W, [-30, 0, -50]), (H, [-70, 0, -20])])},
            "jaw": {"rotation": tr(L, [(W, [15, 0, 0]), (H, [34, 0, 0])])},
            "right_leg": {"rotation": tr(L, [(H, [-20, 0, 0])])}, "right_shin": {"rotation": tr(L, [(H, [30, 0, 0])])},
            "root": {"position": tr(L, [(H, [0, -1.2, 0])])}}


def sig_gatekeeper_rampart(L):
    """Костяной вал: Привратник бьёт кулаками в пол, ревёт, цепь взлетает и падает."""
    return {"right_arm": {"rotation": tr(L, [(W, [-150, 0, 25]), (H, [-35, 0, 10])])},
            "left_arm": {"rotation": tr(L, [(W, [-150, 0, -25]), (H, [-35, 0, -10])])},
            "right_forearm": {"rotation": tr(L, [(W, [-50, 0, 0]), (H, [-5, 0, 0])])},
            "left_forearm": {"rotation": tr(L, [(W, [-50, 0, 0]), (H, [-5, 0, 0])])},
            "body": {"rotation": tr(L, [(W, [-14, 0, 0]), (H, [38, 0, 0])])},
            "head": {"rotation": tr(L, [(W, [-30, 0, 0]), (H, [-10, 0, 0])])},
            "jaw": {"rotation": tr(L, [(W, [20, 0, 0]), (H, [40, 0, 0])])},
            "right_leg": {"rotation": tr(L, [(H, [-35, 0, 10])])}, "left_leg": {"rotation": tr(L, [(H, [-35, 0, -10])])},
            "right_shin": {"rotation": tr(L, [(H, [50, 0, 0])])}, "left_shin": {"rotation": tr(L, [(H, [50, 0, 0])])},
            "root": {"position": tr(L, [(W, [0, 1.5, 0]), (H, [0, -2.0, 0])])},
            "chain0": {"rotation": tr(L, [(W, [-70, 0, 0]), (H, [60, 0, 0])])},
            "weight": {"rotation": tr(L, [(W, [-40, 0, 0]), (H, [70, 0, 0])])},
            "soul": {"scale": tr(L, [(W, [1.3, 1.3, 1.3]), (H, [1.6, 1.6, 1.6])], rest=ONE)}}


def sig_morra_harvest(L):
    """Жатва: Морра раскручивается на месте дважды, коса на вытянутых руках."""
    return {"root": {"rotation": kf([(0, Z), (round(L * 0.3, 3), [0, -30, 0]), (round(L * H, 3), [0, 720, 0]), (round(L, 3), [0, 720, 0])])},
            "right_arm": {"rotation": tr(L, [(0.3, [-60, 0, 60]), (H, [-80, 0, 90])])},
            "left_arm": {"rotation": tr(L, [(0.3, [-60, 0, -30]), (H, [-80, 0, -60])])},
            "body": {"rotation": tr(L, [(0.3, [20, 0, 0]), (H, [10, 0, 0])])},
            "skirt_front": {"rotation": tr(L, [(0.3, [-30, 0, 0]), (H, [-40, 0, 0])])},
            "skirt_back": {"rotation": tr(L, [(0.3, [30, 0, 0]), (H, [40, 0, 0])])}}


def sig_vessa_candles(L):
    """Свечи: Весса поднимает фонарь над головой, пламя разгорается, волосы взлетают."""
    return {"left_arm": {"rotation": tr(L, [(0.4, [-160, 0, -10]), (W, [-170, 0, -15]), (H, [-120, 0, -30])])},
            "left_forearm": {"rotation": tr(L, [(0.4, [-10, 0, 0])])},
            "right_arm": {"rotation": tr(L, [(0.4, [-40, 0, 50]), (H, [-60, 0, 70])])},
            "head": {"rotation": tr(L, [(0.4, [-30, 0, 0]), (H, [-10, 0, 0])])},
            "flame": {"scale": tr(L, [(0.4, [1.5, 1.8, 1.5]), (W, [1.8, 2.2, 1.8]), (H, [2.4, 2.8, 2.4])], rest=ONE)},
            "hair_r0": {"rotation": tr(L, [(0.4, [-30, 0, 20])])}, "hair_l0": {"rotation": tr(L, [(0.4, [-30, 0, -20])])},
            "root": {"position": tr(L, [(0.4, [0, 0.8, 0]), (H, [0, 0.2, 0])])}}


def sig_arachna_rear(L):
    """Арахна встаёт на дыбы: перед поднят, передние лапы над головой, брюшко поджато — и обрушивается."""
    out = {"body": {"rotation": tr(L, [(W, [-35, 0, 0]), (H, [12, 0, 0])])},
           "root": {"position": tr(L, [(W, [0, 2.0, 0]), (H, [0, -0.6, 0])])},
           "abdomen": {"rotation": tr(L, [(W, [40, 0, 0]), (H, [-10, 0, 0])])},
           "head": {"rotation": tr(L, [(W, [20, 0, 0]), (H, [-15, 0, 0])])}}
    for k, (r, l) in enumerate(((35, -35), (20, -20))):
        n = k + 1
        out[f"leg_r{n}_a"] = {"rotation": tr(L, [(W, [-50, 0, r + 20]), (H, [10, 0, -10])])}
        out[f"leg_l{n}_a"] = {"rotation": tr(L, [(W, [-50, 0, l - 20]), (H, [10, 0, 10])])}
    for n in (3, 4):
        out[f"leg_r{n}_b"] = {"rotation": tr(L, [(W, [0, 0, 15]), (H, [0, 0, -5])])}
        out[f"leg_l{n}_b"] = {"rotation": tr(L, [(W, [0, 0, -15]), (H, [0, 0, 5])])}
    return out


def sig_arachna_web(L):
    """Плетение сети: брюшко задирается вверх-вперёд, нити ходят, задние лапы перебирают."""
    out = {"abdomen": {"rotation": tr(L, [(0.4, [40, 0, 0]), (W, [55, 0, 0]), (H, [15, 0, 0])])},
           "body": {"rotation": tr(L, [(0.4, [14, 0, 0]), (H, [4, 0, 0])])}}
    for i in range(3):
        out[f"silk{i}"] = {"rotation": wave(L, 6, 25, axis=0, phase=i)}
    for n in (3, 4):
        out[f"leg_r{n}_a"] = {"rotation": wave(L, 6, 14, axis=2, phase=n)}
        out[f"leg_l{n}_a"] = {"rotation": wave(L, 6, -14, axis=2, phase=n)}
    return out


def sig_rot_ent_roots(L):
    """Корни: Древень опускается на колено и вонзает руки-ветви в землю, сердце бьётся."""
    return {"right_arm": {"rotation": tr(L, [(W, [-150, 0, 20]), (H, [-20, 0, 15])])},
            "left_arm": {"rotation": tr(L, [(W, [-150, 0, -20]), (H, [-20, 0, -15])])},
            "right_forearm": {"rotation": tr(L, [(W, [-30, 0, 0]), (H, [10, 0, 0])])},
            "left_forearm": {"rotation": tr(L, [(W, [-30, 0, 0]), (H, [10, 0, 0])])},
            "body": {"rotation": tr(L, [(W, [-15, 0, 0]), (H, [45, 0, 0])])},
            "right_leg": {"rotation": tr(L, [(H, [-60, 0, 0])])}, "right_shin": {"rotation": tr(L, [(H, [70, 0, 0])])},
            "left_leg": {"rotation": tr(L, [(H, [20, 0, 0])])}, "left_shin": {"rotation": tr(L, [(H, [60, 0, 0])])},
            "root": {"position": tr(L, [(W, [0, 0.8, 0]), (H, [0, -3.0, 0])])},
            "heart": {"scale": tr(L, [(W, [1.3, 1.3, 1.3]), (H, [1.7, 1.7, 1.7])], rest=ONE)}}


def sig_morena_dance(L):
    """Ведьмин танец: Морена кружится с раскинутыми руками, плащ веером, глаз посоха пылает."""
    return {"root": {"rotation": kf([(0, Z), (round(L * 0.9, 3), [0, 540, 0]), (round(L, 3), [0, 540, 0])]),
                     "position": wave(L, 6, 0.6, axis=1)},
            "right_arm": {"rotation": tr(L, [(0.15, [-20, 0, 95]), (0.9, [-30, 0, 100])])},
            "left_arm": {"rotation": tr(L, [(0.15, [-20, 0, -95]), (0.9, [-30, 0, -100])])},
            "head": {"rotation": tr(L, [(0.15, [-25, 0, 10]), (0.9, [-25, 0, -10])])},
            "cloak": {"rotation": tr(L, [(0.15, [-50, 0, 0]), (0.9, [-60, 0, 0])])},
            "eye": {"scale": tr(L, [(0.15, [1.5, 1.5, 1.5]), (0.9, [2.0, 2.0, 2.0])], rest=ONE)}}


def sig_tide_wave(L):
    """Волна: Страж поднимает щит и трезубец к своду — и толкает воду вперёд."""
    return {"right_arm": {"rotation": tr(L, [(W, [-175, 0, 10]), (H, [-80, 0, 0])])},
            "left_arm": {"rotation": tr(L, [(W, [-175, 0, -10]), (H, [-85, 0, 0])])},
            "right_forearm": {"rotation": tr(L, [(W, [-20, 0, 0]), (H, [0, 0, 0])])},
            "left_forearm": {"rotation": tr(L, [(W, [-20, 0, 0]), (H, [0, 0, 0])])},
            "body": {"rotation": tr(L, [(W, [-20, 0, 0]), (H, [25, 0, 0])])},
            "head": {"rotation": tr(L, [(W, [-30, 0, 0]), (H, [5, 0, 0])])},
            "right_leg": {"rotation": tr(L, [(H, [-35, 0, 0])])}, "right_shin": {"rotation": tr(L, [(H, [30, 0, 0])])},
            "left_leg": {"rotation": tr(L, [(H, [25, 0, 0])])},
            "root": {"position": tr(L, [(W, [0, 1.0, 0]), (H, [0, -0.8, 0])])},
            "shield": {"rotation": tr(L, [(W, [0, -30, 0]), (H, [0, 0, 0])])}}


def sig_leviathan_roar(L):
    """Рёв Левиафана: тело встаёт свечой, пасть распахнута, ласты бьют."""
    out = {"head": {"rotation": tr(L, [(W, [-55, 0, 0]), (H, [-35, 0, 0])])},
           "jaw": {"rotation": tr(L, [(0.3, [20, 0, 0]), (W, [38, 0, 0]), (H, [48, 0, 0])])},
           "root": {"position": tr(L, [(W, [0, 3.0, 0]), (H, [0, 2.2, 0])])},
           "flipper_r": {"rotation": wave(L, 8, 45, axis=2)}, "flipper_l": {"rotation": wave(L, 8, -45, axis=2)}}
    for i in range(6):
        out[f"seg{i}"] = {"rotation": tr(L, [(W, [12 + i * 3, 0, 0]), (H, [6, 0, 0])])}
    return out


def sig_naiad_rings(L):
    """Жемчужные кольца: Наяра взмывает, нимб расходится вширь, руки разводят свет."""
    return {"root": {"position": tr(L, [(0.4, [0, 1.6, 0]), (W, [0, 2.2, 0]), (H, [0, 0.6, 0])])},
            "right_arm": {"rotation": tr(L, [(0.4, [-90, 0, 60]), (W, [-100, 0, 75]), (H, [-30, 0, 100])])},
            "left_arm": {"rotation": tr(L, [(0.4, [-90, 0, -60]), (W, [-100, 0, -75]), (H, [-30, 0, -100])])},
            "head": {"rotation": tr(L, [(0.4, [-20, 0, 0]), (H, [0, 0, 0])])},
            "halo": {"rotation": spin(L, 720, axis=1),
                     "scale": tr(L, [(0.4, [1.4, 1.4, 1.4]), (W, [1.8, 1.8, 1.8]), (H, [2.6, 2.6, 2.6])], rest=ONE)},
            "hair": {"rotation": wave(L, 4, 14, axis=0)}}


def sig_smelter_hammer(L):
    """Молот: голем заносит молот обеими руками над головой и бьёт так, что подгибаются ноги."""
    return {"left_arm": {"rotation": tr(L, [(W, [-155, 0, -10]), (H, [-30, 0, 0])])},
            "right_arm": {"rotation": tr(L, [(W, [-150, 0, 25]), (H, [-35, 0, 15])])},
            "left_forearm": {"rotation": tr(L, [(W, [-45, 0, 0]), (H, [0, 0, 0])])},
            "right_forearm": {"rotation": tr(L, [(W, [-45, 0, 0]), (H, [0, 0, 0])])},
            "body": {"rotation": tr(L, [(W, [-14, 0, 0]), (H, [40, 0, 0])])},
            "hammer": {"rotation": tr(L, [(W, [-40, 0, 0]), (H, [30, 0, 0])])},
            "right_leg": {"rotation": tr(L, [(H, [-38, 0, 10])])}, "left_leg": {"rotation": tr(L, [(H, [-38, 0, -10])])},
            "right_shin": {"rotation": tr(L, [(H, [55, 0, 0])])}, "left_shin": {"rotation": tr(L, [(H, [55, 0, 0])])},
            "root": {"position": tr(L, [(W, [0, 1.2, 0]), (H, [0, -2.2, 0])])}}


def sig_chariot_rear(L):
    """Конь встаёт на дыбы, колесница кренится, возница вскидывает руку."""
    return {"horse": {"rotation": tr(L, [(W, [-45, 0, 0]), (H, [10, 0, 0])])},
            "legs_f": {"rotation": tr(L, [(0.4, [-60, 0, 0]), (W, [-90, 0, 0]), (H, [20, 0, 0])])},
            "legs_b": {"rotation": tr(L, [(W, [20, 0, 0]), (H, [-10, 0, 0])])},
            "horse_head": {"rotation": tr(L, [(W, [-40, 0, 0]), (H, [25, 0, 0])])},
            "cart": {"rotation": tr(L, [(W, [8, 0, 6]), (H, [-4, 0, 0])])},
            "driver": {"rotation": tr(L, [(W, [-25, 0, 0]), (H, [25, 0, 0])])},
            "root": {"position": tr(L, [(H, [0, -0.5, 0])])},
            "wheel_r": {"rotation": spin(L, -720, axis=0)}, "wheel_l": {"rotation": spin(L, -720, axis=0)}}


def sig_ash_collapse(L):
    """Обвал: Владыка поднимает меч остриём вниз и вонзает его в пол, опускаясь на колено."""
    return {"right_arm": {"rotation": tr(L, [(W, [-175, 0, 0]), (H, [-60, 0, 0])])},
            "left_arm": {"rotation": tr(L, [(W, [-175, 0, 0]), (H, [-60, 0, 0])])},
            "right_forearm": {"rotation": tr(L, [(W, [-30, 0, 0]), (H, [-10, 0, 0])])},
            "left_forearm": {"rotation": tr(L, [(W, [-30, 0, 0]), (H, [-10, 0, 0])])},
            "sword": {"rotation": tr(L, [(W, [180, 0, 0]), (H, [180, 0, 0])])},
            "body": {"rotation": tr(L, [(W, [-15, 0, 0]), (H, [35, 0, 0])])},
            "head": {"rotation": tr(L, [(W, [-25, 0, 0]), (H, [15, 0, 0])])},
            "right_leg": {"rotation": tr(L, [(H, [-70, 0, 0])])}, "right_shin": {"rotation": tr(L, [(H, [75, 0, 0])])},
            "left_leg": {"rotation": tr(L, [(H, [20, 0, 0])])}, "left_shin": {"rotation": tr(L, [(H, [70, 0, 0])])},
            "root": {"position": tr(L, [(W, [0, 1.0, 0]), (H, [0, -3.2, 0])])},
            "cape": {"rotation": tr(L, [(W, [-25, 0, 0]), (H, [-45, 0, 0])])},
            "crown": {"scale": tr(L, [(W, [1.2, 1.2, 1.2]), (H, [1.4, 1.4, 1.4])], rest=ONE)}}


def sig_ash_crown(L):
    """Корона пепла: Владыка раскидывает руки, корона разгорается, плащ взлетает."""
    return {"right_arm": {"rotation": tr(L, [(0.4, [-30, 0, 85]), (H, [-60, 0, 110])])},
            "left_arm": {"rotation": tr(L, [(0.4, [-30, 0, -85]), (H, [-60, 0, -110])])},
            "head": {"rotation": tr(L, [(0.4, [-30, 0, 0]), (H, [-40, 0, 0])])},
            "body": {"rotation": tr(L, [(0.4, [-12, 0, 0]), (H, [-18, 0, 0])])},
            "crown": {"scale": tr(L, [(0.4, [1.3, 1.3, 1.3]), (H, [1.8, 1.8, 1.8])], rest=ONE)},
            "cape": {"rotation": tr(L, [(0.4, [-40, 0, 0]), (H, [-60, 0, 0])])},
            "root": {"position": tr(L, [(0.4, [0, 0.6, 0])])}}


def sig_doppel_mirror(L):
    """Зеркало: Двойник скрещивает клинки перед собой и замирает, корпус чуть мерцает."""
    flick = [(0.2 + 0.7 * i / 8, [1 + (0.04 if i % 2 else -0.03)] * 3) for i in range(9)]
    return {"right_arm": {"rotation": hold(L, 0.2, [-85, -35, 0])},
            "left_arm": {"rotation": hold(L, 0.2, [-85, 35, 0])},
            "right_forearm": {"rotation": hold(L, 0.2, [-30, 0, 0])},
            "left_forearm": {"rotation": hold(L, 0.2, [-30, 0, 0])},
            "blade_right": {"rotation": hold(L, 0.2, [0, 0, 40])},
            "blade_left": {"rotation": hold(L, 0.2, [0, 0, -40])},
            "head": {"rotation": hold(L, 0.2, [12, 0, 0])},
            "body": {"scale": tr(L, flick, rest=ONE)}}


def sig_chrono_rewind(L):
    """Откат: Хронофаг медленно оборачивается назад, шестерни и стрелки крутятся вспять, корпус мерцает."""
    flick = [(0.1 + 0.8 * i / 10, [1 + (0.05 if i % 2 else -0.04)] * 3) for i in range(11)]
    return {"root": {"rotation": kf([(0, Z), (round(L * H, 3), [0, -360, 0]), (round(L, 3), [0, -360, 0])]),
                     "position": tr(L, [(0.4, [0, 1.2, 0]), (W, [0, 1.6, 0]), (H, [0, 0, 0])])},
            "gears": {"rotation": spin(L, -1440, axis=2)},
            "hand_right": {"rotation": spin(L, 2160, axis=2)},
            "hand_left": {"rotation": spin(L, 720, axis=2)},
            "right_arm": {"rotation": tr(L, [(0.3, [-100, 0, 40]), (W, [-110, 0, 50]), (H, [-40, 0, 20])])},
            "left_arm": {"rotation": tr(L, [(0.3, [-100, 0, -40]), (W, [-110, 0, -50]), (H, [-40, 0, -20])])},
            "chest": {"scale": tr(L, flick, rest=ONE)}}


def sig_xal_slam(L):
    """Крах края: Ксалатар вздымает щупальца над головой и обрушивает их во все стороны, пасть распахнута."""
    out = {"right_arm": {"rotation": tr(L, [(W, [-170, 0, 40]), (H, [-40, 0, 70])])},
           "left_arm": {"rotation": tr(L, [(W, [-170, 0, -40]), (H, [-40, 0, -70])])},
           "body": {"rotation": tr(L, [(W, [-25, 0, 0]), (H, [30, 0, 0])])},
           "head": {"rotation": tr(L, [(W, [-35, 0, 0]), (H, [10, 0, 0])])},
           "jaw": {"rotation": tr(L, [(W, [30, 0, 0]), (H, [45, 0, 0])])},
           "root": {"position": tr(L, [(W, [0, 1.8, 0]), (H, [0, -1.5, 0])])},
           "rift": {"scale": tr(L, [(W, [1.5, 1.5, 1.5]), (H, [2.2, 2.2, 2.2])], rest=ONE), "rotation": spin(L, 540, axis=2)},
           "right_leg": {"rotation": tr(L, [(H, [-30, 0, 12])])}, "left_leg": {"rotation": tr(L, [(H, [-30, 0, -12])])},
           "right_shin": {"rotation": tr(L, [(H, [45, 0, 0])])}, "left_shin": {"rotation": tr(L, [(H, [45, 0, 0])])}}
    for t in range(4):
        side = 1 if t % 2 else -1
        for j in range(4):
            out[f"tent{t}_{j}"] = {"rotation": tr(L, [(W, [-30 - 10 * j, 0, side * 10]), (H, [35 + 8 * j, 0, side * (40 + 10 * j)])])}
    return out


def sig_xal_maw(L):
    """Пасть: Ксалатар наклоняется к цели, челюсть отвисает до груди, щупальца тянутся вперёд."""
    out = {"head": {"rotation": tr(L, [(0.4, [15, 0, 0]), (W, [20, 0, 0]), (H, [-10, 0, 0])])},
           "jaw": {"rotation": tr(L, [(0.4, [40, 0, 0]), (W, [55, 0, 0]), (H, [20, 0, 0])])},
           "body": {"rotation": tr(L, [(0.4, [18, 0, 0]), (H, [8, 0, 0])])},
           "right_arm": {"rotation": tr(L, [(0.4, [-70, 0, 25]), (W, [-80, 0, 35]), (H, [-95, 0, 5])])},
           "left_arm": {"rotation": tr(L, [(0.4, [-70, 0, -25]), (W, [-80, 0, -35]), (H, [-95, 0, -5])])},
           "right_forearm": {"rotation": tr(L, [(W, [-25, 0, 0]), (H, [0, 0, 0])])},
           "left_forearm": {"rotation": tr(L, [(W, [-25, 0, 0]), (H, [0, 0, 0])])},
           "rift": {"scale": tr(L, [(0.4, [1.3, 1.3, 1.3]), (H, [1.7, 1.7, 1.7])], rest=ONE)}}
    for t in range(4):
        side = 1 if t % 2 else -1
        for j in range(4):
            out[f"tent{t}_{j}"] = {"rotation": tr(L, [(0.4, [-40 + 6 * j, 0, side * 12]), (W, [-55 + 6 * j, 0, side * 8]),
                                                      (H, [-20, 0, side * 20])])}
    return out


SIGNATURE = {
    ("lich", "storm"): sig_lich_storm, ("lich", "blizzard"): sig_lich_storm, ("lich", "raise"): sig_lich_raise,
    ("gatekeeper", "rampart"): sig_gatekeeper_rampart, ("gatekeeper", "palisade"): sig_gatekeeper_rampart,
    ("morra", "harvest"): sig_morra_harvest,
    ("vessa", "candles"): sig_vessa_candles,
    ("arachna", "pounce"): sig_arachna_rear, ("arachna", "skitter"): sig_arachna_rear,
    ("arachna", "web"): sig_arachna_web, ("arachna", "net"): sig_arachna_web, ("arachna", "threads"): sig_arachna_web,
    ("rot_ent", "roots"): sig_rot_ent_roots, ("rot_ent", "rootgrid"): sig_rot_ent_roots, ("rot_ent", "grasp"): sig_rot_ent_roots,
    ("morena", "dance"): sig_morena_dance, ("morena", "witchcircle"): sig_morena_dance,
    ("tide_warden", "wave"): sig_tide_wave, ("tide_warden", "backlash"): sig_tide_wave,
    ("leviathan", "roar"): sig_leviathan_roar, ("leviathan", "flood"): sig_leviathan_roar,
    ("naiad", "pearlrings"): sig_naiad_rings, ("naiad", "radiance"): sig_naiad_rings,
    ("smelter", "hammer"): sig_smelter_hammer, ("smelter", "anvils"): sig_smelter_hammer,
    ("chariot", "ring"): sig_chariot_rear, ("chariot", "firesweep"): sig_chariot_rear,
    ("ash_lord", "collapse"): sig_ash_collapse, ("ash_lord", "crown"): sig_ash_crown, ("ash_lord", "erupt"): sig_ash_crown,
    ("doppel", "mirror"): sig_doppel_mirror, ("doppel", "mirrorpools"): sig_doppel_mirror,
    ("chronophage", "rewind"): sig_chrono_rewind, ("chronophage", "stasis"): sig_chrono_rewind,
    ("xalathar", "edge"): sig_xal_slam, ("xalathar", "edgefall"): sig_xal_slam, ("xalathar", "voidfall"): sig_xal_slam,
    ("xalathar", "maw"): sig_xal_maw, ("xalathar", "hunger"): sig_xal_maw,
}


def apply(name, anims, bones=None):
    """Дописать личные слои в анимации a_* хранителя. bones — имена костей модели (чужие кости отбрасываются)."""
    if name not in STYLES:
        return 0
    family, styles = STYLES[name]
    human = family.startswith("humanoid")
    left = family == "humanoid_left"
    n = 0
    for key, style in styles.items():
        an = anims.get(f"a_{key}")
        if an is None:
            continue
        L = an["animation_length"]
        sig = SIGNATURE.get((name, key))
        if sig:
            an["bones"] = {}                     # коронный приём — целиком свой
            put(an, sig(L))
        elif human:
            put(an, body_mech(style, L, left), override=False)
        put(an, flavor(name, style, L), override=False)
        if bones is not None:
            an["bones"] = {b: v for b, v in an["bones"].items() if b in bones}
        n += 1
    return n
