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
 * @author alessandro
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
      if (abs.justifiedType.name.equals(JustifiedTypeName.specified_minutes) 
          || abs.justifiedType.name.equals(JustifiedTypeName.absence_type_minutes)) {
        count++;
      }
    }
    return count;
  }

  /**
   * @return il nome del mese con valore monthNumber null in caso di argomento non valido.
   */
  public String fromIntToStringMonth(Integer monthNumber) {
    return DateUtility.fromIntToStringMonth(monthNumber);


  }

  /**
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
      int absMonth = abs.personDay.date.getMonthOfYear();
      int absDay = abs.personDay.date.getDayOfMonth();

      List<Absence> values = table.get(absMonth, absDay);
      values.add(abs);
    }

    return table;

  }

  /**
   * @return la mappa contenente i tipi di assenza effettuate nell'anno con il relativo numero di
   *     occorrenze.
   */
  private Map<AbsenceType, Integer> buildYearlyAbsenceSummary(List<Absence> yearlyAbsence) {

    Map<AbsenceType, Integer> mappa = new HashMap<AbsenceType, Integer>();
    //mappa che conterra' le entry (tipo assenza, numero occorrenze)

    Integer idx = 0;
    for (Absence abs : yearlyAbsence) {
      boolean stato = mappa.containsKey(abs.absenceType);
      if (stato == false) {
        idx = 1;
        mappa.put(abs.absenceType, idx);
      } else {
        idx = mappa.get(abs.absenceType);
        mappa.remove(abs.absenceType);
        mappa.put(abs.absenceType, idx + 1);
      }
    }
    return mappa;
  }


}
