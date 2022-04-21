package models.dto;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import javax.persistence.Transient;

/**
 * Dto per l'oggetto telework da mandare al teleworkstampings.
 *
 * @author dario
 *
 */
public class NewTeleworkDto {

  public LocalDate date;
  public TeleworkDto beginDay;
  public TeleworkDto endDay;
  public TeleworkDto beginMeal;
  public TeleworkDto endMeal;
  public TeleworkDto beginInterruption;
  public TeleworkDto endInterruption;
  
  @Transient
  public String displayDay() {
    return "" + this.date.getDayOfMonth() + '\t' 
        + this.date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ITALY);
  }
}
