package com.andrey.litrpg.anim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Движение игрока: набор дорожек по частям тела с кадрами во времени.
 * <p>Кадр задаёт поворот в <b>градусах</b> и сдвиг. Для рук, ног, тела и головы сдвиг — в «пикселях» модели
 * (1/16 блока), для {@link Part#ROOT} — в блоках, и там же {@code x} — наклон вперёд, {@code y} — поворот
 * вокруг себя, {@code z} — наклон на правый бок.
 * <p>Повороты частей тела — как в самой игре: у руки {@code x = −90} — вперёд, {@code −180} — вверх,
 * {@code +90} — назад; {@code z} у правой руки в плюс отводит её в сторону, у левой — к себе.
 * <p>Дорожка {@code additive} добавляется к обычной позе (ходьба, взгляд), обычная — заменяет её.
 * Вход и выход сглаживаются за {@link #fade} тиков, поэтому движение не «щёлкает».
 */
public final class Anim {
    /** Часть тела. {@code ROOT} — всё тело целиком (сдвиг и наклон при отрисовке). */
    public enum Part {HEAD, BODY, ARM_R, ARM_L, LEG_R, LEG_L, ROOT}

    /** Как идти к кадру: ровно, плавно, резко, с перехлёстом, с дрожью или рывком. */
    public enum Ease {LINEAR, SMOOTH, SNAP, BACK, SHAKE, STEP}

    public record Key(int t, float x, float y, float z, float ox, float oy, float oz, Ease ease) {
    }

    public record Track(Part part, boolean additive, List<Key> keys) {
    }

    public final String id;
    /** Длина в тиках. */
    public final int ticks;
    /** Сглаживание входа и выхода (тиков). */
    public final int fade;
    public final boolean loop;
    private final List<Track> tracks;

    private Anim(String id, int ticks, int fade, boolean loop, List<Track> tracks) {
        this.id = id;
        this.ticks = ticks;
        this.fade = fade;
        this.loop = loop;
        this.tracks = tracks;
    }

    public List<Track> tracks() {
        return tracks;
    }

    public Track track(Part p) {
        for (Track t : tracks) if (t.part() == p) return t;
        return null;
    }

    public static Builder of(String id, int ticks) {
        return new Builder(id, ticks);
    }

    /** Значение easing: 0 → 0, 1 → 1. */
    public static float ease(Ease e, float p) {
        float q = 1 - p;
        return switch (e) {
            case LINEAR -> p;
            case SMOOTH -> p * p * (3 - 2 * p);
            case SNAP -> 1 - q * q * q * q;
            case BACK -> 1 + 2.70158f * q * q * q - 1.70158f * q * q;
            case SHAKE -> p + (float) Math.sin(p * Math.PI * 6) * 0.09f * q;
            case STEP -> p >= 1 ? 1 : 0;
        };
    }

    public static final class Builder {
        private final String id;
        private final int ticks;
        private int fade = 2;
        private boolean loop;
        private final Map<Part, List<Key>> keys = new EnumMap<>(Part.class);
        private final Map<Part, Boolean> additive = new EnumMap<>(Part.class);
        private Part cur;

        private Builder(String id, int ticks) {
            this.id = id;
            this.ticks = ticks;
        }

        public Builder fade(int t) {
            fade = t;
            return this;
        }

        public Builder loop() {
            loop = true;
            return this;
        }

        /** Дальше кадры этой части тела. Голова по умолчанию добавляется к взгляду. */
        public Builder part(Part p) {
            cur = p;
            keys.computeIfAbsent(p, k -> new ArrayList<>());
            additive.putIfAbsent(p, p == Part.HEAD);
            return this;
        }

        /** Та же часть, но поверх обычной позы (например, ноги продолжают шагать). */
        public Builder add(Part p) {
            part(p);
            additive.put(p, true);
            return this;
        }

        public Builder k(int t, double x, double y, double z) {
            return k(t, x, y, z, Ease.SMOOTH);
        }

        public Builder k(int t, double x, double y, double z, Ease e) {
            return key(t, x, y, z, 0, 0, 0, e);
        }

        /** Кадр с поворотом и сдвигом. */
        public Builder key(int t, double x, double y, double z, double ox, double oy, double oz, Ease e) {
            if (cur == null) throw new IllegalStateException(id + ": сначала part(...)");
            keys.get(cur).add(new Key(t, (float) x, (float) y, (float) z, (float) ox, (float) oy, (float) oz, e));
            return this;
        }

        /** Кадр только со сдвигом. */
        public Builder o(int t, double ox, double oy, double oz) {
            return key(t, 0, 0, 0, ox, oy, oz, Ease.SMOOTH);
        }

        public Anim build() {
            List<Track> list = new ArrayList<>();
            for (var e : keys.entrySet()) {
                List<Key> ks = new ArrayList<>(e.getValue());
                ks.sort((a, b) -> Integer.compare(a.t(), b.t()));
                if (ks.isEmpty()) continue;
                list.add(new Track(e.getKey(), additive.get(e.getKey()), Collections.unmodifiableList(ks)));
            }
            return new Anim(id, ticks, fade, loop, Collections.unmodifiableList(list));
        }
    }
}
