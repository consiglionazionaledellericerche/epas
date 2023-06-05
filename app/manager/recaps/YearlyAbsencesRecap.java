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

package manager.recaps;

import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import it.cnr.iit.epas.DateUtility;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.JustifiedType.JustifiedTypeName;

/**
 * Classe da utilizzare per il rendering delle assenze annuali effettuate da una persona
 * in un anno.
 *
 * @author Alessandro Martelli
 */
public class YearlyAbsencesRecap {

  public Person person;
  public int year;
  public Map<AbsenceType, Integer> absenceSummary = new HashMap<AbsenceType, Integer>();
  public int totalAbsence = 0;
  public int totalHourlyAbsence = 0;
  Table<Integer, Integer, List<Absence>> absenceTable;

  /**
   * Costruttore riepilogo.
   *
   * @param person persona
   * @param year anno
   * @param yearlyAbsence assenze annuali
   */
  public YearlyAbsencesRecap(Person person, int year, List<Absence> yearlyAbsence) {
    this.person = person;
    this.year = year;

    this.totalAbsence = yearlyAbsence.size();
    this.totalHourlyAbsence = checkHourAbsence(yearlyAbsence);
    this.absenceTable = buildYearlyAbsenceTable(yearlyAbsence);
    this.absenceSummary = buildYearlyAbsenceSummary(yearlyAbsence);
  }

  private int checkHourAbsence(List<Absence> yearlyAbsence) {
    int count = 0;
    for (Absence abs : yearlyAbsence) {
      if (abs.getJustifiedType().getName().equals(JustifiedTypeName.specified_minutes) 
          || abs.getJustifiedType().getName().equals(JustifiedTypeName.absence_type_minutes)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Il nome del mese a partire dal numero.
   *
   * @return il nome del mese con valore monthNumber null in caso di argomento non valido.
   */
  public String fromIntToStringMonth(Integer monthNumber) {
    return DateUtility.fromIntToStringMonth(monthNumber);


  }

  /**
   * La tabella contenente i codici di assenza effettuati nel giorno.
   *
   * @return la tabella contenente in ogni cella i codici delle assenze effettuate in quel giorno.
   */
  private Table<Integer, Integer, List<Absence>> buildYearlyAbsenceTable(
      List<Absence> yearlyAbsenceList) {
    
    Table<Integer, Integer, List<Absence>> table = TreeBasedTable.create();

    //dimensionamento tabella 12 righe e 31 colonne
    for (int month = 1; month <= 12; month++) {
      for (int day = 1; day <= 31; day++) {
        table.put(month, day, Lists.newArrayList());
      }
    }

    //inserimento valori
    for (Absence abs : yearlyAbsenceList) {
      int absMonth = abs.getPersonDay().getDate().getMonthOfYear();
      int absDay = abs.getPersonDay().getDate().getDayOfMonth();

      List<Absence> values = table.get(absMonth, absDay);
      values.add(abs);
    }

    return table;

  }

  /**
   * La mappa tipo assenza-quantità.
   *
   * @return la mappa contenente i tipi di assenza effettuate nell'anno con il relativo numero di
   *     occorrenze.
   */
  private Map<AbsenceType, Integer> buildYearlyAbsenceSummary(List<Absence> yearlyAbsence) {

    Map<AbsenceType, Integer> mappa = new HashMap<AbsenceType, Integer>();
    //mappa che conterra' le entry (tipo assenza, numero occorrenze)

    Integer idx = 0;
    for (Absence abs : yearlyAbsence) {
      boolean stato = mappa.containsKey(abs.getAbsenceType());
      if (stato == false) {
        idx = 1;
        mappa.put(abs.getAbsenceType(), idx);
      } else {
        idx = mappa.get(abs.getAbsenceType());
        mappa.remove(abs.getAbsenceType());
        mappa.put(abs.getAbsenceType(), idx + 1);
      }
    }
    return mappa;
  }


}
