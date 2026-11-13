import type { Locator, Page } from '@playwright/test';
import { test, expect } from '../support/fixtures';
import { PERSONNEL_ADMIN_STATE, employeeData } from '../support/env';

/**
 * Finestra modale di gestione del giorno, aperta dal cartellino di un dipendente: è
 * caricata con data-async-modal e le schede sono link data-async che sostituiscono
 * solo il contenuto di #showDay (public/javascripts/script.js). La scheda "Note"
 * mostra la nota del giorno in un campo del form: è un dato inserito da un utente,
 * usato qui per verificare l'escape nel template caricato via ajax.
 */
test.describe('modale di gestione del giorno', () => {
  // lo storico del giorno richiede un ruolo sulla sede del dipendente
  test.use({ storageState: PERSONNEL_ADMIN_STATE });

  /** Payload che, se non fosse sottoposto a escape, uscirebbe dall'attributo value. */
  const PAYLOAD = `e2e "><img src=x onerror="window.__e2eXss=1"><script>window.__e2eXss=2</script>'`;

  const cartellino = () => `/stampings/personStamping?personId=${employeeData().personId}`;

  /** Link al primo giorno del cartellino che ha un person day salvato. */
  async function dayLink(page: Page): Promise<Locator> {
    const links = page.locator('a[data-async-modal="#defaultModal"][href*="persondayhistory" i]');
    await expect(links.first(), 'giorni del cartellino con lo storico').toBeVisible();
    const hrefs = await links.evaluateAll((as) => as.map((a) => a.getAttribute('href') ?? ''));
    const index = hrefs.findIndex((href) => /personDayId=\d+/.test(href));
    expect(index, 'giorno con un person day salvato').toBeGreaterThanOrEqual(0);
    return links.nth(index);
  }

  async function openDay(page: Page): Promise<Locator> {
    await page.goto(cartellino());
    const link = await dayLink(page);
    await Promise.all([
      page.waitForResponse((r) => /persondayhistory/i.test(r.url()) && r.request().method() === 'GET'),
      link.click(),
    ]);
    const modal = page.locator('#defaultModal');
    await expect(modal).toBeVisible();
    await expect(modal.locator('.modal-header h3')).toContainText('Gestione Del Giorno');
    return modal;
  }

  async function openNoteTab(page: Page, modal: Locator) {
    const [response] = await Promise.all([
      page.waitForResponse((r) => /\/persondays\/note/i.test(r.url()) && r.request().method() === 'GET'),
      modal.getByRole('link', { name: 'Note', exact: true }).click(),
    ]);
    return response;
  }

  /** Salva la nota dalla scheda "Note": il form è inviato senza ajax e torna al cartellino. */
  async function saveNote(page: Page, note: string) {
    const modal = await openDay(page);
    await openNoteTab(page, modal);
    await modal.locator('input[name="note"]').fill(note);
    await Promise.all([
      page.waitForURL(/\/stampings\/personStamping/),
      modal.getByRole('button', { name: 'Aggiorna' }).click(),
    ]);
  }

  test('le schede sono caricate via ajax senza lasciare il cartellino', async ({ page }) => {
    const modal = await openDay(page);
    // segna il contenuto attuale: dopo la risposta deve essere stato sostituito
    await modal.locator('#showDay').evaluate((e) => e.setAttribute('data-e2e-stale', ''));

    const response = await openNoteTab(page, modal);
    expect(response.status()).toBe(200);
    const headers = await response.request().allHeaders();
    expect(headers['x-requested-with']).toBe('XMLHttpRequest');

    await expect(modal.locator('[data-e2e-stale]')).toHaveCount(0);
    await expect(modal.locator('#showDay li.active')).toContainText('Note');
    await expect(modal.locator('input[name="note"]')).toBeVisible();
    await expect(modal).toBeVisible();
    await expect(page).toHaveURL(/\/stampings\/personStamping/);
  });

  test('la nota salvata è mostrata come testo e segnalata da una notifica', async ({ page }) => {
    try {
      await saveNote(page, PAYLOAD);

      // notifica del flash.success, mostrata da PNotify
      const notice = page.locator('.ui-pnotify').filter({ hasText: 'Note impostate correttamente.' });
      await expect(notice).toBeVisible();

      // la nota torna nel form caricato via ajax: deve restare il valore del campo
      const modal = await openDay(page);
      await openNoteTab(page, modal);
      await expect(modal.locator('input[name="note"]')).toHaveValue(PAYLOAD);
      await expect(modal.locator('img[src="x"]')).toHaveCount(0);
      await expect(modal.locator('script', { hasText: '__e2eXss' })).toHaveCount(0);
      expect(await page.evaluate(() => (window as { __e2eXss?: number }).__e2eXss)).toBeUndefined();
    } finally {
      await saveNote(page, '');
    }
  });
});
