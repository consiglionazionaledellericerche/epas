package models.enumerate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * Le causali delle timbrature.
 * @author cristian.
 */
@Getter
public enum StampTypes {

  /*
   id |          code          |       description        | identifier
  ----+------------------------+--------------------------+------------
    1 | motiviDiServizio       | Motivi di servizio       | s
    2 | visitaMedica           | Visita Medica            | vm
    3 | permessoSindacale      | Permesso sindacale       | ps
    4 | incaricoDiInsegnamento | Incarico di insegnamento | is
    5 | dirittoAlloStudio      | Diritto allo studio      | das
    6 | motiviPersonali        | Motivi personali         | mp
    7 | reperibilita           | Reperibilità             | r
    8 | intramoenia            | Intramoenia              | i
    9 | guardiaMedica          | Guardia Medica           | gm
   */

  MOTIVI_DI_SERVIZIO("s", "motiviDiServizio", "Motivi di servizio", false, true),
  MOTIVI_DI_SERVIZIO_FUORI_SEDE("sf", "servizioFuoriSede", "Motivi di Servizio Fuori Sede",
      false, false),
  LAVORO_FUORI_SEDE("lfs", "lavoroFuoriSede", "Lavoro fuori sede", true, true),
  PAUSA_PRANZO("pr", "pausaPranzo", "Pausa Pranzo", true, true),
  
  VISITA_MEDICA("vm", "visitaMedica", "Visita Medica", false, false),
  PERMESSO_SINDACALE("ps", "permessoSindacale", "Permesso sindacale", false, false),
  INCARICO_DI_INSEGNAMENTO("is", "incaricoDiInsegnamento", "Incarico di insegnamento", false, 
      false),
  DIRITTO_ALLO_STUDIO("das", "dirittoAlloStudio", "Diritto allo studio", false, false),
  MOTIVI_PERSONALI("mp", "motiviPersonali", "Motivi personali", false, false),
  REPERIBILITA("r", "reperibilita", "Reperibilità ", false, false),
  INTRAMOENIA("i", "intramoenia", "Intramoenia", false, false),
  GUARDIA_MEDICA("gm", "guardiaMedica", "Guardia Medica",false, false),
  PERMESSO_BREVE("pb", "permessoBreve", "Permesso Breve", false, true),
  INIZIO_TELELAVORO("it", "inizioTelelavoro", "Inizio telelavoro", false, true),
  FINE_TELELAVORO("ft", "fineTelelavoro", "Fine telelavoro", false, true),
  INIZIO_PRANZO_TELELAVORO("ipt", "inizioPranzoTelelavoro", "Inizio pranzo telelavoro", false, true),
  FINE_PRANZO_TELELAVORO("fpt", "finePranzoTelelavoro", "Fine pranzo telelavoro", false, true),
  INIZIO_INTERRUZIONE("ii", "inizioInterruzione", "Inizio interruzione", false, true),
  FINE_INTERRUZIONE("fi", "fineInterruzione", "Fine interruzione", false, true);

  private String identifier;
  private String code;
  private String description;
  //se true la timbratura viene considerata per il calcolo della pausa pranzo
  private boolean gapLunchPairs;  
  private boolean isActive;

  StampTypes(String identifier, String code, String description, boolean gapLunchPair, 
      boolean isActive) {
    this.identifier = identifier;
    this.code = code;
    this.description = description;
    this.gapLunchPairs = gapLunchPair;
    this.isActive = isActive;
  }

  /**
   * La causale corrispondente alla stringa passata come parametro.
   * @param code il codice proveniente dal json delle timbrature.
   * @return Lo stampType corrispondente se esiste.
   */
  public static StampTypes byCode(final String code) {
    for (StampTypes value : values()) {
      if (value.code.equalsIgnoreCase(code)) {
        return value;
      }
    }
    return null;
  }

