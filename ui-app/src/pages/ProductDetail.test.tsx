import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import type { ProductDetail as ProductDetailData } from '../api/types';
import { ProductDetail } from './ProductDetail';
import { makeDetail, mockFetch, problem, renderAt, requestedUrls } from '../test/helpers';

const images = [
  { url: 'https://placehold.co/800x800/png?text=One', alt: 'Front view' },
  { url: 'https://placehold.co/800x800/png?text=Two', alt: 'Side view' },
  { url: 'https://placehold.co/800x800/png?text=Three' }, // no alt: the product name stands in
];

function renderDetail(path: string) {
  return renderAt(
    <Routes>
      <Route path="/products/:id" element={<ProductDetail />} />
      <Route path="/products" element={<p>The product list</p>} />
    </Routes>,
    path,
  );
}

function serve(detail: ProductDetailData) {
  return mockFetch({ [`GET /api/products/${detail.id}/details`]: () => detail });
}

function gallery() {
  return screen.getByRole('group', { name: 'Product images' });
}

function mainImage() {
  // Thumbnails are decorative (empty alt), so the only named image is the main one.
  return within(gallery()).getByRole('img');
}

function thumb(position: number, of: number) {
  return within(gallery()).getByRole('button', { name: `Show image ${position} of ${of}` });
}

