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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import manager.configurations.EpasParam;
import models.base.IPropertiesInPeriodOwner;
import models.base.IPropertyInPeriod;
import models.base.PeriodModel;
import models.flows.Group;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;
import play.data.validation.Required;
import play.data.validation.Unique;


/**
 * Un ufficio.
 */
@Getter
@Setter
@Entity
@Audited
public class Office extends PeriodModel implements IPropertiesInPeriodOwner {

  private static final long serialVersionUID = -8689432709728656660L;

  private Long perseoId;

  @Required
  @NotNull
  @Column(nullable = false)
  private String name;

  //Codice della sede, per esempio per la sede di Pisa è "044000"
  private String code;

  //sedeId, serve per l'invio degli attestati, per esempio per la sede di Pisa è "223400"
  @Required
  @Unique
  @NotNull
  private String codeId;

  private String address;

  private LocalDate joiningDate;

  @ManyToOne(fetch = FetchType.LAZY)
  private Institute institute;

  private boolean headQuarter = false;

  @OneToMany(mappedBy = "owner", cascade = {CascadeType.REMOVE})
  private List<User> users = Lists.newArrayList();

  @OneToMany(mappedBy = "office", cascade = {CascadeType.REMOVE})
  private List<BadgeSystem> badgeSystems = Lists.newArrayList();

  @OneToMany(mappedBy = "office", cascade = {CascadeType.REMOVE})
  private List<Configuration> configurations = Lists.newArrayList();

  @OneToMany(mappedBy = "office", cascade = {CascadeType.REMOVE})
  private List<PersonReperibilityType> personReperibilityTypes = Lists.newArrayList();

  @OneToMany(mappedBy = "office", cascade = {CascadeType.REMOVE})
  private List<ShiftCategories> shiftCategories = Lists.newArrayList();


  @OneToMany(mappedBy = "office", cascade = {CascadeType.REMOVE})
  private List<UsersRolesOffices> usersRolesOffices = Lists.newArrayList();

  @OneToMany(mappedBy = "office", cascade = {CascadeType.REMOVE})
  private List<Group> groups = Lists.newArrayList();

  @NotAudited
  @OneToMany(mappedBy = "office")
  private List<WorkingTimeType> workingTimeType = Lists.newArrayList();

  @NotAudited
  @OneToMany(mappedBy = "office")
  private List<TimeSlot> timeSlots = Lists.newArrayList();

  @NotAudited
  @OneToMany(mappedBy = "office")
  private List<ShiftTimeTable> shiftTimeTable = Lists.newArrayList();

  @NotAudited
  @OneToMany(mappedBy = "office")
  private List<TotalOvertime> totalOvertimes = Lists.newArrayList();

  @NotAudited
  @OneToMany(mappedBy = "office")
  private List<Attachment> attachments = Lists.newArrayList();

  @NotAudited
  @OneToMany(mappedBy = "office")
  private List<MealTicket> tickets = Lists.newArrayList();

  @NotAudited
  private LocalDateTime updatedAt;
  
  @OneToMany(mappedBy = "office")
  public List<PersonsOffices> personsOffices = Lists.newArrayList();

  @PreUpdate
  @PrePersist
  private void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  @Transient
  private Boolean isEditable = null;

  public String getName() {
    return this.name;
  }

  @Override
  public String getLabel() {
    return this.name;
  }

  @Override
  public String toString() {
    return getLabel();
  }

  @Override
  public Collection<IPropertyInPeriod> periods(Object type) {

    if (type.getClass().equals(EpasParam.class)) {
      return (Collection<IPropertyInPeriod>) filterConfigurations((EpasParam) type);
    }
    return null;
  }

  @Override
  public Collection<Object> types() {
    return Sets.newHashSet(Arrays.asList(EpasParam.values()));
  }

  /**
   * Filtra dalla lista di configurations le occorrenze del tipo epasParam.
   *
   * @param epasParam filtro
   * @return insieme filtrato
   */
  private Set<IPropertyInPeriod> filterConfigurations(EpasParam epasParam) {
    Set<IPropertyInPeriod> configurations = Sets.newHashSet();
    for (Configuration configuration : this.configurations) {
      if (configuration.getEpasParam().equals(epasParam)) {
        configurations.add(configuration);
      }
    }
    return configurations;
  }

  /**
   * True se l'ufficio contiene il parametro di configurazione con il valore indicato,
   * false altrimenti.
   *
   * @param param Parametro di configurazione da controllare.
   * @param value valore atteso
   * @return true se l'ufficio contiene il parametro di configurazione specificato con il valore
   *     indicato.
   */
  public boolean checkConf(EpasParam param, String value) {
    return configurations.stream().anyMatch(conf -> conf.getEpasParam() == param
        && conf.getFieldValue().equals(value));
  }

  /**
   * 
   * @param date
   * @return
   */
  @Transient
  public List<Person> peopleInOffice(Optional<LocalDate> date) {
    LocalDate dateToConsider;
    if (date.isPresent()) {
      dateToConsider = date.get();
    } else {
      dateToConsider = LocalDate.now();
    }
    return this.personsOffices.stream().filter(po -> 
    !po.getBeginDate().isAfter(dateToConsider) 
    && (po.getEndDate() == null || po.getEndDate().isBefore(dateToConsider)))
        .map(po -> po.person).collect(Collectors.toList());

  }

  @Transient
  public List<Person> getPersons() {
    return peopleInOffice(Optional.<LocalDate>absent());
  }
  
  @Transient
  public List<UsersRolesOffices> getUsersRolesOffices() {
    return this.usersRolesOffices;
  }

}