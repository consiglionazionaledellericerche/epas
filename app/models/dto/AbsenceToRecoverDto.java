package models.dto;

import models.absences.Absence;

import org.joda.time.LocalDate;

public class AbsenceToRecoverDto {

  public Absence absence;
  public LocalDate absenceDate;
  public LocalDate recoverDate;
  public int quantityRecovered;
  public int quantityToRecover;
  public double percentage;

}
