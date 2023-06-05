/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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

package manager.response;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.google.common.base.Function;
import helpers.rest.JacksonModule;
import lombok.Getter;
import lombok.Setter;
import models.absences.Absence;
import org.joda.time.LocalDate;

/**
 * DTO per contenere le informazioni relative all'esito di un inserimento di assenza. 
 */
@JsonFilter(JacksonModule.FILTER)
public class AbsencesResponse {

  public static final String CODICE_FERIE_GIA_PRESENTE =
      "Il codice di assenza é già presente in almeno uno dei giorni in cui lo si voleva inserire";
  public static final String CODICE_GIORNALIERO_GIA_PRESENTE =
      "Esiste già un codice di assenza giornaliero nel periodo indicato. Operazione annullata";
  public static final String NESSUN_CODICE_FERIE_DISPONIBILE_PER_IL_PERIODO_RICHIESTO =
      "Nessun codice ferie disponibile per il periodo richiesto";
  public static final String RIPOSI_COMPENSATIVI_ESAURITI =
      "Numero di giorni di riposo compensativo esauriti per l'anno corrente";
  public static final String MONTE_ORE_INSUFFICIENTE =
      "Monte ore insufficiente per l'assegnamento del riposo compensativo";
  public static final String NON_UTILIZZABILE_NEI_FESTIVI =
      "Codice non utilizzabile in un giorno festivo";
  public static final String NESSUN_CODICE_FERIE_ANNO_PRECEDENTE_37 =
      "Nessun codice ferie dell'anno precedente 37 utilizzabile";
  public static final String ERRORE_GENERICO =
      "Impossibile inserire il codice d'assenza";
  public static final String PERSONDAY_PRECEDENTE_NON_PRESENTE =
      "Nessun personday per il giorno precedente a quando si intende inserire il codice con "
      + "allegato. Verificare";
  public static final String CODICE_NON_UTILIZZABILE = "Il codice di assenza non è utilizzabile "
      + "poichè la qualifica della persona non è presente tra quelle che possono utilizzare il "
      + "codice";

  @Getter
  @Setter
  private LocalDate date;

  @Getter
  @Setter
  private String absenceCode;

  @Getter
  @Setter
  private String warning;

  @Getter
  @Setter
  private boolean insertSucceeded = false;

  @Getter
  @Setter
  private boolean isHoliday = false;

  @Getter
  @Setter
  private boolean isDayInReperibilityOrShift = false;

  @Getter
  @Setter
  private boolean isDayInReperibility = false;

  @Getter
  @Setter
  private boolean isDayInShift = false;

  @Getter
  @Setter
  private Absence absenceAdded;
  
  @Getter
  @Setter
  private Absence absenceInError;

  public AbsencesResponse(LocalDate date, String absenceCode) {
    this.date = date;
    this.absenceCode = absenceCode;
  }

  /**
   * Costruttore.
   *
   * @param date la data
   * @param absenceCode il codice di assenza
   * @param warning il warning 
   */
  public AbsencesResponse(LocalDate date, String absenceCode, String warning) {
    super();
    this.date = date;
    this.absenceCode = absenceCode;
    this.warning = warning;
  }

  /**
   * Enumerato che ritorna la data di un AbsenceResponse.
   *
   * @author Dario Tagliaferri
   *
   */
  public enum ToDate implements Function<AbsencesResponse, LocalDate> {
    INSTANCE;

    @Override
    public LocalDate apply(AbsencesResponse air) {
      return air.date;
    }
  }

}
