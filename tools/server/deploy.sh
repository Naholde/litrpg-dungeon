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
# Зависимости (GeckoLib, Curios) — из кэша Gradle, если на сервере их ещё нет.
dep() { # dep <артефакт в кэше> <маска старых jar на сервере>
  local f; f="$(find "$HOME/.gradle/caches/modules-2" -name "$1" | head -1)"
  [ -z "$f" ] && { echo "нет в кэше: $1" >&2; return; }
  if ! ssh -i "$KEY" "$HOST" "test -f /opt/litrpg/server/mods/$(basename "$f")"; then
    scp -q -i "$KEY" "$f" "$HOST:/tmp/dep.jar"
    ssh -i "$KEY" "$HOST" "rm -f /opt/litrpg/server/mods/$2; install -o mc -g mc -m 644 /tmp/dep.jar /opt/litrpg/server/mods/$(basename "$f"); rm /tmp/dep.jar"
    echo "Установлено: $(basename "$f")"
  fi
}
prop() { grep "^$1=" gradle.properties | cut -d= -f2; }
dep "geckolib-neoforge-1.21.1-$(prop geckolib_version).jar" "geckolib-*.jar"
dep "curios-neoforge-$(prop curios_version).jar" "curios-*.jar"
scp -q -i "$KEY" "$JAR" "$HOST:/tmp/litrpg-new.jar"
ssh -i "$KEY" "$HOST" 'set -e
  rm -f /opt/litrpg/server/mods/litrpg-dungeon-*.jar
  install -o mc -g mc -m 644 /tmp/litrpg-new.jar /opt/litrpg/server/mods/'"$(basename "$JAR")"'
  rm /tmp/litrpg-new.jar
  systemctl restart litrpg
  echo "Сервер перезапущен"'
