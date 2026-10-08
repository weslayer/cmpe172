import { useEffect, useState } from 'react';
import { api, formatTime } from '../api.js';

export default function MyAppointments() {
  const [appointments, setAppointments] = useState(null);
  const [error, setError] = useState('');

  function load() {
    api('/appointments').then(setAppointments, (e) => setError(e.message));
  }

  useEffect(load, []);

  async function cancel(id) {
    setError('');
    try {
      await api(`/appointments/${id}/cancel`, { method: 'POST' });
      load();
    } catch (e) {
      setError(e.message);
    }
  }

  if (!appointments) return error ? <p className="error">{error}</p> : <p className="muted">Loading…</p>;

  // BOOKED means still upcoming; CANCELLED and COMPLETED (past) are history.
  const upcoming = appointments.filter((a) => a.status === 'BOOKED');
  const history = appointments.filter((a) => a.status !== 'BOOKED').reverse();

  return (
    <>
      <h1>My rentals</h1>
      {error && <p className="error">{error}</p>}
      <h2>Upcoming</h2>
      <AppointmentTable rows={upcoming} empty="No upcoming rentals."
        action={(a) => <button className="ghost" onClick={() => cancel(a.appointmentId)}>Cancel</button>} />
      <h2>History</h2>
      <AppointmentTable rows={history} empty="No past or cancelled rentals." />
    </>
  );
}

function AppointmentTable({ rows, empty, action }) {
  return (
    <table>
      <thead>
        <tr><th>#</th><th>Equipment</th><th>Provider</th><th>Period</th><th>Status</th>{action && <th></th>}</tr>
      </thead>
      <tbody>
        {rows.map((a) => (
          <tr key={a.appointmentId}>
            <td>{a.appointmentId}</td>
            <td>{a.serviceName}<div className="muted">{a.assetTag}</div></td>
            <td>{a.providerName}</td>
            <td>{formatTime(a.startsAt)} – {formatTime(a.endsAt)}</td>
            <td><span className={`status ${a.status.toLowerCase()}`}>{a.status}</span></td>
            {action && <td>{action(a)}</td>}
          </tr>
        ))}
        {rows.length === 0 && <tr><td colSpan="6" className="muted">{empty}</td></tr>}
      </tbody>
    </table>
  );
}
