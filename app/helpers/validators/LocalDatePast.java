package helpers.validators;

import org.joda.time.LocalDate;

import play.data.validation.Check;

public class LocalDatePast extends Check {

  @Override
  public boolean isSatisfied(Object validatedObject, Object date) {
    if (date == null) {
      return false;
    }
    setMessage("Richiesta una data passata");
    return LocalDate.now().isAfter((LocalDate) date);
  }
}
