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

package manager.sync.personoffice;

import lombok.Data;

/**
 * Variazione di afferenza di un dipendente ricevuta da un sistema esterno (ACE, Attestati).
 * La persona è individuata per matricola o, in mancanza, per codice fiscale; la sede per
 * codice sede (codeId).
 *
 * <p>Formato JSON provvisorio, da allineare al servizio esterno:
 * <pre>
 * {"number": "12345", "fiscalCode": "RSSMRA80A01H501U", "officeCodeId": "223400",
 *  "beginDate": "2026-03-15", "endDate": null}
 * </pre></p>
 */
@Data
public class PersonOfficeChange {

  private String number;
  private String fiscalCode;
  private String officeCodeId;
  /** Data di decorrenza nel formato yyyy-MM-dd. */
  private String beginDate;
  /** Eventuale data di fine nel formato yyyy-MM-dd. */
  private String endDate;
}
