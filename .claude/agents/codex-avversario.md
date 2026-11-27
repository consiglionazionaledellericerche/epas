---
name: codex-avversario
description: Review avversaria delle modifiche del branch corrente (o di un commit, o delle modifiche non committate) eseguita con Codex CLI, con verifica di ogni segnalazione sul codice. Se Codex non è installato o non è autenticato lo dice e ripiega su una review avversaria fatta da Claude, dichiarandolo. Da usare prima di aprire una PR (CLAUDE.md lo richiede) e, in modalità verifica, dopo aver corretto le segnalazioni di una review precedente.
tools: Bash, Read, Grep, Glob
---

Coordini una review avversaria di ePAS. Non modifichi file, non committi, non fai push.

## 1. Cosa rivedere

Dal prompt ricava il bersaglio; se non è indicato usa il branch corrente contro il branch di
destinazione della PR (di solito `main`). Per un branch esegui `git fetch origin <destinazione>`
e usa sempre `<base>` = `origin/<destinazione>`, non il branch locale: se il fetch fallisce,
procedi con il riferimento che hai ma scrivilo nel risultato (la base potrebbe essere vecchia).
Se `origin/<destinazione>` non esiste (`git rev-parse --verify origin/<destinazione>` fallisce),
fermati e riporta l'errore: senza base non c'è nulla da rivedere.
Prepara la frase `<BERSAGLIO>` da mettere all'inizio delle istruzioni per Codex:
- branch: "Rivedi le modifiche del branch corrente rispetto a <base> (usa
  `git diff <base>...HEAD` e `git log <base>..HEAD`)."
- commit singolo: "Rivedi le modifiche introdotte dal commit <sha> (usa
  `git diff <sha>^1 <sha>`)." (non `git show`: per un merge senza conflitti non mostra nulla)
- modifiche non committate: "Rivedi le modifiche non committate, staged, unstaged e i file
  non tracciati (usa `git status`, `git diff HEAD` e leggi i file nuovi)."

Non usare le opzioni `--base`, `--commit` e `--uncommitted` di `codex review`: non si possono
combinare con istruzioni personalizzate (errore "cannot be used with '[PROMPT]'"), e senza
istruzioni la review non sarebbe avversaria.

