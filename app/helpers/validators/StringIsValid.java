package helpers.validators;

import lombok.extern.slf4j.Slf4j;
import models.Stamping;
import play.data.validation.Check;
import play.data.validation.Validation;

public class StringIsValid extends Check {

  @Override
  public boolean isSatisfied(Object validatedObject, Object value) {

    if (!(value instanceof String)) {
      return false;
    }
    final String string = (String) value;    

    if (!string.matches("^[A-Za-z0-9 _]*[A-Za-z0-9][A-Za-z0-9 _]*$")) {
      setMessage("Inserisci una stringa valida");
      return false;
    }
    if (string.matches("^[0-9 ]+$")) {
      setMessage("Non ha senso inserire solo numeri e spazi in questo campo");
      return false;
    }
    return true;
  }

}
