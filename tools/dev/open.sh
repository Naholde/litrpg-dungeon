#!/usr/bin/env bash
# Открыть окно в dev-клиенте: bash tools/dev/open.sh character 2 | role tank | close
cd "$(dirname "$0")/../../run" && echo "$*" > dev_open.txt && sleep 1.2
