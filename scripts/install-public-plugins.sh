#!/usr/bin/env bash
set -euo pipefail

server_dir="${1:-/opt/csdm-verify/server}"
plugins_dir="$server_dir/plugins"
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

if pgrep -f '[p]aper\.jar' >/dev/null; then
  echo "Paper sigue ejecutandose. Escribe 'stop' en su consola antes de instalar plugins." >&2
  exit 1
fi

install -d -o minecraft -g minecraft -m 0750 "$plugins_dir"
install -d -o minecraft -g minecraft -m 0750 "$plugins_dir/bStats"
if [[ -f "$plugins_dir/bStats/config.yml" ]] && grep -Eq '^enabled:[[:space:]]*true' "$plugins_dir/bStats/config.yml"; then
  echo "bStats ya existe y esta habilitado. Desactivalo antes de continuar." >&2
  exit 1
fi
if [[ ! -f "$plugins_dir/bStats/config.yml" ]]; then
  install -o minecraft -g minecraft -m 0640 "$script_dir/bStats-config.yml" "$plugins_dir/bStats/config.yml"
fi
tmp_dir="$(mktemp -d)"
trap 'rm -rf -- "$tmp_dir"' EXIT

download_and_verify() {
  local filename="$1"
  local url="$2"
  local sha512="$3"
  local staged="$tmp_dir/$filename"

  curl -fL --retry 3 --proto '=https' --tlsv1.2 "$url" -o "$staged"
  printf '%s  %s\n' "$sha512" "$staged" | sha512sum --check --status
  install -o minecraft -g minecraft -m 0640 "$staged" "$plugins_dir/$filename"
  echo "Instalado: $filename"
}

download_and_verify \
  "LuckPerms-Bukkit-5.5.71.jar" \
  "https://cdn.modrinth.com/data/Vebnzrzj/versions/b0mk8uS6/LuckPerms-Bukkit-5.5.71.jar" \
  "188a91f0a543d23bfda32385fca6db63d61e49c8a422bd452a260bd9cbc6a7d7fe45071199e9fca8f3ce43c2b41ee84fd315bd15464577028ff3951a7d4fab27"

download_and_verify \
  "FancyNpcs-2.11.0.jar" \
  "https://cdn.modrinth.com/data/EeyAn23L/versions/zM6uZoPe/FancyNpcs-2.11.0.jar" \
  "e275e9349d9357280438189487b121057c1b98b9b074a3a20a9ffc55f5903397c22443ac83cdc704a0acdb120e9a8ef54dccf73b4eef6349f39b2babad35b907"

download_and_verify \
  "FancyHolograms-2.11.0.jar" \
  "https://cdn.modrinth.com/data/5QNgOj66/versions/VgbsP5NO/FancyHolograms-2.11.0.jar" \
  "ef2817552f96ea2ebb5635df5b6049cd9f66b00761ae2ccd1deb721feff2470a9131b12e52d43862f6fb2663ae54d87cee063ec6aad5d09d1df283133641fbc9"

echo "Plugins publicos instalados y verificados en $plugins_dir"
