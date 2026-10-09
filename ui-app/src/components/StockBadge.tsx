import { STOCK_LEVEL_LABEL, type StockLevel } from '../lib/stock';
import { formatNumber } from '../lib/format';

interface Props {
  level: StockLevel;
  quantity?: number | null;
  /** Show a quiet placeholder while the number is still on its way. */
  pending?: boolean;
}

export function StockBadge({ level, quantity, pending = false }: Props) {
  if (pending) {
    return (
      <span className="stock stock--unknown">
        …<span className="sr-only"> loading stock</span>
      </span>
    );
  }
  const known = level !== 'unknown' && quantity !== null && quantity !== undefined;
  return (
    <span className={`stock stock--${level}`}>
      {known ? formatNumber(quantity) : '—'}
      <span className="sr-only"> ({STOCK_LEVEL_LABEL[level]})</span>
    </span>
  );
}
