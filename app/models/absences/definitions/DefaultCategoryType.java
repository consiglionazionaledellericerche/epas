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
import models.absences.CategoryGroupAbsenceType;
import org.assertj.core.util.Lists;


/**
 * Le categorie di default.
 *
 * @author Alessandro Martelli
 *
 */
public enum DefaultCategoryType {

  MISSIONE_CNR("Missioni CNR", 1, DefaultTab.MISSIONE), 
  MISSIONE_CNR_ESTERO("Missioni CNR Estero", 2, DefaultTab.MISSIONE), 
  FERIE_CNR("Ferie e permessi legge", 2, DefaultTab.FERIE_PERMESSI_RIPOSI), 
  //ESENZIONE_COVID19("Esenzione da lavoro causa COVID19", 3, DefaultTab.FERIE_PERMESSI_RIPOSI),
  RIPOSI_COMPENSATIVI_CNR("Riposi compensativi", 3, DefaultTab.FERIE_PERMESSI_RIPOSI),
  PROROGA_FERIE_2020("Proroga ferie 2020", 4, DefaultTab.FERIE_PERMESSI_RIPOSI),
  PROROGA_FERIE_2021("Proroga ferie 2021", 4, DefaultTab.FERIE_PERMESSI_RIPOSI),
  ASTENSIONE_POSTPARTUM("Astensione post partum", 5, DefaultTab.CONGEDI_PARENTALI), 
  CONGEDI_PRENATALI("Congedi prenatali", 7, DefaultTab.CONGEDI_PARENTALI),

  L_104("Disabilità legge 104/92 - Tre giorni mensili", 6, DefaultTab.LEGGE_104), 
  PERMESSI_PROVVISORI_104("Permessi Provvisori legge 104/92", 7, DefaultTab.LEGGE_104), 
  ALTRI_104("Altri congedi legge 104/92", 8, DefaultTab.LEGGE_104),

  MALATTIA_DIPENDENTE("Malattia dipendente", 8, DefaultTab.MALATTIA), 
  VISITA_MEDICA("Visita medica", 9, DefaultTab.MALATTIA),
  
  VISITA_MEDICA_TERAPIE("Visita medica per terapie", 9, DefaultTab.MALATTIA),

  MALATTIA_FIGLIO_1("Malattia primo figlio", 9, DefaultTab.CONGEDI_PARENTALI), 
  MALATTIA_FIGLIO_2("Malattia secondo figlio", 10, DefaultTab.CONGEDI_PARENTALI), 
  MALATTIA_FIGLIO_3("Malattia terzo figlio", 11, DefaultTab.CONGEDI_PARENTALI),
  MALATTIA_FIGLIO_4("Malattia quarto figlio", 12, DefaultTab.CONGEDI_PARENTALI),

  CONGEDO_MATRIMONIO("Congedo per matrimonio", 12, DefaultTab.CONGEDI_PARENTALI),

  PERMESSO_PRIMO_LUTTO("Permesso per primo lutto", 12, DefaultTab.ALTRI_CODICI), 
  PERMESSO_SECONDO_LUTTO("Permesso per secondo lutto", 12, DefaultTab.ALTRI_CODICI), 
  PERMESSO_TERZO_LUTTO("Permesso per terzo lutto", 12, DefaultTab.ALTRI_CODICI),

  PERMESSI_PERSONALI("Permessi Personali", 12, DefaultTab.FERIE_PERMESSI_RIPOSI), 
  PERMESSO_ESAMI("Permesso esami", 12, DefaultTab.ALTRI_CODICI), 
  ORE_AGGIUNTIVE("Ore aggiuntive", 13, DefaultTab.ALTRI_CODICI), 
  PERMESSO_MOTIVI_PRIVATI_NON_RETIBUITO("Permesso motivi privati non retribuito", 
      13, DefaultTab.ALTRI_CODICI), 
  PERMESSO_MOTIVI_PRIVATI_NON_RETIBUITO_TD("Permesso motivi privati non retribuito t.d.", 
      13, DefaultTab.ALTRI_CODICI),

