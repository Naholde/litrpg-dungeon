package com.andrey.litrpg.anim;

import com.andrey.litrpg.anim.Anim.Ease;

import static com.andrey.litrpg.anim.Anim.Part.ARM_L;
import static com.andrey.litrpg.anim.Anim.Part.ARM_R;
import static com.andrey.litrpg.anim.Anim.Part.BODY;
import static com.andrey.litrpg.anim.Anim.Part.HEAD;
import static com.andrey.litrpg.anim.Anim.Part.LEG_L;
import static com.andrey.litrpg.anim.Anim.Part.LEG_R;
import static com.andrey.litrpg.anim.Anim.Part.ROOT;

/** Движения бойцов ближнего боя: Берсерк, Стриж, Монах, Тень, Жнец, Гарпунёр, Палач. */
final class AnimsMelee {
    private AnimsMelee() {
    }

    static void register() {
        berserker();
        swift();
        monk();
        shadow();
        reaper();
        harpooner();
        headsman();
    }

    // ------------------------------------------------------------------ Берсерк

    private static void berserker() {
        var cleave = Anim.of("berserker.cleave", 15).fade(2);
        Moves.sweep(cleave, ARM_R, 0, 15);
        Anims.add(cleave);

        // Рваная рана: два коротких рывка клинком на себя — мясо рвётся.
        var rend = Anim.of("berserker.rend", 14).fade(2);
        rend.part(ARM_R).k(0, 0, 0, 0).k(3, -95, 40, 30, Ease.SMOOTH).k(5, -70, -30, 10, Ease.SNAP)
                .k(8, -100, 45, 34, Ease.SNAP).k(10, -62, -35, 6, Ease.SNAP).k(14, 0, 0, 0, Ease.SMOOTH);
        rend.part(BODY).k(0, 0, 0, 0).k(5, 6, -14, 0, Ease.SNAP).k(10, 8, -16, 0, Ease.SNAP).k(14, 0, 0, 0);
        Anims.add(rend);

        var leap = Anim.of("berserker.leap", 20).fade(2);
        Moves.leap(leap, 0, 20);
        Anims.add(leap);

        var frenzy = Anim.of("berserker.frenzy", 22).fade(3);
        Moves.roar(frenzy, 0, 22);
        Anims.add(frenzy);

        // Добить: шаг вперёд и короткий тяжёлый удар сверху.
        var execute = Anim.of("berserker.execute", 16).fade(2);
        Moves.chop(execute, ARM_R, 0, 14);
        execute.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(8, 0, 0, 0, 0, 0, 0.3, Ease.SNAP)
                .key(16, 0, 0, 0, 0, 0, 0.15, Ease.SMOOTH);
        Anims.add(execute);

        // Кровавая баня: вся ярость в один замах — трясёт, потом удар обеими руками.
        var bath = Anim.of("berserker.bloodbath", 30).fade(3);
        Moves.tremble(bath, 0, 12);
        Moves.slam(bath, 12, 18);
        bath.part(HEAD).k(0, 0, 0, 0).k(8, -20, 0, 0, Ease.SHAKE).k(21, 18, 0, 0, Ease.SNAP).k(30, 0, 0, 0);
        Anims.add(bath);
    }

    // ------------------------------------------------------------------ Стриж

