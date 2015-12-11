package models;

import com.google.common.collect.Lists;

import it.cnr.iit.epas.NullStringBinder;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import play.data.binding.As;
import play.data.validation.Required;
import play.data.validation.Unique;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;


@Entity
@Audited
@Table(name = "office")
public class Office extends BaseModel {

  private static final long serialVersionUID = -8689432709728656660L;

  @Required
  @Unique
  @NotNull
  @Column(nullable = false)
  public String name;

  //Codice della sede, per esempio per la sede di Pisa è "044000"
  @Unique
  @As(binder = NullStringBinder.class)
  @Column(nullable = false)
  public String code;

  //sedeId, serve per l'invio degli attestati, per esempio per la sede di Pisa è "223400"
  @Required
  @Unique
  @NotNull
  @Column(name = "code_id", nullable = false)
  public String codeId;

  @Column
  public String address;

  @Column(name = "joining_date")
  public LocalDate joiningDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "institute_id")
  public Institute institute;

  public boolean headQuarter = false;

  @OneToMany(mappedBy = "owner", cascade = {CascadeType.REMOVE})
  public List<BadgeReader> badgeReaders = Lists.newArrayList();

  @OneToMany(mappedBy = "office", cascade = {CascadeType.REMOVE})
  public List<BadgeSystem> badgeSystems = Lists.newArrayList();

  @OneToMany(mappedBy = "office", cascade = {CascadeType.REMOVE})
  public List<Person> persons = Lists.newArrayList();

  @OneToMany(mappedBy = "office", cascade = {CascadeType.REMOVE})
  public List<ConfGeneral> confGeneral = Lists.newArrayList();

  @OneToMany(mappedBy = "office", cascade = {CascadeType.REMOVE})
  public List<ConfYear> confYear = Lists.newArrayList();

  @NotAudited
  @OneToMany(mappedBy = "office", cascade = {CascadeType.REMOVE})
  public List<UsersRolesOffices> usersRolesOffices = Lists.newArrayList();

  @NotAudited
  @OneToMany(mappedBy = "office")
  public List<WorkingTimeType> workingTimeType = Lists.newArrayList();

  @NotAudited
  @OneToMany(mappedBy = "office")
  public List<TotalOvertime> totalOvertimes = Lists.newArrayList();

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
}
