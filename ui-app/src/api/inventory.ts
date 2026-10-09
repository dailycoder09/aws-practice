import { getJson, patchJson, withQuery, type RequestOptions } from './client';
import type { AuditEntry, InventoryItem, Page, StockMap } from './types';

export function fetchInventory(
  page: number,
  size: number,
  options?: RequestOptions,
): Promise<Page<InventoryItem>> {
  return getJson<Page<InventoryItem>>(withQuery('/api/inventory', { page, size }), options);
}

/** One call for many SKUs. Unknown SKUs are simply missing from the result. */
export async function fetchStockBatch(skuCodes: string[], options?: RequestOptions): Promise<StockMap> {
  if (skuCodes.length === 0) return {};
  const list = skuCodes.map(encodeURIComponent).join(',');
  return getJson<StockMap>(`/api/inventory/batch?skuCodes=${list}`, options);
}

export function fetchInventoryBySku(sku: string, options?: RequestOptions): Promise<InventoryItem> {
  return getJson<InventoryItem>(`/api/inventory/sku/${encodeURIComponent(sku)}`, options);
}

export function fetchAudit(sku: string, size = 5, options?: RequestOptions): Promise<Page<AuditEntry>> {
  return getJson<Page<AuditEntry>>(
    withQuery(`/api/inventory/sku/${encodeURIComponent(sku)}/audit`, { page: 0, size }),
    options,
  );
}

/**
 * Adds or removes stock. The delta is sent without a leading plus sign,
 * because "+" in a query string is decoded as a space.
 */
export function adjustStock(sku: string, delta: number, options?: RequestOptions): Promise<InventoryItem> {
  return patchJson<InventoryItem>(
    `/api/inventory/sku/${encodeURIComponent(sku)}/adjust?delta=${delta}`,
    options,
  );
}

export async function fetchInventoryCount(options?: RequestOptions): Promise<number> {
  const page = await getJson<Page<InventoryItem>>(withQuery('/api/inventory', { size: 1 }), options);
  return page.totalElements;
}
