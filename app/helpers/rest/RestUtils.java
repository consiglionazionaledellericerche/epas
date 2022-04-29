/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package helpers.rest;

import helpers.JsonResponse;
import play.mvc.Http.Request;

/**
 * Contiene metodi di utilità per la parte REST.
 *
 * @author Cristian Lucchesi
 *
 */
public class RestUtils {

  /**
   * Metodi HTTP utilizzati e verificati 
   * nei controller REST.
   *
   * @author Cristian Lucchesi
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
  
  /**
   * Verifica che l'oggetto passato sia presente (non Null) ed restituisce
   * un not found (404) se non presente.
   */
  public static void checkIfPresent(Object obj) {
    if (obj == null) {
      JsonResponse.notFound();
    }
  }
}
