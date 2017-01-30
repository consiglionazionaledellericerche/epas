package models.enumerate;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
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
      false, true),
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
  GUARDIA_MEDICA("gm", "guardiaMedica", "Guardia Medica" ,false, false),
  PERMESSO_BREVE("pb", "permessoBreve", "Permesso Breve", false, true);

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
   * @return la lista degli stamptypes attivi.
   */
  public static List<StampTypes> onlyActive() {
    return Arrays.stream(values()).filter(StampTypes::isActive).collect(Collectors.toList());
  }

  /**
   * @return true se la causale passata come parametro è attiva. False altrimenti
   */
  public static boolean isActive(final String code) {
    if (byCode(code) != null && byCode(code).isActive) {
      return true;
    }
    return false;
  }

}
