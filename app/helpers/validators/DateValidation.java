package helpers.validators;

import org.joda.time.LocalDate;
import play.data.validation.Check;

public class DateValidation extends Check {

  @Override
  public boolean isSatisfied(Object validatedObject, Object value) {
    if (!(value instanceof LocalDate)) {
      return false;
    }
    final LocalDate date = (LocalDate) value; 
    
    if (date != null) {
      return true;
    }
    setMessage("Inserisci una data valida");
    return false;
  }

}
