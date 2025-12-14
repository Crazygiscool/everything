#!/usr/bin/env bash

set -euo pipefail

# CONFIG
MC_VERSION="latest"   # <-- supports "latest" now
SERVER_DIR="run"
PLUGINS_DIR="$SERVER_DIR/plugins"
PAPER_JAR="$SERVER_DIR/paper.jar"
META_DIR="$SERVER_DIR/.meta"
PAPER_BUILD_FILE="$META_DIR/paper_build.txt"

FLAGS_SCRIPT="$SERVER_DIR/flags.sh"

mkdir -p "$PLUGINS_DIR" "$META_DIR"

###############################################
# Resolve "latest" Minecraft version
###############################################
if [[ "$MC_VERSION" == "latest" ]]; then
  echo "Resolving latest Minecraft version from Paper API..."
  MC_VERSION="$(curl -s https://api.papermc.io/v2/projects/paper \
    | grep -o '"versions":[^]]*' \
    | grep -o '"[0-9][^"]*"' \
    | tr -d '"' \
    | tail -1)"
  echo "Latest Minecraft version is: $MC_VERSION"
fi

###############################################
# Helper: get latest Paper build
###############################################
get_latest_paper_build() {
  curl -s "https://api.papermc.io/v2/projects/paper/versions/$MC_VERSION" \
    | grep -o '"builds":[^]]*' \
    | grep -o '[0-9]\+' \
    | tail -1
}

###############################################
# Paper: download only if newer
###############################################
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

###############################################
# Copy your plugin(s)
###############################################
echo "Copying your plugin(s)..."
cp build/libs/*.jar "$PLUGINS_DIR/"
