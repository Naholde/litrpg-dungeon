"""Можно ли уйти от удара: python tools/balance/dodge_table.py → по каждой разметке хранителей время и путь.
Норма: реакция 0,5 с + путь/5 блоков в секунду (бег) ≤ время заполнения разметки. «рывком» — хватает только с рывком
(5 блоков сразу). «НЕЛЬЗЯ» — не уйти даже с рывком."""
import glob
import os

ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'build', 'reports', 'bench')
REACT, SPEED, DASH = 0.5, 5.0, 5.0
bad = 0
for f in sorted(glob.glob(os.path.join(ROOT, 'boss_*.txt'))):
    lines = open(f, encoding='utf-8').read().splitlines()
    boss = lines[0].split('=')[1]
    names = {}
    seen = set()
    marks = []
    for l in lines[1:]:
        k, v = l.split('=', 1)
        if k == 'ability':
            key, name, ticks = v.split(';')
            names[key] = (name, int(ticks))
        elif k == 'mark':
            marks.append(v.split(';'))
        elif k == 'error':
            print(f'{boss}: ОШИБКА {v}')
    marked = set()
    for key, shape, size, ticks in marks:
        marked.add(key)
        size, t = float(size), int(ticks) / 20
        sig = (key, shape, size, t)
        if sig in seen:
            continue
        seen.add(sig)
        if shape == 'soak':
            verdict = 'встать вместе'
        elif shape == 'move':
            verdict = 'двигаться'
        elif shape == 'intercept':
            verdict = 'перехватить'
        else:
            need = REACT + size / SPEED
            need_dash = REACT + max(0.0, size - DASH) / SPEED
            verdict = 'ок' if t >= need else ('рывком' if t >= need_dash else 'НЕЛЬЗЯ')
            if verdict != 'ок':
                bad += 1
        print(f'{boss:12} {names.get(key, (key,))[0]:24} {shape:6} путь {size:4.1f} время {t:4.1f}с  {verdict}')
    for key, (name, ticks) in names.items():
        if key not in marked:
            print(f'{boss:12} {name:24} без разметки (подготовка {ticks / 20:.1f}с)')
print('не успеть бегом:', bad)
