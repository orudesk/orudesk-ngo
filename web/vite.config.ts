/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// As in the core's app: /api goes to the NGO edition's API, so the page and the API share one origin.
const apiUrl = process.env.ORUDESK_API_URL ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: { '/api': apiUrl },
    fs: { allow: ['..', '../../orudesk/web'] }, // the core's packages are outside this folder
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['@orudesk/web-core/test-setup'],
  },
});
