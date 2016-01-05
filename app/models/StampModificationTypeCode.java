package models;


public enum StampModificationTypeCode {

  /*
   id | code |                                        description
  ----+------+-------------------------------------------------------------------------------------
    1 | p    | Tempo calcolato togliendo dal tempo di lavoro la durata dell'intervallo pranzo
    2 | e    | Ora di entrata calcolata perché la durata dell'intervallo pranzo è minore del minimo
    3 | m    | Timbratura modificata dall'amministratore
    4 | x    | Ora inserita automaticamente per considerare il tempo di lavoro a cavallo
             | della mezzanotte
    5 | f    | Tempo di lavoro che si avrebbe uscendo adesso
    6 | d    | Considerato presente se non ci sono codici di assenza (orario di lavoro
             | autodichiarato)
    7 | md   | Timbratura modificata dal dipendente
   */



  FOR_DAILY_LUNCH_TIME("p"),
  FOR_MIN_LUNCH_TIME("e"),
  MARKED_BY_ADMIN("m"),
  TO_CONSIDER_TIME_AT_TURN_OF_MIDNIGHT("x"),
  ACTUAL_TIME_AT_WORK("f"),
  FIXED_WORKINGTIME("d"),
  MARKED_BY_EMPLOYEE("md");


  private String code;

  private StampModificationTypeCode(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

}
