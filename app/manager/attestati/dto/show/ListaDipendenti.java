package manager.attestati.dto.show;

import com.google.common.collect.Lists;
import java.util.List;

public class ListaDipendenti {

  public String codiceSede;
  public int anno;
  public int mese;
  public List<Matricola> dipendenti = Lists.newArrayList();

  public static class Matricola {
    public String matricola;
  }

}
