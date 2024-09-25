package controllers;

import lombok.extern.slf4j.Slf4j;
import play.mvc.Controller;
import play.mvc.With;

@Slf4j
@With({Resecure.class})
public class Instances extends Controller {

  public static void importInstance() {
    render();
  }
}
