package controllers;

import play.*;
import play.mvc.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

import models.*;

@With(Secure.class)
public class Application extends Controller {
	
    public static void index() {
        render();
    }

}