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

package models.absences.definitions;

import com.google.common.base.Optional;
import java.util.List;
import models.absences.CategoryTab;
import org.assertj.core.util.Lists;


/**
 * Tab di default.
 *
 * @author Alessandro Martelli
 *
 */
public enum DefaultTab {

  MISSIONE("Missione", 1), FERIE_PERMESSI_RIPOSI("Ferie Permessi e Riposi", 2),

  MALATTIA("Malattia e Visite Mediche", 4), CONGEDI_PARENTALI("Congedi Parentali",
      5), LEGGE_104("L. 104", 6),

  ALTRI_CODICI("Altri Codici", 7),

  // Per dipendenti
  LAVORO_FUORI_SEDE("Lavoro Fuori Sede", 9), 
  FERIE_DIPENDENTI("Ferie e Permessi", 10), 
  RIPOSI_DIPENDENTI("Riposo Compensativo", 11), 
  TELELAVORO("Telelavoro", 12), 
  TELELAVORO_RICERCATORI_TECNOLOGI("Telelavoro ricercatori tecnologi", 13), 
  LEGGE_104_DIPENDENTI("L. 104 dipendenti", 13),
  LEGGE_104_PARENTI_DIPENDENTI("L. 104 parenti dipendenti", 14), 
  STUDIO_DIPENDENTI("Diritto allo studio dipendenti", 13), 
  COVID19("Emergenza covid-19", 16), 
  LAVORO_AGILE("Lavoro agile", 20),
  COD39LA("Assistenza parenti immunodepressi", 18),
  SMART("Smartworking", 17),
  ORE_AGGIUNTIVE_DIPENDENTI("Ore aggiuntive dipendenti", 15), 
  CONGEDI_PARENTALI_DIPENDENTI("Congedi Parentali dipendenti", 24),
  VISITA_MEDICA_DIPENDENTI("Visita medica dipendenti", 24),
  AUTOMATICI("Codici Automatici", 11);

  public String description;
  public int priority;

  private DefaultTab(String description, int priority) {
    this.description = description;
    this.priority = priority;
  }

  /**
   * Ricerca le categorie modellate e non presenti fra quelle passate in arg (db).
   *
   * @return list
   */
  public static List<DefaultTab> missing(List<CategoryTab> allTabs) {
    List<DefaultTab> missing = Lists.newArrayList();
    for (DefaultTab defaultCategory : DefaultTab.values()) {
      boolean found = false;
      for (CategoryTab category : allTabs) {
        if (defaultCategory.name().equals(category.name)) {
          found = true;
          break;
        }
      }
      if (!found) {
        missing.add(defaultCategory);
      }
    }
    return missing;
  }

  /**
   * L'enumerato corrispettivo della tab (se esiste...)
   *
   * @return optional dell'enumerato
   */
  public static Optional<DefaultTab> byName(CategoryTab tab) {
    for (DefaultTab defaultTab : DefaultTab.values()) {
      if (defaultTab.name().equals(tab.name)) {
        return Optional.of(defaultTab);
      }
    }
    return Optional.absent();
  }
}
