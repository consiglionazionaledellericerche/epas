package models;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import models.base.IPropertiesInPeriodOwner;
import models.base.IPropertyInPeriod;
import models.base.PeriodModel;

import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import play.data.validation.Required;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "contracts")
public class Contract extends PeriodModel implements IPropertiesInPeriodOwner {

  private static final long serialVersionUID = -4472102414284745470L;

  /*
   * Quando viene valorizzata la sourceDateResidual, deve essere valorizzata
   * anche la sourceDateMealTicket
   */
  @Column(name = "source_date_residual")
  public LocalDate sourceDateResidual = null;

  @Column(name = "source_date_meal_ticket")
  public LocalDate sourceDateMealTicket = null;

  @Column(name = "source_by_admin")
  public boolean sourceByAdmin = true;

  @Column(name = "source_vacation_last_year_used")
  public Integer sourceVacationLastYearUsed = null;

  @Column(name = "source_vacation_current_year_used")
  public Integer sourceVacationCurrentYearUsed = null;

  @Column(name = "source_permission_used")
  public Integer sourcePermissionUsed = null;

  @Column(name = "source_recovery_day_used")
  public Integer sourceRecoveryDayUsed = null;

  @Column(name = "source_remaining_minutes_last_year")
  public Integer sourceRemainingMinutesLastYear = null;

  @Column(name = "source_remaining_minutes_current_year")
  public Integer sourceRemainingMinutesCurrentYear = null;

  @Column(name = "source_remaining_meal_ticket")
  public Integer sourceRemainingMealTicket = null;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "person_id")
  public Person person;

  @OneToMany(mappedBy = "contract", cascade = CascadeType.REMOVE)
  @OrderBy("beginFrom")
  public List<VacationPeriod> vacationPeriods = Lists.newArrayList();

  @OneToMany(mappedBy = "contract", cascade = CascadeType.REMOVE)
  public List<ContractMonthRecap> contractMonthRecaps = Lists.newArrayList();

  //data di termine contratto in casi di licenziamento, pensione, morte, ecc ecc...

  @Column(name = "end_contract")
  public LocalDate endContract;

  @NotAudited
  @OneToMany(mappedBy = "contract", cascade = {CascadeType.REMOVE})
  @OrderBy("beginDate")
  public Set<ContractWorkingTimeType> contractWorkingTimeType = Sets.newHashSet();

  @NotAudited
  @OneToMany(mappedBy = "contract", cascade = {CascadeType.REMOVE})
  @OrderBy("beginDate")
  public Set<ContractStampProfile> contractStampProfile = Sets.newHashSet();

  @NotAudited
  @OneToMany(mappedBy = "contract", cascade = {CascadeType.REMOVE})
  public List<MealTicket> mealTickets;

  @Required
  public boolean onCertificate = true;

  @Transient
  private List<ContractWorkingTimeType> contractWorkingTimeTypeAsList;

  @Override
  public String toString() {
    return String.format("Contract[%d] - person.id = %d, "
        + "beginDate = %s, endDate = %s, endContract = %s",
            id, person.id, beginDate, endDate, endContract);
  }

  /**
   * Ritorna il ContractStampProfile attivo alla data.
   */
  public Optional<ContractStampProfile> getContractStampProfileFromDate(LocalDate date) {

    for (ContractStampProfile csp : contractStampProfile) {
      if (csp.dateRange().contains(date)) {
        return Optional.fromNullable(csp);
      }
    }
    return Optional.absent();
  }

  /**
   * Conversione della lista dei contractWorkingtimeType da Set a List.
   */
  public List<ContractWorkingTimeType> getContractWorkingTimeTypeAsList() {
    return Lists.newArrayList(contractWorkingTimeType);
  }


  /* (non-Javadoc)
   * @see models.base.IPropertiesInPeriodOwner#periods(java.lang.Object)
   */
  @Override
  public Collection<IPropertyInPeriod> periods(Object type) {
     
    if (type.equals(ContractWorkingTimeType.class)) {
      return Sets.<IPropertyInPeriod>newHashSet(contractWorkingTimeType);
    }
    if (type.equals(ContractStampProfile.class)) {
      return Sets.<IPropertyInPeriod>newHashSet(contractStampProfile);
    }
    return null;
  }

  @Override
  public LocalDate calculatedEnd() {
    if (this.endContract != null) {
      return this.endContract;
    } 
    return this.endDate;
  }


}
