package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import controllers.Resecure.BasicAuth;
import dao.PersonDao;
import helpers.JsonResponse;
import it.cnr.iit.epas.JsonStampingBinder;
import javax.inject.Inject;
import manager.AbsenceManager;
import manager.StampingManager;
import manager.cache.AbsenceTypeManager;
import models.absences.AbsenceType;
import models.exports.AbsenceFromClient;
import models.exports.StampingFromClient;
import play.data.binding.As;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;


@With(Resecure.class)
public class StampingsFromClient extends Controller {

  @Inject
  static SecurityRules rules;
  @Inject
  static StampingManager stampingManager;
  @Inject
  static AbsenceTypeManager absenceTypeManager;
  @Inject
  static AbsenceManager absenceManager;
  @Inject
  static PersonDao personDao;

  /**
   * Aggiunge una timbratura ad una persona.
   */
  @BasicAuth
  public static void create(@As(binder = JsonStampingBinder.class) StampingFromClient body) {

    // Malformed Json (400)
    if (body == null) {
      JsonResponse.badRequest();
    }

    // Badge number not present (404)
    if (!stampingManager.linkToPerson(body).isPresent()) {
      JsonResponse.notFound();
    }

    // Stamping already present (409)
    if (!stampingManager.createStampingFromClient(body, true)) {
      JsonResponse.conflict();
    }

    // Success (200)
    JsonResponse.ok();
  }

  /**
   * Inserimento timbratura senza ricalcolo.
   */
  @BasicAuth
  public static void createNotRecompute(@As(binder = JsonStampingBinder.class)
      StampingFromClient body) {

    // Malformed Json (400)
    if (body == null) {
      JsonResponse.badRequest();
    }

    // Badge number not present (404)
    if (!stampingManager.linkToPerson(body).isPresent()) {
      JsonResponse.badRequest();
    }

    // Stamping already present (409)
    if (!stampingManager.createStampingFromClient(body, false)) {
      JsonResponse.conflict();
    }

    // Success (200)
    JsonResponse.ok();
  }


  /**
   * Vecchio metodo che permetteva la verifica dell'assenza a partire dal DTO passato.
   * @deprecated utilizzare rest.Absences.insertAbsence
   *     Inserimento di assenza con ricalcolo.
   */
  @Deprecated
  @BasicAuth
  public static void absence(AbsenceFromClient body) {

    if (body == null) {
      badRequest();
      return;
    }

    AbsenceType abt = absenceTypeManager.getAbsenceType(body.code);

    Optional<Integer> justifiedMinutes = Optional.absent();
    if (!Strings.isNullOrEmpty(body.inizio) && !Strings.isNullOrEmpty(body.fine)) {
      justifiedMinutes = Optional.fromNullable(Integer.parseInt(body.fine)
          - Integer.parseInt(body.inizio));
    }

    absenceManager.insertAbsenceRecompute(body.person, body.date,
        Optional.fromNullable(body.date), Optional.absent(),
        abt, Optional.absent(), Optional.absent(), justifiedMinutes);

    renderText("ok");
  }

  /**
   * Vecchio metodo che permetteva l'inserimento dell'assenza a partire dal DTO passato
   * senza effettuare ricalcoli.
   * @deprecated utilizzare rest.Absences.insertAbsence
   *     Inserimento di assenza senza ricalcolo.
   */
  @Deprecated
  @BasicAuth
  public static void absenceNotRecompute(AbsenceFromClient body) {

    if (body == null) {
      badRequest();
    }

    AbsenceType abt = absenceTypeManager.getAbsenceType(body.code);

    Optional<Integer> justifiedMinutes = Optional.absent();
    if (!Strings.isNullOrEmpty(body.inizio) && !Strings.isNullOrEmpty(body.fine)) {
      justifiedMinutes = Optional.fromNullable(Integer.parseInt(body.fine)
          - Integer.parseInt(body.inizio));
    }

    absenceManager.insertAbsenceNotRecompute(body.person, body.date,
        Optional.fromNullable(body.date), Optional.absent(),
        abt, Optional.absent(), Optional.absent(), justifiedMinutes);

    renderText("ok");
  }

}
