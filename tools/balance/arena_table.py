"""Арены под приёмы (обновление 14, пункт 10):  python tools/balance/arena_table.py [--md]

Читает отчёты стенда `gradlew runGameTestServer -Pbench` (build/reports/bench/boss_<id>.txt, строки reach= —
каждая разметка: на сколько блоков от центра арены легла и какого она размера) и для каждого хранителя считает:
  «вокруг себя» — самый большой удар с центром у хранителя (≤ 2,5 блока от него — хранитель на стенде ходит);
  «по цели»     — самая большая разметка под бойцом;
  «до края»     — самая дальняя точка, до которой достала разметка в бою (игроки стенда стоят в 2–5 блоках);
  «на весь зал» — приёмы, чья разметка накрывает арену целиком (радиус ≥ 0,85 арены: поле плит, вал-кольцо,
                  крах края) — их размер берётся от арены, размер арены они не задают.
Нужный радиус арены = max(вокруг себя + 6 — кольцо, где отряд стоит вне удара и куда отбегают рывком;
                          по цели × 2 + 3 — две метки на разных бойцах не накрывают всю арену; 12).
Вывод — таблица (Markdown с --md), её копия — в docs/UPDATE14.md.
"""
import glob
import math
import os
import sys

sys.stdout.reconfigure(encoding='utf-8')
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'build', 'reports', 'bench')
SELF = 2.5
RING = 6
MIN_R = 12


def load(path):
    out = {'boss': None, 'arena': 0.0, 'names': {}, 'reach': []}
    for line in open(path, encoding='utf-8').read().splitlines():
        if '=' not in line:
            continue
        k, v = line.split('=', 1)
        if k == 'boss':
            out['boss'] = v
        elif k == 'arena':
            out['arena'] = float(v)
        elif k == 'ability':
            key, name, _ = v.split(';')
            out['names'][key] = name
        elif k == 'reach':
            key, off, r, me = (v.split(';') + [None])[:4]
            off, r = float(off), float(r)
            out['reach'].append((key, off, r, off if me is None else float(me)))
    return out


def analyse(b):
    self_r, self_k, targ_r, targ_k, far = 0.0, '', 0.0, '', 0.0
    whole = set()
    for key, off, r, me in b['reach']:
        if r >= b['arena'] * 0.85:
            whole.add(key)
            continue
        far = max(far, off + r)
        if me <= SELF:
            if r > self_r:
                self_r, self_k = r, key
        elif r > targ_r:
            targ_r, targ_k = r, key
    need = max(self_r + RING, targ_r * 2 + 3, MIN_R)
    return self_r, self_k, targ_r, targ_k, far, math.ceil(need), sorted(whole)


def main():
    md = '--md' in sys.argv
    files = sorted(glob.glob(os.path.join(ROOT, 'boss_*.txt')))
    if not files:
        print('нет отчётов: сначала gradlew runGameTestServer -Pbench')
        sys.exit(1)
    rows = []
    for f in files:
        b = load(f)
        if not b['reach']:
            continue
        s, sk, t, tk, far, need, whole = analyse(b)
        whole = ', '.join(b['names'].get(k, k) for k in whole) or '—'
        have = b['arena']
        verdict = 'тесно' if have < need - 0.5 else ('просторно' if have > need + 6 else 'впору')
        rows.append((b['boss'], have, s, b['names'].get(sk, sk), t, b['names'].get(tk, tk), far, need, verdict, whole))
    if md:
        print('| Хранитель | Арена сейчас | Вокруг себя (приём) | По цели (приём) | До края | На весь зал | Нужно | Итог |')
        print('|---|---|---|---|---|---|---|---|')
        for r in rows:
            print(f'| {r[0]} | {r[1]:.0f} | {r[2]:.1f} ({r[3]}) | {r[4]:.1f} ({r[5]}) | {r[6]:.1f} | {r[9]} | {r[7]} | {r[8]} |')
    else:
        for r in rows:
            print(f'{r[0]:14} арена {r[1]:4.0f}  вокруг себя {r[2]:5.1f} ({r[3]})  по цели {r[4]:4.1f} ({r[5]})  '
                  f'до края {r[6]:5.1f}  нужно {r[7]:3}  — {r[8]}   [весь зал: {r[9]}]')


if __name__ == '__main__':
    main()
