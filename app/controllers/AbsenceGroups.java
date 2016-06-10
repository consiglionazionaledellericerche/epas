package controllers;

import models.AbsenceTypeGroup;
import models.Office;

import play.mvc.Controller;
import play.mvc.With;

import java.util.List;

@With({Resecure.class, RequestInit.class})
public class AbsenceGroups extends Controller {

  public static void index(Office office) {
    
    
    List<AbsenceTypeGroup> groups = AbsenceTypeGroup.findAll();
    render(groups);
  }
  
}
