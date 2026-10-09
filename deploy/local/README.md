# Run the whole shop locally in containers (Podman or Docker)

One command builds the four images and starts a local copy of the AWS layout:

```
            http://localhost:8000
                    |
              [ gateway ]            nginx applying the same path rules as the ALB
        /api/products*  /api/inventory*  everything else
              |               |                |
     product-a/b      inventory-a/b        ui-a/b        a and b = two simulated VMs
     (hostname vm-a / vm-b)
```

Each simulated VM runs `product-service`, `inventory-service` and `ui-app` with its own H2 database file, exactly like your two EC2 instances. The services call each other through the gateway, the way they would through the ALB DNS name.

## Before the first run (Podman)

Four JVMs need memory. The default Podman machine (2 GB) is too small:

```bash
podman machine init --cpus 4 --memory 6144     # first time only
podman machine start
```

If a machine already exists, resize it while stopped:

```bash
podman machine stop
podman machine set --cpus 4 --memory 6144
podman machine start
```

Check it works with `podman --version` and `podman info`.

## Run

```bash
deploy/local/up.sh        # build images, start everything, wait until the apps are ready
deploy/local/down.sh      # stop and remove the containers and the network
```

Then open **http://localhost:8000**. Use `GATEWAY_PORT=9000 deploy/local/up.sh` if 8000 is taken. To use Docker instead: `CONTAINER_CLI=docker deploy/local/up.sh` (written for it, but only Podman is the intended runtime here).

The first build downloads Maven and npm dependencies and takes a few minutes. Later builds reuse the cache.

## What to try

| Try | Expected |
|---|---|
| Dashboard, press Refresh several times | The server name alternates between `vm-a` and `vm-b` |
| "Send 20 requests" | Roughly a 10 / 10 split per service |
| `podman stop product-b`, then refresh | Requests keep working through `vm-a`; the gateway skips the stopped VM |
| `podman start product-b` then `podman restart gateway` | Traffic returns to both VMs |
| Add stock on the Inventory page, then refresh a few times | The quantity can differ between refreshes: each VM has its own H2 file |
| `podman logs -f product-a` | The service log of one VM |

## How it differs from the real ALB

- nginx retries the other backend when one refuses a connection, so a stopped VM is usually invisible to the user. The ALB marks a target unhealthy after its health checks fail (and returns errors in the meantime).
- nginx looks up the backend names once at start. That's why `up.sh` starts the gateway last and why it needs a restart after you recreate containers.
- The ALB uses health check paths (`/actuator/health`, `/healthz`); here a refused connection stands in for them.
- Hostnames are set to `vm-a` / `vm-b` for readability. On EC2 you'll see the real host name and, when the metadata hop limit is 2, the instance ID.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `cannot reach its engine` | `podman machine start` |
| Containers killed or restarting, "Killed" in `podman logs` | Not enough memory: raise it with `podman machine set --memory 6144` |
| `port is already allocated` | Another process uses 8000: set `GATEWAY_PORT` |
| Build prompts to pick a registry | Shouldn't happen: the Dockerfiles use full `docker.io/...` image names |
| `host not found in upstream` in `podman logs gateway` | A backend container is missing: run `down.sh` then `up.sh` |
| A service did not become ready | `podman logs product-a` (or the one named in the message) |
