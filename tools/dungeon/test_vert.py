"""
Проверка набора вертикали на полигоне (`proving.py`):  python -I tools/dungeon/test_vert.py

1. Полигон проходится целиком (строгая проверка: без западней и утоплений), и каждая часть достижима.
2. Без лестницы Галерея и Зал разлома недостижимы — проверка не «пролетает» сквозь стены.
3. Правила прыжка: разрыв 4 не перепрыгнуть, вверх — только на блок.
4. Яма без выхода — западня; вода, которой есть куда течь, — ошибка сразу при постройке.
5. Обрывы без перил: лоза или лестница в одной клетке шахты 2×2 (с разбега — мимо, 28 блоков вниз) — ошибка;
   на забор и стенку (перила) не встать.
"""
import os
import sys

sys.stdout.reconfigure(encoding='utf-8')
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))

import check  # noqa: E402
import kit  # noqa: E402
import proving  # noqa: E402

FAIL = []


def ok(cond, msg):
    print(('  ✓ ' if cond else '  ✗ ') + msg)
    if not cond:
        FAIL.append(msg)


def run(m, s, strict=True):
    errors, warns = [], []
    st = s.json()
    check.check_stage(m, st, set(), set(), errors, strict, warns)
    return errors, warns


def reached_set(m, s):
    st = s.json()
    w = check.Walker(m, set(), st['box'][1])
    camp = tuple(int(v // 1) for v in st['camp'][:3])
    return w, w.reach(camp, st['box'])


def main():
    print('Полигон вертикали:')
    m, s, marks = proving.build()
    errors, warns = run(m, s)
    ok(not errors, 'проходится строго: ' + ('; '.join(errors) if errors else 'без ошибок'))
    w, reached = reached_set(m, s)
    print(f'    достижимо клеток: {len(reached)}')
    for name, at in marks.items():
        if len(at) == 2:
            hit = any(c[0] == at[0] and c[2] == at[1] and c[1] >= 80 for c in reached)
        else:
            hit = check.near(reached, at, r=1, dy=1)
        ok(hit, f'достижимо: {name} {at}')
    # пузырьковый подъём: в колонне можно только вверх
    col = [c for c in reached if c[0] == proving.X(91) and c[2] == 26]
    ok(len(col) >= 14, f'подъём пузырями: {len(col)} клеток колонны')
    ok(all((c[0], c[1] - 1, c[2]) not in w.moves(*c) for c in col), 'пузыри не пускают вниз')
    # с моста на площадку и паркур — только прыжками (есть рёбра длиной > 1)
    jumps = [(c, n) for c in reached if c[1] >= 88 for n in w.moves(*c) if abs(n[0] - c[0]) + abs(n[2] - c[2]) > 1]
    ok(len(jumps) > 0, f'прыжки через разрывы: {len(jumps)}')

    print('Без лестницы:')
    m2, s2, marks2 = proving.build(ladder=False)
    errors, _ = run(m2, s2)
    ok(any('разлом недостижим' in e for e in errors), 'разлом недостижим без лестницы')
    _, reached2 = reached_set(m2, s2)
    ok(not check.near(reached2, marks2['gallery'], r=1, dy=1), 'Галерея недостижима без лестницы')

    print('Прыжки:')
    ok(kit.jump_ok((0, 64, 0), (4, 64, 0)), 'разрыв 3 на одном уровне — да')
    ok(not kit.jump_ok((0, 64, 0), (5, 64, 0)), 'разрыв 4 — нет')
    ok(kit.jump_ok((0, 64, 0), (3, 65, 0)), 'разрыв 2 вверх на блок — да')
    ok(not kit.jump_ok((0, 64, 0), (4, 65, 0)), 'разрыв 3 вверх — нет')
    ok(not kit.jump_ok((0, 64, 0), (1, 66, 0)), 'на два блока вверх — нет')
    try:
        kit.Map().parkour([(0, 64, 0), (5, 64, 0)], 'stone')
        ok(False, 'паркур с разрывом 4 строится')
    except ValueError:
        ok(True, 'паркур с разрывом 4 — ошибка при постройке')

    print('Западня и вода:')
    m3, s3, _ = proving.build()
    t = m3.themes['proving']
    # яма 2×2 глубиной 3 в лагере — спрыгнуть можно, выбраться нельзя
    hole = (proving.X(9), 30)
    for x in (hole[0], hole[0] + 1):
        for z in (hole[1], hole[1] + 1):
            for y in (proving.G0 - 3, proving.G0 - 2, proving.G0 - 1):
                m3.blocks.pop((x, y, z), None)
            m3.set(x, proving.G0 - 4, z, 'stone')
    errors, _ = run(m3, s3)
    ok(any('западня' in e for e in errors), 'яма без выхода найдена: ' + '; '.join(e for e in errors if 'западня' in e)[:120])
    leak = kit.Map()
    leak.carve(0, 64, 0, 3, 66, 3, t)
    leak.themes = m3.themes
    leak.pool(1, 1, 2, 2, 64, 1, t)
    leak.blocks[(0, 64, 1)] = kit.AIR
    try:
        leak.shell(2)
        leak.blocks.pop((3, 64, 1), None)
        leak.check_water([(1, 64, 1), (2, 64, 1), (1, 64, 2), (2, 64, 2)])
        ok(False, 'текущая вода не замечена')
    except ValueError as e:
        ok(True, f'вода, которой есть куда течь, — ошибка ({e})')

    print('Обрывы без перил:')
    m4, s4, _ = proving.build(narrow=True)
    errors, _ = run(m4, s4)
    ok(any('обрыв без перил' in e for e in errors), 'строгая проверка не пропускает обрывы')
    w4, r4 = reached_set(m4, s4)
    cl = check.drops(w4, r4)
    X = proving.X
    ok(any(X(107) <= to[0] <= X(108) and h >= 20 for h, _, to in cl), 'лоза в одной клетке шахты 2×2 — обрыв найден')
    ok(any(X(99) <= to[0] <= X(100) and h >= 10 for h, _, to in cl), 'лестница в одной клетке шахты 2×2 — обрыв найден')
    ok(not check.drops(*reached_set(m, s)), 'на полигоне обрывов нет (лоза и лестница во всю шахту, рукав, перила)')
    t = m4.themes['proving']
    fm = kit.Map()
    fm.themes = m4.themes
    fm.carve(0, 64, 0, 4, 66, 0, t)
    fm.later(lambda: [fm.set(x, 63, 0, 'stone') for x in range(5)] + [fm.set(2, 64, 0, 'oak_fence')])
    fm.shell(2)
    fw = check.Walker(fm, set())
    ok(not fw.stand(2, 65, 0), 'на забор не встать')
    ok((2, 65, 0) not in fw.moves(1, 64, 0) and (3, 64, 0) not in fw.reach((0, 64, 0), (-9, 60, -9, 9, 70, 9)),
       'забор — преграда, не ступенька')

    if FAIL:
        print(f'НЕ ПРОШЛО: {len(FAIL)}')
        sys.exit(1)
    print('Всё в порядке.')


if __name__ == '__main__':
    main()
