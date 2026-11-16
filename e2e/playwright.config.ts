import { defineConfig, devices } from '@playwright/test';
import { BASE_URL, EMPLOYEE_STATE } from './support/env';

/**
 * I test girano contro un ePAS già avviato (vedi stack.sh). Condividono lo stesso
 * database, quindi sono eseguiti in sequenza e non inseriscono dati oltre a quelli
 * creati da global-setup.ts: l'unica modifica, la nota di un giorno in
 * tests/day-modal.spec.ts, è ripristinata al termine del test.
 */
export default defineConfig({
  testDir: './tests',
  globalSetup: './global-setup.ts',
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  timeout: 60_000,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : [['list']],
  use: {
    baseURL: BASE_URL,
    locale: 'it-IT',
    storageState: EMPLOYEE_STATE,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
});
