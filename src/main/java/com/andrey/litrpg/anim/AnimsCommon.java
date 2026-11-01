package com.andrey.litrpg.anim;

import com.andrey.litrpg.anim.Anim.Ease;

import static com.andrey.litrpg.anim.Anim.Part.ARM_L;
import static com.andrey.litrpg.anim.Anim.Part.ARM_R;
import static com.andrey.litrpg.anim.Anim.Part.BODY;
import static com.andrey.litrpg.anim.Anim.Part.HEAD;
import static com.andrey.litrpg.anim.Anim.Part.LEG_R;
import static com.andrey.litrpg.anim.Anim.Part.ROOT;

/** Общие движения (замах на всякий случай, поза чтения, рывок) и движения трёх начальных навыков роли. */
final class AnimsCommon {
    private AnimsCommon() {
    }

    static void register() {
        // Замах на случай, если у навыка забыли движение: лучше простой удар, чем ничего.
        var swing = Anim.of("common.swing", 12).fade(2);
        Moves.chop(swing, ARM_R, 0, 12);
        Anims.add(swing);

        // Поза чтения: растягивается под любое время чтения.
        var cast = Anim.of("common.cast", 20).fade(3).loop();
        Moves.channel(cast, 0, 20);
        Anims.add(cast);

        var dash = Anim.of("common.dash", 10).fade(1);
        Moves.dash(dash, 0, 10);
        Anims.add(dash);

        // --- танк: провокация, удар щитом, упор
        // Провокация: рука вперёд и резко к себе — «ну давай».
        var taunt = Anim.of("tank.taunt", 14).fade(2);
        taunt.part(ARM_R).k(0, 0, 0, 0).k(4, -95, 10, 10, Ease.SMOOTH).k(7, -120, 55, 30, Ease.SNAP)
                .k(10, -95, 10, 10, Ease.SNAP).k(14, 0, 0, 0, Ease.SMOOTH);
        taunt.part(BODY).k(0, 0, 0, 0).k(7, -10, 0, 0, Ease.SNAP).k(14, 0, 0, 0);
        taunt.part(HEAD).k(0, 0, 0, 0).k(7, -12, 0, 0, Ease.SNAP).k(14, 0, 0, 0);
        taunt.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(7, 0, 0, 0, 0, 0.04, 0, Ease.SNAP)
                .key(14, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(taunt);

        var bash = Anim.of("tank.shield_bash", 14).fade(2);
        bash.part(ARM_L).k(0, 0, 0, 0).k(5, -60, 60, -45, Ease.SMOOTH).key(8, -88, -5, -10, 0, 0, -4, Ease.SNAP)
                .k(14, 0, 0, 0, Ease.SMOOTH);
        bash.part(ARM_R).k(0, 0, 0, 0).k(5, -20, -20, 15, Ease.SMOOTH).k(8, 30, 0, 10, Ease.SNAP).k(14, 0, 0, 0);
        bash.part(BODY).k(0, 0, 0, 0).k(5, 0, -22, 0, Ease.SMOOTH).k(8, 14, 16, 0, Ease.SNAP).k(14, 0, 0, 0);
        bash.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(8, 0, 0, 0, 0, 0, 0.22, Ease.SNAP)
                .key(14, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(bash);

        var bulwark = Anim.of("tank.bulwark", 20).fade(3);
        Moves.guard(bulwark, 0, 20);
        Anims.add(bulwark);

        // --- боец: тяжёлый удар, вихрь, нож
        var heavy = Anim.of("dps.heavy_strike", 16).fade(2);
        Moves.chop(heavy, ARM_R, 0, 16);
        heavy.part(ROOT).key(0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(9, 0, 0, 0, 0, 0, 0.1, Ease.SNAP)
                .key(16, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        Anims.add(heavy);

        var whirl = Anim.of("dps.whirlwind", 18).fade(2);
        Moves.spin(whirl, 0, 18, 1);
        Anims.add(whirl);

        var knife = Anim.of("dps.throwing_knife", 11).fade(1);
        Moves.toss(knife, ARM_R, 0, 11);
        Anims.add(knife);

        // --- поддержка: повязка (читается), обновление, священный свет
        var mend = Anim.of("sup.mend", 14).fade(2);
        Moves.mend(mend, ARM_R, 0, 14);
        Anims.add(mend);

        var mendCast = Anim.of("sup.mend.cast", 20).fade(3).loop();
        mendCast.part(ARM_R).k(0, -70, 20, 14).k(6, -80, 24, 18, Ease.SMOOTH).k(13, -70, 20, 14, Ease.SMOOTH)
                .k(20, -70, 20, 14);
        mendCast.part(ARM_L).k(0, -115, -50, -14).k(6, -120, -54, -16, Ease.SMOOTH).k(13, -115, -50, -14, Ease.SMOOTH)
                .k(20, -115, -50, -14);
        mendCast.part(HEAD).k(0, 6, 0, 0).k(10, 10, 0, 0).k(20, 6, 0, 0);
        Anims.add(mendCast);

        var renew = Anim.of("sup.renew", 18).fade(3);
        Moves.bless(renew, 0, 18);
        Anims.add(renew);

        // Священный свет: рука к небу, свет собирается — и вниз.
        var smite = Anim.of("sup.smite", 16).fade(2);
        smite.part(ARM_R).k(0, 0, 0, 0).k(6, -178, -10, 12, Ease.SMOOTH).k(9, -175, -8, 10)
                .k(11, -40, 6, -6, Ease.SNAP).k(16, 0, 0, 0, Ease.SMOOTH);
        smite.part(ARM_L).k(0, 0, 0, 0).k(6, -40, 20, -25, Ease.SMOOTH).k(11, -10, 10, -12, Ease.SNAP).k(16, 0, 0, 0);
        smite.part(BODY).k(0, 0, 0, 0).k(6, -12, 0, 0).k(11, 18, 0, 0, Ease.SNAP).k(16, 0, 0, 0);
        smite.part(HEAD).k(0, 0, 0, 0).k(6, -24, 0, 0).k(11, 10, 0, 0, Ease.SNAP).k(16, 0, 0, 0);
        smite.add(LEG_R).k(0, 0, 0, 0).k(11, -12, 0, 0, Ease.SNAP).k(16, 0, 0, 0);
        Anims.add(smite);
    }
}
