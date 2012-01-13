package controllers;

import it.cnr.iit.epas.FromMysqlToPostgres;

import java.sql.SQLException;

import play.mvc.*;

public class Administration extends Controller {

    public static void index() {
        render();
    }
    
    
//    public static void migrateDb(){
//        	try {        		
//				FromMysqlToPostgres.fillTables();
//				FromMysqlToPostgres.fillOtherTables();
//			} catch (InstantiationException e) {
//				e.printStackTrace();
//				render("migration_error");
//			} catch (IllegalAccessException e) {
//				render("migration_error");
//				e.printStackTrace();
//			} catch (ClassNotFoundException e) {
//				render("migration_error");
//				e.printStackTrace();
//			} catch (SQLException e) {
//				render("migration_error");
//				e.printStackTrace();
//			}
//    	render();
//    };
}
