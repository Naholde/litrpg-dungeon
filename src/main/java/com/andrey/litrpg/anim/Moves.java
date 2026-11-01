package com.andrey.litrpg.anim;

import com.andrey.litrpg.anim.Anim.Builder;
import com.andrey.litrpg.anim.Anim.Ease;

import static com.andrey.litrpg.anim.Anim.Part.ARM_L;
import static com.andrey.litrpg.anim.Anim.Part.ARM_R;
import static com.andrey.litrpg.anim.Anim.Part.BODY;
import static com.andrey.litrpg.anim.Anim.Part.HEAD;
import static com.andrey.litrpg.anim.Anim.Part.LEG_L;
import static com.andrey.litrpg.anim.Anim.Part.LEG_R;
import static com.andrey.litrpg.anim.Anim.Part.ROOT;

/**
 * Готовые движения, из которых собираются анимации навыков: замах, выпад, рывок, чтение, призыв.
 * Каждое пишет кадры в {@code b} с момента {@code t0} и занимает {@code len} тиков — поэтому движения
 * складываются в связку: замах → удар → возврат.
 * <p>Повороты в градусах: у руки {@code −90} — вперёд, {@code −180} — вверх, {@code +90} — назад.
 * Для левой руки знаки поворотов переворачиваются сами.
 */
public final class Moves {
    private Moves() {
    }

    private static int s(Anim.Part arm) {
        return arm == ARM_L ? -1 : 1;
    }

    private static Anim.Part other(Anim.Part arm) {
        return arm == ARM_L ? ARM_R : ARM_L;
    }

    // ------------------------------------------------------------------ ближний бой

