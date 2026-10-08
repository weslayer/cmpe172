import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import { api, formatMoney, formatTime } from '../api.js';

const PAGE_SIZE = 5;

export default function Slots({ user }) {
  // Filters and page live in the URL, so refresh and the back button keep them.
  const [params, setParams] = useSearchParams();
  const [equipment, setEquipment] = useState([]);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const page = Number(params.get('page') ?? 0);

  useEffect(() => {
    api('/services').then(setEquipment, (e) => setError(e.message));
  }, []);

  useEffect(() => {
    const query = new URLSearchParams(params);
    query.set('size', PAGE_SIZE);
    let current = true; // ignore a slower response for filters the user already changed
    api('/slots?' + query).then((r) => current && setResult(r), (e) => setError(e.message));
    return () => { current = false; };
  }, [params]);

  const providers = [...new Map(equipment.map((e) => [e.providerId, e.providerName]))];

  function setFilter(name, value) {
    const next = new URLSearchParams(params);
    if (value) next.set(name, value);
    else next.delete(name);
    next.delete('page');
    setParams(next);
  }

  function goToPage(p) {
    const next = new URLSearchParams(params);
    next.set('page', p);
    setParams(next);
  }

  const lastPage = result ? Math.max(0, Math.ceil(result.totalElements / PAGE_SIZE) - 1) : 0;

  return (
    <>
      <h1>Available slots</h1>
      <div className="filters">
        <label>
          Equipment
          <select value={params.get('serviceId') ?? ''} onChange={(e) => setFilter('serviceId', e.target.value)}>
            <option value="">Any</option>
            {equipment.map((e) => <option key={e.serviceId} value={e.serviceId}>{e.name}</option>)}
          </select>
        </label>
        <label>
          Provider
          <select value={params.get('providerId') ?? ''} onChange={(e) => setFilter('providerId', e.target.value)}>
            <option value="">Any</option>
            {providers.map(([id, name]) => <option key={id} value={id}>{name}</option>)}
          </select>
        </label>
        <label>
          Date
          <input type="date" value={params.get('date') ?? ''} onChange={(e) => setFilter('date', e.target.value)} />
        </label>
      </div>

      {error && <p className="error">{error}</p>}
      {result && (
        <>
          <table>
            <thead>
              <tr><th>Equipment</th><th>Provider</th><th>Period</th><th>Per day</th><th></th></tr>
            </thead>
            <tbody>
              {result.content.map((slot) => (
                <tr key={slot.slotId}>
                  <td>{slot.serviceName}<div className="muted">{slot.assetTag}</div></td>
                  <td>{slot.providerName}</td>
                  <td>{formatTime(slot.startsAt)} – {formatTime(slot.endsAt)}</td>
                  <td>{formatMoney(slot.dailyRate)}</td>
                  <td>
                    {user?.role !== 'PROVIDER' && (
                      <Link className="button" to={`/slots/${slot.slotId}/book`} state={{ slot }}>Book</Link>
                    )}
                  </td>
                </tr>
              ))}
              {result.content.length === 0 && (
                <tr><td colSpan="5" className="muted">No open slots match these filters.</td></tr>
              )}
            </tbody>
          </table>
          <div className="pager">
            <button className="ghost" disabled={page <= 0} onClick={() => goToPage(page - 1)}>‹ Prev</button>
            <span className="muted">Page {page + 1} of {lastPage + 1} · {result.totalElements} slots</span>
            <button className="ghost" disabled={page >= lastPage} onClick={() => goToPage(page + 1)}>Next ›</button>
          </div>
        </>
      )}
    </>
  );
}
