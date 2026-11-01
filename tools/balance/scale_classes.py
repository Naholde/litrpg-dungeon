"""Уравнивание классов по стенду (фаза 12).

    python tools/balance/scale_classes.py [--dry]

Берёт замеры build/reports/bench/<класс>_45.txt и умножает параметры навыков класса в коде:
бойцам — урон (damage), лечащим — лечение и щиты (heal, shield). Цель в «единицах» (здоровье бойца уровня):
боец — 0,36 по одной цели и не больше 1,15 по толпе из пяти; лекарь — 0,42 лечения в секунду.
Множитель ограничен 0,6…1,6 за проход — после правки стенд перезапускают и смотрят снова.
"""
import glob
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(__file__), '..', '..')
BENCH = os.path.join(ROOT, 'build', 'reports', 'bench')
SKILLS = os.path.join(ROOT, 'src', 'main', 'java', 'com', 'andrey', 'litrpg', 'skill', 'classes')
ONE, ALL, HEAL = 0.36, 1.15, 0.42
# Поддержка с ролью «усилитель» (знамёна, время): лечит меньше, остальное — в баффах.
HEAL_TARGET = {'bannerman': 0.26}
# Хронолог лечит «откатом» настоящего урона — стенд его не меряет, правится руками.
SKIP = {'chronolog'}
LO, HI = 0.6, 1.6
DRY = '--dry' in sys.argv

SRC = {f: open(f, encoding='utf-8').read() for f in glob.glob(os.path.join(SKILLS, '*.java'))}


def scale_param(sid, kinds, f):
    for path, s in SRC.items():
        m = re.search(r'super\("' + re.escape(sid) + r'",', s)
        if not m:
            continue
        end = s.find('public String template', m.end())
        body = s[m.end():end]

        new = re.sub(r'\b(' + kinds + r')\("(\w+)", ([\d.]+)\)', lambda pm: f'{pm.group(1)}("{pm.group(2)}", '
                     + f'{round(float(pm.group(3)) * f, 3):g})', body)
        SRC[path] = s[:m.end()] + new + s[end:]
        return
    raise SystemExit('нет навыка ' + sid)


for f in sorted(glob.glob(os.path.join(BENCH, '*_45.txt'))):
    d = dict(l.split('=', 1) for l in open(f, encoding='utf-8').read().splitlines())
    if d['class'].startswith('kit_') or d['class'] in SKIP:
        continue
    u = float(d['unit'])
    one, al, hps = float(d['dps_one']) / u, float(d['dps_all']) / u, (float(d['hps']) + float(d['sps'])) / u
    ids = [c.split(':')[0] for c in d['casts'].split()]
    ids = [i for i in ids if not i.split('.')[0] in ('tank', 'dps', 'sup')]
    if d['role'] == 'DAMAGE':
        k = min(ONE / one, ALL / al)
        kinds = 'damage'
    elif d['role'] == 'SUPPORT':
        k = HEAL_TARGET.get(d['class'], HEAL) / max(hps, 0.01)
        kinds = 'heal|shield'
    else:
        continue
    k = max(LO, min(HI, k))
    if abs(k - 1) < 0.06:
        print(f'{d["class"]:14} в пределах ({one:.2f}/{al:.2f}/{hps:.2f})')
        continue
    print(f'{d["class"]:14} x{k:.2f}  (цель {one:.2f}, толпа {al:.2f}, лечение {hps:.2f})')
    for sid in ids:
        scale_param(sid, kinds, k)

if not DRY:
    for path, s in SRC.items():
        open(path, 'w', encoding='utf-8', newline='').write(s)
