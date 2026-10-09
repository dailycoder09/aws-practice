import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter } from 'react-router-dom';
import type { InventoryItem, Page, Product, ProductDetail, ServerInfo } from '../api/types';

export function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

export function problem(status: number, title: string, detail?: string): Response {
  return new Response(JSON.stringify({ title, status, detail }), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  });
}

type Handler = (url: URL, init?: RequestInit) => unknown;

/**
 * Replaces fetch with a router. Keys look like "GET /api/products". A handler
 * may return a Response, or any value to be sent as 200 JSON. Unmatched
 * requests answer 404 so a forgotten route shows up as an error, not a hang.
 */
export function mockFetch(routes: Record<string, Handler>) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const raw = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url;
    const url = new URL(raw, 'http://localhost');
    const method = (init?.method ?? 'GET').toUpperCase();
    const handler = routes[`${method} ${url.pathname}`];
    if (!handler) return problem(404, 'Not Found', `No mock for ${method} ${url.pathname}`);
    const result = await handler(url, init);
    return result instanceof Response ? result : json(result);
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

export function requestedUrls(fetchMock: ReturnType<typeof mockFetch>): string[] {
  return fetchMock.mock.calls.map(([input]) => String(input));
}

export function renderAt(ui: ReactElement, path = '/') {
  return render(
    <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      {ui}
    </MemoryRouter>,
  );
}

export function pageOf<T>(
  content: T[],
  { number = 0, size = 20, totalElements = content.length }: { number?: number; size?: number; totalElements?: number } = {},
): Page<T> {
  const totalPages = Math.ceil(totalElements / size);
  return {
    content,
    totalElements,
    totalPages,
    number,
    size,
    first: number === 0,
    last: number >= totalPages - 1,
    numberOfElements: content.length,
  };
}

export function makeProduct(id: number, name: string, skuCode: string, overrides: Partial<Product> = {}): Product {
  return {
    id,
    name,
    category: 'Electronics',
    price: 19.99,
    skuCode,
    createdAt: '2026-10-01T09:00:00',
    updatedAt: '2026-10-01T09:00:00',
    ...overrides,
  };
}

/** A bare product detail: no images, highlights or specifications until a test adds them. */
export function makeDetail(overrides: Partial<ProductDetail> = {}): ProductDetail {
  return {
    id: 1,
    name: 'Alpha Phone',
    category: 'Electronics',
    price: 799.99,
    skuCode: 'ALPHA-1',
    stockQuantity: 42,
    images: [],
    highlights: [],
    specifications: [],
    createdAt: '2026-10-01T09:00:00',
    updatedAt: '2026-10-01T09:00:00',
    ...overrides,
  };
}

export function makeItem(
  id: number,
  skuCode: string,
  quantityOnHand: number,
  reorderThreshold: number,
): InventoryItem {
  return {
    id,
    skuCode,
    quantityOnHand,
    reorderThreshold,
    createdAt: '2026-10-01T09:00:00',
    updatedAt: '2026-10-01T09:00:00',
  };
}

export function makeServerInfo(
  hostname: string,
  { instanceId, zone, forwardedFor }: { instanceId?: string; zone?: string; forwardedFor?: string } = {},
): ServerInfo {
  return {
    application: { name: 'product-service', version: '1.2.3', profiles: ['dev'], port: 8081 },
    host: {
      hostname,
      addresses: [
        { interfaceName: 'eth0', address: '10.0.1.23' },
        { interfaceName: 'lo', address: '127.0.0.1' },
        { interfaceName: 'eth0', address: 'fe80::1' },
      ],
      container: false,
    },
    request: {
      hostHeader: 'shop.example.com',
      serverAddress: '10.0.1.23',
      serverPort: 8081,
      clientAddress: '10.0.0.5',
      forwardedFor,
    },
    runtime: {
      javaVersion: '17.0.14',
      javaVendor: 'OpenLogic',
      osName: 'Linux',
      osVersion: '6.1',
      osArch: 'amd64',
      cpus: 2,
      heapUsedMb: 210,
      heapMaxMb: 512,
      heapUsedPercent: 41,
      pid: 4121,
      startedAt: new Date(Date.now() - 12_000).toISOString(),
      startedAtEpochMs: Date.now() - 12_000,
      uptime: '00:00:12',
    },
    ...(instanceId || zone ? { cloud: { instanceId, availabilityZone: zone } } : {}),
  };
}