    /** Замах над головой и рубящий удар вниз. */
    public static void chop(Builder b, Anim.Part arm, int t0, int len) {
        int up = t0 + (int) (len * 0.45), hit = t0 + (int) (len * 0.62);
        int k = s(arm);
        b.part(arm).k(t0, 0, 0, 0).k(up, -195, -10 * k, 18 * k, Ease.SMOOTH).k(hit, -15, 5 * k, -8 * k, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(up, -6, 10 * k, 0).k(hit, 16, -8 * k, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
    }

    /** Боковой размах через всё тело. */
    public static void sweep(Builder b, Anim.Part arm, int t0, int len) {
        int up = t0 + (int) (len * 0.4), hit = t0 + (int) (len * 0.66);
        int k = s(arm);
        b.part(arm).k(t0, 0, 0, 0).k(up, -70, 95 * k, 75 * k, Ease.SMOOTH).k(hit, -80, -85 * k, -35 * k, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(up, 0, 26 * k, 0).k(hit, 4, -24 * k, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
    }

    /** Колющий выпад: рука уходит назад и выстреливает вперёд. */
    public static void thrust(Builder b, Anim.Part arm, int t0, int len) {
        int back = t0 + (int) (len * 0.35), hit = t0 + (int) (len * 0.55);
        int k = s(arm);
        b.part(arm).k(t0, 0, 0, 0).key(back, -55, 35 * k, 25 * k, 0, 0, 2, Ease.SMOOTH)
                .key(hit, -92, -6 * k, -4 * k, 0, 0, -5, Ease.SNAP).k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(back, -4, 16 * k, 0).key(hit, 10, -12 * k, 0, 0, 0, -1, Ease.SNAP)
                .k(t0 + len, 0, 0, 0);
        b.add(LEG_L).k(t0, 0, 0, 0).k(hit, -22, 0, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
    }

    /** Удар снизу вверх. */
    public static void uppercut(Builder b, Anim.Part arm, int t0, int len) {
        int down = t0 + (int) (len * 0.35), hit = t0 + (int) (len * 0.6);
        int k = s(arm);
        b.part(arm).k(t0, 0, 0, 0).k(down, 55, 10 * k, 12 * k, Ease.SMOOTH).k(hit, -165, -8 * k, -14 * k, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(down, 18, 8 * k, 0).k(hit, -14, -6 * k, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
        b.add(LEG_R).k(t0, 0, 0, 0).k(hit, -14, 0, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
    }

    /** Двумя руками над головой — и в землю. */
    public static void slam(Builder b, int t0, int len) {
        int up = t0 + (int) (len * 0.45), hit = t0 + (int) (len * 0.62);
        b.part(ARM_R).k(t0, 0, 0, 0).k(up, -205, -12, 22, Ease.SMOOTH).k(hit, -8, 4, -6, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_L).k(t0, 0, 0, 0).k(up, -205, 12, -22, Ease.SMOOTH).k(hit, -8, -4, 6, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(up, -14, 0, 0).k(hit, 28, 0, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
        b.part(ROOT).key(t0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(up, 0, 0, 0, 0, 0.22, 0, Ease.SMOOTH)
                .key(hit, 0, 0, 0, 0, -0.08, 0, Ease.SNAP).key(t0 + len, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
    }

    /** Крест двумя руками. */
    public static void cross(Builder b, int t0, int len) {
        int up = t0 + (int) (len * 0.38), hit = t0 + (int) (len * 0.6);
        b.part(ARM_R).k(t0, 0, 0, 0).k(up, -120, 60, 70, Ease.SMOOTH).k(hit, -60, -70, -50, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_L).k(t0, 0, 0, 0).k(up + 2, -120, -60, -70, Ease.SMOOTH).k(hit + 2, -60, 70, 50, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(up, 0, 18, 0).k(hit + 2, 6, -18, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
    }

    /** Разворот вокруг себя с разведёнными руками. {@code turns} — сколько оборотов. */
    public static void spin(Builder b, int t0, int len, int turns) {
        b.part(ARM_R).k(t0, 0, 0, 0).k(t0 + 2, -88, -10, 82, Ease.SNAP).k(t0 + len - 2, -88, -10, 82)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_L).k(t0, 0, 0, 0).k(t0 + 2, -88, 10, -82, Ease.SNAP).k(t0 + len - 2, -88, 10, -82)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ROOT).k(t0, 0, 0, 0).k(t0 + len - 2, 0, 360 * turns, 0, Ease.LINEAR).k(t0 + len, 0, 360 * turns, 0);
        b.part(BODY).k(t0, 0, 0, 0).k(t0 + 3, 8, 0, 0).k(t0 + len, 0, 0, 0);
    }

    /** Пинок вперёд. */
    public static void kick(Builder b, int t0, int len) {
        int hit = t0 + (int) (len * 0.5);
        b.part(LEG_R).k(t0, 0, 0, 0).k(hit - 2, 30, 0, 0, Ease.SMOOTH).k(hit, -95, 0, 0, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_L).k(t0, 0, 0, 0).k(hit, -40, 20, -50, Ease.SNAP).k(t0 + len, 0, 0, 0);
        b.part(ARM_R).k(t0, 0, 0, 0).k(hit, 35, 0, 15, Ease.SNAP).k(t0 + len, 0, 0, 0);
        b.part(BODY).k(t0, 0, 0, 0).k(hit, -12, 0, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
    }

    /** Топнуть ногой. */
    public static void stomp(Builder b, int t0, int len) {
        int up = t0 + (int) (len * 0.45), hit = t0 + (int) (len * 0.62);
        b.part(LEG_R).k(t0, 0, 0, 0).k(up, -75, 0, 10, Ease.SMOOTH).k(hit, 8, 0, 0, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_R).k(t0, 0, 0, 0).k(up, -30, 0, 25, Ease.SMOOTH).k(hit, 20, 0, 5, Ease.SNAP).k(t0 + len, 0, 0, 0);
        b.part(ARM_L).k(t0, 0, 0, 0).k(up, -30, 0, -25, Ease.SMOOTH).k(hit, 20, 0, -5, Ease.SNAP).k(t0 + len, 0, 0, 0);
        b.part(ROOT).key(t0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(up, 0, 0, 0, 0, 0.28, 0, Ease.SMOOTH)
                .key(hit, 0, 0, 0, 0, 0, 0, Ease.SNAP).key(t0 + len, 0, 0, 0, 0, 0, 0, Ease.LINEAR);
    }

    /** Кулаками в землю. */
    public static void fists(Builder b, int t0, int len) {
        int up = t0 + (int) (len * 0.4), hit = t0 + (int) (len * 0.58);
        b.part(ARM_R).k(t0, 0, 0, 0).k(up, -150, -20, 35, Ease.SMOOTH).k(hit, 15, 10, -10, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_L).k(t0, 0, 0, 0).k(up, -150, 20, -35, Ease.SMOOTH).k(hit, 15, -10, 10, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(up, -10, 0, 0).k(hit, 34, 0, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
        b.part(ROOT).key(t0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(hit, 0, 0, 0, 0, -0.12, 0, Ease.SNAP)
                .key(t0 + len, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
    }

    // ------------------------------------------------------------------ защита

    /** Поднять щит (левая рука перед собой), присесть. */
    public static void guard(Builder b, int t0, int len) {
        b.part(ARM_L).k(t0, 0, 0, 0).k(t0 + 3, -75, 35, -30, Ease.SNAP).k(t0 + len - 3, -72, 32, -28)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_R).k(t0, 0, 0, 0).k(t0 + 3, -25, -15, 10, Ease.SMOOTH).k(t0 + len - 3, -22, -12, 8)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(t0 + 3, 12, -8, 0, Ease.SNAP).k(t0 + len - 3, 10, -6, 0).k(t0 + len, 0, 0, 0);
        b.part(ROOT).key(t0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(t0 + 3, 0, 0, 0, 0, -0.08, 0, Ease.SNAP)
                .key(t0 + len - 3, 0, 0, 0, 0, -0.08, 0, Ease.LINEAR).key(t0 + len, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
    }

    /** Руки крест-накрест на груди — и развести. */
    public static void brace(Builder b, int t0, int len) {
        int mid = t0 + (int) (len * 0.5);
        b.part(ARM_R).k(t0, 0, 0, 0).k(t0 + 3, -95, 55, 15, Ease.SNAP).k(mid, -95, 55, 15).k(t0 + len, 0, 0, 0, Ease.BACK);
        b.part(ARM_L).k(t0, 0, 0, 0).k(t0 + 3, -95, -55, -15, Ease.SNAP).k(mid, -95, -55, -15).k(t0 + len, 0, 0, 0, Ease.BACK);
        b.part(BODY).k(t0, 0, 0, 0).k(t0 + 3, 14, 0, 0, Ease.SNAP).k(mid, 10, 0, 0).k(t0 + len, -6, 0, 0, Ease.BACK);
        b.part(HEAD).k(t0, 0, 0, 0).k(t0 + 3, 16, 0, 0).k(mid, 12, 0, 0).k(t0 + len, -8, 0, 0, Ease.BACK);
    }

    /** Вколотиться в землю: присесть, руки назад. */
    public static void dig(Builder b, int t0, int len) {
        b.part(ARM_R).k(t0, 0, 0, 0).k(t0 + 4, 48, -10, 18, Ease.SNAP).k(t0 + len - 4, 44, -8, 16).k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_L).k(t0, 0, 0, 0).k(t0 + 4, 48, 10, -18, Ease.SNAP).k(t0 + len - 4, 44, 8, -16).k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(t0 + 4, 20, 0, 0, Ease.SNAP).k(t0 + len - 4, 18, 0, 0).k(t0 + len, 0, 0, 0);
        b.part(ROOT).key(t0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(t0 + 4, 0, 0, 0, 0, -0.18, 0, Ease.SNAP)
                .key(t0 + len - 4, 0, 0, 0, 0, -0.16, 0, Ease.LINEAR).key(t0 + len, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
    }

    // ------------------------------------------------------------------ колдовство

    /** Руки к небу. */
    public static void raise(Builder b, int t0, int len) {
        int top = t0 + (int) (len * 0.45);
        b.part(ARM_R).k(t0, 0, 0, 0).k(top, -172, -8, 16, Ease.SMOOTH).k(t0 + len - 2, -168, -6, 14)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_L).k(t0, 0, 0, 0).k(top, -172, 8, -16, Ease.SMOOTH).k(t0 + len - 2, -168, 6, -14)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(top, -12, 0, 0).k(t0 + len, 0, 0, 0);
        b.part(HEAD).k(t0, 0, 0, 0).k(top, -22, 0, 0).k(t0 + len, 0, 0, 0);
    }

    /** Ладонь вперёд — толчок силой. */
    public static void palm(Builder b, Anim.Part arm, int t0, int len) {
        int back = t0 + (int) (len * 0.35), hit = t0 + (int) (len * 0.55);
        int k = s(arm);
        b.part(arm).k(t0, 0, 0, 0).k(back, -50, 40 * k, 30 * k, Ease.SMOOTH)
                .key(hit, -96, 0, -6 * k, 0, 0, -3, Ease.SNAP).k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(back, -6, 14 * k, 0).k(hit, 12, -10 * k, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
    }

    /** Обе ладони вперёд. */
    public static void push(Builder b, int t0, int len) {
        int back = t0 + (int) (len * 0.4), hit = t0 + (int) (len * 0.58);
        b.part(ARM_R).k(t0, 0, 0, 0).k(back, -70, 45, 20, Ease.SMOOTH).key(hit, -95, 6, -4, 0, 0, -3, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_L).k(t0, 0, 0, 0).k(back, -70, -45, -20, Ease.SMOOTH).key(hit, -95, -6, 4, 0, 0, -3, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(back, -8, 0, 0).k(hit, 14, 0, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
    }

    /** Чтение: руки в стороны, ладони вверх, лёгкая дрожь. Для {@code .cast} — с {@code loop()}. */
    public static void channel(Builder b, int t0, int len) {
        int q = Math.max(2, len / 4);
        b.part(ARM_R).k(t0, -62, -28, 58).k(t0 + q, -70, -24, 64, Ease.SHAKE).k(t0 + 2 * q, -60, -30, 56, Ease.SHAKE)
                .k(t0 + 3 * q, -70, -24, 64, Ease.SHAKE).k(t0 + len, -62, -28, 58, Ease.SHAKE);
        b.part(ARM_L).k(t0, -62, 28, -58).k(t0 + q, -58, 24, -52, Ease.SHAKE).k(t0 + 2 * q, -70, 30, -64, Ease.SHAKE)
                .k(t0 + 3 * q, -58, 24, -52, Ease.SHAKE).k(t0 + len, -62, 28, -58, Ease.SHAKE);
        b.part(BODY).k(t0, -5, 0, 0).k(t0 + 2 * q, -8, 0, 0).k(t0 + len, -5, 0, 0);
        b.part(HEAD).k(t0, -6, 0, 0).k(t0 + 2 * q, -10, 0, 0).k(t0 + len, -6, 0, 0);
    }

    /** Чертить знак в воздухе одной рукой. */
    public static void sign(Builder b, Anim.Part arm, int t0, int len) {
        int k = s(arm), q = Math.max(1, len / 4);
        b.part(arm).k(t0, 0, 0, 0).k(t0 + q, -110, 20 * k, 28 * k, Ease.SMOOTH).k(t0 + 2 * q, -75, -18 * k, 46 * k)
                .k(t0 + 3 * q, -120, 24 * k, 22 * k).key(t0 + len - 1, -95, 0, 10 * k, 0, 0, -2, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(HEAD).k(t0, 0, 0, 0).k(t0 + 2 * q, -8, 6 * k, 0).k(t0 + len, 0, 0, 0);
    }

    /** Ладонь к земле: призыв, тотем, росток. */
    public static void toGround(Builder b, Anim.Part arm, int t0, int len) {
        int down = t0 + (int) (len * 0.5);
        int k = s(arm);
        b.part(arm).k(t0, 0, 0, 0).k(t0 + 3, -140, -10 * k, 20 * k, Ease.SMOOTH).k(down, 25, 8 * k, 6 * k, Ease.SNAP)
                .k(t0 + len - 2, 20, 6 * k, 5 * k).k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(t0 + 3, -8, 8 * k, 0).k(down, 32, -6 * k, 0, Ease.SNAP).k(t0 + len - 2, 28, -4 * k, 0)
                .k(t0 + len, 0, 0, 0);
        b.part(ROOT).key(t0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(down, 0, 0, 0, 0, -0.14, 0, Ease.SNAP)
                .key(t0 + len - 2, 0, 0, 0, 0, -0.12, 0, Ease.LINEAR).key(t0 + len, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        b.add(LEG_R).k(t0, 0, 0, 0).k(down, -30, 0, 14, Ease.SNAP).k(t0 + len - 2, -28, 0, 12).k(t0 + len, 0, 0, 0);
    }


    /** Только рука к земле, без приседания — когда телом в это время занято другое движение. */
    public static void handDown(Builder b, Anim.Part arm, int t0, int len) {
        int down = t0 + (int) (len * 0.5);
        int k = s(arm);
        b.part(arm).k(t0, 0, 0, 0).k(t0 + 2, -135, -10 * k, 22 * k, Ease.SMOOTH).k(down, 28, 8 * k, 6 * k, Ease.SNAP)
                .k(t0 + len - 1, 22, 6 * k, 5 * k).k(t0 + len, 0, 0, 0, Ease.SMOOTH);
    }

    /** Указать на цель. */
    public static void point(Builder b, Anim.Part arm, int t0, int len) {
        int k = s(arm);
        b.part(arm).k(t0, 0, 0, 0).k(t0 + 2, -60, 30 * k, 22 * k, Ease.SMOOTH)
                .key(t0 + (int) (len * 0.45), -94, 0, 4 * k, 0, 0, -2, Ease.SNAP).k(t0 + len - 2, -92, 0, 4 * k)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(HEAD).k(t0, 0, 0, 0).k(t0 + (int) (len * 0.45), 0, -6 * k, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
    }

    /** Схватить и рвануть на себя. */
    public static void pull(Builder b, Anim.Part arm, int t0, int len) {
        int out = t0 + (int) (len * 0.35), back = t0 + (int) (len * 0.6);
        int k = s(arm);
        b.part(arm).k(t0, 0, 0, 0).key(out, -92, 8 * k, 6 * k, 0, 0, -4, Ease.SMOOTH)
                .key(back, -40, 48 * k, 26 * k, 0, 0, 4, Ease.SNAP).k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(out, 10, -8 * k, 0).k(back, -12, 20 * k, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
        b.part(ROOT).key(t0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(back, 0, 0, 0, 0, 0, -0.08, Ease.SNAP)
                .key(t0 + len, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
    }

    /** Сгрести руками вверх (подкинуть). */
    public static void scoop(Builder b, int t0, int len) {
        int low = t0 + (int) (len * 0.3), up = t0 + (int) (len * 0.55);
        b.part(ARM_R).k(t0, 0, 0, 0).k(low, 40, 20, 14, Ease.SMOOTH).k(up, -182, -6, 24, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_L).k(t0, 0, 0, 0).k(low, 40, -20, -14, Ease.SMOOTH).k(up, -182, 6, -24, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(low, 22, 0, 0).k(up, -16, 0, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
        b.part(ROOT).key(t0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(low, 0, 0, 0, 0, -0.1, 0, Ease.SMOOTH)
                .key(up, 0, 0, 0, 0, 0.12, 0, Ease.SNAP).key(t0 + len, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
    }

    // ------------------------------------------------------------------ стрельба и броски

    /** Натянуть лук и отпустить. */
    public static void bow(Builder b, int t0, int len) {
        int draw = t0 + (int) (len * 0.5), shot = t0 + (int) (len * 0.62);
        b.part(ARM_L).k(t0, 0, 0, 0).k(draw, -92, -12, -8, Ease.SMOOTH).k(shot, -88, -10, -6, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_R).k(t0, 0, 0, 0).k(draw, -86, -52, 10, Ease.SMOOTH).k(shot, -80, -20, 6, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(draw, 0, 20, 0).k(shot, 0, 10, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
    }

    /** Вскинуть и выстрелить с отдачей. */
    public static void shot(Builder b, Anim.Part arm, int t0, int len) {
        int aim = t0 + (int) (len * 0.4), fire = t0 + (int) (len * 0.55);
        int k = s(arm);
        b.part(arm).k(t0, 0, 0, 0).k(aim, -94, 6 * k, 8 * k, Ease.SMOOTH).key(fire, -108, 6 * k, 8 * k, 0, 0, 2, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(other(arm)).k(t0, 0, 0, 0).k(aim, -78, -22 * k, -14 * k, Ease.SMOOTH).k(fire, -74, -20 * k, -12 * k, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(aim, 0, 12 * k, 0).k(fire, -6, 14 * k, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
        b.part(ROOT).key(t0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(fire, 0, 0, 0, 0, 0, -0.06, Ease.SNAP)
                .key(t0 + len, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
    }

    /** Бросок из-за плеча. */
    public static void toss(Builder b, Anim.Part arm, int t0, int len) {
        int back = t0 + (int) (len * 0.4), out = t0 + (int) (len * 0.58);
        int k = s(arm);
        b.part(arm).k(t0, 0, 0, 0).k(back, 95, -30 * k, 25 * k, Ease.SMOOTH).k(out, -150, 10 * k, -10 * k, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(back, -10, 24 * k, 0).k(out, 14, -18 * k, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
    }

    /** Бросок снизу (сеть, ловушка, склянка). */
    public static void lob(Builder b, Anim.Part arm, int t0, int len) {
        int back = t0 + (int) (len * 0.35), out = t0 + (int) (len * 0.55);
        int k = s(arm);
        b.part(arm).k(t0, 0, 0, 0).k(back, 55, 20 * k, 16 * k, Ease.SMOOTH).k(out, -120, -8 * k, -6 * k, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(back, 16, 10 * k, 0).k(out, -8, -8 * k, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
    }

    /** Хлыст, цепь, гарпун: бросок сбоку и возврат. */
    public static void lash(Builder b, Anim.Part arm, int t0, int len) {
        int back = t0 + (int) (len * 0.3), out = t0 + (int) (len * 0.5), ret = t0 + (int) (len * 0.78);
        int k = s(arm);
        b.part(arm).k(t0, 0, 0, 0).k(back, -30, 70 * k, 40 * k, Ease.SMOOTH)
                .key(out, -100, -30 * k, 6 * k, 0, 0, -3, Ease.SNAP).k(ret, -45, 40 * k, 20 * k, Ease.SMOOTH)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(back, 0, 24 * k, 0).k(out, 8, -16 * k, 0, Ease.SNAP).k(ret, -4, 10 * k, 0)
                .k(t0 + len, 0, 0, 0);
    }

    // ------------------------------------------------------------------ движение всем телом

    /** Рывок: присесть, наклон вперёд, руки назад. */
    public static void dash(Builder b, int t0, int len) {
        b.part(ARM_R).k(t0, 0, 0, 0).k(t0 + 2, 70, -20, 20, Ease.SNAP).k(t0 + len - 2, 60, -16, 16)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_L).k(t0, 0, 0, 0).k(t0 + 2, 70, 20, -20, Ease.SNAP).k(t0 + len - 2, 60, 16, -16)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(t0 + 2, 30, 0, 0, Ease.SNAP).k(t0 + len - 2, 26, 0, 0).k(t0 + len, 0, 0, 0);
        b.part(ROOT).key(t0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(t0 + 2, 0, 0, 0, 0, -0.1, 0, Ease.SNAP)
                .key(t0 + len - 2, 0, 0, 0, 0, -0.08, 0, Ease.LINEAR).key(t0 + len, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
    }

    /** Прыжок вверх с подтянутыми ногами. */
    public static void leap(Builder b, int t0, int len) {
        int up = t0 + (int) (len * 0.45);
        b.part(ARM_R).k(t0, 0, 0, 0).k(t0 + 3, 45, 0, 15, Ease.SMOOTH).k(up, -175, -10, 20, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_L).k(t0, 0, 0, 0).k(t0 + 3, 45, 0, -15, Ease.SMOOTH).k(up, -175, 10, -20, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(LEG_R).k(t0, 0, 0, 0).k(t0 + 3, 25, 0, 0).k(up, -55, 0, 10, Ease.SNAP).k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(LEG_L).k(t0, 0, 0, 0).k(t0 + 3, 25, 0, 0).k(up, -40, 0, -10, Ease.SNAP).k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ROOT).key(t0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(t0 + 3, 0, 0, 0, 0, -0.14, 0, Ease.SMOOTH)
                .key(up, 0, 0, 0, 0, 0.3, 0, Ease.SNAP).key(t0 + len, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
    }

    /** Кувырок через себя. */
    public static void flip(Builder b, int t0, int len) {
        b.part(ARM_R).k(t0, 0, 0, 0).k(t0 + 2, -170, -10, 25, Ease.SNAP).k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_L).k(t0, 0, 0, 0).k(t0 + 2, -170, 10, -25, Ease.SNAP).k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(LEG_R).k(t0, 0, 0, 0).k(t0 + len / 2, -70, 0, 10, Ease.SMOOTH).k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(LEG_L).k(t0, 0, 0, 0).k(t0 + len / 2, -50, 0, -10, Ease.SMOOTH).k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ROOT).key(t0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(t0 + len / 2, -180, 0, 0, 0, 0.45, 0, Ease.LINEAR)
                .key(t0 + len - 1, -360, 0, 0, 0, 0, 0, Ease.LINEAR).key(t0 + len, 0, 0, 0, 0, 0, 0, Ease.STEP);
    }

    /** Исчезнуть: сжаться и уйти вниз. */
    public static void sink(Builder b, int t0, int len) {
        b.part(ARM_R).k(t0, 0, 0, 0).k(t0 + 3, -20, 40, 10, Ease.SNAP).k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_L).k(t0, 0, 0, 0).k(t0 + 3, -20, -40, -10, Ease.SNAP).k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(t0 + 3, 24, 0, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
        b.part(ROOT).key(t0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(t0 + len - 2, 0, 0, 0, 0, -0.5, 0, Ease.SMOOTH)
                .key(t0 + len, 0, 0, 0, 0, 0, 0, Ease.STEP);
    }

    /** Клич: голову вверх, руки назад, грудь вперёд. */
    public static void roar(Builder b, int t0, int len) {
        int top = t0 + (int) (len * 0.4);
        b.part(ARM_R).k(t0, 0, 0, 0).k(top, 55, -35, 40, Ease.SNAP).k(t0 + len - 3, 48, -30, 36).k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_L).k(t0, 0, 0, 0).k(top, 55, 35, -40, Ease.SNAP).k(t0 + len - 3, 48, 30, -36).k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(top, -18, 0, 0, Ease.SNAP).k(t0 + len - 3, -14, 0, 0).k(t0 + len, 0, 0, 0);
        b.part(HEAD).k(t0, 0, 0, 0).k(top, -32, 0, 0, Ease.SNAP).k(t0 + len - 3, -28, 0, 0).k(t0 + len, 0, 0, 0);
        b.part(ROOT).key(t0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(top, 0, 0, 0, 0, 0.06, 0, Ease.SNAP)
                .key(t0 + len, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
    }

    /** Молитва: ладони вместе, голова вниз. */
    public static void pray(Builder b, int t0, int len) {
        b.part(ARM_R).k(t0, 0, 0, 0).k(t0 + 4, -118, 48, 12, Ease.SMOOTH).k(t0 + len - 4, -115, 45, 10)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_L).k(t0, 0, 0, 0).k(t0 + 4, -118, -48, -12, Ease.SMOOTH).k(t0 + len - 4, -115, -45, -10)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(HEAD).k(t0, 0, 0, 0).k(t0 + 4, 22, 0, 0, Ease.SMOOTH).k(t0 + len - 4, 20, 0, 0).k(t0 + len, 0, 0, 0);
        b.part(BODY).k(t0, 0, 0, 0).k(t0 + 4, 6, 0, 0).k(t0 + len, 0, 0, 0);
    }

    /** Благословение: руки вверх и мягко развести ладонями вниз. */
    public static void bless(Builder b, int t0, int len) {
        int top = t0 + (int) (len * 0.35), open = t0 + (int) (len * 0.65);
        b.part(ARM_R).k(t0, 0, 0, 0).k(top, -165, -20, 10, Ease.SMOOTH).k(open, -105, -40, 55, Ease.SMOOTH)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_L).k(t0, 0, 0, 0).k(top, -165, 20, -10, Ease.SMOOTH).k(open, -105, 40, -55, Ease.SMOOTH)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(HEAD).k(t0, 0, 0, 0).k(top, -18, 0, 0).k(open, 8, 0, 0).k(t0 + len, 0, 0, 0);
        b.part(BODY).k(t0, 0, 0, 0).k(top, -8, 0, 0).k(open, 4, 0, 0).k(t0 + len, 0, 0, 0);
    }

    /** Лечение: одна рука к союзнику, другая у сердца. */
    public static void mend(Builder b, Anim.Part arm, int t0, int len) {
        int out = t0 + (int) (len * 0.4);
        int k = s(arm);
        b.part(arm).k(t0, 0, 0, 0).k(out, -92, 10 * k, 10 * k, Ease.SMOOTH).k(t0 + len - 3, -88, 8 * k, 8 * k)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(other(arm)).k(t0, 0, 0, 0).k(out, -125, -55 * k, -18 * k, Ease.SMOOTH).k(t0 + len - 3, -120, -52 * k, -16 * k)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(out, -4, -6 * k, 0).k(t0 + len, 0, 0, 0);
    }

    /** Воткнуть знамя в землю. */
    public static void plant(Builder b, int t0, int len) {
        int up = t0 + (int) (len * 0.4), down = t0 + (int) (len * 0.6);
        b.part(ARM_R).k(t0, 0, 0, 0).k(up, -185, -14, 14, Ease.SMOOTH).key(down, -12, 6, 4, 0, 0, -2, Ease.SNAP)
                .k(t0 + len - 2, -10, 4, 3).k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(ARM_L).k(t0, 0, 0, 0).k(up, -95, 35, -20, Ease.SMOOTH).k(down, -35, 20, -10, Ease.SNAP)
                .k(t0 + len, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(up, -10, 0, 0).k(down, 22, 0, 0, Ease.SNAP).k(t0 + len, 0, 0, 0);
        b.part(ROOT).key(t0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH).key(up, 0, 0, 0, 0, 0.12, 0, Ease.SMOOTH)
                .key(down, 0, 0, 0, 0, -0.1, 0, Ease.SNAP).key(t0 + len, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
    }

    /** Дрожь всем телом (сила рвётся наружу). */
    public static void tremble(Builder b, int t0, int len) {
        int q = Math.max(1, len / 6);
        b.part(ROOT).key(t0, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        for (int i = 1; i <= 5; i++) {
            b.part(ROOT).key(t0 + i * q, 0, 0, i % 2 == 0 ? 3 : -3, i % 2 == 0 ? 0.05 : -0.05, 0.02, 0, Ease.SNAP);
        }
        b.part(ROOT).key(t0 + len, 0, 0, 0, 0, 0, 0, Ease.SMOOTH);
        b.part(BODY).k(t0, 0, 0, 0).k(t0 + 2 * q, 10, 6, 0, Ease.SHAKE).k(t0 + 4 * q, 8, -6, 0, Ease.SHAKE)
                .k(t0 + len, 0, 0, 0);
    }
}
