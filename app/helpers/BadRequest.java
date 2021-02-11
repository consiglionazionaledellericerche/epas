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
 * @author Cristian Lucchesi
 */
public class BadRequest extends Result {

  private static final long serialVersionUID = -884396263781488733L;

  private String description;

  /**
   * Costruttore di default.
   */
  public BadRequest() {
    super();
  }

  /**
   * Costruttore con description.
   */
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

  /**
   * Applica il risultato alla response.
   */
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
