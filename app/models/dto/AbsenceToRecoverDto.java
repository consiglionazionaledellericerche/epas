package models.dto;

import org.joda.time.LocalDate;

public class AbsenceToRecoverDto {

  public String code;
  public LocalDate absenceDate;
  public LocalDate recoverDate;
  public int quantityRecovered;
  public int quantityToRecover;
}
