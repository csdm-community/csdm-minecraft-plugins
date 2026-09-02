#!/usr/bin/env bash
set -euo pipefail

server_dir="${1:-/opt/csdm-verify/server}"
artifact_dir="${2:-$(pwd)}"
plugins_dir="$server_dir/plugins"

if pgrep -f '[p]aper\.jar' >/dev/null; then
  echo "Paper sigue ejecutandose. Escribe 'stop' en su consola antes de instalar plugins." >&2
  exit 1
fi

install -d -o minecraft -g minecraft -m 0750 "$plugins_dir"

for plugin in CSDMVerify CSDMCommunity CSDMAdmin; do
  source_file="$artifact_dir/$plugin.jar"
  if [[ ! -f "$source_file" ]]; then
    echo "Falta $source_file" >&2
    exit 1
  fi
  unzip -t "$source_file" >/dev/null
  install -o minecraft -g minecraft -m 0640 "$source_file" "$plugins_dir/$plugin.jar"
done

echo "Plugins CSDM instalados en $plugins_dir"
