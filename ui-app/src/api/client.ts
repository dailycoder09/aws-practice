// A small fetch wrapper. Every request bypasses the HTTP cache so the load
// balancer really picks a server each time. Failures become ApiError.

export class ApiError extends Error {
  readonly status: number;
  readonly title: string;
  readonly detail?: string;

  constructor(status: number, title: string, detail?: string) {
    super(detail ?? title);
    this.name = 'ApiError';
    this.status = status;
    this.title = title;
    this.detail = detail;
  }
}

export interface RequestOptions {
  signal?: AbortSignal;
}

export const NETWORK_ERROR_TITLE = 'Could not reach the server';

export function isAbortError(error: unknown): boolean {
  return typeof error === 'object' && error !== null && (error as { name?: unknown }).name === 'AbortError';
}

/** The most specific message we have for showing to a person. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.detail ?? error.title;
  if (error instanceof Error) return error.message;
  return 'Something went wrong';
}

export type QueryValue = string | number | boolean | undefined | null;

/** Appends a query string, skipping empty values. */
export function withQuery(path: string, params: Record<string, QueryValue>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === '') continue;
    search.set(key, String(value));
  }
  const query = search.toString();
  return query ? `${path}?${query}` : path;
}

async function parseError(response: Response): Promise<ApiError> {
  const fallbackTitle = `HTTP ${response.status}`;
  let body: unknown;
  try {
    body = JSON.parse(await response.text());
  } catch {
    return new ApiError(response.status, fallbackTitle);
  }
  if (body && typeof body === 'object') {
    const { title, detail } = body as { title?: unknown; detail?: unknown };
    return new ApiError(
      response.status,
      typeof title === 'string' && title ? title : fallbackTitle,
      typeof detail === 'string' && detail ? detail : undefined,
    );
  }
  return new ApiError(response.status, fallbackTitle);
}

async function request<T>(method: 'GET' | 'PATCH', path: string, options: RequestOptions): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, {
      method,
      cache: 'no-store',
      headers: { Accept: 'application/json, application/problem+json' },
      signal: options.signal,
    });
  } catch (error) {
    if (isAbortError(error)) throw error;
    throw new ApiError(0, NETWORK_ERROR_TITLE);
  }

  if (!response.ok) throw await parseError(response);

  try {
    return (await response.json()) as T;
  } catch {
    // Typically index.html served for an /api path nobody is routing.
    throw new ApiError(
      response.status,
      'Unexpected response',
      'The server answered with something other than JSON. Check that the load balancer routes /api/ paths to the services.',
    );
  }
}

export function getJson<T>(path: string, options: RequestOptions = {}): Promise<T> {
  return request<T>('GET', path, options);
}

export function patchJson<T>(path: string, options: RequestOptions = {}): Promise<T> {
  return request<T>('PATCH', path, options);
}
