#!/usr/bin/env bash
# Script de inicio rápido del Sistema POS La Canasta
set -e
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$DIR"

if [ -z "$JAVA_HOME" ] && [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
    export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
fi

echo "Iniciando Sistema POS La Canasta..."
./mvnw javafx:run
