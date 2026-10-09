# ui-app

A React front end for the two Spring Boot services in this repo, product-service (port 8081) and inventory-service (port 8082).

It is also a practice tool for AWS load balancing. The dashboard shows which server answered each request and how traffic is split across the servers behind an Application Load Balancer (ALB).

The browser calls relative URLs (`/api/products/...` and `/api/inventory/...`) on the same origin it was loaded from. There is no CORS setup and no base URL to configure.

Stack: Vite, React 18, TypeScript (strict), react-router-dom 6, plain `fetch`. Tests use Vitest and Testing Library.

## Pages

- **Dashboard** (`/`): one panel per service with its status, the server nameplate (hostname), IPv4 addresses, EC2 instance and zone, request path, uptime and heap. Press Refresh to ask again. When the load balancer sends the request to the other VM, the hostname changes and a hint says so. Below the panels, the traffic distribution widget sends N requests (1 to 100, default 20, at most 5 in flight) to each service and shows how many each server answered. A totals strip shows product, category and inventory record counts.
- **Products** (`/products`): a grid of product cards with picture, brand, name, price, and, when the product has a discount, the struck-through MRP and "N% off". Search (debounced), category filter and pagination. The state lives in the URL as `?q=&category=&page=`, so refresh and deep links work. `page` is one-based, matching the "Page X of Y" label. Each page makes one batch call to the inventory service for stock badges. If that call fails, the list still shows. The whole card is a link to the product page (the name is the real link, so it works from the keyboard).
- **Product page** (`/products/:id`): a gallery (large picture, thumbnails, Left and Right arrow keys), brand, name, SKU (click to copy), price with the MRP and what you save, a stock line (`In stock (42)`, `Only 7 left`, `Out of stock`, `Stock unavailable`), warranty and seller, highlights, description and specifications grouped in tables. It reads `GET /api/products/{id}/details`. An id that is not a positive whole number, or one the service answers 404 for, shows "This product doesn't exist" with a link back to the list.
- **Inventory** (`/inventory`): paged stock table. Each row has an amount field with Add and Remove buttons that call `PATCH /api/inventory/sku/{sku}/adjust?delta=n`. The row updates in place, and a rejected change (for example, going below zero) shows the reason on that row.
- **SKU panel**: opens from the Inventory table. It shows the product, its stock record and its last five stock changes, plus an "Open product page" link when the product exists. The three sections load separately, so one service being down never blanks the panel.

### Product images

The image URLs in product-service are dummy placeholders (`https://placehold.co/...`). Replace them with real ones through `PUT /api/products/{id}/images`. The UI only renders `http` and `https` URLs, and shows a fallback tile with the product's initials when an image is missing or can't load (for example when the browser is offline or blocks the image host). The fallback is drawn locally and makes no request.

## Run it locally

You need Node 22 and npm 10.

```sh
npm ci
npm run dev
```

`npm run dev` starts Vite on http://localhost:5173. It proxies `/api/products` to `http://localhost:8081` and `/api/inventory` to `http://localhost:8082`, so both services must be running locally. Without them the pages open, but show "unreachable" and error messages.

Locally there is one server per service, so the traffic distribution widget correctly shows 100 % on one host. It only splits when two VMs sit behind the ALB.

```sh
npm test         # run the tests once
npm run test:watch
npm run build    # type-check, then write the static site to dist/
npm run preview  # serve dist/ locally (no API proxy to the services)
```

## Docker

```sh
docker build -t ui-app .
docker run -p 8080:8080 ui-app
```

The image builds the app with Node, then serves `dist/` from `nginxinc/nginx-unprivileged` (non-root, port 8080). `/healthz` returns `ok` for health checks. Unknown paths fall back to `index.html`, so deep links such as `/products` work.

### Open it through the ALB, not the container alone

nginx here serves static files only. It has no `/api` proxy. If you open http://localhost:8080 directly, the pages load, but every API call returns the app's own `index.html` and the UI reports "The server answered with something other than JSON".

Open the app through the ALB, which routes the API paths to the services, or use the Vite dev server (`npm run dev`), which proxies them.

## ALB listener rules

All three targets sit behind one listener, so the browser sees a single origin.

| Priority | Condition (path) | Forward to | Port |
| --- | --- | --- | --- |
| 10 | `/api/products*` | product-service target group | 8081 |
| 20 | `/api/inventory*` | inventory-service target group | 8082 |
| default | everything else | UI target group (this container) | 8080 |

UI target group health check: `/healthz`.

For the traffic split to show up, register both VMs in each service's target group. The two `server-info` endpoints (`/api/products/server-info` and `/api/inventory/server-info`) report which instance answered.
