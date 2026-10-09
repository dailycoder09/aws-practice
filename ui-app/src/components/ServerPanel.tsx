import { useEffect, useId, useRef, useState } from 'react';
import { errorMessage } from '../api/client';
import { fetchServerInfo, type ServiceDefinition } from '../api/serverInfo';
import type { ServerInfo } from '../api/types';
import { useAsync } from '../hooks/useAsync';
import { useNow } from '../hooks/useNow';
import { formatTime, formatUptime, withPeriod } from '../lib/format';
import { serverKey } from '../lib/traffic';
import { CopyButton } from './CopyButton';
import { Led } from './Led';

const HEAP_HOT_PERCENT = 85;
const IPV4 = /^\d{1,3}(\.\d{1,3}){3}$/;

function ipv4Addresses(info: ServerInfo) {
  return info.host.addresses.filter(({ address }) => IPV4.test(address) && !address.startsWith('127.'));
}

function Uptime({ info }: { info: ServerInfo }) {
  const now = useNow(1000);
  const { startedAtEpochMs, uptime } = info.runtime;
  return <span className="mono">{startedAtEpochMs ? formatUptime(now - startedAtEpochMs) : uptime}</span>;
}

function HeapBar({ info }: { info: ServerInfo }) {
  const { heapUsedMb, heapMaxMb, heapUsedPercent } = info.runtime;
  const percent = Math.min(100, Math.max(0, heapUsedPercent ?? (heapMaxMb ? (heapUsedMb / heapMaxMb) * 100 : 0)));
  const hot = percent >= HEAP_HOT_PERCENT;
  return (
    <>
      <span className="bar" aria-hidden="true">
        <span className={`bar-fill ${hot ? 'bar-fill--hot' : ''}`} style={{ width: `${percent}%` }} />
      </span>
      <span className="mono">
        {heapUsedMb} of {heapMaxMb} MB
      </span>
      {hot && <span className="sr-only">(high)</span>}
    </>
  );
}

function CloudFacts({ info }: { info: ServerInfo }) {
  const { instanceId, availabilityZone } = info.cloud ?? {};
  if (!instanceId && !availabilityZone) return <li className="muted fact-note">Not on EC2</li>;
  return (
    <>
      {instanceId && (
        <li>
          <span className="fact-key">instance</span>
          <CopyButton value={instanceId} what="instance ID" className="mono" />
        </li>
      )}
      {availabilityZone && (
        <li>
          <span className="fact-key">zone</span>
          <span className="mono">{availabilityZone}</span>
        </li>
      )}
    </>
  );
}

function RequestPath({ info }: { info: ServerInfo }) {
  const { clientAddress, forwardedFor, serverAddress, serverPort } = info.request;
  return (
    <ol className="path" aria-label="How this request reached the server">
      <li>
        <span className="node-title">Connection from</span>
        <span className="mono">{clientAddress}</span>
      </li>
      {forwardedFor && (
        <li>
          <span className="node-title">Forwarded for</span>
          <span className="mono">{forwardedFor}</span>
        </li>
      )}
      <li className="is-server">
        <span className="node-title">Served by</span>
        <span className="mono">
          {serverAddress}:{serverPort}
        </span>
      </li>
    </ol>
  );
}

export function ServerPanel({ service }: { service: ServiceDefinition }) {
  const titleId = useId();
  const { data, error, loading, reload } = useAsync((signal) => fetchServerInfo(service, { signal }), [service.key]);

  // Remember which server answered last time, to notice when the load balancer picks another one.
  const previousKey = useRef<string | null>(null);
  const [differs, setDiffers] = useState(false);
  const [updatedAt, setUpdatedAt] = useState<number | null>(null);
  useEffect(() => {
    if (!data) return;
    const key = serverKey(data);
    setDiffers(previousKey.current !== null && previousKey.current !== key);
    previousKey.current = key;
    setUpdatedAt(Date.now());
  }, [data]);

  const down = Boolean(error);
  const up = Boolean(data) && !down;
  const addresses = data ? ipv4Addresses(data) : [];

  return (
    <section className="server-panel" aria-labelledby={titleId}>
      <header className="panel-head">
        <h2 id={titleId}>{service.label}</h2>
        <span className={`status ${down ? 'status--down' : ''}`} role="status">
          <Led state={down ? 'down' : up ? 'up' : 'pending'} />
          {down ? 'Unreachable' : up ? 'Up' : 'Checking'}
        </span>
        <button type="button" className="btn btn--small" onClick={reload} disabled={loading}>
          {loading && data ? 'Refreshing…' : 'Refresh'}
        </button>
      </header>

      {down && (
        <div className="problem" role="alert">
          <p>
            <strong>The {service.label.toLowerCase()} did not answer.</strong> {withPeriod(errorMessage(error))} Check
            that it is running and that its load balancer target is healthy, then refresh.
          </p>
        </div>
      )}

      {!data && loading && <p className="muted">Asking the server who it is…</p>}

      {data && !down && (
        <>
          <p className="answering">
            <Led state="up" />
            This request was answered by
          </p>
          <p className="nameplate">
            <CopyButton value={data.host.hostname} what="hostname" />
          </p>
          {differs && (
            <p className="hint" role="status">
              Answered by a different server than last time.
            </p>
          )}

          <ul className="facts">
            {addresses.map(({ interfaceName, address }) => (
              <li key={`${interfaceName}-${address}`}>
                <span className="fact-key">{interfaceName}</span>
                <CopyButton value={address} what="IP address" className="mono" />
              </li>
            ))}
            <CloudFacts info={data} />
          </ul>

          <RequestPath info={data} />

          <dl className="vitals">
            <div>
              <dt>Uptime</dt>
              <dd>
                <Uptime info={data} />
              </dd>
            </div>
            <div>
              <dt>Heap</dt>
              <dd>
                <HeapBar info={data} />
              </dd>
            </div>
            <div>
              <dt>Version</dt>
              <dd className="mono">{data.application.version}</dd>
            </div>
            <div>
              <dt>Profiles</dt>
              <dd className="mono">{data.application.profiles.length ? data.application.profiles.join(', ') : 'default'}</dd>
            </div>
          </dl>
          {updatedAt && <p className="muted updated">Updated {formatTime(updatedAt)}</p>}
        </>
      )}
    </section>
  );
}
