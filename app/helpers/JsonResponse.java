package helpers;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;

import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.RenderJson;
import play.mvc.results.Result;

public class JsonResponse {
	
	private final static String NOT_FOUND = "HTTP 404";
	private final static String BAD_REQUEST = "HTTP 400";
	
    public static void notFound() {
	    Http.Response.current().status = Http.StatusCode.NOT_FOUND;
        throw new RenderJson(NOT_FOUND);
    }
    
    public static void notFound(String message) {
	    Http.Response.current().status = Http.StatusCode.NOT_FOUND;
        throw new RenderJson(NOT_FOUND + " - " + message);
    }
  
    public static void badRequest() {
    	Http.Response.current().status = Http.StatusCode.BAD_REQUEST;
        throw new RenderJson(BAD_REQUEST);
    }
    
    public static void badRequest(String message) {
    	Http.Response.current().status = Http.StatusCode.BAD_REQUEST;
        throw new RenderJson(BAD_REQUEST + " - " + message);
    }

}

