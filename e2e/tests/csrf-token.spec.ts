import { test, expect } from '../support/fixtures';
import { ADMIN_STATE, employeeData } from '../support/env';

/**
 * Token anti-CSRF nelle chiamate ajax: la pagina lo espone nel tag meta csrf-token e
 * public/javascripts/script.js lo invia nell'header X-CSRF-Token in tutte le chiamate
 * ajax diverse da GET. ePAS è avviato da stack.sh con CSRF_MODE=enforce, quindi le
 * richieste senza un token valido sono rifiutate con 403.
 *
 * Le richieste usano la simulazione di inserimento di un'assenza (POST
 * /absencegroups/insert), che non salva dati.
 */
test.describe('token anti-CSRF nelle chiamate ajax', () => {
  test.use({ storageState: ADMIN_STATE });

  const cartellino = () => `/stampings/personStamping?personId=${employeeData().personId}`;

  async function csrfToken(page: import('@playwright/test').Page) {
    const token = await page.locator('meta[name="csrf-token"]').getAttribute('content');
    expect(token, 'token nel tag meta csrf-token').toBeTruthy();
    return token!;
  }

  test('il form ajax della modale invia il token nell\'header', async ({ page }) => {
    await page.goto(cartellino());
    const token = await csrfToken(page);

    const modal = page.locator('#modalGroupAbsence');
    await Promise.all([
      page.waitForResponse((r) => r.url().includes('/absencegroups/insert') && r.request().method() === 'GET'),
      page.locator('a[data-async-modal="#modalGroupAbsence"]').first().click(),
    ]);
    await expect(modal).toBeVisible();

    // la modifica della data invia il form con una POST ajax
    const to = modal.locator('input[name="to"]:not([type="hidden"])');
    const from = await to.locator('xpath=ancestor::form').locator('input[name="from"][type="hidden"]').inputValue();
    await to.fill(from);
    const [response] = await Promise.all([
      page.waitForResponse((r) => r.url().includes('/absencegroups/insert') && r.request().method() === 'POST'),
      to.dispatchEvent('change'),
    ]);
    expect(response.status()).toBe(200);
    expect(await response.request().headerValue('x-csrf-token')).toBe(token);
  });

  test('le POST ajax senza un token valido sono rifiutate', async ({ page }) => {
    test.skip(!!process.env.CSRF_MODE && process.env.CSRF_MODE !== 'enforce',
      `ePAS avviato con CSRF_MODE=${process.env.CSRF_MODE}: le richieste senza token non sono rifiutate`);
    await page.goto(cartellino());
    const token = await csrfToken(page);
    // data di un giorno del cartellino, generata dal server: non dipende dal fuso del runner
    const href = await page.locator('a[data-async-modal="#modalGroupAbsence"]').first().getAttribute('href');
    const from = new URL(href ?? '', page.url()).searchParams.get('from');
    expect(from, 'data nel link di inserimento assenza').toBeTruthy();

    const post = (headers: Record<string, string>) => page.evaluate(async ([personId, from, headers]) => {
      const response = await fetch('/absencegroups/insert', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded', ...headers },
        body: new URLSearchParams({ personId, from }).toString(),
      });
      return response.status;
    }, [String(employeeData().personId), from!, headers] as const);

    expect(await post({}), 'senza token (ePAS deve girare con CSRF_MODE=enforce)').toBe(403);
    expect(await post({ 'X-CSRF-Token': 'e2e-token-non-valido' }), 'token non valido').toBe(403);
    expect(await post({ 'X-CSRF-Token': token }), 'token della pagina').toBe(200);
  });
});
