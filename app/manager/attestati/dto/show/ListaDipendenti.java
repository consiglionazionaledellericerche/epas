package manager.attestati.dto.show;

import com.beust.jcommander.internal.Lists;

import manager.attestati.dto.show.SeatCertification.PersonCertification;

import java.util.List;

public class ListaDipendenti {

  public int codiceSede;
  public int anno;
  public int mese;
  public List<Matricola> dipendenti = Lists.newArrayList();
  
  public static class Matricola {
    public int matricola;
  }
  
}
