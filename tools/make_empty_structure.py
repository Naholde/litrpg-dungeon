"""Шаблоны GameTest: empty.nbt — 1×1×1 воздух; arena.nbt — 9×6×9, каменный пол и воздух
(тестовый сервер обносит площадку барьером — для проверок «по взгляду» нужна просторная арена).

Запуск: python tools/make_empty_structure.py
"""
import gzip
import struct
from pathlib import Path

DIR = Path(__file__).resolve().parent.parent / "src/main/resources/data/litrpg/structure"
DATA_VERSION = 3955  # Minecraft 1.21.1

END, INT, LIST, COMPOUND, STRING = 0, 3, 9, 10, 8


def name(n):
    b = n.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def tag(t, n, payload):
    return bytes([t]) + name(n) + payload


def int_list(values):
    return bytes([INT]) + struct.pack(">i", len(values)) + b"".join(struct.pack(">i", v) for v in values)


def compound_list(items):
    if not items:
        return bytes([END]) + struct.pack(">i", 0)
    return bytes([COMPOUND]) + struct.pack(">i", len(items)) + b"".join(i + bytes([END]) for i in items)


def structure(name, size, palette, blocks):
    root = b"".join([
        tag(INT, "DataVersion", struct.pack(">i", DATA_VERSION)),
        tag(LIST, "size", int_list(list(size))),
        tag(LIST, "palette", compound_list([tag(STRING, "Name", name_(b)) for b in palette])),
        tag(LIST, "blocks", compound_list([
            tag(LIST, "pos", int_list(list(pos))) + tag(INT, "state", struct.pack(">i", st)) for pos, st in blocks
        ])),
        tag(LIST, "entities", compound_list([])),
    ])
    out = DIR / f"{name}.nbt"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(gzip.compress(tag(COMPOUND, "", root + bytes([END]))))
    print("written", out)


def name_(block):
    return name(block)


structure("empty", (1, 1, 1), ["minecraft:air"], [((0, 0, 0), 0)])
W, H = 9, 6
structure("arena", (W, H, W), ["minecraft:air", "minecraft:smooth_stone"],
          [((x, y, z), 1 if y == 0 else 0) for x in range(W) for y in range(H) for z in range(W)])
