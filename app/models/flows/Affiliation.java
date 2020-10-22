package models.flows;

import com.google.common.collect.Range;
import helpers.validators.AffiliationCheck;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import models.Person;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import play.data.validation.CheckWith;
import play.data.validation.Required;

@Data
@EqualsAndHashCode(callSuper = true)
@Audited
@Entity
public class Affiliation extends BaseModel {

  private static final long serialVersionUID = -8101378323853245726L;
  
  @Required
  @CheckWith(AffiliationCheck.class)
  @ManyToOne
  private Group group;

  @Required
  @ManyToOne
  private Person person;
  
  @play.data.validation.Range(min = 0.0, max = 100)
  private BigDecimal percentage = BigDecimal.valueOf(100);

  @NotNull
  @Required
  private LocalDate beginDate;

  private LocalDate endDate;

  /**
   * Il Range che comprende le date di inizio e fine dell'assegnazione.
   */
  public Range<LocalDate> getRange() {
    if (endDate != null) {
      return Range.closed(beginDate, endDate);
    }
    return Range.atLeast(beginDate);
  }
  
  /**
   * Verifica di appartenenza a questa affiliazione.
   * 
   * @return true se la data passata è compresa nelle data di validità 
   *     di questa affiliazione, false altrimenti.
   */
  public boolean contains(LocalDate date) {
    return getRange().contains(date);
  }
    
  /**
   * Verifica di sovrapposizione con il range di questa affiliazione.
   * @return true se il range passato si sovrappone a quello definito
   *     questa affiliazione.
   */
  public boolean overlap(Range<LocalDate> otherRange) {
    return getRange().isConnected(otherRange);
  }
  
  /**
   * Verifica di sovrapposizione tra due affiliazioni.
   */
  public boolean overlap(Affiliation otherAffiliation) {
    return overlap(otherAffiliation.getRange()) && group.equals(otherAffiliation.getGroup());
  }
  
  /**
   * Verificare se l'affiliazione è attiva nella data corrente.
   * @return true se l'affiliazione è attiva nella data corrente, false altrimenti.
   */
  public boolean isActive() {
    return beginDate.isBefore(LocalDate.now()) 
        && (endDate == null || LocalDate.now().isBefore(endDate));
  }
}