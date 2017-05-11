package helpers;

import java.io.IOException;

import play.Logger;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.Result;

/**
 * BadRequest (HTTP 400) with response content.
 *
 * @author cristian
 */
public class BadRequest extends Result {

  private static final long serialVersionUID = -884396263781488733L;

  private String description;

  public BadRequest() {
    super();
  }

  public BadRequest(String description) {
    super(description);
    this.description = description;
  }

  /**
   * Send a 400 Bad request.
   */
  public static void badRequest(String description) {
    throw new BadRequest(description);
  }

  @Override
  public void apply(Request request, Response response) {
    response.status = Http.StatusCode.BAD_REQUEST;
    try {
      response.out.write(description.getBytes());
    } catch (IOException ioe) {
      Logger.error("Exception during apply of badRequest with description");
    }
  }
}
