package controllers;

import play.*;
import play.mvc.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

import models.*;

public class Application extends Controller {
	
    public static void index() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {

        render();
    }

}