"""
Анимации навыков хранителей: у каждого навыка — своя анимация «a_<ключ>» длиной в его подготовку (из BossRegistry),
плюс «phase» (переход фазы), «enrage» (неистовство) и «relocate» (уход во второй зал).

Стиль замаха — по смыслу навыка (STYLES), тело — по семейству модели (человек, паук, змей, колесница). У каждого
хранителя свои вариации (амплитуда, рука, наклон) — одинаковых движений у разных хранителей нет.
"""
import random
import re
import zlib
from pathlib import Path

REGISTRY = Path(__file__).resolve().parent.parent / "src/main/java/com/andrey/litrpg/boss/BossRegistry.java"

# хранитель (модель) → семейство и стиль каждого навыка
STYLES = {
    "trial_warden": ("humanoid", {"smash": "cleave", "wave": "spin", "meteor": "raise", "burden": "point", "mend": "channel", "salvo": "point"}),
    "gatekeeper": ("humanoid", {"chain": "cleave", "rampart": "spin", "call": "summon", "shield": "channel", "salvo": "point", "lunge": "charge"}),
    "morra": ("humanoid", {"reap": "cleave", "spears": "thrust", "bond": "point", "waves": "spin"}),
    "vessa": ("humanoid_left", {"mend": "channel", "curse": "point", "nova": "overhead", "creep": "point", "stare": "channel"}),
    "lich": ("humanoid", {"scythe": "cleave", "storm": "raise", "tomb": "point", "raise": "summon", "dark": "channel", "frost": "raise"}),
    "arachna": ("spider", {"bite": "bite", "web": "spin_web", "cocoon": "spin_web", "eggs": "lay", "skitter": "lay", "threads": "spin_web"}),
    "rot_ent": ("humanoid", {"slam": "overhead", "roots": "summon", "spores": "raise", "heart": "channel", "circles": "spin", "bind": "summon"}),
    "morena": ("humanoid_left", {"hex": "thrust", "fear": "point", "cauldron": "raise", "inside": "spin", "eye": "channel", "crawl": "point", "grove": "summon"}),
    "tide_warden": ("humanoid", {"trident": "thrust", "wave": "overhead", "whirl": "spin", "bubbles": "raise", "salvo": "point", "surge": "spin", "columns": "raise"}),
    "leviathan": ("serpent", {"bite": "bite", "tail": "tail", "dive": "dive", "ram": "dive", "flood": "tail", "wall": "tail", "deluge": "dive"}),
    "naiad": ("humanoid_left", {"staff": "cleave", "spears": "thrust", "song": "channel", "radiance": "raise", "chase": "point", "duet": "channel", "ray": "channel", "coral": "raise"}),
    "smelter": ("humanoid", {"hammer": "overhead", "pour": "thrust", "sparks": "thrust", "rivers": "overhead", "molten": "overhead", "castwall": "thrust"}),
    "chariot": ("chariot", {"charge": "charge", "ring": "rear", "wheels": "rear", "trample": "charge", "firesweep": "rear"}),
    "ash_lord": ("humanoid", {"sword": "cleave", "storm": "spin", "collapse": "overhead", "ego": "point", "erupt": "raise", "ashfall": "raise", "veins": "spin", "closing": "point"}),
    "doppel": ("humanoid", {"strike": "cleave", "mirror": "channel", "swap": "point", "link": "point", "copies": "channel", "places": "point", "glasswall": "channel"}),
    "chronophage": ("humanoid", {"slash": "cleave", "glass": "summon", "rewind": "raise", "waves": "raise", "clock": "summon", "closing": "raise", "hand": "summon"}),
    "xalathar": ("humanoid", {"smash": "overhead", "gaze": "point", "hunger": "channel", "edge": "spin", "voidfall": "overhead", "edgefall": "spin", "ray": "point", "pillars": "raise"}),
}


def cast_times():
    """{хранитель: {навык: секунды подготовки}} из BossRegistry.java."""
    src = REGISTRY.read_text(encoding="utf-8")
    out = {}
    for m in re.finditer(r'reg\("(\w+)",.*?p\((.*?)\)\);', src, re.S):
        out[m.group(1)] = {k: float(v) for k, v in re.findall(r'"(\w+)_cast",\s*([\d.]+)', m.group(2))}
    return out


def kf(pairs):
    return {f"{t:.3f}".rstrip("0").rstrip("."): list(v) for t, v in pairs}


def once(length, bones):
    return {"animation_length": round(length, 3), "bones": bones}


def three(length, rest, wind, hit):
    """Покой → замах (85% времени) → удар → покой."""
    return kf([(0, rest), (length * 0.85, wind), (length * 0.93, hit), (length, rest)])


