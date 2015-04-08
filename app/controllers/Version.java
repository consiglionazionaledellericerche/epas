package controllers;

import java.io.File;
import java.io.IOException;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

@With( {Secure.class, RequestInit.class} )
public class Version extends Controller {

    public static void showVersion() {
    	String version= null;
    	try{
    	version = Files.toString(new File("conf/version.conf"), Charsets.UTF_8);
    	}
    	catch(IOException e){
    		Logger.error("File di versione 'version.conf' non trovato");
    	}
        render(version);
    }

}
