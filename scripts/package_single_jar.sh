#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INPUT_JAR=""
OUTPUT_JAR=""
KEEP_TMP=0

usage() {
  cat <<'EOF'
Package Comet-Raids into a single distributable JAR.

Usage:
  scripts/package_single_jar.sh --input <plugin.jar> [--output <out.jar>]
  scripts/package_single_jar.sh --input <plugin.jar> [--output-dir <dir>]

Examples:
  scripts/package_single_jar.sh --input /path/to/CometMod.jar
  scripts/package_single_jar.sh --input build/libs/CometMod.jar --output dist/Comet-Raids-all-in-one.jar

Notes:
  - This script does not compile Java sources.
  - It repacks an existing plugin JAR and injects:
      Common/
      Server/
      manifest.json
      comet_config.json
      fixed_spawns.json
EOF
}

die() {
  echo "Error: $*" >&2
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --input|-i)
      [[ $# -ge 2 ]] || die "--input requires a value"
      INPUT_JAR="$2"
      shift 2
      ;;
    --output|-o)
      [[ $# -ge 2 ]] || die "--output requires a value"
      OUTPUT_JAR="$2"
      shift 2
      ;;
    --output-dir)
      [[ $# -ge 2 ]] || die "--output-dir requires a value"
      out_dir="$2"
      shift 2
      ;;
    --keep-tmp)
      KEEP_TMP=1
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      die "Unknown argument: $1 (use --help)"
      ;;
  esac
done

[[ -n "$INPUT_JAR" ]] || die "Missing --input <plugin.jar>"
[[ -f "$INPUT_JAR" ]] || die "Input JAR not found: $INPUT_JAR"

for cmd in unzip zip; do
  command -v "$cmd" >/dev/null 2>&1 || die "Required command not found: $cmd"
done

for required in Common Server manifest.json comet_config.json fixed_spawns.json; do
  [[ -e "$ROOT_DIR/$required" ]] || die "Required project path missing: $required"
done

input_base="$(basename "$INPUT_JAR")"
input_stem="${input_base%.jar}"

if [[ -z "${OUTPUT_JAR:-}" ]]; then
  if [[ -n "${out_dir:-}" ]]; then
    OUTPUT_JAR="$out_dir/${input_stem}-all-in-one.jar"
  else
    OUTPUT_JAR="$ROOT_DIR/dist/${input_stem}-all-in-one.jar"
  fi
fi

mkdir -p "$(dirname "$OUTPUT_JAR")"
OUTPUT_JAR_ABS="$(cd "$(dirname "$OUTPUT_JAR")" && pwd)/$(basename "$OUTPUT_JAR")"

TMP_DIR="$(mktemp -d)"
cleanup() {
  if [[ "$KEEP_TMP" -eq 0 ]]; then
    rm -rf "$TMP_DIR"
  else
    echo "Temporary directory kept: $TMP_DIR"
  fi
}
trap cleanup EXIT

STAGING_DIR="$TMP_DIR/staging"
mkdir -p "$STAGING_DIR"

unzip -qq "$INPUT_JAR" -d "$STAGING_DIR"

# Repacking invalidates signatures from any previously signed jar.
if [[ -d "$STAGING_DIR/META-INF" ]]; then
  find "$STAGING_DIR/META-INF" -type f \( -name '*.SF' -o -name '*.RSA' -o -name '*.DSA' \) -delete || true
fi

cp -a "$ROOT_DIR/Common" "$STAGING_DIR/"
cp -a "$ROOT_DIR/Server" "$STAGING_DIR/"
cp -a "$ROOT_DIR/manifest.json" "$STAGING_DIR/"
cp -a "$ROOT_DIR/comet_config.json" "$STAGING_DIR/"
cp -a "$ROOT_DIR/fixed_spawns.json" "$STAGING_DIR/"

rm -f "$OUTPUT_JAR_ABS"
(cd "$STAGING_DIR" && zip -X -r -q "$OUTPUT_JAR_ABS" .)

echo "Created: $OUTPUT_JAR_ABS"
echo "Size: $(du -h "$OUTPUT_JAR_ABS" | awk '{print $1}')"
echo "Included roots:"
echo "  - Common/"
echo "  - Server/"
echo "  - manifest.json"
echo "  - comet_config.json"
echo "  - fixed_spawns.json"
