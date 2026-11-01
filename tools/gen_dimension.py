"""
Измерение подземелья: тип измерения (без неба и солнца, тусклый фон), измерение (генератор по карте) и биомы этапов
(цвет тумана и воды, частицы в воздухе, звуки пещеры). Музыка этапов подключается в gen_music.py.

    python tools/gen_dimension.py
"""
import json
from pathlib import Path

DATA = Path(__file__).resolve().parent.parent / "src/main/resources/data/litrpg"

# ключ: (fog, water, water_fog, частица, вероятность)
BIOMES = {
    "lobby": (0x1A1428, 0x3D57D6, 0x050533, "minecraft:white_ash", 0.002),
    "crypt": (0x14161B, 0x3A4A5A, 0x0A1018, "minecraft:ash", 0.004),
    "thicket": (0x122016, 0x3A6B3A, 0x0C1E0C, "minecraft:warped_spore", 0.006),
    "temple": (0x0A1C2A, 0x1E6F8A, 0x08283A, "minecraft:underwater", 0.012),
    "forge": (0x2A0F08, 0x8A4A1E, 0x2A1004, "minecraft:white_ash", 0.012),
    "spire": (0x0C0614, 0x5A2A8A, 0x12061E, "minecraft:reverse_portal", 0.004),
}
ORDER = ["lobby", "crypt", "thicket", "temple", "forge", "spire"]


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


def main():
    write(DATA / "dimension_type/dungeon.json", {
        "ultrawarm": False, "natural": False, "piglin_safe": False, "respawn_anchor_works": False, "bed_works": False,
        "has_raids": False, "has_skylight": False, "has_ceiling": True, "coordinate_scale": 1.0, "ambient_light": 0.08,
        "fixed_time": 18000, "logical_height": 256, "effects": "litrpg:dungeon",
        "infiniburn": "#minecraft:infiniburn_overworld", "min_y": 0, "height": 256,
        "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0,
    })
    write(DATA / "dimension/dungeon.json", {
        "type": "litrpg:dungeon",
        "generator": {"type": "litrpg:dungeon",
                      "biome_source": {"type": "litrpg:stages", "stages": [f"litrpg:{k}" for k in ORDER]}},
    })
    for key, (fog, water, water_fog, particle, prob) in BIOMES.items():
        write(DATA / f"worldgen/biome/{key}.json", {
            "has_precipitation": False, "temperature": 0.5, "downfall": 0.0,
            "effects": {
                "fog_color": fog, "sky_color": fog, "water_color": water, "water_fog_color": water_fog,
                "particle": {"options": {"type": particle}, "probability": prob},
                "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
            },
            "spawners": {}, "spawn_costs": {}, "carvers": {}, "features": [],
        })
    print("Измерение и", len(BIOMES), "биомов записаны.")


if __name__ == "__main__":
    main()
