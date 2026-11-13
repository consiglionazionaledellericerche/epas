import { test, expect } from '../support/fixtures';

const SESSION_COOKIE = 'PLAY_SESSION';

test.describe('cookie di sessione', () => {

  test.describe('utente non autenticato', () => {
    test.use({ storageState: { cookies: [], origins: [] } });

    test('è emesso con gli attributi HttpOnly e Secure', async ({ page, context }) => {
      const response = await page.goto('/login');
      const setCookie = (await response!.headersArray())
        .filter((h) => h.name.toLowerCase() === 'set-cookie')
        .map((h) => h.value)
        .find((value) => value.startsWith(`${SESSION_COOKIE}=`));
      expect(setCookie, 'header Set-Cookie della sessione').toBeDefined();
      expect(setCookie).toMatch(/;\s*HttpOnly/i);
      expect(setCookie).toMatch(/;\s*Secure/i);

      const cookie = (await context.cookies()).find((c) => c.name === SESSION_COOKIE);
      expect(cookie, 'cookie accettato dal browser').toBeDefined();
      expect(cookie!.httpOnly).toBe(true);
      expect(cookie!.secure).toBe(true);
    });
  });

  test('non è leggibile da JavaScript e la sessione resta valida', async ({ page, context }) => {
    await page.goto('/');
    // l'utente è autenticato: arriva al proprio cartellino, non alla pagina di login
    await expect(page).toHaveURL(/\/stampings\/stampings/);
    await expect(page).toHaveTitle(/Timbrature/);

    expect(await page.evaluate(() => document.cookie)).not.toContain(SESSION_COOKIE);

    const cookie = (await context.cookies()).find((c) => c.name === SESSION_COOKIE);
    expect(cookie!.httpOnly).toBe(true);
    expect(cookie!.secure).toBe(true);
  });
});
