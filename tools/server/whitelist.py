#!/usr/bin/env python3
"""Белый список для сервера без лицензии (online-mode=false, TLauncher).

Команда /whitelist add ищет ник на серверах Mojang и записывает UUID лицензии — а игрок с TLauncher
заходит с «офлайн»-UUID, и сервер его не пускает. Этот скрипт пишет правильный UUID.

  python3 whitelist.py add Ник [Ник…]    python3 whitelist.py remove Ник    python3 whitelist.py list
Затем: mc-cmd "whitelist reload".
"""
import hashlib
import json
import sys
import uuid
from pathlib import Path

FILE = Path("/opt/litrpg/server/whitelist.json")


def offline_uuid(name):
    # Как в Minecraft: UUID.nameUUIDFromBytes("OfflinePlayer:" + name) — MD5, версия 3.
    h = bytearray(hashlib.md5(("OfflinePlayer:" + name).encode("utf-8")).digest())
    h[6] = (h[6] & 0x0F) | 0x30
    h[8] = (h[8] & 0x3F) | 0x80
    return str(uuid.UUID(bytes=bytes(h)))


def main():
    entries = json.loads(FILE.read_text(encoding="utf-8")) if FILE.exists() else []
    cmd, names = (sys.argv[1] if len(sys.argv) > 1 else "list"), sys.argv[2:]
    if cmd == "add":
        for n in names:
            entries = [e for e in entries if e["name"].lower() != n.lower()]
            entries.append({"uuid": offline_uuid(n), "name": n})
    elif cmd == "remove":
        low = {n.lower() for n in names}
        entries = [e for e in entries if e["name"].lower() not in low]
    for e in entries:
        print(e["name"], e["uuid"])
    if cmd in ("add", "remove"):
        FILE.write_text(json.dumps(entries, indent=2, ensure_ascii=False), encoding="utf-8")


if __name__ == "__main__":
    main()
