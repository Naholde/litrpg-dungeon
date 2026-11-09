"""Вырезать и увеличить часть снимка: python tools/dev/crop.py in.png x0 y0 x1 y1 [k] out.png"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from model_preview import read_png  # noqa: E402
from png import Canvas  # noqa: E402

src, x0, y0, x1, y1 = sys.argv[1], *map(int, sys.argv[2:6])
k = int(sys.argv[6]) if len(sys.argv) > 7 else 2
out = sys.argv[-1]
w, h, px = read_png(src)
c = Canvas((x1 - x0) * k, (y1 - y0) * k)
for y in range(y0, y1):
    for x in range(x0, x1):
        col = px[y * w + x]
        c.rect((x - x0) * k, (y - y0) * k, (x - x0 + 1) * k, (y - y0 + 1) * k, col)
c.save(out)
print(out)
