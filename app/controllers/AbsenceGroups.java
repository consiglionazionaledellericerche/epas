package controllers;

import models.Office;

import play.mvc.Controller;
import play.mvc.With;

@With({Resecure.class, RequestInit.class})
public class AbsenceGroups extends Controller {

  public static void index(Office office) {
    
    
    
    render();
  }
  
}