    private static void swift() {
        // Порыв: проскочить сквозь врага — выпад и уход за спину.
        var rush = Anim.of("swift.rush", 14).fade(1);
        Moves.thrust(rush, ARM_R, 0, 10);
        rush.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(6, 10, 0, 0, 0, 0, 0.35, Ease.SNAP)
                .key(14, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(rush);

        // Веер клинков: три удара подряд, руки по очереди.
        var fan = Anim.of("swift.fan", 20).fade(2);
        Moves.sweep(fan, ARM_R, 0, 7);
        Moves.sweep(fan, ARM_L, 7, 6);
        Moves.sweep(fan, ARM_R, 13, 7);
        Anims.add(fan);

        // Пируэт: оборот на одной ноге, клинки в стороны.
        var pirouette = Anim.of("swift.pirouette", 18).fade(2);
        Moves.spin(pirouette, 0, 18, 2);
        pirouette.part(LEG_R).k(0, 0, 0, 0).k(3, -40, 0, 22, Ease.SNAP).k(15, -38, 0, 20).k(18, 0, 0, 0, Ease.SMOOTH);
        pirouette.part(ROOT).key(3, 0, 0, 0, 0, 0.1, 0, Ease.SNAP).key(15, 0, 720, 0, 0, 0.1, 0, Ease.LINEAR);
        Anims.add(pirouette);

        // Танец клинков: лёгкая стойка, перенос веса с ноги на ногу.
        var dance = Anim.of("swift.dance", 26).fade(3);
        dance.part(ARM_R).k(0, 0, 0, 0).k(5, -58, -36, 44, Ease.SMOOTH).k(13, -48, -26, 36, Ease.SMOOTH)
                .k(21, -58, -36, 44, Ease.SMOOTH).k(26, 0, 0, 0, Ease.SMOOTH);
        dance.part(ARM_L).k(0, 0, 0, 0).k(5, -48, 26, -36, Ease.SMOOTH).k(13, -58, 36, -44, Ease.SMOOTH)
                .k(21, -48, 26, -36, Ease.SMOOTH).k(26, 0, 0, 0, Ease.SMOOTH);
        dance.part(BODY).k(0, 0, 0, 0).k(5, 0, 14, 0).k(13, 0, -14, 0).k(21, 0, 14, 0).k(26, 0, 0, 0);
        dance.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(5, 0, 0, 5, 0.06, 0.04, 0, Ease.SMOOTH)
                .key(13, 0, 0, -5, -0.06, 0.04, 0, Ease.SMOOTH).key(21, 0, 0, 5, 0.06, 0.04, 0, Ease.SMOOTH)
                .key(26, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(dance);

        // Сквозняк: весь разгон в один проход по прямой.
        var draft = Anim.of("swift.draft", 16).fade(1);
        Moves.dash(draft, 0, 16);
        draft.part(ARM_R).k(0, 0, 0, 0).k(3, -88, -20, 76, Ease.SNAP).k(11, -85, -18, 74).k(16, 0, 0, 0, Ease.SMOOTH);
        draft.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(4, 14, 0, 0, 0, -0.08, 0.5, Ease.SNAP)
                .key(11, 12, 0, 0, 0, -0.08, 0.5, Ease.LINEAR).key(16, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(draft);

        // Последний взмах: длинный замах — и один росчерк.
        var last = Anim.of("swift.last_swing", 24).fade(2);
        last.part(ARM_R).k(0, 0, 0, 0).k(10, -40, 110, 85, Ease.SMOOTH).k(13, -38, 108, 84)
                .k(16, -95, -95, -40, Ease.SNAP).k(24, 0, 0, 0, Ease.SMOOTH);
        last.part(ARM_L).k(0, 0, 0, 0).k(10, -30, -40, -40, Ease.SMOOTH).k(16, -20, 30, -20, Ease.SNAP)
                .k(24, 0, 0, 0, Ease.SMOOTH);
        last.part(BODY).k(0, 0, 0, 0).k(10, 0, 34, 0, Ease.SMOOTH).k(16, 6, -30, 0, Ease.SNAP).k(24, 0, 0, 0);
        last.part(HEAD).k(0, 0, 0, 0).k(10, 0, -20, 0).k(16, 0, 16, 0, Ease.SNAP).k(24, 0, 0, 0);
        last.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(10, 0, 0, 0, 0, -0.06, -0.12, Ease.SMOOTH)
                .key(16, 0, 0, 0, 0, 0, 0.2, Ease.SNAP).key(24, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(last);
    }

    // ------------------------------------------------------------------ Монах

    private static void monk() {
        // Печать удара: ладонь в цель и печать под ноги.
        var seal = Anim.of("monk.seal_strike", 16).fade(2);
        Moves.palm(seal, ARM_R, 0, 11);
        Moves.toGround(seal, ARM_L, 8, 8);
        Anims.add(seal);

        // Печать связи: обе руки чертят знак, между печатями натягивается нить.
        var bind = Anim.of("monk.seal_bind", 20).fade(2);
        Moves.sign(bind, ARM_R, 0, 14);
        bind.part(ARM_L).k(0, 0, 0, 0).k(5, -100, -30, -34, Ease.SMOOTH).k(11, -80, 20, -48, Ease.SMOOTH)
                .k(15, -96, -10, -14, Ease.SNAP).k(20, 0, 0, 0, Ease.SMOOTH);
        bind.part(BODY).k(0, 0, 0, 0).k(11, 0, 10, 0).k(15, 6, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        Anims.add(bind);

        // Круг сомкнулся: ладони вместе — и развести, фигура вспыхивает.
        var circle = Anim.of("monk.circle", 22).fade(2);
        circle.part(ARM_R).k(0, 0, 0, 0).k(6, -112, 55, 14, Ease.SMOOTH).k(10, -112, 55, 14)
                .k(14, -96, -34, 66, Ease.SNAP).k(22, 0, 0, 0, Ease.SMOOTH);
        circle.part(ARM_L).k(0, 0, 0, 0).k(6, -112, -55, -14, Ease.SMOOTH).k(10, -112, -55, -14)
                .k(14, -96, 34, -66, Ease.SNAP).k(22, 0, 0, 0, Ease.SMOOTH);
        circle.part(BODY).k(0, 0, 0, 0).k(6, 10, 0, 0).k(14, -14, 0, 0, Ease.SNAP).k(22, 0, 0, 0);
        circle.part(HEAD).k(0, 0, 0, 0).k(6, 12, 0, 0).k(14, -16, 0, 0, Ease.SNAP).k(22, 0, 0, 0);
        circle.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(14, 0, 0, 0, 0, 0.1, 0, Ease.SNAP)
                .key(22, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(circle);

        var palm = Anim.of("monk.palm", 14).fade(2);
        Moves.palm(palm, ARM_R, 0, 14);
        Anims.add(palm);

        // Сто ладоней: шесть ладоней подряд.
        var hundred = Anim.of("monk.hundred", 26).fade(2);
        for (int i = 0; i < 6; i++) Moves.palm(hundred, i % 2 == 0 ? ARM_R : ARM_L, i * 4, 5);
        hundred.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(12, 0, 0, 0, 0, 0, 0.1, Ease.SMOOTH)
                .key(26, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(hundred);

        // Гора падает: прыжок и удар с небес.
        var mountain = Anim.of("monk.mountain", 32).fade(3);
        Moves.leap(mountain, 0, 16);
        Moves.slam(mountain, 16, 16);
        Anims.add(mountain);
    }

    // ------------------------------------------------------------------ Тень

    private static void shadow() {
        // Из темноты: присесть, пропасть — и удар из-за спины.
        var dark = Anim.of("shadow.from_dark", 18).fade(2);
        Moves.sink(dark, 0, 7);
        Moves.thrust(dark, ARM_R, 8, 10);
        Anims.add(dark);

        // Веер ножей: кисть расходится веером перед собой.
        var knives = Anim.of("shadow.knives", 14).fade(1);
        knives.part(ARM_R).k(0, 0, 0, 0).k(4, -60, 80, 40, Ease.SMOOTH).k(7, -95, 10, 6, Ease.SNAP)
                .k(9, -90, -40, -10, Ease.SNAP).k(14, 0, 0, 0, Ease.SMOOTH);
        knives.part(ARM_L).k(0, 0, 0, 0).k(4, -45, -30, -26, Ease.SMOOTH).k(9, -30, -10, -12, Ease.SNAP)
                .k(14, 0, 0, 0, Ease.SMOOTH);
        knives.part(BODY).k(0, 0, 0, 0).k(4, 0, 22, 0).k(9, 8, -18, 0, Ease.SNAP).k(14, 0, 0, 0);
        Anims.add(knives);

        var cloak = Anim.of("shadow.cloak", 16).fade(2);
        Moves.sink(cloak, 0, 16);
        Anims.add(cloak);

        // Ночь: развести руки низко — тьма расходится по полу.
        var night = Anim.of("shadow.night", 22).fade(3);
        night.part(ARM_R).k(0, 0, 0, 0).k(7, -120, 50, 16, Ease.SMOOTH).k(13, -24, -20, 50, Ease.SNAP)
                .k(17, -22, -18, 48).k(22, 0, 0, 0, Ease.SMOOTH);
        night.part(ARM_L).k(0, 0, 0, 0).k(7, -120, -50, -16, Ease.SMOOTH).k(13, -24, 20, -50, Ease.SNAP)
                .k(17, -22, 18, -48).k(22, 0, 0, 0, Ease.SMOOTH);
        night.part(BODY).k(0, 0, 0, 0).k(7, -8, 0, 0).k(13, 20, 0, 0, Ease.SNAP).k(22, 0, 0, 0);
        night.part(HEAD).k(0, 0, 0, 0).k(7, -14, 0, 0).k(13, 16, 0, 0, Ease.SNAP).k(22, 0, 0, 0);
        Anims.add(night);

        // Яд на клинке: провести клинком по ладони.
        var poison = Anim.of("shadow.poison", 18).fade(2);
        poison.part(ARM_L).k(0, 0, 0, 0).k(5, -105, -40, -20, Ease.SMOOTH).k(13, -100, -36, -18)
                .k(18, 0, 0, 0, Ease.SMOOTH);
        poison.part(ARM_R).k(0, 0, 0, 0).k(5, -110, 55, 10, Ease.SMOOTH).k(10, -95, 20, 24, Ease.SMOOTH)
                .k(13, -112, 58, 8, Ease.SNAP).k(18, 0, 0, 0, Ease.SMOOTH);
        poison.part(HEAD).k(0, 0, 0, 0).k(7, 22, 0, 0).k(13, 20, 0, 0).k(18, 0, 0, 0);
        Anims.add(poison);

        // Перерезать: короткий росчерк на уровне горла.
        var cut = Anim.of("shadow.cut", 12).fade(1);
        cut.part(ARM_R).k(0, 0, 0, 0).k(3, -105, 70, 35, Ease.SMOOTH).k(6, -100, -60, -14, Ease.SNAP)
                .k(12, 0, 0, 0, Ease.SMOOTH);
        cut.part(BODY).k(0, 0, 0, 0).k(3, 0, 20, 0).k(6, 4, -18, 0, Ease.SNAP).k(12, 0, 0, 0);
        cut.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(6, 0, 0, 0, 0, 0, 0.14, Ease.SNAP)
                .key(12, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(cut);
    }

    // ------------------------------------------------------------------ Жнец

    private static void reaper() {
        // Коса: широкий взмах на 150° с разворотом корпуса.
        var scythe = Anim.of("reaper.scythe", 18).fade(2);
        scythe.part(ARM_R).k(0, 0, 0, 0).k(7, -55, 120, 80, Ease.SMOOTH).k(12, -75, -100, -30, Ease.SNAP)
                .k(18, 0, 0, 0, Ease.SMOOTH);
        scythe.part(ARM_L).k(0, 0, 0, 0).k(7, -40, 70, -10, Ease.SMOOTH).k(12, -60, -40, -30, Ease.SNAP)
                .k(18, 0, 0, 0, Ease.SMOOTH);
        scythe.part(BODY).k(0, 0, 0, 0).k(7, 0, 36, 0, Ease.SMOOTH).k(12, 6, -32, 0, Ease.SNAP).k(18, 0, 0, 0);
        scythe.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(7, 0, 20, 0, 0, 0, 0, Ease.SMOOTH)
                .key(12, 0, -24, 0, 0, 0, 0, Ease.SNAP).key(18, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(scythe);

        var sickle = Anim.of("reaper.sickle", 18).fade(2);
        Moves.lash(sickle, ARM_R, 0, 18);
        Anims.add(sickle);

        // Метка смерти: два пальца к цели — и знак гаснет.
        var mark = Anim.of("reaper.mark", 15).fade(2);
        Moves.point(mark, ARM_R, 0, 15);
        mark.part(BODY).k(0, 0, 0, 0).k(7, 0, -12, 0, Ease.SNAP).k(15, 0, 0, 0);
        Anims.add(mark);

        // Жатва: оборот косой и удар в землю — души лопаются.
        var harvest = Anim.of("reaper.harvest", 26).fade(3);
        Moves.spin(harvest, 0, 14, 1);
        Moves.fists(harvest, 14, 12);
        Anims.add(harvest);

        // Проводник: руки чуть разведены, жнец скользит над полом.
        var ferryman = Anim.of("reaper.ferryman", 24).fade(3);
        ferryman.part(ARM_R).k(0, 0, 0, 0).k(6, -28, -24, 40, Ease.SMOOTH).k(18, -26, -22, 38, Ease.SHAKE)
                .k(24, 0, 0, 0, Ease.SMOOTH);
        ferryman.part(ARM_L).k(0, 0, 0, 0).k(6, -28, 24, -40, Ease.SMOOTH).k(18, -26, 22, -38, Ease.SHAKE)
                .k(24, 0, 0, 0, Ease.SMOOTH);
        ferryman.part(HEAD).k(0, 0, 0, 0).k(6, -12, 0, 0).k(18, -10, 0, 0).k(24, 0, 0, 0);
        ferryman.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(8, 0, 0, 0, 0, 0.14, 0, Ease.SMOOTH)
                .key(18, 0, 0, 0, 0, 0.12, 0, Ease.SMOOTH).key(24, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(ferryman);

        // Приговор: рука вверх, счёт закрыт — ладонь сжимается в кулак.
        var sentence = Anim.of("reaper.sentence", 20).fade(2);
        sentence.part(ARM_R).k(0, 0, 0, 0).k(6, -168, -10, 18, Ease.SMOOTH).k(11, -165, -8, 16, Ease.SHAKE)
                .k(14, -150, 0, 6, Ease.SNAP).k(20, 0, 0, 0, Ease.SMOOTH);
        sentence.part(ARM_L).k(0, 0, 0, 0).k(6, -30, 20, -18, Ease.SMOOTH).k(20, 0, 0, 0);
        sentence.part(HEAD).k(0, 0, 0, 0).k(6, -24, 0, 0).k(14, -18, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        sentence.part(BODY).k(0, 0, 0, 0).k(6, -10, 0, 0).k(14, -6, 0, 0).k(20, 0, 0, 0);
        Anims.add(sentence);
    }

    // ------------------------------------------------------------------ Гарпунёр

    private static void harpooner() {
        var th = Anim.of("harpoon.throw", 16).fade(2);
        Moves.lash(th, ARM_R, 0, 16);
        Anims.add(th);

        // Двойная цепь: обе руки в стороны — цепь между двумя врагами.
        var dbl = Anim.of("harpoon.double", 18).fade(2);
        dbl.part(ARM_R).k(0, 0, 0, 0).k(5, -40, 60, 40, Ease.SMOOTH).key(9, -92, -40, 10, 0, 0, -3, Ease.SNAP)
                .k(18, 0, 0, 0, Ease.SMOOTH);
        dbl.part(ARM_L).k(0, 0, 0, 0).k(5, -40, -60, -40, Ease.SMOOTH).key(9, -92, 40, -10, 0, 0, -3, Ease.SNAP)
                .k(18, 0, 0, 0, Ease.SMOOTH);
        dbl.part(BODY).k(0, 0, 0, 0).k(9, 10, 0, 0, Ease.SNAP).k(18, 0, 0, 0);
        Anims.add(dbl);

        // Намотка: три оборота цепью над головой и бросок.
        var windup = Anim.of("harpoon.windup", 26).fade(2);
        windup.part(ARM_R).k(0, 0, 0, 0).k(3, -170, -30, 30, Ease.SMOOTH).k(8, -170, 30, 30, Ease.LINEAR)
                .k(13, -170, -30, 30, Ease.LINEAR).k(18, -170, 30, 30, Ease.LINEAR)
                .k(21, -30, 0, -8, Ease.SNAP).k(26, 0, 0, 0, Ease.SMOOTH);
        windup.part(BODY).k(0, 0, 0, 0).k(8, 0, 18, 0, Ease.LINEAR).k(13, 0, -18, 0, Ease.LINEAR)
                .k(18, 0, 18, 0, Ease.LINEAR).k(21, 16, 0, 0, Ease.SNAP).k(26, 0, 0, 0);
        windup.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(8, 0, 30, 0, 0, 0, 0, Ease.LINEAR)
                .key(13, 0, -20, 0, 0, 0, 0, Ease.LINEAR).key(18, 0, 20, 0, 0, 0, 0, Ease.LINEAR)
                .key(21, 0, 0, 0, 0, 0, 0.1, Ease.SNAP).key(26, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(windup);

        // Кандалы: бросок под ноги и рывок вверх — цепь на ногах.
        var shackles = Anim.of("harpoon.shackles", 18).fade(2);
        Moves.lob(shackles, ARM_R, 0, 12);
        shackles.part(ARM_L).k(0, 0, 0, 0).k(8, -50, -25, -30, Ease.SMOOTH).k(12, -20, -10, -14, Ease.SNAP)
                .k(18, 0, 0, 0, Ease.SMOOTH);
        Anims.add(shackles);

        // Рывок на цепи: гарпунёр сам летит к цели.
        var self = Anim.of("harpoon.pull_self", 16).fade(1);
        self.part(ARM_R).k(0, 0, 0, 0).k(4, -95, 10, 8, Ease.SNAP).k(8, -40, 45, 24, Ease.SNAP)
                .k(16, 0, 0, 0, Ease.SMOOTH);
        self.part(ARM_L).k(0, 0, 0, 0).k(8, 50, 20, -18, Ease.SNAP).k(16, 0, 0, 0, Ease.SMOOTH);
        self.part(BODY).k(0, 0, 0, 0).k(4, -8, 0, 0).k(8, 28, 0, 0, Ease.SNAP).k(16, 0, 0, 0);
        self.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(4, 0, 0, 0, 0, 0, -0.1, Ease.SNAP)
                .key(10, 14, 0, 0, 0, 0.1, 0.45, Ease.SNAP).key(16, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(self);

        // Тяга бездны: обе цепи вокруг — и всех в одну точку.
        var abyss = Anim.of("harpoon.abyss", 28).fade(3);
        abyss.part(ARM_R).k(0, 0, 0, 0).k(8, -100, -60, 70, Ease.SMOOTH).k(12, -98, -58, 68)
                .k(18, -70, 70, 14, Ease.SNAP).k(28, 0, 0, 0, Ease.SMOOTH);
        abyss.part(ARM_L).k(0, 0, 0, 0).k(8, -100, 60, -70, Ease.SMOOTH).k(12, -98, 58, -68)
                .k(18, -70, -70, -14, Ease.SNAP).k(28, 0, 0, 0, Ease.SMOOTH);
        abyss.part(BODY).k(0, 0, 0, 0).k(8, -14, 0, 0).k(18, 22, 0, 0, Ease.SNAP).k(28, 0, 0, 0);
        abyss.part(HEAD).k(0, 0, 0, 0).k(8, -18, 0, 0).k(18, 14, 0, 0, Ease.SNAP).k(28, 0, 0, 0);
        abyss.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(12, 0, 0, 0, 0, 0.08, 0, Ease.SMOOTH)
                .key(18, 0, 0, 0, 0, -0.08, 0, Ease.SNAP).key(28, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(abyss);
    }

    // ------------------------------------------------------------------ Палач

    private static void headsman() {
        // Тяжёлый замах: топор поднят над головой и дрожит — этот замах не сбить.
        var swingCast = Anim.of("headsman.swing.cast", 32).fade(3);
        swingCast.part(ARM_R).k(0, 0, 0, 0).k(10, -200, -16, 24, Ease.SMOOTH).k(18, -205, -14, 26, Ease.SHAKE)
                .k(26, -200, -16, 24, Ease.SHAKE).k(32, -205, -14, 26, Ease.SHAKE);
        swingCast.part(ARM_L).k(0, 0, 0, 0).k(10, -195, 24, -30, Ease.SMOOTH).k(18, -200, 22, -32, Ease.SHAKE)
                .k(26, -195, 24, -30, Ease.SHAKE).k(32, -200, 22, -32, Ease.SHAKE);
        swingCast.part(BODY).k(0, 0, 0, 0).k(10, -18, 0, 0, Ease.SMOOTH).k(18, -20, 0, 0, Ease.SHAKE)
                .k(32, -18, 0, 0, Ease.SHAKE);
        swingCast.part(HEAD).k(0, 0, 0, 0).k(10, -14, 0, 0).k(32, -12, 0, 0);
        swingCast.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(10, 0, 0, 0, 0, 0.06, 0, Ease.SMOOTH)
                .key(18, 0, 0, 2, 0.02, 0.06, 0, Ease.SHAKE).key(26, 0, 0, -2, -0.02, 0.06, 0, Ease.SHAKE)
                .key(32, 0, 0, 2, 0.02, 0.06, 0, Ease.SHAKE);
        Anims.add(swingCast);

        // И сам удар: всё тело за топором.
        var swing = Anim.of("headsman.swing", 18).fade(1);
        swing.part(ARM_R).k(0, -200, -16, 24).k(4, -10, 6, -8, Ease.SNAP).k(9, -4, 4, -6).k(18, 0, 0, 0, Ease.SMOOTH);
        swing.part(ARM_L).k(0, -195, 24, -30).k(4, -10, -6, 8, Ease.SNAP).k(9, -4, -4, 6).k(18, 0, 0, 0, Ease.SMOOTH);
        swing.part(BODY).k(0, -18, 0, 0).k(4, 38, 0, 0, Ease.SNAP).k(9, 34, 0, 0).k(18, 0, 0, 0, Ease.SMOOTH);
        swing.part(HEAD).k(0, -12, 0, 0).k(4, 20, 0, 0, Ease.SNAP).k(18, 0, 0, 0);
        swing.part(ROOT).key(0, 0, 0, 0, 0, 0.06, 0, Ease.LINEAR).key(4, 0, 0, 0, 0, -0.12, 0.1, Ease.SNAP)
                .key(9, 0, 0, 0, 0, -0.1, 0.08, Ease.LINEAR).key(18, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(swing);

        var hook = Anim.of("headsman.hook", 20).fade(2);
        Moves.lash(hook, ARM_R, 0, 20);
        Anims.add(hook);

        // Колесо: тяжёлый оборот топором вокруг себя.
        var wheel = Anim.of("headsman.wheel", 24).fade(2);
        Moves.spin(wheel, 0, 24, 1);
        wheel.part(ARM_R).k(0, 0, 0, 0).k(4, -70, -30, 90, Ease.SNAP).k(20, -70, -30, 90).k(24, 0, 0, 0, Ease.SMOOTH);
        wheel.part(BODY).k(0, 0, 0, 0).k(4, 14, 0, 0, Ease.SNAP).k(20, 12, 0, 0).k(24, 0, 0, 0);
        Anims.add(wheel);

        // Приговор исполнен: короткий рубящий сверху вниз по раненому.
        var verdict = Anim.of("headsman.verdict", 17).fade(2);
        Moves.chop(verdict, ARM_R, 0, 15);
        verdict.part(ARM_L).k(0, 0, 0, 0).k(7, -180, 20, -26, Ease.SMOOTH).k(10, -10, -4, 6, Ease.SNAP)
                .k(17, 0, 0, 0, Ease.SMOOTH);
        Anims.add(verdict);

        // Плаха: поставить колоду перед собой.
        var block = Anim.of("headsman.block", 20).fade(2);
        Moves.toGround(block, ARM_L, 0, 14);
        block.part(ARM_R).k(0, 0, 0, 0).k(6, -40, -20, 22, Ease.SMOOTH).k(14, -20, -10, 12).k(20, 0, 0, 0, Ease.SMOOTH);
        Anims.add(block);

        // Эшафот: встать как врос, топор на плечо.
        var scaffold = Anim.of("headsman.scaffold", 24).fade(3);
        Moves.dig(scaffold, 0, 24);
        scaffold.part(ARM_R).k(0, 0, 0, 0).k(5, -120, 40, 20, Ease.SNAP).k(19, -118, 38, 18).k(24, 0, 0, 0, Ease.SMOOTH);
        scaffold.part(HEAD).k(0, 0, 0, 0).k(5, -10, 0, 0, Ease.SNAP).k(19, -8, 0, 0).k(24, 0, 0, 0);
        Anims.add(scaffold);
    }
}
