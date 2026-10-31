#!/usr/bin/env bash

set -euo pipefail

# CONFIG
MC_VERSION="latest"
SERVER_DIR="run"
PLUGINS_DIR="$SERVER_DIR/plugins"
PAPER_JAR="$SERVER_DIR/paper.jar"
META_DIR="$SERVER_DIR/.meta"
PAPER_URL_FILE="$META_DIR/paper_url.txt"
USER_AGENT="Everything-Plugin-Dev/1.0 (https://github.com/Crazygiscool/everything)"

mkdir -p "$PLUGINS_DIR" "$META_DIR"

echo "Building Everything..."
./gradlew build

echo "Resolving a recent stable Paper build..."
if [[ "$MC_VERSION" == "latest" ]]; then
    PROJECT_JSON=$(curl -fsSL -H "User-Agent: $USER_AGENT" \
        "https://fill.papermc.io/v3/projects/paper")
    VERSIONS=$(echo "$PROJECT_JSON" | \
        jq -r '.versions | to_entries[] | .value[]' | sort -V -r)
    if [[ -z "$VERSIONS" ]]; then
        echo "Could not resolve Paper versions."
        exit 1
    fi

    for VERSION in $VERSIONS; do
        BUILDS=$(curl -fsSL -H "User-Agent: $USER_AGENT" \
            "https://fill.papermc.io/v3/projects/paper/versions/$VERSION/builds")
        PAPER_URL=$(echo "$BUILDS" | \
            jq -r 'first(.[] | select(.channel == "STABLE") | .downloads["server:default"].url) // "null"')
        if [[ "$PAPER_URL" != "null" ]]; then
            MC_VERSION="$VERSION"
            echo "Selected Paper version: $MC_VERSION"
            break
        fi
    done
else
    BUILDS=$(curl -fsSL -H "User-Agent: $USER_AGENT" \
        "https://fill.papermc.io/v3/projects/paper/versions/$MC_VERSION/builds")
    PAPER_URL=$(echo "$BUILDS" | \
        jq -r 'first(.[] | select(.channel == "STABLE") | .downloads["server:default"].url) // "null"')
fi

if [[ "$PAPER_URL" == "null" || -z "$PAPER_URL" ]]; then
    echo "Could not resolve a stable Paper build for $MC_VERSION."
    exit 1
fi

CURRENT_URL=""
if [[ -f "$PAPER_URL_FILE" ]]; then
    CURRENT_URL=$(cat "$PAPER_URL_FILE")
fi

if [[ "$CURRENT_URL" != "$PAPER_URL" || ! -f "$PAPER_JAR" ]]; then
    echo "Downloading Paper..."
    curl -fL -H "User-Agent: $USER_AGENT" -o "$PAPER_JAR" "$PAPER_URL"
    echo "$PAPER_URL" > "$PAPER_URL_FILE"
else
    echo "Paper is already on the selected stable build."
fi

echo "Preparing plugin directory (Everything only)..."
mkdir -p "$PLUGINS_DIR"
find "$PLUGINS_DIR" -maxdepth 1 -type f -name '*.jar' -delete

SHADED_JARS=(build/libs/everything-*-b*.jar)
if [[ ! -f "${SHADED_JARS[0]}" ]]; then
    echo "No shaded plugin jar found in build/libs."
    exit 1
fi
LATEST_PLUGIN=$(ls -t build/libs/everything-*-b*.jar | head -n 1)
cp "$LATEST_PLUGIN" "$PLUGINS_DIR/"
echo "Deployed $(basename "$LATEST_PLUGIN") to $PLUGINS_DIR/"
