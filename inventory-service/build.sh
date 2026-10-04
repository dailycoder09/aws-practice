#!/usr/bin/env bash
# Builds/runs inventory-service using JDK 17, without touching your default Java 8 setup.
# JAVA_HOME is only set for the mvn process this script launches.
#
# Usage:
#   ./build.sh          -> mvn clean verify (compiles, runs tests + JaCoCo gate)
#   ./build.sh package  -> mvn clean package -DskipTests (fast jar build, matches the Dockerfile)
#   ./build.sh run      -> mvn spring-boot:run (starts the app on :8082 against the local H2 file)
set -euo pipefail

JAVA17_HOME="/Library/Java/JavaVirtualMachines/openlogic-openjdk-17.jdk/Contents/Home"

if [ ! -x "$JAVA17_HOME/bin/java" ]; then
  echo "JDK 17 not found at $JAVA17_HOME"
  exit 1
fi

cd "$(dirname "$0")"

MODE="${1:-verify}"

echo "== inventory-service build =="
echo "   java: $("$JAVA17_HOME/bin/java" -version 2>&1 | head -1)"
echo "   mode: $MODE"
echo

case "$MODE" in
  verify)
    exec env JAVA_HOME="$JAVA17_HOME" mvn clean verify
    ;;
  package)
    exec env JAVA_HOME="$JAVA17_HOME" mvn clean package -DskipTests
    ;;
  run)
    exec env JAVA_HOME="$JAVA17_HOME" mvn spring-boot:run
    ;;
  *)
    echo "Unknown mode: $MODE (expected: verify | package | run)"
    exit 1
    ;;
esac
