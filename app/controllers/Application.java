package controllers;

import play.*;
import play.mvc.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

import models.*;

public class Application extends Controller {
	
	//static String mySqldriver = "com.mysql.jdbc.Driver";
	//static Connection conn = null;	

    public static void index() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
    	//Class.forName(mySqldriver).newInstance();
    	//conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/IIT","root", "orologio");
    	//FillAbsences.riempiAssenze();    	
    	//RiempiOrario.riempiOrario();
    	FillTable.riempiTabelle();
        render();
    }

}