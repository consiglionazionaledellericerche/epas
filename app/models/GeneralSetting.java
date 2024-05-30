/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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

package models;

import javax.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import models.base.BaseModel;
import org.hibernate.envers.Audited;

/**
 * Configurazione generale di ePAS.
 *
 */
@Getter
@Setter
@ToString
@Entity
@Audited
public class GeneralSetting extends BaseModel {

  private static final long serialVersionUID = 881278299637007974L;

  private boolean regulationsEnabled = false;
  
  //Cookie policy
  private boolean cookiePolicyEnabled = false;
  private String cookiePolicyContent;
  
  // Parametri gestione anagrafica
  
  private boolean personCreationEnabled = true;
  private boolean syncBadgesEnabled = false;
  private boolean syncOfficesEnabled = false;
  private boolean syncPersonsEnabled = false;
  
  // Fine parametri gestione anagrafica
  
  // Parametri gestione invio dati a fine mese
  
  private boolean onlyMealTicket = false;

  // Fine parametri gestione invio dati a fine mese
  
  // Parametri gestione codici di competenza turno
  
  private String startDailyShift = "6:00";
  
  private String endDailyShift = "19:00";
  
  private String startNightlyShift = "19:00";
  
  private String endNightlyShift = "6:00";
  // Fine parametri gestione codici di competenza turno
  
  // Parametri gestione gruppi
  
  private boolean handleGroupsByInstitute = true;
  
  private boolean enableDailyPresenceForManager = true;
  
  // Fine parametri gestione gruppi
  
  // Parametri gestione giorni di turno
  
  private boolean saturdayHolidayShift = true;
  
  private boolean roundingShiftQuantity = false;
  
  private boolean enableUniqueDailyShift = true;
  
  private boolean holidayShiftInNightToo = false;
    
  // Fine parametri gestione giorni di turno
  
  // Parametri visualizzazione richieste di flusso
  
  private boolean enableIllnessFlow = false;
  
  // Parametro per abilitazione visualizzazione parametro covid19
  private boolean enableAutoconfigCovid19 = false;
  
  private boolean enableAutoconfigSmartworking = false;
  
  /**
   * Numero massimo di giorni nel passato per cui è possibile
   * inserire timbrature via REST.
   */
  private int maxDaysInPastForRestStampings = 90;
  
  /**
   * Numero massimo di giorni nel passato per cui è possibile
   * inserire assenze via REST e/o via UI.
   */
  private int maxMonthsInPastForAbsences = 12;
  
  /*
   * Indica se è possibile o meno configurare per i livelli I-III
   * la richiesta di approvazione ferie da parte di un responsabile di 
   * gruppo o del responsabile di sede. 
   */
  private boolean enableAbsenceTopLevelAuthorization = true;

  /*
   * Indica se condizionare l'inserimento manuale di una persona in anagrafica
   */
  private boolean warningInsertPerson = true;
  
  /*
   * Indica se mostrare o meno i parametri di configurazione per il flusso di richiesta
   * degli straordinari
   */
  private boolean showOvertimeRequest = false;

  /**
   * Indica se è attiva l'integrazione con epas-service.
   */
  private boolean epasServiceEnabled = false;

  /**
   * URL base servizio epas-service.
   */
  private String epasServiceUrl = "http://epas-service:8080/"; 

  /**
   * Indica se è attiva l'integrazione con epas-delpdesk-service.
   */
  private boolean epasHelpdeskServiceEnabled = false;

  /**
   * URL base servizio epas-helpdesk-service.
   */
  private String epasHelpdeskServiceUrl = "http://epas-helpdesk-service:8080/"; 
  
  /**
   * Indica se è attivo il monte ore di straordinario per il dipendente
   */
  private boolean enableOvertimePerPerson = true;
  
  /**
   * Indica se si può utilizzare o meno la richiesta preventiva di straordinario
   */
  private boolean enableOvertimeRequestInAdvance = false;
 
  /**
   * Indica se il servizio Attestati del CNR deve utilizzare l'SSO CNR per il rilascio del 
   * token oppure il token JWT direttamente rilasciato da Attestati.
   */
  private boolean enableSsoForAttestati = false;
  
  /**
   * Timeout in secondi per le chiamate REST effettuate ad Attestati.
   */
  private int timeoutAttestati = 60;


}