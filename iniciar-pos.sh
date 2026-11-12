#!/usr/bin/env bash
# Script de inicio rápido del Sistema POS La Canasta
set -e
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$DIR"
echo "Iniciando Sistema POS La Canasta..."
./mvnw javafx:run
