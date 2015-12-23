package models.exports;


/**
 * Oggetto per l'esportazione di un periodo di assenza per una certa persona relativa a un
 * certo codice.
 *
 * @author dario
 * @author alessandro
 * @author arianna (aggiunto id)
 */
public class PersonPeriodAbsenceCode {

  public long personId;
  public String name;
  public String surname;
  public String code;
  public String start;
  public String end;
}
