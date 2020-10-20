package helpers.validators;

import lombok.extern.slf4j.Slf4j;
import play.data.validation.Check;
import play.data.validation.Validation;

@Slf4j
public class StringIsValid extends Check {

  @Override
  public boolean isSatisfied(Object validatedObject, Object value) {

    log.debug("Validatore della stringa...");
    if (!(validatedObject instanceof String)) {
      return false;
    }
    final String string = (String) validatedObject;    

    if (!string.matches("^[a-zA-Z0-9_]+$")) {
      setMessage("Inserisci una stringa valida");
      Validation.addError("stamping.place", "Inserisci una stringa valida");
      return false;
    }
    if (string.matches("^[0-9 ]+$")) {
      setMessage("Non ha senso inserire solo numeri e spazi in questo campo");
      Validation.addError("stamping.place", "Non ha senso inserire solo numeri e spazi in questo campo");
      return false;
    }
    
    return true;
  }

}
