import { formatNumber } from './format';
import { stockLevel, type StockLevel } from './stock';

/**
 * The URL to put in an <img src>, or undefined. Image URLs come from an
 * editable API, so anything that is not an absolute http or https URL
 * (javascript:, data:, a relative path, junk) is ignored.
 */
export function safeImageUrl(value: string | null | undefined): string | undefined {
  if (typeof value !== 'string') return undefined;
  const trimmed = value.trim();
  if (trimmed === '') return undefined;
  try {
    const url = new URL(trimmed);
    return url.protocol === 'http:' || url.protocol === 'https:' ? url.href : undefined;
  } catch {
    return undefined;
  }
}

/** "Alpha Phone Pro" -> "AP". The first letter of up to two words; "?" when there is none. */
export function initials(name: string | null | undefined): string {
  const letters: string[] = [];
  for (const word of (name ?? '').split(/\s+/)) {
    const first = Array.from(word).find((character) => /[\p{L}\p{N}]/u.test(character));
    if (first) letters.push(first.toLocaleUpperCase('en-US'));
    if (letters.length === 2) break;
  }
  return letters.length > 0 ? letters.join('') : '?';
}

/** 20 -> "20% off". Nothing when the server sent no discount. */
export function formatDiscount(percent: number | null | undefined): string | undefined {
  if (percent === null || percent === undefined || !Number.isFinite(percent)) return undefined;
  const whole = Math.round(percent);
  return whole > 0 ? `${whole}% off` : undefined;
}

export interface Savings {
  /** In dollars, rounded to cents. */
  amount: number;
  /** Whole percent; 0 when it cannot be worked out. */
  percent: number;
}

/**
 * What a shopper saves against the MRP. Only when the MRP really is above the
 * price. The server's percentage wins when it sent one, otherwise it is derived.
 */
export function savings(
  price: number,
  mrp: number | null | undefined,
  discountPercent?: number | null,
): Savings | undefined {
  if (mrp === null || mrp === undefined || !Number.isFinite(mrp) || !Number.isFinite(price)) return undefined;
  if (mrp <= price) return undefined;
  const amount = Math.round((mrp - price) * 100) / 100;
  if (amount <= 0) return undefined;
  const fromServer = discountPercent !== null && discountPercent !== undefined && discountPercent > 0;
  const percent = fromServer ? Math.round(discountPercent) : Math.round(((mrp - price) / mrp) * 100);
  return { amount, percent };
}

/** "You save $200.00 (20% off)". The bracket is left out when there is no percentage to show. */
export function formatSavings(saved: Savings, formatAmount: (value: number) => string): string {
  const percent = formatDiscount(saved.percent);
  return `You save ${formatAmount(saved.amount)}${percent ? ` (${percent})` : ''}`;
}

export interface StockStatus {
  level: StockLevel;
  text: string;
}

/** The wording for the stock line on the detail page. Same levels as the badges. */
export function stockStatus(quantity: number | null | undefined): StockStatus {
  const level = stockLevel(quantity);
  switch (level) {
    case 'ok':
      return { level, text: `In stock (${formatNumber(quantity)})` };
    case 'low':
      return { level, text: `Only ${formatNumber(quantity)} left` };
    case 'out':
      return { level, text: 'Out of stock' };
    default:
      return { level, text: 'Stock unavailable' };
  }
}

/** A route param as a product id, or undefined for anything that is not a positive whole number. */
export function parseProductId(value: string | undefined): number | undefined {
  if (value === undefined || !/^[1-9]\d*$/.test(value)) return undefined;
  const id = Number(value);
  return Number.isSafeInteger(id) ? id : undefined;
}
