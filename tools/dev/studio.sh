#!/usr/bin/env bash
# Фотостудия для моделей: светлая площадка на y=150 в подземелье, игрок в творческом режиме, ночь.
#   bash tools/dev/studio.sh                 — построить и встать
#   bash tools/dev/studio.sh morra vessa lich — построить и выставить модели в ряд (через 4 блока)
cd "$(dirname "$0")/../../run"
{
  echo "gamemode creative Dev"
  echo "time set midnight"
  echo "litrpg show clear"
  echo "execute in litrpg:dungeon run fill -4 149 -4 28 149 28 minecraft:sea_lantern"
  echo "execute in litrpg:dungeon run fill -4 150 -4 28 150 28 minecraft:gray_concrete"
  echo "execute in litrpg:dungeon run fill -3 151 -3 27 157 27 minecraft:light[level=15] replace minecraft:air"
} > dev_commands.txt
sleep 1.5
if [ $# -gt 0 ]; then
  n=$#; i=0; : > dev_commands.txt
  for e in "$@"; do
    x=$(( 12 - (n - 1) * 2 + i * 4 ))
    echo "execute in litrpg:dungeon run tp Dev $x 151 20 180 0" >> dev_commands.txt
    echo "execute as Dev at Dev run litrpg show $e" >> dev_commands.txt
    i=$((i + 1))
  done
  echo "execute in litrpg:dungeon run tp Dev 12 152 $(( 22 + n )) 180 8" >> dev_commands.txt
  sleep 1.5
fi