describe('Product detail page', () => {
  it('asks for the details of the product in the URL and titles the page with its name', async () => {
    const fetchMock = serve(makeDetail({ id: 7, name: 'Golf Putter' }));
    renderDetail('/products/7');

    expect(await screen.findByRole('heading', { level: 1, name: 'Golf Putter' })).toBeInTheDocument();
    expect(requestedUrls(fetchMock)).toEqual(['/api/products/7/details']);
    await waitFor(() => expect(document.title).toBe('Golf Putter · Microservices demo'));
  });

  it('shows a loading message first', async () => {
    serve(makeDetail());
    renderDetail('/products/1');

    expect(screen.getByRole('status')).toHaveTextContent('Loading product…');
    expect(await screen.findByRole('heading', { level: 1 })).toBeInTheDocument();
    expect(screen.queryByText('Loading product…')).not.toBeInTheDocument();
  });

  it('shows brand, name, category, a copyable SKU, warranty and seller', async () => {
    serve(
      makeDetail({
        name: 'Alpha Phone',
        brand: 'Alpha',
        category: 'Electronics',
        warranty: '2 years',
        seller: 'Alpha Retail Ltd',
      }),
    );
    renderDetail('/products/1');

    expect(await screen.findByRole('heading', { level: 1, name: 'Alpha Phone' })).toBeInTheDocument();
    expect(screen.getByText('Alpha')).toHaveClass('detail-brand');
    expect(screen.getByText('Electronics')).toBeInTheDocument();
    const sku = screen.getByRole('button', { name: 'ALPHA-1' });
    expect(sku).toHaveClass('copy', 'mono');
    expect(sku).toHaveAttribute('title', 'Copy SKU');

    expect(screen.getByText('Warranty').nextElementSibling).toHaveTextContent('2 years');
    expect(screen.getByText('Seller').nextElementSibling).toHaveTextContent('Alpha Retail Ltd');
  });

  it('leaves out the brand, category, warranty and seller when there are none', async () => {
    serve(makeDetail({ category: undefined }));
    renderDetail('/products/1');

    await screen.findByRole('heading', { level: 1, name: 'Alpha Phone' });
    expect(document.querySelector('.detail-brand')).toBeNull();
    expect(screen.queryByText('Warranty')).not.toBeInTheDocument();
    expect(screen.queryByText('Seller')).not.toBeInTheDocument();
    expect(screen.queryByText('Electronics')).not.toBeInTheDocument();
  });

  it('links back to the product list from a breadcrumb', async () => {
    serve(makeDetail());
    const user = userEvent.setup();
    renderDetail('/products/1');

    const breadcrumb = await screen.findByRole('navigation', { name: 'Breadcrumb' });
    expect(within(breadcrumb).getByText('Alpha Phone')).toHaveAttribute('aria-current', 'page');
    await user.click(within(breadcrumb).getByRole('link', { name: 'Products' }));
    expect(await screen.findByText('The product list')).toBeInTheDocument();
  });

  describe('gallery', () => {
    it('shows the first image large, with a thumbnail for each image', async () => {
      serve(makeDetail({ images }));
      renderDetail('/products/1');

      await screen.findByRole('heading', { level: 1 });
      expect(mainImage()).toHaveAttribute('src', images[0].url);
      expect(mainImage()).toHaveAttribute('alt', 'Front view');
      expect(within(gallery()).getAllByRole('button')).toHaveLength(3);
      expect(thumb(1, 3)).toHaveAttribute('aria-current', 'true');
      expect(thumb(2, 3)).not.toHaveAttribute('aria-current');
      expect(thumb(3, 3)).not.toHaveAttribute('aria-current');
    });

    it('changes the main image when a thumbnail is clicked', async () => {
      serve(makeDetail({ images }));
      const user = userEvent.setup();
      renderDetail('/products/1');
      await screen.findByRole('heading', { level: 1 });

      await user.click(thumb(2, 3));
      expect(mainImage()).toHaveAttribute('src', images[1].url);
      expect(mainImage()).toHaveAttribute('alt', 'Side view');
      expect(thumb(2, 3)).toHaveAttribute('aria-current', 'true');
      expect(thumb(1, 3)).not.toHaveAttribute('aria-current');

      await user.click(thumb(3, 3));
      expect(mainImage()).toHaveAttribute('src', images[2].url);
      expect(mainImage()).toHaveAttribute('alt', 'Alpha Phone');
    });

    it('moves between images with the arrow keys and keeps focus on the selected thumbnail', async () => {
      serve(makeDetail({ images }));
      const user = userEvent.setup();
      renderDetail('/products/1');
      await screen.findByRole('heading', { level: 1 });

      await user.click(thumb(1, 3));
      await user.keyboard('{ArrowRight}');
      expect(mainImage()).toHaveAttribute('src', images[1].url);
      expect(thumb(2, 3)).toHaveAttribute('aria-current', 'true');
      expect(thumb(2, 3)).toHaveFocus();

      await user.keyboard('{ArrowRight}');
      expect(mainImage()).toHaveAttribute('src', images[2].url);
      expect(thumb(3, 3)).toHaveFocus();

      await user.keyboard('{ArrowLeft}');
      expect(mainImage()).toHaveAttribute('src', images[1].url);
      expect(thumb(2, 3)).toHaveFocus();
    });

    it('wraps around at either end', async () => {
      serve(makeDetail({ images }));
      const user = userEvent.setup();
      renderDetail('/products/1');
      await screen.findByRole('heading', { level: 1 });

      await user.click(thumb(1, 3));
      await user.keyboard('{ArrowLeft}');
      expect(thumb(3, 3)).toHaveAttribute('aria-current', 'true');
      await user.keyboard('{ArrowRight}');
      expect(thumb(1, 3)).toHaveAttribute('aria-current', 'true');
    });

    it('leaves Alt+Arrow (browser back and forward) alone', async () => {
      serve(makeDetail({ images }));
      const user = userEvent.setup();
      renderDetail('/products/1');
      await screen.findByRole('heading', { level: 1 });

      await user.click(thumb(1, 3));
      await user.keyboard('{Alt>}{ArrowRight}{/Alt}');
      expect(thumb(1, 3)).toHaveAttribute('aria-current', 'true');
    });

    it('shows the fallback tile when there are no images', async () => {
      serve(makeDetail({ images: [] }));
      renderDetail('/products/1');

      await screen.findByRole('heading', { level: 1 });
      expect(within(gallery()).queryByRole('button')).not.toBeInTheDocument();
      expect(gallery().querySelector('img')).toBeNull();
      const tile = within(gallery()).getByRole('img', { name: 'Alpha Phone' });
      expect(tile).toHaveClass('picture-fallback');
      expect(tile).toHaveTextContent('AP');
    });

    it('does not show a thumbnail strip for a single image', async () => {
      serve(makeDetail({ images: [images[0]] }));
      renderDetail('/products/1');

      await screen.findByRole('heading', { level: 1 });
      expect(mainImage()).toHaveAttribute('src', images[0].url);
      expect(within(gallery()).queryByRole('button')).not.toBeInTheDocument();
    });

    it('shows the fallback for an image that fails to load and for that image only', async () => {
      serve(makeDetail({ images }));
      const user = userEvent.setup();
      renderDetail('/products/1');
      await screen.findByRole('heading', { level: 1 });

      fireEvent.error(mainImage());
      expect(within(gallery()).getByRole('img', { name: 'Front view' })).toHaveClass('picture-fallback');
      expect(within(gallery()).getByRole('img', { name: 'Front view' })).toHaveTextContent('AP');

      // The other images are fine: the thumbnails still show, and so does the next main image.
      expect(gallery().querySelectorAll('.thumb img')).toHaveLength(3);
      await user.click(thumb(2, 3));
      expect(mainImage()).toHaveAttribute('src', images[1].url);
    });

    it('never renders an image URL that is not http or https', async () => {
      serve(makeDetail({ images: [{ url: 'javascript:alert(1)' }, { url: 'data:image/png;base64,AAAA' }] }));
      renderDetail('/products/1');

      await screen.findByRole('heading', { level: 1 });
      expect(gallery().querySelector('img')).toBeNull();
      expect(within(gallery()).getByRole('img', { name: 'Alpha Phone' })).toHaveClass('picture-fallback');
    });
  });

  describe('price', () => {
    it('shows the MRP struck through and the saving when the MRP is above the price', async () => {
      serve(makeDetail({ price: 799.99, mrp: 999.99, discountPercent: 20 }));
      renderDetail('/products/1');

      await screen.findByRole('heading', { level: 1 });
      expect(screen.getByText('$799.99')).toHaveClass('price-now');
      expect(screen.getByText('$999.99').tagName).toBe('S');
      expect(screen.getByText('You save $200.00 (20% off)')).toBeInTheDocument();
    });

    it('works out the percentage itself when the server sent none', async () => {
      serve(makeDetail({ price: 75, mrp: 100 }));
      renderDetail('/products/1');

      expect(await screen.findByText('You save $25.00 (25% off)')).toBeInTheDocument();
    });

    it.each([
      ['no MRP', undefined],
      ['an MRP equal to the price', 799.99],
      ['an MRP below the price', 500],
    ])('shows only the price with %s', async (_label, mrp) => {
      serve(makeDetail({ price: 799.99, mrp }));
      renderDetail('/products/1');

      await screen.findByRole('heading', { level: 1 });
      expect(screen.getByText('$799.99')).toHaveClass('price-now');
      expect(document.querySelector('s')).toBeNull();
      expect(screen.queryByText(/You save/)).not.toBeInTheDocument();
    });
  });

  describe('stock line', () => {
    it.each([
      [42, 'In stock (42)', 'ok'],
      [21, 'In stock (21)', 'ok'],
      [20, 'Only 20 left', 'low'],
      [7, 'Only 7 left', 'low'],
      [1, 'Only 1 left', 'low'],
      [0, 'Out of stock', 'out'],
      [null, 'Stock unavailable', 'unknown'],
      [undefined, 'Stock unavailable', 'unknown'],
    ])('quantity %s reads "%s"', async (stockQuantity, text, level) => {
      serve(makeDetail({ stockQuantity }));
      renderDetail('/products/1');

      expect(await screen.findByText(text)).toHaveClass('stock-line', `stock-line--${level}`);
    });
  });

  describe('sections', () => {
    it('lists the highlights', async () => {
      serve(makeDetail({ highlights: ['6.1 inch display', '2 day battery', '  '] }));
      renderDetail('/products/1');

      const section = (await screen.findByRole('heading', { level: 2, name: 'Highlights' })).closest('section')!;
      const items = within(section).getAllByRole('listitem');
      expect(items.map((item) => item.textContent)).toEqual(['6.1 inch display', '2 day battery']);
    });

    it('shows the description as plain text with its line breaks kept', async () => {
      const description = 'First paragraph.\n\nSecond line with <b>markup</b> & an ampersand.';
      serve(makeDetail({ description }));
      renderDetail('/products/1');

      const section = (await screen.findByRole('heading', { level: 2, name: 'Description' })).closest('section')!;
      const text = section.querySelector('p')!;
      expect(text).toHaveClass('detail-description');
      // Verbatim, newlines included. The line breaks are shown by `white-space: pre-line` on
      // .detail-description in app.css (jsdom does not load the stylesheet, so not checked here).
      expect(text.textContent).toBe(description);
      expect(text.querySelector('b')).toBeNull(); // markup is text, not HTML
      expect(text.innerHTML).toContain('&lt;b&gt;markup&lt;/b&gt;');
    });

    it('shows one table per specification group, with the key as the row header', async () => {
      serve(
        makeDetail({
          specifications: [
            {
              group: 'General',
              items: [
                { key: 'Model', value: 'AP-1' },
                { key: 'Colour', value: 'Graphite' },
              ],
            },
            { group: 'Display', items: [{ key: 'Size', value: '6.1 inch' }] },
            { group: 'Empty group', items: [] },
          ],
        }),
      );
      renderDetail('/products/1');

      await screen.findByRole('heading', { level: 2, name: 'Specifications' });
      expect(screen.getAllByRole('table')).toHaveLength(2);

      const general = screen.getByRole('table', { name: 'General' });
      const rows = within(general).getAllByRole('row');
      expect(rows).toHaveLength(2);
      expect(within(rows[0]).getByRole('rowheader', { name: 'Model' })).toHaveAttribute('scope', 'row');
      expect(within(rows[0]).getByRole('cell')).toHaveTextContent('AP-1');
      expect(within(rows[1]).getByRole('rowheader', { name: 'Colour' })).toBeInTheDocument();
      expect(within(rows[1]).getByRole('cell')).toHaveTextContent('Graphite');

      const display = screen.getByRole('table', { name: 'Display' });
      expect(within(display).getByRole('rowheader', { name: 'Size' })).toBeInTheDocument();
      expect(within(display).getByRole('cell')).toHaveTextContent('6.1 inch');
      expect(screen.queryByRole('table', { name: 'Empty group' })).not.toBeInTheDocument();
    });

    it('leaves out sections that have nothing to show', async () => {
      serve(makeDetail({ description: undefined, highlights: [], specifications: [] }));
      renderDetail('/products/1');

      await screen.findByRole('heading', { level: 1 });
      expect(screen.queryByRole('heading', { level: 2, name: 'Highlights' })).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { level: 2, name: 'Description' })).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { level: 2, name: 'Specifications' })).not.toBeInTheDocument();
    });

    it('copes with lists missing from the response', async () => {
      const partial = { ...makeDetail() } as Partial<ProductDetailData>;
      delete partial.images;
      delete partial.highlights;
      delete partial.specifications;
      mockFetch({ 'GET /api/products/1/details': () => partial });
      renderDetail('/products/1');

      expect(await screen.findByRole('heading', { level: 1, name: 'Alpha Phone' })).toBeInTheDocument();
      expect(within(gallery()).getByRole('img', { name: 'Alpha Phone' })).toHaveClass('picture-fallback');
    });
  });

  describe('when something is wrong', () => {
    it('says the product does not exist when the API answers 404', async () => {
      mockFetch({ 'GET /api/products/9/details': () => problem(404, 'Not Found', 'Product with id 9 not found') });
      renderDetail('/products/9');

      expect(await screen.findByRole('heading', { level: 1, name: 'Product not found' })).toBeInTheDocument();
      expect(screen.getByText("This product doesn't exist. It may have been removed.")).toBeInTheDocument();
      expect(screen.getByRole('link', { name: 'Back to products' })).toHaveAttribute('href', '/products');
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
      await waitFor(() => expect(document.title).toBe('Product not found · Microservices demo'));
    });

    it('treats an id that is not a number as not found, without calling the API', async () => {
      const fetchMock = mockFetch({});
      renderDetail('/products/abc');

      expect(await screen.findByRole('heading', { level: 1, name: 'Product not found' })).toBeInTheDocument();
      expect(screen.getByText("This product doesn't exist. It may have been removed.")).toBeInTheDocument();
      expect(screen.getByRole('link', { name: 'Back to products' })).toHaveAttribute('href', '/products');
      expect(fetchMock).not.toHaveBeenCalled();
    });

    it.each(['0', '-3', '1.5', '12abc'])('treats the id "%s" as not found too', async (id) => {
      const fetchMock = mockFetch({});
      renderDetail(`/products/${id}`);

      expect(await screen.findByRole('heading', { level: 1, name: 'Product not found' })).toBeInTheDocument();
      expect(fetchMock).not.toHaveBeenCalled();
    });

    it('shows the reason and a retry for any other failure', async () => {
      let attempts = 0;
      mockFetch({
        'GET /api/products/1/details': () => {
          attempts += 1;
          return attempts === 1 ? problem(500, 'Internal Server Error', 'database is down') : makeDetail();
        },
      });
      const user = userEvent.setup();
      renderDetail('/products/1');

      expect(await screen.findByRole('alert')).toHaveTextContent('Could not load the product. database is down.');
      expect(screen.queryByText("This product doesn't exist. It may have been removed.")).not.toBeInTheDocument();
      expect(screen.getByRole('link', { name: 'Back to products' })).toHaveAttribute('href', '/products');

      await user.click(screen.getByRole('button', { name: 'Try again' }));
      expect(await screen.findByRole('heading', { level: 1, name: 'Alpha Phone' })).toBeInTheDocument();
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('says so when the server cannot be reached at all', async () => {
      vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
      renderDetail('/products/1');

      expect(await screen.findByRole('alert')).toHaveTextContent('Could not load the product. Could not reach the server.');
    });
  });
});
