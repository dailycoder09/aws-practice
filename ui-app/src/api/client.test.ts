import { ApiError, errorMessage, getJson, patchJson, withQuery } from './client';

function stubFetch(result: Response) {
  const fetchMock = vi.fn(async () => result);
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

function stubFetchFailure(reason: unknown) {
  const fetchMock = vi.fn(async () => {
    throw reason;
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

function jsonResponse(body: unknown, status = 200, type = 'application/json') {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': type } });
}

describe('getJson', () => {
  it('returns the parsed body and bypasses the HTTP cache', async () => {
    const fetchMock = stubFetch(jsonResponse({ hello: 'world' }));
    await expect(getJson('/api/thing')).resolves.toEqual({ hello: 'world' });
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/thing',
      expect.objectContaining({ method: 'GET', cache: 'no-store' }),
    );
  });

  it('turns a problem+json body into an ApiError with the detail', async () => {
    stubFetch(jsonResponse({ title: 'Bad Request', status: 400, detail: 'Stock cannot go below zero' }, 400, 'application/problem+json'));
    const failure = await getJson('/api/thing').catch((error: unknown) => error);
    expect(failure).toBeInstanceOf(ApiError);
    expect(failure).toMatchObject({ status: 400, title: 'Bad Request', detail: 'Stock cannot go below zero' });
    expect((failure as ApiError).message).toBe('Stock cannot go below zero');
  });

  it('uses the title alone when a problem body has no detail', async () => {
    stubFetch(jsonResponse({ title: 'Not Found', status: 404 }, 404, 'application/problem+json'));
    const failure = (await getJson('/api/thing').catch((error: unknown) => error)) as ApiError;
    expect(failure.status).toBe(404);
    expect(failure.detail).toBeUndefined();
    expect(errorMessage(failure)).toBe('Not Found');
  });

  it('falls back to "HTTP <status>" when the error body is not JSON', async () => {
    stubFetch(new Response('<html>Bad gateway</html>', { status: 502, headers: { 'Content-Type': 'text/html' } }));
    const failure = (await getJson('/api/thing').catch((error: unknown) => error)) as ApiError;
    expect(failure).toBeInstanceOf(ApiError);
    expect(failure.status).toBe(502);
    expect(failure.title).toBe('HTTP 502');
    expect(failure.detail).toBeUndefined();
  });

  it('says the server could not be reached when the network fails', async () => {
    stubFetchFailure(new TypeError('Failed to fetch'));
    const failure = (await getJson('/api/thing').catch((error: unknown) => error)) as ApiError;
    expect(failure).toBeInstanceOf(ApiError);
    expect(failure.status).toBe(0);
    expect(failure.title).toBe('Could not reach the server');
    expect(errorMessage(failure)).toBe('Could not reach the server');
  });

  it('lets an abort through untouched so callers can ignore it', async () => {
    const abort = new DOMException('Aborted', 'AbortError');
    stubFetchFailure(abort);
    await expect(getJson('/api/thing')).rejects.toBe(abort);
  });

  it('explains a successful response that is not JSON', async () => {
    stubFetch(new Response('<!doctype html><title>app</title>', { status: 200, headers: { 'Content-Type': 'text/html' } }));
    const failure = (await getJson('/api/thing').catch((error: unknown) => error)) as ApiError;
    expect(failure).toBeInstanceOf(ApiError);
    expect(failure.title).toBe('Unexpected response');
    expect(failure.detail).toMatch(/load balancer/);
  });
});

describe('patchJson', () => {
  it('sends a PATCH', async () => {
    const fetchMock = stubFetch(jsonResponse({ quantityOnHand: 8 }));
    await expect(patchJson('/api/inventory/sku/A/adjust?delta=3')).resolves.toEqual({ quantityOnHand: 8 });
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/inventory/sku/A/adjust?delta=3',
      expect.objectContaining({ method: 'PATCH', cache: 'no-store' }),
    );
  });
});

describe('withQuery', () => {
  it('skips empty values and encodes the rest', () => {
    expect(withQuery('/p', { a: 'x y', b: '', c: undefined, d: null, e: 0 })).toBe('/p?a=x+y&e=0');
  });

  it('returns the bare path when nothing is left', () => {
    expect(withQuery('/p', { a: '' })).toBe('/p');
  });
});
