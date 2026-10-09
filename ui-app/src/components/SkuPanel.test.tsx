import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { SkuPanel } from './SkuPanel';
import { makeItem, makeProduct, mockFetch, pageOf, problem, renderAt } from '../test/helpers';

function Harness() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button type="button" onClick={() => setOpen(true)}>
        Open panel
      </button>
      {open && <SkuPanel sku="ALPHA-1" onClose={() => setOpen(false)} />}
    </>
  );
}

describe('SkuPanel', () => {
  it('loads three sections independently, so one failing never blanks the others', async () => {
    mockFetch({
      'GET /api/products/sku/ALPHA-1': () => problem(404, 'Not Found'),
      'GET /api/inventory/sku/ALPHA-1': () => makeItem(1, 'ALPHA-1', 7, 10),
      'GET /api/inventory/sku/ALPHA-1/audit': () => problem(500, 'Internal Server Error', 'audit table is locked'),
    });
    const user = userEvent.setup();
    renderAt(<Harness />);
    await user.click(screen.getByRole('button', { name: 'Open panel' }));

    const dialog = await screen.findByRole('dialog', { name: /ALPHA-1/ });
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(await within(dialog).findByText('This SKU is not in the product catalog.')).toBeInTheDocument();
    // No product, so nothing to open.
    expect(within(dialog).queryByRole('link', { name: 'Open product page' })).not.toBeInTheDocument();
    expect(await within(dialog).findByText('7')).toHaveClass('stock--low');
    expect(within(dialog).getByText('10')).toBeInTheDocument();
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('audit table is locked');
  });

  it('shows product details, a missing stock record and recent changes', async () => {
    mockFetch({
      'GET /api/products/sku/ALPHA-1': () => makeProduct(1, 'Alpha Phone', 'ALPHA-1', { price: 999.99, description: 'A very fine phone' }),
      'GET /api/inventory/sku/ALPHA-1': () => problem(404, 'Not Found'),
      'GET /api/inventory/sku/ALPHA-1/audit': () =>
        pageOf([
          { id: 2, skuCode: 'ALPHA-1', changeType: 'ADJUSTED', previousQuantity: 10, newQuantity: 7, changedAt: '2026-10-04T08:43:44' },
          { id: 1, skuCode: 'ALPHA-1', changeType: 'CREATED', newQuantity: 10, changedAt: '2026-10-04T08:00:00' },
        ]),
    });
    const user = userEvent.setup();
    renderAt(<Harness />);
    await user.click(screen.getByRole('button', { name: 'Open panel' }));

    const dialog = await screen.findByRole('dialog', { name: /ALPHA-1/ });
    expect(await within(dialog).findByText('Alpha Phone')).toBeInTheDocument();
    expect(within(dialog).getByText('$999.99')).toBeInTheDocument();
    expect(within(dialog).getByText('A very fine phone')).toBeInTheDocument();
    expect(within(dialog).getByRole('link', { name: 'Open product page' })).toHaveAttribute('href', '/products/1');
    expect(await within(dialog).findByText('No stock record for this SKU.')).toBeInTheDocument();
    const entries = await within(dialog).findAllByRole('listitem');
    expect(entries).toHaveLength(2);
    expect(entries[0]).toHaveTextContent('Adjusted');
    expect(entries[0]).toHaveTextContent('10');
    expect(entries[0]).toHaveTextContent('7');
    expect(entries[1]).toHaveTextContent('Created');
  });

  it('takes focus on open, closes on Escape and gives focus back', async () => {
    mockFetch({
      'GET /api/products/sku/ALPHA-1': () => makeProduct(1, 'Alpha Phone', 'ALPHA-1'),
      'GET /api/inventory/sku/ALPHA-1': () => makeItem(1, 'ALPHA-1', 50, 10),
      'GET /api/inventory/sku/ALPHA-1/audit': () => pageOf([]),
    });
    const user = userEvent.setup();
    renderAt(<Harness />);
    const opener = screen.getByRole('button', { name: 'Open panel' });
    await user.click(opener);

    const dialog = await screen.findByRole('dialog', { name: /ALPHA-1/ });
    expect(dialog).toHaveFocus();

    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(opener).toHaveFocus();
  });

  it('closes from the Close button and keeps Tab inside the drawer', async () => {
    mockFetch({
      'GET /api/products/sku/ALPHA-1': () => makeProduct(1, 'Alpha Phone', 'ALPHA-1'),
      'GET /api/inventory/sku/ALPHA-1': () => makeItem(1, 'ALPHA-1', 50, 10),
      'GET /api/inventory/sku/ALPHA-1/audit': () => pageOf([]),
    });
    const user = userEvent.setup();
    renderAt(<Harness />);
    await user.click(screen.getByRole('button', { name: 'Open panel' }));
    const dialog = await screen.findByRole('dialog', { name: /ALPHA-1/ });
    const close = within(dialog).getByRole('button', { name: 'Close' });
    // Wait for the product, so the set of focusable things is settled: Close, then the link.
    const open = await within(dialog).findByRole('link', { name: 'Open product page' });

    await user.tab();
    expect(close).toHaveFocus();
    await user.tab();
    expect(open).toHaveFocus();
    // Past the last one, Tab wraps back to the first instead of leaving the drawer.
    await user.tab();
    expect(close).toHaveFocus();
    // And Shift+Tab from the first wraps to the last.
    await user.tab({ shift: true });
    expect(open).toHaveFocus();

    await user.click(close);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
