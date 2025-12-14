#!/usr/bin/env bash

set -euo pipefail

# CONFIG
MC_VERSION="1.20.4"
SERVER_DIR="run"
PLUGINS_DIR="$SERVER_DIR/plugins"
PAPER_JAR="$SERVER_DIR/paper.jar"
META_DIR="$SERVER_DIR/.meta"
PAPER_BUILD_FILE="$META_DIR/paper_build.txt"

FLAGS_SCRIPT="$SERVER_DIR/flags.sh"
mkdir -p "$PLUGINS_DIR" "$META_DIR"

############################
# Helper: get latest Paper build
############################
get_latest_paper_build() {
  curl -s "https://api.papermc.io/v2/projects/paper/versions/$MC_VERSION" \
    | grep -o '"builds":[^]]*' \
    | grep -o '[0-9]\+' \
    | tail -1
}

############################
# Paper: download only if newer
############################
echo "Checking latest Paper build for $MC_VERSION..."
LATEST_BUILD="$(get_latest_paper_build)"
echo "Latest Paper build: $LATEST_BUILD"

CURRENT_BUILD="0"
if [[ -f "$PAPER_BUILD_FILE" ]]; then
  CURRENT_BUILD="$(cat "$PAPER_BUILD_FILE" || echo "0")"
fi

if [[ "$LATEST_BUILD" != "$CURRENT_BUILD" ]] || [[ ! -f "$PAPER_JAR" ]]; then
  echo "Updating Paper from build $CURRENT_BUILD to $LATEST_BUILD..."
  curl -L -o "$PAPER_JAR" \
    "https://api.papermc.io/v2/projects/paper/versions/$MC_VERSION/builds/$LATEST_BUILD/downloads/paper-$MC_VERSION-$LATEST_BUILD.jar"
  echo "$LATEST_BUILD" > "$PAPER_BUILD_FILE"
else
  echo "Paper is up to date (build $CURRENT_BUILD)."
fi

############################
# Helper: conditional plugin download using ETag
############################
download_if_changed() {
  local url="$1"
  local jar="$2"
  local meta_file="$3"
  local name="$4"

  echo "Checking $name..."

  # Use ETag if available to avoid redownloading unchanged jars
  local etag_old=""
  [[ -f "$meta_file" ]] && etag_old="$(cat "$meta_file" || echo "")"

  # HEAD request to see if changed
  local headers
  headers="$(curl -sI "$url")"
  local etag_new
  etag_new="$(printf '%s\n' "$headers" | awk 'tolower($1) ~ /^etag:/ {print $2}' | tr -d '\r"')"

  if [[ -n "$etag_new" && "$etag_new" == "$etag_old" && -f "$jar" ]]; then
    echo "$name is up to date (ETag match: $etag_new)."
    return 0
  fi

  echo "Downloading latest $name..."
  curl -L "$url" -o "$jar"

  if [[ -n "$etag_new" ]]; then
    echo "$etag_new" > "$meta_file"
  fi
}

############################
# Copy your plugin(s)
############################
echo "Copying your plugin(s)..."
cp build/libs/*.jar "$PLUGINS_DIR/"

############################
# Run server via flags.sh
############################
echo "Starting server with flags.sh..."

if [[ ! -x "$FLAGS_SCRIPT" ]]; then
  echo "run.sh not found or not executable at: $FLAGS_SCRIPT"
  echo "Create it and make it executable, e.g.:"
  echo "  #!/usr/bin/env bash"
  echo "  java -Xms2G -Xmx2G -jar paper.jar nogui"
  exit 1
fi

cd "$SERVER_DIR"
./run.sh
