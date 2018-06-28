package controllers;

import com.google.common.base.Optional;

import dao.AbsenceDao;
import dao.TimeVariationDao;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import manager.ConsistencyManager;
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
  @Inject
  private static ConsistencyManager consistencyManager;
  @Inject
  private static TimeVariationDao timeVariationDao;

  /**
   * Action che abilita la finestra di assegnamento di una variazione.
   * @param absenceId l'id dell'assenza da compensare
   */
  public static void addVariation(long absenceId) {
    final Absence absence = absenceDao.getAbsenceById(absenceId);
    notFoundIfNull(absence);
    rules.checkIfPermitted(absence.personDay.person);
    int totalTimeRecovered = absence.timeVariations.stream().mapToInt(i -> i.timeVariation).sum();
    int difference = absence.timeToRecover - totalTimeRecovered;
    int hours = difference / 60;
    int minutes = difference % 60;
    render(absence, hours, minutes);
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
    if (absence.personDay.date.isAfter(LocalDate.now())) {
      flash.error("Non si può recuperare un'assenza %s - %s prima che questa sia sopraggiunta", 
          absence.absenceType.code, absence.absenceType.description);
      Stampings.personStamping(absence.personDay.person.id, 
          LocalDate.now().getYear(), LocalDate.now().getMonthOfYear());
    }
    TimeVariation timeVariation = timeVariationManager.create(absence, hours, minutes);
    
    timeVariation.save();
    consistencyManager.updatePersonSituation(absence.personDay.person.id, LocalDate.now());
    flash.success("Aggiornato recupero ore per assenza %s in data %s", 
        absence.absenceType.code, absence.personDay.date);
    Stampings.personStamping(absence.personDay.person.id, 
        LocalDate.now().getYear(), LocalDate.now().getMonthOfYear());
  }

  /**
   * Metodo che rimuove la variazione oraria corrispondente all'id passato come parametro.
   * @param timeVariationId l'id della variazione oraria
   */
  public static void removeVariation(long timeVariationId) {
    final TimeVariation timeVariation = timeVariationDao.getById(timeVariationId);
    notFoundIfNull(timeVariation);
    rules.checkIfPermitted(timeVariation.absence.personDay.person);

    Absence absence = timeVariation.absence;
    timeVariation.delete();
    consistencyManager.updatePersonSituation(absence.personDay.person.id, LocalDate.now());
    flash.success("Rimossa variazione oraria per il %s del giorno %s", 
        absence.absenceType.code, absence.personDay.date);
    Stampings.personStamping(absence.personDay.person.id, 
        LocalDate.now().getYear(), LocalDate.now().getMonthOfYear());

  }
}
