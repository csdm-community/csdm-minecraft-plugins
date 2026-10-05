#!/usr/bin/env bash
# Build a pinned revision before stopping Paper; update only CSDMCommunity.
set -euo pipefail
revision=${1:?Uso: bash actualizar-community.sh SHA_COMPLETO}
[[ "$revision" =~ ^[a-f0-9]{40}$ ]] || { echo 'SHA inválido'; exit 1; }
[[ $EUID -eq 0 ]] || { echo 'Ejecuta como root en el VPS.'; exit 1; }
server=/opt/csdm-verify/server
target="$server/plugins/CSDMCommunity.jar"
[[ -f "$target" ]] || { echo 'No se encontró CSDMCommunity.jar'; exit 1; }
systemctl is-active --quiet csdm-verify
install -d -m 0755 /opt/csdm-builds /opt/csdm-backups
work=$(mktemp -d /opt/csdm-builds/community-0.3.13.XXXXXX)
curl --fail --location --retry 3 \
  "https://codeload.github.com/csdm-community/csdm-minecraft-plugins/tar.gz/$revision" \
  -o "$work/source.tar.gz"
mkdir "$work/source"
tar -xzf "$work/source.tar.gz" -C "$work/source" --strip-components=1
chown -R minecraft:minecraft "$work"
chmod 0755 "$work"
runuser -u minecraft -- bash -c \
  'cd "$1"; bash ./gradlew :csdm-community:build --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx768m' \
  bash "$work/source"
jar="$work/source/csdm-community/build/libs/csdm-community-0.3.13.jar"
[[ -s "$jar" ]] || { echo 'No se generó el JAR esperado'; exit 1; }
backup=$(mktemp -d /opt/csdm-backups/community-0.3.13.XXXXXX)
cp -a "$target" "$backup/CSDMCommunity.jar"
if [[ -d "$server/plugins/CSDMCommunity" ]]; then
  cp -a "$server/plugins/CSDMCommunity" "$backup/configuracion"
fi
echo "RESPALDO: $backup"
rollback() {
  trap - ERR
  echo 'Falló la actualización. Restaurando el JAR anterior...'
  systemctl stop csdm-verify || true
  cp -a "$backup/CSDMCommunity.jar" "$target"
  systemctl start csdm-verify
  exit 1
}
trap rollback ERR
systemctl stop csdm-verify
install -o minecraft -g minecraft -m 0640 "$jar" "$target"
touch "$work/start-marker"
systemctl start csdm-verify
# Wait for this startup, not for a previous log's Done line.
ready=false
for ((attempt=0; attempt<90; attempt++)); do
  if [[ "$server/logs/latest.log" -nt "$work/start-marker" ]] \
      && grep -q 'Enabling CSDMCommunity v0.3.13' "$server/logs/latest.log" \
      && grep -q 'Done (' "$server/logs/latest.log"; then
    ready=true
    break
  fi
  sleep 2
done
[[ "$ready" == true ]]
if grep -Eq 'Error occurred while enabling CSDMCommunity|Disabling CSDMCommunity v0.3.13' "$server/logs/latest.log"; then
  rollback
fi
systemctl is-active --quiet csdm-verify
trap - ERR
echo 'CSDMCommunity 0.3.13 instalado. Vuelve a entrar desde Bedrock y Java.'
grep -E 'CSDMCommunity|ERROR|Done \(' "$server/logs/latest.log" | tail -n 20
