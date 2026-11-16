import type { Page } from '@playwright/test';
import { test, expect } from '../support/fixtures';
import { ADMIN_STATE, PERSONNEL_ADMIN_STATE, employeeData } from '../support/env';

/**
 * Azioni che modificano dati annotate con PostOnly (#651): CsrfProtection rifiuta le
 * GET, che non portano il token anti-CSRF e potrebbero arrivare da un link di un altro
 * sito. Nelle view i link verso queste azioni hanno data-method="post" e
 * public/javascripts/script.js li invia come form POST con il token.
 *
 * ePAS è avviato da stack.sh con CSRF_MODE=enforce: con off o report le GET non sono
 * rifiutate e i test sono saltati.
 */
test.describe('azioni che modificano dati solo in POST', () => {
  test.skip(!!process.env.CSRF_MODE && process.env.CSRF_MODE !== 'enforce',
    `ePAS avviato con CSRF_MODE=${process.env.CSRF_MODE}: le GET non sono rifiutate`);

  /** Token anti-CSRF della pagina corrente, dal tag meta csrf-token. */
  async function csrfToken(page: Page) {
    const token = await page.locator('meta[name="csrf-token"]').getAttribute('content');
    expect(token, 'token nel tag meta csrf-token').toBeTruthy();
    return token!;
  }

  /** Richiesta dalla pagina, con i cookie della sessione: stato e testo della risposta. */
  function send(page: Page, method: 'GET' | 'POST', url: string, token?: string) {
    return page.evaluate(async ([method, url, token]) => {
      const response = await fetch(url, {
        method,
        headers: token ? { 'X-CSRF-Token': token } : {},
        redirect: 'manual',
      });
      return { status: response.status };
    }, [method, url, token] as const);
  }

  test.describe('verifica sulle azioni', () => {
    // l'utente DEVELOPER ha accesso a tutte le azioni: un 403 è quello di CsrfProtection
    // (la pagina di errore è la stessa dei permessi, il motivo è solo nel log)
    test.use({ storageState: ADMIN_STATE });

    /**
     * Azioni annotate per ultime, con l'id di un oggetto che non esiste: superata la
     * verifica rispondono 404 senza modificare nulla.
     */
    const ACTIONS = [
      '/stampings/delete?id=0',
      '/stampings/updateEmployee?stampingId=0',
      '/users/removeRole?uroId=0',
      '/institutes/delete?id=0',
      '/configurations/removeAttachment?attachmentId=0',
      '/contractualclauses/delete?contractualClauseId=0',
      '/contractualreferences/delete?contractualReferenceId=0',
      '/contractualreferences/deleteFile?contractualReferenceId=0',
      '/badgesystems/joinPersonNumbers?badgeSystemId=0',
      '/absencegroups/syncCertificationsAbsences?personId=0&year=2025',
      '/administration/fixPreviousContract?id=0',
    ];

    for (const url of ACTIONS) {
      test(`GET rifiutata e POST con il token accettata: ${url}`, async ({ page }) => {
        await page.goto('/');
        const token = await csrfToken(page);

        const get = await send(page, 'GET', url);
        expect(get.status, 'GET senza token').toBe(403);

        const post = await send(page, 'POST', url, token);
        expect(post.status, 'POST con il token: oggetto inesistente').toBe(404);
      });
    }

    /**
     * Azioni che in POST modificherebbero dati anche senza un id: solo la GET, che è
     * rifiutata da CsrfProtection prima di eseguire l'azione.
     */
    for (const url of [
      '/administration/fixContractsWithWrongPreviousContract',
      '/checkgreenpasses/save',
      '/workingtimes/save',
    ]) {
      test(`GET rifiutata: ${url}`, async ({ page }) => {
        await page.goto('/');
        const get = await send(page, 'GET', url);
        expect(get.status, 'GET senza token').toBe(403);
      });
    }
  });

  test.describe('eliminazione di una timbratura dal cartellino', () => {
    test.use({ storageState: PERSONNEL_ADMIN_STATE });

    const cartellino = () => `/stampings/personStamping?personId=${employeeData().personId}`;
    /** Orario della timbratura di prova, per riconoscerla nel cartellino. */
    const TIME = '07:13';

    /** Id delle timbrature del cartellino, dai link di modifica. */
    async function stampingIds(page: Page): Promise<number[]> {
      await page.goto(cartellino());
      const hrefs = await page.locator('a[data-async-modal="#modalStamping"][href*="stampings/edit" i]')
        .evaluateAll((as) => as.map((a) => a.getAttribute('href') ?? ''));
      return hrefs.map((href) => Number(href.match(/stampingId=(\d+)/)?.[1])).filter(Boolean);
    }

    /** Elimina con una POST la timbratura indicata. */
    async function deleteStamping(page: Page, stampingId: number) {
      await page.goto(cartellino());
      const response = await page.request.post('/stampings/delete', {
        form: { authenticityToken: await csrfToken(page), id: String(stampingId) },
      });
      expect(response.ok(), `eliminazione della timbratura ${stampingId}`).toBeTruthy();
    }

    /**
     * Timbrature di prova rimaste da un'esecuzione interrotta: una seconda timbratura
     * nello stesso giorno e allo stesso orario non sarebbe inserita.
     */
    async function removeLeftovers(page: Page) {
      await page.goto(cartellino());
      const hrefs = await page.locator('a[data-async-modal="#modalStamping"][href*="stampings/edit" i]')
        .evaluateAll((as, time) => as.filter((a) => a.textContent?.trim() === time)
          .map((a) => a.getAttribute('href') ?? ''), TIME);
      for (const href of hrefs) {
        await deleteStamping(page, Number(href.match(/stampingId=(\d+)/)?.[1]));
      }
    }

    /** Inserisce una timbratura con il form di inserimento: restituisce il suo id. */
    async function insertStamping(page: Page): Promise<number> {
      const before = await stampingIds(page);
      // primo giorno del mese del cartellino, generato dal server
      const href = await page.locator('a[href*="stampings/insert" i]').first().getAttribute('href');
      const date = new URL(href ?? '', page.url()).searchParams.get('date');
      expect(date, 'data nel link di inserimento timbratura').toBeTruthy();
      const response = await page.request.post('/stampings/save', {
        form: {
          authenticityToken: await csrfToken(page),
          personId: String(employeeData().personId),
          date: date!,
          time: TIME,
          'stamping.way': 'in',
        },
      });
      expect(response.ok() && !response.url().includes('/login'),
        `inserimento della timbratura (HTTP ${response.status()})`).toBeTruthy();
      const added = (await stampingIds(page)).filter((id) => !before.includes(id));
      expect(added, 'timbratura inserita').toHaveLength(1);
      return added[0];
    }

    test('la GET è rifiutata, il link "Elimina" invia un form POST', async ({ page }) => {
      await removeLeftovers(page);
      const stampingId = await insertStamping(page);
      try {
        // un link verso l'azione, anche dallo stesso sito, non elimina la timbratura
        const get = await page.request.get('/stampings/delete', { params: { id: stampingId } });
        expect(get.status(), 'GET senza token').toBe(403);
        expect(await stampingIds(page)).toContain(stampingId);

        // dal cartellino: modale della timbratura, scheda "Elimina", link "Elimina"
        await page.locator(`a[data-async-modal="#modalStamping"][href*="stampingId=${stampingId}"]`)
          .first().click();
        const modal = page.locator('#modalStamping');
        await expect(modal).toBeVisible();
        await modal.getByRole('tab', { name: 'Elimina', exact: true }).click();
        const link = modal.locator('a[data-method="post"][href*="stampings/delete" i]');
        await expect(link).toBeVisible();
        const token = await csrfToken(page);
        const [request] = await Promise.all([
          page.waitForRequest((r) => /\/stampings\/delete/i.test(r.url())),
          link.click(),
        ]);
        expect(request.method()).toBe('POST');
        expect(new URLSearchParams(request.postData() ?? '').get('authenticityToken')).toBe(token);
        const response = await request.response();
        expect(response?.status(), 'risposta alla POST').not.toBe(403);
        await page.waitForLoadState();

        expect(await stampingIds(page)).not.toContain(stampingId);
      } finally {
        // se il test si è interrotto prima dell'eliminazione, la timbratura è rimossa qui
        if ((await stampingIds(page)).includes(stampingId)) {
          await deleteStamping(page, stampingId);
        }
      }
    });
  });
});
