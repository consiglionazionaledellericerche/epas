import play.Play;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Classe wrapper utilizzare per avviare il server del play.
 * @author marco
 *
 */
public class FrameworkStarter {

  /**
   * Avvia il play in modalità server.
   * 
   */
  public static void main(String[] args) throws Exception {

    Play.frameworkPath = new File(System.getProperty("playFramework"));
    final Class<?> cls = Class.forName(System.getProperty("playMainClass",
        "play.server.Server"));
    final Method meth = cls.getMethod("main", String[].class);
    meth.invoke(null, (Object) args);
  }
}
