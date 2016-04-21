package helpers.rest;

/**
 * @author daniele on 19/04/16.
 */
public class ApiRequestException extends RuntimeException {

  private String message;

  public ApiRequestException(final String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return message;
  }
}
