#!/usr/bin/env bash
# Готовит папку run/ для dev-клиента: без экрана первого запуска, русский язык, без паузы при потере фокуса.
set -e
cd "$(dirname "$0")/../.."
mkdir -p run
[ -f run/options.txt ] && grep -q '^onboardAccessibility:false' run/options.txt && exit 0
cp tools/dev/options.txt run/options.txt
echo "run/options.txt подготовлен"
