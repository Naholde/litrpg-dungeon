package com.andrey.litrpg.client.music;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.ClientBoss;
import com.andrey.litrpg.client.ClientRun;
import com.andrey.litrpg.world.ModWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Музыка: у хранителя — его трек (пока идёт бой), в преддверии — тихая тема, на титрах — свой трек. Треки сменяются
 * плавно: новый нарастает за секунду, старый затихает за две. Ванильная музыка в подземелье молчит.
 */
public final class MusicController {
    private static Track playing;
    private static String current = "";
    /** Трек, заказанный экраном (титры); пусто — решает обстановка. */
    private static String forced = "";

    private MusicController() {
    }

    public static void force(String id) {
        forced = id == null ? "" : id;
    }

    /** Что должно звучать сейчас. */
    private static String desired() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return "";
        if (!forced.isEmpty()) return forced;
        if (ClientBoss.active() && !ClientBoss.music().isEmpty()) return ClientBoss.music();
        if (mc.level.dimension() == ModWorld.DUNGEON && ClientRun.stage() == 0 && mc.player.getX() < 1024) return "time_flies";
        return "";
    }

    /** Подземелье: своя музыка вместо ванильной. */
    public static boolean inDungeon() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.dimension() == ModWorld.DUNGEON;
    }

    public static void tick() {
        String want = desired();
        if (want.equals(current) && (playing == null || !playing.isStopped())) return;
        if (want.equals(current) && playing != null && playing.isStopped() && !want.isEmpty()) {
            playing = null; // доиграл без петли (не нашёлся файл) — не перезапускать каждую секунду
            return;
        }
        if (playing != null) playing.fadeOut();
        current = want;
        playing = null;
        if (want.isEmpty()) return;
        playing = new Track(LitRPG.id("music." + want));
        Minecraft.getInstance().getSoundManager().play(playing);
    }

    public static void stopAll() {
        if (playing != null) playing.fadeOut();
        playing = null;
        current = "";
        forced = "";
    }

    /** Трек с плавным нарастанием и затуханием, по кругу. */
    private static final class Track extends AbstractSoundInstance implements TickableSoundInstance {
        private float level;
        private boolean fading;
        private boolean stopped;

        Track(net.minecraft.resources.ResourceLocation id) {
            super(id, SoundSource.MUSIC, RandomSource.create());
            looping = true;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
            volume = 0.01f;
        }

        void fadeOut() {
            fading = true;
        }

        @Override
        public boolean isStopped() {
            return stopped;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            level = fading ? level - 0.025f : Math.min(1f, level + 0.05f);
            if (level <= 0) {
                stopped = true;
                level = 0;
            }
            volume = Math.max(0.001f, level);
        }
    }
}
