package manager.attestati.old;

/**
 * Sollevata per eventuali eccezioni di comunicazione con il sistema degli attestati.
 *
 * @author cristian
 */
@SuppressWarnings("serial")
public final class AttestatiException extends RuntimeException {

  public AttestatiException(final String message) {
    super(message);
  }

  public String toString() {
    return getMessage();
  }
}
