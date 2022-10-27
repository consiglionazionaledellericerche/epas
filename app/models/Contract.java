/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package models;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import helpers.validators.ContractBeforeSourceResidualAndOverlapingCheck;
import helpers.validators.ContractEndContractCheck;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import models.base.IPropertiesInPeriodOwner;
import models.base.IPropertyInPeriod;
import models.base.PeriodModel;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;
import play.data.validation.CheckWith;
import play.data.validation.Max;
import play.data.validation.Min;
import play.data.validation.Required;

/**
 * Contratto di un dipendente.
 */
@Getter
@Setter
@Entity
@Table(name = "contracts")
@Audited
public class Contract extends PeriodModel implements IPropertiesInPeriodOwner {

  private static final long serialVersionUID = -4472102414284745470L;
  
  private String perseoId;

  private String externalId;
  
  /**
   * Patch per gestire i contratti con dati mancanti da dcp. E' true unicamente per segnalare tempo
   * determinato senza data fine specificata.
   */
  @Column(name = "is_temporary")
  private boolean isTemporaryMissing;

  /*
   * Quando viene valorizzata la sourceDateResidual, deve essere valorizzata
   * anche la sourceDateMealTicket
   */
  @CheckWith(ContractBeforeSourceResidualAndOverlapingCheck.class)
  @Getter
  private LocalDate sourceDateResidual = null;
  
  @Getter
  private LocalDate sourceDateVacation = null;

  @Getter
  private LocalDate sourceDateMealTicket = null;
  
  @Getter
  private LocalDate sourceDateRecoveryDay = null;

  public boolean sourceByAdmin = true;

  @Getter
  @Max(32)
  private Integer sourceVacationLastYearUsed = null;

  @Getter
  @Max(32)
  private Integer sourceVacationCurrentYearUsed = null;

  @Getter
  @Max(4)
  private Integer sourcePermissionUsed = null;

  // Valore puramente indicativo per impedire che vengano inseriti i riposi compensativi in minuti
  @Min(0)
  @Max(100)
  private Integer sourceRecoveryDayUsed = null;

  private Integer sourceRemainingMinutesLastYear = null;

  private Integer sourceRemainingMinutesCurrentYear = null;

  @Getter
  private Integer sourceRemainingMealTicket = null;

  @ManyToOne(fetch = FetchType.LAZY)
  private Person person;

  @Getter
  @OneToMany(mappedBy = "contract", cascade = CascadeType.REMOVE)
  @OrderBy("beginDate")
  private List<VacationPeriod> vacationPeriods = Lists.newArrayList();

  @OneToMany(mappedBy = "contract", cascade = CascadeType.REMOVE)
  private List<ContractMonthRecap> contractMonthRecaps = Lists.newArrayList();

  //data di termine contratto in casi di licenziamento, pensione, morte, ecc ecc...

  @CheckWith(ContractEndContractCheck.class)
  @Getter
  private LocalDate endContract;

  @Getter
  @NotAudited
  @OneToMany(mappedBy = "contract", cascade = CascadeType.REMOVE)
  @OrderBy("beginDate")
  private Set<ContractWorkingTimeType> contractWorkingTimeType = Sets.newHashSet();

  @Getter
  @NotAudited
  @OneToMany(mappedBy = "contract", cascade = CascadeType.REMOVE)
  @OrderBy("beginDate")
  private Set<ContractMandatoryTimeSlot> contractMandatoryTimeSlots = Sets.newHashSet();
  
  @Getter
  @NotAudited
  @OneToMany(mappedBy = "contract", cascade = CascadeType.REMOVE)
  @OrderBy("beginDate")
  private Set<PersonalWorkingTime> personalWorkingTimes = Sets.newHashSet();
  
  @NotAudited
  @OneToMany(mappedBy = "contract", cascade = CascadeType.REMOVE)
  @OrderBy("beginDate")
  private Set<ContractStampProfile> contractStampProfile = Sets.newHashSet();

  @NotAudited
  @OneToMany(mappedBy = "contract", cascade = CascadeType.REMOVE)
  private List<MealTicket> mealTickets;

  @Required
  private boolean onCertificate = true;

  @Transient
  private List<ContractWorkingTimeType> contractWorkingTimeTypeAsList;
   
  @Getter
  @Setter
  @OneToOne
  private Contract previousContract;

  @NotAudited
  private LocalDateTime updatedAt;

