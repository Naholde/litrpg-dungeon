package com.andrey.litrpg.client.look;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Облик класса: geo.json того же вида, что пишет каркас моделей ({@code tools/models/hy.py}).
 * Геометрия считается так же, как у GeckoLib и у {@code tools/model_preview.py} (ось x отражена, поворот Z·Y·X
 * с минусом у X и Y) — поэтому в игре облик выглядит ровно как на превью.
 * <p>
 * Кости верхнего уровня называются как части тела игрока ({@code head, body, right_arm, left_arm, right_leg, left_leg})
 * и при отрисовке идут за ними; кости {@code cape*} дополнительно развеваются, как ванильный плащ.
 */
public final class LookModel {
    /** Грань: 4 угла (x, y, z в пикселях модели, отражённая ось x), uv в долях текстуры, нормаль. */
    public record Quad(float[] pos, float[] uv, Vector3f normal) {
    }

    public static final class Bone {
        public final String name;
        public final String parent;
        /** Опорная точка в отражённых координатах, пиксели. */
        public final Vector3f pivot;
        /** Свой поворот кости из файла (вокруг опорной точки). */
        public final Matrix4f local;
        public final List<Quad> quads = new ArrayList<>();
        public final List<Bone> children = new ArrayList<>();

        Bone(String name, String parent, Vector3f pivot, Matrix4f local) {
            this.name = name;
            this.parent = parent;
            this.pivot = pivot;
            this.local = local;
        }

        public boolean cape() {
            return name.startsWith("cape");
        }
    }

    /** Углы граней в долях куба: верх-лево, верх-право, низ-право, низ-лево текстуры (как в model_preview.py). */
    private static final Map<String, float[][]> FACES = Map.of(
            "north", new float[][]{{1, 1, 0}, {0, 1, 0}, {0, 0, 0}, {1, 0, 0}, {0, 0, -1}},
            "south", new float[][]{{0, 1, 1}, {1, 1, 1}, {1, 0, 1}, {0, 0, 1}, {0, 0, 1}},
            "east", new float[][]{{0, 1, 0}, {0, 1, 1}, {0, 0, 1}, {0, 0, 0}, {-1, 0, 0}},
            "west", new float[][]{{1, 1, 1}, {1, 1, 0}, {1, 0, 0}, {1, 0, 1}, {1, 0, 0}},
            "up", new float[][]{{0, 1, 1}, {1, 1, 1}, {1, 1, 0}, {0, 1, 0}, {0, 1, 0}},
            "down", new float[][]{{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}, {0, -1, 0}});

    public final List<Bone> tops = new ArrayList<>();
    public final int quadCount;

    private LookModel(List<Bone> tops, int quadCount) {
        this.tops.addAll(tops);
        this.quadCount = quadCount;
    }

    /** Поворот как у GeckoLib: Z·Y·X, у X и Y знак минус (градусы из файла). */
    static Matrix4f euler(float rx, float ry, float rz) {
        return new Matrix4f()
                .rotateZ((float) Math.toRadians(rz))
                .rotateY((float) Math.toRadians(-ry))
                .rotateX((float) Math.toRadians(-rx));
    }

    private static Matrix4f around(Vector3f p, Matrix4f r) {
        return new Matrix4f().translate(p).mul(r).translate(-p.x, -p.y, -p.z);
    }

    private static float[] vec(JsonElement e, int n) {
        float[] out = new float[n];
        if (e == null) return out;
        JsonArray a = e.getAsJsonArray();
        for (int i = 0; i < n && i < a.size(); i++) out[i] = a.get(i).getAsFloat();
        return out;
    }

    public static LookModel parse(JsonObject root) {
        JsonObject g = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject d = g.getAsJsonObject("description");
        float tw = d.get("texture_width").getAsFloat(), th = d.get("texture_height").getAsFloat();
        Map<String, Bone> by = new HashMap<>();
        List<Bone> order = new ArrayList<>();
        int quads = 0;
        for (JsonElement be : g.getAsJsonArray("bones")) {
            JsonObject b = be.getAsJsonObject();
            String name = b.get("name").getAsString();
            String parent = b.has("parent") ? b.get("parent").getAsString() : null;
            float[] p = vec(b.get("pivot"), 3);
            Vector3f pivot = new Vector3f(-p[0], p[1], p[2]);
            float[] r = vec(b.get("rotation"), 3);
            Bone bone = new Bone(name, parent, pivot, around(pivot, euler(r[0], r[1], r[2])));
            if (b.has("cubes")) {
                for (JsonElement ce : b.getAsJsonArray("cubes")) quads += cube(ce.getAsJsonObject(), bone, tw, th);
            }
            by.put(name, bone);
            order.add(bone);
        }
        List<Bone> tops = new ArrayList<>();
        for (Bone b : order) {
            Bone parent = b.parent == null ? null : by.get(b.parent);
            if (parent != null && !"root".equals(parent.name)) parent.children.add(b);
            else if (!"root".equals(b.name)) tops.add(b);
        }
        return new LookModel(tops, quads);
    }

    private static int cube(JsonObject c, Bone bone, float tw, float th) {
        float[] o = vec(c.get("origin"), 3), s = vec(c.get("size"), 3);
        float inf = c.has("inflate") ? c.get("inflate").getAsFloat() : 0;
        float x0 = -(o[0] + s[0]) - inf, y0 = o[1] - inf, z0 = o[2] - inf;
        float ex = s[0] + 2 * inf, ey = s[1] + 2 * inf, ez = s[2] + 2 * inf;
        Matrix4f m = new Matrix4f();
        if (c.has("rotation")) {
            float[] r = vec(c.get("rotation"), 3);
            float[] cp = vec(c.get("pivot"), 3);
            m = around(new Vector3f(-cp[0], cp[1], cp[2]), euler(r[0], r[1], r[2]));
        }
        JsonObject uv = c.getAsJsonObject("uv");
        int n = 0;
        for (var f : FACES.entrySet()) {
            if (uv == null || !uv.has(f.getKey())) continue;
            JsonObject spec = uv.getAsJsonObject(f.getKey());
            float[] uv0 = vec(spec.get("uv"), 2), us = vec(spec.get("uv_size"), 2);
            if (us[0] == 0 || us[1] == 0) continue;
            float[][] k = f.getValue();
            float[] pos = new float[12];
            for (int i = 0; i < 4; i++) {
                Vector4f v = m.transform(new Vector4f(x0 + ex * k[i][0], y0 + ey * k[i][1], z0 + ez * k[i][2], 1));
                pos[i * 3] = v.x;
                pos[i * 3 + 1] = v.y;
                pos[i * 3 + 2] = v.z;
            }
            float u0 = uv0[0] / tw, v0 = uv0[1] / th, u1 = (uv0[0] + us[0]) / tw, v1 = (uv0[1] + us[1]) / th;
            float[] uvs = {u0, v0, u1, v0, u1, v1, u0, v1};
            Vector3f normal = m.transformDirection(new Vector3f(k[4][0], k[4][1], k[4][2])).normalize();
            bone.quads.add(new Quad(pos, uvs, normal));
            n++;
        }
        return n;
    }
}
