package com.andrey.litrpg.anim;

import com.andrey.litrpg.anim.Anim.Ease;

import static com.andrey.litrpg.anim.Anim.Part.ARM_L;
import static com.andrey.litrpg.anim.Anim.Part.ARM_R;
import static com.andrey.litrpg.anim.Anim.Part.BODY;
import static com.andrey.litrpg.anim.Anim.Part.HEAD;
import static com.andrey.litrpg.anim.Anim.Part.LEG_L;
import static com.andrey.litrpg.anim.Anim.Part.LEG_R;
import static com.andrey.litrpg.anim.Anim.Part.ROOT;

/** Движения шести танков: Стражник, Кровавый рыцарь, Зерцало, Гравитон, Ледяной бастион, Паладин. */
final class AnimsTank {
    private AnimsTank() {
    }

    static void register() {
        guardian();
        bloodKnight();
        mirror();
        graviton();
        bastion();
        paladin();
    }

    // ------------------------------------------------------------------ Стражник

    private static void guardian() {
        // Под прикрытие: закрыть союзника — шаг в сторону и щит наотлёт.
        var cover = Anim.of("guardian.cover", 18).fade(2);
        cover.part(ARM_L).k(0, 0, 0, 0).k(4, -100, -55, -60, Ease.SNAP).k(13, -95, -50, -56)
                .k(18, 0, 0, 0, Ease.SMOOTH);
        cover.part(ARM_R).k(0, 0, 0, 0).k(4, -35, 25, 20, Ease.SMOOTH).k(13, -30, 22, 18).k(18, 0, 0, 0, Ease.SMOOTH);
        cover.part(BODY).k(0, 0, 0, 0).k(4, 6, -26, 0, Ease.SNAP).k(13, 5, -24, 0).k(18, 0, 0, 0);
        cover.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(4, 0, 0, -8, -0.3, 0, 0, Ease.SNAP)
                .key(13, 0, 0, -7, -0.28, 0, 0, Ease.LINEAR).key(18, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(cover);

        // Натиск: разогнаться плечом вперёд.
        var charge = Anim.of("guardian.charge", 16).fade(1);
        charge.part(ARM_L).k(0, 0, 0, 0).k(3, -80, 50, -35, Ease.SNAP).k(10, -78, 48, -33).k(16, 0, 0, 0, Ease.SMOOTH);
        charge.part(ARM_R).k(0, 0, 0, 0).k(3, 60, -15, 18, Ease.SNAP).k(10, 55, -12, 16).k(16, 0, 0, 0, Ease.SMOOTH);
        charge.part(BODY).k(0, 0, 0, 0).k(3, 34, -10, 0, Ease.SNAP).k(10, 30, -8, 0).k(16, 0, 0, 0);
        charge.part(HEAD).k(0, 0, 0, 0).k(3, -18, 0, 0, Ease.SNAP).k(10, -16, 0, 0).k(16, 0, 0, 0);
        charge.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(3, 12, 0, 0, 0, -0.06, 0.1, Ease.SNAP)
                .key(10, 10, 0, 0, 0, -0.06, 0.1, Ease.LINEAR).key(16, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(charge);

        var brace = Anim.of("guardian.brace", 20).fade(3);
        Moves.dig(brace, 0, 20);
        brace.part(ARM_L).k(0, 0, 0, 0).k(4, -70, 40, -40, Ease.SNAP).k(16, -68, 38, -38).k(20, 0, 0, 0, Ease.SMOOTH);
        Anims.add(brace);

        // Купол: руки вверх и развести — над отрядом встаёт щит.
        var dome = Anim.of("guardian.dome", 22).fade(3);
        dome.part(ARM_R).k(0, 0, 0, 0).k(8, -165, -16, 8, Ease.SMOOTH).k(13, -150, -24, 46, Ease.SNAP)
                .k(18, -148, -22, 44).k(22, 0, 0, 0, Ease.SMOOTH);
        dome.part(ARM_L).k(0, 0, 0, 0).k(8, -165, 16, -8, Ease.SMOOTH).k(13, -150, 24, -46, Ease.SNAP)
                .k(18, -148, 22, -44).k(22, 0, 0, 0, Ease.SMOOTH);
        dome.part(BODY).k(0, 0, 0, 0).k(8, -10, 0, 0).k(13, -6, 0, 0).k(22, 0, 0, 0);
        dome.part(HEAD).k(0, 0, 0, 0).k(8, -20, 0, 0).k(18, -14, 0, 0).k(22, 0, 0, 0);
        dome.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(13, 0, 0, 0, 0, 0.08, 0, Ease.SNAP)
                .key(22, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(dome);

        // Несокрушимый: кулаки сжаты, стоит и не падает.
        var unbreakable = Anim.of("guardian.unbreakable", 26).fade(3);
        Moves.tremble(unbreakable, 2, 20);
        unbreakable.part(ARM_R).k(0, 0, 0, 0).k(4, 32, -30, 26, Ease.SNAP).k(22, 28, -26, 24).k(26, 0, 0, 0, Ease.SMOOTH);
        unbreakable.part(ARM_L).k(0, 0, 0, 0).k(4, 32, 30, -26, Ease.SNAP).k(22, 28, 26, -24).k(26, 0, 0, 0, Ease.SMOOTH);
        unbreakable.part(HEAD).k(0, 0, 0, 0).k(4, -16, 0, 0, Ease.SNAP).k(22, -14, 0, 0).k(26, 0, 0, 0);
        Anims.add(unbreakable);
    }

    // ------------------------------------------------------------------ Кровавый рыцарь

    private static void bloodKnight() {
        // Кровавый удар: замах через себя, в конце рука остаётся опущенной — кровь капает.
        var strike = Anim.of("blood.strike", 16).fade(2);
        Moves.chop(strike, ARM_R, 0, 13);
        strike.part(ARM_L).k(0, 0, 0, 0).k(6, -40, 25, -22, Ease.SMOOTH).k(9, 20, 10, -8, Ease.SNAP).k(16, 0, 0, 0);
        Anims.add(strike);

        // Клеймо: ладонь к цели, пальцы сжимаются — на враге загорается знак.
        var brand = Anim.of("blood.brand", 16).fade(2);
        brand.part(ARM_R).k(0, 0, 0, 0).k(5, -92, 8, 10, Ease.SMOOTH).k(9, -96, 4, 4, Ease.SNAP)
                .k(11, -84, 10, 14, Ease.SNAP).k(16, 0, 0, 0, Ease.SMOOTH);
        brand.part(ARM_L).k(0, 0, 0, 0).k(5, -30, -20, 14, Ease.SMOOTH).k(16, 0, 0, 0);
        brand.part(BODY).k(0, 0, 0, 0).k(9, 8, -10, 0, Ease.SNAP).k(16, 0, 0, 0);
        brand.part(HEAD).k(0, 0, 0, 0).k(9, 8, 0, 0).k(16, 0, 0, 0);
        Anims.add(brand);

        var chains = Anim.of("blood.chains", 18).fade(2);
        Moves.lash(chains, ARM_R, 0, 18);
        Anims.add(chains);

        // Багряный пир: собрать свою кровь к груди — и выпить её.
        var feast = Anim.of("blood.feast", 22).fade(3);
        feast.part(ARM_R).k(0, 0, 0, 0).k(6, -80, -40, 55, Ease.SMOOTH).k(12, -125, 50, 12, Ease.SNAP)
                .k(17, -120, 46, 10).k(22, 0, 0, 0, Ease.SMOOTH);
        feast.part(ARM_L).k(0, 0, 0, 0).k(6, -80, 40, -55, Ease.SMOOTH).k(12, -125, -50, -12, Ease.SNAP)
                .k(17, -120, -46, -10).k(22, 0, 0, 0, Ease.SMOOTH);
        feast.part(BODY).k(0, 0, 0, 0).k(6, 10, 0, 0).k(12, -16, 0, 0, Ease.SNAP).k(17, -12, 0, 0).k(22, 0, 0, 0);
        feast.part(HEAD).k(0, 0, 0, 0).k(6, 14, 0, 0).k(12, -26, 0, 0, Ease.SNAP).k(17, -22, 0, 0).k(22, 0, 0, 0);
        feast.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(12, 0, 0, 0, 0, 0.1, 0, Ease.SNAP)
                .key(22, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(feast);

        // Кровавые нити: руки врастопырку, нити тянутся ко всем — и рыцарь медленно сжимает кулаки.
        var threads = Anim.of("blood.threads", 34).fade(3);
        threads.part(ARM_R).k(0, 0, 0, 0).k(5, -110, -35, 70, Ease.SNAP).k(12, -104, -30, 76, Ease.SHAKE)
                .k(20, -112, -36, 68, Ease.SHAKE).k(27, -96, -24, 58, Ease.SMOOTH).k(34, 0, 0, 0, Ease.SMOOTH);
        threads.part(ARM_L).k(0, 0, 0, 0).k(5, -110, 35, -70, Ease.SNAP).k(12, -112, 36, -68, Ease.SHAKE)
                .k(20, -104, 30, -76, Ease.SHAKE).k(27, -96, 24, -58, Ease.SMOOTH).k(34, 0, 0, 0, Ease.SMOOTH);
        threads.part(BODY).k(0, 0, 0, 0).k(5, -16, 0, 0, Ease.SNAP).k(20, -12, 0, 0).k(27, 8, 0, 0).k(34, 0, 0, 0);
        threads.part(HEAD).k(0, 0, 0, 0).k(5, -26, 0, 0, Ease.SNAP).k(20, -22, 0, 0).k(27, 12, 0, 0).k(34, 0, 0, 0);
        threads.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(5, 0, 0, 0, 0, 0.12, 0, Ease.SNAP)
                .key(20, 0, 0, 0, 0.03, 0.1, 0, Ease.SHAKE).key(34, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(threads);
    }

    // ------------------------------------------------------------------ Зерцало

    private static void mirror() {
        // Стекло: руки крест-накрест, тело застывает стеклом.
        var glass = Anim.of("mirror.glass", 20).fade(3);
        Moves.brace(glass, 0, 20);
        Anims.add(glass);

        // Вернуть сполна: всё накопленное — конусом вперёд.
        var repay = Anim.of("mirror.repay", 18).fade(2);
        repay.part(ARM_R).k(0, 0, 0, 0).k(7, -118, 58, 20, Ease.SMOOTH).key(11, -92, 8, -8, 0, 0, -4, Ease.SNAP)
                .k(18, 0, 0, 0, Ease.SMOOTH);
        repay.part(ARM_L).k(0, 0, 0, 0).k(7, -118, -58, -20, Ease.SMOOTH).key(11, -92, -8, 8, 0, 0, -4, Ease.SNAP)
                .k(18, 0, 0, 0, Ease.SMOOTH);
        repay.part(BODY).k(0, 0, 0, 0).k(7, -14, 0, 0).k(11, 22, 0, 0, Ease.SNAP).k(18, 0, 0, 0);
        repay.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(11, 0, 0, 0, 0, 0, -0.1, Ease.SNAP)
                .key(18, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(repay);

        // Грань: провести рукой перед собой, как по стеклу, — снаряды летят обратно.
        var edge = Anim.of("mirror.edge", 15).fade(2);
        edge.part(ARM_L).k(0, 0, 0, 0).k(4, -75, -45, -52, Ease.SMOOTH).k(9, -82, 45, -8, Ease.SNAP)
                .k(15, 0, 0, 0, Ease.SMOOTH);
        edge.part(ARM_R).k(0, 0, 0, 0).k(4, -25, 15, 12, Ease.SMOOTH).k(15, 0, 0, 0);
        edge.part(BODY).k(0, 0, 0, 0).k(4, 0, -16, 0).k(9, 4, 14, 0, Ease.SNAP).k(15, 0, 0, 0);
        edge.part(HEAD).k(0, 0, 0, 0).k(4, 0, -10, 0).k(9, 0, 8, 0, Ease.SNAP).k(15, 0, 0, 0);
        Anims.add(edge);

        // Зеркальный зал: медленный оборот — вокруг встают отражения.
        var hall = Anim.of("mirror.hall", 28).fade(3);
        hall.part(ARM_R).k(0, 0, 0, 0).k(5, -92, -20, 72, Ease.SMOOTH).k(23, -88, -18, 70).k(28, 0, 0, 0, Ease.SMOOTH);
        hall.part(ARM_L).k(0, 0, 0, 0).k(5, -92, 20, -72, Ease.SMOOTH).k(23, -88, 18, -70).k(28, 0, 0, 0, Ease.SMOOTH);
        hall.part(ROOT).k(0, 0, 0, 0).k(24, 0, 360, 0, Ease.SMOOTH).k(28, 0, 360, 0);
        hall.part(HEAD).k(0, 0, 0, 0).k(12, -10, 0, 0).k(28, 0, 0, 0);
        Anims.add(hall);

        // Двойник: отвести руку в сторону — из неё выходит копия.
        var dbl = Anim.of("mirror.double", 18).fade(2);
        dbl.part(ARM_R).k(0, 0, 0, 0).k(6, -70, -50, 60, Ease.SMOOTH).key(10, -58, -72, 78, 0, 0, 0, Ease.SNAP)
                .k(18, 0, 0, 0, Ease.SMOOTH);
        dbl.part(ARM_L).k(0, 0, 0, 0).k(6, -45, 30, -30, Ease.SMOOTH).k(18, 0, 0, 0);
        dbl.part(BODY).k(0, 0, 0, 0).k(6, 0, 14, 0).k(10, 0, 20, 0, Ease.SNAP).k(18, 0, 0, 0);
        dbl.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(10, 0, 0, 6, 0.14, 0, 0, Ease.SNAP)
                .key(18, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(dbl);
    }

    // ------------------------------------------------------------------ Гравитон

    private static void graviton() {
        // Воронка: развести руки и свести их в точку — толпу тянет к центру.
        var funnel = Anim.of("graviton.funnel", 20).fade(2);
        funnel.part(ARM_R).k(0, 0, 0, 0).k(6, -95, -45, 75, Ease.SMOOTH).k(12, -105, 60, 10, Ease.SNAP)
                .k(20, 0, 0, 0, Ease.SMOOTH);
        funnel.part(ARM_L).k(0, 0, 0, 0).k(6, -95, 45, -75, Ease.SMOOTH).k(12, -105, -60, -10, Ease.SNAP)
                .k(20, 0, 0, 0, Ease.SMOOTH);
        funnel.part(BODY).k(0, 0, 0, 0).k(6, -10, 0, 0).k(12, 14, 0, 0, Ease.SNAP).k(20, 0, 0, 0);
        Anims.add(funnel);

        // Пресс: обе ладони вниз, вес давит в землю.
        var press = Anim.of("graviton.press", 22).fade(3);
        press.part(ARM_R).k(0, 0, 0, 0).k(6, -150, -20, 40, Ease.SMOOTH).k(12, -35, -6, 22, Ease.SNAP)
                .k(17, -32, -5, 20).k(22, 0, 0, 0, Ease.SMOOTH);
        press.part(ARM_L).k(0, 0, 0, 0).k(6, -150, 20, -40, Ease.SMOOTH).k(12, -35, 6, -22, Ease.SNAP)
                .k(17, -32, 5, -20).k(22, 0, 0, 0, Ease.SMOOTH);
        press.part(BODY).k(0, 0, 0, 0).k(6, -12, 0, 0).k(12, 24, 0, 0, Ease.SNAP).k(17, 22, 0, 0).k(22, 0, 0, 0);
        press.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(12, 0, 0, 0, 0, -0.1, 0, Ease.SNAP)
                .key(17, 0, 0, 0, 0, -0.09, 0, Ease.LINEAR).key(22, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(press);

        var toss = Anim.of("graviton.toss", 18).fade(2);
        Moves.scoop(toss, 0, 18);
        Anims.add(toss);

        // Инверсия: обвести рукой круг перед собой — снаряды сворачивают внутрь.
        var inversion = Anim.of("graviton.inversion", 20).fade(2);
        Moves.sign(inversion, ARM_R, 0, 20);
        inversion.part(ARM_L).k(0, 0, 0, 0).k(8, -60, 40, -35, Ease.SMOOTH).k(20, 0, 0, 0, Ease.SMOOTH);
        Anims.add(inversion);

        // Чёрная дыра: 4 секунды держит точку — руки сводятся над головой, тело выгибает.
        var hole = Anim.of("graviton.blackhole", 80).fade(4);
        hole.part(ARM_R).k(0, 0, 0, 0).k(8, -150, -50, 70, Ease.SMOOTH).k(24, -168, -20, 26, Ease.SMOOTH)
                .k(44, -174, -10, 14, Ease.SHAKE).k(64, -176, -6, 8, Ease.SHAKE).k(72, -120, -30, 50, Ease.SNAP)
                .k(80, 0, 0, 0, Ease.SMOOTH);
        hole.part(ARM_L).k(0, 0, 0, 0).k(8, -150, 50, -70, Ease.SMOOTH).k(24, -168, 20, -26, Ease.SMOOTH)
                .k(44, -174, 10, -14, Ease.SHAKE).k(64, -176, 6, -8, Ease.SHAKE).k(72, -120, 30, -50, Ease.SNAP)
                .k(80, 0, 0, 0, Ease.SMOOTH);
        hole.part(BODY).k(0, 0, 0, 0).k(8, -14, 0, 0).k(44, -22, 0, 0, Ease.SHAKE).k(64, -20, 0, 0)
                .k(72, 12, 0, 0, Ease.SNAP).k(80, 0, 0, 0);
        hole.part(HEAD).k(0, 0, 0, 0).k(8, -28, 0, 0).k(64, -34, 0, 0).k(72, 16, 0, 0, Ease.SNAP).k(80, 0, 0, 0);
        hole.part(LEG_R).k(0, 0, 0, 0).k(10, 0, 0, 14, Ease.SMOOTH).k(64, 0, 0, 16).k(80, 0, 0, 0, Ease.SMOOTH);
        hole.part(LEG_L).k(0, 0, 0, 0).k(10, 0, 0, -14, Ease.SMOOTH).k(64, 0, 0, -16).k(80, 0, 0, 0, Ease.SMOOTH);
        hole.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(12, 0, 0, 0, 0, 0.18, 0, Ease.SMOOTH)
                .key(44, 0, 0, 0, 0.04, 0.26, 0, Ease.SHAKE).key(64, 0, 0, 0, -0.04, 0.3, 0, Ease.SHAKE)
                .key(72, 0, 0, 0, 0, -0.06, 0, Ease.SNAP).key(80, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(hole);
    }

    // ------------------------------------------------------------------ Ледяной бастион

    private static void bastion() {
        // Ледяной панцирь: руки к груди, лёд нарастает — и развести.
        var armor = Anim.of("bastion.armor", 20).fade(3);
        armor.part(ARM_R).k(0, 0, 0, 0).k(5, -100, 60, 18, Ease.SMOOTH).k(12, -100, 60, 18)
                .k(16, -70, -20, 62, Ease.BACK).k(20, 0, 0, 0, Ease.SMOOTH);
        armor.part(ARM_L).k(0, 0, 0, 0).k(5, -100, -60, -18, Ease.SMOOTH).k(12, -100, -60, -18)
                .k(16, -70, 20, -62, Ease.BACK).k(20, 0, 0, 0, Ease.SMOOTH);
        armor.part(BODY).k(0, 0, 0, 0).k(5, 14, 0, 0).k(12, 12, 0, 0).k(16, -10, 0, 0, Ease.BACK).k(20, 0, 0, 0);
        armor.part(HEAD).k(0, 0, 0, 0).k(5, 16, 0, 0).k(16, -12, 0, 0, Ease.BACK).k(20, 0, 0, 0);
        Anims.add(armor);

        // Стужа: ладони вниз в стороны, от бастиона идёт холод.
        var frost = Anim.of("bastion.frost", 22).fade(3);
        frost.part(ARM_R).k(0, 0, 0, 0).k(7, -42, -30, 58, Ease.SMOOTH).k(17, -40, -28, 56, Ease.SHAKE)
                .k(22, 0, 0, 0, Ease.SMOOTH);
        frost.part(ARM_L).k(0, 0, 0, 0).k(7, -42, 30, -58, Ease.SMOOTH).k(17, -40, 28, -56, Ease.SHAKE)
                .k(22, 0, 0, 0, Ease.SMOOTH);
        frost.part(BODY).k(0, 0, 0, 0).k(7, -6, 0, 0).k(22, 0, 0, 0);
        frost.part(HEAD).k(0, 0, 0, 0).k(7, 10, 0, 0).k(22, 0, 0, 0);
        Anims.add(frost);

        // Ледяная стена: ладони вперёд и вверх — из земли встаёт стена.
        var wall = Anim.of("bastion.wall", 20).fade(2);
        wall.part(ARM_R).k(0, 0, 0, 0).k(5, -30, 20, 24, Ease.SMOOTH).key(10, -100, 6, 6, 0, 0, -3, Ease.SNAP)
                .k(14, -140, 4, 10, Ease.SNAP).k(20, 0, 0, 0, Ease.SMOOTH);
        wall.part(ARM_L).k(0, 0, 0, 0).k(5, -30, -20, -24, Ease.SMOOTH).key(10, -100, -6, -6, 0, 0, -3, Ease.SNAP)
                .k(14, -140, -4, -10, Ease.SNAP).k(20, 0, 0, 0, Ease.SMOOTH);
        wall.part(BODY).k(0, 0, 0, 0).k(5, 12, 0, 0).k(10, 8, 0, 0, Ease.SNAP).k(14, -12, 0, 0, Ease.SNAP)
                .k(20, 0, 0, 0);
        wall.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(10, 0, 0, 0, 0, -0.06, 0, Ease.SNAP)
                .key(14, 0, 0, 0, 0, 0.04, 0, Ease.SNAP).key(20, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(wall);

        // Вечная мерзлота: присесть и приложить обе ладони к земле.
        var permafrost = Anim.of("bastion.permafrost", 24).fade(3);
        permafrost.part(ARM_R).k(0, 0, 0, 0).k(6, -120, -20, 30, Ease.SMOOTH).k(12, 22, 10, 8, Ease.SNAP)
                .k(19, 20, 8, 7).k(24, 0, 0, 0, Ease.SMOOTH);
        permafrost.part(ARM_L).k(0, 0, 0, 0).k(6, -120, 20, -30, Ease.SMOOTH).k(12, 22, -10, -8, Ease.SNAP)
                .k(19, 20, -8, -7).k(24, 0, 0, 0, Ease.SMOOTH);
        permafrost.part(BODY).k(0, 0, 0, 0).k(6, -8, 0, 0).k(12, 36, 0, 0, Ease.SNAP).k(19, 34, 0, 0).k(24, 0, 0, 0);
        permafrost.add(LEG_R).k(0, 0, 0, 0).k(12, -34, 0, 16, Ease.SNAP).k(19, -32, 0, 14).k(24, 0, 0, 0);
        permafrost.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(12, 0, 0, 0, 0, -0.2, 0, Ease.SNAP)
                .key(19, 0, 0, 0, 0, -0.18, 0, Ease.LINEAR).key(24, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(permafrost);

        // Ледяная гробница: руки на груди, тело каменеет на шесть секунд.
        var tomb = Anim.of("bastion.tomb", 120).fade(5);
        tomb.part(ARM_R).k(0, 0, 0, 0).k(6, -112, 52, 10, Ease.SNAP).k(110, -112, 52, 10).k(120, 0, 0, 0, Ease.SMOOTH);
        tomb.part(ARM_L).k(0, 0, 0, 0).k(6, -112, -52, -10, Ease.SNAP).k(110, -112, -52, -10).k(120, 0, 0, 0, Ease.SMOOTH);
        tomb.part(BODY).k(0, 0, 0, 0).k(6, 8, 0, 0, Ease.SNAP).k(110, 8, 0, 0).k(120, 0, 0, 0);
        tomb.part(HEAD).k(0, 0, 0, 0).k(6, 14, 0, 0, Ease.SNAP).k(110, 14, 0, 0).k(120, 0, 0, 0);
        tomb.part(LEG_R).k(0, 0, 0, 0).k(6, 0, 0, 6, Ease.SNAP).k(110, 0, 0, 6).k(120, 0, 0, 0, Ease.SMOOTH);
        tomb.part(LEG_L).k(0, 0, 0, 0).k(6, 0, 0, -6, Ease.SNAP).k(110, 0, 0, -6).k(120, 0, 0, 0, Ease.SMOOTH);
        Anims.add(tomb);
    }

    // ------------------------------------------------------------------ Паладин клятвы

    private static void paladin() {
        // Клятва света: кулак к сердцу, голова склонена.
        var oath = Anim.of("paladin.oath", 24).fade(3);
        oath.part(ARM_R).k(0, 0, 0, 0).k(6, -128, 62, 14, Ease.SMOOTH).k(16, -126, 60, 12).k(24, 0, 0, 0, Ease.SMOOTH);
        oath.part(ARM_L).k(0, 0, 0, 0).k(6, 20, 15, -14, Ease.SMOOTH).k(16, 18, 12, -12).k(24, 0, 0, 0, Ease.SMOOTH);
        oath.part(HEAD).k(0, 0, 0, 0).k(6, 20, 0, 0, Ease.SMOOTH).k(14, 18, 0, 0).k(20, -14, 0, 0, Ease.SNAP)
                .k(24, 0, 0, 0);
        oath.part(BODY).k(0, 0, 0, 0).k(6, 8, 0, 0).k(20, -6, 0, 0, Ease.SNAP).k(24, 0, 0, 0);
        Anims.add(oath);

        var strike = Anim.of("paladin.strike", 15).fade(2);
        Moves.chop(strike, ARM_R, 0, 15);
        strike.part(ARM_L).k(0, 0, 0, 0).k(7, -55, 30, -28, Ease.SMOOTH).k(15, 0, 0, 0);
        Anims.add(strike);

        // Божественный щит: руки вверх крест-накрест, свет вспыхивает.
        var divine = Anim.of("paladin.divine", 20).fade(2);
        divine.part(ARM_R).k(0, 0, 0, 0).k(5, -150, 40, 30, Ease.SNAP).k(14, -148, 38, 28).k(20, 0, 0, 0, Ease.SMOOTH);
        divine.part(ARM_L).k(0, 0, 0, 0).k(5, -150, -40, -30, Ease.SNAP).k(14, -148, -38, -28).k(20, 0, 0, 0, Ease.SMOOTH);
        divine.part(HEAD).k(0, 0, 0, 0).k(5, -22, 0, 0, Ease.SNAP).k(14, -20, 0, 0).k(20, 0, 0, 0);
        divine.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(5, 0, 0, 0, 0, 0.1, 0, Ease.SNAP)
                .key(14, 0, 0, 0, 0, 0.08, 0, Ease.LINEAR).key(20, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(divine);

        // Возложение рук: обе ладони к союзнику, своё здоровье уходит ему.
        var hands = Anim.of("paladin.hands", 22).fade(3);
        hands.part(ARM_R).k(0, 0, 0, 0).k(7, -95, 14, 12, Ease.SMOOTH).k(16, -92, 12, 10, Ease.SHAKE)
                .k(22, 0, 0, 0, Ease.SMOOTH);
        hands.part(ARM_L).k(0, 0, 0, 0).k(7, -95, -14, -12, Ease.SMOOTH).k(16, -92, -12, -10, Ease.SHAKE)
                .k(22, 0, 0, 0, Ease.SMOOTH);
        hands.part(BODY).k(0, 0, 0, 0).k(7, 14, 0, 0).k(16, 12, 0, 0).k(22, 0, 0, 0);
        hands.part(HEAD).k(0, 0, 0, 0).k(7, 16, 0, 0).k(16, 14, 0, 0).k(22, 0, 0, 0);
        Anims.add(hands);

        // Суд света: меч вверх, свет собирается — и обеими руками в землю.
        var judgment = Anim.of("paladin.judgment", 26).fade(3);
        judgment.part(ARM_R).k(0, 0, 0, 0).k(8, -180, -12, 14, Ease.SMOOTH).k(14, -178, -10, 12, Ease.SHAKE)
                .k(18, -6, 6, -6, Ease.SNAP).k(26, 0, 0, 0, Ease.SMOOTH);
        judgment.part(ARM_L).k(0, 0, 0, 0).k(8, -180, 12, -14, Ease.SMOOTH).k(14, -178, 10, -12, Ease.SHAKE)
                .k(18, -6, -6, 6, Ease.SNAP).k(26, 0, 0, 0, Ease.SMOOTH);
        judgment.part(BODY).k(0, 0, 0, 0).k(8, -16, 0, 0).k(18, 30, 0, 0, Ease.SNAP).k(26, 0, 0, 0);
        judgment.part(HEAD).k(0, 0, 0, 0).k(8, -30, 0, 0).k(18, 16, 0, 0, Ease.SNAP).k(26, 0, 0, 0);
        judgment.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(8, 0, 0, 0, 0, 0.2, 0, Ease.SMOOTH)
                .key(14, 0, 0, 0, 0, 0.22, 0, Ease.SHAKE).key(18, 0, 0, 0, 0, -0.1, 0, Ease.SNAP)
                .key(26, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(judgment);
    }
}
