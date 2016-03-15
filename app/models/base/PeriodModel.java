package models.base;

import com.google.common.collect.Lists;

import edu.emory.mathcs.backport.java.util.Collections;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import lombok.Getter;
import lombok.Setter;

import models.Contract;
import models.Office;

import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.db.jpa.JPA;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PostPersist;
import javax.validation.constraints.NotNull;

@MappedSuperclass
public abstract class PeriodModel extends BaseModel
    implements IPeriodModel, Comparable<PeriodModel> {

  @Getter
  @Setter
  @NotNull
  @Column(name = "begin_date")
  public LocalDate beginDate;

  @Getter
  @Setter
  @Column(name = "end_date")
  public LocalDate endDate;

  @Override
  public int compareTo(PeriodModel other) {
    if (other == null || beginDate == null) {
      return 0;
    }
    return beginDate.compareTo(other.beginDate);
  }

  @Override
  public LocalDate calculatedEnd() {
    return endDate;
  }
  
  @Override
  public DateInterval periodInterval() {
   return new DateInterval(this.getBeginDate(), this.calculatedEnd()); 
  }
  

  /**
   * Quando si cambiano le date di inizio e fine dell'owner questo algoritmo sistema i periodi: <br>
   * 1) Elimina i periodi che non appartegono più all'intervallo dell'owner <br>
   * 2) Aggiusta il primo periodo impostando la sua data inizio 
   *    alla nuova data inizio dell'owner.<br>
   * 3) Aggiusta l'ultimo periodo impostando la sua data fine alla nuova data fine dell'owner.<br>
   * Persiste il nuovo stato.
   * @param owner owner
   * @param propertyInPeriodClass tipo periodi
   */
  @PostPersist
  public void updatePropertiesInPeriodOwner() {
    
    IPropertiesInPeriodOwner owner = null;
    if (this instanceof Contract) {
      owner = (Contract)this;
    }
    if (this instanceof Office) {
      owner = (Office)this;
    }
    
    DateInterval ownerInterval = new DateInterval(owner.getBeginDate(), owner.calculatedEnd());
    
    for (Object type : owner.types()) {
      // 1) Cancello quelli che non appartengono più a contract
      for (IPropertyInPeriod propertyInPeriod: owner.periods(type)) {
        if (DateUtility.intervalIntersection(ownerInterval, 
            new DateInterval(propertyInPeriod.getBeginDate(), propertyInPeriod.getEndDate())) 
            == null) {
          propertyInPeriod._delete();
        }
      }

      JPA.em().flush();

      final List<IPropertyInPeriod> periods = Lists.newArrayList(owner.periods(type));
      Collections.sort(periods);

      // Sistemo il primo
      IPropertyInPeriod first = periods.get(0);
      first.setBeginDate(ownerInterval.getBegin());
      first._save();

      // Sistemo l'ultimo
      IPropertyInPeriod last = periods.get(periods.size() - 1);
      last.setEndDate(ownerInterval.getEnd());
      if (DateUtility.isInfinity(last.getEndDate())) {
        last.setEndDate(null);
      }
      last._save();

      JPA.em().flush();
    }
  }
  

}
