package controllers;

import java.sql.SQLException;

import it.cnr.iit.epas.FromMysqlToPostgres;
import it.cnr.iit.epas.PopulatePersonDay;
import play.mvc.Controller;

public class Administration extends Controller {

    public static void index() {
        render();
    }
    
    public static void importAll() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
    	FromMysqlToPostgres.importAll();
    	FromMysqlToPostgres.importNotInOldDb();
    	renderText("Importate tutte le persone dalla vecchia applicazione");
    }
    
    /**
     * aggiunto metodo di popolamento iniziale del personDay di modo da rendere tutte le informazioni su ciascuna persona persistenti
     * sul db già al momento della prima visualizzazione.
     */
    public static void populatePersonDay(){
    	PopulatePersonDay.PopulatePersonDayForOne();
    	renderText("Calcolate tutte le informazioni su tempi di lavoro, progressivo e differenza per i person day di Cristian Lucchesi");
    }
    
}
