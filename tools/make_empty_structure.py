"""Пишет шаблон GameTest data/litrpg/structure/empty.nbt — площадка 1×1×1 с воздухом.

Запуск: python tools/make_empty_structure.py
"""
import gzip
import struct
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/data/litrpg/structure/empty.nbt"
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


root = b"".join([
    tag(INT, "DataVersion", struct.pack(">i", DATA_VERSION)),
    tag(LIST, "size", int_list([1, 1, 1])),
    tag(LIST, "palette", compound_list([tag(STRING, "Name", name("minecraft:air"))])),
    tag(LIST, "blocks", compound_list([
        tag(LIST, "pos", int_list([0, 0, 0])) + tag(INT, "state", struct.pack(">i", 0)),
    ])),
    tag(LIST, "entities", compound_list([])),
])
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_bytes(gzip.compress(tag(COMPOUND, "", root + bytes([END]))))
print("written", OUT)
