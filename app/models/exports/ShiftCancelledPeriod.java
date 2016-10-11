package models.exports;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import models.ShiftType;

import org.joda.time.LocalDate;

/**
 * Classe di supporto per l'esportazione delle informazioni relative ai turni delle persone: Turni
 * cancellati.
 *
 * @author arianna
 */
@RequiredArgsConstructor 
@AllArgsConstructor
public class ShiftCancelledPeriod {

  public final LocalDate start;
  public LocalDate end;
  public final ShiftType shiftType;

}
