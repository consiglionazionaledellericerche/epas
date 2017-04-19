package models.absences.definitions;

import com.google.common.base.Optional;

import java.util.List;

import models.absences.CategoryGroupAbsenceType;

import org.assertj.core.util.Lists;

/**
 * Le categorie di default.
 * 
 * @author alessandro
 *
 */
public enum DefaultCategoryType {

  MISSIONE_CNR("Missioni CNR", 1, DefaultTab.MISSIONE), 
  FERIE_CNR("Ferie e permessi legge", 2, DefaultTab.FERIE_PERMESSI_RIPOSI),
  RIPOSI_COMPENSATIVI_CNR("Riposi compensativi", 3, DefaultTab.FERIE_PERMESSI_RIPOSI),
  
  ASTENSIONE_POSTPARTUM("Astensione post partum", 5, DefaultTab.CONGEDI_PARENTALI),
  CONGEDI_PRENATALI("Congedi prenatali", 7, DefaultTab.CONGEDI_PARENTALI),
  
  L_104("Disabilità legge 104/92 - Tre giorni mensili", 6, DefaultTab.LEGGE_104),
  PERMESSI_PROVVISORI_104("Permessi Provvisori legge 104/92", 7, DefaultTab.LEGGE_104),
  ALTRI_104("Altri congedi legge 104/92", 8, DefaultTab.LEGGE_104),
  
  MALATTIA_DIPENDENTE("Malattia dipendente", 8, DefaultTab.MALATTIA),
  VISITA_MEDICA("Visita medica", 9, DefaultTab.MALATTIA),
  
  MALATTIA_FIGLIO_1("Malattia primo figlio", 9, DefaultTab.CONGEDI_PARENTALI),
  MALATTIA_FIGLIO_2("Malattia secondo figlio", 10, DefaultTab.CONGEDI_PARENTALI),
  MALATTIA_FIGLIO_3("Malattia terzo figlio", 11, DefaultTab.CONGEDI_PARENTALI),
  
  CONGEDO_MATRIMONIO("Congedo per matrimonio", 12, 
      DefaultTab.CONGEDI_PARENTALI),


  
  PERMESSI_PERSONALI("Permessi Personali", 12, DefaultTab.FERIE_PERMESSI_RIPOSI),
  //DIRITTO_STUDIO("Diritto allo studio", 13, DefaultTab.ALTRE_TIPOLOGIE),
  //TELELAVORO("Telelavoro", 14, DefaultTab.ALTRI_GRUPPI),
  //PERMESSI_SINDACALI("Permessi Sindacali", 15, DefaultTab.ALTRI_GRUPPI),
  ALTRI_CODICI("Altri Codici", 16, DefaultTab.ALTRI_CODICI),
  ASPETTATIVA("Codici Aspettativa", 16, DefaultTab.ALTRI_CODICI),
  PUBBLICA_FUNZIOINE("Pubblica Funzione", 16, DefaultTab.ALTRI_CODICI),

  LAVORO_FUORI_SEDE("Lavoro fuori sede", 17, DefaultTab.LAVORO_FUORI_SEDE),
  FERIE_DIPENDENTI("Ferie e permessi legge", 18, DefaultTab.FERIE_DIPENDENTI),
  
  CODICI_AUTOMATICI("Codici Automatici", 18, DefaultTab.AUTOMATICI);

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
   * @return list
   */
  public static List<DefaultCategoryType> missing(List<CategoryGroupAbsenceType> allCategories) {
    List<DefaultCategoryType> missing = Lists.newArrayList();
    for (DefaultCategoryType defaultCategory : DefaultCategoryType.values()) {
      boolean found = false;
      for (CategoryGroupAbsenceType category : allCategories) {
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
   * L'enumerato corrispettivo della categoria (se esiste...) 
   * @return optional dell'enumerato
   */
  public static Optional<DefaultCategoryType> byName(CategoryGroupAbsenceType category) {
    for (DefaultCategoryType defaultCategory : DefaultCategoryType.values()) {
      if (defaultCategory.name().equals(category.name)) {
        return Optional.of(defaultCategory);
      }
    }
    return Optional.absent();
  }
}