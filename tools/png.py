"""Минимальный PNG без PIL: холст RGBA, точки, заливки, запись в файл.

    from png import Canvas
    c = Canvas(16, 16)
    c.set(3, 4, (255, 0, 0, 255))
    c.save("out.png")
"""
import struct
import zlib
from pathlib import Path


class Canvas:
    def __init__(self, w, h, fill=(0, 0, 0, 0)):
        self.w, self.h = w, h
        self.px = [list(fill) for _ in range(w * h)]

    def get(self, x, y):
        return tuple(self.px[y * self.w + x])

    def set(self, x, y, rgba):
        if 0 <= x < self.w and 0 <= y < self.h:
            if len(rgba) == 3:
                rgba = (*rgba, 255)
            self.px[y * self.w + x] = list(rgba)

    def blend(self, x, y, rgba):
        """Наложить полупрозрачный цвет поверх."""
        if not (0 <= x < self.w and 0 <= y < self.h):
            return
        r, g, b, a = rgba
        dst = self.px[y * self.w + x]
        fa = a / 255
        out_a = fa + dst[3] / 255 * (1 - fa)
        if out_a <= 0:
            return
        for i, c in enumerate((r, g, b)):
            dst[i] = round((c * fa + dst[i] * dst[3] / 255 * (1 - fa)) / out_a)
        dst[3] = round(out_a * 255)

    def rect(self, x0, y0, x1, y1, rgba):
        """Заливка [x0, x1) × [y0, y1)."""
        for y in range(y0, y1):
            for x in range(x0, x1):
                self.set(x, y, rgba)

    def save(self, path):
        raw = b"".join(b"\x00" + bytes(c for p in self.px[y * self.w:(y + 1) * self.w] for c in p)
                       for y in range(self.h))

        def chunk(kind, data):
            return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

        png = (b"\x89PNG\r\n\x1a\n"
               + chunk(b"IHDR", struct.pack(">IIBBBBB", self.w, self.h, 8, 6, 0, 0, 0))
               + chunk(b"IDAT", zlib.compress(raw, 9))
               + chunk(b"IEND", b""))
        Path(path).parent.mkdir(parents=True, exist_ok=True)
        Path(path).write_bytes(png)

    def scaled(self, k):
        """Увеличенная копия (для просмотра через Read)."""
        out = Canvas(self.w * k, self.h * k)
        for y in range(self.h):
            for x in range(self.w):
                out.rect(x * k, y * k, x * k + k, y * k + k, self.get(x, y))
        return out


def shade(rgb, k):
    """Темнее (k < 1) или светлее (k > 1)."""
    return tuple(max(0, min(255, round(c * k))) for c in rgb[:3]) + ((rgb[3],) if len(rgb) == 4 else (255,))


def hexrgb(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)
