package controllers.rest.v2;

import helpers.JsonResponse;
import play.mvc.Http.Request;

public class RestUtil {

  /**
   * Metodi HTTP utilizzati e verificati 
   * nei controller REST.
   * 
   * @author cristian
   *
   */
  public enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE;
  }

  /**
   * Verifica che il metodo della request sia quello specificato, altrimenti
   * ritorna un bad request con la spiegazione.
   */
  public static void checkMethod(Request request, HttpMethod httpMethod) {
    if (!httpMethod.equals(HttpMethod.valueOf(request.method))) {
      JsonResponse.badRequest(
          String.format("This method supports only the %s method", httpMethod));
    }
  }
}
