import fs from 'node:fs';
import { request, type APIRequestContext } from '@playwright/test';
import {
  ADMIN_STATE, ADMIN_USERNAME, AUTH_DIR, BASE_URL, EMPLOYEE, EMPLOYEE_DATA, EMPLOYEE_STATE,
  PERSONNEL_ADMIN, PERSONNEL_ADMIN_ROLE, PERSONNEL_ADMIN_STATE, adminPassword,
} from './support/env';

/**
 * Prepara i dati usati dai test su un ePAS appena avviato con un database vuoto:
 * con l'utente admin di bootstrap crea un dipendente con un contratto nella sede
 * iniziale e un amministratore del personale della sede, poi salva le sessioni
 * dell'admin e dei due dipendenti. I test partono già autenticati come il dipendente
 * (storageState in playwright.config.ts).
 *
 * È ripetibile: i dipendenti e il ruolo già presenti vengono riusati.
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
  const employee = await findEmployee(rest, EMPLOYEE.number)
    ?? await createEmployee(rest, EMPLOYEE, office.id);
  await ensureContract(rest, web, employee.id);
  fs.writeFileSync(EMPLOYEE_DATA, JSON.stringify({ personId: employee.id }));

  const personnelAdmin = await findEmployee(rest, PERSONNEL_ADMIN.number)
    ?? await createEmployee(rest, PERSONNEL_ADMIN, office.id);
  // il form di assegnazione dei ruoli elenca solo le persone con un contratto attivo
  await ensureContract(rest, web, personnelAdmin.id);
  await assignRole(web, office.id, PERSONNEL_ADMIN, PERSONNEL_ADMIN_ROLE);

  await saveSessionAs(personnelAdmin.id, password, PERSONNEL_ADMIN_STATE);
  await saveSessionAs(employee.id, password, EMPLOYEE_STATE);
}

async function ensureContract(rest: APIRequestContext, web: APIRequestContext, personId: number) {
  if (!(await hasContract(rest, personId))) {
    await createContract(web, personId);
    if (!(await hasContract(rest, personId))) {
      throw new Error(`Contratto non creato per il dipendente ${personId}`);
    }
  }
}

/**
 * L'admin passa all'utente del dipendente, che non ha una password locale, e salva la
 * sessione. Ogni cambio parte da un nuovo login dell'admin: dopo il cambio la sessione
 * è quella del dipendente.
 */
async function saveSessionAs(personId: number, password: string, statePath: string) {
  const web = await request.newContext({ baseURL: BASE_URL });
  try {
    await login(web, ADMIN_USERNAME, password);
    const switched = await web.get('/administration/switchUserToPersonUser', {
      params: { id: personId },
    });
    if (!switched.ok()) {
      throw new Error(`Cambio utente fallito: HTTP ${switched.status()}`);
    }
    await web.storageState({ path: statePath });
  } finally {
    await web.dispose();
  }
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
async function findEmployee(rest: APIRequestContext, number: string): Promise<{ id: number } | undefined> {
  const response = await rest.get('/rest/v2/persons/show', { params: { number } });
  if (response.status() === 404) {
    return undefined;
  }
  return jsonOrThrow(response, 'ricerca del dipendente');
}

async function createEmployee(
  rest: APIRequestContext, person: typeof EMPLOYEE, officeId: number,
): Promise<{ id: number }> {
  const response = await rest.post('/rest/v2/persons/create', { data: { ...person, officeId } });
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

/**
 * Assegna al dipendente un ruolo sulla sede con il form dell'amministrazione, che elenca
 * utenti e ruoli con le loro etichette. Se il ruolo è già assegnato il form risponde 400.
 */
async function assignRole(
  web: APIRequestContext, officeId: number, person: typeof EMPLOYEE, roleLabel: string,
) {
  const form = await web.get('/administrators/blank', { params: { officeId } });
  const html = await form.text();
  const token = html.match(/name="authenticityToken" value="([^"]+)"/)?.[1];
  const user = optionValue(html, `${person.surname} ${person.name}`);
  const role = optionValue(html, roleLabel);
  if (!form.ok() || !token || !user || !role) {
    throw new Error(`Form di assegnazione del ruolo non valido (HTTP ${form.status()})`);
  }
  const response = await web.post('/administrators/save', {
    form: {
      authenticityToken: token,
      'uro.office.id': String(officeId),
      'uro.user.id': user,
      'uro.role.id': role,
    },
  });
  const alreadyAssigned = response.status() === 400
    && (await response.text()).includes('Esiste già quel ruolo');
  if (!alreadyAssigned && (!response.ok() || response.url().includes('/login'))) {
    throw new Error(`Assegnazione del ruolo fallita: HTTP ${response.status()}, ${response.url()}`);
  }
}

/** Valore della option con l'etichetta indicata, eventualmente seguita da " - sede". */
function optionValue(html: string, label: string): string | undefined {
  const escaped = label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  return html.match(new RegExp(`<option value="(\\d+)"[^>]*>\\s*${escaped}\\s*(?:<|-)`))?.[1];
}

async function jsonOrThrow(response: Awaited<ReturnType<APIRequestContext['get']>>, what: string) {
  if (!response.ok()) {
    throw new Error(`${what}: HTTP ${response.status()} ${await response.text()}`);
  }
  return response.json();
}
