#!/usr/bin/env bash
# Первичная настройка VPS (Ubuntu 24.04) под сервер LitRPG. Запускать от root; повторный запуск безопасен.
#   scp -r tools/server root@HOST:/root/litrpg-setup && ssh root@HOST bash /root/litrpg-setup/setup.sh
set -euo pipefail

NEO_VERSION="${NEO_VERSION:-21.1.265}"
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT=/opt/litrpg
SERVER=$ROOT/server

export DEBIAN_FRONTEND=noninteractive
echo "== Часовой пояс"
timedatectl set-timezone Europe/Moscow

echo "== Пакеты"
# Старые конфиги оставляем (иначе dpkg спрашивает про cloud.cfg и падает без терминала).
APT_OPTS=(-y -q -o Acquire::Retries=5 -o Dpkg::Options::=--force-confdef -o Dpkg::Options::=--force-confold)
dpkg --configure -a --force-confdef --force-confold || true
# Зеркало хостинга иногда отвечает 503 — тогда переходим на основное зеркало Ubuntu.
if ! apt-get -o Acquire::Retries=5 update -q || ! apt-get "${APT_OPTS[@]}" --download-only upgrade; then
  sed -i 's#http://[a-z.]*clouds.archive.ubuntu.com#http://archive.ubuntu.com#' /etc/apt/sources.list.d/ubuntu.sources
  apt-get -o Acquire::Retries=5 update -q
fi
apt-get "${APT_OPTS[@]}" upgrade
apt-get "${APT_OPTS[@]}" install openjdk-21-jre-headless ufw fail2ban unattended-upgrades curl python3 zip

echo "== SSH: только по ключу"
cat > /etc/ssh/sshd_config.d/00-litrpg.conf <<'EOF'
PasswordAuthentication no
KbdInteractiveAuthentication no
PermitRootLogin prohibit-password
EOF
sshd -t
systemctl reload ssh

echo "== Брандмауэр: SSH и Minecraft"
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp
ufw allow 25565/tcp
ufw --force enable
systemctl enable --now fail2ban

echo "== Пользователь и папки"
id mc >/dev/null 2>&1 || useradd -r -m -d $ROOT -s /bin/bash mc
mkdir -p $SERVER/mods $ROOT/backups

echo "== NeoForge $NEO_VERSION"
if [ ! -f "$SERVER/libraries/net/neoforged/neoforge/$NEO_VERSION/unix_args.txt" ]; then
  cd $SERVER
  curl -fsSL -o installer.jar "https://maven.neoforged.net/releases/net/neoforged/neoforge/$NEO_VERSION/neoforge-$NEO_VERSION-installer.jar"
  java -jar installer.jar --installServer >/dev/null
  rm -f installer.jar installer.jar.log
fi

echo "== Настройки сервера"
cp "$HERE/user_jvm_args.txt" $SERVER/user_jvm_args.txt
echo "eula=true" > $SERVER/eula.txt
if [ ! -f $SERVER/server.properties ]; then
  RCON_PASS="$(head -c 24 /dev/urandom | base64 | tr -d '/+=')"
  sed "s/__RCON_PASSWORD__/$RCON_PASS/" "$HERE/server.properties" > $SERVER/server.properties
fi
install -m 755 "$HERE/rcon.py" $ROOT/rcon.py
install -m 755 "$HERE/backup.sh" $ROOT/backup.sh
install -m 755 "$HERE/whitelist.py" $ROOT/whitelist.py
cat > /usr/local/bin/mc-cmd <<EOF
#!/bin/sh
exec python3 $ROOT/rcon.py $SERVER/server.properties "\$@"
EOF
chmod 755 /usr/local/bin/mc-cmd
chown -R mc:mc $ROOT

echo "== Служба и бэкапы"
install -m 644 "$HERE/litrpg.service" /etc/systemd/system/litrpg.service
systemctl daemon-reload
systemctl enable litrpg
echo "0 4 * * * root $ROOT/backup.sh >> $ROOT/backups/backup.log 2>&1" > /etc/cron.d/litrpg-backup

echo "== Готово. Запуск: systemctl start litrpg; команды: mc-cmd list"
