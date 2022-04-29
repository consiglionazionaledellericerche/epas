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

import play.mvc.Http;
import play.mvc.results.RenderJson;

/**
 * Una risposta Json con i suoi possibili codici
 * http di risposta.
 */
public final class JsonResponse {

  private static final String SIMPLE = "HTTP %s";
  private static final String WITH_MSG = "HTTP %s - %s";

  public static final int CONFLICT = 409;

  private JsonResponse() {
  }

  public static void ok() {
    Http.Response.current().status = Http.StatusCode.OK;
    throw new RenderJson(String.format(SIMPLE, Http.StatusCode.OK));
  }

  public static void ok(String message) {
    Http.Response.current().status = Http.StatusCode.OK;
    throw new RenderJson(String.format(WITH_MSG, Http.StatusCode.OK, message));
  }

  public static void notFound() {
    Http.Response.current().status = Http.StatusCode.NOT_FOUND;
    throw new RenderJson(String.format(SIMPLE, Http.StatusCode.NOT_FOUND));
  }

  public static void notFound(String message) {
    Http.Response.current().status = Http.StatusCode.NOT_FOUND;
    throw new RenderJson(String.format(WITH_MSG, Http.StatusCode.NOT_FOUND, message));
  }

  public static void badRequest() {
    Http.Response.current().status = Http.StatusCode.BAD_REQUEST;
    throw new RenderJson(String.format(SIMPLE, Http.StatusCode.BAD_REQUEST));
  }

  public static void badRequest(String message) {
    Http.Response.current().status = Http.StatusCode.BAD_REQUEST;
    throw new RenderJson(String.format(WITH_MSG, Http.StatusCode.BAD_REQUEST, message));
  }

  public static void conflict() {
    Http.Response.current().status = CONFLICT;
    throw new RenderJson(String.format(SIMPLE, CONFLICT));
  }

  public static void conflict(String message) {
    Http.Response.current().status = CONFLICT;
    throw new RenderJson(String.format(WITH_MSG, CONFLICT, message));
  }

  public static void unauthorized(String message) {
    Http.Response.current().status = Http.StatusCode.UNAUTHORIZED;
    throw new RenderJson(String.format(WITH_MSG, Http.StatusCode.UNAUTHORIZED, message));    
  }
  
  public static void internalError(String message) {
    Http.Response.current().status = Http.StatusCode.INTERNAL_ERROR;
    throw new RenderJson(String.format(WITH_MSG, Http.StatusCode.INTERNAL_ERROR, message));    
  }
}

