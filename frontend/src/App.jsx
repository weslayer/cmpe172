import { useEffect, useState } from 'react';
import { Link, NavLink, Navigate, Route, Routes, useLocation } from 'react-router';
import { api } from './api.js';
import Home from './pages/Home.jsx';
import Slots from './pages/Slots.jsx';
import Book from './pages/Book.jsx';
import Login from './pages/Login.jsx';
import MyAppointments from './pages/MyAppointments.jsx';
import Provider from './pages/Provider.jsx';

export default function App() {
  // undefined while the session check is in flight, null when signed out.
  const [user, setUser] = useState(undefined);

  useEffect(() => {
    api('/auth/me').then(setUser, () => setUser(null));
  }, []);

  async function signOut() {
    await api('/auth/logout', { method: 'POST' });
    window.location.assign('/'); // full reload drops all client state along with the session
  }

  return (
    <>
      <header className="topbar">
        <Link to="/" className="brand">Equipment Rental</Link>
        <nav>
          <NavLink to="/slots">Browse</NavLink>
          {user?.role === 'CUSTOMER' && <NavLink to="/me/appointments">My rentals</NavLink>}
          {user?.role === 'PROVIDER' && <NavLink to="/provider">My equipment</NavLink>}
        </nav>
        <div className="account">
          {user ? (
            <>
              <span className="muted">{user.fullName}</span>
              <button className="ghost" onClick={signOut}>Sign out</button>
            </>
          ) : (
            user === null && <Link to="/login">Sign in</Link>
          )}
        </div>
      </header>

      <main>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/slots" element={<Slots user={user} />} />
          <Route path="/slots/:slotId/book" element={
            <RequireRole user={user} role="CUSTOMER"><Book user={user} /></RequireRole>
          } />
          <Route path="/login" element={<Login onLogin={setUser} />} />
          <Route path="/me/appointments" element={
            <RequireRole user={user} role="CUSTOMER"><MyAppointments /></RequireRole>
          } />
          <Route path="/provider" element={
            <RequireRole user={user} role="PROVIDER"><Provider user={user} /></RequireRole>
          } />
          <Route path="*" element={<p>Page not found. <Link to="/">Go home</Link></p>} />
        </Routes>
      </main>
    </>
  );
}

// Convenience only: the API enforces roles with 401/403 regardless of what the UI shows.
function RequireRole({ user, role, children }) {
  const location = useLocation();
  if (user === undefined) {
    return null;
  }
  if (user === null) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search, slot: location.state?.slot }} />;
  }
  if (user.role !== role) {
    return <p className="error">This page is only for {role.toLowerCase()} accounts.</p>;
  }
  return children;
}
