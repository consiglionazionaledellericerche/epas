package controllers;

import dao.ContractualClauseDao;
import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.With;

@With({Resecure.class})
public class ContractualClauses extends Controller {

  @Inject
  ContractualClauseDao contractualClauseDao;
  
  public static void list() {
    
  }
}
