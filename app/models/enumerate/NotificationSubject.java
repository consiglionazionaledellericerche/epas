/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package models.enumerate;

import com.google.common.collect.Maps;
import java.util.Map;
import models.Competence;
import models.Person;
import models.Stamping;
import models.absences.Absence;
import models.flows.AbsenceRequest;
import models.flows.CompetenceRequest;
import models.informationrequests.IllnessRequest;
import models.informationrequests.ParentalLeaveRequest;
import models.informationrequests.ServiceRequest;
import models.informationrequests.TeleworkRequest;
import play.mvc.Router;

/**
 * Notification subject types.
 *
 * @author Marco Andrieni
 */
public enum NotificationSubject {
  
  /*
   * Notifiche di sistema.
   */
  SYSTEM,

  /*
   * Commento.
   */
  COMMENT,

  /*
   * Messaggio.
   */
  MESSAGE,

  /*
   * Notifiche relative a timbrature inserite o modificate
   */
  STAMPING,

  /*
   * Notifiche relative alle assenze inserite o modificate
   */
  ABSENCE,

  /*
   * Notifiche relative alle competenze inserite o modificate
   */
  COMPETENCE,

  /**
   * Notifiche per i flussi di lavoro. 
   */
  ABSENCE_REQUEST,

  /*
   * Notifiche relative ai flussi di lavoro per competenza.
   */
  COMPETENCE_REQUEST,
  
  /*
   * Notifica per malattia
   */
  ILLNESS_INFORMATION,
  
  /*
   * Notifica per uscita di servizio
   */
  SERVICE_INFORMATION,
  
  /*
   * Notifica per telelavoro
   */
  TELEWORK_INFORMATION,
  
  /*
   * Notifica per congedo parentale per il padre
   */
  PARENTAL_LEAVE_INFORMATION,


  /*
   * Notifiche per i cambi di assegnazione ad un ufficio.
   */
  PERSON_HAS_CHANGED_OFFICE;

  private String toUrl(String action, Map<String, Object> params) {
    if (params == null) {
      return Router.reverse(action).url;
    } else {
      return Router.reverse(action, params).url;
    }
  }

  /**
   * Url della show dell'oggetto riferito nella notifica.
   *
   * @param referenceId id dell'oggetto
   * @return url con la show dell'oggetto
   */
  public String toUrl(Long referenceId) {
    final Map<String, Object> params = Maps.newHashMap();
    switch (this) {
      case COMMENT:
        params.put("id", referenceId);
        return toUrl("Comments.show", params);
      case MESSAGE:
        params.put("id", referenceId);
        return toUrl("Messages.show", params);
      case STAMPING:
        final Stamping stamping = Stamping.findById(referenceId);
        if (stamping == null) {
          return null;
        }
        params.put("month", stamping.getDate().getMonthOfYear());
        params.put("year", stamping.getDate().getYear());
        params.put("personId", stamping.getPersonDay().getPerson().id);
        return toUrl("Stampings.personStamping", params);
      case ABSENCE:
        final Absence absence = Absence.findById(referenceId);
        if (absence == null) {
          return null;
        }
        params.put("month", absence.getPersonDay().getDate().getMonthOfYear());
        params.put("year", absence.getPersonDay().getDate().getYear());
        params.put("personId", absence.getPersonDay().getPerson().id);
        return toUrl("Stampings.personStamping", params);
      case COMPETENCE:
        final Competence competence = Competence.findById(referenceId);
        if (competence == null) {
          return null;
        }
        params.put("month", competence.getMonth());
        params.put("year", competence.getYear());
        params.put("officeId", competence.getPerson().getOffice().id);
        params.put("competenceCodeId", competence.getCompetenceCode().id);
        return toUrl("Competences.showCompetences", params);
      case ABSENCE_REQUEST:
        final AbsenceRequest absenceRequest = AbsenceRequest.findById(referenceId);
        params.put("id", absenceRequest.id);
        params.put("type", absenceRequest.getType());
        return toUrl("AbsenceRequests.show", params);
      case COMPETENCE_REQUEST:
        final CompetenceRequest competenceRequest = CompetenceRequest.findById(referenceId);
        params.put("id", competenceRequest.id);
        params.put("type", competenceRequest.getType());
        return toUrl("CompetenceRequests.show", params);
      case ILLNESS_INFORMATION:
        final IllnessRequest illnessRequest = IllnessRequest.findById(referenceId);
        params.put("id", illnessRequest.id);
        params.put("type", illnessRequest.getInformationType());
        return toUrl("InformationRequests.show", params);
      case SERVICE_INFORMATION:
        final ServiceRequest serviceRequest = ServiceRequest.findById(referenceId);
        params.put("id", serviceRequest.id);
        params.put("type", serviceRequest.getInformationType());
        return toUrl("InformationRequests.show", params);
      case TELEWORK_INFORMATION:
        final TeleworkRequest teleworkRequest = TeleworkRequest.findById(referenceId);
        params.put("id", teleworkRequest.id);
        params.put("type", teleworkRequest.getInformationType());
        return toUrl("InformationRequests.show", params);
      case PERSON_HAS_CHANGED_OFFICE:
        //Se non c'è riferimento alla persona allora vuol dire che non è 
        //più gestita dal precedente ufficio.
        if (referenceId == null) {
          return null;
        }
        final Person person = Person.findById(referenceId);        
        if (person == null) {
          return null;
        }
        params.put("personId", person.id);
        return toUrl("Persons.edit", params);
      // case SYSTEM:
      case PARENTAL_LEAVE_INFORMATION:
        final ParentalLeaveRequest parentalLeaveRequest = 
            ParentalLeaveRequest.findById(referenceId);
        params.put("id", parentalLeaveRequest.id);
        params.put("type", parentalLeaveRequest.getInformationType());
        return toUrl("InformationRequests.show", params);
      default:
        throw new IllegalStateException("unknown target: " + this.name());
    }
  }


  public boolean isRedirect() {
    return this != SYSTEM;
  }
}
