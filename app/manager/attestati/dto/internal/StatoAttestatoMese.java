package manager.attestati.dto.internal;

import com.google.common.collect.Lists;

import java.util.List;

import org.joda.time.LocalDate;

public class StatoAttestatoMese {

  public int id;
  public StatoDipendenteMese dipendente;
  public boolean controlliDisattivati;
  public boolean controlliCompetenzaDisattivati;
  public boolean attestatoInizializzato;
  public String stato;
  public boolean inErrore;
  public boolean datiParttimeAssenti;

  public static class StatoDipendenteMese {
    public int matricola;
    public String nominativo;
    public Long dataAssunzione;
    public Long dataCessazione;
    public String currentCodiceOrario;

    /**
     * @return inizio contratto.
     */
    public LocalDate getBeginContract() {
      if (dataAssunzione == null) {
        return null;
      }
      return new LocalDate(dataAssunzione);
    }

    /**
     * @return inizio contratto.
     */
    public LocalDate getEndContract() {
      if (dataCessazione == null) {
        return null;
      }
      return new LocalDate(dataCessazione);
    }
  }



}
