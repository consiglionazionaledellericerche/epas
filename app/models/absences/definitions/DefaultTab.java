package models.absences.definitions;

import java.util.List;
import org.assertj.core.util.Lists;
import com.google.common.base.Optional;
import models.absences.CategoryTab;

/**
 * Tab di default.
 * 
 * @author alessandro
 *
 */
public enum DefaultTab {

  MISSIONE("Missione", 1), FERIE_PERMESSI_RIPOSI("Ferie Permessi e Riposi", 2),

  MALATTIA("Malattia e Visite Mediche", 4), CONGEDI_PARENTALI("Congedi Parentali",
      5), LEGGE_104("L. 104", 6),

  ALTRI_CODICI("Altri Codici", 7),

  // Per dipendenti
  LAVORO_FUORI_SEDE("Lavoro Fuori Sede", 9), FERIE_DIPENDENTI("Ferie e Permessi",
      10), RIPOSI_DIPENDENTI("Riposo Compensativo", 11), TELELAVORO("Telelavoro",
          12), LEGGE_104_DIPENDENTI("L. 104 dipendenti", 13), LEGGE_104_PARENTI_DIPENDENTI(
              "L. 104 parenti dipendenti", 8), STUDIO_DIPENDENTI("Diritto allo studio dipendenti",
                  13), COVID19("Emergenza covid-19", 14), ORE_AGGIUNTIVE_DIPENDENTI(
                      "Ore aggiuntive dipendenti", 15), AUTOMATICI("Codici Automatici", 11);

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
