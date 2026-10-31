#!/usr/bin/env bash
# Команда серверу на VPS: bash tools/dev/srv.sh "litrpg level set Dev 20"
ssh -i "${LITRPG_KEY:-$HOME/.ssh/litrpg_vps}" "${LITRPG_HOST:-root@109.248.163.212}" "mc-cmd $(printf '%q' "$*")"
