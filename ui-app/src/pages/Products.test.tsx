import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Products } from './Products';
import { makeProduct, mockFetch, pageOf, problem, renderAt, requestedUrls } from '../test/helpers';

const products = [
  makeProduct(1, 'Alpha Phone', 'ALPHA-1', {
    price: 1234.5,
    brand: 'Alpha',
    mrp: 1500,
    discountPercent: 18,
    imageUrl: 'https://placehold.co/800x800/png?text=Alpha',
  }),
  makeProduct(2, 'Bravo Lamp', 'BRAVO-2', { category: 'Home' }),
  // An MRP equal to the price is not a discount: the server sends no discountPercent.
  makeProduct(3, 'Charlie Mug', 'CHARLIE-3', { category: 'Home', brand: 'Charlie Co', mrp: 19.99 }),
];

function baseRoutes() {
  return {
    'GET /api/products': () => pageOf(products, { totalElements: 45 }),
    'GET /api/products/search': () => pageOf([products[1]]),
    'GET /api/products/categories': () => ['Electronics', 'Home'],
    // CHARLIE-3 is deliberately missing: the inventory service has no record for it.
    'GET /api/inventory/batch': () => ({ 'ALPHA-1': 215, 'BRAVO-2': 13 }),
  };
}

/** The card (list item) that holds a product's name link. */
async function cardOf(name: string): Promise<HTMLElement> {
  const link = await screen.findByRole('link', { name });
  return link.closest('li')!;
}