def humanoid(style, L, v, left):
    a, b = ("left_arm", "right_arm") if left else ("right_arm", "left_arm")
    s = -1 if left else 1
    k = v["amp"]
    z = [0, 0, 0]
    if style == "cleave":
        return {a: {"rotation": three(L, z, [-70 * k, 60 * s * k, 35 * s], [-85 * k, -70 * s * k, -20 * s])},
                "body": {"rotation": three(L, z, [0, 35 * s * k, 0], [8, -30 * s * k, 0])},
                b: {"rotation": three(L, z, [20, 0, -15 * s], [-20, 0, -10 * s])}}
    if style == "overhead":
        return {"right_arm": {"rotation": three(L, z, [-170 * k, 0, 15], [-15, 0, 5])},
                "left_arm": {"rotation": three(L, z, [-170 * k, 0, -15], [-15, 0, -5])},
                "body": {"rotation": three(L, z, [-18 * k, 0, 0], [32 * k, 0, 0])},
                "root": {"position": three(L, z, [0, 1.5, 0], [0, -1, 0])}}
    if style == "thrust":
        return {a: {"rotation": three(L, z, [45 * k, 15 * s, 10 * s], [-95 * k, -5 * s, 0])},
                "body": {"rotation": three(L, z, [-12, 20 * s * k, 0], [18 * k, -10 * s, 0])},
                "root": {"position": three(L, z, [0, 0, 2], [0, 0, -3 * k])}}
    if style == "spin":
        return {"root": {"rotation": kf([(0, z), (L * 0.8, [0, -25 * k, 0]), (L * 0.97, [0, 360, 0]), (L, [0, 360, 0])])},
                "body": {"rotation": three(L, z, [22 * k, 0, 0], [-5, 0, 0])},
                "right_arm": {"rotation": three(L, z, [-40, 0, 70 * k], [-10, 0, 95])},
                "left_arm": {"rotation": three(L, z, [-40, 0, -70 * k], [-10, 0, -95])}}
    if style == "raise":
        return {"right_arm": {"rotation": kf([(0, z), (L * 0.4, [-150 * k, 0, 25]), (L * 0.8, [-165, 0, 35]), (L * 0.92, [-70, 0, 10]), (L, z)])},
                "left_arm": {"rotation": kf([(0, z), (L * 0.4, [-150 * k, 0, -25]), (L * 0.8, [-165, 0, -35]), (L * 0.92, [-70, 0, -10]), (L, z)])},
                "head": {"rotation": three(L, z, [-30 * k, 0, 0], [10, 0, 0])}}
    if style == "summon":
        return {"right_arm": {"rotation": three(L, z, [-20, 0, 70 * k], [-60, 0, 30])},
                "left_arm": {"rotation": three(L, z, [-20, 0, -70 * k], [-60, 0, -30])},
                "body": {"rotation": three(L, z, [28 * k, 0, 0], [-10, 0, 0])},
                "head": {"rotation": three(L, z, [30, 0, 0], [-25, 0, 0])}}
    if style == "channel":
        pulse = [(i * L / 6, [1 + 0.04 * k * (i % 2), 1 + 0.04 * k * (i % 2), 1 + 0.04 * k * (i % 2)]) for i in range(7)]
        return {"right_arm": {"rotation": kf([(0, z), (L * 0.2, [-70, 35, 0]), (L * 0.9, [-75, 40, 0]), (L, z)])},
                "left_arm": {"rotation": kf([(0, z), (L * 0.2, [-70, -35, 0]), (L * 0.9, [-75, -40, 0]), (L, z)])},
                "body": {"rotation": kf([(0, z), (L * 0.2, [12 * k, 0, 0]), (L * 0.9, [14 * k, 0, 0]), (L, z)]), "scale": kf(pulse)},
                "head": {"rotation": kf([(0, z), (L * 0.2, [20, 0, 0]), (L, z)])}}
    # point — указать на цель
    return {a: {"rotation": kf([(0, z), (L * 0.3, [-95 * k, -15 * s, 0]), (L * 0.85, [-100 * k, -10 * s, 0]), (L * 0.93, [-110, 0, 0]), (L, z)])},
            "head": {"rotation": kf([(0, z), (L * 0.3, [-5, 15 * s, 0]), (L, z)])},
            "body": {"rotation": kf([(0, z), (L * 0.3, [0, -15 * s * k, 0]), (L, z)])}}


def spider(style, L, v):
    z = [0, 0, 0]
    k = v["amp"]
    if style == "bite":
        return {"head": {"rotation": three(L, z, [-35 * k, 0, 0], [30, 0, 0])},
                "body": {"position": three(L, z, [0, 1, 2], [0, 0, -3])}}
    if style == "spin_web":
        return {"abdomen": {"rotation": three(L, z, [-45 * k, 0, 0], [10, 0, 0])},
                "body": {"rotation": three(L, z, [12, 0, 0], [-5, 0, 0])}}
    # lay — кладка
    return {"abdomen": {"scale": kf([(0, [1, 1, 1]), (L * 0.5, [1.2 * k, 0.9, 1.2 * k]), (L * 0.9, [0.9, 1.1, 0.9]), (L, [1, 1, 1])]),
                        "rotation": kf([(0, z), (L * 0.5, [25, 0, 0]), (L, z)])}}