  @PreUpdate
  @PrePersist
  private void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  /**
   * Ritorna la lista dei vacationPeriods del contratto e del precedente se presente.
   *
   * @return i vacationPeriods del contratto più quelli del contratto precedente se presente.
   * 
   */
  @Transient
  public List<VacationPeriod> getExtendedVacationPeriods() {
    List<VacationPeriod> vp = new ArrayList<VacationPeriod>(getVacationPeriods());
    if (getPreviousContract() != null) {
      vp.addAll(getPreviousContract().getVacationPeriods());
    }
    return vp;
  }
  
  @Override
  public String toString() {
    return String.format("Contract[%d] - person.id = %d, "
            + "beginDate = %s, endDate = %s, endContract = %s, calculatedEnd = %s, perseoId = %s",
        id, person != null ? person.id : null, getBeginDate(), getEndDate(), endContract,
        calculatedEnd(), perseoId);
  }

  /**
   * Ritorna il ContractStampProfile attivo alla data.
   */
  @Transient
  public Optional<ContractStampProfile> getContractStampProfileFromDate(LocalDate date) {

    for (ContractStampProfile csp : contractStampProfile) {
      if (csp.dateRange().contains(date)) {
        return Optional.fromNullable(csp);
      }
    }
    return Optional.absent();
  }

  /**
   * La lista ordinata dei contractWorkingTimeType.
   *
   * @return lista
   */
  @Transient
  public List<ContractWorkingTimeType> getContractWorkingTimeTypeOrderedList() {
    List<ContractWorkingTimeType> list = Lists.newArrayList(contractWorkingTimeType);
    Collections.sort(list);
    return list;
  }


  /* (non-Javadoc)
   * @see models.base.IPropertiesInPeriodOwner#periods(java.lang.Object)
   */
  @Override
  public Collection<IPropertyInPeriod> periods(Object type) {

    if (type.equals(ContractWorkingTimeType.class)) {
      return Sets.newHashSet(contractWorkingTimeType);
    }
    if (type.equals(ContractStampProfile.class)) {
      return Sets.newHashSet(contractStampProfile);
    }
    if (type.equals(VacationPeriod.class)) {
      return Sets.newHashSet(getVacationPeriods());
    }
    if (type.equals(ContractMandatoryTimeSlot.class)) {
      return Sets.newHashSet(contractMandatoryTimeSlots);
    }
    if (type.equals(PersonalWorkingTime.class)) {
      return Sets.newHashSet(personalWorkingTimes);
    }
    return null;
  }

  @Override
  public Collection<Object> types() {
    return ImmutableSet.of(ContractWorkingTimeType.class, ContractStampProfile.class,
        VacationPeriod.class, ContractMandatoryTimeSlot.class);
  }

  @Override
  public LocalDate calculatedEnd() {
    return computeEnd(getEndDate(), endContract);
  }

  /**
   * Ritorna la data di fine contratto.
   *
   * @param endDate la data di terminazione contratto (per T.D.)
   * @param endContract la data di fine esperienza (per T.I. -> pensione)
   * @return la data di fine contratto.
   */
  public static LocalDate computeEnd(LocalDate endDate, LocalDate endContract) {
    if (endContract != null) {
      return endContract;
    }
    return endDate;
  }

  /**
   * true se il contratto è correttamente sincronizzato, false altrimenti.
   *
   * @return true se il contratto è correttamente sincronizzato, false altrimenti.
   */
  @Transient
  public boolean isProperSynchronized() {
    if (calculatedEnd() == null || !calculatedEnd().isBefore(LocalDate.now())) {
      return perseoId != null;
    }
    return true;
  }

  /**
   * Il Range che comprende le date di inizio e fine/chiusura del contratto.
   */
  public Range<LocalDate> getRange() {
    if (calculatedEnd() != null) {
      return Range.closed(getBeginDate(), calculatedEnd());
    }
    return Range.atLeast(getBeginDate());
  }

  /**
   * Verifica di sovrapposizione con il range di questo contratto.
   *
   * @return true se il range passato si sovrappone a quello definito
   *     in questo contratto.
   */
  public boolean overlap(Range<LocalDate> otherRange) {
    return getRange().isConnected(otherRange);
  }
  
  /**
   * Verifica di sovrapposizione tra due contratti.
   */
  public boolean overlap(Contract otherContract) {
    return overlap(otherContract.getRange());
  }
}
