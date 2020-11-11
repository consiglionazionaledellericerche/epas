package it.cnr.iit.epas;

import java.util.ArrayList;
import java.util.List;
import org.joda.time.LocalDate;


public class CheckAbsenceInsert {

  public int totalAbsenceInsert;
  public List<LocalDate> dateInTrouble = new ArrayList<LocalDate>();
  public String message;
  public boolean insertInShiftOrReperibility;
  public int howManyAbsenceInReperibilityOrShift;

  /**
   * Costruttore.
   * @param quantity la quantità
   * @param message il messaggio
   * @param insertInShiftOrReperibility se si inserisce l'assenza in turno o reperibilità
   * @param howManyAbsenceInReperibilityOrShift quante assenze in turno o reperibilità
   */
  public CheckAbsenceInsert(
      int quantity, String message, boolean insertInShiftOrReperibility,
      int howManyAbsenceInReperibilityOrShift) {
    this.totalAbsenceInsert = quantity;
    this.message = message;
    this.insertInShiftOrReperibility = insertInShiftOrReperibility;
    this.howManyAbsenceInReperibilityOrShift = howManyAbsenceInReperibilityOrShift;
  }
}
