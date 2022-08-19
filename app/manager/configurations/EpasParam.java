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

package manager.configurations;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.configurations.EpasParam.EpasParamValueType.IpList;
import manager.configurations.EpasParam.EpasParamValueType.LocalTimeInterval;
import models.Office;
import models.Person;
import models.enumerate.BlockType;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;
import org.joda.time.format.DateTimeFormat;
import org.testng.collections.Lists;
import play.Play;

/**
 * I Parametri di ePAS.
 */
@Slf4j
public enum EpasParam {

  //#######################################
  // GENERAL PARAMS

  ABSENCES_FOR_EMPLOYEE("absences_for_employee",
      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  ENABLE_CALENDARSHIFT("enable_calendar_shift",
      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  ENABLE_CALENDAR_REPERIBILITY("enable_calendar_reperibility",
      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),
  
  ENABLE_REPERIBILITY_APPROVAL_BEFORE_END_MONTH("enable_reperibility_approval_before_end_month",
      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  ENABLE_MISSIONS_INTEGRATION("enable_mission_integration",
      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),
  

  //#######################################
  // PERSON PARAMS

  OFF_SITE_STAMPING("off_site_stamping",
      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Person.class),


  TELEWORK("telework",

      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Person.class),

  TELEWORK_STAMPINGS("telework_stampings",

      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Person.class),
  
  ENABLE_TELEWORK_STAMPINGS_FOR_WORKTIME("enable_telework_stampings_for_worktime",

      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Person.class),

  DISABLED_PERSON_PERMISSION("disabled_person_permission",

      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Person.class),

  DISABLED_RELATIVE_PERMISSION("disabled_relative_permission",

      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Person.class),
  
  SECOND_DISABLED_RELATIVE_PERMISSION("second_disabled_relative_permission",

      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Person.class),

  OFF_SITE_ABSENCE_WITH_CONVENTION("off_site_absence_with_convention",

      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Person.class),

  RIGHT_TO_STUDY("right_to_study",

      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Person.class),

  COVID_19("covid_19",

      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Person.class),
  
  AGILE_WORK("agile_work",

      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Person.class),

  ADDITIONAL_HOURS("additional_hours",
      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Person.class),
  
  PARENTAL_LEAVE_AND_CHILD_ILLNESS("parental_leave_and_child_illness",
      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Person.class),
  
  AGILE_WORK_OR_DISABLED_PEOPLE_ASSISTANCE("agile_work_or_disabled_people_assistance",
      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Person.class),
  
  SMARTWORKING("smartworking",
      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Person.class),

  
  //#######################################################################################
  // GENERAL PARAMS
  

  PEOPLE_ALLOWED_INSERT_MEDICAL_EXAM("people_allowed_insert_medical_exam",
      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),
  
  MEAL_TICKET_BLOCK_TYPE("meal_ticket_block_type",
      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.ENUM,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  DAY_OF_PATRON("dayOfPatron",
      EpasParamCategory.PERIODIC,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.DAY_MONTH,
      EpasParamValueType.formatValue(new MonthDay(1, 1)),
      Lists.newArrayList(RecomputationType.DAYS, RecomputationType.RESIDUAL_HOURS,
          RecomputationType.RESIDUAL_MEALTICKETS),
      Office.class),

  WEB_STAMPING_ALLOWED("web_stamping_allowed",
      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  ADDRESSES_ALLOWED("addresses_allowed",
      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.IP_LIST,
      EpasParamValueType.formatValue(new IpList(Lists.<String>newArrayList())),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  NUMBER_OF_VIEWING_COUPLE("number_of_viewing_couple",
      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.INTEGER,
      EpasParamValueType.formatValue(2),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  DATE_START_MEAL_TICKET("date_start_meal_ticket",
      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.LOCALDATE,
      EpasParamValueType.formatValue(new LocalDate(2014, 7, 1)),
      Lists.newArrayList(RecomputationType.RESIDUAL_MEALTICKETS),
      Office.class),

  SEND_EMAIL("send_email",
      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  SEND_ADMIN_NOTIFICATION("send_admin_notification",
      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),


  /**
   * Viene utilizzato per popolare il campo replyTo delle mail inviate dal sistema.
   */
  EMAIL_TO_CONTACT("email_to_contact",
      EpasParamCategory.GENERAL,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.EMAIL,
      EpasParamValueType.formatValue(""),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Nuovo parametro per consentire/inibire la possibilità del dipendente di gestirsi
   * l'orario di lavoro fuori sede.
   */
  WORKING_OFF_SITE("working_off_site",
      EpasParamCategory.AUTOCERTIFICATION,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Tecnici e Ricercatori possono inserirsi le ferie e riposi. 
   * Con notifica al direttore / responsabile.
   */
  TR_VACATIONS("researchers_technologists_vacations",
      EpasParamCategory.AUTOCERTIFICATION,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),
  TR_COMPENSATORY("researchers_technologists_compensatory",
      EpasParamCategory.AUTOCERTIFICATION,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Permette di abilitare/disabilitare la funzione di autocertificazione per i livelli 1-3 del
   * proprio orario di lavoro.
   */
  TR_AUTOCERTIFICATION("researchers_technologists_autocertification",
      EpasParamCategory.AUTOCERTIFICATION,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  //#######################################
  // YEARLY PARAMS

  EXPIRY_VACATION_PAST_YEAR("expiry_vacation_past_year",
      EpasParamCategory.YEARLY,
      EpasParamTimeType.YEARLY,
      EpasParamValueType.DAY_MONTH,
      EpasParamValueType.formatValue(new MonthDay(8, 31)),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  MONTH_EXPIRY_RECOVERY_DAYS_13("month_expire_recovery_days_13",
      EpasParamCategory.YEARLY,
      EpasParamTimeType.YEARLY,
      EpasParamValueType.MONTH,
      EpasParamValueType.formatValue(0),
      Lists.newArrayList(RecomputationType.RESIDUAL_HOURS),
      Office.class),


  MONTH_EXPIRY_RECOVERY_DAYS_49("month_expire_recovery_days_49",
      EpasParamCategory.YEARLY,
      EpasParamTimeType.YEARLY,
      EpasParamValueType.MONTH,
      EpasParamValueType.formatValue(3),
      Lists.newArrayList(RecomputationType.RESIDUAL_HOURS),
      Office.class),

  MAX_RECOVERY_DAYS_13("max_recovery_days_13",
      EpasParamCategory.YEARLY,
      EpasParamTimeType.YEARLY,
      EpasParamValueType.INTEGER,
      EpasParamValueType.formatValue(22),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  MAX_RECOVERY_DAYS_49("max_recovery_days_49",
      EpasParamCategory.YEARLY,
      EpasParamTimeType.YEARLY,
      EpasParamValueType.INTEGER,
      EpasParamValueType.formatValue(0),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  //#######################################
  // PERIODIC PARAMS

  MATERNITY_PERIOD("maternity_period",
      EpasParamCategory.PERIODIC,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.newArrayList(RecomputationType.DAYS, RecomputationType.RESIDUAL_HOURS,
          RecomputationType.RESIDUAL_MEALTICKETS),
      Person.class),

  HOUR_MAX_TO_CALCULATE_WORKTIME("hour_max_to_calculate_worktime",
      EpasParamCategory.PERIODIC,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.LOCALTIME,
      EpasParamValueType.formatValue(new LocalTime(5, 0)),
      Lists.newArrayList(RecomputationType.DAYS, RecomputationType.RESIDUAL_HOURS,
          RecomputationType.RESIDUAL_MEALTICKETS),
      Office.class),

  WORK_INTERVAL_MISSION_DAY("work_interval_mission_day",
      EpasParamCategory.PERIODIC,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.LOCALTIME_INTERVAL,
      EpasParamValueType
      .formatValue(new LocalTimeInterval(new LocalTime(7, 0), new LocalTime(19, 0))),
      Lists.newArrayList(RecomputationType.DAYS, RecomputationType.RESIDUAL_HOURS,
          RecomputationType.RESIDUAL_MEALTICKETS),
      Office.class),

  LUNCH_INTERVAL("lunch_interval",
      EpasParamCategory.PERIODIC,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.LOCALTIME_INTERVAL,
      EpasParamValueType
      .formatValue(new LocalTimeInterval(new LocalTime(12, 0), new LocalTime(15, 0))),
      Lists.newArrayList(RecomputationType.DAYS, RecomputationType.RESIDUAL_HOURS,
          RecomputationType.RESIDUAL_MEALTICKETS),
      Office.class),

  WORK_INTERVAL("work_interval",
      EpasParamCategory.PERIODIC,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.LOCALTIME_INTERVAL,
      EpasParamValueType
      .formatValue(new LocalTimeInterval(new LocalTime(0, 0), new LocalTime(23, 59))),
      Lists.newArrayList(RecomputationType.DAYS, RecomputationType.RESIDUAL_HOURS,
          RecomputationType.RESIDUAL_MEALTICKETS),
      Office.class),

  //#######################################
  // FLOWS PARAMS

  /**
   * Attivazione o meno della richieste di assenza generico.
   */
  ENABLE_FLOWS(
      "enable_flows",
      EpasParamCategory.FLOWS,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  SEND_FLOWS_NOTIFICATION("send_flows_notification",
      EpasParamCategory.FLOWS,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  SEND_MANAGER_NOTIFICATION_FOR_661("send_manager_notification_for_661",
      EpasParamCategory.FLOWS,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  ENABLE_37_FLOW("enable_37_flow",
      EpasParamCategory.FLOWS,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Per i livelli I-III è necessaria l'approvazione delle ferie da parte del responsabile di sede.
   */
  VACATION_REQUEST_I_III_OFFICE_HEAD_APPROVAL_REQUIRED(
      "vacation_request_i_iii_office_head_approval_required",
      EpasParamCategory.FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Per i livelli IV-VIII è necessaria l'approvazione delle ferie da parte del 
   * responsabile di sede.
   */
  VACATION_REQUEST_IV_VIII_OFFICE_HEAD_APPROVAL_REQUIRED(
      "vacation_request_iv_viii_office_head_approval_required",
      EpasParamCategory.FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Per i livelli I-III è necessaria l'approvazione delle ferie da parte dell'eventuale
   * responsabile del gruppo di lavoro.
   */
  VACATION_REQUEST_I_III_MANAGER_APPROVAL_REQUIRED(
      "vacation_request_i_iii_manager_approval_required",
      EpasParamCategory.FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Per i livelli IV-VIII è necessaria l'approvazione delle ferie da parte dell'eventuale
   * responsabile del gruppo di lavoro.
   */
  VACATION_REQUEST_IV_VIII_MANAGER_APPROVAL_REQUIRED(
      "vacation_request_iv_viii_manager_approval_required",
      EpasParamCategory.FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Per i livelli I-III è necessaria l'approvazione delle ferie da parte del responsabile di sede.
   */
  VACATION_REQUEST_MANAGER_OFFICE_HEAD_APPROVAL_REQUIRED(
      "vacation_request_manager_office_head_approval_required",
      EpasParamCategory.FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),


  /**
   * Per i livelli I-III è necessaria l'approvazione dei riposi compensativi da parte del 
   * responsabile di sede.
   */
  COMPENSATORY_REST_REQUEST_I_III_OFFICE_HEAD_APPROVAL_REQUIRED(
      "compensatory_rest_i_iii_request_office_head_approval_required",
      EpasParamCategory.FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Per i livelli IV-VIII è necessaria l'approvazione dei riposi compensativi da parte del 
   * responsabile di sede.
   */
  COMPENSATORY_REST_REQUEST_IV_VIII_OFFICE_HEAD_APPROVAL_REQUIRED(
      "compensatory_rest_iv_viii_request_office_head_approval_required",
      EpasParamCategory.FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Per i livelli I-III è necessaria l'approvazione dei riposi compensativi da parte 
   * dell'eventuale responsabile di gruppo.
   */
  COMPENSATORY_REST_REQUEST_I_III_MANAGER_APPROVAL_REQUIRED(
      "compensatory_rest_i_iii_request_manager_approval_required",
      EpasParamCategory.FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Per i livelli IV-VIII è necessaria l'approvazione dei riposi compensativi da parte 
   * dell'eventuale responsabile di gruppo.
   */
  COMPENSATORY_REST_REQUEST_IV_VIII_MANAGER_APPROVAL_REQUIRED(
      "compensatory_rest_iv_viii_request_manager_approval_required",
      EpasParamCategory.FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Per i responsabili di gruppo è necessaria l'approvazione dei riposi compensativi da parte del 
   * responsabile di sede.
   */
  COMPENSATORY_REST_REQUEST_MANAGER_OFFICE_HEAD_APPROVAL_REQUIRED(
      "compensatory_rest_manager_request_office_head_approval_required",
      EpasParamCategory.FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Per i livelli I-III è necessaria l'approvazione dei permessi personali da parte del 
   * responsabile di sede.
   */
  PERSONAL_PERMISSION_REQUEST_I_III_OFFICE_HEAD_APPROVAL_REQUIRED(
      "personal_permission_i_iii_request_office_head_approval_required",
      EpasParamCategory.FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Per i livelli IV-VIII è necessaria l'approvazione dei permessi personali da parte del 
   * responsabile di sede.
   */
  PERSONAL_PERMISSION_REQUEST_IV_VIII_OFFICE_HEAD_APPROVAL_REQUIRED(
      "personal_permission_iv_viii_request_office_head_approval_required",
      EpasParamCategory.FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Per i livelli I-III è necessaria l'approvazione dei permessi personali da parte 
   * dell'eventuale responsabile di gruppo.
   */
  PERSONAL_PERMISSION_REQUEST_I_III_MANAGER_APPROVAL_REQUIRED(
      "personal_permission_i_iii_request_manager_approval_required",
      EpasParamCategory.FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Per i livelli IV-VIII è necessaria l'approvazione dei permessi personali da parte 
   * dell'eventuale responsabile di gruppo.
   */
  PERSONAL_PERMISSION_REQUEST_IV_VIII_MANAGER_APPROVAL_REQUIRED(
      "personal_permission_iv_viii_request_manager_approval_required",
      EpasParamCategory.FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Per i responsabili di gruppo è necessaria l'approvazione dei permessi personali da parte del 
   * responsabile di sede.
   */
  PERSONAL_PERMISSION_REQUEST_MANAGER_OFFICE_HEAD_APPROVAL_REQUIRED(
      "personal_permission_manager_request_office_head_approval_required",
      EpasParamCategory.FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  //#####################################################################
  //FLOWS PARAMS COMPETENCES
  
  /**
   * Attivazione o meno delle richieste di straordinario.
   */
  ENABLE_COMPETENCE_FLOWS(
      "enable_competence_flows",
      EpasParamCategory.COMPETENCE_FLOWS,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),
  
  
  /**
   * Per i livelli IV-VIII è necessaria l'approvazione del cambio di reperibilità da parte 
   * dell'eventuale responsabile del servizio di reperibilità.
   */
  CHANGE_REPERIBILITY_REQUEST_REPERIBILITY_MANAGER_APPROVAL_REQUIRED(
      "change_reperibility_request_reperibility_manager_approval_required",
      EpasParamCategory.COMPETENCE_FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),
  
  /**
   * Per i livelli IV-VIII è necessaria l'approvazione del cambio di reperibilità 
   * da parte di un reperibile.
   */
  CHANGE_REPERIBILITY_REQUEST_EMPLOYEE_APPROVAL_REQUIRED(
      "change_reperibility_request_employee_approval_required",
      EpasParamCategory.COMPETENCE_FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),
  
  //#####################################################################
  //INFORMATION FLOWS PARAMS
  
  
  /**
   * Attivazione o meno delle richieste di straordinario.
   */
  ENABLE_INFORMATION_FLOWS(
      "enable_information_flows",
      EpasParamCategory.INFORMATION_FLOWS,
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),
  
  
  /**
   * Per i livelli I-III è necessaria la presa visione delle info di malattia da parte 
   * del responsabile di sede.
   */
  ILLNESS_INFORMATION_I_III_OFFICE_HEAD_APPROVAL_REQUIRED(
      "illness_information_i_iii_office_head_approval_required",
      EpasParamCategory.INFORMATION_FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Per i livelli IV-VIII è necessaria la presa visione delle info di malattia da parte del 
   * responsabile di sede.
   */
  ILLNESS_INFORMATION_IV_VIII_OFFICE_HEAD_APPROVAL_REQUIRED(
      "illness_information_iv_viii_office_head_approval_required",
      EpasParamCategory.INFORMATION_FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),
  
  /**
   * Per i livelli I-III è necessaria la presa visione delle info di malattia da parte 
   * dell'amministratore del personale.
   */
  ILLNESS_INFORMATION_I_III_ADMINISTRATIVE_APPROVAL_REQUIRED(
      "illness_information_i_iii_administrative_approval_required",
      EpasParamCategory.INFORMATION_FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Per i livelli IV-VIII è necessaria la presa visione delle info di malattia da parte 
   * dell'amministratore del personale. 
   */
  ILLNESS_INFORMATION_IV_VIII_ADMINISTRATIVE_APPROVAL_REQUIRED(
      "illness_information_iv_viii_administrative_approval_required",
      EpasParamCategory.INFORMATION_FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),
  
  /**
   * Per i livelli I-III è necessaria la presa visione delle info di malattia da parte 
   * dell'amministratore del personale.
   */
  ILLNESS_INFORMATION_I_III_MANAGER_APPROVAL_REQUIRED(
      "illness_information_i_iii_manager_approval_required",
      EpasParamCategory.INFORMATION_FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Per i livelli IV-VIII è necessaria la presa visione delle info di malattia da parte 
   * dell'amministratore del personale. 
   */
  ILLNESS_INFORMATION_IV_VIII_MANAGER_APPROVAL_REQUIRED(
      "illness_information_iv_viii_manager_approval_required",
      EpasParamCategory.INFORMATION_FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),
  
  /**
   * Per i livelli I-III è necessaria la presa visione del telelavoro da parte 
   * del responsabile di sede.
   */
  TELEWORK_INFORMATION_I_III_OFFICE_HEAD_APPROVAL_REQUIRED(
      "telework_information_i_iii_office_head_approval_required",
      EpasParamCategory.INFORMATION_FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Per i livelli IV-VIII è necessaria la presa visione del telelavoro da parte del 
   * responsabile di sede.
   */
  TELEWORK_INFORMATION_IV_VIII_OFFICE_HEAD_APPROVAL_REQUIRED(
      "telework_information_iv_viii_office_head_approval_required",
      EpasParamCategory.INFORMATION_FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),
  
  /**
   * Per i livelli I-III è necessaria la presa visione delle uscite di servizio da parte 
   * del responsabile di sede.
   */
  SERVICE_INFORMATION_I_III_OFFICE_HEAD_APPROVAL_REQUIRED(
      "service_information_i_iii_office_head_approval_required",
      EpasParamCategory.INFORMATION_FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Per i livelli IV-VIII è necessaria la presa visione delle uscite di servizio da parte del 
   * responsabile di sede.
   */
  SERVICE_INFORMATION_IV_VIII_OFFICE_HEAD_APPROVAL_REQUIRED(
      "service_information_iv_viii_office_head_approval_required",
      EpasParamCategory.INFORMATION_FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class),
  
  /**
   * Per i livelli IV-VIII è necessaria la presa visione delle uscite di servizio da parte 
   * del responsabile di gruppo.
   */
  SERVICE_INFORMATION_IV_VIII_MANAGER_APPROVAL_REQUIRED(
      "service_information_iv_viii_manager_approval_required",
      EpasParamCategory.INFORMATION_FLOWS,
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(true),
      Lists.<RecomputationType>newArrayList(),
      Office.class);

  public final String name;
  public final EpasParamCategory category;
  public final EpasParamTimeType epasParamTimeType;
  public final EpasParamValueType epasParamValueType;
  public final List<RecomputationType> recomputationTypes;
  public final Object defaultValue;
  public final Class<?> target;

  /**
   * Costruttore per inizializzazione configurazione parametri.
   */
  EpasParam(String name, EpasParamCategory category, EpasParamTimeType epasParamTimeType,
      EpasParamValueType epasParamValueType, Object defaultValue,
      List<RecomputationType> recomputationTypes, Class<?> target) {
    this.name = name;
    this.category = category;
    this.epasParamTimeType = epasParamTimeType;
    this.epasParamValueType = epasParamValueType;
    this.defaultValue = defaultValue;
    this.recomputationTypes = recomputationTypes;
    this.target = target;
  }

  /**
   * Verifica se il parametro è annuale.
   */
  public boolean isYearly() {
    return this.epasParamTimeType.equals(EpasParamTimeType.YEARLY);
  }

  /**
   * Verifica se il parametro è generale.
   */
  public boolean isGeneral() {
    return this.epasParamTimeType.equals(EpasParamTimeType.GENERAL);
  }


  /**
   * Verifica se il parametro è periodico.
   */
  public boolean isPeriodic() {
    return this.epasParamTimeType.equals(EpasParamTimeType.PERIODIC);
  }

  /**
   * Categorizzazione dei parametri di ePAS.
   */
  public enum EpasParamCategory {
    GENERAL, YEARLY, PERIODIC, AUTOCERTIFICATION, FLOWS, COMPETENCE_FLOWS, INFORMATION_FLOWS
  }

  /**
   * Tipologie di periodicità temporale del parametro.
   */
  public enum EpasParamTimeType {
    GENERAL, YEARLY, PERIODIC;

    /**
     * Verifica se la periodicità è GENERAL.
     */
    public boolean isGeneral() {
      return this == GENERAL;
    }
  }

  /**
   * Tipologie di ricalcolo.
   */
  public enum RecomputationType {
    DAYS, RESIDUAL_HOURS, RESIDUAL_MEALTICKETS
  }

  /**
   * Enumerato con i tipi di valori che può assumere un parametro di configurazione.
   *
   * @author Alessandro Martelli
   */
  public enum EpasParamValueType {

    LOCALTIME, LOCALTIME_INTERVAL, LOCALDATE, DAY_MONTH, MONTH,
    EMAIL, IP_LIST, INTEGER, BOOLEAN, ENUM;

    /**
     * Rappresenta un intervallo di LocalTime.
     */
    public static class LocalTimeInterval {
      public LocalTime from;
      public LocalTime to;

      /**
       * Costruttore.
       */
      public LocalTimeInterval(LocalTime from, LocalTime to) {
        this.from = from;
        this.to = to;
      }

      @Override
      public String toString() {
        return formatValue(this);
      }
    }

    /**
     * Rappresenta una lista di IP.
     */
    public static class IpList {
      public List<String> ipList;

      // TODO: validation

      /**
       * Costruttore.
       */
      public IpList(List<String> ipList) {
        this.ipList = ipList;
      }

      @Override
      public String toString() {
        return formatValue(this);
      }
    }

    public static final String DAY_MONTH_SEPARATOR = "/";
    public static final String LOCALTIME_INTERVAL_SEPARATOR = "-";
    public static final String LOCALTIME_FORMATTER = "HH:mm";
    public static final String IP_LIST_SEPARATOR = ",";

    /**
     * Converte il tipo primitivo nella formattazione string.
     *
     * @param value l'oggetto da convertire in stringa
     * @return la stringa rappresentante il valore passato
     */
    public static String formatValue(final Object value) {
      if (value instanceof String) {
        return value.toString();
      }

      if (value instanceof Boolean) {
        return value.toString();
      }

      if (value instanceof Integer) {
        return value.toString();
      }

      if (value instanceof LocalTime) {
        return ((LocalTime) value).toString(LOCALTIME_FORMATTER);
      }

      if (value instanceof LocalDate) {
        return ((LocalDate) value).toString();
      }

      if (value instanceof LocalTimeInterval) {
        return formatValue(((LocalTimeInterval) value).from)
            + LOCALTIME_INTERVAL_SEPARATOR
            + formatValue(((LocalTimeInterval) value).to);
      }

      if (value instanceof MonthDay) {
        return ((MonthDay) value).getDayOfMonth() + DAY_MONTH_SEPARATOR
            + ((MonthDay) value).getMonthOfYear();
      }

      if (value instanceof IpList) {
        return Joiner.on(IP_LIST_SEPARATOR + "\n").join(((IpList) value).ipList);
      }
      
      if (value instanceof BlockType) {
        return value.toString();
      }

      return null;
    }

    /**
     * Converte il valore in oggetto.
     */
    public static Object parseValue(final EpasParamValueType type, final String value) {
      try {
        switch (type) {
          case LOCALDATE:
            return new LocalDate(value);
          case LOCALTIME:
            return LocalTime.parse(value, DateTimeFormat.forPattern(LOCALTIME_FORMATTER));
          case LOCALTIME_INTERVAL:
            LocalTimeInterval interval = new LocalTimeInterval(
                (LocalTime) parseValue(
                    LOCALTIME, value.trim().split(LOCALTIME_INTERVAL_SEPARATOR)[0]),
                (LocalTime) parseValue(
                    LOCALTIME, value.trim().split(LOCALTIME_INTERVAL_SEPARATOR)[1]));
            if (interval.to.isBefore(interval.from)) {
              return null;
            } else {
              return interval;
            }
          case DAY_MONTH:
            return new MonthDay(
                Integer.parseInt(value.split(DAY_MONTH_SEPARATOR)[1]),
                Integer.parseInt(value.split(DAY_MONTH_SEPARATOR)[0]));
          case MONTH:
            return Integer.parseInt(value);
          case EMAIL:
            return value;
          case IP_LIST:
            return new IpList(
                Splitter.on(IP_LIST_SEPARATOR)
                .trimResults().omitEmptyStrings().splitToList(value.trim()));
          case INTEGER:
            return Integer.parseInt(value);
          case BOOLEAN:
            return Boolean.parseBoolean(value);
          case ENUM:
            return BlockType.valueOf(value);
          default:
            log.warn("Tipo non riconosciuto: {}", type);
        }
      } catch (Exception ex) {
        return null;
      }
      return null;
    }
  }

  /**
   * Verifica la lista dei cds non abilitati a visualizzare la 
   * "Presenze automatica".
   *
   * @return la lista dei cds che non sono abilitati a visualizzare la 
   *     "Presenza automatica" sui contratti dei dipendenti.
   */
  public static Set<String> revokedCdsStampProfilePermission() {
    val cds = ImmutableSet.copyOf(
        Play.configuration.getProperty("permission.revoke.contract.stamp_profile.cds", "000")
        .split(","));
    log.trace("revokedCds4StampProfilePermission = {}", cds);    
    return cds;
  }

}