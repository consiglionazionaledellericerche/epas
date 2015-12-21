package models.base;


import org.joda.time.LocalDate;

import play.db.Model;

/**
 * Il modello è un periodo del contratto con un valore.
 *
 * @author alessandro
 */
public interface IPropertyInPeriod extends IPeriodModel, Model {

  /**
   * L'owner del periodo (il contenitore dei periodi con proprietà).
   * @return l'owner
   */
  IPropertiesInPeriodOwner getOwner();

  /**
   * Imposta l'owner del periodo.
   */
  void setOwner(IPropertiesInPeriodOwner target);


  /**
   * Il tipo della proprietà.
   * @return tipo proprietà
   */
  Object getType();

  /**
   * Imposta il tipo della proprietà.

   */
  void setType(Object value);


  /**
   * Il valore del periodo.
   */
  Object getValue();

  /**
   * Imposta il valore del periodo.
   */
  void setValue(Object value);

  /**
   * Se il valore di otherValue è lo stesso del value del periodo.
   */
  boolean periodValueEquals(Object otherValue);


  /**
   * Una nuova istanza del tipo PeriodModel.
   */
  IPropertyInPeriod newInstance();

  /**
   * Contiene l'informazione se all'interno del periodo vi è la prima data da ricalcolare.
   * @return la data da cui ricalcolare
   */
  public LocalDate getRecomputeFrom();

  public void setRecomputeFrom(LocalDate from);
}
