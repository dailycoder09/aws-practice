import { useEffect, useId, useRef, useState } from 'react';
import { fetchServerInfo, SERVICES, type ServiceKey } from '../api/serverInfo';
import { formatNumber, formatPercent } from '../lib/format';
import { runTraffic, tallyTraffic, type TrafficSample } from '../lib/traffic';

const MIN_REQUESTS = 1;
const MAX_REQUESTS = 100;
const DEFAULT_REQUESTS = 20;
const MAX_IN_FLIGHT = 5;

interface Progress {
  samples: TrafficSample[];
  total: number;
}

type Results = Partial<Record<ServiceKey, Progress>>;

function parseCount(raw: string): number | null {
  if (!/^\d+$/.test(raw.trim())) return null;
  const value = Number(raw);
  return value >= MIN_REQUESTS && value <= MAX_REQUESTS ? value : null;
}

function ServiceTally({ label, progress, running }: { label: string; progress?: Progress; running: boolean }) {
  const titleId = useId();
  const tally = tallyTraffic(progress?.samples ?? []);
  const done = tally.total;
  const total = progress?.total ?? 0;
  const inProgress = running && progress !== undefined && done < total;

  return (
    <div className="tally" role="group" aria-labelledby={titleId}>
      <h3 id={titleId}>{label}</h3>
      {!progress ? (
        <p className="muted">{running ? 'Waiting for its turn…' : 'Nothing sent yet.'}</p>
      ) : (
        <>
          {tally.rows.length === 0 && <p className="muted">No successful responses yet.</p>}
          <ul className="dist">
            {tally.rows.map((row) => (
              <li key={row.server}>
                <span className="dist-name mono">{row.server}</span>
                <span className="dist-bar" aria-hidden="true">
                  <span className="dist-fill" style={{ width: `${row.percent}%` }} />
                </span>
                <span className="dist-count mono">{formatNumber(row.count)}</span>
                <span className="dist-percent mono">{formatPercent(row.percent)}</span>
              </li>
            ))}
          </ul>
          <p className="tally-foot">
            <span className={tally.failures > 0 ? 'failures' : 'muted'}>
              {tally.failures} {tally.failures === 1 ? 'failure' : 'failures'}
            </span>
            <span className="muted mono">
              {inProgress ? `${done} of ${total} sent` : `${done} sent`}
            </span>
          </p>
          {inProgress && (
            <span
              className="bar bar--wide"
              role="progressbar"
              aria-label={`${label} requests sent`}
              aria-valuemin={0}
              aria-valuemax={total}
              aria-valuenow={done}
            >
              <span className="bar-fill bar-fill--sky" style={{ width: `${(done / total) * 100}%` }} />
            </span>
          )}
        </>
      )}
    </div>
  );
}

export function TrafficDistribution() {
  const inputId = useId();
  const [raw, setRaw] = useState(String(DEFAULT_REQUESTS));
  const [running, setRunning] = useState(false);
  const [results, setResults] = useState<Results>({});
  const controller = useRef<AbortController | null>(null);

  // Stop sending if the page is left mid-run.
  useEffect(() => () => controller.current?.abort(), []);

  const count = parseCount(raw);

  async function send() {
    if (count === null || running) return;
    const abort = new AbortController();
    controller.current = abort;
    setRunning(true);
    setResults({});
    // One service at a time keeps the total in flight at or below the limit.
    for (const service of SERVICES) {
      if (abort.signal.aborted) break;
      setResults((current) => ({ ...current, [service.key]: { samples: [], total: count } }));
      const samples = await runTraffic(
        count,
        (index) =>
          fetchServerInfo(service, { cacheBuster: `${Date.now()}-${index}`, signal: abort.signal }),
        {
          concurrency: MAX_IN_FLIGHT,
          signal: abort.signal,
          onSample: (sample) => {
            if (abort.signal.aborted) return;
            setResults((current) => {
              const existing = current[service.key] ?? { samples: [], total: count };
              return { ...current, [service.key]: { ...existing, samples: [...existing.samples, sample] } };
            });
          },
        },
      );
      if (abort.signal.aborted) break;
      setResults((current) => ({ ...current, [service.key]: { samples, total: count } }));
    }
    if (!abort.signal.aborted) setRunning(false);
  }

  return (
    <section className="traffic" aria-labelledby="traffic-title">
      <h2 id="traffic-title">Traffic distribution</h2>
      <p className="muted lede">
        Sends requests to each service&rsquo;s server-info endpoint and counts which server answered. Behind the load
        balancer with two servers, expect a split. On one machine, all of it lands on one host.
      </p>

      <div className="traffic-controls">
        <label htmlFor={inputId}>Requests per service</label>
        <input
          id={inputId}
          className="field field--number mono"
          type="number"
          inputMode="numeric"
          min={MIN_REQUESTS}
          max={MAX_REQUESTS}
          step={1}
          value={raw}
          disabled={running}
          aria-invalid={count === null}
          onChange={(event) => setRaw(event.target.value)}
          onBlur={() => {
            const parsed = Number(raw);
            if (raw.trim() === '' || Number.isNaN(parsed)) setRaw(String(DEFAULT_REQUESTS));
            else setRaw(String(Math.min(MAX_REQUESTS, Math.max(MIN_REQUESTS, Math.round(parsed)))));
          }}
        />
        <button type="button" className="btn btn--primary" onClick={send} disabled={running || count === null}>
          {running ? 'Sending…' : count === null ? 'Send requests' : `Send ${count} ${count === 1 ? 'request' : 'requests'}`}
        </button>
        {count === null && (
          <span className="field-error" role="alert">
            Enter a whole number from {MIN_REQUESTS} to {MAX_REQUESTS}.
          </span>
        )}
      </div>

      <div className="tallies">
        {SERVICES.map((service) => (
          <ServiceTally key={service.key} label={service.label} progress={results[service.key]} running={running} />
        ))}
      </div>
    </section>
  );
}
