package models;

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

import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import models.base.BaseModel;
import models.base.IPeriodTarget;
import play.data.validation.Required;


/**
 * @author dario
 */
@Entity
@Table(name = "contracts")
public class Contract extends BaseModel implements IPeriodTarget {

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

  @Required
  @NotNull
  @Column(name = "begin_contract")
  public LocalDate beginContract;

  @Column(name = "expire_contract")
  public LocalDate expireContract;

  //data di termine contratto in casi di licenziamento, pensione, morte, ecc ecc...

  @Column(name = "end_contract")
  public LocalDate endContract;

  @NotAudited
  @OneToMany(mappedBy = "contract", cascade = {CascadeType.REMOVE})
  @OrderBy("beginDate")
  public Set<ContractWorkingTimeType> contractWorkingTimeType = Sets.newHashSet();

  @NotAudited
  @OneToMany(mappedBy = "contract", cascade = {CascadeType.REMOVE})
  @OrderBy("startFrom")
  public Set<ContractStampProfile> contractStampProfile = Sets.newHashSet();

  @NotAudited
  @OneToMany(mappedBy = "contract", cascade = {CascadeType.REMOVE})
  public List<MealTicket> mealTickets;
  /**
   * I contratti con onCertificate = true sono quelli dei dipendenti CNR e corrispondono a quelli
   * con l'obbligo dell'attestato di presenza da inviare a Roma
   */
  @Required
  public boolean onCertificate = true;


//	public void setSourceDateResidual(String date){
//		this.sourceDateResidual = new LocalDate(date);
//	}
  @Transient
  private List<ContractWorkingTimeType> contractWorkingTimeTypeAsList;

  @Override
  public String toString() {
    return String.format("Contract[%d] - person.id = %d, beginContract = %s, expireContract = %s, endContract = %s",
            id, person.id, beginContract, expireContract, endContract);
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

  @Override
  public Contract getValue() {
    return this;
  }

}
