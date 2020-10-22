package models.enumerate;

/**
 * Ruoli di sistema.
 * 
 * @author daniele
 * @since 30/08/16.
 */
public enum AccountRole {
  DEVELOPER,
  ADMIN,
  MISSIONS_MANAGER,
  CONTRACTUAL_MANAGER,
  //Amministratore in sola lettura
  RO_ADMIN, 
  ABSENCES_MANAGER,
  //Può leggere i dati di tutto il personale relativi
  //a presenze e assenze del personale
  PERSON_DAYS_READER
}