  /**
   * La causale corrispondente all'identificativo passato.
   * @param identifier La Stringa identificativa dello Stamptype.
   * @return Lo stampType corrispondente se esiste.
   */
  public static StampTypes byIdentifier(final String identifier) {
    for (StampTypes value : values()) {
      if (value.identifier.equalsIgnoreCase(identifier)) {
        return value;
      }
    }
    return null;
  }

  /**
   * La lista delle causali attive.
   * @return la lista degli stamptypes attivi.
   */
  public static List<StampTypes> onlyActive() {
    return Arrays.stream(values())
        .filter(StampTypes::isActive).collect(Collectors.toList());
  }

  /**
   * Lista delle stampTypes attive ma senza il lavoro fuori sede.
   * @return lista delle stampTypes attive ma senza il lavoro fuori sede.
   */
  public static List<StampTypes> onlyActiveWithoutOffSiteWork() {
    return onlyActive().stream().filter(StampTypes::isNotOffSiteWork).collect(Collectors.toList());
  }
  
  /**
   * La lista delle stampTypes associatbili a timbrature in telelavoro.
   * @return la lista di stampTypes associabili a timbrature in telelavoro.
   */
  public static List<StampTypes> onlyActiveInTelework() {
    return onlyActive().stream().filter(StampTypes::ableForTeleworkStamping)
        .collect(Collectors.toList());
  }
  
  /**
   * 
   * @return la lista di stampTypes di inizio e fine lavoro in telelavoro.
   */
  public static List<StampTypes> beginEndTelework() {
    return onlyActive().stream().filter(StampTypes::isBeginEndTelework)
        .collect(Collectors.toList());
  }
  
  /**
   * 
   * @return la lista di stampTypes di inizio e fine pranzo in telelavoro.
   */
  public static List<StampTypes> beginEndMealInTelework() {
    return onlyActive().stream().filter(StampTypes::isBeginEndMealInTelework)
        .collect(Collectors.toList());
  }
  
  /**
   * 
   * @return la lista di stampTypes di inizio e fine interruzione in telelavoro.
   */
  public static List<StampTypes> beginEndInterruptionInTelework() {
    return onlyActive().stream().filter(StampTypes::isBeginEndInterruptionInTelework)
        .collect(Collectors.toList());
  }
  
  /**
   * Verifica se una causale è attiva a partire dal suo codice passato come parametro.
   * @return true se la causale passata come parametro è attiva. False altrimenti
   */
  public static boolean isActive(final String code) {
    if (byCode(code) != null && byCode(code).isActive) {
      return true;
    }
    return false;
  }
  
  /**
   * Controlla se questa causale è lavoro fuori sede.
   * @return true se la timbratura corrisponde ad un timbrature per lavoro effettuato fuori sede
   */
  public boolean isOffSiteWork() {
    return this == LAVORO_FUORI_SEDE;
  }
  
  /**
   * Controlla se questa causale non è lavoro fuori sede.
   * @return
   */
  public boolean isNotOffSiteWork() {
    return this != LAVORO_FUORI_SEDE;
  }
  
  /**
   * Controlla se la causale è abilitata per le timbrature in telelavoro.
   * @return le causali associabili a timbrature in telelavoro
   */
  public boolean ableForTeleworkStamping() {
    return this == FINE_INTERRUZIONE || this == FINE_PRANZO_TELELAVORO 
        || this == FINE_TELELAVORO || this == INIZIO_INTERRUZIONE 
        || this == INIZIO_PRANZO_TELELAVORO || this == INIZIO_TELELAVORO;
  }
  
  public boolean isBeginEndTelework() {
    return this == INIZIO_TELELAVORO || this == FINE_TELELAVORO;
  }
  
  public boolean isBeginEndMealInTelework() {
    return this == INIZIO_PRANZO_TELELAVORO || this == FINE_PRANZO_TELELAVORO;
  }
  
  public boolean isBeginEndInterruptionInTelework() {
    return this == INIZIO_INTERRUZIONE || this == FINE_INTERRUZIONE;
  }

}
