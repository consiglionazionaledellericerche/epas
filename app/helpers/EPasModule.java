package helpers;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import injection.AutoRegister;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.inject.Named;
import play.Play;

@AutoRegister
public class EPasModule extends AbstractModule {

  @Provides @Named("app.instance")
  public String getAppInstance() {
    String hostname = "devel";
    try  {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      //Volutamente vuoto
    } 
    return Play.configuration.getProperty("app.instance", String.format("%s", hostname));
  }
}
