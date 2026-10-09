import { BrowserRouter, Link, NavLink, Route, Routes } from 'react-router-dom';
import { useDocumentTitle } from './hooks/useDocumentTitle';
import { Dashboard } from './pages/Dashboard';
import { Inventory } from './pages/Inventory';
import { ProductDetail } from './pages/ProductDetail';
import { Products } from './pages/Products';

function NotFound() {
  useDocumentTitle('Page not found');
  return (
    <header className="page-head">
      <h1>Page not found</h1>
      <p className="muted">
        There is nothing at this address. Go back to the <Link to="/">dashboard</Link>.
      </p>
    </header>
  );
}

function Nav() {
  return (
    <header className="nav">
      <div className="shell nav-inner">
        <Link className="brand" to="/">
          Microservices demo
        </Link>
        <nav aria-label="Main">
          <NavLink to="/" end>
            Dashboard
          </NavLink>
          <NavLink to="/products">Products</NavLink>
          <NavLink to="/inventory">Inventory</NavLink>
        </nav>
      </div>
    </header>
  );
}

/** Everything except the router, so tests can supply their own. */
export function AppRoutes() {
  return (
    <>
      <a className="skip-link" href="#main">
        Skip to content
      </a>
      <Nav />
      <main id="main" className="shell">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/products" element={<Products />} />
          <Route path="/products/:id" element={<ProductDetail />} />
          <Route path="/inventory" element={<Inventory />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>
    </>
  );
}

export default function App() {
  return (
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <AppRoutes />
    </BrowserRouter>
  );
}
