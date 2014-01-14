package controllers;

import java.util.List;

import models.Office;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class})
public class Offices extends Controller {

	@Check(Security.INSERT_AND_UPDATE_OFFICES)
	public static void showOffices(){
		List<Office> officeList = Office.findAll();
		render(officeList);
	}

}
