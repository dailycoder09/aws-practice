# Running the three apps behind one Application Load Balancer

Two VMs each run three containers: `product-service` (8081), `inventory-service` (8082) and `ui-app` (nginx, 8080 inside the container). One ALB sends each request to the right app by URL path.

## Why the old setup broke the home page

A target group (TG) must hold identical copies of one app. If the listener's default action forwards 50/50 to a product TG and an inventory TG, half of all requests to `/` reach inventory-service, which has no `/` route and answers 404. Balancing across the two VMs happens *inside* each TG, not between TGs.

## Target layout

One listener on port 80. Three target groups, each with **both VMs** registered. Stickiness off, so every refresh can reach a different VM.

| Priority | Condition | Forward to | Target port | Health check path |
|---|---|---|---|---|
| 10 | Path is `/api/products*` | `product-tg` | 8081 | `/actuator/health` |
| 20 | Path is `/api/inventory*` | `inventory-tg` | 8082 | `/actuator/health` |
| default | everything else | `ui-tg` | 8080 | `/healthz` |

- Use plain **Forward to one target group** actions. Do not use weighted forwarding.
- Health check success code `200`. Spring's health endpoint returns 200 only when the app is up.
- `/api/products/server-info` and `/api/inventory/server-info` sit under those prefixes, so they need no extra rules.

Optional: send API docs to the product service with a rule `/api-docs*` or `/swagger-ui*` -> `product-tg`. Both services expose `/swagger-ui.html` and `/actuator/*`, so without a rule those paths land on the UI target group. Reach each service's Swagger directly at `VM-IP:8081` or `VM-IP:8082` instead.

## Security groups

| Group | Inbound |
|---|---|
| ALB SG | 80 from where users are (for practice, `0.0.0.0/0`) |
| Instance SG | 8080, 8081 and 8082 from the **ALB SG** only; 22 from your own IP |

## Containers on each VM

`deploy/aws/user-data.sh` does it all at first boot: it installs Docker, clones the repo, builds the three images and starts the containers. Paste it into **Advanced details > User data** when launching each instance, and follow progress with `sudo tail -f /var/log/user-data.log`.

The containers run with `--network host`, so each app listens on its own port on the instance (UI 8080, product 8081, inventory 8082) and the target groups point straight at those ports. Use at least a 2 GB instance (t3.small); the script adds swap so the Maven build doesn't run out of memory.

The repo must contain the code you want to deploy: commit and push to GitHub before launching an instance, because it clones `main`.

### Service-to-service URLs

With `--network host`, `localhost` on the instance is shared by all three containers, so product-service reaches inventory-service at `http://localhost:8082` and the other way round. That is the default in the code, so there is nothing to configure. Each VM talks to the services on its own VM.

If you run the containers with `-p` mappings instead, `localhost` would mean the container itself and the calls would fail. Then set `INVENTORY_SERVICE_URL` and `PRODUCT_SERVICE_URL` to the ALB DNS name (both calls already live under `/api/products` and `/api/inventory`, so the ALB routes them) or to the VM's private IP.

## Seeing which server answered

Open the ALB DNS name. The dashboard shows, for each service, the hostname, IPs and EC2 instance ID that answered. Press Refresh a few times and the name changes as the ALB alternates between VMs. "Send 20 requests" tallies the answers and should show roughly a 50/50 split.

The EC2 details (instance ID, zone, type) come from the instance metadata service. With `--network host`, as in `user-data.sh`, they appear with the default settings. If you run the containers with `-p` mappings instead, the instance's metadata hop limit must be 2:

```bash
aws ec2 modify-instance-metadata-options --instance-id <i-...> \
  --http-put-response-hop-limit 2 --http-tokens required
```

Without it the EC2 section reads "Not on EC2" even though it is. The dashboard still works.

## What to expect with two VMs and H2

Each VM's containers use their own H2 database file, so the two VMs do **not** share data. The seed data is identical, but anything changed afterwards (for example "Add stock" in the UI) exists only on the VM that handled that request. Refreshing can then show different quantities depending on which VM answers. That is a normal consequence of file databases behind a load balancer. A shared database (RDS) is the fix if you want one consistent state later.

## Quick fix without the React app

If you need the home page working before the UI is deployed: delete the 50/50 default action, set the default to forward to `product-tg`, and add one rule `/api/inventory*` -> `inventory-tg`. Product-service's old server-rendered pages have been removed, so `/` on the product service will answer 404 until the UI target group is in place.

## Checks after the change

| Check | Expected |
|---|---|
| `http://<ALB>/` | The React app loads |
| `http://<ALB>/products` (hard refresh) | The app loads, not a 404 |
| `http://<ALB>/api/products?size=1` | JSON from product-service |
| `http://<ALB>/api/inventory?size=1` | JSON from inventory-service |
| Dashboard Refresh, several times | Hostname alternates between the two VMs |
| Stop one VM's `product-service` container | `product-tg` marks it unhealthy; the UI keeps working through the other VM |
