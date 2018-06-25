package models.flows.enumerate;

/**
 * Tipologie di eventi sulli richieste di assenza.
 * 
 * @author cristian
 *
 */
public enum AbsenceRequestEventType {
  EMPLOYEE_REQUEST,
  MANAGER_APPROVAL,
  MANAGER_REFUSAL,
  ADMINISTRATIVE_APPROVAL,
  ADMINISTRATIVE_REFUSAL,
  OFFICE_HEAD_APPROVAL,
  OFFICE_HEAD_REFUSAL,
  EXPIRING;
}
