package models.flows.enumerate;

/**
 * Enumerato per le tipologie di stato nel flusso di approvazione delle richieste
 * di flusso informativo.
 * 
 * @author dario
 *
 */
public enum InformationRequestEventType {

  STARTING_APPROVAL_FLOW,
  OFFICE_HEAD_ACKNOWLEDGMENT,
  OFFICE_HEAD_REFUSAL,
  ADMINISTRATIVE_ACKNOWLEDGMENT,
  ADMINISTRATIVE_REFUSAL,
  DELETE,
  EPAS_REFUSAL,
  COMPLETE;
}
