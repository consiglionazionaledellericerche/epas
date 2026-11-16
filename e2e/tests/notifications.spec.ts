import { test, expect } from '../support/fixtures';

/**
 * Notifiche: initepas() (public/javascripts/script.js) trasforma gli elementi
 * [data-notify] in notifiche PNotify. Il testo è letto con .text(), quindi le entità
 * HTML già sottoposte a escape dal template tornano caratteri: PNotify deve mostrarle
 * come testo (title_escape e text_escape) e non interpretarle come HTML.
 *
 * Il messaggio di un flash con dati inseriti da un utente è riprodotto aggiungendo alla
 * pagina un elemento come quello del tag pnotify.html, senza modificare dati.
 */
test.describe('notifiche', () => {
  test('titolo e testo sono mostrati come testo', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveURL(/\/stampings\/stampings/);

    const title = '<b id="e2e-title">titolo</b>';
    const text = '<img id="e2e-text" src=x onerror="window.__e2eXss=1">testo';
    await page.evaluate(([title, text]) => {
      const div = document.createElement('div');
      div.className = 'sr-only';
      div.setAttribute('data-notify', title);
      div.setAttribute('data-notify-type', 'error');
      // come nel template: il testo del flash è nel contenuto, con l'escape HTML
      div.textContent = text;
      // initepas() cerca tra i discendenti: il contenitore limita l'inizializzazione
      // all'elemento aggiunto, senza ripeterla sul resto della pagina
      const wrapper = document.createElement('div');
      wrapper.appendChild(div);
      document.body.appendChild(wrapper);
      (window as any).$(wrapper).initepas();
    }, [title, text]);

    const notice = page.locator('.ui-pnotify').filter({ hasText: 'testo' });
    await expect(notice).toBeVisible();
    await expect(notice.locator('.ui-pnotify-title')).toHaveText(title);
    await expect(notice.locator('.ui-pnotify-text')).toHaveText(text);
    await expect(page.locator('#e2e-title, #e2e-text')).toHaveCount(0);
    expect(await page.evaluate(() => (window as { __e2eXss?: number }).__e2eXss)).toBeUndefined();
  });
});
