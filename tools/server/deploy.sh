#!/usr/bin/env bash
# Собрать мод и поставить его на сервер с перезапуском.
#   bash tools/server/deploy.sh            — сборка + установка
#   bash tools/server/deploy.sh --no-build — только установка готового jar
set -euo pipefail
HOST="${LITRPG_HOST:-root@109.248.163.212}"
KEY="${LITRPG_KEY:-$HOME/.ssh/litrpg_vps}"
cd "$(dirname "$0")/../.."

if [ "${1:-}" != "--no-build" ]; then
  export JAVA_HOME="${JAVA_HOME:-C:/Users/arsen/.litrpg-tools/jdk-21.0.13+9}"
  ./gradlew.bat build --console=plain -q
fi
JAR="$(ls -t build/libs/litrpg-dungeon-*.jar | head -1)"
echo "Ставлю $JAR"
# Зависимости (GeckoLib) — из кэша Gradle, если на сервере их ещё нет.
GECKO_VER="$(grep '^geckolib_version=' gradle.properties | cut -d= -f2)"
GECKO="$(find "$HOME/.gradle/caches/modules-2" -name "geckolib-neoforge-1.21.1-$GECKO_VER.jar" | head -1)"
if [ -n "$GECKO" ] && ! ssh -i "$KEY" "$HOST" "test -f /opt/litrpg/server/mods/$(basename "$GECKO")"; then
  scp -q -i "$KEY" "$GECKO" "$HOST:/tmp/gecko.jar"
  ssh -i "$KEY" "$HOST" "rm -f /opt/litrpg/server/mods/geckolib-*.jar; install -o mc -g mc -m 644 /tmp/gecko.jar /opt/litrpg/server/mods/$(basename "$GECKO"); rm /tmp/gecko.jar"
  echo "GeckoLib $GECKO_VER установлен"
fi
scp -q -i "$KEY" "$JAR" "$HOST:/tmp/litrpg-new.jar"
ssh -i "$KEY" "$HOST" 'set -e
  rm -f /opt/litrpg/server/mods/litrpg-dungeon-*.jar
  install -o mc -g mc -m 644 /tmp/litrpg-new.jar /opt/litrpg/server/mods/'"$(basename "$JAR")"'
  rm /tmp/litrpg-new.jar
  systemctl restart litrpg
  echo "Сервер перезапущен"'