def serpent(style, L, v):
    z = [0, 0, 0]
    k = v["amp"]
    if style == "bite":
        return {"head": {"rotation": three(L, z, [-40 * k, 0, 0], [28, 0, 0])}}
    if style == "tail":
        bones = {f"seg{i}": {"rotation": three(L, z, [0, 25 * k * (1 if i % 2 else -1), 0], [0, -40 * k, 0])} for i in range(6)}
        bones["root"] = {"rotation": kf([(0, z), (L * 0.85, [0, 20, 0]), (L * 0.97, [0, 360, 0]), (L, [0, 360, 0])])}
        return bones
    return {"head": {"rotation": kf([(0, z), (L * 0.6, [-50 * k, 0, 0]), (L, [40, 0, 0])])},
            "root": {"position": kf([(0, z), (L * 0.6, [0, 2, 0]), (L, [0, -20, 0])])}}


def chariot(style, L, v):
    z = [0, 0, 0]
    if style == "charge":
        return {"horse": {"rotation": three(L, z, [-30, 0, 0], [10, 0, 0])},
                "legs_f": {"rotation": three(L, z, [-70, 0, 0], [30, 0, 0])},
                "wheel_l": {"rotation": kf([(0, z), (L, [-720, 0, 0])])}, "wheel_r": {"rotation": kf([(0, z), (L, [-720, 0, 0])])}}
    return {"root": {"rotation": kf([(0, z), (L * 0.8, [0, -20, 0]), (L, [0, 360, 0])])},
            "horse": {"rotation": three(L, z, [-40, 0, 0], [0, 0, 0])}}


FAMILY = {"humanoid": lambda st, L, v: humanoid(st, L, v, False), "humanoid_left": lambda st, L, v: humanoid(st, L, v, True),
          "spider": spider, "serpent": serpent, "chariot": chariot}


def specials(family, v):
    z = [0, 0, 0]
    k = v["amp"]
    if family.startswith("humanoid"):
        phase = {"head": {"rotation": kf([(0, z), (0.4, [-35, 0, 0]), (1.2, [-35, 0, 0]), (1.6, z)])},
                 "right_arm": {"rotation": kf([(0, z), (0.4, [-30, 0, 85 * k]), (1.2, [-30, 0, 90]), (1.6, z)])},
                 "left_arm": {"rotation": kf([(0, z), (0.4, [-30, 0, -85 * k]), (1.2, [-30, 0, -90]), (1.6, z)])},
                 "body": {"rotation": kf([(0, z), (0.4, [-12, 0, 0]), (1.6, z)])}}
        relocate = {"right_arm": {"rotation": kf([(0, z), (1.0, [-170, 0, 20]), (3.0, [-170, 0, 20])])},
                    "left_arm": {"rotation": kf([(0, z), (1.0, [-170, 0, -20]), (3.0, [-170, 0, -20])])},
                    "root": {"scale": kf([(0, [1, 1, 1]), (2.4, [1, 1, 1]), (3.0, [0.2, 1.6, 0.2])])}}
    else:
        phase = {"root": {"rotation": kf([(0, z), (0.5, [-25 * k, 0, 0]), (1.2, [-25, 0, 0]), (1.6, z)])}}
        relocate = {"root": {"scale": kf([(0, [1, 1, 1]), (2.4, [1, 1, 1]), (3.0, [0.2, 0.2, 0.2])])}}
    shake = [(i * 0.08, [0, 0, (6 if i % 2 else -6) * k]) for i in range(13)]
    enrage = {"root": {"rotation": kf(shake)}}
    return {"phase": once(1.6, phase), "enrage": once(1.0, enrage), "relocate": once(3.0, relocate)}


def add(name, anims):
    """Добавить анимации навыков хранителя {@code name} (если он хранитель) в словарь анимаций модели."""
    if name not in STYLES:
        return
    family, styles = STYLES[name]
    casts = cast_times()
    reg = "morra" if name == "vessa" else name
    rnd = random.Random(zlib.crc32(name.encode()))
    v = {"amp": 0.85 + rnd.random() * 0.35}
    for key, style in styles.items():
        L = max(0.8, casts.get(name, casts.get(reg, {})).get(key, 1.5))
        anims[f"a_{key}"] = once(L, FAMILY[family](style, L, v))
    anims.update(specials(family, v))
