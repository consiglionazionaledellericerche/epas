package controllers;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import play.Logger;
import play.mvc.*;

@With( {Secure.class, NavigationMenu.class} )
public class Faq extends Controller {

    public static void faq() {
        render();
    }

}
