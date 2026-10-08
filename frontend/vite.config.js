import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The dev server proxies the API, so the session and CSRF cookies stay same-origin (no CORS).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
});
