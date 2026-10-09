import { useEffect, useId, useRef, type KeyboardEvent, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { Link } from 'react-router-dom';
import { ApiError } from '../api/client';
import { fetchAudit, fetchInventoryBySku } from '../api/inventory';
import { fetchProductBySku } from '../api/products';
import type { ChangeType } from '../api/types';
import { useAsync, type AsyncResult } from '../hooks/useAsync';
import { formatDateTime, formatNumber, formatPrice } from '../lib/format';
import { inventoryLevel } from '../lib/stock';
import { ErrorMessage } from './ErrorMessage';
import { StockBadge } from './StockBadge';

const FOCUSABLE = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

const CHANGE_LABEL: Record<ChangeType, string> = {
  CREATED: 'Created',
  ADJUSTED: 'Adjusted',
  UPDATED: 'Updated',
  DELETED: 'Deleted',
};

function isNotFound(error: Error | undefined): boolean {
  return error instanceof ApiError && error.status === 404;
}

interface SectionProps<T> {
  title: string;
  state: AsyncResult<T>;
  notFound: string;
  failed: string;
  hint: string;
  children: (data: T) => ReactNode;
}

/** One independently loading block. Whatever happens here never affects its siblings. */
function Section<T>({ title, state, notFound, failed, hint, children }: SectionProps<T>) {
  const titleId = useId();
  const { data, error, loading, reload } = state;
  let body: ReactNode;
  if (error) {
    body = isNotFound(error) ? (
      <p className="muted">{notFound}</p>
    ) : (
      <ErrorMessage what={failed} error={error} hint={hint} onRetry={reload} />
    );
  } else if (data === undefined || loading) {
    body = <p className="muted">Loading…</p>;
  } else {
    body = children(data);
  }
  return (
    <section className="sku-section" aria-labelledby={titleId}>
      <h3 id={titleId}>{title}</h3>
      {body}
    </section>
  );
}

function ProductSection({ sku }: { sku: string }) {
  const state = useAsync((signal) => fetchProductBySku(sku, { signal }), [sku]);
  return (
    <Section
      title="Product"
      state={state}
      notFound="This SKU is not in the product catalog."
      failed="Could not load the product"
      hint="The product service may be down."
    >
      {(product) => (
        <>
          <dl className="sku-facts">
            <div>
              <dt>Name</dt>
              <dd>{product.name}</dd>
            </div>
            <div>
              <dt>Category</dt>
              <dd>{product.category ?? '—'}</dd>
            </div>
            <div>
              <dt>Price</dt>
              <dd className="mono">{formatPrice(product.price)}</dd>
            </div>
            {product.description && (
              <div className="sku-description">
                <dt>Description</dt>
                <dd>{product.description}</dd>
              </div>
            )}
          </dl>
          <p className="sku-open">
            <Link className="btn btn--small" to={`/products/${product.id}`}>
              Open product page
            </Link>
          </p>
        </>
      )}
    </Section>
  );
}

function StockSection({ sku }: { sku: string }) {
  const state = useAsync((signal) => fetchInventoryBySku(sku, { signal }), [sku]);
  return (
    <Section
      title="Stock"
      state={state}
      notFound="No stock record for this SKU."
      failed="Could not load stock"
      hint="The inventory service may be down."
    >
      {(item) => (
        <dl className="sku-facts">
          <div>
            <dt>On hand</dt>
            <dd>
              <StockBadge level={inventoryLevel(item)} quantity={item.quantityOnHand} />
            </dd>
          </div>
          <div>
            <dt>Reorder threshold</dt>
            <dd className="mono">{formatNumber(item.reorderThreshold)}</dd>
          </div>
        </dl>
      )}
    </Section>
  );
}

function AuditSection({ sku }: { sku: string }) {
  const state = useAsync((signal) => fetchAudit(sku, 5, { signal }), [sku]);
  return (
    <Section
      title="Recent stock changes"
      state={state}
      notFound="No stock changes recorded."
      failed="Could not load stock changes"
      hint="The inventory service may be down."
    >
      {(page) =>
        page.content.length === 0 ? (
          <p className="muted">No stock changes recorded.</p>
        ) : (
          <ol className="audit">
            {page.content.map((entry) => (
              <li key={entry.id}>
                <span className="audit-type">{CHANGE_LABEL[entry.changeType] ?? entry.changeType}</span>
                <span className="mono audit-change">
                  {entry.previousQuantity ?? '—'} <span aria-hidden="true">→</span>
                  <span className="sr-only"> to </span> {entry.newQuantity ?? '—'}
                </span>
                <time className="muted audit-time" dateTime={entry.changedAt}>
                  {formatDateTime(entry.changedAt)}
                </time>
              </li>
            ))}
          </ol>
        )
      }
    </Section>
  );
}

interface Props {
  sku: string;
  onClose: () => void;
}

/**
 * A side drawer that shows one SKU from both services. Product, stock and
 * history load separately, so one service being down leaves the rest usable.
 */
export function SkuPanel({ sku, onClose }: Props) {
  const titleId = useId();
  const dialog = useRef<HTMLDivElement>(null);
  const onCloseRef = useRef(onClose);
  onCloseRef.current = onClose;

  // Move focus in on open, give it back on close, and stop the page scrolling underneath.
  useEffect(() => {
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    dialog.current?.focus();
    const overflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = overflow;
      if (opener && opener.isConnected) opener.focus();
    };
  }, []);

  useEffect(() => {
    function onKeyDown(event: globalThis.KeyboardEvent) {
      if (event.key === 'Escape') {
        event.stopPropagation();
        onCloseRef.current();
      }
    }
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, []);

  // Keep Tab inside the drawer.
  function trapTab(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key !== 'Tab' || !dialog.current) return;
    const items = Array.from(dialog.current.querySelectorAll<HTMLElement>(FOCUSABLE));
    if (items.length === 0) {
      event.preventDefault();
      return;
    }
    const firstItem = items[0];
    const lastItem = items[items.length - 1];
    const active = document.activeElement;
    if (event.shiftKey && (active === firstItem || active === dialog.current)) {
      event.preventDefault();
      lastItem.focus();
    } else if (!event.shiftKey && active === lastItem) {
      event.preventDefault();
      firstItem.focus();
    }
  }

  return createPortal(
    <div className="drawer-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <div
        ref={dialog}
        className="drawer"
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        onKeyDown={trapTab}
      >
        <header className="drawer-head">
          <h2 id={titleId}>
            <span className="muted">SKU </span>
            <span className="mono">{sku}</span>
          </h2>
          <button type="button" className="btn btn--small" onClick={onClose}>
            Close
          </button>
        </header>
        <div className="drawer-body">
          <ProductSection sku={sku} />
          <StockSection sku={sku} />
          <AuditSection sku={sku} />
        </div>
      </div>
    </div>,
    document.body,
  );
}
