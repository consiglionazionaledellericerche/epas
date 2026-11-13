/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Azione che modifica dati: non è accettata in GET (né HEAD), così non può essere eseguita da
 * un link o da una navigazione proveniente da un altro sito, che non porta il token anti-CSRF.
 *
 * <p>La verifica è fatta da controllers.CsrfProtection e segue il parametro csrf.mode
 * (off, report, enforce), come la verifica del token.</p>
 *
 * <p>Per le azioni che in GET mostrano una pagina di conferma e modificano i dati solo quando
 * un parametro booleano è true (es. confirmed), si indica il parametro in confirmParam: la GET
 * è accettata solo se il parametro non è true.</p>
 *
 * <p>Nelle view i link verso queste azioni vanno resi con data-method="post", gestito da
 * script.js, oppure sostituiti da un form.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PostOnly {

  /**
   * Il parametro booleano che, se true, indica che la richiesta modifica i dati. Vuoto se
   * l'azione modifica sempre i dati.
   */
  String confirmParam() default "";
}
