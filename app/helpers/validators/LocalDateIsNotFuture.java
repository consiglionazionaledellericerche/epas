package helpers.validators;

import org.joda.time.LocalDate;
import play.data.validation.Check;

public class LocalDateIsNotFuture extends Check {

  @Override
  public boolean isSatisfied(Object validatedObject, Object date) {
    if (date == null) {
      return false;
    }
    setMessage("Richiesta una data non futura");
    return !((LocalDate) date).isAfter(LocalDate.now());
  }
}