  ALTRI_CODICI("Altri Codici", 16, DefaultTab.ALTRI_CODICI), 
  ASPETTATIVA("Codici Aspettativa", 17, DefaultTab.ALTRI_CODICI), 
  PUBBLICA_FUNZIOINE("Pubblica Funzione", 18, DefaultTab.ALTRI_CODICI),

  LAVORO_FUORI_SEDE("Lavoro fuori sede", 17, DefaultTab.LAVORO_FUORI_SEDE), 
  FERIE_DIPENDENTI("Ferie e permessi legge", 18, DefaultTab.FERIE_DIPENDENTI), 
  RIPOSI_COMPENSATIVI_DIPENDENTI("Riposi compensativi", 19, DefaultTab.RIPOSI_DIPENDENTI), 
  TELELAVORO("Telelavoro", 20, DefaultTab.TELELAVORO),
  TELELAVORO_RICERCATORI_TECNOLOGI("Telelavoro ricercatori tecnologi", 21, 
      DefaultTab.TELELAVORO_RICERCATORI_TECNOLOGI),
  L_104_DIPENDENTI("Disabilità legge 104/92 - Tre giorni mensili dipendenti", 20,
      DefaultTab.LEGGE_104_DIPENDENTI), 
  L_104_PARENTI_DIPENDENTI("Disabilità legge 104/92 - Tre giorni mensili parenti dipendenti", 21,
      DefaultTab.LEGGE_104_PARENTI_DIPENDENTI), 
  STUDIO_DIPENDENTI("Permesso studio", 19, DefaultTab.STUDIO_DIPENDENTI), 
  ALTRI_104_DIPENDENTI("Altri congedi legge 104/92", 20, DefaultTab.LEGGE_104_DIPENDENTI), 
  COVID_19("Emergenza Covid-19", 22, DefaultTab.COVID19), 
  L_AGILE("Lavoro agile", 23, DefaultTab.LAVORO_AGILE), 
  COD39_LA("Lavoro agile assistenza dis/immunodepressi", 22, DefaultTab.COD39LA),
  SMART("Lavoro agile smartworking", 25, DefaultTab.SMART),
  ORE_AGGIUNTIVE_DIPENDENTI("Ore aggiuntive dipendenti", 23, DefaultTab.ORE_AGGIUNTIVE_DIPENDENTI), 
  ASTENSIONE_POSTPARTUM_DIPENDENTI("Astensione post partum dipendenti", 
      24, DefaultTab.CONGEDI_PARENTALI_DIPENDENTI),
  VISITA_MEDICA_DIPENDENTE("Visita medica dipendente", 24, DefaultTab.VISITA_MEDICA_DIPENDENTI),
  CODICI_AUTOMATICI("Codici Automatici", 23, DefaultTab.AUTOMATICI);

  public String description;
  public int priority;
  public DefaultTab categoryTab;

  private DefaultCategoryType(String description, int priority, DefaultTab categoryTab) {
    this.description = description;
    this.priority = priority;
    this.categoryTab = categoryTab;
  }

  /**
   * Ricerca le categorie modellate e non presenti fra quelle passate in arg (db).
   *
   * @return list
   */
  public static List<DefaultCategoryType> missing(List<CategoryGroupAbsenceType> allCategories) {
    List<DefaultCategoryType> missing = Lists.newArrayList();
    for (DefaultCategoryType defaultCategory : DefaultCategoryType.values()) {
      boolean found = false;
      for (CategoryGroupAbsenceType category : allCategories) {
        if (defaultCategory.name().equals(category.getName())) {
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
   * L'enumerato corrispettivo della categoria (se esiste...)
   *
   * @return optional dell'enumerato
   */
  public static Optional<DefaultCategoryType> byName(CategoryGroupAbsenceType category) {
    for (DefaultCategoryType defaultCategory : DefaultCategoryType.values()) {
      if (defaultCategory.name().equals(category.getName())) {
        return Optional.of(defaultCategory);
      }
    }
    return Optional.absent();
  }
}
