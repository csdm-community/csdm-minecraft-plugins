#!/usr/bin/env bash
set -euo pipefail

server_dir=/opt/csdm-verify/server
service=csdm-verify
jar="$server_dir/plugins/CSDMAdmin.jar"
expected_sha=7bc12a57294bf72698b570c90d0ef0a5cd3e04299060042f126b270bbeacea3a
source_url=https://raw.githubusercontent.com/csdm-community/csdm-minecraft-plugins/1c43edf212a9ffabc3dc5e94a1a670ef1b800244/releases/0.3.5/CSDMAdmin.jar

[[ $EUID -eq 0 ]] || { echo 'Ejecuta como root.' >&2; exit 1; }
[[ -f "$jar" ]] || { echo 'No se encontró CSDMAdmin.jar.' >&2; exit 1; }
[[ $(systemctl show "$service" -p WorkingDirectory --value) == "$server_dir" ]] || exit 1
systemctl is-active --quiet "$service"

download=$(mktemp /root/csdm-admin-0.3.5.XXXXXX.jar)
pending_restart=0
backup=''
cleanup() {
    status=$?
    trap - EXIT
    if [[ $status -ne 0 && $pending_restart -eq 1 ]]; then
        echo 'La instalación falló; restaurando el JAR anterior.' >&2
        systemctl stop "$service" || true
        if [[ $(systemctl show "$service" -p MainPID --value) == 0 ]]; then
            cp -p "$backup/CSDMAdmin.jar" "$jar"
            systemctl start "$service" || true
        else
            echo "Paper sigue ejecutándose. Respaldo: $backup" >&2
        fi
    fi
    rm -f "$download"
    exit "$status"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

curl -fL --retry 3 --connect-timeout 15 --max-time 120 "$source_url" -o "$download"
echo "$expected_sha  $download" | sha256sum -c -
python3 -c 'import sys,zipfile; z=zipfile.ZipFile(sys.argv[1]); assert z.testzip() is None; assert "staff-messages.yml" in z.namelist()' "$download"

mkdir -p /opt/csdm-verify/backups
backup=$(mktemp -d /opt/csdm-verify/backups/staff-mode-0.3.5.XXXXXX)
cp -p "$jar" "$backup/CSDMAdmin.jar"
jar_uid=$(stat -c %u "$jar")
jar_gid=$(stat -c %g "$jar")
jar_mode=$(stat -c %a "$jar")
pending_restart=1
systemctl stop "$service"
[[ $(systemctl show "$service" -p MainPID --value) == 0 ]]
if [[ -d "$server_dir/plugins/CSDMAdmin" ]]; then
    cp -a "$server_dir/plugins/CSDMAdmin" "$backup/CSDMAdmin"
fi
install -o "$jar_uid" -g "$jar_gid" -m "$jar_mode" "$download" "$jar"
systemctl start "$service"
systemctl is-active --quiet "$service"
pending_restart=0
echo "CSDMAdmin 0.3.5 copiado y arranque solicitado. Respaldo: $backup"
echo 'Cuando termine el arranque, entra y activa /sm y prueba la brújula o /staff tp.'
