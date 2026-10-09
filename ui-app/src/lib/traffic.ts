import type { ServerInfo } from '../api/types';

export type TrafficSample = { ok: true; server: string } | { ok: false };

export interface TrafficRow {
  server: string;
  count: number;
  /** Share of the successful responses, 0 to 100. */
  percent: number;
}

export interface TrafficTally {
  rows: TrafficRow[];
  succeeded: number;
  failures: number;
  total: number;
}

/** Which server answered: the EC2 instance ID when there is one, else the hostname. */
export function serverKey(info: ServerInfo): string {
  return info.cloud?.instanceId || info.host.hostname;
}

/** Groups samples by server, busiest first. Percentages are of successful responses. */
export function tallyTraffic(samples: readonly TrafficSample[]): TrafficTally {
  const counts = new Map<string, number>();
  let failures = 0;
  for (const sample of samples) {
    if (sample.ok) counts.set(sample.server, (counts.get(sample.server) ?? 0) + 1);
    else failures += 1;
  }
  const succeeded = samples.length - failures;
  const rows = [...counts.entries()]
    .map(([server, count]) => ({ server, count, percent: (count / succeeded) * 100 }))
    .sort((a, b) => b.count - a.count || a.server.localeCompare(b.server));
  return { rows, succeeded, failures, total: samples.length };
}

export interface RunTrafficOptions {
  /** How many requests may be in flight at once. */
  concurrency?: number;
  /** Called after every request finishes, with the sample and how many are done. */
  onSample?: (sample: TrafficSample, done: number) => void;
  signal?: AbortSignal;
}

/**
 * Sends `count` requests through `send`, never more than `concurrency` at a
 * time. A request that throws counts as a failure. Samples come back in
 * request order.
 */
export async function runTraffic(
  count: number,
  send: (index: number) => Promise<ServerInfo>,
  { concurrency = 5, onSample, signal }: RunTrafficOptions = {},
): Promise<TrafficSample[]> {
  const samples: TrafficSample[] = new Array<TrafficSample>(Math.max(count, 0));
  let next = 0;
  let done = 0;

  async function worker(): Promise<void> {
    while (next < count && !signal?.aborted) {
      const index = next++;
      let sample: TrafficSample;
      try {
        sample = { ok: true, server: serverKey(await send(index)) };
      } catch {
        sample = { ok: false };
      }
      samples[index] = sample;
      done += 1;
      onSample?.(sample, done);
    }
  }

  const workers = Array.from({ length: Math.min(Math.max(concurrency, 1), Math.max(count, 0)) }, worker);
  await Promise.all(workers);
  return samples.filter(Boolean);
}
