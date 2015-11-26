package it.cnr.iit.epas;

import models.AbsenceType;

/**
 * @author dario questa classe mi occorre per ritornare un oggetto nel quale sintetizzare il
 *         risultato della chiamata della funzione di assegnamento di un codice di assenza oraria
 *         con gruppo di riferimento. In modo da stabilire se un certo codice possa essere preso o
 *         meno e, nel caso possa essere preso, se necessita anche del corrispondente codice di
 *         rimpiazzamento determinato dal raggiungimento del limite per esso previsto
 */
public class CheckMessage {
  public boolean check;
  public String message;
  public AbsenceType absenceType = null;


  public CheckMessage(boolean check, String message, AbsenceType absenceType) {
    this.check = check;
    this.message = message;
    this.absenceType = absenceType;
  }
}
