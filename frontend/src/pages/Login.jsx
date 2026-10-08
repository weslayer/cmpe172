import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router';
import { api } from '../api.js';

export default function Login({ onLogin }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const { state } = useLocation();

  async function submit(event) {
    event.preventDefault();
    setError('');
    try {
      const user = await api('/auth/login', { method: 'POST', form: { email, password } });
      onLogin(user);
      const home = user.role === 'PROVIDER' ? '/provider' : '/slots';
      navigate(state?.from ?? home, { replace: true, state: { slot: state?.slot } });
    } catch (e) {
      setError(e.status === 401 ? 'Wrong email or password.' : e.message);
    }
  }

  return (
    <form className="panel narrow" onSubmit={submit}>
      <h1>Sign in</h1>
      <label>
        Email
        <input type="email" autoComplete="username" required value={email} onChange={(e) => setEmail(e.target.value)} />
      </label>
      <label>
        Password
        <input type="password" autoComplete="current-password" required value={password}
          onChange={(e) => setPassword(e.target.value)} />
      </label>
      {error && <p className="error">{error}</p>}
      <button type="submit">Sign in</button>
    </form>
  );
}