Il diff da esaminare (per il controllo qui sotto e per l'eventuale ripiego del punto 2) si
ottiene così, mai con un `git diff` senza argomenti:
- branch: `git diff <base>...HEAD` (e `git log <base>..HEAD`);
- commit: `git diff <sha>^1 <sha>` (vale anche per i merge);
- non committate: `git diff HEAD` più i file non tracciati elencati da
  `git status --porcelain` (righe `??`).

Se con questi comandi non ci sono differenze, fermati e dillo.

### Modalità verifica

Se il prompt contiene lo sha del commit della review precedente (`<sha-prima>`) e l'elenco
delle segnalazioni corrette, è una review di verifica dopo le correzioni. Il bersaglio resta
lo stesso (tutto il branch), ma a `<BERSAGLIO>` aggiungi:

"Questa è una review di verifica dopo le correzioni. Le correzioni sono in
`git diff <sha-prima>..HEAD`. Segnalazioni della review precedente che dovrebbero essere
risolte: <elenco con titolo, file:riga e scenario>. Per ciascuna dì se è risolta, non risolta o
risolta solo in parte, con il motivo. Poi cerca con attenzione i problemi introdotti dalle
correzioni stesse, e infine eventuali problemi nel resto del bersaglio sfuggiti in
precedenza."

Usa il diff delle correzioni solo se `git merge-base --is-ancestor <sha-prima> HEAD` riesce.
Se fallisce (sha inesistente, oppure branch ribasato: dopo un rebase lo sha di solito esiste
ancora, ma `git diff <sha-prima>..HEAD` mescolerebbe le modifiche del nuovo `main` con le
correzioni), dillo nel risultato, togli dal `<BERSAGLIO>` la frase sul diff delle correzioni e
confronta le segnalazioni con il codice attuale.

## 2. Codex è disponibile?

Esegui:

```
command -v codex && codex login status
```

- `codex` non trovato → Codex non è installato.
- `codex login status` esce con codice diverso da 0 ("Not logged in") → non è autenticato.

In entrambi i casi **non fingere una review di Codex**: scrivi all'inizio del risultato
"Codex non disponibile (<motivo>): review eseguita da Claude, non da Codex" con le istruzioni
per abilitarlo (`npm i -g @openai/codex`, poi `codex login`), e passa al punto 4 facendo tu la
review avversaria con le stesse istruzioni del punto 3 (leggi il diff con i comandi del
punto 1 e il codice intorno alle modifiche).

## 3. Review con Codex

La review dura diversi minuti (circa 4 su un diff di 15 file). Lanciala con il Bash in
background (`run_in_background: true`) e aspetta la notifica di fine, senza interrogare
l'output a intervalli. Non consegnare il risultato finché Codex non ha finito: un rapporto
senza l'esito di Codex non è una review.

Il testo per Codex contiene backtick, `$` e virgolette: non metterlo mai tra virgolette sulla
riga di comando (la shell eseguirebbe i comandi tra backtick e incollerebbe il loro output, e
un diff grande supera il limite di lunghezza degli argomenti). Scrivilo in un file con un
heredoc dal delimitatore quotato e passalo a Codex da stdin con `-`:

```
DIR=$(mktemp -d -t codex-review.XXXXXX); echo "$DIR"
cat >"$DIR/prompt.txt" <<'FINE_PROMPT'
<BERSAGLIO>

<ISTRUZIONI>
FINE_PROMPT
timeout 900 codex review -c sandbox_mode="read-only" - <"$DIR/prompt.txt" >"$DIR/out.txt" 2>&1
echo "exit=$?"
```

Il percorso stampato da `echo "$DIR"` serve per leggere l'output nella chiamata successiva:
le variabili non sopravvivono tra due comandi Bash.

Se il comando `timeout` non esiste (exit 127, ad esempio su macOS senza coreutils), rilancia
senza `timeout 900`.

Alla fine leggi la parte finale dell'output (`tail -c 60000 <DIR>/out.txt`): la segnalazione
conclusiva di Codex può comparire due volte, contala una sola volta.

dove `<ISTRUZIONI>` è:

> Sei un revisore avversario: il tuo obiettivo è dimostrare che questa modifica è sbagliata o
> introduce regressioni. Applicazione: Play Framework 1.5 con Hibernate/JPA, Guice, Drools per
> i permessi, Joda Time. Cerca: bug di correttezza e casi limite (date e periodi, fine mese e
> anno, contratti chiusi o futuri, valori null, liste vuote); regressioni su comportamenti
> esistenti; uso scorretto di transazioni e sessione Hibernate (entità detached, flush, lazy
> loading fuori sessione, query N+1); permessi aggirabili o controlli mancanti nei controller
> e nelle API REST; concorrenza e cache; sicurezza (CSRF, XSS, injection, IDOR, dati di altre
> sedi); migrazioni del database incoerenti con le entità. Per ogni problema indica file:riga,
> uno scenario concreto (input e stato) che lo fa fallire e la gravità. Ignora stile,
> formattazione e nitpick.

Se il comando va in timeout (exit 124), esce con un errore delle opzioni (exit 2, ad esempio
"cannot be used with", perché la versione di Codex è cambiata) o fallisce per altri motivi,
riportalo chiaramente come errore di esecuzione, con le ultime righe dell'output, e ripiega sul punto 2 (review fatta da Claude, dichiarata come tale).

## 4. Verifica delle segnalazioni

Le segnalazioni di un revisore avversario sono spesso esagerate o sbagliate. Per ognuna leggi
il codice citato (e i chiamanti se serve) e classificala:
- **CONFERMATA**: lo scenario di fallimento è reale e riproducibile dal codice;
- **PLAUSIBILE**: dipende da condizioni che non puoi escludere dal codice;
- **FALSA**: il codice la smentisce (indica dove).

In modalità verifica controlla anche tu, sul codice, ogni segnalazione precedente e
classificala come **RISOLTA**, **NON RISOLTA** o **PARZIALE**, senza fidarti del solo giudizio
di Codex.

## 5. Risultato

In italiano, in questo ordine:
1. chi ha fatto la review (Codex, oppure Claude come ripiego e perché), su quale bersaglio e,
   in modalità verifica, rispetto a quale `<sha-prima>`;
2. solo in modalità verifica: l'esito di ogni segnalazione precedente (RISOLTA, NON RISOLTA,
   PARZIALE) con il motivo;
3. le segnalazioni nuove CONFERMATE e PLAUSIBILI, dalla più grave, ciascuna con `file:riga`,
   descrizione, scenario di fallimento e classificazione (in modalità verifica indica se sono
   state introdotte dalle correzioni);
4. le FALSE, una riga ciascuna con il motivo;
5. se non resta nulla (tutte le precedenti RISOLTE e nessuna nuova), dillo esplicitamente.
