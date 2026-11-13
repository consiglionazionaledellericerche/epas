import type { Page, Response } from '@playwright/test';
import { test, expect } from '../support/fixtures';
import { ADMIN_STATE, employeeData } from '../support/env';

/**
 * Finestra modale di inserimento di un'assenza dal cartellino di un dipendente: è
 * caricata e aggiornata con le chiamate ajax di public/javascripts/script.js
 * (data-async-modal, data-async, form auto-submit). I test si fermano alla
 * simulazione: nessuna assenza viene inserita.
 */
test.describe('modale di inserimento assenza', () => {
  test.use({ storageState: ADMIN_STATE });

  const insertUrl = (response: Response, method: string) =>
    response.url().includes('/absencegroups/insert') && response.request().method() === method;

  async function openModal(page: Page) {
    await page.goto(`/stampings/personStamping?personId=${employeeData().personId}`);
    const [response] = await Promise.all([
      page.waitForResponse((r) => insertUrl(r, 'GET')),
      page.locator('a[data-async-modal="#modalGroupAbsence"]').first().click(),
    ]);
    return response;
  }

  test('si apre con una chiamata ajax autenticata', async ({ page }) => {
    const response = await openModal(page);
    expect(response.status()).toBe(200);

    const headers = await response.request().allHeaders();
    expect(headers['x-requested-with']).toBe('XMLHttpRequest');
    expect(headers['cookie']).toContain('PLAY_SESSION=');

    const modal = page.locator('#modalGroupAbsence');
    await expect(modal).toBeVisible();
    await expect(modal.getByRole('heading')).toContainText('Nuovo codice in data');
    await expect(modal.getByRole('heading')).toContainText('Rossi Mario');
    // il form con la simulazione è nella risposta ajax
    await expect(modal.locator('input[name="to"]:not([type="hidden"])')).toBeVisible();
    await expect(modal.getByRole('button', { name: 'Inserisci' })).toBeVisible();
  });

  test('il cambio di scheda aggiorna solo il contenuto della modale', async ({ page }) => {
    await openModal(page);
    const modal = page.locator('#modalGroupAbsence');
    const tab = modal.getByRole('link', { name: 'Ferie Permessi e Riposi' });

    const [response] = await Promise.all([
      page.waitForResponse((r) => insertUrl(r, 'GET') && r.url().includes('categoryTab.id')),
      tab.click(),
    ]);
    expect(response.status()).toBe(200);

    await expect(modal.locator('li.active')).toContainText('Ferie Permessi e Riposi');
    await expect(modal).toBeVisible();
    await expect(page).toHaveURL(/\/stampings\/personStamping/);
  });

  test('la modifica della data invia il form via ajax e mostra la simulazione', async ({ page }) => {
    await openModal(page);
    const modal = page.locator('#modalGroupAbsence');
    const to = modal.locator('input[name="to"]:not([type="hidden"])');
    // la data di partenza è nel campo nascosto "from", sempre valorizzato: "to"
    // può essere vuoto
    await expect(to, 'campo "to" modificabile').toBeEditable();
    const from = await to.locator('xpath=ancestor::form').locator('input[name="from"][type="hidden"]').inputValue();
    expect(from, 'data "from" nel formato dd/MM/yyyy').toMatch(/^\d{2}\/\d{2}\/\d{4}$/);
    const [day, month, year] = from.split('/').map(Number);
    const nextDay = new Date(year, month - 1, day + 1);
    const newTo = [nextDay.getDate(), nextDay.getMonth() + 1, nextDay.getFullYear()]
      .map((n) => String(n).padStart(2, '0')).join('/');

    // segna il contenuto attuale: dopo la risposta deve essere stato sostituito
    await modal.locator('#absenceData > *').first().evaluate((e) => e.setAttribute('data-e2e-stale', ''));
    await to.fill(newTo);
    const [response] = await Promise.all([
      page.waitForResponse((r) => insertUrl(r, 'POST')),
      to.dispatchEvent('change'),
    ]);
    expect(response.status()).toBe(200);
    expect(response.request().postData()).toContain('authenticityToken=');

    // la risposta sostituisce il contenuto della modale, senza passare dal login
    await expect(modal).toBeVisible();
    await expect(modal.locator('[data-e2e-stale]')).toHaveCount(0);
    await expect(modal.locator('input[name="to"]:not([type="hidden"])')).toHaveValue(newTo);
    await expect(modal.getByRole('button', { name: 'Inserisci' })).toBeVisible();
    await expect(page).toHaveURL(/\/stampings\/personStamping/);
  });
});
