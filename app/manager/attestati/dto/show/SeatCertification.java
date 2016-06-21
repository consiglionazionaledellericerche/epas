package manager.attestati.dto.show;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Json sulla situazione della persona per il mese specificato da Attestati.
 *
 * @author alessandro
 */
public class SeatCertification {

  public int codiceSede;
  public int anno;
  public int mese;
  public List<PersonCertification> dipendenti = Lists.newArrayList();

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(SeatCertification.class)
        .add("codiceSede", codiceSede)
        .add("anno", anno)
        .add("mese", mese)
        .add("dipendenti", dipendenti)
        .toString();
  }

  public static class PersonCertification {
    public int matricola;
    public boolean validato;
    public int numBuoniPasto;
    public List<RigaAssenza> righeAssenza = Lists.newArrayList();
    public List<RigaCompetenza> righeCompetenza = Lists.newArrayList(); //??
    public List<RigaFormazione> righeFormazione = Lists.newArrayList();

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(PersonCertification.class)
          .add("matricola", matricola)
          .add("validato", validato)
          .add("numBuoniPasto", numBuoniPasto)
          .add("righeAssenza", righeAssenza)
          .toString();
    }
  }

}
