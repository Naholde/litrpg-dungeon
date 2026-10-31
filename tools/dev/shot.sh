#!/usr/bin/env bash
# Снимок экрана dev-клиента: bash tools/dev/shot.sh <имя> → run/screenshots/<имя>.png (ждёт появления файла).
set -e
cd "$(dirname "$0")/../../run"
rm -f "screenshots/$1.png"
echo "$1" > dev_screenshot.txt
for i in $(seq 1 40); do [ -f "screenshots/$1.png" ] && { echo "run/screenshots/$1.png"; exit 0; }; sleep 0.5; done
echo "снимок не появился" >&2; exit 1
