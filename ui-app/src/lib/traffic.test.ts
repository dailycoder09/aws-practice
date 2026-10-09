import { runTraffic, serverKey, tallyTraffic, type TrafficSample } from './traffic';
import { makeServerInfo } from '../test/helpers';

const ok = (server: string): TrafficSample => ({ ok: true, server });
const failed: TrafficSample = { ok: false };

describe('serverKey', () => {
  it('prefers the EC2 instance ID', () => {
    expect(serverKey(makeServerInfo('ip-10-0-1-23', { instanceId: 'i-0abc' }))).toBe('i-0abc');
  });

  it('falls back to the hostname off EC2', () => {
    expect(serverKey(makeServerInfo('laptop.local'))).toBe('laptop.local');
  });
});

describe('tallyTraffic', () => {
  it('counts per server, busiest first, with percentages of successful responses', () => {
    const tally = tallyTraffic([ok('b'), ok('a'), ok('a'), ok('a')]);
    expect(tally.rows).toEqual([
      { server: 'a', count: 3, percent: 75 },
      { server: 'b', count: 1, percent: 25 },
    ]);
    expect(tally).toMatchObject({ succeeded: 4, failures: 0, total: 4 });
  });

  it('counts failures separately and leaves them out of the percentages', () => {
    const tally = tallyTraffic([ok('a'), failed, ok('b'), failed]);
    expect(tally.failures).toBe(2);
    expect(tally.succeeded).toBe(2);
    expect(tally.total).toBe(4);
    expect(tally.rows.map((row) => row.percent)).toEqual([50, 50]);
  });

  it('breaks ties by server name so the order is stable', () => {
    expect(tallyTraffic([ok('z'), ok('a')]).rows.map((row) => row.server)).toEqual(['a', 'z']);
  });

  it('handles no samples', () => {
    expect(tallyTraffic([])).toEqual({ rows: [], succeeded: 0, failures: 0, total: 0 });
  });

  it('handles every request failing', () => {
    expect(tallyTraffic([failed, failed])).toEqual({ rows: [], succeeded: 0, failures: 2, total: 2 });
  });

  it('shows 100 percent for a single server', () => {
    expect(tallyTraffic([ok('only'), ok('only')]).rows).toEqual([{ server: 'only', count: 2, percent: 100 }]);
  });
});

describe('runTraffic', () => {
  it('never has more requests in flight than the limit', async () => {
    let inFlight = 0;
    let peak = 0;
    const samples = await runTraffic(
      12,
      async () => {
        inFlight += 1;
        peak = Math.max(peak, inFlight);
        await new Promise((resolve) => setTimeout(resolve, 2));
        inFlight -= 1;
        return makeServerInfo('host');
      },
      { concurrency: 5 },
    );
    expect(samples).toHaveLength(12);
    expect(peak).toBe(5);
  });

  it('records a thrown request as a failure and keeps going', async () => {
    const samples = await runTraffic(4, async (index) => {
      if (index === 1) throw new Error('boom');
      return makeServerInfo(index % 2 === 0 ? 'a' : 'b');
    });
    expect(samples).toEqual([ok('a'), failed, ok('a'), ok('b')]);
  });

  it('reports progress after every request', async () => {
    const seen: number[] = [];
    await runTraffic(3, async () => makeServerInfo('a'), { onSample: (_sample, done) => seen.push(done) });
    expect(seen).toEqual([1, 2, 3]);
  });

  it('sends nothing for a count of zero', async () => {
    const send = vi.fn();
    expect(await runTraffic(0, send)).toEqual([]);
    expect(send).not.toHaveBeenCalled();
  });
});
