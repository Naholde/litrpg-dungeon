package com.andrey.litrpg.anim;

import com.andrey.litrpg.anim.Anim.Key;
import com.andrey.litrpg.anim.Anim.Part;
import com.andrey.litrpg.anim.Anim.Track;

/**
 * Поза на мгновение времени: повороты и сдвиги по частям тела. Считается одинаково на клиенте и в тесте,
 * поэтому движение можно проверить обычным автотестом, не запуская игру.
 */
public final class AnimPose {
    private static final int N = Part.values().length;
    /** По 6 чисел на часть: поворот x, y, z (градусы) и сдвиг ox, oy, oz. */
    private final float[] v = new float[N * 6];
    private final boolean[] has = new boolean[N];
    private final boolean[] add = new boolean[N];
    /** Сила позы: 0 — обычная поза игры, 1 — полностью наша. */
    public float weight;

    public void clear() {
        java.util.Arrays.fill(v, 0);
        java.util.Arrays.fill(has, false);
        java.util.Arrays.fill(add, false);
        weight = 0;
    }

    public boolean has(Part p) {
        return has[p.ordinal()];
    }

    public boolean additive(Part p) {
        return add[p.ordinal()];
    }

    public float x(Part p) {
        return v[p.ordinal() * 6];
    }

    public float y(Part p) {
        return v[p.ordinal() * 6 + 1];
    }

    public float z(Part p) {
        return v[p.ordinal() * 6 + 2];
    }

    public float ox(Part p) {
        return v[p.ordinal() * 6 + 3];
    }

    public float oy(Part p) {
        return v[p.ordinal() * 6 + 4];
    }

    public float oz(Part p) {
        return v[p.ordinal() * 6 + 5];
    }

    /**
     * Снять позу движения {@code a} в момент {@code t} (тиков от начала, может быть дробным).
     * {@code speed} — во сколько раз движение растянуто (2 — вдвое медленнее).
     */
    public void sample(Anim a, float t, float speed) {
        clear();
        float len = Math.max(1, a.ticks * Math.max(0.05f, speed));
        float time = a.loop ? t % len : Math.min(t, len);
        float local = time / Math.max(0.0001f, speed);
        weight = fade(a, time, len);
        for (Track tr : a.tracks()) {
            int i = tr.part().ordinal();
            has[i] = true;
            add[i] = tr.additive();
            fill(tr, local, i);
        }
    }

    /** Плавный вход и выход: к началу и к концу движения сила падает до нуля. */
    private static float fade(Anim a, float time, float len) {
        if (a.loop) return 1;
        float f = Math.max(1, a.fade);
        float in = Math.min(1, time / f);
        float out = Math.min(1, (len - time) / f);
        return Math.max(0, Math.min(in, out));
    }

    private void fill(Track tr, float t, int i) {
        var keys = tr.keys();
        Key prev = keys.get(0);
        if (t <= prev.t()) {
            set(i, prev, prev, 0, Anim.Ease.LINEAR);
            return;
        }
        for (int k = 1; k < keys.size(); k++) {
            Key next = keys.get(k);
            if (t <= next.t()) {
                float span = Math.max(1e-4f, next.t() - prev.t());
                set(i, prev, next, Math.min(1, (t - prev.t()) / span), next.ease());
                return;
            }
            prev = next;
        }
        set(i, prev, prev, 0, Anim.Ease.LINEAR);
    }

    private void set(int i, Key a, Key b, float p, Anim.Ease e) {
        float k = Anim.ease(e, p);
        v[i * 6] = a.x() + (b.x() - a.x()) * k;
        v[i * 6 + 1] = a.y() + (b.y() - a.y()) * k;
        v[i * 6 + 2] = a.z() + (b.z() - a.z()) * k;
        v[i * 6 + 3] = a.ox() + (b.ox() - a.ox()) * k;
        v[i * 6 + 4] = a.oy() + (b.oy() - a.oy()) * k;
        v[i * 6 + 5] = a.oz() + (b.oz() - a.oz()) * k;
    }
}
