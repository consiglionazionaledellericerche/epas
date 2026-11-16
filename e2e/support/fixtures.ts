import { test as base, expect } from '@playwright/test';

/**
 * Test di base: ogni test fallisce se nella pagina si verifica un errore JavaScript
 * non gestito, anche quando le altre verifiche passano.
 */
export const test = base.extend<{ pageErrors: Error[] }>({
  pageErrors: [async ({ page }, use) => {
    const errors: Error[] = [];
    page.on('pageerror', (error) => errors.push(error));
    await use(errors);
    expect(errors, 'errori JavaScript nella pagina').toEqual([]);
  }, { auto: true }],
});

export { expect };
