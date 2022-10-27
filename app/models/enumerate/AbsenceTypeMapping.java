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

package models.enumerate;

import models.absences.AbsenceType;

/**
 * Enumerato per il mapping del tipo assenza.
 *
 * @author Cristian Lucchesi
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
    return absenceType != null && absenceType.getCode().equals(code);
  }
}

