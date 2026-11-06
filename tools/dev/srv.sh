#!/usr/bin/env bash
# Команда серверу на VPS: bash tools/dev/srv.sh "litrpg level set Dev 20"
ssh -i "${LITRPG_KEY:-$HOME/.ssh/litrpg_vps}" "${LITRPG_HOST:-root@95.182.99.124}" "mc-cmd $(printf '%q' "$*")"
