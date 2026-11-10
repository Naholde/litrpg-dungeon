package com.andrey.litrpg.world;

import com.andrey.litrpg.LitRPG;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

/**
 * Блоки подземелья из {@code /litrpg_map/map.bin} (пишет {@code tools/gen_dungeon.py}). Генератор мира ставит их в чанки,
 * а прохождение по ним восстанавливает этап после вайпа («как было»).
 * <p>Формат (gzip): {@code LRPGMAP1}, палитра строк-состояний, затем секции 16³: координаты секции и RLE пар
 * (длина, индекс палитры) в порядке {@code (y·16 + z)·16 + x}.
 */
public final class DungeonMap {
    public static final String RESOURCE = "/litrpg_map/map.bin";
    private static volatile DungeonMap instance;

    private final BlockState[] palette;
    /** Секция → индексы палитры (short, 4096). */
    private final Map<Long, short[]> sections;
    private final String hash;
    /** Строки палитры, которые игра не узнала (заменены воздухом): ошибка генератора карты — тест требует пустоты. */
    private final java.util.List<String> unknown;

    private DungeonMap(BlockState[] palette, Map<Long, short[]> sections, String hash, java.util.List<String> unknown) {
        this.palette = palette;
        this.sections = sections;
        this.hash = hash;
        this.unknown = java.util.List.copyOf(unknown);
    }

    /** Блоки карты, которых нет в игре (опечатка в имени или лишнее свойство, напр. ось у ножки гриба). */
    public java.util.List<String> unknownBlocks() {
        return unknown;
    }

    public static DungeonMap get() {
        DungeonMap m = instance;
        if (m == null) {
            synchronized (DungeonMap.class) {
                m = instance;
                if (m == null) instance = m = load();
            }
        }
        return m;
    }

    /** Версия карты — хеш файла. Изменилась → мир подземелья на сервере пересоздаётся. */
    public static String fileHash() {
        try (InputStream in = DungeonMap.class.getResourceAsStream(RESOURCE)) {
            if (in == null) return "none";
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(in.readAllBytes())).substring(0, 16);
        } catch (Exception e) {
            return "error";
        }
    }

    private static DungeonMap load() {
        long t0 = System.currentTimeMillis();
        try (InputStream raw = DungeonMap.class.getResourceAsStream(RESOURCE)) {
            if (raw == null) {
                LitRPG.LOG.error("[LitRPG] Нет карты {} — подземелье будет пустым", RESOURCE);
                return new DungeonMap(new BlockState[]{Blocks.AIR.defaultBlockState()}, Map.of(), "none", java.util.List.of());
            }
            byte[] bytes = raw.readAllBytes();
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes)).substring(0, 16);
            DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)));
            byte[] magic = new byte[8];
            in.readFully(magic);
            if (!"LRPGMAP1".equals(new String(magic, java.nio.charset.StandardCharsets.US_ASCII))) {
                throw new IOException("не карта LitRPG");
            }
            int n = in.readInt();
            BlockState[] palette = new BlockState[n];
            java.util.List<String> unknown = new java.util.ArrayList<>();
            for (int i = 0; i < n; i++) palette[i] = parse(in.readUTF(), unknown);
            int count = in.readInt();
            Map<Long, short[]> sections = new ConcurrentHashMap<>(count * 2);
            for (int s = 0; s < count; s++) {
                int sx = in.readInt(), sy = in.readInt(), sz = in.readInt();
                short[] data = new short[4096];
                int i = 0;
                while (i < 4096) {
                    int len = readVarInt(in), idx = readVarInt(in);
                    for (int k = 0; k < len; k++) data[i++] = (short) idx;
                }
                sections.put(SectionPos.asLong(sx, sy, sz), data);
            }
            LitRPG.LOG.info("[LitRPG] Карта подземелья: {} секций, палитра {}, версия {} ({} мс)", count, n, hash,
                    System.currentTimeMillis() - t0);
            return new DungeonMap(palette, sections, hash, unknown);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось прочитать карту подземелья " + RESOURCE, e);
        }
    }

    private static BlockState parse(String s, java.util.List<String> unknown) {
        try {
            return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), s, false).blockState();
        } catch (Exception e) {
            LitRPG.LOG.error("[LitRPG] Карта: неизвестный блок «{}» — заменён воздухом", s);
            unknown.add(s);
            return Blocks.AIR.defaultBlockState();
        }
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int value = 0, shift = 0;
        while (true) {
            int b = in.readUnsignedByte();
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return value;
            shift += 7;
        }
    }

    public String hash() {
        return hash;
    }

    /** Секция карты или null (пусто). */
    public short[] section(int sx, int sy, int sz) {
        return sections.get(SectionPos.asLong(sx, sy, sz));
    }

    public BlockState state(int index) {
        return palette[index];
    }

    /** Исходный блок в точке (воздух — если карта там ничего не ставила). */
    public BlockState original(BlockPos pos) {
        short[] s = section(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
        if (s == null) return Blocks.AIR.defaultBlockState();
        return palette[s[((pos.getY() & 15) * 16 + (pos.getZ() & 15)) * 16 + (pos.getX() & 15)]];
    }
}
