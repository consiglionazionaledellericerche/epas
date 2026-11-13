import fs from 'node:fs';
import path from 'node:path';

export const BASE_URL = process.env.E2E_BASE_URL ?? 'http://localhost:9000';

export const AUTH_DIR = path.join(__dirname, '..', '.auth');
/** Sessione dell'utente admin creato dal job di bootstrap. */
export const ADMIN_STATE = path.join(AUTH_DIR, 'admin.json');
/** Sessione del dipendente di prova creato da global-setup.ts. */
export const EMPLOYEE_STATE = path.join(AUTH_DIR, 'employee.json');
/** Dati del dipendente di prova (id della persona). */
export const EMPLOYEE_DATA = path.join(AUTH_DIR, 'employee-data.json');

export const ADMIN_USERNAME = process.env.E2E_ADMIN_USERNAME ?? 'admin';

/**
 * Password dell'utente admin di bootstrap. Su un database vuoto è quella iniziale
 * documentata nel README: è letta da lì per non ripeterla in un altro file.
 */
export function adminPassword(): string {
  if (process.env.E2E_ADMIN_PASSWORD) {
    return process.env.E2E_ADMIN_PASSWORD;
  }
  const readme = fs.readFileSync(path.join(__dirname, '..', '..', 'README.md'), 'utf8');
  const match = readme.match(/username: _admin_ password _([^_\s]+)_/);
  if (!match) {
    throw new Error('Password iniziale di admin non trovata nel README: impostare E2E_ADMIN_PASSWORD');
  }
  return match[1];
}

/** Dipendente di prova: la matricola lo identifica tra un'esecuzione e l'altra. */
export const EMPLOYEE = {
  name: 'Mario',
  surname: 'Rossi',
  email: 'mario.rossi@example.com',
  number: 'E2E-0001',
  qualification: 4,
};

export function employeeData(): { personId: number } {
  return JSON.parse(fs.readFileSync(EMPLOYEE_DATA, 'utf8'));
}
