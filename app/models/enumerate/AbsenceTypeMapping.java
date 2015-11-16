package models.enumerate;

import models.AbsenceType;

/**
 * @author cristian
 *
 */
public enum AbsenceTypeMapping {

	AST_FAC_POST_PART_MAG_3_ANNI_1_FIGLIO("24"),
	AST_FAC_POST_PART_MIN_3_ANNI_1_FIGLIO("24S"),
	AST_FAC_POST_PART_30PERC_1_FIGLIO("25"),
	FERIE_ANNO_PRECEDENTE("31"),
	FERIE_ANNO_CORRENTE("32"),
	FERIE_ANNO_PRECEDENTE_DOPO_31_08("37"),
	FERIE_FESTIVITA_SOPPRESSE_EPAS("FER"),
	RIPOSO_COMPENSATIVO("91"),
	MISSIONE("92"),
	FESTIVITA_SOPPRESSE("94"),
	FER("FER"),
	TELELAVORO("103");

	private String code;

	private AbsenceTypeMapping(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public boolean is(AbsenceType absenceType) {
		return absenceType != null && absenceType.code.equals(code);
	}
}

