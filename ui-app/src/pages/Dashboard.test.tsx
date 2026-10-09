import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Dashboard } from './Dashboard';
import { makeItem, makeProduct, makeServerInfo, mockFetch, pageOf, problem, renderAt } from '../test/helpers';

function totalsRoutes() {
  return {
    'GET /api/products': () => pageOf([makeProduct(1, 'Alpha', 'A-1')], { size: 1, totalElements: 5010 }),
    'GET /api/products/categories': () => ['Electronics', 'Home', 'Toys'],
    'GET /api/inventory': () => pageOf([makeItem(1, 'A-1', 5, 1)], { size: 1, totalElements: 4980 }),
  };
}

describe('Dashboard', () => {
  it('shows the nameplate, addresses and EC2 facts for a service that answers', async () => {
    mockFetch({
      ...totalsRoutes(),
      'GET /api/products/server-info': () =>
        makeServerInfo('ip-10-0-1-23.ec2.internal', { instanceId: 'i-0abc123', zone: 'eu-west-1a', forwardedFor: '198.51.100.4' }),
      'GET /api/inventory/server-info': () => makeServerInfo('ip-10-0-2-77.ec2.internal', { instanceId: 'i-0def456', zone: 'eu-west-1b' }),
    });
    renderAt(<Dashboard />);

    const panel = await screen.findByRole('region', { name: 'Product service' });
    expect(await within(panel).findByText('ip-10-0-1-23.ec2.internal')).toBeInTheDocument();
    expect(within(panel).getByText('Up')).toBeInTheDocument();
    expect(within(panel).getByText('10.0.1.23')).toBeInTheDocument();
    expect(within(panel).queryByText('127.0.0.1')).not.toBeInTheDocument(); // loopback says nothing about the server
    expect(within(panel).queryByText('fe80::1')).not.toBeInTheDocument(); // IPv4 only
    expect(within(panel).getByText('i-0abc123')).toBeInTheDocument();
    expect(within(panel).getByText('eu-west-1a')).toBeInTheDocument();
    expect(within(panel).getByText('198.51.100.4')).toBeInTheDocument();
    expect(within(panel).getByText('10.0.1.23:8081')).toBeInTheDocument();
    expect(within(panel).getByText('1.2.3')).toBeInTheDocument();
    expect(within(panel).getByText('210 of 512 MB')).toBeInTheDocument();
    expect(within(panel).getByText(/^00:00:\d\d$/)).toBeInTheDocument(); // live uptime

    const other = screen.getByRole('region', { name: 'Inventory service' });
    expect(await within(other).findByText('ip-10-0-2-77.ec2.internal')).toBeInTheDocument();
  });

  it('says "Not on EC2" when there is no cloud block', async () => {
    mockFetch({
      ...totalsRoutes(),
      'GET /api/products/server-info': () => makeServerInfo('laptop.local'),
      'GET /api/inventory/server-info': () => makeServerInfo('laptop.local'),
    });
    renderAt(<Dashboard />);

    const panel = await screen.findByRole('region', { name: 'Product service' });
    expect(await within(panel).findByText('Not on EC2')).toBeInTheDocument();
  });

  it('marks a service unreachable when its call fails, and keeps the other one', async () => {
    mockFetch({
      ...totalsRoutes(),
      'GET /api/products/server-info': () => makeServerInfo('ip-10-0-1-23'),
      'GET /api/inventory/server-info': () => problem(503, 'Service Unavailable', 'no healthy targets'),
    });
    renderAt(<Dashboard />);

    const down = screen.getByRole('region', { name: 'Inventory service' });
    expect(await within(down).findByText('Unreachable')).toBeInTheDocument();
    expect(within(down).getByRole('alert')).toHaveTextContent('no healthy targets.');
    const up = screen.getByRole('region', { name: 'Product service' });
    expect(await within(up).findByText('ip-10-0-1-23')).toBeInTheDocument();
    expect(within(up).getByText('Up')).toBeInTheDocument();
  });

  it('notices when a refresh is answered by a different server', async () => {
    let calls = 0;
    mockFetch({
      ...totalsRoutes(),
      'GET /api/products/server-info': () => makeServerInfo(++calls === 1 ? 'host-a' : 'host-b'),
      'GET /api/inventory/server-info': () => makeServerInfo('inv-1'),
    });
    const user = userEvent.setup();
    renderAt(<Dashboard />);

    const panel = await screen.findByRole('region', { name: 'Product service' });
    expect(await within(panel).findByText('host-a')).toBeInTheDocument();
    expect(within(panel).queryByText(/different server than last time/)).not.toBeInTheDocument();

    await user.click(within(panel).getByRole('button', { name: 'Refresh' }));

    expect(await within(panel).findByText('host-b')).toBeInTheDocument();
    expect(await within(panel).findByText('Answered by a different server than last time.')).toBeInTheDocument();
  });

  it('shows each total, and a dash for one that fails without hiding the others', async () => {
    mockFetch({
      ...totalsRoutes(),
      'GET /api/inventory': () => problem(500, 'Internal Server Error'),
      'GET /api/products/server-info': () => makeServerInfo('a'),
      'GET /api/inventory/server-info': () => makeServerInfo('b'),
    });
    renderAt(<Dashboard />);

    const totals = screen.getByLabelText('Totals');
    expect(await within(totals).findByText('5,010')).toBeInTheDocument();
    expect(await within(totals).findByText('3')).toBeInTheDocument();
    const inventoryTotal = within(totals).getByText('Inventory records').closest('div')!;
    expect(await within(inventoryTotal).findByText('—')).toBeInTheDocument();
  });
});
