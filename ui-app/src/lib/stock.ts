export type StockLevel = 'ok' | 'low' | 'out' | 'unknown';

/** Catalog rows have no per-item threshold, so they share this one. */
export const DEFAULT_LOW_STOCK_THRESHOLD = 20;

/**
 * out at 0 (or below), low at or under the threshold, ok above it,
 * unknown when we have no number.
 */
export function stockLevel(
  quantity: number | null | undefined,
  threshold: number = DEFAULT_LOW_STOCK_THRESHOLD,
): StockLevel {
  if (quantity === null || quantity === undefined || Number.isNaN(quantity)) return 'unknown';
  if (quantity <= 0) return 'out';
  if (quantity <= threshold) return 'low';
  return 'ok';
}

/** The inventory page judges each item against its own reorder threshold. */
export function inventoryLevel(item: { quantityOnHand: number; reorderThreshold: number }): StockLevel {
  return stockLevel(item.quantityOnHand, item.reorderThreshold);
}

export const STOCK_LEVEL_LABEL: Record<StockLevel, string> = {
  ok: 'in stock',
  low: 'low stock',
  out: 'out of stock',
  unknown: 'stock unknown',
};
