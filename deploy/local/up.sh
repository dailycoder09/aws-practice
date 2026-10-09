#!/usr/bin/env bash
# Runs the whole shop locally in containers, shaped like the AWS setup:
#   two "VMs" (a and b), each with product-service, inventory-service and ui-app,
#   behind a gateway container that applies the same path rules as the ALB.
#
# Usage:   deploy/local/up.sh            (uses podman)
#          CONTAINER_CLI=docker deploy/local/up.sh
#          GATEWAY_PORT=9000 deploy/local/up.sh     (default port is 8000)
#          deploy/local/up.sh --build-only          (build the four images, start nothing)
set -euo pipefail

CLI="${CONTAINER_CLI:-podman}"
PORT="${GATEWAY_PORT:-8000}"
NET="shop-net"
# Four JVMs share one container machine, so keep each one small.
JAVA_LOCAL="-Xms128m -Xmx320m -XX:+UseG1GC"

cd "$(dirname "$0")/../.."

if ! command -v "$CLI" >/dev/null 2>&1; then
  echo "$CLI was not found on your PATH."
  exit 1
fi

if ! "$CLI" info >/dev/null 2>&1; then
  echo "$CLI is installed but cannot reach its engine."
  if [ "$CLI" = "podman" ]; then
    echo "Start the Podman machine first:  podman machine start"
    echo "(first time:  podman machine init --cpus 4 --memory 6144 && podman machine start)"
  fi
  exit 1
fi

# A leftover Docker config (~/.docker/config.json) can name credential helpers (for example gcloud)
# that fail without a terminal and stop every image pull. All images here are public, so podman
# gets an empty credentials file instead. Set REGISTRY_AUTH_FILE yourself to use real credentials.
if [ "$CLI" = "podman" ] && [ -z "${REGISTRY_AUTH_FILE:-}" ]; then
  AUTH_FILE="$(mktemp)"
  echo '{}' > "$AUTH_FILE"
  trap 'rm -f "$AUTH_FILE"' EXIT
  export REGISTRY_AUTH_FILE="$AUTH_FILE"
fi

# podman only keeps HEALTHCHECK instructions when the image is built in docker format.
BUILD_FORMAT=""
if [ "$CLI" = "podman" ]; then
  BUILD_FORMAT="--format docker"
fi

# Behind a company proxy (HTTPS_PROXY set), the build steps need it too. It is passed in explicitly;
# the Maven Dockerfiles turn it into a Maven proxy setting, npm and apk read it directly.
PROXY_URL="${HTTPS_PROXY:-${https_proxy:-${HTTP_PROXY:-${http_proxy:-}}}}"

build_image() {
  local tag="$1" context="$2"
  if [ -n "$PROXY_URL" ]; then
    "$CLI" build $BUILD_FORMAT \
      --build-arg "HTTP_PROXY=$PROXY_URL" --build-arg "HTTPS_PROXY=$PROXY_URL" \
      -t "$tag" "$context"
  else
    "$CLI" build $BUILD_FORMAT -t "$tag" "$context"
  fi
}

echo "==> Building images (the first build downloads Maven and npm dependencies and takes a few minutes)"
build_image product-service:local product-service
build_image inventory-service:local inventory-service
build_image ui-app:local ui-app
build_image shop-gateway:local deploy/local/gateway

if [ "${1:-}" = "--build-only" ]; then
  echo
  echo "Images built:"
  "$CLI" images --format 'table {{.Repository}}\t{{.Tag}}\t{{.Size}}' | grep -E "REPOSITORY|product-service|inventory-service|ui-app|shop-gateway"
  echo
  echo "Start them later with:  deploy/local/up.sh"
  exit 0
fi

# The running containers must not inherit the proxy: the health checks call localhost.
RUN_PROXY_FLAG=""
if [ "$CLI" = "podman" ]; then
  RUN_PROXY_FLAG="--http-proxy=false"
fi

echo "==> Preparing network and removing old containers"
"$CLI" network inspect "$NET" >/dev/null 2>&1 || "$CLI" network create "$NET" >/dev/null
for name in gateway product-a product-b inventory-a inventory-b ui-a ui-b; do
  "$CLI" rm -f "$name" >/dev/null 2>&1 || true
done

echo "==> Starting the two simulated VMs"
for side in a b; do
  # The services call each other through the gateway, like they would through the ALB DNS name.
  "$CLI" run -d $RUN_PROXY_FLAG --name "product-$side" --hostname "vm-$side" --network "$NET" \
    -e JAVA_OPTS="$JAVA_LOCAL" -e INVENTORY_SERVICE_URL=http://gateway:8080 \
    product-service:local >/dev/null
  "$CLI" run -d $RUN_PROXY_FLAG --name "inventory-$side" --hostname "vm-$side" --network "$NET" \
    -e JAVA_OPTS="$JAVA_LOCAL" -e PRODUCT_SERVICE_URL=http://gateway:8080 \
    inventory-service:local >/dev/null
  "$CLI" run -d $RUN_PROXY_FLAG --name "ui-$side" --hostname "vm-$side" --network "$NET" \
    ui-app:local >/dev/null
done

# The gateway resolves the backend names when it starts, so it goes up last.
echo "==> Starting the gateway on port $PORT"
"$CLI" run -d $RUN_PROXY_FLAG --name gateway --network "$NET" -p "$PORT:8080" shop-gateway:local >/dev/null

wait_for() {
  local name="$1" pattern="$2"
  for _ in $(seq 1 90); do
    if "$CLI" logs "$name" 2>&1 | grep -q "$pattern"; then
      return 0
    fi
    sleep 2
  done
  echo "$name did not become ready. Look at its log:  $CLI logs $name"
  return 1
}

echo "==> Waiting for the Spring Boot apps (about 20-40 seconds each)"
wait_for product-a "Started ProductServiceApplication"
wait_for product-b "Started ProductServiceApplication"
wait_for inventory-a "Started InventoryServiceApplication"
wait_for inventory-b "Started InventoryServiceApplication"

echo
echo "Ready:  http://localhost:$PORT"
echo
echo "Try it:"
echo "  - Open the dashboard and press Refresh: the server name alternates between vm-a and vm-b."
echo "  - 'Send 20 requests' shows the split between the two VMs."
echo "  - Simulate a VM failing:   $CLI stop product-b"
echo "    Bring it back:           $CLI start product-b   (then:  $CLI restart gateway)"
echo "  - Stock you change on one VM is not visible on the other: each has its own H2 file."
echo
echo "Stop everything:  deploy/local/down.sh"
