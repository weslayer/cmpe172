import { useState } from 'react';
import { Link, useLocation, useParams } from 'react-router';
import { api, formatMoney, formatTime } from '../api.js';

export default function Book({ user }) {
  const { slotId } = useParams();
  // The slot comes from the row clicked on the Slots page; there is no single-slot endpoint.
  const slot = useLocation().state?.slot;
  const [booking, setBooking] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function confirm(event) {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      setBooking(await api('/appointments', { method: 'POST', json: { slotId: Number(slotId) } }));
    } catch (e) {
      setError(e.status === 409 ? `${e.message}. Someone may have just booked it.` : e.message);
    } finally {
      setBusy(false);
    }
  }

  if (booking) {
    return (
      <section className="panel">
        <h1>Booking confirmed</h1>
        <dl>
          <dt>Reference</dt><dd>#{booking.appointmentId}</dd>
          <dt>Equipment</dt><dd>{booking.serviceName} ({booking.assetTag})</dd>
          <dt>Period</dt><dd>{formatTime(booking.startsAt)} – {formatTime(booking.endsAt)}</dd>
          <dt>Status</dt><dd>{booking.status}</dd>
        </dl>
        <Link className="button" to="/me/appointments">View my rentals</Link>
      </section>
    );
  }

  if (!slot) {
    return <p>Pick a slot from the <Link to="/slots">available slots</Link> to book it.</p>;
  }

  return (
    <form className="panel" onSubmit={confirm}>
      <h1>Book equipment</h1>
      <dl>
        <dt>Equipment</dt><dd>{slot.serviceName} ({slot.assetTag})</dd>
        <dt>Provider</dt><dd>{slot.providerName}</dd>
        <dt>Period</dt><dd>{formatTime(slot.startsAt)} – {formatTime(slot.endsAt)}</dd>
        <dt>Rate</dt><dd>{formatMoney(slot.dailyRate)}/day</dd>
        <dt>Renter</dt><dd>{user.fullName} ({user.email})</dd>
      </dl>
      {error && <p className="error">{error}</p>}
      <button type="submit" disabled={busy}>{busy ? 'Booking…' : 'Confirm booking'}</button>
    </form>
  );
}
