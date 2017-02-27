package helpers.rest;

/**
 * @author daniele on 19/04/16.
 */
public class ApiRequestException extends RuntimeException {

  private static final long serialVersionUID = 5106927141254697844L;

  public ApiRequestException(final String message) {
    super(message);
  }

  @Override
  public String toString() {
    return getMessage();
  }
}
