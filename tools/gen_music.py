"""
Музыка владельца → src/main/resources/assets/litrpg/sounds/music/<id>.ogg (Vorbis, громкость выровнена до −16 LUFS,
плавное начало и конец) и записи в sounds.json. Папка с треками в .gitignore: на GitHub их нет, в jar — есть.

    python tools/gen_music.py --src "C:/Users/arsen/Downloads"

Какой трек где звучит — TRACKS (ключ — часть имени файла). Хранителю трек назначается в BossRegistry (поле music),
этапу — в gen_dimension.py (музыка биома).
"""
import argparse
import json
import os
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/litrpg"
FFMPEG = os.environ.get("FFMPEG", r"C:\Users\arsen\.litrpg-tools\ffmpeg_dl\ffmpeg-master-latest-win64-gpl\bin\ffmpeg.exe")

# id трека: (часть имени файла, подпись для Кодекса)
TRACKS = {
    "woops": ("Bountyhunter_-_Woops", "Bountyhunter — Woops"),
    "formula": ("Formula_1_Theme", "Brian Tyler — Formula 1 Theme"),
    "kurayami": ("Kurayami No Naka", "Dj Univxrsel — Kurayami No Naka (Slowed)"),
    "ecstasy": ("The_Ecstasy_Of_Gold", "Ennio Morricone — The Ecstasy of Gold"),
    "aura": ("Aura of Glory", "Eru Flow Flame — Aura of Glory 2.0"),
    "fight_club": ("Fight Club Soundtrack", "The Dust Brothers — Fight Club Main Theme"),
    "no_time": ("No_Time_for_Caution", "Hans Zimmer — No Time for Caution"),
    "fear": ("Hensonn_-_Fear", "Hensonn — Fear"),
    "blue_lock": ("BLUE_LOCK", "NUEKI, TOLCHONOV — BLUE LOCK EKI"),
    "jane": ("Jane_Hoodtrap", "Qura — Jane Hoodtrap"),
    "fragment": ("fragment_slowed", "SLXUGHTER — fragment (slowed)"),
    "ring_frei": ("Ring_frei", "Schwarzer Engel — Ring frei"),
    "time_flies": ("As_Time_Flies", "Tys Music — As Time Flies"),
    "red": ("Vessel_-_Red", "Vessel — Red Sex"),
    "blud": ("blud (Ultra Slowed)", "blud (Ultra Slowed)"),
    "next_opponent": ("Your_Next_Opponent_Is_You", "ugolki soznaniya — Your Next Opponent Is You"),
    "wutiwant": ("wutiwant", "wutiwant"),
}


def convert(src, dst):
    dst.parent.mkdir(parents=True, exist_ok=True)
    # длительность — для затухания в конце
    probe = subprocess.run([FFMPEG.replace("ffmpeg.exe", "ffprobe.exe"), "-v", "error", "-show_entries", "format=duration",
                            "-of", "csv=p=0", str(src)], capture_output=True, text=True)
    dur = float(probe.stdout.strip() or 0)
    fade = f"afade=t=in:d=0.6,afade=t=out:st={max(0.0, dur - 2.5):.2f}:d=2.5" if dur > 10 else "anull"
    subprocess.run([FFMPEG, "-v", "error", "-y", "-i", str(src), "-vn", "-map_metadata", "-1",
                    "-af", f"loudnorm=I=-16:TP=-1.5:LRA=11,{fade}", "-ar", "44100", "-ac", "2",
                    "-c:a", "libvorbis", "-q:a", "3", str(dst)], check=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--src", required=True, help="папка с mp3 владельца")
    args = ap.parse_args()
    files = [f for f in Path(args.src).iterdir() if f.suffix.lower() == ".mp3"]
    sounds_path = ROOT / "sounds.json"
    sounds = json.loads(sounds_path.read_text(encoding="utf-8")) if sounds_path.exists() else {}
    for tid, (needle, title) in TRACKS.items():
        match = [f for f in files if needle.lower() in f.name.lower()]
        if not match:
            print(f"  нет файла для «{tid}» ({needle})")
            continue
        dst = ROOT / "sounds/music" / f"{tid}.ogg"
        convert(match[0], dst)
        sounds[f"music.{tid}"] = {"sounds": [{"name": f"litrpg:music/{tid}", "stream": True}]}
        print(f"  {tid}: {match[0].name} → {dst.name} ({dst.stat().st_size // 1024} КБ)")
    sounds_path.write_text(json.dumps(sounds, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


if __name__ == "__main__":
    main()
