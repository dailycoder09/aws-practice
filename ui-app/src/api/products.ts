import { getJson, withQuery, type RequestOptions } from './client';
import type { Page, Product, ProductDetail } from './types';

export interface ProductQuery {
  q?: string;
  category?: string;
  /** Zero-based, as the API expects. */
  page: number;
  size: number;
}

/** Lists products, or searches when a search term or category is given. */
export function fetchProducts(query: ProductQuery, options?: RequestOptions): Promise<Page<Product>> {
  const q = query.q?.trim();
  const category = query.category?.trim();
  if (q || category) {
    return getJson<Page<Product>>(
      withQuery('/api/products/search', {
        searchTerm: q,
        category,
        page: query.page,
        size: query.size,
      }),
      options,
    );
  }
  return getJson<Page<Product>>(
    withQuery('/api/products', { page: query.page, size: query.size, sortBy: 'id', sortDir: 'asc' }),
    options,
  );
}

export function fetchCategories(options?: RequestOptions): Promise<string[]> {
  return getJson<string[]>('/api/products/categories', options);
}

export function fetchProductBySku(sku: string, options?: RequestOptions): Promise<Product> {
  return getJson<Product>(`/api/products/sku/${encodeURIComponent(sku)}`, options);
}

/** One product with images, highlights, specifications and stock. A missing id answers 404. */
export function getProductDetails(id: number, options?: RequestOptions): Promise<ProductDetail> {
  return getJson<ProductDetail>(`/api/products/${encodeURIComponent(String(id))}/details`, options);
}

export async function fetchProductCount(options?: RequestOptions): Promise<number> {
  const page = await getJson<Page<Product>>(withQuery('/api/products', { size: 1 }), options);
  return page.totalElements;
}
