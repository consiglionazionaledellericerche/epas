package controllers;

import dao.AbsenceDao;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import manager.TimeVariationManager;

import models.TimeVariation;
import models.absences.Absence;

import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;



@Slf4j
@With({Resecure.class})
public class TimeVariations extends Controller {
  
  @Inject
  private static AbsenceDao absenceDao;
  @Inject
  static SecurityRules rules;
  @Inject
  private static TimeVariationManager timeVariationManager;

  /**
   * Action che abilita la finestra di assegnamento di una variazione.
   * @param absenceId l'id dell'assenza da compensare
   */
  public static void addVariation(long absenceId) {
    final Absence absence = absenceDao.getAbsenceById(absenceId);
    notFoundIfNull(absence);
    rules.checkIfPermitted(absence.personDay.person);
    render(absence);
  }
  
  /**
   * Metodo che permette il salvataggio della variazione oraria da associare ai 91CE presi in 
   * precedenza.
   * @param absenceId l'id dell'assenza da giustificare
   * @param hours le ore da restituire
   * @param minutes i minuti da restituire
   */
  public static void saveVariation(long absenceId, int hours, int minutes) {
    final Absence absence = absenceDao.getAbsenceById(absenceId);
    notFoundIfNull(absence);
    rules.checkIfPermitted(absence.personDay.person);
    TimeVariation timeVariation = timeVariationManager.create(absence, hours, minutes);
    
    timeVariation.save();
    flash.success("Aggiornato recupero ore per assenza %s in data %s", 
        absence.absenceType.code, absence.personDay.date);
    Stampings.personStamping(absence.personDay.person.id, 
        LocalDate.now().getYear(), LocalDate.now().getMonthOfYear());
  }
}
