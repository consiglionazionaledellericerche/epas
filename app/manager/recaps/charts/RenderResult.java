package manager.recaps.charts;

import models.enumerate.CheckType;

import org.joda.time.LocalDate;

/**
 * classe privata per la restituzione del risultato relativo al processo di controllo sulle assenze
 * dell'anno passato.
 **/
public class RenderResult {
  public String line;
  public Integer matricola;
  public String nome;
  public String cognome;
  public String codice;
  public LocalDate data;
  public boolean check;
  public String message;
  public String codiceInAnagrafica;
  public CheckType type;

  /**
   * Costruttore.
   */
  public RenderResult(
      String line, Integer matricola, String nome, String cognome, String codice,
      LocalDate data, boolean check, String message, String codiceInAnagrafica, CheckType type) {
    this.line = line;
    this.matricola = matricola;
    this.nome = nome;
    this.codice = codice;
    this.cognome = cognome;
    this.data = data;
    this.check = check;
    this.message = message;
    this.codiceInAnagrafica = codiceInAnagrafica;
    this.type = type;

  }
}
