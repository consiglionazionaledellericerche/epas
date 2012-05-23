package controllers;

import java.sql.SQLException;

import it.cnr.iit.epas.FromMysqlToPostgres;
import it.cnr.iit.epas.PopulatePersonDay;
import play.mvc.Controller;
import play.mvc.results.RenderText;

public class Administration extends Controller {

    public static void index() {
        render();
    }
    
    public static void importAll() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
    	FromMysqlToPostgres.importAll();
    	FromMysqlToPostgres.importNotInOldDb();
    	PopulatePersonDay.fillWorkingTimeTypeDays();
    	renderText("Importate tutte le persone dalla vecchia applicazione + aggiunti i workingtimetypeday");
    }
    
    /**
     * aggiunto metodo di popolamento iniziale del personDay di modo da rendere tutte le informazioni su ciascuna persona persistenti
     * sul db già al momento della prima visualizzazione.
     */
    public static void populatePersonDay(){
    	PopulatePersonDay.PopulatePersonDayForOne();
    	renderText("Calcolate tutte le informazioni su tempi di lavoro, progressivo e differenza per i person day di Cristian Lucchesi");
    }
    
    /**
     * metodo per aggiungere i workingTimeTypeDay per il workingTimeType normale-mod 
     */
    public static void addWorkingTimeType(){
    	PopulatePersonDay.fillWorkingTimeTypeDays();
    	renderText("Aggiunti days per il working time type normal-mod");
    }
    
}
