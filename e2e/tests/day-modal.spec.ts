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

  /** Link allo storico dei giorni del cartellino. */
  const dayLinks = (page: Page) =>
    page.locator('a[data-async-modal="#defaultModal"][href*="persondayhistory" i]');

  /** Id del primo giorno del cartellino che ha un person day salvato. */
  async function firstPersonDayId(page: Page): Promise<number> {
    await page.goto(cartellino());
    const links = dayLinks(page);
    await expect(links.first(), 'giorni del cartellino con lo storico').toBeVisible();
    const hrefs = await links.evaluateAll((as) => as.map((a) => a.getAttribute('href') ?? ''));
    const id = hrefs.map((href) => href.match(/personDayId=(\d+)/)?.[1]).find(Boolean);
    expect(id, 'giorno con un person day salvato').toBeTruthy();
    return Number(id);
  }

  /** Apre la modale del giorno indicato dal cartellino. */
  async function openDay(page: Page, personDayId: number): Promise<Locator> {
    await page.goto(cartellino());
    const links = dayLinks(page);
    await expect(links.first(), 'giorni del cartellino con lo storico').toBeVisible();
    const hrefs = await links.evaluateAll((as) => as.map((a) => a.getAttribute('href') ?? ''));
    const pattern = new RegExp(`personDayId=${personDayId}(?:&|$)`);
    const index = hrefs.findIndex((href) => pattern.test(href));
    expect(index, `giorno ${personDayId} nel cartellino`).toBeGreaterThanOrEqual(0);
    await Promise.all([
      page.waitForResponse((r) => /persondayhistory/i.test(r.url()) && r.request().method() === 'GET'),
      links.nth(index).click(),
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
  async function saveNote(page: Page, personDayId: number, note: string) {
    const modal = await openDay(page, personDayId);
    await openNoteTab(page, modal);
    await modal.locator('input[name="note"]').fill(note);
    await Promise.all([
      page.waitForURL(/\/stampings\/personStamping/),
      modal.getByRole('button', { name: 'Aggiorna' }).click(),
    ]);
  }

  /**
   * Form della nota letto senza passare dall'interfaccia: valore attuale e token del form.
   * Serve a ripristinare la nota anche se il test si è interrotto a metà.
   */
  async function noteForm(page: Page, personDayId: number) {
    const response = await page.request.get('/persondays/note', { params: { personDayId } });
    expect(response.ok(), `form della nota del giorno ${personDayId}`).toBeTruthy();
    const html = await response.text();
    return page.evaluate((html) => {
      const doc = new DOMParser().parseFromString(html, 'text/html');
      const value = (name: string) =>
        doc.querySelector<HTMLInputElement>(`input[name="${name}"]`)?.value;
      return { note: value('note') ?? '', token: value('authenticityToken') };
    }, html);
  }

  /** Riporta la nota del giorno al valore indicato con una POST diretta al form. */
  async function restoreNote(page: Page, personDayId: number, note: string) {
    const { token } = await noteForm(page, personDayId);
    const response = await page.request.post('/persondays/note', {
      form: {
        ...(token ? { authenticityToken: token } : {}),
        personDayId: String(personDayId),
        confirmed: 'true',
        note,
      },
    });
    expect(response.ok() && !response.url().includes('/login'),
      `ripristino della nota del giorno ${personDayId}`).toBeTruthy();
    expect((await noteForm(page, personDayId)).note).toBe(note);
  }

  test('le schede sono caricate via ajax senza lasciare il cartellino', async ({ page }) => {
    const modal = await openDay(page, await firstPersonDayId(page));
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
    const personDayId = await firstPersonDayId(page);
    const original = (await noteForm(page, personDayId)).note;
    try {
      await saveNote(page, personDayId, PAYLOAD);

      // notifica del flash.success, mostrata da PNotify
      const notice = page.locator('.ui-pnotify').filter({ hasText: 'Note impostate correttamente.' });
      await expect(notice).toBeVisible();

      // la nota torna nel form caricato via ajax: deve restare il valore del campo
      const modal = await openDay(page, personDayId);
      await openNoteTab(page, modal);
      await expect(modal.locator('input[name="note"]')).toHaveValue(PAYLOAD);
      await expect(modal.locator('img[src="x"]')).toHaveCount(0);
      await expect(modal.locator('script', { hasText: '__e2eXss' })).toHaveCount(0);
      expect(await page.evaluate(() => (window as { __e2eXss?: number }).__e2eXss)).toBeUndefined();
    } finally {
      // ripristino senza interfaccia: un errore nel test non si ripete nel cleanup
      await restoreNote(page, personDayId, original);
    }
  });
});
