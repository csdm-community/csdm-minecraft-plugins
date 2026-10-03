#!/usr/bin/env bash
set -euo pipefail

server_dir=/opt/csdm-verify/server
service=csdm-verify
release_url=https://raw.githubusercontent.com/csdm-community/csdm-minecraft-plugins/89451d42c50d5450fd7d62b3dd1c5a4c11239603/releases/0.3.8
[[ $EUID -eq 0 ]] || { echo 'Ejecuta como root.' >&2; exit 1; }
[[ $(systemctl show "$service" -p WorkingDirectory --value) == "$server_dir" ]] || exit 1
systemctl is-active --quiet "$service"
for name in CSDMAdmin CSDMVerify; do
    [[ -f "$server_dir/plugins/$name.jar" ]] || { echo "Falta $name.jar" >&2; exit 1; }
done

download=$(mktemp -d /root/csdm-rescate-0.3.8.XXXXXX)
pending_restart=0
backup=''
cleanup() {
    status=$?
    trap - EXIT
    if [[ $status -ne 0 && $pending_restart -eq 1 ]]; then
        echo 'La instalación falló; restaurando los dos JAR anteriores.' >&2
        systemctl stop "$service" || true
        if [[ $(systemctl show "$service" -p MainPID --value) == 0 ]]; then
            for name in CSDMAdmin CSDMVerify; do
                cp -p "$backup/$name.jar" "$server_dir/plugins/$name.jar"
            done
            systemctl start "$service" || true
        else
            echo "Paper sigue ejecutándose. Respaldo: $backup" >&2
        fi
    fi
    rm -rf -- "$download"
    exit "$status"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

for name in CSDMAdmin CSDMVerify; do
    curl -fL --retry 3 --connect-timeout 15 --max-time 120 "$release_url/$name.jar" -o "$download/$name.jar"
done
printf '%s  %s\n' 870e5bb87fbdae5c1e35e288cf34e92e411ef4a4f6ffa4616a2bddb9a573e97b "$download/CSDMAdmin.jar" a6aa06195e661f32434aee53aa39488a9d9a1435b1a919626fce1f1c83f06523 "$download/CSDMVerify.jar" | sha256sum -c -
python3 - "$download" <<'PY'
import sys, zipfile
from pathlib import Path
for name in ('CSDMAdmin', 'CSDMVerify'):
    with zipfile.ZipFile(Path(sys.argv[1]) / (name + '.jar')) as jar:
        assert jar.testzip() is None
        descriptor = jar.read('plugin.yml').decode()
        assert name in descriptor and '0.3.8' in descriptor
PY
mkdir -p /opt/csdm-verify/backups
backup=$(mktemp -d /opt/csdm-verify/backups/rescate-verify-0.3.8.XXXXXX)
for name in CSDMAdmin CSDMVerify; do
    cp -p "$server_dir/plugins/$name.jar" "$backup/$name.jar"
done
pending_restart=1
systemctl stop "$service"
[[ $(systemctl show "$service" -p MainPID --value) == 0 ]]
for name in CSDMAdmin CSDMVerify; do
    jar="$server_dir/plugins/$name.jar"
    install -o "$(stat -c %u "$jar")" -g "$(stat -c %g "$jar")" -m "$(stat -c %a "$jar")" "$download/$name.jar" "$jar"
done
systemctl start "$service"
systemctl is-active --quiet "$service"
pending_restart=0
echo "CSDMAdmin y CSDMVerify 0.3.8 copiados; arranque solicitado. Respaldo: $backup"
echo 'Espera a que termine de iniciar Minecraft y prueba la caída desde verificación.'
