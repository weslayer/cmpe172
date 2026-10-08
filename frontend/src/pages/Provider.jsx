import { useEffect, useState } from 'react';
import { api, formatTime } from '../api.js';

export default function Provider({ user }) {
  const [equipment, setEquipment] = useState([]);
  const [slots, setSlots] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [form, setForm] = useState({ serviceId: '', startsAt: '', endsAt: '' });
  const [error, setError] = useState('');

  function load() {
    // Open, unbooked, future slots are the removable ones; booked ones show up under bookings.
    api(`/slots?providerId=${user.providerId}&size=100`).then((page) => setSlots(page.content), (e) => setError(e.message));
    api('/provider/appointments').then(setBookings, (e) => setError(e.message));
  }

  useEffect(() => {
    api(`/services?providerId=${user.providerId}`).then(setEquipment, (e) => setError(e.message));
    load();
  }, [user.providerId]);

  async function run(request) {
    setError('');
    try {
      await request();
      load();
    } catch (e) {
      setError(e.message);
    }
  }

  function createSlot(event) {
    event.preventDefault();
    run(async () => {
      // datetime-local has no zone; toISOString sends the browser's local time as UTC.
      await api('/provider/slots', {
        method: 'POST',
        json: {
          serviceId: Number(form.serviceId),
          startsAt: new Date(form.startsAt).toISOString(),
          endsAt: new Date(form.endsAt).toISOString(),
        },
      });
      setForm({ ...form, startsAt: '', endsAt: '' });
    });
  }

  const set = (name) => (e) => setForm({ ...form, [name]: e.target.value });

  return (
    <>
      <h1>My equipment</h1>
      {error && <p className="error">{error}</p>}

      <h2>Add an availability slot</h2>
      <form className="filters" onSubmit={createSlot}>
        <label>
          Equipment
          <select required value={form.serviceId} onChange={set('serviceId')}>
            <option value="">Choose…</option>
            {equipment.map((e) => <option key={e.serviceId} value={e.serviceId}>{e.name}</option>)}
          </select>
        </label>
        <label>
          Starts
          <input type="datetime-local" required value={form.startsAt} onChange={set('startsAt')} />
        </label>
        <label>
          Ends
          <input type="datetime-local" required value={form.endsAt} onChange={set('endsAt')} />
        </label>
        <button type="submit">Add slot</button>
      </form>

      <h2>Open slots</h2>
      <table>
        <thead><tr><th>Equipment</th><th>Period</th><th></th></tr></thead>
        <tbody>
          {slots.map((s) => (
            <tr key={s.slotId}>
              <td>{s.serviceName}<div className="muted">{s.assetTag}</div></td>
              <td>{formatTime(s.startsAt)} – {formatTime(s.endsAt)}</td>
              <td>
                <button className="ghost" onClick={() => run(() => api(`/provider/slots/${s.slotId}`, { method: 'DELETE' }))}>
                  Remove
                </button>
              </td>
            </tr>
          ))}
          {slots.length === 0 && <tr><td colSpan="3" className="muted">No open slots.</td></tr>}
        </tbody>
      </table>

      <h2>Bookings with me</h2>
      <table>
        <thead><tr><th>#</th><th>Equipment</th><th>Period</th><th>Customer</th><th>Status</th></tr></thead>
        <tbody>
          {bookings.map((a) => (
            <tr key={a.appointmentId}>
              <td>{a.appointmentId}</td>
              <td>{a.serviceName}</td>
              <td>{formatTime(a.startsAt)} – {formatTime(a.endsAt)}</td>
              <td>{a.customerName}<div className="muted">{a.customerEmail}</div></td>
              <td><span className={`status ${a.status.toLowerCase()}`}>{a.status}</span></td>
            </tr>
          ))}
          {bookings.length === 0 && <tr><td colSpan="5" className="muted">No bookings yet.</td></tr>}
        </tbody>
      </table>
    </>
  );
}
