# Istruzioni per Claude Code

## Review avversaria prima di ogni PR

Prima di aprire una pull request (`gh pr create` o equivalente) esegui sempre il subagent
`codex-avversario` (definito in `.claude/agents/codex-avversario.md`) sul branch da proporre,
rispetto al branch di destinazione della PR (di solito `main`).

- Correggi le segnalazioni CONFERMATE prima di aprire la PR; per le PLAUSIBILI decidi con
  l'utente se intervenire.
- Dopo aver corretto, committa le correzioni ed esegui di nuovo `codex-avversario` in modalità
  verifica: passagli lo sha del commit su cui era stata fatta la review precedente e l'elenco
  delle segnalazioni corrette (titolo, `file:riga`, scenario). Correggi ciò che la verifica
  trova ancora o di nuovo, e ripeti. Al massimo due review di verifica: se dopo la seconda
  restano segnalazioni CONFERMATE, fermati e chiedi all'utente come procedere.
- Lo stesso vale quando correggi, su una PR già aperta, segnalazioni di una review avversaria.
- Riporta all'utente l'esito: chi ha fatto la review (Codex, oppure Claude come ripiego perché
  Codex non è installato, non è autenticato o è andato in timeout) e le segnalazioni rimaste.
- Se l'utente chiede esplicitamente di aprire la PR senza review, rispetta la richiesta.
