import { useId, type ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError } from '../api/client';
import { getProductDetails } from '../api/products';
import type { ProductDetail as ProductDetailData } from '../api/types';
import { CopyButton } from '../components/CopyButton';
import { ErrorMessage } from '../components/ErrorMessage';
import { ProductGallery } from '../components/ProductGallery';
import { useAsync } from '../hooks/useAsync';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { formatPrice } from '../lib/format';
import { formatSavings, parseProductId, savings, stockStatus } from '../lib/product';

function ProductNotFound() {
  return (
    <header className="page-head">
      <h1>Product not found</h1>
      <p className="muted">This product doesn't exist. It may have been removed.</p>
      <p className="page-head-link">
        <Link to="/products">Back to products</Link>
      </p>
    </header>
  );
}

function DetailSkeleton() {
  return (
    <div className="detail" aria-busy="true">
      <p className="muted detail-loading" role="status">
        Loading product…
      </p>
      <div className="detail-top" aria-hidden="true">
        <div className="skeleton skeleton--picture" />
        <div className="detail-info">
          <div className="skeleton skeleton--line skeleton--short" />
          <div className="skeleton skeleton--title" />
          <div className="skeleton skeleton--line" />
          <div className="skeleton skeleton--price" />
          <div className="skeleton skeleton--line skeleton--short" />
        </div>
      </div>
    </div>
  );
}

function DetailSection({ title, children }: { title: string; children: ReactNode }) {
  const titleId = useId();
  return (
    <section className="detail-section" aria-labelledby={titleId}>
      <h2 id={titleId}>{title}</h2>
      {children}
    </section>
  );
}

function ProductView({ product }: { product: ProductDetailData }) {
  // The lists are required by the contract, but the API is editable: never crash on a missing one.
  const images = product.images ?? [];
  const highlights = (product.highlights ?? []).filter((line) => line.trim() !== '');
  const groups = (product.specifications ?? []).filter((group) => (group.items ?? []).length > 0);
  const saved = savings(product.price, product.mrp, product.discountPercent);
  const stock = stockStatus(product.stockQuantity);

  return (
    <div className="detail">
      <nav className="crumbs" aria-label="Breadcrumb">
        <ol>
          <li>
            <Link to="/products">Products</Link>
          </li>
          <li aria-current="page">{product.name}</li>
        </ol>
      </nav>

      <div className="detail-top">
        <ProductGallery images={images} name={product.name} />

        <div className="detail-info">
          {product.brand && <p className="detail-brand">{product.brand}</p>}
          <h1 className="detail-title">{product.name}</h1>
          <p className="detail-ref">
            {product.category && <span>{product.category}</span>}
            <span>
              <span className="muted">SKU </span>
              <CopyButton value={product.skuCode} what="SKU" className="mono">
                {product.skuCode}
              </CopyButton>
            </span>
          </p>

          <div className="price-block">
            <p className="price-now">
              <span className="sr-only">Price </span>
              {formatPrice(product.price)}
            </p>
            {saved && (
              <p className="price-was">
                <s className="mrp">
                  <span className="sr-only">MRP </span>
                  {formatPrice(product.mrp)}
                </s>
                <span className="discount">{formatSavings(saved, formatPrice)}</span>
              </p>
            )}
          </div>

          <p className={`stock-line stock-line--${stock.level}`}>
            <span className="stock-dot" aria-hidden="true" />
            {stock.text}
          </p>

          {(product.warranty || product.seller) && (
            <dl className="detail-facts">
              {product.warranty && (
                <div>
                  <dt>Warranty</dt>
                  <dd>{product.warranty}</dd>
                </div>
              )}
              {product.seller && (
                <div>
                  <dt>Seller</dt>
                  <dd>{product.seller}</dd>
                </div>
              )}
            </dl>
          )}

          {highlights.length > 0 && (
            <DetailSection title="Highlights">
              <ul className="highlights">
                {highlights.map((line, position) => (
                  <li key={`${position}-${line}`}>{line}</li>
                ))}
              </ul>
            </DetailSection>
          )}
        </div>
      </div>

      {product.description && product.description.trim() !== '' && (
        <DetailSection title="Description">
          {/* Plain text. CSS keeps the line breaks; it is never parsed as HTML. */}
          <p className="detail-description">{product.description}</p>
        </DetailSection>
      )}

      {groups.length > 0 && (
        <DetailSection title="Specifications">
          <div className="spec-groups">
            {groups.map((group, groupPosition) => (
              <table className="spec-table" key={`${groupPosition}-${group.group}`}>
                <caption>{group.group}</caption>
                <tbody>
                  {group.items.map((item, itemPosition) => (
                    <tr key={`${itemPosition}-${item.key}`}>
                      <th scope="row">{item.key}</th>
                      <td>{item.value}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ))}
          </div>
        </DetailSection>
      )}
    </div>
  );
}

function LoadedProduct({ id }: { id: number }) {
  const state = useAsync((signal) => getProductDetails(id, { signal }), [id]);
  const { data, error, loading, reload } = state;
  const missing = error instanceof ApiError && error.status === 404;

  useDocumentTitle(data ? data.name : missing ? 'Product not found' : 'Product');

  if (missing) return <ProductNotFound />;
  if (error) {
    return (
      <div className="detail">
        <ErrorMessage
          what="Could not load the product"
          error={error}
          hint="The product service may be down."
          onRetry={reload}
        />
        <p>
          <Link to="/products">Back to products</Link>
        </p>
      </div>
    );
  }
  if (data === undefined || loading) return <DetailSkeleton />;
  return <ProductView product={data} />;
}

function MissingProduct() {
  useDocumentTitle('Product not found');
  return <ProductNotFound />;
}

export function ProductDetail() {
  const { id } = useParams();
  const productId = parseProductId(id);
  if (productId === undefined) return <MissingProduct />;
  // Keyed, so moving from one product to another starts clean (gallery position, loading state).
  return <LoadedProduct key={productId} id={productId} />;
}
