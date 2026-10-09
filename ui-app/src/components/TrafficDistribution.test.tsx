import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TrafficDistribution } from './TrafficDistribution';
import { makeServerInfo, mockFetch, problem, requestedUrls } from '../test/helpers';

describe('TrafficDistribution', () => {
  it('tallies which server answered, per service, and counts failures', async () => {
    let productCalls = 0;
    const fetchMock = mockFetch({
      // Two hostnames alternating, as a load balancer with two targets would.
      'GET /api/products/server-info': () => makeServerInfo(++productCalls % 2 === 1 ? 'web-a' : 'web-b'),
      'GET /api/inventory/server-info': () => problem(503, 'Service Unavailable'),
    });
    const user = userEvent.setup();
    render(<TrafficDistribution />);

    const input = screen.getByLabelText('Requests per service');
    expect(input).toHaveValue(20);
    await user.clear(input);
    await user.type(input, '4');
    await user.click(screen.getByRole('button', { name: 'Send 4 requests' }));

    const products = screen.getByRole('group', { name: 'Product service' });
    expect(await within(products).findByText('4 sent')).toBeInTheDocument();
    const rowA = within(products).getByText('web-a').closest('li')!;
    const rowB = within(products).getByText('web-b').closest('li')!;
    expect(rowA).toHaveTextContent('2');
    expect(rowA).toHaveTextContent('50%');
    expect(rowB).toHaveTextContent('2');
    expect(rowB).toHaveTextContent('50%');
    expect(within(products).getByText('0 failures')).toBeInTheDocument();

    const inventory = screen.getByRole('group', { name: 'Inventory service' });
    expect(await within(inventory).findByText('4 failures')).toBeInTheDocument();
    expect(within(inventory).getByText('No successful responses yet.')).toBeInTheDocument();

    // Every request carries a cache-buster and the button is usable again.
    const urls = requestedUrls(fetchMock);
    expect(urls).toHaveLength(8);
    expect(new Set(urls).size).toBe(8);
    expect(urls.every((url) => /\/server-info\?t=\d+-\d+$/.test(url))).toBe(true);
    expect(await screen.findByRole('button', { name: 'Send 4 requests' })).toBeEnabled();
  });

  it('reports 100 percent on one host when there is only one server', async () => {
    mockFetch({
      'GET /api/products/server-info': () => makeServerInfo('only-host', { instanceId: 'i-0one' }),
      'GET /api/inventory/server-info': () => makeServerInfo('only-host', { instanceId: 'i-0one' }),
    });
    const user = userEvent.setup();
    render(<TrafficDistribution />);
    const input = screen.getByLabelText('Requests per service');
    await user.clear(input);
    await user.type(input, '3');
    await user.click(screen.getByRole('button', { name: 'Send 3 requests' }));

    const products = screen.getByRole('group', { name: 'Product service' });
    expect(await within(products).findByText('3 sent')).toBeInTheDocument();
    // Tallied by EC2 instance ID, not hostname.
    expect(within(products).getByText('i-0one').closest('li')).toHaveTextContent('100%');
  });

  it('will not send an out-of-range number of requests', async () => {
    mockFetch({});
    const user = userEvent.setup();
    render(<TrafficDistribution />);
    const input = screen.getByLabelText('Requests per service');
    await user.clear(input);
    await user.type(input, '101');

    expect(screen.getByRole('button', { name: 'Send requests' })).toBeDisabled();
    expect(screen.getByRole('alert')).toHaveTextContent('Enter a whole number from 1 to 100.');
  });
});