describe('Products page', () => {
  it('shows each product as a card with a link to its detail page', async () => {
    mockFetch(baseRoutes());
    renderAt(<Products />, '/products');

    const alpha = await screen.findByRole('link', { name: 'Alpha Phone' });
    expect(alpha).toHaveAttribute('href', '/products/1');
    expect(screen.getByRole('link', { name: 'Bravo Lamp' })).toHaveAttribute('href', '/products/2');
    expect(screen.getByRole('link', { name: 'Charlie Mug' })).toHaveAttribute('href', '/products/3');
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
    // Names are links now. They no longer open the SKU panel.
    expect(screen.queryByRole('button', { name: 'Alpha Phone' })).not.toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('shows brand, price, picture and initials on each card, and omits what is absent', async () => {
    mockFetch(baseRoutes());
    renderAt(<Products />, '/products');

    const alpha = await cardOf('Alpha Phone');
    expect(within(alpha).getByText('Alpha')).toBeInTheDocument();
    expect(within(alpha).getByText('$1,234.50')).toBeInTheDocument();
    const picture = alpha.querySelector('img')!;
    expect(picture).toHaveAttribute('src', 'https://placehold.co/800x800/png?text=Alpha');
    expect(picture).toHaveAttribute('loading', 'lazy');

    // No brand and no image on this one: no brand line, the initials tile instead.
    const bravo = await cardOf('Bravo Lamp');
    expect(bravo.querySelector('.product-brand')).toBeNull();
    expect(bravo.querySelector('img')).toBeNull();
    expect(within(bravo).getByText('BL')).toBeInTheDocument();
    expect(within(bravo).getByText('$19.99')).toBeInTheDocument();
  });

  it('strikes through the MRP and shows the percentage only when the product has a discount', async () => {
    mockFetch(baseRoutes());
    renderAt(<Products />, '/products');

    const alpha = await cardOf('Alpha Phone');
    const mrp = within(alpha).getByText('$1,500.00');
    expect(mrp.tagName).toBe('S');
    expect(within(alpha).getByText('18% off')).toHaveClass('discount');

    for (const name of ['Bravo Lamp', 'Charlie Mug']) {
      const card = await cardOf(name);
      expect(card.querySelector('s')).toBeNull();
      expect(within(card).queryByText(/% off/)).not.toBeInTheDocument();
    }
    expect(screen.getAllByText(/% off/)).toHaveLength(1);
  });

  it('puts a stock badge on each card from the batch call, in one request', async () => {
    const fetchMock = mockFetch(baseRoutes());
    renderAt(<Products />, '/products');

    expect(await screen.findByText('215')).toHaveClass('stock--ok');
    expect(screen.getByText('13')).toHaveClass('stock--low');
    const charlie = await cardOf('Charlie Mug');
    expect(within(charlie).getByText('—')).toHaveClass('stock--unknown');

    expect(screen.getByText('Showing 1 to 3 of 45 products')).toBeInTheDocument();
    expect(screen.getByText('Page 1 of 3')).toBeInTheDocument();
    expect(screen.queryByText(/Stock levels are unavailable/)).not.toBeInTheDocument();

    const batchCalls = requestedUrls(fetchMock).filter((url) => url.startsWith('/api/inventory/batch'));
    expect(batchCalls).toEqual(['/api/inventory/batch?skuCodes=ALPHA-1,BRAVO-2,CHARLIE-3']);
  });

  it('still shows the cards, with grey badges and a notice, when the stock call fails', async () => {
    mockFetch({
      ...baseRoutes(),
      'GET /api/inventory/batch': () => problem(503, 'Service Unavailable', 'inventory is down'),
    });
    renderAt(<Products />, '/products');

    expect(await screen.findByRole('link', { name: 'Alpha Phone' })).toBeInTheDocument();
    expect(
      await screen.findByText('Stock levels are unavailable right now. The inventory service may be down.'),
    ).toBeInTheDocument();
    await waitFor(() => expect(document.querySelectorAll('.stock--unknown')).toHaveLength(3));
    expect(screen.getByRole('link', { name: 'Bravo Lamp' })).toBeInTheDocument();
    expect(screen.getByText('$1,234.50')).toBeInTheDocument();
  });

  it('searches when the URL has a search term or category', async () => {
    const fetchMock = mockFetch(baseRoutes());
    renderAt(<Products />, '/products?q=lamp&category=Home&page=2');

    expect(await screen.findByRole('link', { name: 'Bravo Lamp' })).toBeInTheDocument();
    expect(screen.getByRole('searchbox')).toHaveValue('lamp');
    expect(screen.getByRole('combobox', { name: 'Category' })).toHaveValue('Home');
    const search = requestedUrls(fetchMock).find((url) => url.startsWith('/api/products/search'))!;
    const params = new URL(search, 'http://localhost').searchParams;
    expect(params.get('searchTerm')).toBe('lamp');
    expect(params.get('category')).toBe('Home');
    expect(params.get('page')).toBe('1'); // the URL is one-based, the API zero-based
  });

  it('waits for a pause in typing, then searches', async () => {
    const fetchMock = mockFetch(baseRoutes());
    const user = userEvent.setup();
    renderAt(<Products />, '/products');
    await screen.findByRole('link', { name: 'Alpha Phone' });

    await user.type(screen.getByRole('searchbox'), 'lamp');

    await waitFor(() =>
      expect(requestedUrls(fetchMock).some((url) => url.startsWith('/api/products/search?searchTerm=lamp'))).toBe(true),
    );
    // One request for the finished word, not one per keystroke.
    expect(requestedUrls(fetchMock).filter((url) => url.startsWith('/api/products/search'))).toHaveLength(1);
  });

  it('searches by category when one is picked', async () => {
    const fetchMock = mockFetch(baseRoutes());
    const user = userEvent.setup();
    renderAt(<Products />, '/products');
    await screen.findByRole('link', { name: 'Alpha Phone' });
    await screen.findByRole('option', { name: 'Home' });

    await user.selectOptions(screen.getByRole('combobox', { name: 'Category' }), 'Home');

    expect(await screen.findByRole('link', { name: 'Bravo Lamp' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Alpha Phone' })).not.toBeInTheDocument();
    const search = requestedUrls(fetchMock).find((url) => url.startsWith('/api/products/search'))!;
    expect(new URL(search, 'http://localhost').searchParams.get('category')).toBe('Home');
    expect(screen.getByRole('button', { name: 'Clear filters' })).toBeInTheDocument();
  });

  it('moves to the next page and asks the API for it', async () => {
    const fetchMock = mockFetch(baseRoutes());
    const user = userEvent.setup();
    renderAt(<Products />, '/products');
    await screen.findByRole('link', { name: 'Alpha Phone' });

    await user.click(screen.getByRole('button', { name: 'Next' }));

    await waitFor(() =>
      expect(
        requestedUrls(fetchMock).some(
          (url) => url.startsWith('/api/products?') && new URL(url, 'http://localhost').searchParams.get('page') === '1',
        ),
      ).toBe(true),
    );
  });

  it('shows an empty state with a way to clear the filters', async () => {
    mockFetch({ ...baseRoutes(), 'GET /api/products/search': () => pageOf([]) });
    const user = userEvent.setup();
    renderAt(<Products />, '/products?q=zzz');

    expect(await screen.findByText('No products match. Clear the search or pick another category.')).toBeInTheDocument();
    expect(screen.queryByRole('list')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Clear search and category' }));
    expect(await screen.findByRole('link', { name: 'Alpha Phone' })).toBeInTheDocument();
    expect(screen.getByRole('searchbox')).toHaveValue('');
  });

  it('says what went wrong when the product list cannot load', async () => {
    mockFetch({ ...baseRoutes(), 'GET /api/products': () => problem(500, 'Internal Server Error', 'database is down') });
    renderAt(<Products />, '/products');

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not load products. database is down.');
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
  });
});
