import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { api, formatMoney } from '../api.js';

export default function Home() {
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api('/home').then(setSummary, (e) => setError(e.message));
  }, []);

  if (error) return <p className="error">{error}</p>;
  if (!summary) return <p className="muted">Loading…</p>;

  return (
    <>
      <h1>{summary.application}</h1>
      <div className="tiles">
        <div className="tile"><strong>{summary.equipmentCount}</strong>Equipment</div>
        <div className="tile"><strong>{summary.providerCount}</strong>Providers</div>
        <div className="tile"><strong>{summary.availableSlotCount}</strong>Open slots</div>
      </div>

      <h2>Featured equipment</h2>
      <ul className="cards">
        {summary.featuredEquipment.map((item) => (
          <li key={item.serviceId} className="card">
            <strong>{item.name}</strong>
            <span className="muted">{item.category} · {item.providerName}</span>
            <span>{formatMoney(item.dailyRate)}/day</span>
            <Link to={`/slots?serviceId=${item.serviceId}`}>See open slots</Link>
          </li>
        ))}
      </ul>
    </>
  );
}
