package controllers;


import com.google.common.base.Optional;
import com.google.common.base.Strings;

import controllers.Resecure.BasicAuth;

import dao.PersonDao;

import it.cnr.iit.epas.JsonStampingBinder;

import manager.AbsenceManager;
import manager.StampingManager;
import manager.cache.AbsenceTypeManager;

import models.AbsenceType;
import models.exports.AbsenceFromClient;
import models.exports.StampingFromClient;

import play.data.binding.As;
import play.db.jpa.Blob;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

import javax.inject.Inject;


@With({Resecure.class, RequestInit.class})
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
  public static String create(@As(binder = JsonStampingBinder.class) StampingFromClient body) {

    //rulesssssssssssssss

    if (body == null) {
      badRequest();
    }

    if (stampingManager.createStampingFromClient(body, true)) {
      return "OK";
    }

    return "KO";
  }

  /**
   * Inserimento timbratura senza ricalcolo.
   */
  @BasicAuth
  public static String createNotRecompute(@As(binder = JsonStampingBinder.class) 
      StampingFromClient body) {

    //rulesssssssssssssss

    if (body == null) {
      badRequest();
    }

    if (stampingManager.createStampingFromClient(body, false)) {
      return "OK";
    }

    return "KO";
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
