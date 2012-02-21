package controllers;

import java.sql.SQLException;

import it.cnr.iit.epas.FromMysqlToPostgres;
import play.mvc.Controller;

public class Administration extends Controller {

    public static void index() {
        render();
    }
    
    public static void importAll() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
    	FromMysqlToPostgres.importAll();
    	renderText("Importate tute le persone dalla vecchia applicazione");
    }
    
}
