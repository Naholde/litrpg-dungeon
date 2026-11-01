"""Сводка стенда классов: python tools/balance/bench_table.py [--casts] → таблица в «единицах» (unit = здоровье бойца уровня)."""
import glob
import os
import sys

ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'build', 'reports', 'bench')
rows = []
for f in glob.glob(os.path.join(ROOT, '*.txt')):
    if os.path.basename(f).startswith('boss_'):
        continue
    rows.append(dict(l.split('=', 1) for l in open(f, encoding='utf-8').read().splitlines()))
for lv in sorted({r['level'] for r in rows}, key=int):
    print('== уровень', lv)
    for role in ('TANK', 'DAMAGE', 'SUPPORT'):
        rs = sorted([r for r in rows if r['level'] == lv and r['role'] == role], key=lambda r: -float(r['dps_one']))
        for r in rs:
            u = float(r['unit'])
            line = (f"{role[:3]} {r['class']:14} цель {float(r['dps_one']) / u:5.2f} все {float(r['dps_all']) / u:5.2f} "
                    f"леч {float(r['hps']) / u:5.2f} щит {float(r['sps']) / u:5.2f} танк получает {float(r['taken']):.2f}")
            if '--casts' in sys.argv:
                line += '  ' + r['casts']
            print(line)
