import { SERVICES } from '../api/serverInfo';
import { ServerPanel } from '../components/ServerPanel';
import { Totals } from '../components/Totals';
import { TrafficDistribution } from '../components/TrafficDistribution';
import { useDocumentTitle } from '../hooks/useDocumentTitle';

export function Dashboard() {
  useDocumentTitle('Dashboard');
  return (
    <>
      <header className="page-head">
        <h1>Dashboard</h1>
        <p className="muted">Which server answered, how traffic is split between servers, and what is in the catalog.</p>
      </header>

      <Totals />

      <div className="panels">
        {SERVICES.map((service) => (
          <ServerPanel key={service.key} service={service} />
        ))}
      </div>

      <TrafficDistribution />
    </>
  );
}
