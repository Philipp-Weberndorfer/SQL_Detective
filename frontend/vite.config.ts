/// <reference types="vitest" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Im Entwicklungsmodus (npm run dev) leitet Vite /api an das lokal laufende
// Backend weiter. Dadurch laufen Frontend und API ueber denselben Ursprung,
// und Sitzungs- und CSRF-Cookie funktionieren ohne CORS-Konfiguration.
// Im Container uebernimmt nginx diese Aufgabe (nginx.conf).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
  },
});
