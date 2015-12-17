package models.base;

import java.util.Collection;

/**
 * Il modello è il target del periodo. EX. Contract
 *
 * @author alessandro
 *
 * @param <T>
 */
public interface IPropertiesInPeriodOwner extends IPeriodModel {

  /**
   * La lista dei periodi del tipo specificato.
   * @return
   */
  Collection<IPropertyInPeriod> periods(Object type);
}
