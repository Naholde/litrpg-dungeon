"""
Генератор подземелья: строит преддверие и этапы, проверяет схему и проходимость, пишет
src/main/resources/litrpg_map/{map.bin,layout.json}. Если проверка не прошла — файлы не пишутся.

    python tools/gen_dungeon.py [--preview build/preview/map]

После изменения карты на сервере мир подземелья пересоздаётся сам (версия карты — хеш map.bin).
"""
import argparse
import json
import os
import sys
import time

sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'dungeon'))
import check  # noqa: E402
import draft  # noqa: E402
import kit  # noqa: E402
import lobby  # noqa: E402
import stage1  # noqa: E402

STAGES = {n: draft for n in range(1, 6)}  # этапы, у которых есть настоящая карта, заменяют черновик
STAGES[1] = stage1


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--preview', help='папка для картинок «вид сверху»')
    args = ap.parse_args()
    t0 = time.time()
    lm, lobby_json = lobby.build()
    maps = {'lobby': lm}
    stages = []
    for n, mod in STAGES.items():
        m, s = mod.build(n)
        maps[n] = m
        stages.append(s)
    layout = {'version': 1, 'lobby': lobby_json, 'stages': [s.json() for s in stages]}
    errors = check.check(maps, json.loads(json.dumps(layout)))
    if errors:
        print('Карта НЕ записана — ошибки:')
        for e in errors:
            print('  ✗', e)
        sys.exit(1)
    pal, secs = kit.write_map(list(maps.values()), os.path.join(kit.ROOT, 'map.bin'))
    kit.write_layout(lobby_json, stages, os.path.join(kit.ROOT, 'layout.json'))
    size = os.path.getsize(os.path.join(kit.ROOT, 'map.bin'))
    print(f'Карта: {len(maps)} частей, палитра {pal}, секций {secs}, {size // 1024} КБ, {time.time() - t0:.1f} с. Всё в порядке.')
    if args.preview:
        os.makedirs(args.preview, exist_ok=True)
        kit.preview(lm, os.path.join(args.preview, 'lobby.png'), (0, 0, 64, 64), scale=4)
        for s in stages:
            x1, _, z1, x2, _, z2 = s.box
            marks = [(p['at'][0], p['at'][2], (255, 60, 60)) for p in s.packs]
            marks += [(b['at'][0], b['at'][2], (255, 200, 0)) for b in s.bosses]
            marks += [(s.camp[0], s.camp[2], (60, 255, 60))]
            kit.preview(maps[s.n], os.path.join(args.preview, f'stage{s.n}.png'), (x1, z1, x2, z2), marks, scale=3)


if __name__ == '__main__':
    main()
