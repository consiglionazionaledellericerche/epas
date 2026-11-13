# Test end-to-end

Test di ePAS eseguiti con [Playwright](https://playwright.dev) in un browser vero, contro
l'immagine docker dell'applicazione. Coprono quello che `play autotest` non esegue: i
template nel browser e il javascript (finestre modali, chiamate ajax, cookie).

## Esecuzione

Servono docker e Node 22 o successivo.

```
docker build -t epas:e2e .
e2e/stack.sh up epas:e2e          # postgres vuoto + ePAS su http://localhost:9000
cd e2e
npm ci
npx playwright install chromium   # solo la prima volta
npx playwright test
cd .. && e2e/stack.sh down
```

Con `npx playwright test --ui` i test si seguono passo per passo. Dopo un errore,
`npx playwright show-trace test-results/<test>/trace.zip` mostra la traccia.

Variabili opzionali:

- `E2E_PORT` (per `stack.sh`): porta locale di ePAS, default 9000;
- `E2E_BASE_URL`: indirizzo di ePAS per i test, default `http://localhost:9000`. Lo
  stack imposta `SESSION_SECURE=true` e i browser accettano i cookie con l'attributo Secure
  in http solo su `localhost`: con un altro host (es. un docker remoto) serve https,
  oppure ePAS avviato con `SESSION_SECURE=false`, altrimenti il login del setup fallisce;
- `E2E_ADMIN_USERNAME`, `E2E_ADMIN_PASSWORD`: utente con ruolo DEVELOPER usato per
  preparare i dati. Il default è l'utente `admin` creato dal job di bootstrap su un
  database vuoto.

Non lanciare i test contro un ePAS con dati reali: `global-setup.ts` crea una persona.

## Struttura

- `stack.sh` avvia e ferma i container.
- `global-setup.ts` gira una volta prima dei test: con l'utente admin crea il dipendente
  di prova con un contratto e salva in `.auth/` le sessioni dell'admin e del dipendente.
- `support/fixtures.ts` è il `test` da importare nei test: fa fallire il test se la
  pagina solleva un errore javascript.
- `tests/` contiene i test. Partono autenticati come il dipendente; per usare l'admin:
  `test.use({ storageState: ADMIN_STATE })`.

I test condividono lo stesso database e sono eseguiti in sequenza: un test non deve
dipendere dai dati lasciati da un altro e, se inserisce dei dati, li deve rimuovere.
