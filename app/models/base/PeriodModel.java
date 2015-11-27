package models.base;

import com.google.common.base.Optional;

import it.cnr.iit.epas.DateUtility;

import org.joda.time.LocalDate;

import play.data.validation.Required;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

@MappedSuperclass
public abstract class PeriodModel extends BaseModel implements IPeriodModel, Comparable<PeriodModel> {
  
  @Required
  @Column(name = "begin_date")
  public LocalDate beginDate;

  @Column(name = "end_date")
  public LocalDate endDate;
  
  /**
   * Contiene l'informazione se all'interno del periodo vi è la prima data da ricalcolare.
   */
  @Transient
  public LocalDate recomputeFrom; 
  
  @Override
  public LocalDate getBegin() {
    return this.beginDate;
  }
  
  @Override
  public void setBegin(LocalDate begin) {
    this.beginDate = begin;
  }

  @Override
  public Optional<LocalDate> getEnd() {
    return Optional.fromNullable(this.endDate);
  }
  
  // FIXME: non riesco a impostare il generico Optional<LocalDate>
  @Override
  public void setEnd(Optional end) {
    if (end.isPresent() && !DateUtility.isInfinity((LocalDate)end.get())) {
      this.endDate = (LocalDate)end.get();
    } else {
      this.endDate = null;
    }
  }

  @Override
  public int compareTo(PeriodModel other) {
    if (getBegin().isBefore(other.getBegin())) {
      return -1;
    } else if (getBegin().isAfter(other.getBegin())) {
      return 1;
    } else {
      return 0;
    }
  }

  @Override
  public PeriodModel newInstance() {
    Class superClass = this.getClass();
    Object o;
    try {
      o = superClass.newInstance();
      return (PeriodModel)o;
    } catch (InstantiationException | IllegalAccessException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
    
  }


  
}