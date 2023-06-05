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

}