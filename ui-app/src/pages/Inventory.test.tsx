import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Inventory } from './Inventory';
import { makeItem, mockFetch, pageOf, problem, renderAt, requestedUrls } from '../test/helpers';

const items = [makeItem(1, 'ALPHA-1', 5, 10), makeItem(2, 'BRAVO-2', 0, 5), makeItem(3, 'CHARLIE-3', 300, 50)];

function rowFor(sku: string): HTMLElement {
  return screen.getByRole('button', { name: sku }).closest('tr')!;
}

describe('Inventory page', () => {
  it('colours the quantity by the item own reorder threshold', async () => {
    mockFetch({ 'GET /api/inventory': () => pageOf(items) });
    renderAt(<Inventory />, '/inventory');

    expect(await screen.findByRole('button', { name: 'ALPHA-1' })).toBeInTheDocument();
    expect(within(rowFor('ALPHA-1')).getByText('5')).toHaveClass('stock--low');
    expect(within(rowFor('BRAVO-2')).getByText('0')).toHaveClass('stock--out');
    expect(within(rowFor('CHARLIE-3')).getByText('300')).toHaveClass('stock--ok');
    expect(screen.getByText('Showing 1 to 3 of 3 records')).toBeInTheDocument();
  });

  it('shows the problem detail inline on the row when the adjustment is rejected', async () => {
    const fetchMock = mockFetch({
      'GET /api/inventory': () => pageOf(items),
      'PATCH /api/inventory/sku/ALPHA-1/adjust': () =>
        problem(400, 'Bad Request', 'Cannot remove 50 units of ALPHA-1: only 5 on hand'),
    });
    const user = userEvent.setup();
    renderAt(<Inventory />, '/inventory');
    await screen.findByRole('button', { name: 'ALPHA-1' });

    const row = rowFor('ALPHA-1');
    const amount = within(row).getByRole('spinbutton');
    await user.clear(amount);
    await user.type(amount, '50');
    await user.click(within(row).getByRole('button', { name: /remove/i }));

    expect(await within(row).findByRole('alert')).toHaveTextContent('Cannot remove 50 units of ALPHA-1: only 5 on hand');
    // Nothing changed, the buttons are usable again, and other rows are untouched.
    expect(within(row).getByText('5')).toBeInTheDocument();
    expect(within(row).getByRole('button', { name: /remove/i })).toBeEnabled();
    expect(within(rowFor('BRAVO-2')).queryByRole('alert')).not.toBeInTheDocument();
    expect(requestedUrls(fetchMock)).toContain('/api/inventory/sku/ALPHA-1/adjust?delta=-50');
  });

  it('updates the row in place after a successful add', async () => {
    const fetchMock = mockFetch({
      'GET /api/inventory': () => pageOf(items),
      'PATCH /api/inventory/sku/ALPHA-1/adjust': () => makeItem(1, 'ALPHA-1', 8, 10),
    });
    const user = userEvent.setup();
    renderAt(<Inventory />, '/inventory');
    await screen.findByRole('button', { name: 'ALPHA-1' });

    const row = rowFor('ALPHA-1');
    const amount = within(row).getByRole('spinbutton');
    await user.clear(amount);
    await user.type(amount, '3');
    await user.click(within(row).getByRole('button', { name: /add/i }));

    expect(await within(row).findByText('8')).toHaveClass('stock--low');
    expect(row).toHaveClass('flash-a');
    expect(requestedUrls(fetchMock)).toContain('/api/inventory/sku/ALPHA-1/adjust?delta=3');
    // The list was not reloaded to show the change.
    expect(requestedUrls(fetchMock).filter((url) => url.startsWith('/api/inventory?'))).toHaveLength(1);
  });

  it('asks for a whole number instead of sending a bad amount', async () => {
    const fetchMock = mockFetch({ 'GET /api/inventory': () => pageOf(items) });
    const user = userEvent.setup();
    renderAt(<Inventory />, '/inventory');
    await screen.findByRole('button', { name: 'ALPHA-1' });

    const row = rowFor('ALPHA-1');
    await user.clear(within(row).getByRole('spinbutton'));
    await user.click(within(row).getByRole('button', { name: /add/i }));

    expect(await within(row).findByRole('alert')).toHaveTextContent('Enter a whole number of 1 or more.');
    expect(requestedUrls(fetchMock).some((url) => url.includes('/adjust'))).toBe(false);
  });

  it('says what went wrong when the list cannot load', async () => {
    mockFetch({ 'GET /api/inventory': () => problem(503, 'Service Unavailable') });
    renderAt(<Inventory />, '/inventory');

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not load inventory.');
    expect(screen.getByRole('alert')).toHaveTextContent('Service Unavailable');
  });
});
