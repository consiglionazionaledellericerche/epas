package controllers;


import com.google.common.base.Optional;
import com.google.common.base.Strings;

import controllers.Resecure.BasicAuth;

import dao.PersonDao;

import it.cnr.iit.epas.JsonStampingBinder;

import lombok.extern.slf4j.Slf4j;

import manager.AbsenceManager;
import manager.StampingManager;
import manager.cache.AbsenceTypeManager;

import models.absences.AbsenceType;
import models.exports.AbsenceFromClient;
import models.exports.StampingFromClient;

import play.data.binding.As;
import play.db.jpa.Blob;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;
import security.SecurityRules;

import javax.inject.Inject;


@With(Resecure.class)
@Slf4j
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

  public static final int CONFLICT = 409;

  /**
   * Aggiunge una timbratura ad una persona.
   */
  @BasicAuth
  public static void create(@As(binder = JsonStampingBinder.class) StampingFromClient body) {

    //rulesssssssssssssss

    // Malformed Json (400)
    if (body == null) {
      badRequest();
    }

    // Badge number not present (404)
    if (body.person == null) {
      log.warn("Non e' stato possibile recuperare la persona a cui si riferisce la timbratura,"
          + " matricolaFirma={}. Controllare il database.", body.numeroBadge);
      // non uso il notFound() per evitare di restituire il contenuto html
      response.status = Http.StatusCode.NOT_FOUND;
      return;
    }

    // Stamping already present (409)
    if (!stampingManager.createStampingFromClient(body, true)) {
      response.status = CONFLICT;
      return;
    }

    // Success (200)
    ok();
  }

  /**
   * Inserimento timbratura senza ricalcolo.
   */
  @BasicAuth
  public static void createNotRecompute(@As(binder = JsonStampingBinder.class)
      StampingFromClient body) {

    //rulesssssssssssssss

    // Malformed Json (400)
    if (body == null) {
      badRequest();
    }

    // Badge number not present (404)
    if (body.person == null) {
      log.warn("Non e' stato possibile recuperare la persona a cui si riferisce la timbratura,"
          + " matricolaFirma={}. Controllare il database.", body.numeroBadge);
      // non uso il notFound() per evitare di restituire il contenuto html
      response.status = Http.StatusCode.NOT_FOUND;
      return;
    }


    // Stamping already present (409)
    if (!stampingManager.createStampingFromClient(body, false)) {
      response.status = CONFLICT;
      return;
    }

    // Success (200)
    ok();
  }


  /**
   * Inseriemento di assenza con ricalcolo.
   */
  @BasicAuth
  public static void absence(AbsenceFromClient body) {

    if (body == null) {
      badRequest();
    }

    AbsenceType abt = absenceTypeManager.getAbsenceType(body.code);

    Optional<Integer> justifiedMinutes = Optional.<Integer>absent();
    if (!Strings.isNullOrEmpty(body.inizio) && !Strings.isNullOrEmpty(body.fine)) {
      justifiedMinutes = Optional.fromNullable(Integer.parseInt(body.fine)
          - Integer.parseInt(body.inizio));
    }

    absenceManager.insertAbsenceRecompute(body.person, body.date,
        Optional.fromNullable(body.date),
        abt, Optional.<Blob>absent(), Optional.<String>absent(), justifiedMinutes);

    renderText("ok");
  }

  /**
   * Inserimento di assenza senza ricalcolo.
   */
  @BasicAuth
  public static void absenceNotRecompute(AbsenceFromClient body) {

    if (body == null) {
      badRequest();
    }

    AbsenceType abt = absenceTypeManager.getAbsenceType(body.code);

    Optional<Integer> justifiedMinutes = Optional.<Integer>absent();
    if (!Strings.isNullOrEmpty(body.inizio) && !Strings.isNullOrEmpty(body.fine)) {
      justifiedMinutes = Optional.fromNullable(Integer.parseInt(body.fine)
          - Integer.parseInt(body.inizio));
    }

    absenceManager.insertAbsenceNotRecompute(body.person, body.date,
        Optional.fromNullable(body.date),
        abt, Optional.<Blob>absent(), Optional.<String>absent(), justifiedMinutes);

    renderText("ok");
  }


}
