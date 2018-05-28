package controllers;

import play.mvc.Controller;
import play.mvc.With;

@With({Resecure.class})
public class Regulations extends Controller {

  public static void list() {
    todo();
  }
}
