import fs from 'node:fs';
import { request, type APIRequestContext } from '@playwright/test';
import {
  ADMIN_STATE, ADMIN_USERNAME, AUTH_DIR, BASE_URL, EMPLOYEE, EMPLOYEE_DATA, EMPLOYEE_STATE,
  adminPassword,
} from './support/env';

/**
 * Prepara i dati usati dai test su un ePAS appena avviato con un database vuoto:
 * con l'utente admin di bootstrap crea un dipendente con un contratto nella sede
 * iniziale, poi salva le sessioni dell'admin e del dipendente. I test partono già
 * autenticati come il dipendente (storageState in playwright.config.ts).
 *
 * È ripetibile: se il dipendente esiste già viene riusato.
 */
export default async function globalSetup() {
  fs.mkdirSync(AUTH_DIR, { recursive: true });
  const password = adminPassword();

  // Le API REST usano la basic auth, le pagine web la sessione del login.
  const rest = await request.newContext({
    baseURL: BASE_URL,
    httpCredentials: { username: ADMIN_USERNAME, password, send: 'always' },
  });
  const web = await request.newContext({ baseURL: BASE_URL });
  try {
    await prepare(rest, web, password);
  } finally {
    await rest.dispose();
    await web.dispose();
  }
}

async function prepare(rest: APIRequestContext, web: APIRequestContext, password: string) {
  await login(web, ADMIN_USERNAME, password);
  await web.storageState({ path: ADMIN_STATE });

  const office = await firstOffice(rest);
  let person = await findEmployee(rest);
  if (!person) {
    person = await createEmployee(rest, office.id);
  }
  if (!(await hasContract(rest, person.id))) {
    await createContract(web, person.id);
    if (!(await hasContract(rest, person.id))) {
      throw new Error(`Contratto non creato per il dipendente ${person.id}`);
    }
  }
  fs.writeFileSync(EMPLOYEE_DATA, JSON.stringify({ personId: person.id }));

  // L'admin passa all'utente del dipendente, che non ha una password locale.
  const switched = await web.get('/administration/switchUserToPersonUser', {
    params: { id: person.id },
  });
  if (!switched.ok()) {
    throw new Error(`Cambio utente fallito: HTTP ${switched.status()}`);
  }
  await web.storageState({ path: EMPLOYEE_STATE });
}

async function login(web: APIRequestContext, username: string, password: string) {
  const response = await web.post('/secure/authenticate', { form: { username, password } });
  if (!response.ok() || response.url().includes('/login')) {
    throw new Error(`Login di ${username} fallito (HTTP ${response.status()}, ${response.url()})`);
  }
}

async function firstOffice(rest: APIRequestContext): Promise<{ id: number; codeId: string }> {
  const response = await rest.get('/rest/v3/offices/all');
  const offices = await jsonOrThrow(response, 'elenco delle sedi');
  if (offices.length === 0) {
    throw new Error('Nessuna sede presente: il job di bootstrap non è stato eseguito (JOBS_ACTIVE=true)');
  }
  return offices[0];
}

/**
 * Cerca il dipendente per matricola: /rest/v2/persons/list restituisce solo le persone
 * con un contratto attivo e non troverebbe un dipendente rimasto senza contratto da
 * un'esecuzione precedente interrotta.
 */
async function findEmployee(rest: APIRequestContext): Promise<{ id: number } | undefined> {
  const response = await rest.get('/rest/v2/persons/show', { params: { number: EMPLOYEE.number } });
  if (response.status() === 404) {
    return undefined;
  }
  return jsonOrThrow(response, 'ricerca del dipendente');
}

async function createEmployee(rest: APIRequestContext, officeId: number): Promise<{ id: number }> {
  const response = await rest.post('/rest/v2/persons/create', { data: { ...EMPLOYEE, officeId } });
  return jsonOrThrow(response, 'creazione del dipendente');
}

async function hasContract(rest: APIRequestContext, personId: number): Promise<boolean> {
  const response = await rest.get('/rest/v2/contracts/byPerson', { params: { id: personId } });
  return (await jsonOrThrow(response, 'contratti del dipendente')).length > 0;
}

/**
 * Il contratto è inserito con il form web: l'API REST di creazione non permette di
 * indicare il tipo di contratto, che è obbligatorio (#672).
 */
async function createContract(web: APIRequestContext, personId: number) {
  const form = await web.get('/contracts/insert', { params: { 'person.id': personId } });
  const html = await form.text();
  const token = html.match(/name="authenticityToken" value="([^"]+)"/)?.[1];
  // orario di lavoro "Normale", presente nei dati iniziali
  const wtt = html.match(/<option value="(\d+)"[^>]*>\s*Normale\s*</)?.[1];
  if (!form.ok() || !token || !wtt) {
    throw new Error(`Form di inserimento del contratto non valido (HTTP ${form.status()})`);
  }
  const response = await web.post('/contracts/save', {
    form: {
      authenticityToken: token,
      'contract.person.id': String(personId),
      'contract.beginDate': `01/01/${new Date().getFullYear() - 1}`,
      'contract.contractType': 'structured_public_administration',
      'wtt.id': wtt,
    },
  });
  // i redirect sono seguiti: un rinvio al login risponde comunque 200
  if (!response.ok() || response.url().includes('/login')) {
    throw new Error(`Inserimento del contratto fallito: HTTP ${response.status()}, ${response.url()}`);
  }
}

async function jsonOrThrow(response: Awaited<ReturnType<APIRequestContext['get']>>, what: string) {
  if (!response.ok()) {
    throw new Error(`${what}: HTTP ${response.status()} ${await response.text()}`);
  }
  return response.json();
}
