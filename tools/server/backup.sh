#!/usr/bin/env bash
# Ночной бэкап мира: сохранение выключается на время архивации, хранится 7 дней.
set -uo pipefail
ROOT=/opt/litrpg
STAMP="$(date +%Y-%m-%d_%H%M)"
cd $ROOT/server || exit 1
[ -d world ] || { echo "$STAMP: мира ещё нет"; exit 0; }

running=0
if mc-cmd "save-off" >/dev/null 2>&1; then
  running=1
  mc-cmd "save-all flush" >/dev/null 2>&1
  sleep 5
fi
tar czf "$ROOT/backups/world-$STAMP.tar.gz" world
[ $running = 1 ] && mc-cmd "save-on" >/dev/null 2>&1
chown mc:mc "$ROOT/backups/world-$STAMP.tar.gz"
find $ROOT/backups -name 'world-*.tar.gz' -mtime +7 -delete
echo "$STAMP: готово, $(du -h "$ROOT/backups/world-$STAMP.tar.gz" | cut -f1)"
