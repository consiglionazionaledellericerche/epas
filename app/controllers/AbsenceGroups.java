package controllers;

import models.AbsenceTypeGroup;
import models.Office;
import models.enumerate.AccumulationBehaviour;

import org.testng.collections.Lists;

import play.mvc.Controller;
import play.mvc.With;

import java.util.List;

@With({Resecure.class, RequestInit.class})
public class AbsenceGroups extends Controller {

  public static void index(Office office) {
    
    
    List<AbsenceTypeGroup> groups = AbsenceTypeGroup.findAll();
    
    List<AbsenceTypeGroup> noMoreAbsencesAccepted = Lists.newArrayList();
    List<AbsenceTypeGroup> replaceCodeAndDecreaseAccumulation = Lists.newArrayList();
    List<AbsenceTypeGroup> otherGroups = Lists.newArrayList();
    
    for (AbsenceTypeGroup group : groups) {
      if (group.accumulationBehaviour.equals(AccumulationBehaviour.noMoreAbsencesAccepted)) {
        noMoreAbsencesAccepted.add(group);
      } else if (group.accumulationBehaviour
          .equals(AccumulationBehaviour.replaceCodeAndDecreaseAccumulation)) {
        replaceCodeAndDecreaseAccumulation.add(group);
      } else {
        otherGroups.add(group);
      }
      
    }
    
    render(noMoreAbsencesAccepted, replaceCodeAndDecreaseAccumulation, otherGroups);
  }
  
}
