#!/usr/bin/env bash
# Stops and removes the local shop containers and their network (images are kept).
set -uo pipefail

CLI="${CONTAINER_CLI:-podman}"

for name in gateway product-a product-b inventory-a inventory-b ui-a ui-b; do
  "$CLI" rm -f "$name" >/dev/null 2>&1 && echo "removed $name"
done
"$CLI" network rm shop-net >/dev/null 2>&1 && echo "removed network shop-net"
echo "done"
