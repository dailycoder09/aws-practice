import { getJson, withQuery, type RequestOptions } from './client';
import type { ServerInfo } from './types';

export type ServiceKey = 'products' | 'inventory';

export interface ServiceDefinition {
  key: ServiceKey;
  label: string;
  path: string;
}

export const SERVICES: readonly ServiceDefinition[] = [
  { key: 'products', label: 'Product service', path: '/api/products/server-info' },
  { key: 'inventory', label: 'Inventory service', path: '/api/inventory/server-info' },
];

export interface ServerInfoOptions extends RequestOptions {
  /** Appended as ?t=... so no cache in between can answer for the server. */
  cacheBuster?: string;
}

export function fetchServerInfo(service: ServiceDefinition, options: ServerInfoOptions = {}): Promise<ServerInfo> {
  const { cacheBuster, ...rest } = options;
  return getJson<ServerInfo>(withQuery(service.path, { t: cacheBuster }), rest);
}
