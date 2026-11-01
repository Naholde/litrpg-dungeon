package com.andrey.litrpg.anim;

import com.andrey.litrpg.anim.Anim.Ease;

import static com.andrey.litrpg.anim.Anim.Part.ARM_L;
import static com.andrey.litrpg.anim.Anim.Part.ARM_R;
import static com.andrey.litrpg.anim.Anim.Part.BODY;
import static com.andrey.litrpg.anim.Anim.Part.HEAD;
import static com.andrey.litrpg.anim.Anim.Part.LEG_R;
import static com.andrey.litrpg.anim.Anim.Part.ROOT;

/**
 * Движения колдунов и «армии»: Пиромант, Криомант, Громовержец, Чумной владыка, Астромант,
 * Призыватель, Иллюзионист, Чернокнижник, Разломщик.
 */
final class AnimsMagic {
    private AnimsMagic() {
    }

    static void register() {
        pyromancer();
        cryomancer();
        stormcaller();
        plague();
        astromancer();
        summoner();
        illusionist();
        warlock();
        rift();
    }

    // ------------------------------------------------------------------ Пиромант

    private static void pyromancer() {
        // Искра: щелчок пальцами в сторону цели.
        var spark = Anim.of("pyro.spark", 11).fade(1);
        spark.part(ARM_R).k(0, 0, 0, 0).k(3, -80, 30, 26, Ease.SMOOTH).k(5, -92, 6, 8, Ease.SNAP)
                .k(7, -86, 14, 16, Ease.SNAP).k(11, 0, 0, 0, Ease.SMOOTH);
        spark.part(BODY).k(0, 0, 0, 0).k(5, 4, -8, 0, Ease.SNAP).k(11, 0, 0, 0);
        Anims.add(spark);

        // Горящая земля: ладонь книзу — пол под врагами занимается.
        var ground = Anim.of("pyro.ground", 18).fade(2);
        ground.part(ARM_R).k(0, 0, 0, 0).k(5, -120, -20, 30, Ease.SMOOTH).k(10, -46, 10, 16, Ease.SNAP)
                .k(14, -42, 8, 14).k(18, 0, 0, 0, Ease.SMOOTH);
        ground.part(ARM_L).k(0, 0, 0, 0).k(5, -40, 25, -20, Ease.SMOOTH).k(18, 0, 0, 0, Ease.SMOOTH);
        ground.part(BODY).k(0, 0, 0, 0).k(5, -8, 0, 0).k(10, 18, 0, 0, Ease.SNAP).k(18, 0, 0, 0);
        Anims.add(ground);

        // Взрыв: сжать кулак и рвануть вниз — слои жара детонируют.
        var det = Anim.of("pyro.detonate", 15).fade(2);
        det.part(ARM_R).k(0, 0, 0, 0).k(5, -140, 20, 24, Ease.SMOOTH).k(8, -136, 16, 20)
                .k(10, -30, 0, 10, Ease.SNAP).k(15, 0, 0, 0, Ease.SMOOTH);
        det.part(ARM_L).k(0, 0, 0, 0).k(5, -60, -30, -24, Ease.SMOOTH).k(10, -20, -10, -10, Ease.SNAP)
                .k(15, 0, 0, 0, Ease.SMOOTH);
        det.part(BODY).k(0, 0, 0, 0).k(5, -10, 0, 0).k(10, 22, 0, 0, Ease.SNAP).k(15, 0, 0, 0);
        det.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(10, 0, 0, 0, 0, -0.08, 0, Ease.SNAP)
                .key(15, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(det);

        // Выдох пламени: вдохнуть, отклониться — и выдох конусом.
        var breath = Anim.of("pyro.breath", 22).fade(2);
        breath.part(ARM_R).k(0, 0, 0, 0).k(7, -130, 50, 14, Ease.SMOOTH).k(11, -118, 44, 12)
                .k(14, -70, 10, 30, Ease.SNAP).k(22, 0, 0, 0, Ease.SMOOTH);
        breath.part(ARM_L).k(0, 0, 0, 0).k(7, -130, -50, -14, Ease.SMOOTH).k(11, -118, -44, -12)
                .k(14, -70, -10, -30, Ease.SNAP).k(22, 0, 0, 0, Ease.SMOOTH);
        breath.part(BODY).k(0, 0, 0, 0).k(7, -18, 0, 0, Ease.SMOOTH).k(11, -16, 0, 0)
                .k(14, 20, 0, 0, Ease.SNAP).k(22, 0, 0, 0);
        breath.part(HEAD).k(0, 0, 0, 0).k(7, -26, 0, 0, Ease.SMOOTH).k(14, 14, 0, 0, Ease.SNAP).k(22, 0, 0, 0);
        Anims.add(breath);

        // Огненный шторм: обе руки вверх и резко вниз — метеоры падают.
        var storm = Anim.of("pyro.firestorm", 26).fade(3);
        storm.part(ARM_R).k(0, 0, 0, 0).k(9, -176, -14, 16, Ease.SMOOTH).k(14, -172, -12, 14, Ease.SHAKE)
                .k(18, -54, 6, 14, Ease.SNAP).k(26, 0, 0, 0, Ease.SMOOTH);
        storm.part(ARM_L).k(0, 0, 0, 0).k(9, -176, 14, -16, Ease.SMOOTH).k(14, -172, 12, -14, Ease.SHAKE)
                .k(18, -54, -6, -14, Ease.SNAP).k(26, 0, 0, 0, Ease.SMOOTH);
        storm.part(BODY).k(0, 0, 0, 0).k(9, -16, 0, 0).k(18, 24, 0, 0, Ease.SNAP).k(26, 0, 0, 0);
        storm.part(HEAD).k(0, 0, 0, 0).k(9, -30, 0, 0).k(18, 18, 0, 0, Ease.SNAP).k(26, 0, 0, 0);
        storm.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(14, 0, 0, 0, 0, 0.14, 0, Ease.SHAKE)
                .key(18, 0, 0, 0, 0, -0.06, 0, Ease.SNAP).key(26, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(storm);

        // Пекло: пиромант горит сам — руки в стороны, голова вверх, жар рвётся.
        var inferno = Anim.of("pyro.inferno", 32).fade(3);
        Moves.tremble(inferno, 4, 22);
        inferno.part(ARM_R).k(0, 0, 0, 0).k(6, -120, -40, 66, Ease.SNAP).k(16, -126, -36, 70, Ease.SHAKE)
                .k(26, -120, -40, 66, Ease.SHAKE).k(32, 0, 0, 0, Ease.SMOOTH);
        inferno.part(ARM_L).k(0, 0, 0, 0).k(6, -120, 40, -66, Ease.SNAP).k(16, -126, 36, -70, Ease.SHAKE)
                .k(26, -120, 40, -66, Ease.SHAKE).k(32, 0, 0, 0, Ease.SMOOTH);
        inferno.part(HEAD).k(0, 0, 0, 0).k(6, -34, 0, 0, Ease.SNAP).k(26, -30, 0, 0).k(32, 0, 0, 0);
        Anims.add(inferno);
    }

    // ------------------------------------------------------------------ Криомант

    private static void cryomancer() {
        var needle = Anim.of("cryo.needle", 12).fade(1);
        Moves.point(needle, ARM_R, 0, 12);
        Anims.add(needle);

        // Вьюга: круг рукой над головой — метель встаёт.
        var blizzard = Anim.of("cryo.blizzard", 24).fade(2);
        blizzard.part(ARM_R).k(0, 0, 0, 0).k(5, -170, -40, 20, Ease.SMOOTH).k(10, -170, 0, 20, Ease.LINEAR)
                .k(15, -170, 40, 20, Ease.LINEAR).k(19, -120, 10, 30, Ease.SNAP).k(24, 0, 0, 0, Ease.SMOOTH);
        blizzard.part(ARM_L).k(0, 0, 0, 0).k(5, -60, 30, -40, Ease.SMOOTH).k(19, -50, 20, -34).k(24, 0, 0, 0, Ease.SMOOTH);
        blizzard.part(HEAD).k(0, 0, 0, 0).k(10, -18, 0, 0).k(24, 0, 0, 0);
        blizzard.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(15, 0, 40, 0, 0, 0, 0, Ease.SMOOTH)
                .key(24, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(blizzard);

        // Заморозка: ладонь вперёд, пальцы расходятся — цель схватывает лёд.
        var freeze = Anim.of("cryo.freeze", 18).fade(2);
        freeze.part(ARM_R).k(0, 0, 0, 0).k(5, -92, 12, 10, Ease.SMOOTH).k(9, -96, 4, 2, Ease.SNAP)
                .k(13, -94, 8, 18, Ease.SMOOTH).k(18, 0, 0, 0, Ease.SMOOTH);
        freeze.part(ARM_L).k(0, 0, 0, 0).k(5, -70, -30, -20, Ease.SMOOTH).k(13, -66, -26, -18).k(18, 0, 0, 0);
        freeze.part(BODY).k(0, 0, 0, 0).k(9, 8, 0, 0, Ease.SNAP).k(18, 0, 0, 0);
        Anims.add(freeze);

        // Раскол: кулак сверху вниз — лёд разлетается осколками.
        var shatter = Anim.of("cryo.shatter", 15).fade(2);
        Moves.chop(shatter, ARM_R, 0, 13);
        shatter.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(8, 0, 0, 0, 0, -0.1, 0, Ease.SNAP)
                .key(15, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(shatter);

        // Ледяные шипы: ладонь к земле и рывок вперёд — из пола идёт полоса.
        var spikes = Anim.of("cryo.spikes", 18).fade(2);
        spikes.part(ARM_R).k(0, 0, 0, 0).k(5, -130, -16, 26, Ease.SMOOTH).k(9, -30, 6, 14, Ease.SNAP)
                .key(13, -58, 6, 10, 0, 0, -4, Ease.SNAP).k(18, 0, 0, 0, Ease.SMOOTH);
        spikes.part(ARM_L).k(0, 0, 0, 0).k(9, -24, -8, -12, Ease.SNAP).k(18, 0, 0, 0, Ease.SMOOTH);
        spikes.part(BODY).k(0, 0, 0, 0).k(9, 26, 0, 0, Ease.SNAP).k(13, 16, 0, 0).k(18, 0, 0, 0);
        spikes.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(9, 0, 0, 0, 0, -0.12, 0, Ease.SNAP)
                .key(18, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(spikes);

        // Абсолютный ноль: руки крест-накрест — и развести, всё сковано.
        var zero = Anim.of("cryo.zero", 28).fade(3);
        zero.part(ARM_R).k(0, 0, 0, 0).k(8, -104, 58, 16, Ease.SMOOTH).k(14, -104, 58, 16, Ease.SHAKE)
                .k(18, -86, -40, 74, Ease.SNAP).k(23, -84, -38, 72).k(28, 0, 0, 0, Ease.SMOOTH);
        zero.part(ARM_L).k(0, 0, 0, 0).k(8, -104, -58, -16, Ease.SMOOTH).k(14, -104, -58, -16, Ease.SHAKE)
                .k(18, -86, 40, -74, Ease.SNAP).k(23, -84, 38, -72).k(28, 0, 0, 0, Ease.SMOOTH);
        zero.part(BODY).k(0, 0, 0, 0).k(8, 12, 0, 0).k(18, -14, 0, 0, Ease.SNAP).k(28, 0, 0, 0);
        zero.part(HEAD).k(0, 0, 0, 0).k(8, 14, 0, 0).k(18, -18, 0, 0, Ease.SNAP).k(28, 0, 0, 0);
        zero.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(14, 0, 0, 0, 0, -0.06, 0, Ease.SMOOTH)
                .key(18, 0, 0, 0, 0, 0.1, 0, Ease.SNAP).key(28, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(zero);
    }

    // ------------------------------------------------------------------ Громовержец

    private static void stormcaller() {
        // Цепь молний: рука хлёстко вперёд, пальцы врастопырку.
        var chain = Anim.of("storm.chain", 14).fade(1);
        chain.part(ARM_R).k(0, 0, 0, 0).k(4, -130, 40, 30, Ease.SMOOTH).key(7, -94, 0, 6, 0, 0, -3, Ease.SNAP)
                .k(10, -88, 20, 20, Ease.SNAP).k(14, 0, 0, 0, Ease.SMOOTH);
        chain.part(BODY).k(0, 0, 0, 0).k(4, -8, 12, 0).k(7, 12, -8, 0, Ease.SNAP).k(14, 0, 0, 0);
        Anims.add(chain);

        // Разряд: короткий удар кулаком вперёд.
        var jolt = Anim.of("storm.jolt", 11).fade(1);
        Moves.thrust(jolt, ARM_R, 0, 11);
        Anims.add(jolt);

        // Статика: два пальца вбок — искры прыгают по соседям.
        var stat = Anim.of("storm.static", 14).fade(1);
        stat.part(ARM_R).k(0, 0, 0, 0).k(4, -75, -50, 40, Ease.SMOOTH).k(7, -80, 30, 26, Ease.SNAP)
                .k(10, -78, -20, 34, Ease.SNAP).k(14, 0, 0, 0, Ease.SMOOTH);
        stat.part(HEAD).k(0, 0, 0, 0).k(7, 0, -12, 0, Ease.SNAP).k(10, 0, 8, 0, Ease.SNAP).k(14, 0, 0, 0);
        Anims.add(stat);

        // Грозовая туча: рука вверх, круг — над целью собирается туча.
        var cloud = Anim.of("storm.cloud", 22).fade(2);
        cloud.part(ARM_R).k(0, 0, 0, 0).k(6, -175, -30, 16, Ease.SMOOTH).k(11, -175, 20, 16, Ease.LINEAR)
                .k(15, -120, 40, 24, Ease.SNAP).k(22, 0, 0, 0, Ease.SMOOTH);
        cloud.part(ARM_L).k(0, 0, 0, 0).k(6, -40, 20, -24, Ease.SMOOTH).k(22, 0, 0, 0, Ease.SMOOTH);
        cloud.part(HEAD).k(0, 0, 0, 0).k(6, -26, 0, 0).k(15, -10, 0, 0, Ease.SNAP).k(22, 0, 0, 0);
        cloud.part(BODY).k(0, 0, 0, 0).k(6, -12, 0, 0).k(15, 6, 0, 0, Ease.SNAP).k(22, 0, 0, 0);
        Anims.add(cloud);

        // Громовой шаг: пропасть во вспышке — и появиться с удара.
        var step = Anim.of("storm.step", 16).fade(1);
        step.part(ARM_R).k(0, 0, 0, 0).k(3, -30, 40, 14, Ease.SNAP).k(6, -175, -10, 16, Ease.SNAP)
                .k(9, -20, 6, -6, Ease.SNAP).k(16, 0, 0, 0, Ease.SMOOTH);
        step.part(ARM_L).k(0, 0, 0, 0).k(3, -30, -40, -14, Ease.SNAP).k(9, -16, -6, 6, Ease.SNAP)
                .k(16, 0, 0, 0, Ease.SMOOTH);
        step.part(BODY).k(0, 0, 0, 0).k(3, 16, 0, 0, Ease.SNAP).k(6, -16, 0, 0, Ease.SNAP)
                .k(9, 24, 0, 0, Ease.SNAP).k(16, 0, 0, 0);
        step.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(3, 0, 0, 0, 0, -0.14, 0, Ease.SNAP)
                .key(6, 0, 180, 0, 0, 0.2, 0, Ease.SNAP).key(9, 0, 360, 0, 0, -0.08, 0, Ease.SNAP)
                .key(16, 0, 360, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(step);

        // Буря: обе руки держат небо, молнии бьют сами.
        var tempest = Anim.of("storm.tempest", 30).fade(3);
        tempest.part(ARM_R).k(0, 0, 0, 0).k(7, -178, -16, 18, Ease.SMOOTH).k(15, -174, -14, 16, Ease.SHAKE)
                .k(24, -178, -16, 18, Ease.SHAKE).k(30, 0, 0, 0, Ease.SMOOTH);
        tempest.part(ARM_L).k(0, 0, 0, 0).k(7, -178, 16, -18, Ease.SMOOTH).k(15, -174, 14, -16, Ease.SHAKE)
                .k(24, -178, 16, -18, Ease.SHAKE).k(30, 0, 0, 0, Ease.SMOOTH);
        tempest.part(BODY).k(0, 0, 0, 0).k(7, -16, 0, 0).k(24, -14, 0, 0, Ease.SHAKE).k(30, 0, 0, 0);
        tempest.part(HEAD).k(0, 0, 0, 0).k(7, -32, 0, 0).k(24, -30, 0, 0).k(30, 0, 0, 0);
        tempest.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(7, 0, 0, 0, 0, 0.1, 0, Ease.SMOOTH)
                .key(15, 0, 0, 2, 0.03, 0.12, 0, Ease.SHAKE).key(24, 0, 0, -2, -0.03, 0.1, 0, Ease.SHAKE)
                .key(30, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(tempest);
    }

    // ------------------------------------------------------------------ Чумной владыка

    private static void plague() {
        // Мор: ладонь с кривыми пальцами к цели.
        var mor = Anim.of("plague.plague", 14).fade(2);
        mor.part(ARM_R).k(0, 0, 0, 0).k(5, -88, 20, 16, Ease.SMOOTH).k(9, -94, 6, 4, Ease.SNAP)
                .k(11, -86, 14, 12).k(14, 0, 0, 0, Ease.SMOOTH);
        mor.part(HEAD).k(0, 0, 0, 0).k(9, 10, 0, 0, Ease.SNAP).k(14, 0, 0, 0);
        mor.part(BODY).k(0, 0, 0, 0).k(9, 10, -6, 0, Ease.SNAP).k(14, 0, 0, 0);
        Anims.add(mor);

        // Распространение: смахнуть рукой в сторону — зараза прыгает на соседей.
        var spread = Anim.of("plague.spread", 16).fade(2);
        spread.part(ARM_R).k(0, 0, 0, 0).k(5, -95, 60, 30, Ease.SMOOTH).k(9, -90, -55, 10, Ease.SNAP)
                .k(16, 0, 0, 0, Ease.SMOOTH);
        spread.part(BODY).k(0, 0, 0, 0).k(5, 0, 20, 0).k(9, 6, -20, 0, Ease.SNAP).k(16, 0, 0, 0);
        spread.part(HEAD).k(0, 0, 0, 0).k(5, 0, 14, 0).k(9, 0, -14, 0, Ease.SNAP).k(16, 0, 0, 0);
        Anims.add(spread);

        // Увядание: сжать кулак — лечение цели сохнет.
        var wither = Anim.of("plague.wither", 16).fade(2);
        wither.part(ARM_R).k(0, 0, 0, 0).k(5, -96, 10, 12, Ease.SMOOTH).k(10, -82, 24, 26, Ease.SNAP)
                .k(12, -80, 22, 24).k(16, 0, 0, 0, Ease.SMOOTH);
        wither.part(ARM_L).k(0, 0, 0, 0).k(5, -36, -20, -16, Ease.SMOOTH).k(16, 0, 0, 0, Ease.SMOOTH);
        wither.part(BODY).k(0, 0, 0, 0).k(10, -8, -10, 0, Ease.SNAP).k(16, 0, 0, 0);
        Anims.add(wither);

        // Рой мух: обе руки низко в стороны — туча поднимается с пола.
        var swarm = Anim.of("plague.swarm", 20).fade(2);
        swarm.part(ARM_R).k(0, 0, 0, 0).k(6, -110, 40, 20, Ease.SMOOTH).k(11, -28, -24, 46, Ease.SNAP)
                .k(15, -26, -22, 44).k(20, 0, 0, 0, Ease.SMOOTH);
        swarm.part(ARM_L).k(0, 0, 0, 0).k(6, -110, -40, -20, Ease.SMOOTH).k(11, -28, 24, -46, Ease.SNAP)
                .k(15, -26, 22, -44).k(20, 0, 0, 0, Ease.SMOOTH);
        swarm.part(BODY).k(0, 0, 0, 0).k(6, -6, 0, 0).k(11, 20, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        Anims.add(swarm);

        // Обратить: обвести рукой вокруг головы врага — он идёт на своих.
        var turn = Anim.of("plague.turn", 20).fade(2);
        turn.part(ARM_R).k(0, 0, 0, 0).k(5, -100, 40, 24, Ease.SMOOTH).k(10, -112, 0, 14, Ease.LINEAR)
                .k(14, -100, -40, 24, Ease.LINEAR).k(16, -88, -10, 10, Ease.SNAP).k(20, 0, 0, 0, Ease.SMOOTH);
        turn.part(HEAD).k(0, 0, 0, 0).k(10, -8, 10, 0).k(16, 0, -8, 0, Ease.SNAP).k(20, 0, 0, 0);
        turn.part(BODY).k(0, 0, 0, 0).k(10, 0, 10, 0).k(16, 4, -10, 0, Ease.SNAP).k(20, 0, 0, 0);
        Anims.add(turn);

        // Пандемия: руки в стороны, голова назад — зараза идёт по всем.
        var pandemic = Anim.of("plague.pandemic", 28).fade(3);
        pandemic.part(ARM_R).k(0, 0, 0, 0).k(8, -104, -46, 68, Ease.SMOOTH).k(14, -108, -42, 72, Ease.SHAKE)
                .k(20, -70, -20, 40, Ease.SNAP).k(28, 0, 0, 0, Ease.SMOOTH);
        pandemic.part(ARM_L).k(0, 0, 0, 0).k(8, -104, 46, -68, Ease.SMOOTH).k(14, -108, 42, -72, Ease.SHAKE)
                .k(20, -70, 20, -40, Ease.SNAP).k(28, 0, 0, 0, Ease.SMOOTH);
        pandemic.part(BODY).k(0, 0, 0, 0).k(8, -18, 0, 0).k(14, -20, 0, 0, Ease.SHAKE).k(20, 16, 0, 0, Ease.SNAP)
                .k(28, 0, 0, 0);
        pandemic.part(HEAD).k(0, 0, 0, 0).k(8, -30, 0, 0).k(20, 14, 0, 0, Ease.SNAP).k(28, 0, 0, 0);
        pandemic.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(14, 0, 0, 0, 0, 0.12, 0, Ease.SHAKE)
                .key(20, 0, 0, 0, 0, -0.06, 0, Ease.SNAP).key(28, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(pandemic);
    }

    // ------------------------------------------------------------------ Астромант

    private static void astromancer() {
        // Падающая звезда: указать в небо и перевести руку на точку.
        var falling = Anim.of("astro.falling", 20).fade(2);
        falling.part(ARM_R).k(0, 0, 0, 0).k(6, -178, -10, 14, Ease.SMOOTH).k(10, -174, -8, 12)
                .key(14, -96, 4, 6, 0, 0, -3, Ease.SNAP).k(20, 0, 0, 0, Ease.SMOOTH);
        falling.part(HEAD).k(0, 0, 0, 0).k(6, -28, 0, 0).k(14, 8, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        falling.part(BODY).k(0, 0, 0, 0).k(6, -12, 0, 0).k(14, 10, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        Anims.add(falling);

        // Светило: сложить ладони и отпустить звезду перед собой.
        var lum = Anim.of("astro.luminary", 22).fade(2);
        lum.part(ARM_R).k(0, 0, 0, 0).k(7, -118, 50, 16, Ease.SMOOTH).k(12, -116, 48, 14, Ease.SHAKE)
                .k(16, -92, 10, 10, Ease.SNAP).k(22, 0, 0, 0, Ease.SMOOTH);
        lum.part(ARM_L).k(0, 0, 0, 0).k(7, -118, -50, -16, Ease.SMOOTH).k(12, -116, -48, -14, Ease.SHAKE)
                .k(16, -92, -10, -10, Ease.SNAP).k(22, 0, 0, 0, Ease.SMOOTH);
        lum.part(HEAD).k(0, 0, 0, 0).k(7, 12, 0, 0).k(16, -6, 0, 0, Ease.SNAP).k(22, 0, 0, 0);
        lum.part(BODY).k(0, 0, 0, 0).k(7, 10, 0, 0).k(16, -8, 0, 0, Ease.SNAP).k(22, 0, 0, 0);
        Anims.add(lum);

        // Звёздный луч: рука вытянута, луч держится.
        var beam = Anim.of("astro.beam", 20).fade(2);
        beam.part(ARM_R).k(0, 0, 0, 0).k(4, -70, 30, 24, Ease.SMOOTH).key(8, -94, 4, 6, 0, 0, -3, Ease.SNAP)
                .key(15, -92, 4, 6, 0, 0, -3, Ease.SHAKE).k(20, 0, 0, 0, Ease.SMOOTH);
        beam.part(ARM_L).k(0, 0, 0, 0).k(8, -106, -40, -18, Ease.SMOOTH).k(15, -104, -38, -16).k(20, 0, 0, 0);
        beam.part(BODY).k(0, 0, 0, 0).k(8, 8, -8, 0, Ease.SNAP).k(15, 6, -6, 0).k(20, 0, 0, 0);
        Anims.add(beam);

        // Созвездие: рука чертит линии от звезды к звезде.
        var cons = Anim.of("astro.constellation", 24).fade(2);
        cons.part(ARM_R).k(0, 0, 0, 0).k(5, -120, 50, 26, Ease.SMOOTH).k(10, -80, -40, 50, Ease.LINEAR)
                .k(14, -140, 20, 20, Ease.LINEAR).k(18, -90, -10, 40, Ease.SNAP).k(24, 0, 0, 0, Ease.SMOOTH);
        cons.part(HEAD).k(0, 0, 0, 0).k(10, -14, -10, 0).k(18, -8, 8, 0).k(24, 0, 0, 0);
        cons.part(BODY).k(0, 0, 0, 0).k(10, 0, -12, 0).k(18, 0, 10, 0).k(24, 0, 0, 0);
        Anims.add(cons);

        // Притяжение светил: обе руки сводятся к себе — звёзды тянут врагов.
        var pull = Anim.of("astro.pull", 22).fade(2);
        pull.part(ARM_R).k(0, 0, 0, 0).k(7, -96, -50, 72, Ease.SMOOTH).k(13, -110, 54, 14, Ease.SNAP)
                .k(17, -108, 50, 12).k(22, 0, 0, 0, Ease.SMOOTH);
        pull.part(ARM_L).k(0, 0, 0, 0).k(7, -96, 50, -72, Ease.SMOOTH).k(13, -110, -54, -14, Ease.SNAP)
                .k(17, -108, -50, -12).k(22, 0, 0, 0, Ease.SMOOTH);
        pull.part(BODY).k(0, 0, 0, 0).k(7, -10, 0, 0).k(13, 14, 0, 0, Ease.SNAP).k(22, 0, 0, 0);
        pull.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(13, 0, 0, 0, 0, 0, -0.08, Ease.SNAP)
                .key(22, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(pull);

        // Сверхновая: руки вверх и резко в стороны — все светила лопаются.
        var nova = Anim.of("astro.supernova", 28).fade(3);
        nova.part(ARM_R).k(0, 0, 0, 0).k(9, -170, -20, 10, Ease.SMOOTH).k(15, -168, -18, 8, Ease.SHAKE)
                .k(19, -84, -50, 84, Ease.SNAP).k(24, -82, -48, 82).k(28, 0, 0, 0, Ease.SMOOTH);
        nova.part(ARM_L).k(0, 0, 0, 0).k(9, -170, 20, -10, Ease.SMOOTH).k(15, -168, 18, -8, Ease.SHAKE)
                .k(19, -84, 50, -84, Ease.SNAP).k(24, -82, 48, -82).k(28, 0, 0, 0, Ease.SMOOTH);
        nova.part(BODY).k(0, 0, 0, 0).k(9, -14, 0, 0).k(19, -20, 0, 0, Ease.SNAP).k(28, 0, 0, 0);
        nova.part(HEAD).k(0, 0, 0, 0).k(9, -30, 0, 0).k(19, -34, 0, 0, Ease.SNAP).k(28, 0, 0, 0);
        nova.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(15, 0, 0, 0, 0, 0.1, 0, Ease.SHAKE)
                .key(19, 0, 0, 0, 0, 0.2, 0, Ease.SNAP).key(28, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(nova);
    }

    // ------------------------------------------------------------------ Призыватель

    private static void summoner() {
        // Поднять скелета: рука к земле — из пола лезет помощник.
        var skeleton = Anim.of("summon.skeleton", 22).fade(2);
        Moves.toGround(skeleton, ARM_R, 0, 16);
        skeleton.part(ARM_L).k(0, 0, 0, 0).k(8, -40, -24, -20, Ease.SMOOTH).k(22, 0, 0, 0, Ease.SMOOTH);
        Anims.add(skeleton);

        // Тени-волки: обе ладони к земле, две тени срываются с места.
        var wolves = Anim.of("summon.wolves", 20).fade(2);
        Moves.handDown(wolves, ARM_R, 0, 12);
        Moves.handDown(wolves, ARM_L, 2, 12);
        wolves.part(BODY).k(0, 0, 0, 0).k(7, 30, 0, 0, Ease.SNAP).k(14, 26, 0, 0).k(20, 0, 0, 0, Ease.SMOOTH);
        wolves.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(7, 0, 0, 0, 0, -0.14, 0, Ease.SNAP)
                .key(14, 0, 0, 0, 0, -0.12, 0, Ease.LINEAR).key(20, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(wolves);

        // Приказ: резко указать рукой — вся армия туда.
        var order = Anim.of("summon.order", 15).fade(1);
        Moves.point(order, ARM_R, 0, 15);
        order.part(BODY).k(0, 0, 0, 0).k(7, 10, -10, 0, Ease.SNAP).k(15, 0, 0, 0);
        order.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(7, 0, 0, 0, 0, 0, 0.08, Ease.SNAP)
                .key(15, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(order);

        // Костяной голем: тяжёлый подъём — руки вытягивают его из земли.
        var golem = Anim.of("summon.golem", 30).fade(3);
        golem.part(ARM_R).k(0, 0, 0, 0).k(7, 30, -20, 16, Ease.SMOOTH).k(14, 20, -16, 14, Ease.SHAKE)
                .k(22, -165, -12, 18, Ease.SMOOTH).k(30, 0, 0, 0, Ease.SMOOTH);
        golem.part(ARM_L).k(0, 0, 0, 0).k(7, 30, 20, -16, Ease.SMOOTH).k(14, 20, 16, -14, Ease.SHAKE)
                .k(22, -165, 12, -18, Ease.SMOOTH).k(30, 0, 0, 0, Ease.SMOOTH);
        golem.part(BODY).k(0, 0, 0, 0).k(7, 28, 0, 0).k(14, 24, 0, 0, Ease.SHAKE).k(22, -14, 0, 0, Ease.SMOOTH)
                .k(30, 0, 0, 0);
        golem.part(HEAD).k(0, 0, 0, 0).k(7, 20, 0, 0).k(22, -26, 0, 0).k(30, 0, 0, 0);
        golem.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(7, 0, 0, 0, 0, -0.16, 0, Ease.SMOOTH)
                .key(14, 0, 0, 0, 0.03, -0.14, 0, Ease.SHAKE).key(22, 0, 0, 0, 0, 0.08, 0, Ease.SMOOTH)
                .key(30, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(golem);

        // Разрыв душ: сжать кулаки и развести — помощники лопаются.
        var burst = Anim.of("summon.burst", 18).fade(2);
        burst.part(ARM_R).k(0, 0, 0, 0).k(6, -100, 50, 14, Ease.SMOOTH).k(10, -98, 48, 12)
                .k(13, -74, -46, 70, Ease.SNAP).k(18, 0, 0, 0, Ease.SMOOTH);
        burst.part(ARM_L).k(0, 0, 0, 0).k(6, -100, -50, -14, Ease.SMOOTH).k(10, -98, -48, -12)
                .k(13, -74, 46, -70, Ease.SNAP).k(18, 0, 0, 0, Ease.SMOOTH);
        burst.part(BODY).k(0, 0, 0, 0).k(6, 10, 0, 0).k(13, -12, 0, 0, Ease.SNAP).k(18, 0, 0, 0);
        Anims.add(burst);

        // Легион: руки широко, армия встаёт разом.
        var legion = Anim.of("summon.legion", 32).fade(3);
        legion.part(ARM_R).k(0, 0, 0, 0).k(8, -150, -40, 50, Ease.SMOOTH).k(16, -146, -36, 48, Ease.SHAKE)
                .k(22, -40, -16, 30, Ease.SNAP).k(32, 0, 0, 0, Ease.SMOOTH);
        legion.part(ARM_L).k(0, 0, 0, 0).k(8, -150, 40, -50, Ease.SMOOTH).k(16, -146, 36, -48, Ease.SHAKE)
                .k(22, -40, 16, -30, Ease.SNAP).k(32, 0, 0, 0, Ease.SMOOTH);
        legion.part(BODY).k(0, 0, 0, 0).k(8, -16, 0, 0).k(22, 18, 0, 0, Ease.SNAP).k(32, 0, 0, 0);
        legion.part(HEAD).k(0, 0, 0, 0).k(8, -24, 0, 0).k(22, 12, 0, 0, Ease.SNAP).k(32, 0, 0, 0);
        legion.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(16, 0, 0, 0, 0, 0.12, 0, Ease.SHAKE)
                .key(22, 0, 0, 0, 0, -0.1, 0, Ease.SNAP).key(32, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(legion);
    }

    // ------------------------------------------------------------------ Иллюзионист

    private static void illusionist() {
        // Двойник: отвести руку в сторону — копия выходит из неё.
        var dbl = Anim.of("illusion.double", 18).fade(2);
        dbl.part(ARM_R).k(0, 0, 0, 0).k(6, -95, -40, 50, Ease.SMOOTH).k(10, -80, -66, 76, Ease.SNAP)
                .k(14, -78, -62, 74).k(18, 0, 0, 0, Ease.SMOOTH);
        dbl.part(ARM_L).k(0, 0, 0, 0).k(6, -40, 26, -24, Ease.SMOOTH).k(18, 0, 0, 0, Ease.SMOOTH);
        dbl.part(HEAD).k(0, 0, 0, 0).k(10, 0, -16, 0, Ease.SNAP).k(18, 0, 0, 0);
        Anims.add(dbl);

        // Подмена: быстрый оборот — и ты уже там.
        var swap = Anim.of("illusion.swap", 13).fade(1);
        swap.part(ARM_R).k(0, 0, 0, 0).k(3, -60, 50, 40, Ease.SNAP).k(7, -40, -40, 20, Ease.SNAP)
                .k(13, 0, 0, 0, Ease.SMOOTH);
        swap.part(ARM_L).k(0, 0, 0, 0).k(3, -60, -50, -40, Ease.SNAP).k(7, -40, 40, -20, Ease.SNAP)
                .k(13, 0, 0, 0, Ease.SMOOTH);
        swap.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(7, 0, 180, 0, 0, 0.1, 0, Ease.SNAP)
                .key(11, 0, 360, 0, 0, 0, 0, Ease.SNAP).key(13, 0, 360, 0, 0, 0, 0, Ease.LINEAR);
        Anims.add(swap);

        // Морок: провести ладонью перед своим лицом.
        var charm = Anim.of("illusion.charm", 18).fade(2);
        charm.part(ARM_R).k(0, 0, 0, 0).k(5, -140, 46, 20, Ease.SMOOTH).k(10, -136, -30, 18, Ease.SMOOTH)
                .k(13, -96, -6, 10, Ease.SNAP).k(18, 0, 0, 0, Ease.SMOOTH);
        charm.part(HEAD).k(0, 0, 0, 0).k(5, -6, 10, 0).k(13, 0, -8, 0, Ease.SNAP).k(18, 0, 0, 0);
        charm.part(BODY).k(0, 0, 0, 0).k(10, 0, 8, 0).k(13, 4, -6, 0, Ease.SNAP).k(18, 0, 0, 0);
        Anims.add(charm);

        // Калейдоскоп: руки веером — сразу три копии.
        var kal = Anim.of("illusion.kaleidoscope", 20).fade(2);
        kal.part(ARM_R).k(0, 0, 0, 0).k(6, -110, 40, 16, Ease.SMOOTH).k(11, -86, -56, 80, Ease.SNAP)
                .k(15, -84, -52, 78).k(20, 0, 0, 0, Ease.SMOOTH);
        kal.part(ARM_L).k(0, 0, 0, 0).k(6, -110, -40, -16, Ease.SMOOTH).k(11, -86, 56, -80, Ease.SNAP)
                .k(15, -84, 52, -78).k(20, 0, 0, 0, Ease.SMOOTH);
        kal.part(BODY).k(0, 0, 0, 0).k(6, 8, 0, 0).k(11, -10, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        kal.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(11, 0, 0, 0, 0, 0.08, 0, Ease.SNAP)
                .key(20, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(kal);

        // Отражённый удар: щелчок пальцами — двойники повторяют удар.
        var strike = Anim.of("illusion.strike", 13).fade(1);
        strike.part(ARM_R).k(0, 0, 0, 0).k(4, -120, 30, 24, Ease.SMOOTH).k(6, -112, 10, 14, Ease.SNAP)
                .k(9, -118, 26, 22, Ease.SNAP).k(13, 0, 0, 0, Ease.SMOOTH);
        strike.part(HEAD).k(0, 0, 0, 0).k(6, -8, 0, 0, Ease.SNAP).k(13, 0, 0, 0);
        Anims.add(strike);

        // Исчезновение: руки вверх — все двойники лопаются, иллюзионист растворяется.
        var vanish = Anim.of("illusion.vanish", 20).fade(2);
        vanish.part(ARM_R).k(0, 0, 0, 0).k(6, -168, -16, 14, Ease.SNAP).k(10, -120, -46, 66, Ease.SNAP)
                .k(20, 0, 0, 0, Ease.SMOOTH);
        vanish.part(ARM_L).k(0, 0, 0, 0).k(6, -168, 16, -14, Ease.SNAP).k(10, -120, 46, -66, Ease.SNAP)
                .k(20, 0, 0, 0, Ease.SMOOTH);
        vanish.part(BODY).k(0, 0, 0, 0).k(6, -14, 0, 0, Ease.SNAP).k(10, 12, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        vanish.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(6, 0, 0, 0, 0, 0.12, 0, Ease.SNAP)
                .key(14, 0, 0, 0, 0, -0.3, 0, Ease.SMOOTH).key(20, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(vanish);
    }

    // ------------------------------------------------------------------ Чернокнижник

    private static void warlock() {
        // Проклятие слабости: кисть выворачивается книзу.
        var curse = Anim.of("warlock.curse", 16).fade(2);
        curse.part(ARM_R).k(0, 0, 0, 0).k(5, -96, 24, 14, Ease.SMOOTH).k(10, -80, -10, 30, Ease.SNAP)
                .k(12, -78, -8, 28).k(16, 0, 0, 0, Ease.SMOOTH);
        curse.part(HEAD).k(0, 0, 0, 0).k(10, 10, 6, 0, Ease.SNAP).k(16, 0, 0, 0);
        curse.part(BODY).k(0, 0, 0, 0).k(10, 8, -8, 0, Ease.SNAP).k(16, 0, 0, 0);
        Anims.add(curse);

        // Порча: тянущий жест пальцем — гниль ползёт.
        var cor = Anim.of("warlock.corruption", 18).fade(2);
        cor.part(ARM_R).k(0, 0, 0, 0).k(5, -100, 16, 10, Ease.SMOOTH).k(11, -94, -6, 20, Ease.SMOOTH)
                .k(14, -86, -16, 28, Ease.SNAP).k(18, 0, 0, 0, Ease.SMOOTH);
        cor.part(ARM_L).k(0, 0, 0, 0).k(5, -34, -22, -18, Ease.SMOOTH).k(18, 0, 0, 0, Ease.SMOOTH);
        cor.part(BODY).k(0, 0, 0, 0).k(11, 6, -6, 0).k(18, 0, 0, 0);
        Anims.add(cor);

        // Пакт: полоснуть себя по ладони — своя кровь в силу.
        var pact = Anim.of("warlock.pact", 20).fade(2);
        pact.part(ARM_L).k(0, 0, 0, 0).k(5, -112, -44, -18, Ease.SMOOTH).k(14, -108, -40, -16)
                .k(20, 0, 0, 0, Ease.SMOOTH);
        pact.part(ARM_R).k(0, 0, 0, 0).k(5, -120, 56, 12, Ease.SMOOTH).k(9, -100, 24, 26, Ease.SNAP)
                .k(12, -124, 58, 10, Ease.SNAP).k(20, 0, 0, 0, Ease.SMOOTH);
        pact.part(HEAD).k(0, 0, 0, 0).k(7, 24, 0, 0).k(14, -10, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        pact.part(BODY).k(0, 0, 0, 0).k(7, 12, 0, 0).k(14, -8, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        pact.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(9, 0, 0, 0, 0, -0.05, 0, Ease.SNAP)
                .key(14, 0, 0, 0, 0, 0.06, 0, Ease.SNAP).key(20, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(pact);

        // Кровавый круг: обвести круг на полу своей кровью.
        var circle = Anim.of("warlock.circle", 24).fade(2);
        circle.part(ARM_R).k(0, 0, 0, 0).k(5, -58, -40, 36, Ease.SMOOTH).k(10, -50, 0, 22, Ease.LINEAR)
                .k(15, -58, 40, 36, Ease.LINEAR).k(19, -40, 10, 24, Ease.SNAP).k(24, 0, 0, 0, Ease.SMOOTH);
        circle.part(BODY).k(0, 0, 0, 0).k(5, 24, -12, 0, Ease.SMOOTH).k(15, 24, 12, 0, Ease.LINEAR)
                .k(19, 20, 0, 0).k(24, 0, 0, 0);
        circle.part(HEAD).k(0, 0, 0, 0).k(10, 22, 0, 0).k(19, 18, 0, 0).k(24, 0, 0, 0);
        circle.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(10, 0, 0, 0, 0, -0.12, 0, Ease.SMOOTH)
                .key(19, 0, 0, 0, 0, -0.1, 0, Ease.LINEAR).key(24, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(circle);

        // Щупальца бездны: обе руки в пол — из него лезут щупальца.
        var tent = Anim.of("warlock.tentacles", 22).fade(2);
        Moves.handDown(tent, ARM_R, 0, 14);
        Moves.handDown(tent, ARM_L, 0, 14);
        tent.part(BODY).k(0, 0, 0, 0).k(8, 34, 0, 0, Ease.SNAP).k(14, 30, 0, 0).k(22, 0, 0, 0, Ease.SMOOTH);
        tent.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(8, 0, 0, 0, 0, -0.16, 0, Ease.SNAP)
                .key(14, 0, 0, 0, 0, -0.14, 0, Ease.LINEAR).key(22, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(tent);

        // Жертвенный приговор: кинжал к себе — и весь удар в цель.
        var sac = Anim.of("warlock.sacrifice", 30).fade(3);
        sac.part(ARM_R).k(0, 0, 0, 0).k(8, -150, 60, 10, Ease.SMOOTH).k(13, -128, 40, 16, Ease.SNAP)
                .k(17, -126, 38, 14, Ease.SHAKE).key(22, -96, 0, 4, 0, 0, -5, Ease.SNAP)
                .k(30, 0, 0, 0, Ease.SMOOTH);
        sac.part(ARM_L).k(0, 0, 0, 0).k(8, -60, -40, -30, Ease.SMOOTH).k(17, -40, -20, -20)
                .k(22, -70, -10, -14, Ease.SNAP).k(30, 0, 0, 0, Ease.SMOOTH);
        sac.part(BODY).k(0, 0, 0, 0).k(8, -12, 0, 0).k(13, 16, 0, 0, Ease.SNAP).k(17, 14, 0, 0, Ease.SHAKE)
                .k(22, -10, 0, 0, Ease.SNAP).k(30, 0, 0, 0);
        sac.part(HEAD).k(0, 0, 0, 0).k(8, -16, 0, 0).k(13, 22, 0, 0, Ease.SNAP).k(22, -14, 0, 0, Ease.SNAP)
                .k(30, 0, 0, 0);
        sac.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(13, 0, 0, 0, 0, -0.08, 0, Ease.SNAP)
                .key(17, 0, 0, 2, 0.03, -0.08, 0, Ease.SHAKE).key(22, 0, 0, 0, 0, 0.04, 0.12, Ease.SNAP)
                .key(30, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(sac);
    }

    // ------------------------------------------------------------------ Разломщик

    private static void rift() {
        // Разлом: взять воздух двумя руками и разорвать.
        var r = Anim.of("rift.rift", 20).fade(2);
        r.part(ARM_R).k(0, 0, 0, 0).k(6, -100, 36, 14, Ease.SMOOTH).k(10, -98, 34, 12)
                .k(14, -90, -40, 60, Ease.SNAP).k(20, 0, 0, 0, Ease.SMOOTH);
        r.part(ARM_L).k(0, 0, 0, 0).k(6, -100, -36, -14, Ease.SMOOTH).k(10, -98, -34, -12)
                .k(14, -90, 40, -60, Ease.SNAP).k(20, 0, 0, 0, Ease.SMOOTH);
        r.part(BODY).k(0, 0, 0, 0).k(6, 8, 0, 0).k(14, -10, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        r.part(HEAD).k(0, 0, 0, 0).k(14, -10, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        Anims.add(r);

        // Шаг в пустоту: шагнуть в трещину и выйти с удара.
        var step = Anim.of("rift.step", 16).fade(1);
        Moves.sink(step, 0, 6);
        Moves.palm(step, ARM_R, 7, 9);
        step.part(ROOT).key(10, 0, 180, 0, 0, 0, 0, Ease.SNAP).key(14, 0, 360, 0, 0, 0, 0, Ease.SNAP);
        Anims.add(step);

        var mark = Anim.of("rift.mark", 13).fade(1);
        Moves.point(mark, ARM_R, 0, 13);
        Anims.add(mark);

        // Схлопывание: хлопок ладонями — все разломы закрываются.
        var collapse = Anim.of("rift.collapse", 18).fade(2);
        collapse.part(ARM_R).k(0, 0, 0, 0).k(6, -92, -56, 70, Ease.SMOOTH).k(9, -104, 52, 12, Ease.SNAP)
                .k(12, -102, 50, 10).k(18, 0, 0, 0, Ease.SMOOTH);
        collapse.part(ARM_L).k(0, 0, 0, 0).k(6, -92, 56, -70, Ease.SMOOTH).k(9, -104, -52, -12, Ease.SNAP)
                .k(12, -102, -50, -10).k(18, 0, 0, 0, Ease.SMOOTH);
        collapse.part(BODY).k(0, 0, 0, 0).k(6, -8, 0, 0).k(9, 14, 0, 0, Ease.SNAP).k(18, 0, 0, 0);
        collapse.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(9, 0, 0, 0, 0, -0.06, 0, Ease.SNAP)
                .key(18, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(collapse);

        // Зев: руки книзу и в стороны — пол открывается.
        var maw = Anim.of("rift.maw", 24).fade(3);
        maw.part(ARM_R).k(0, 0, 0, 0).k(7, -126, 40, 18, Ease.SMOOTH).k(13, -30, -26, 44, Ease.SNAP)
                .k(18, -28, -24, 42).k(24, 0, 0, 0, Ease.SMOOTH);
        maw.part(ARM_L).k(0, 0, 0, 0).k(7, -126, -40, -18, Ease.SMOOTH).k(13, -30, 26, -44, Ease.SNAP)
                .k(18, -28, 24, -42).k(24, 0, 0, 0, Ease.SMOOTH);
        maw.part(BODY).k(0, 0, 0, 0).k(7, -10, 0, 0).k(13, 24, 0, 0, Ease.SNAP).k(18, 22, 0, 0).k(24, 0, 0, 0);
        maw.part(HEAD).k(0, 0, 0, 0).k(13, 18, 0, 0, Ease.SNAP).k(24, 0, 0, 0);
        maw.add(LEG_R).k(0, 0, 0, 0).k(13, -16, 0, 10, Ease.SNAP).k(18, -14, 0, 8).k(24, 0, 0, 0);
        Anims.add(maw);

        // Изнанка: руки крест-накрест, разломщик наполовину не здесь.
        var inside = Anim.of("rift.inside", 26).fade(3);
        inside.part(ARM_R).k(0, 0, 0, 0).k(6, -98, 52, 14, Ease.SNAP).k(14, -94, 48, 12, Ease.SHAKE)
                .k(21, -98, 52, 14, Ease.SHAKE).k(26, 0, 0, 0, Ease.SMOOTH);
        inside.part(ARM_L).k(0, 0, 0, 0).k(6, -98, -52, -14, Ease.SNAP).k(14, -94, -48, -12, Ease.SHAKE)
                .k(21, -98, -52, -14, Ease.SHAKE).k(26, 0, 0, 0, Ease.SMOOTH);
        inside.part(BODY).k(0, 0, 0, 0).k(6, 10, 0, 0, Ease.SNAP).k(21, 8, 0, 0).k(26, 0, 0, 0);
        inside.part(HEAD).k(0, 0, 0, 0).k(6, -8, 0, 0).k(21, -6, 0, 0).k(26, 0, 0, 0);
        inside.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(6, 0, 0, 0, 0, 0.06, 0, Ease.SNAP)
                .key(14, 0, 0, 3, 0.04, 0.06, 0, Ease.SHAKE).key(21, 0, 0, -3, -0.04, 0.06, 0, Ease.SHAKE)
                .key(26, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(inside);
    }
}
