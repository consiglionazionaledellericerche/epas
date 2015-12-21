package helpers.attestati;

/**
 * Sollevata per eventuali eccezioni di comunicazione con il sistema degli attestati.
 *
 * @author cristian
 */
@SuppressWarnings("serial")
public final class AttestatiException extends RuntimeException {

  private String exception;

  public AttestatiException(final String exception) {
    this.exception = exception;
  }

  public String toString() {
    return exception;
  }
}
