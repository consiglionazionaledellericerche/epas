package models;

import com.google.common.collect.Lists;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.data.validation.Required;
import play.data.validation.Unique;


/**
 * Tipo di reperibilità.
 * 
 * @author cristian
 */
@Audited
@Entity
@Table(name = "person_reperibility_types")
public class PersonReperibilityType extends BaseModel {

  private static final long serialVersionUID = 3234688199593333012L;

  @Required
  @Unique
  public String description;

  @OneToMany(mappedBy = "personReperibilityType")
  public List<PersonReperibility> personReperibilities;

  /* responsabile della reperibilità */
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @Required
  public Person supervisor;
  
  public boolean disabled;
  
  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @NotNull
  public Office office; 
 
  @OneToMany(mappedBy = "personReperibilityType", cascade = CascadeType.REMOVE)
  public Set<ReperibilityTypeMonth> monthsStatus = new HashSet<>();
  
  @ManyToMany
  public List<Person> managers = Lists.newArrayList();
  
  /*Tipo di competenza mensile*/
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @NotNull
  public MonthlyCompetenceType monthlyCompetenceType;

  @Override
  public String toString() {
    return this.description;
  }
  
  /**
   * Ritorna l'oggetto che contiene l'approvazione della reperibilità alla data.
   * @param date la data da considerare
   * @return l'oggetto che contiene l'approvazione della reperibilità se esistente.
   */
  @Transient
  public Optional<ReperibilityTypeMonth> monthStatusByDate(LocalDate date) {
    final YearMonth requestedMonth = new YearMonth(date);
    return monthsStatus.stream()
        .filter(reperibilityTypeMonth -> reperibilityTypeMonth
            .yearMonth.equals(requestedMonth)).findFirst();
  }

  /**
   * Controlla se la reperibilità è stata approvata alla data passata come parametro.
   * @param date la data da verificare
   * @return true se la reperibilità è stata approvata alla data date, false altrimenti.
   */
  @Transient
  public boolean approvedOn(LocalDate date) {
    Optional<ReperibilityTypeMonth> monthStatus = monthStatusByDate(date);
    if (monthStatus.isPresent()) {
      return monthStatus.get().approved;
    } else {
      return false;
    }
  }
}
