package controllers;

import controllers.shib.Check;
import controllers.shib.Shibboleth;
import play.mvc.Controller;
import play.mvc.With;

@With(Shibboleth.class)
public class TestShib extends Controller{
	
	@Check(Security.INSERT_AND_UPDATE_STAMPING)
	public static void redirect(){
		render("@Application.indexAdmin");
	}
}
