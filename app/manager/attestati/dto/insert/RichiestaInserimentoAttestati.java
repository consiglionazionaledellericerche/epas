package manager.attestati.dto.insert;

import com.google.common.collect.Lists;

import java.util.List;

public class RichiestaInserimentoAttestati {

  public int codiceSede;
  public int anno;
  public int mese;
  
  public List<ItemDipendente> dipendenti = Lists.newArrayList();
    
  public static class ItemDipendente {
    
    public int matricola;
    public Integer numBuoniPasto = null;
    public List<InsertRigaAssenza> righeAssenza = Lists.newArrayList();
    public List<InsertRigaCompetenza> righeCompetenza = Lists.newArrayList();
    public List<InsertRigaFormazione> righeFormazione = Lists.newArrayList();
  }
  
  public static class InsertRigaCompetenza {
    public String codiceCompetenza;
    public int numOre;
  }
  
  public static class InsertRigaFormazione {
    public int giornoInizio;
    public int giornoFine;
    public int numOre;
  }
  
  public static class InsertRigaAssenza {
    public String codiceAssenza;
    public int giornoInizio;
    public int giornoFine;
  }
  
  
}
