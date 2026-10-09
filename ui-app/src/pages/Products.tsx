import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { fetchStockBatch } from '../api/inventory';
import { fetchCategories, fetchProducts } from '../api/products';
import { ErrorMessage } from '../components/ErrorMessage';
import { Pager } from '../components/Pager';
import { ProductPicture } from '../components/ProductPicture';
import { StockBadge } from '../components/StockBadge';
import { useAsync } from '../hooks/useAsync';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { formatNumber, formatPrice } from '../lib/format';
import { formatDiscount } from '../lib/product';
import { stockLevel } from '../lib/stock';

const PAGE_SIZE = 20;
const SEARCH_DEBOUNCE_MS = 300;

interface Changes {
  q?: string;
  category?: string;
  /** One-based, as shown in the URL and the pager. */
  page?: number;
}

export function Products() {
  useDocumentTitle('Products');
  const [params, setParams] = useSearchParams();
  const q = params.get('q') ?? '';
  const category = params.get('category') ?? '';
  const page = Math.max(1, Number.parseInt(params.get('page') ?? '1', 10) || 1);
  const filtered = q !== '' || category !== '';

  const [text, setText] = useState(q);
  const debounce = useRef<number | undefined>(undefined);

  // Everything the page shows lives in the URL, so refresh and deep links work.
  function update(changes: Changes, replace: boolean) {
    setParams(
      (previous) => {
        const next = new URLSearchParams(previous);
        for (const key of ['q', 'category'] as const) {
          if (!(key in changes)) continue;
          const value = changes[key];
          if (value) next.set(key, value);
          else next.delete(key);
        }
        if (changes.page && changes.page > 1) next.set('page', String(changes.page));
        else next.delete('page');
        return next;
      },
      { replace },
    );
  }

  function onSearchChange(value: string) {
    setText(value);
    window.clearTimeout(debounce.current);
    // Keep the text as typed (a trailing space is part of "foo bar"); only blank means no search.
    debounce.current = window.setTimeout(
      () => update({ q: value.trim() === '' ? '' : value }, true),
      SEARCH_DEBOUNCE_MS,
    );
  }

  // Back and forward change the URL under us: follow it.
  useEffect(() => {
    window.clearTimeout(debounce.current);
    setText(q);
  }, [q]);
  useEffect(() => () => window.clearTimeout(debounce.current), []);

  function clearFilters() {
    window.clearTimeout(debounce.current);
    setText('');
    update({ q: '', category: '' }, false);
  }

  const categories = useAsync((signal) => fetchCategories({ signal }), []);
  const products = useAsync(
    (signal) => fetchProducts({ q, category, page: page - 1, size: PAGE_SIZE }, { signal }),
    [q, category, page],
  );

  // One stock call per page of results. A failure here never hides the list.
  const pageData = products.data;
  const skus = useMemo(() => (pageData?.content ?? []).map((product) => product.skuCode), [pageData]);
  const stock = useAsync(async (signal) => ({ skus, map: await fetchStockBatch(skus, { signal }) }), [skus]);
  const stockFailed = Boolean(stock.error);
  const stockFresh = stock.data?.skus === skus;

  const rows = pageData?.content ?? [];
  const categoryOptions = categories.data ?? [];
  const options = category && !categoryOptions.includes(category) ? [category, ...categoryOptions] : categoryOptions;

  let summary = '';
  if (pageData && pageData.numberOfElements > 0) {
    const from = pageData.number * pageData.size + 1;
    const to = pageData.number * pageData.size + pageData.numberOfElements;
    summary = `Showing ${formatNumber(from)} to ${formatNumber(to)} of ${formatNumber(pageData.totalElements)} products`;
  }

  return (
    <>
      <header className="page-head">
        <h1>Products</h1>
        <p className="muted" aria-live="polite">
          {summary || (products.loading ? 'Loading products…' : ' ')}
        </p>
      </header>

      <form className="filters" role="search" onSubmit={(event) => event.preventDefault()}>
        <div className="filter">
          <label htmlFor="product-search">Search</label>
          <input
            id="product-search"
            className="field"
            type="search"
            placeholder="Name or description"
            value={text}
            onChange={(event) => onSearchChange(event.target.value)}
            autoComplete="off"
          />
        </div>
        <div className="filter">
          <label htmlFor="product-category">Category</label>
          <select
            id="product-category"
            className="field"
            value={category}
            onChange={(event) => update({ category: event.target.value }, false)}
          >
            <option value="">All categories</option>
            {options.map((name) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </select>
        </div>
        {filtered && (
          <button type="button" className="btn" onClick={clearFilters}>
            Clear filters
          </button>
        )}
      </form>

      {stockFailed && (
        <p className="notice" role="status">
          Stock levels are unavailable right now. The inventory service may be down.
        </p>
      )}

      {products.error && (
        <ErrorMessage
          what="Could not load products"
          error={products.error}
          hint="Check that the product service is running, then try again."
          onRetry={products.reload}
        />
      )}

      {!products.error && pageData && rows.length === 0 && !products.loading && (
        <div className="empty">
          {pageData.totalElements > 0 ? (
            <>
              <p>This page is past the end of the results.</p>
              <button type="button" className="btn" onClick={() => update({}, false)}>
                Go to the first page
              </button>
            </>
          ) : filtered ? (
            <>
              <p>No products match. Clear the search or pick another category.</p>
              <button type="button" className="btn" onClick={clearFilters}>
                Clear search and category
              </button>
            </>
          ) : (
            <p>The catalog is empty. Add products through the product service API.</p>
          )}
        </div>
      )}

      {!products.error && rows.length > 0 && (
        <>
          <ul className="product-grid" aria-busy={products.loading} data-loading={products.loading}>
            {rows.map((product) => {
              const quantity = stockFailed || !stockFresh ? undefined : stock.data?.map[product.skuCode];
              const discount = formatDiscount(product.discountPercent);
              return (
                <li key={product.id} className="product-card">
                  {/* The name beside it already says what this is, so the picture is decorative. */}
                  <ProductPicture url={product.imageUrl} alt="" name={product.name} />
                  <div className="product-card-body">
                    {product.brand && <p className="product-brand">{product.brand}</p>}
                    <h2 className="product-name">
                      {/* The link's ::after covers the whole card, so the card is one click target. */}
                      <Link className="product-link" to={`/products/${product.id}`}>
                        {product.name}
                      </Link>
                    </h2>
                    <p className="product-price">
                      <span className="price">
                        <span className="sr-only">Price </span>
                        {formatPrice(product.price)}
                      </span>
                      {discount && (
                        <>
                          {typeof product.mrp === 'number' && (
                            <s className="mrp">
                              <span className="sr-only">MRP </span>
                              {formatPrice(product.mrp)}
                            </s>
                          )}
                          <span className="discount">{discount}</span>
                        </>
                      )}
                    </p>
                    <p className="product-stock">
                      <span className="muted">Stock</span>
                      <StockBadge
                        level={stockLevel(quantity)}
                        quantity={quantity}
                        pending={!stockFailed && !stockFresh}
                      />
                    </p>
                  </div>
                </li>
              );
            })}
          </ul>
          <p className="legend">Stock: green above 20, amber at 20 or fewer, red at 0, grey when unknown.</p>
        </>
      )}

      {pageData && pageData.totalPages > 0 && !products.error && (
        <Pager
          page={pageData.number}
          totalPages={pageData.totalPages}
          first={pageData.first}
          last={pageData.last}
          disabled={products.loading}
          onPrevious={() => update({ page: page - 1 }, false)}
          onNext={() => update({ page: page + 1 }, false)}
        />
      )}
    </>
  );
}
