package models.base;

import lombok.Getter;
import lombok.Setter;

import org.joda.time.LocalDate;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

/**
 * Classe base per le proprietà in un determinato periodo.
 * @author cristian
 *
 */
@MappedSuperclass
public abstract class PropertyInPeriod extends PeriodModel implements IPropertyInPeriod {

  private static final long serialVersionUID = 2434367290313120345L;


  /**
   * Contiene l'informazione se all'interno del periodo vi è la prima data da ricalcolare.
   */
  @Transient
  @Getter
  @Setter
  public LocalDate recomputeFrom;

  // TODO da verificare
  public PropertyInPeriod newInstance() {
    Class<?> superClass = this.getClass();
    PropertyInPeriod obj = null;
    try {
      obj = (PropertyInPeriod)superClass.newInstance();
      obj.setOwner(this.getOwner());
      obj.setValue(this.getValue());
      return (PropertyInPeriod)obj;
    } catch (InstantiationException | IllegalAccessException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;

  }
}
