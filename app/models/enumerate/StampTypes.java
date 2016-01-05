package models.enumerate;

import lombok.Getter;

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

  MOTIVI_DI_SERVIZIO("s", "motiviDiServizio","Motivi di servizio"),
  VISITA_MEDICA("vm", "visitaMedica","Visita Medica"),
  PERMESSO_SINDACALE("ps", "permessoSindacale","Permesso sindacale"),
  INCARICO_DI_INSEGNAMENTO("is", "incaricoDiInsegnamento","Incarico di insegnamento"),
  DIRITTO_ALLO_STUDIO("das", "dirittoAlloStudio","Diritto allo studio"),
  MOTIVI_PERSONALI("mp", "motiviPersonali","Motivi personali"),
  REPERIBILITA("r", "reperibilita","Reperibilità "),
  INTRAMOENIA("i", "intramoenia","Intramoenia"),
  GUARDIA_MEDICA("gm", "guardiaMedica","Guardia Medica"),
  PAUSA_PRANZO("pr", "pausaPranzo","Pausa Pranzo");

  private String identifier;
  private String code;
  private String description;

  StampTypes(String identifier, String code, String description) {
    this.identifier = identifier;
    this.code = code;
    this.description = description;
  }

  /**
   *
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
   *
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

}
