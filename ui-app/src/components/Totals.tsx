import { fetchCategories, fetchProductCount } from '../api/products';
import { fetchInventoryCount } from '../api/inventory';
import { useAsync } from '../hooks/useAsync';
import { formatNumber } from '../lib/format';

function Total({ label, value, failed, loading }: { label: string; value?: number; failed: boolean; loading: boolean }) {
  const text = failed ? '—' : value === undefined ? (loading ? '…' : '—') : formatNumber(value);
  return (
    <div>
      <dt>{label}</dt>
      <dd className="mono" title={failed ? 'Could not load this number' : undefined}>
        {text}
        {failed && <span className="sr-only"> (unavailable)</span>}
      </dd>
    </div>
  );
}

/** Three numbers, each loaded on its own so one failure never hides the others. */
export function Totals() {
  const products = useAsync((signal) => fetchProductCount({ signal }), []);
  const categories = useAsync(async (signal) => (await fetchCategories({ signal })).length, []);
  const inventory = useAsync((signal) => fetchInventoryCount({ signal }), []);

  return (
    <dl className="totals" aria-label="Totals">
      <Total label="Products" value={products.data} failed={Boolean(products.error)} loading={products.loading} />
      <Total
        label="Categories"
        value={categories.data}
        failed={Boolean(categories.error)}
        loading={categories.loading}
      />
      <Total
        label="Inventory records"
        value={inventory.data}
        failed={Boolean(inventory.error)}
        loading={inventory.loading}
      />
    </dl>
  );
}
