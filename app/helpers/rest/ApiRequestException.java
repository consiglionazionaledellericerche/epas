package helpers.rest;

/**
 * @author daniele on 19/04/16.
 */
public class ApiRequestException extends RuntimeException {

  private static final long serialVersionUID = 5106927141254697844L;
  private final String message;

  public ApiRequestException(final String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
