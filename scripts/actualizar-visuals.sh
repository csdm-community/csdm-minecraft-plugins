#!/usr/bin/env bash
# Build a pinned revision before stopping Paper; update only CSDMVisuals.
set -euo pipefail
revision=${1:?Uso: bash actualizar-visuals.sh SHA_COMPLETO}
[[ "$revision" =~ ^[a-f0-9]{40}$ ]] || { echo 'SHA inválido'; exit 1; }
[[ $EUID -eq 0 ]] || { echo 'Ejecuta como root en el VPS.'; exit 1; }
server=/opt/csdm-verify/server
target="$server/plugins/CSDMVisuals.jar"
[[ -f "$target" ]] || { echo 'No se encontró CSDMVisuals.jar'; exit 1; }
systemctl is-active --quiet csdm-verify
install -d -m 0755 /opt/csdm-builds /opt/csdm-backups
work=$(mktemp -d /opt/csdm-builds/visuals-0.3.15.XXXXXX)
curl --fail --location --retry 3 \
  "https://codeload.github.com/csdm-community/csdm-minecraft-plugins/tar.gz/$revision" \
  -o "$work/source.tar.gz"
mkdir "$work/source"
tar -xzf "$work/source.tar.gz" -C "$work/source" --strip-components=1
chown -R minecraft:minecraft "$work"
chmod 0755 "$work"
runuser -u minecraft -- bash -c \
  'cd "$1"; bash ./gradlew :csdm-visuals:build --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx768m' \
  bash "$work/source"
jar="$work/source/csdm-visuals/build/libs/csdm-visuals-0.3.15.jar"
[[ -s "$jar" ]] || { echo 'No se generó el JAR esperado'; exit 1; }
backup=$(mktemp -d /opt/csdm-backups/visuals-0.3.15.XXXXXX)
cp -a "$target" "$backup/CSDMVisuals.jar"
if [[ -d "$server/plugins/CSDMVisuals" ]]; then
  cp -a "$server/plugins/CSDMVisuals" "$backup/configuracion"
fi
echo "RESPALDO: $backup"
rollback() {
  trap - ERR
  echo 'Falló la actualización. Restaurando el JAR anterior...'
  systemctl stop csdm-verify || true
  cp -a "$backup/CSDMVisuals.jar" "$target"
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
      && grep -q 'Enabling CSDMVisuals v0.3.15' "$server/logs/latest.log" \
      && grep -q 'Done (' "$server/logs/latest.log"; then
    ready=true
    break
  fi
  sleep 2
done
[[ "$ready" == true ]]
if grep -Eq 'Error occurred while enabling CSDMVisuals|Disabling CSDMVisuals v0.3.15' "$server/logs/latest.log"; then
  rollback
fi
systemctl is-active --quiet csdm-verify
trap - ERR
echo 'CSDMVisuals 0.3.15 instalado. Vuelve a entrar desde Bedrock y Java.'
grep -E 'CSDMVisuals|ERROR|Done \(' "$server/logs/latest.log" | tail -n 20
