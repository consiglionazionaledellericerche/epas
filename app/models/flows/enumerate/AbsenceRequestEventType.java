package models.flows.enumerate;

/**
 * Tipologie di eventi sulle richieste di assenza.
 * 
 * @author cristian
 *
 */
public enum AbsenceRequestEventType {
  STARTING_APPROVAL_FLOW,
  MANAGER_APPROVAL,
  MANAGER_REFUSAL,
  ADMINISTRATIVE_APPROVAL,
  ADMINISTRATIVE_REFUSAL,
  OFFICE_HEAD_APPROVAL,
  OFFICE_HEAD_REFUSAL,
  COMPLETE,
  EPAS_REFUSAL,
  DELETE,
  EXPIRING;
}
