import { test, expect } from '../support/fixtures';

// file senza contenuto fisso, per i quali non si può indicare un hash
const WITHOUT_INTEGRITY = ['www.gstatic.com', 'fonts.googleapis.com'];

test.describe('Subresource Integrity', () => {

  test('script e fogli di stile dei CDN hanno un hash che il browser accetta', async ({ page }) => {
    const failures: string[] = [];
    page.on('console', (message) => {
      // es. "Failed to find a valid digest in the 'integrity' attribute for resource ..."
      if (message.type() === 'error' && /integrity/i.test(message.text())) {
        failures.push(message.text());
      }
    });
    page.on('requestfailed', (request) => {
      if (new URL(request.url()).host !== new URL(page.url()).host) {
        failures.push(`${request.url()}: ${request.failure()?.errorText}`);
      }
    });

    await page.goto('/');
    await expect(page).toHaveTitle(/Timbrature/);
    await page.waitForLoadState('networkidle');

    const external = await page.evaluate(() =>
      [...document.querySelectorAll<HTMLScriptElement | HTMLLinkElement>(
        'script[src], link[rel="stylesheet"][href]')]
        .map((el) => ({
          url: (el as HTMLScriptElement).src || (el as HTMLLinkElement).href,
          integrity: el.integrity,
          crossOrigin: el.crossOrigin,
        }))
        .filter((el) => new URL(el.url).host !== location.host));
    const checked = external.filter((el) => !WITHOUT_INTEGRITY.includes(new URL(el.url).host));

    expect(checked.length, 'script e fogli di stile caricati dai CDN').toBeGreaterThan(10);
    for (const el of checked) {
      expect(el.integrity, `integrity di ${el.url}`).toMatch(/^sha(384|512)-/);
      expect(el.crossOrigin, `crossorigin di ${el.url}`).toBe('anonymous');
    }
    expect(failures, 'file dei CDN rifiutati o non caricati').toEqual([]);

    // le librerie caricate con l'hash sono state eseguite
    const loaded = await page.evaluate(() => ({
      jquery: typeof (window as any).jQuery,
      select2: typeof (window as any).jQuery?.fn?.select2,
      dataTable: typeof (window as any).jQuery?.fn?.dataTable,
      datepicker: typeof (window as any).jQuery?.fn?.datepicker,
      moment: typeof (window as any).moment,
      bootbox: typeof (window as any).bootbox,
    }));
    for (const [name, type] of Object.entries(loaded)) {
      expect(type, `libreria ${name}`).not.toBe('undefined');
    }
  });
});
