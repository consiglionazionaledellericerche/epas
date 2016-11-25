package helpers;

import play.mvc.Http;
import play.mvc.results.RenderJson;

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

}

