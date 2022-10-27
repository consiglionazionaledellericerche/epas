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

package cnr.sync.dto;

import models.absences.Absence;

/**
 * DTO per rappresentare i dati un'assenza via REST.
 */
public class AbsenceRest {

  public Long id;
  public String date;
  public String absenceCode;
  public String description;
  public String name;
  public String surname;
  public Boolean hasAttachment;
  
  /**
   * Costruisce una nuova istanza del DTO a partire dall'assenza.
   */
  public static AbsenceRest build(Absence absence) {
    AbsenceRest ar = new AbsenceRest();
    ar.id = absence.id;
    ar.absenceCode = absence.getAbsenceType().getCode();
    ar.description = absence.getAbsenceType().getDescription();
    ar.date = absence.getPersonDay().getDate().toString();
    ar.name = absence.getPersonDay().getPerson().getName();
    ar.surname = absence.getPersonDay().getPerson().getSurname();
    ar.hasAttachment = absence.getAbsenceFile() != null && absence.getAbsenceFile().get() != null;
    return ar;
  }
}
