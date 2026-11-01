package com.andrey.litrpg.anim;

import com.andrey.litrpg.anim.Anim.Ease;

import static com.andrey.litrpg.anim.Anim.Part.ARM_L;
import static com.andrey.litrpg.anim.Anim.Part.ARM_R;
import static com.andrey.litrpg.anim.Anim.Part.BODY;
import static com.andrey.litrpg.anim.Anim.Part.HEAD;
import static com.andrey.litrpg.anim.Anim.Part.LEG_R;
import static com.andrey.litrpg.anim.Anim.Part.ROOT;

/** Движения поддержки: Лекарь, Хронолог, Теневой жрец, Друид, Шаман, Знаменосец. */
final class AnimsSupport {
    private AnimsSupport() {
    }

    static void register() {
        cleric();
        chronolog();
        shadowPriest();
        druid();
        shaman();
        bannerman();
    }

    // ------------------------------------------------------------------ Лекарь

    private static void cleric() {
        var bandage = Anim.of("cleric.bandage", 16).fade(2);
        Moves.mend(bandage, ARM_R, 0, 16);
        Anims.add(bandage);

        // Молитва: две секунды с ладонями у груди.
        var prayerCast = Anim.of("cleric.prayer.cast", 24).fade(3).loop();
        prayerCast.part(ARM_R).k(0, -116, 47, 11).k(8, -120, 50, 13, Ease.SMOOTH).k(16, -116, 47, 11, Ease.SMOOTH)
                .k(24, -116, 47, 11);
        prayerCast.part(ARM_L).k(0, -116, -47, -11).k(8, -120, -50, -13, Ease.SMOOTH)
                .k(16, -116, -47, -11, Ease.SMOOTH).k(24, -116, -47, -11);
        prayerCast.part(HEAD).k(0, 20, 0, 0).k(12, 24, 0, 0).k(24, 20, 0, 0);
        prayerCast.part(BODY).k(0, 6, 0, 0).k(12, 8, 0, 0).k(24, 6, 0, 0);
        Anims.add(prayerCast);

        // И сама молитва: руки раскрываются вверх, свет уходит в отряд.
        var prayer = Anim.of("cleric.prayer", 20).fade(2);
        prayer.part(ARM_R).k(0, -116, 47, 11).k(6, -160, 10, 24, Ease.SNAP).k(12, -140, -30, 54, Ease.SMOOTH)
                .k(20, 0, 0, 0, Ease.SMOOTH);
        prayer.part(ARM_L).k(0, -116, -47, -11).k(6, -160, -10, -24, Ease.SNAP).k(12, -140, 30, -54, Ease.SMOOTH)
                .k(20, 0, 0, 0, Ease.SMOOTH);
        prayer.part(HEAD).k(0, 20, 0, 0).k(6, -24, 0, 0, Ease.SNAP).k(12, -18, 0, 0).k(20, 0, 0, 0);
        prayer.part(BODY).k(0, 6, 0, 0).k(6, -14, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        prayer.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(6, 0, 0, 0, 0, 0.1, 0, Ease.SNAP)
                .key(20, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(prayer);

        // Святилище: обвести круг и опустить ладони — встаёт убежище.
        var sanctuary = Anim.of("cleric.sanctuary", 24).fade(3);
        sanctuary.part(ARM_R).k(0, 0, 0, 0).k(7, -150, -30, 30, Ease.SMOOTH).k(12, -150, 20, 30, Ease.LINEAR)
                .k(17, -56, 10, 30, Ease.SNAP).k(24, 0, 0, 0, Ease.SMOOTH);
        sanctuary.part(ARM_L).k(0, 0, 0, 0).k(7, -150, 30, -30, Ease.SMOOTH).k(12, -150, -20, -30, Ease.LINEAR)
                .k(17, -56, -10, -30, Ease.SNAP).k(24, 0, 0, 0, Ease.SMOOTH);
        sanctuary.part(HEAD).k(0, 0, 0, 0).k(7, -20, 0, 0).k(17, 10, 0, 0, Ease.SNAP).k(24, 0, 0, 0);
        sanctuary.part(BODY).k(0, 0, 0, 0).k(7, -10, 0, 0).k(17, 12, 0, 0, Ease.SNAP).k(24, 0, 0, 0);
        Anims.add(sanctuary);

        // Чудо: обе руки к небу, свет заливает отряд.
        var miracle = Anim.of("cleric.miracle", 30).fade(3);
        miracle.part(ARM_R).k(0, 0, 0, 0).k(9, -178, -12, 14, Ease.SMOOTH).k(16, -175, -10, 12, Ease.SHAKE)
                .k(22, -150, -34, 50, Ease.SMOOTH).k(30, 0, 0, 0, Ease.SMOOTH);
        miracle.part(ARM_L).k(0, 0, 0, 0).k(9, -178, 12, -14, Ease.SMOOTH).k(16, -175, 10, -12, Ease.SHAKE)
                .k(22, -150, 34, -50, Ease.SMOOTH).k(30, 0, 0, 0, Ease.SMOOTH);
        miracle.part(HEAD).k(0, 0, 0, 0).k(9, -34, 0, 0).k(22, -26, 0, 0).k(30, 0, 0, 0);
        miracle.part(BODY).k(0, 0, 0, 0).k(9, -18, 0, 0).k(22, -12, 0, 0).k(30, 0, 0, 0);
        miracle.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(16, 0, 0, 0, 0, 0.16, 0, Ease.SMOOTH)
                .key(22, 0, 0, 0, 0, 0.12, 0, Ease.SMOOTH).key(30, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(miracle);
    }

    // ------------------------------------------------------------------ Хронолог

    private static void chronolog() {
        // Полный откат: рука чертит круг против хода — время идёт назад.
        var rewind = Anim.of("chrono.rewind", 20).fade(2);
        rewind.part(ARM_R).k(0, 0, 0, 0).k(5, -120, 40, 22, Ease.SMOOTH).k(9, -150, 0, 18, Ease.LINEAR)
                .k(13, -120, -40, 22, Ease.LINEAR).k(16, -96, -8, 10, Ease.SNAP).k(20, 0, 0, 0, Ease.SMOOTH);
        rewind.part(ARM_L).k(0, 0, 0, 0).k(5, -40, -24, -20, Ease.SMOOTH).k(16, -30, -14, -14).k(20, 0, 0, 0);
        rewind.part(HEAD).k(0, 0, 0, 0).k(9, -10, 8, 0).k(16, 0, -6, 0, Ease.SNAP).k(20, 0, 0, 0);
        Anims.add(rewind);

        // Остановка: ладонь вперёд — «стой».
        var stasis = Anim.of("chrono.stasis", 16).fade(2);
        stasis.part(ARM_R).k(0, 0, 0, 0).k(4, -60, 30, 20, Ease.SMOOTH).key(7, -98, 0, 4, 0, 0, -3, Ease.SNAP)
                .k(12, -96, 0, 4).k(16, 0, 0, 0, Ease.SMOOTH);
        stasis.part(BODY).k(0, 0, 0, 0).k(7, 10, 0, 0, Ease.SNAP).k(12, 8, 0, 0).k(16, 0, 0, 0);
        stasis.part(HEAD).k(0, 0, 0, 0).k(7, -6, 0, 0, Ease.SNAP).k(16, 0, 0, 0);
        Anims.add(stasis);

        // Ускорить: два быстрых щелчка по отряду.
        var haste = Anim.of("chrono.haste", 14).fade(1);
        haste.part(ARM_R).k(0, 0, 0, 0).k(3, -110, 30, 20, Ease.SMOOTH).k(5, -96, 8, 8, Ease.SNAP)
                .k(8, -110, 30, 20, Ease.SNAP).k(10, -96, 8, 8, Ease.SNAP).k(14, 0, 0, 0, Ease.SMOOTH);
        haste.part(BODY).k(0, 0, 0, 0).k(5, 6, -6, 0, Ease.SNAP).k(10, 6, -6, 0, Ease.SNAP).k(14, 0, 0, 0);
        Anims.add(haste);

        // Петля времени: срабатывает сама — вокруг хронолога на миг идёт кольцо времени.
        var loop = Anim.of("chrono.loop", 18).fade(2);
        loop.part(ARM_R).k(0, 0, 0, 0).k(4, -140, 30, 24, Ease.SNAP).k(9, -140, -30, 24, Ease.LINEAR)
                .k(13, -120, 0, 30, Ease.SMOOTH).k(18, 0, 0, 0, Ease.SMOOTH);
        loop.part(ARM_L).k(0, 0, 0, 0).k(4, -140, -30, -24, Ease.SNAP).k(9, -140, 30, -24, Ease.LINEAR)
                .k(13, -120, 0, -30, Ease.SMOOTH).k(18, 0, 0, 0, Ease.SMOOTH);
        loop.part(HEAD).k(0, 0, 0, 0).k(4, -18, 0, 0, Ease.SNAP).k(13, -10, 0, 0).k(18, 0, 0, 0);
        loop.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(4, 0, 0, 0, 0, 0.1, 0, Ease.SNAP)
                .key(13, 0, -360, 0, 0, 0.08, 0, Ease.SMOOTH).key(18, 0, -360, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(loop);

        // Песочные часы: поставить часы на пол и перевернуть.
        var hourglass = Anim.of("chrono.hourglass", 22).fade(2);
        Moves.toGround(hourglass, ARM_R, 0, 14);
        hourglass.part(ARM_L).k(0, 0, 0, 0).k(7, -120, -40, -20, Ease.SMOOTH).k(13, -40, -14, -16, Ease.SNAP)
                .k(22, 0, 0, 0, Ease.SMOOTH);
        Anims.add(hourglass);

        // Перемотка: широкий жест назад — весь отряд откатывает раны.
        var replay = Anim.of("chrono.replay", 24).fade(3);
        replay.part(ARM_R).k(0, 0, 0, 0).k(7, -90, -60, 70, Ease.SMOOTH).k(13, -110, 60, 16, Ease.SMOOTH)
                .k(17, -90, -20, 50, Ease.SNAP).k(24, 0, 0, 0, Ease.SMOOTH);
        replay.part(ARM_L).k(0, 0, 0, 0).k(7, -90, 60, -70, Ease.SMOOTH).k(13, -110, -60, -16, Ease.SMOOTH)
                .k(17, -90, 20, -50, Ease.SNAP).k(24, 0, 0, 0, Ease.SMOOTH);
        replay.part(BODY).k(0, 0, 0, 0).k(7, 0, -18, 0).k(13, 0, 18, 0).k(17, -8, 0, 0, Ease.SNAP).k(24, 0, 0, 0);
        replay.part(HEAD).k(0, 0, 0, 0).k(7, 0, -12, 0).k(13, 0, 12, 0).k(24, 0, 0, 0);
        replay.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(13, 0, -30, 0, 0, 0.06, 0, Ease.SMOOTH)
                .key(17, 0, 0, 0, 0, 0.04, 0, Ease.SNAP).key(24, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(replay);
    }

    // ------------------------------------------------------------------ Теневой жрец

    private static void shadowPriest() {
        var lash = Anim.of("priest.lash", 16).fade(2);
        Moves.lash(lash, ARM_R, 0, 16);
        Anims.add(lash);

        // Искупление: одна рука к союзнику, другая к врагу — боль идёт в лечение.
        var aton = Anim.of("priest.atonement", 20).fade(2);
        aton.part(ARM_R).k(0, 0, 0, 0).k(6, -94, -40, 20, Ease.SMOOTH).k(12, -92, -38, 18, Ease.SHAKE)
                .k(20, 0, 0, 0, Ease.SMOOTH);
        aton.part(ARM_L).k(0, 0, 0, 0).k(6, -94, 40, -20, Ease.SMOOTH).k(12, -92, 38, -18, Ease.SHAKE)
                .k(20, 0, 0, 0, Ease.SMOOTH);
        aton.part(HEAD).k(0, 0, 0, 0).k(6, 0, -14, 0).k(12, 0, 12, 0).k(20, 0, 0, 0);
        aton.part(BODY).k(0, 0, 0, 0).k(6, -6, 0, 0).k(20, 0, 0, 0);
        Anims.add(aton);

        // Сумерки: руки низко в стороны — зона, где чужая боль лечит своих.
        var twilight = Anim.of("priest.twilight", 22).fade(3);
        twilight.part(ARM_R).k(0, 0, 0, 0).k(7, -120, 40, 18, Ease.SMOOTH).k(12, -36, -20, 50, Ease.SNAP)
                .k(17, -34, -18, 48, Ease.SHAKE).k(22, 0, 0, 0, Ease.SMOOTH);
        twilight.part(ARM_L).k(0, 0, 0, 0).k(7, -120, -40, -18, Ease.SMOOTH).k(12, -36, 20, -50, Ease.SNAP)
                .k(17, -34, 18, -48, Ease.SHAKE).k(22, 0, 0, 0, Ease.SMOOTH);
        twilight.part(BODY).k(0, 0, 0, 0).k(7, -10, 0, 0).k(12, 14, 0, 0, Ease.SNAP).k(22, 0, 0, 0);
        twilight.part(HEAD).k(0, 0, 0, 0).k(7, -14, 0, 0).k(17, 8, 0, 0).k(22, 0, 0, 0);
        Anims.add(twilight);

        // Сделка: прижать кулак к груди — своё здоровье в чужое лечение.
        var bargain = Anim.of("priest.bargain", 20).fade(2);
        bargain.part(ARM_R).k(0, 0, 0, 0).k(5, -130, 60, 12, Ease.SMOOTH).k(9, -126, 56, 10, Ease.SNAP)
                .k(14, -110, 30, 26, Ease.SMOOTH).k(20, 0, 0, 0, Ease.SMOOTH);
        bargain.part(ARM_L).k(0, 0, 0, 0).k(5, -40, -30, -20, Ease.SMOOTH).k(14, -86, -16, -12, Ease.SNAP)
                .k(20, 0, 0, 0, Ease.SMOOTH);
        bargain.part(HEAD).k(0, 0, 0, 0).k(9, 22, 0, 0, Ease.SNAP).k(14, -8, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        bargain.part(BODY).k(0, 0, 0, 0).k(9, 16, 0, 0, Ease.SNAP).k(14, -6, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        Anims.add(bargain);

        // Покров тьмы: обернуться тьмой, как плащом.
        var veil = Anim.of("priest.veil", 20).fade(2);
        veil.part(ARM_R).k(0, 0, 0, 0).k(6, -100, -50, 60, Ease.SMOOTH).k(11, -108, 58, 14, Ease.SNAP)
                .k(16, -106, 54, 12).k(20, 0, 0, 0, Ease.SMOOTH);
        veil.part(ARM_L).k(0, 0, 0, 0).k(6, -100, 50, -60, Ease.SMOOTH).k(11, -108, -58, -14, Ease.SNAP)
                .k(16, -106, -54, -12).k(20, 0, 0, 0, Ease.SMOOTH);
        veil.part(BODY).k(0, 0, 0, 0).k(11, 12, 0, 0, Ease.SNAP).k(16, 10, 0, 0).k(20, 0, 0, 0);
        veil.part(HEAD).k(0, 0, 0, 0).k(11, 14, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        Anims.add(veil);

        // Пожирание тени: тянуть обеими руками к себе — чужие раны в здоровье отряда.
        var devour = Anim.of("priest.devour", 26).fade(3);
        devour.part(ARM_R).k(0, 0, 0, 0).k(8, -94, -56, 74, Ease.SMOOTH).k(13, -92, -54, 72, Ease.SHAKE)
                .k(18, -120, 56, 14, Ease.SNAP).k(26, 0, 0, 0, Ease.SMOOTH);
        devour.part(ARM_L).k(0, 0, 0, 0).k(8, -94, 56, -74, Ease.SMOOTH).k(13, -92, 54, -72, Ease.SHAKE)
                .k(18, -120, -56, -14, Ease.SNAP).k(26, 0, 0, 0, Ease.SMOOTH);
        devour.part(BODY).k(0, 0, 0, 0).k(8, -12, 0, 0).k(18, 16, 0, 0, Ease.SNAP).k(26, 0, 0, 0);
        devour.part(HEAD).k(0, 0, 0, 0).k(8, -16, 0, 0).k(18, 12, 0, 0, Ease.SNAP).k(26, 0, 0, 0);
        devour.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(13, 0, 0, 0, 0, 0.08, 0, Ease.SHAKE)
                .key(18, 0, 0, 0, 0, -0.06, 0, Ease.SNAP).key(26, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(devour);
    }

    // ------------------------------------------------------------------ Друид

    private static void druid() {
        // Цветок жизни: бережно опустить ладонь — под союзником распускается цветок.
        var flower = Anim.of("druid.flower", 20).fade(3);
        flower.part(ARM_R).k(0, 0, 0, 0).k(6, -110, 20, 20, Ease.SMOOTH).k(12, -58, 8, 14, Ease.SMOOTH)
                .k(16, -56, 6, 12).k(20, 0, 0, 0, Ease.SMOOTH);
        flower.part(ARM_L).k(0, 0, 0, 0).k(6, -40, -20, -18, Ease.SMOOTH).k(20, 0, 0, 0, Ease.SMOOTH);
        flower.part(HEAD).k(0, 0, 0, 0).k(12, 14, 0, 0).k(20, 0, 0, 0);
        flower.part(BODY).k(0, 0, 0, 0).k(12, 12, 0, 0).k(20, 0, 0, 0);
        Anims.add(flower);

        // Лоза: взмах кистью — лоза перескакивает дальше.
        var vine = Anim.of("druid.vine", 18).fade(2);
        vine.part(ARM_R).k(0, 0, 0, 0).k(5, -70, -40, 40, Ease.SMOOTH).k(9, -100, 40, 14, Ease.SNAP)
                .k(12, -86, 10, 24, Ease.SMOOTH).k(18, 0, 0, 0, Ease.SMOOTH);
        vine.part(BODY).k(0, 0, 0, 0).k(5, 0, -14, 0).k(9, 0, 14, 0, Ease.SNAP).k(18, 0, 0, 0);
        Anims.add(vine);

        // Шипы: ладонь книзу, из земли идут колючки.
        var thorns = Anim.of("druid.thorns", 16).fade(2);
        thorns.part(ARM_R).k(0, 0, 0, 0).k(5, -120, -16, 24, Ease.SMOOTH).k(9, -34, 8, 14, Ease.SNAP)
                .k(12, -32, 6, 12).k(16, 0, 0, 0, Ease.SMOOTH);
        thorns.part(BODY).k(0, 0, 0, 0).k(9, 22, 0, 0, Ease.SNAP).k(16, 0, 0, 0);
        thorns.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(9, 0, 0, 0, 0, -0.08, 0, Ease.SNAP)
                .key(16, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(thorns);

        // Живое древо: поднять руки — из пола растёт дерево.
        var tree = Anim.of("druid.tree", 28).fade(3);
        tree.part(ARM_R).k(0, 0, 0, 0).k(7, 26, -18, 14, Ease.SMOOTH).k(14, -60, -20, 36, Ease.SMOOTH)
                .k(21, -160, -14, 20, Ease.SMOOTH).k(28, 0, 0, 0, Ease.SMOOTH);
        tree.part(ARM_L).k(0, 0, 0, 0).k(7, 26, 18, -14, Ease.SMOOTH).k(14, -60, 20, -36, Ease.SMOOTH)
                .k(21, -160, 14, -20, Ease.SMOOTH).k(28, 0, 0, 0, Ease.SMOOTH);
        tree.part(BODY).k(0, 0, 0, 0).k(7, 26, 0, 0).k(14, 6, 0, 0).k(21, -14, 0, 0).k(28, 0, 0, 0);
        tree.part(HEAD).k(0, 0, 0, 0).k(7, 18, 0, 0).k(21, -24, 0, 0).k(28, 0, 0, 0);
        tree.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(7, 0, 0, 0, 0, -0.14, 0, Ease.SMOOTH)
                .key(21, 0, 0, 0, 0, 0.06, 0, Ease.SMOOTH).key(28, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(tree);

        // Цветение: раскрыть ладони вверх — все лечения вспыхивают разом.
        var bloom = Anim.of("druid.bloom", 20).fade(2);
        bloom.part(ARM_R).k(0, 0, 0, 0).k(6, -110, 46, 14, Ease.SMOOTH).k(11, -92, -30, 60, Ease.BACK)
                .k(15, -90, -28, 58).k(20, 0, 0, 0, Ease.SMOOTH);
        bloom.part(ARM_L).k(0, 0, 0, 0).k(6, -110, -46, -14, Ease.SMOOTH).k(11, -92, 30, -60, Ease.BACK)
                .k(15, -90, 28, -58).k(20, 0, 0, 0, Ease.SMOOTH);
        bloom.part(BODY).k(0, 0, 0, 0).k(6, 10, 0, 0).k(11, -12, 0, 0, Ease.BACK).k(20, 0, 0, 0);
        bloom.part(HEAD).k(0, 0, 0, 0).k(6, 12, 0, 0).k(11, -16, 0, 0, Ease.BACK).k(20, 0, 0, 0);
        Anims.add(bloom);

        // Весна: широкий круг руками — всё расцветает быстрее.
        var spring = Anim.of("druid.spring", 26).fade(3);
        spring.part(ARM_R).k(0, 0, 0, 0).k(7, -60, -40, 54, Ease.SMOOTH).k(13, -150, -20, 26, Ease.LINEAR)
                .k(19, -110, 40, 16, Ease.SMOOTH).k(26, 0, 0, 0, Ease.SMOOTH);
        spring.part(ARM_L).k(0, 0, 0, 0).k(7, -60, 40, -54, Ease.SMOOTH).k(13, -150, 20, -26, Ease.LINEAR)
                .k(19, -110, -40, -16, Ease.SMOOTH).k(26, 0, 0, 0, Ease.SMOOTH);
        spring.part(HEAD).k(0, 0, 0, 0).k(13, -24, 0, 0).k(19, -8, 0, 0).k(26, 0, 0, 0);
        spring.part(BODY).k(0, 0, 0, 0).k(13, -14, 0, 0).k(19, 4, 0, 0).k(26, 0, 0, 0);
        spring.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(13, 0, 0, 0, 0, 0.1, 0, Ease.SMOOTH)
                .key(19, 0, 0, 0, 0, 0.06, 0, Ease.SMOOTH).key(26, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(spring);
    }

    // ------------------------------------------------------------------ Шаман

    private static void shaman() {
        // Тотем исцеления: поставить тотем и отпустить.
        var heal = Anim.of("shaman.totem_heal", 22).fade(2);
        Moves.toGround(heal, ARM_R, 0, 15);
        heal.part(ARM_L).k(0, 0, 0, 0).k(8, -50, -26, -22, Ease.SMOOTH).k(14, -30, -14, -14).k(22, 0, 0, 0, Ease.SMOOTH);
        Anims.add(heal);

        // Тотем бури: вбить тотем в пол ударом.
        var storm = Anim.of("shaman.totem_storm", 22).fade(2);
        storm.part(ARM_R).k(0, 0, 0, 0).k(7, -175, -16, 20, Ease.SMOOTH).k(11, -14, 6, 6, Ease.SNAP)
                .k(16, -12, 4, 5).k(22, 0, 0, 0, Ease.SMOOTH);
        storm.part(ARM_L).k(0, 0, 0, 0).k(7, -70, 30, -24, Ease.SMOOTH).k(11, -24, 12, -12, Ease.SNAP)
                .k(22, 0, 0, 0, Ease.SMOOTH);
        storm.part(BODY).k(0, 0, 0, 0).k(7, -12, 0, 0).k(11, 28, 0, 0, Ease.SNAP).k(16, 24, 0, 0).k(22, 0, 0, 0);
        storm.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(7, 0, 0, 0, 0, 0.08, 0, Ease.SMOOTH)
                .key(11, 0, 0, 0, 0, -0.12, 0, Ease.SNAP).key(22, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(storm);

        // Молния: рука вверх — и вперёд.
        var lightning = Anim.of("shaman.lightning", 16).fade(2);
        lightning.part(ARM_R).k(0, 0, 0, 0).k(5, -172, -12, 16, Ease.SMOOTH).k(8, -168, -10, 14)
                .key(11, -96, 4, 6, 0, 0, -3, Ease.SNAP).k(16, 0, 0, 0, Ease.SMOOTH);
        lightning.part(HEAD).k(0, 0, 0, 0).k(5, -22, 0, 0).k(11, 6, 0, 0, Ease.SNAP).k(16, 0, 0, 0);
        lightning.part(BODY).k(0, 0, 0, 0).k(5, -12, 0, 0).k(11, 12, 0, 0, Ease.SNAP).k(16, 0, 0, 0);
        Anims.add(lightning);

        // Тотем земли: топнуть и вдавить тотем — он даёт защиту и скорость.
        var earth = Anim.of("shaman.totem_earth", 24).fade(2);
        Moves.stomp(earth, 0, 12);
        Moves.handDown(earth, ARM_R, 12, 10);
        Anims.add(earth);

        // Духи предков: руки вверх, зов — все тотемы срабатывают разом.
        var anc = Anim.of("shaman.ancestors", 26).fade(3);
        anc.part(ARM_R).k(0, 0, 0, 0).k(8, -168, -20, 24, Ease.SMOOTH).k(15, -164, -18, 22, Ease.SHAKE)
                .k(20, -140, -36, 48, Ease.SNAP).k(26, 0, 0, 0, Ease.SMOOTH);
        anc.part(ARM_L).k(0, 0, 0, 0).k(8, -168, 20, -24, Ease.SMOOTH).k(15, -164, 18, -22, Ease.SHAKE)
                .k(20, -140, 36, -48, Ease.SNAP).k(26, 0, 0, 0, Ease.SMOOTH);
        anc.part(HEAD).k(0, 0, 0, 0).k(8, -30, 0, 0).k(20, -22, 0, 0).k(26, 0, 0, 0);
        anc.part(BODY).k(0, 0, 0, 0).k(8, -16, 0, 0).k(20, -10, 0, 0).k(26, 0, 0, 0);
        anc.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(15, 0, 0, 0, 0, 0.12, 0, Ease.SHAKE)
                .key(20, 0, 0, 0, 0, 0.08, 0, Ease.SNAP).key(26, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(anc);

        // Поглотить тотемы: притянуть их силу к себе и раздать отряду.
        var consume = Anim.of("shaman.consume", 22).fade(2);
        consume.part(ARM_R).k(0, 0, 0, 0).k(7, -80, -50, 66, Ease.SMOOTH).k(12, -116, 50, 14, Ease.SNAP)
                .k(16, -100, -10, 40, Ease.SMOOTH).k(22, 0, 0, 0, Ease.SMOOTH);
        consume.part(ARM_L).k(0, 0, 0, 0).k(7, -80, 50, -66, Ease.SMOOTH).k(12, -116, -50, -14, Ease.SNAP)
                .k(16, -100, 10, -40, Ease.SMOOTH).k(22, 0, 0, 0, Ease.SMOOTH);
        consume.part(BODY).k(0, 0, 0, 0).k(7, -8, 0, 0).k(12, 14, 0, 0, Ease.SNAP).k(16, -6, 0, 0).k(22, 0, 0, 0);
        Anims.add(consume);
    }

    // ------------------------------------------------------------------ Знаменосец

    private static void bannerman() {
        // Знамя силы: поднять древко высоко и воткнуть, потом кулак вверх.
        var power = Anim.of("banner.power", 24).fade(2);
        Moves.plant(power, 0, 16);
        power.part(ARM_L).k(0, 0, 0, 0).k(9, -35, 20, -10, Ease.SNAP).k(18, -160, 10, -24, Ease.SNAP)
                .k(21, -158, 8, -22).k(24, 0, 0, 0, Ease.SMOOTH);
        power.part(HEAD).k(0, 0, 0, 0).k(18, -20, 0, 0, Ease.SNAP).k(24, 0, 0, 0);
        Anims.add(power);

        // Знамя стойкости: вдавить древко в пол и упереться.
        var guard = Anim.of("banner.guard", 26).fade(3);
        Moves.plant(guard, 0, 16);
        guard.part(ROOT).key(16, 0, 0, 0, 0, -0.1, 0, Ease.SNAP).key(22, 0, 0, 0, 0, -0.09, 0, Ease.LINEAR)
                .key(26, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        guard.part(BODY).k(17, 20, 0, 0, Ease.SNAP).k(22, 18, 0, 0).k(26, 0, 0, 0, Ease.SMOOTH);
        guard.part(ARM_L).k(17, -60, 30, -34, Ease.SNAP).k(22, -58, 28, -32).k(26, 0, 0, 0, Ease.SMOOTH);
        Anims.add(guard);

        // Знамя скорости: воткнуть на бегу, коротким движением.
        var speed = Anim.of("banner.speed", 18).fade(2);
        Moves.plant(speed, 0, 12);
        speed.part(ARM_L).k(0, 0, 0, 0).k(7, -80, 40, -28, Ease.SNAP).k(12, -40, 20, -16, Ease.SNAP)
                .k(18, 0, 0, 0, Ease.SMOOTH);
        speed.part(ROOT).key(12, 0, 0, 0, 0, 0, 0.12, Ease.SNAP).key(18, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(speed);

        // Клич: голову вверх, знамя в небо — отряд поднимается.
        var shout = Anim.of("banner.shout", 24).fade(2);
        shout.part(ARM_R).k(0, 0, 0, 0).k(7, -182, -14, 16, Ease.SNAP).k(18, -176, -12, 14, Ease.SHAKE)
                .k(24, 0, 0, 0, Ease.SMOOTH);
        shout.part(ARM_L).k(0, 0, 0, 0).k(9, 55, 35, -40, Ease.SNAP).k(21, 48, 30, -36).k(24, 0, 0, 0, Ease.SMOOTH);
        shout.part(BODY).k(0, 0, 0, 0).k(9, -18, 0, 0, Ease.SNAP).k(21, -14, 0, 0).k(24, 0, 0, 0);
        shout.part(HEAD).k(0, 0, 0, 0).k(9, -32, 0, 0, Ease.SNAP).k(21, -28, 0, 0).k(24, 0, 0, 0);
        shout.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(9, 0, 0, 0, 0, 0.08, 0, Ease.SNAP)
                .key(21, 0, 0, 0, 0, 0.06, 0, Ease.LINEAR).key(24, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(shout);

        // Сомкнуть ряды: обеими руками собрать отряд к знамени.
        var ranks = Anim.of("banner.ranks", 20).fade(2);
        ranks.part(ARM_R).k(0, 0, 0, 0).k(6, -92, -56, 72, Ease.SMOOTH).k(12, -112, 54, 14, Ease.SNAP)
                .k(16, -110, 50, 12).k(20, 0, 0, 0, Ease.SMOOTH);
        ranks.part(ARM_L).k(0, 0, 0, 0).k(6, -92, 56, -72, Ease.SMOOTH).k(12, -112, -54, -14, Ease.SNAP)
                .k(16, -110, -50, -12).k(20, 0, 0, 0, Ease.SMOOTH);
        ranks.part(BODY).k(0, 0, 0, 0).k(6, -8, 0, 0).k(12, 12, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        ranks.part(HEAD).k(0, 0, 0, 0).k(6, -10, 0, 0).k(12, 8, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        Anims.add(ranks);

        // Последний бой: знамя над головой двумя руками — пока оно стоит, никто не умирает.
        var last = Anim.of("banner.last_stand", 30).fade(3);
        last.part(ARM_R).k(0, 0, 0, 0).k(8, -190, -18, 20, Ease.SMOOTH).k(16, -186, -16, 18, Ease.SHAKE)
                .k(24, -190, -18, 20, Ease.SHAKE).k(30, 0, 0, 0, Ease.SMOOTH);
        last.part(ARM_L).k(0, 0, 0, 0).k(8, -186, 22, -24, Ease.SMOOTH).k(16, -182, 20, -22, Ease.SHAKE)
                .k(24, -186, 22, -24, Ease.SHAKE).k(30, 0, 0, 0, Ease.SMOOTH);
        last.part(HEAD).k(0, 0, 0, 0).k(8, -28, 0, 0).k(24, -24, 0, 0).k(30, 0, 0, 0);
        last.part(BODY).k(0, 0, 0, 0).k(8, -16, 0, 0).k(24, -14, 0, 0).k(30, 0, 0, 0);
        last.add(LEG_R).k(0, 0, 0, 0).k(8, 0, 0, 12, Ease.SMOOTH).k(24, 0, 0, 12).k(30, 0, 0, 0, Ease.SMOOTH);
        last.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(8, 0, 0, 0, 0, 0.1, 0, Ease.SMOOTH)
                .key(16, 0, 0, 2, 0.02, 0.12, 0, Ease.SHAKE).key(24, 0, 0, -2, -0.02, 0.1, 0, Ease.SHAKE)
                .key(30, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(last);
    }
}
