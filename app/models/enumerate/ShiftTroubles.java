package models.enumerate;

public enum ShiftTroubles {

  //persona non assegnata all'attività
  PERSON_NOT_ASSIGNED,
  //turno non completato
  NOT_COMPLETE_SHIFT,
  //timbratura fuori dalla tolleranza per ingresso/uscita
  OUT_OF_STAMPING_TOLERANCE,
  // non c'è abbastanza tempo a lavoro per avere il turno
  NOT_ENOUGH_WORKING_TIME,
  // problemi su altro slot
  PROBLEMS_ON_OTHER_SLOT,
  // la persona è assente nel giorno
  PERSON_IS_ABSENT,
  // superato il tempo previsto per la pausa in turno
  EXCEEDED_BREAKTIME
}
