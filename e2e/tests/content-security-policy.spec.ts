import { test, expect } from '../support/fixtures';

const REPORT_ONLY = 'content-security-policy-report-only';
const ENFORCE = 'content-security-policy';

test.describe('Content Security Policy', () => {

  test('la pagina è inviata in sola segnalazione e continua a funzionare', async ({ page }) => {
    const response = await page.goto('/');
    await expect(page).toHaveURL(/\/stampings\/stampings/);
    await expect(page).toHaveTitle(/Timbrature/);

    const headers = response!.headers();
    expect(headers[ENFORCE], 'nessuna policy bloccante di default').toBeUndefined();
    const policy = headers[REPORT_ONLY];
    expect(policy, 'header Content-Security-Policy-Report-Only').toBeDefined();
    expect(policy).toContain("default-src 'self'");
    expect(policy).toMatch(/script-src 'self' https:\/\//);
    expect(policy).toMatch(/report-uri \/csp\/report$/);

    // in sola segnalazione gli script dei CDN e quelli della pagina sono eseguiti
    expect(await page.evaluate(() => typeof (window as any).jQuery)).toBe('function');
  });

  test('il browser segnala le violazioni a /csp/report', async ({ page }) => {
    await page.goto('/');
    // la pagina può segnalare anche altre violazioni: si aspetta quella dello script di prova
    const report = page.waitForResponse((r) => r.url().endsWith('/csp/report')
        && (r.request().postData() ?? '').includes('csp-test.invalid'));
    // uno script da un host non previsto dalla policy: non viene bloccato ma è segnalato
    await page.evaluate(() => {
      const script = document.createElement('script');
      script.src = 'https://csp-test.invalid/script.js';
      document.head.appendChild(script);
    });
    const response = await report;
    expect(response.status()).toBe(204);
    const sent = response.request().postDataJSON()['csp-report'];
    expect(sent['blocked-uri']).toContain('csp-test.invalid');
    expect(sent['effective-directive']).toBe('script-src-elem');
  });

  test.describe('utente non autenticato', () => {
    test.use({ storageState: { cookies: [], origins: [] } });

    test('la policy è presente anche sulla pagina di login', async ({ page }) => {
      const response = await page.goto('/login');
      expect(response!.headers()[REPORT_ONLY]).toContain("default-src 'self'");
    });
  });
});
