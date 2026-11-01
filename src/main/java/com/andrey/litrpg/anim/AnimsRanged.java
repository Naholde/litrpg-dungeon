package com.andrey.litrpg.anim;

import com.andrey.litrpg.anim.Anim.Ease;

import static com.andrey.litrpg.anim.Anim.Part.ARM_L;
import static com.andrey.litrpg.anim.Anim.Part.ARM_R;
import static com.andrey.litrpg.anim.Anim.Part.BODY;
import static com.andrey.litrpg.anim.Anim.Part.HEAD;
import static com.andrey.litrpg.anim.Anim.Part.LEG_R;
import static com.andrey.litrpg.anim.Anim.Part.ROOT;

/** Движения стрелков: Снайпер и Механик. */
final class AnimsRanged {
    private AnimsRanged() {
    }

    static void register() {
        sniper();
        mechanic();
    }

    // ------------------------------------------------------------------ Снайпер

    private static void sniper() {
        // Прицелиться: вскинуть оружие и замереть — чем дольше стоишь, тем больнее выстрел.
        var aim = Anim.of("sniper.aim", 60).fade(4);
        aim.part(ARM_R).k(0, 0, 0, 0).k(6, -92, -46, 12, Ease.SMOOTH).k(30, -90, -44, 11, Ease.SHAKE)
                .k(54, -92, -46, 12, Ease.SHAKE).k(60, 0, 0, 0, Ease.SMOOTH);
        aim.part(ARM_L).k(0, 0, 0, 0).k(6, -94, -14, -10, Ease.SMOOTH).k(30, -92, -13, -9, Ease.SHAKE)
                .k(54, -94, -14, -10, Ease.SHAKE).k(60, 0, 0, 0, Ease.SMOOTH);
        aim.part(BODY).k(0, 0, 0, 0).k(6, 4, 16, 0, Ease.SMOOTH).k(54, 4, 15, 0).k(60, 0, 0, 0);
        aim.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(6, 0, 0, 0, 0, -0.04, 0, Ease.SMOOTH)
                .key(54, 0, 0, 0, 0, -0.04, 0, Ease.LINEAR).key(60, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(aim);

        // Задержать дыхание: вдох, грудь вверх, руки каменеют.
        var breath = Anim.of("sniper.breath", 24).fade(3);
        breath.part(ARM_R).k(0, 0, 0, 0).k(5, -88, -42, 10, Ease.SMOOTH).k(18, -88, -42, 10).k(24, 0, 0, 0, Ease.SMOOTH);
        breath.part(ARM_L).k(0, 0, 0, 0).k(5, -90, -12, -8, Ease.SMOOTH).k(18, -90, -12, -8).k(24, 0, 0, 0, Ease.SMOOTH);
        breath.part(BODY).k(0, 0, 0, 0).k(5, -10, 14, 0, Ease.SMOOTH).k(18, -8, 14, 0).k(24, 0, 0, 0);
        breath.part(HEAD).k(0, 0, 0, 0).k(5, -8, 0, 0).k(18, -6, 0, 0).k(24, 0, 0, 0);
        breath.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(5, 0, 0, 0, 0, 0.05, 0, Ease.SMOOTH)
                .key(18, 0, 0, 0, 0, 0.04, 0, Ease.LINEAR).key(24, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(breath);

        var mark = Anim.of("sniper.mark", 15).fade(2);
        Moves.point(mark, ARM_R, 0, 15);
        Anims.add(mark);

        var trap = Anim.of("sniper.trap", 18).fade(2);
        Moves.lob(trap, ARM_R, 0, 12);
        trap.part(ARM_L).k(0, 0, 0, 0).k(8, -45, -25, -26, Ease.SMOOTH).k(18, 0, 0, 0, Ease.SMOOTH);
        Anims.add(trap);

        // Навылет: прицел держится, ствол чуть дрожит.
        var pierceCast = Anim.of("sniper.pierce.cast", 16).fade(2);
        pierceCast.part(ARM_R).k(0, 0, 0, 0).k(4, -96, -48, 12, Ease.SNAP).k(10, -98, -47, 13, Ease.SHAKE)
                .k(16, -96, -48, 12, Ease.SHAKE);
        pierceCast.part(ARM_L).k(0, 0, 0, 0).k(4, -98, -14, -10, Ease.SNAP).k(10, -100, -13, -9, Ease.SHAKE)
                .k(16, -98, -14, -10, Ease.SHAKE);
        pierceCast.part(BODY).k(0, 0, 0, 0).k(4, 2, 18, 0, Ease.SNAP).k(16, 2, 17, 0);
        Anims.add(pierceCast);

        var pierce = Anim.of("sniper.pierce", 16).fade(1);
        Moves.shot(pierce, ARM_R, 0, 16);
        pierce.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(9, 0, 0, 0, 0, 0, -0.16, Ease.SNAP)
                .key(16, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(pierce);

        // Контрольный: короткий сухой выстрел.
        var fin = Anim.of("sniper.finisher", 13).fade(1);
        Moves.shot(fin, ARM_R, 0, 13);
        fin.part(HEAD).k(0, 0, 0, 0).k(7, 6, -6, 0, Ease.SNAP).k(13, 0, 0, 0);
        Anims.add(fin);
    }

    // ------------------------------------------------------------------ Механик

    private static void mechanic() {
        // Турель: поставить станок и щёлкнуть тумблером — он раскрывается.
        var turret = Anim.of("mech.turret", 22).fade(2);
        Moves.toGround(turret, ARM_R, 0, 14);
        turret.part(ARM_L).k(0, 0, 0, 0).k(8, -30, -20, -18, Ease.SMOOTH).k(15, -60, -10, -24, Ease.SNAP)
                .k(22, 0, 0, 0, Ease.SMOOTH);
        Anims.add(turret);

        // Дробовик: выстрел в упор и перезарядка цевьём.
        var shotgun = Anim.of("mech.shotgun", 18).fade(1);
        Moves.shot(shotgun, ARM_R, 0, 11);
        shotgun.part(ARM_L).k(0, 0, 0, 0).k(5, -80, -24, -14, Ease.SMOOTH).key(8, -76, -22, -12, 0, 0, 3, Ease.SNAP)
                .key(13, -82, -24, -14, 0, 0, -2, Ease.SNAP).k(18, 0, 0, 0, Ease.SMOOTH);
        shotgun.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(7, 0, 0, 0, 0, 0, -0.2, Ease.SNAP)
                .key(18, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(shotgun);

        var mine = Anim.of("mech.mine", 14).fade(1);
        Moves.lob(mine, ARM_R, 0, 14);
        Anims.add(mine);

        // Мортира: ствол вверх, три хлопка.
        var mortar = Anim.of("mech.mortar", 22).fade(2);
        mortar.part(ARM_R).k(0, 0, 0, 0).k(6, -150, -20, 26, Ease.SMOOTH).key(10, -154, -20, 26, 0, 0, 2, Ease.SNAP)
                .key(14, -150, -20, 26, 0, 0, 0, Ease.SNAP).key(18, -154, -20, 26, 0, 0, 2, Ease.SNAP)
                .k(22, 0, 0, 0, Ease.SMOOTH);
        mortar.part(ARM_L).k(0, 0, 0, 0).k(6, -120, 30, -30, Ease.SMOOTH).k(18, -118, 28, -28)
                .k(22, 0, 0, 0, Ease.SMOOTH);
        mortar.part(BODY).k(0, 0, 0, 0).k(6, -12, 0, 0).k(18, -10, 0, 0).k(22, 0, 0, 0);
        mortar.part(HEAD).k(0, 0, 0, 0).k(6, -20, 0, 0).k(18, -18, 0, 0).k(22, 0, 0, 0);
        Anims.add(mortar);

        // Разгон станков: дёрнуть рычаг на поясе два раза.
        var over = Anim.of("mech.overdrive", 20).fade(2);
        over.part(ARM_R).k(0, 0, 0, 0).k(4, -50, 50, 18, Ease.SMOOTH).k(7, -18, 40, 10, Ease.SNAP)
                .k(11, -50, 50, 18, Ease.SNAP).k(14, -18, 40, 10, Ease.SNAP).k(20, 0, 0, 0, Ease.SMOOTH);
        over.part(ARM_L).k(0, 0, 0, 0).k(4, -40, -45, -16, Ease.SMOOTH).k(14, -36, -42, -14).k(20, 0, 0, 0, Ease.SMOOTH);
        over.part(BODY).k(0, 0, 0, 0).k(7, 10, 0, 0, Ease.SNAP).k(14, 12, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        over.part(HEAD).k(0, 0, 0, 0).k(7, 14, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        Anims.add(over);

        // Полный арсенал: оборот с расстановкой трёх турелей вокруг себя.
        var arsenal = Anim.of("mech.arsenal", 30).fade(3);
        Moves.handDown(arsenal, ARM_R, 0, 9);
        Moves.handDown(arsenal, ARM_L, 10, 9);
        Moves.handDown(arsenal, ARM_R, 20, 9);
        arsenal.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(10, 0, 120, 0, 0, -0.1, 0, Ease.SMOOTH)
                .key(20, 0, 240, 0, 0, -0.1, 0, Ease.SMOOTH).key(28, 0, 360, 0, 0, -0.1, 0, Ease.SMOOTH)
                .key(30, 0, 360, 0, 0, 0, 0, Ease.SMOOTH);
        arsenal.add(LEG_R).k(0, 0, 0, 0).k(14, -20, 0, 10, Ease.SMOOTH).k(30, 0, 0, 0, Ease.SMOOTH);
        Anims.add(arsenal);
    }
}
