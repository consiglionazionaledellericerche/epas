package models;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.cnr.iit.epas.NullStringBinder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import manager.configurations.EpasParam;
import models.base.IPropertiesInPeriodOwner;
import models.base.IPropertyInPeriod;
import models.base.PeriodModel;
import models.flows.Group;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;
import play.data.binding.As;
import play.data.validation.Required;
import play.data.validation.Unique;


@Entity
@Audited
public class Office extends PeriodModel implements IPropertiesInPeriodOwner {

  private static final long serialVersionUID = -8689432709728656660L;

  public Long perseoId;

  @Required
  @NotNull
  @Column(nullable = false)
  public String name;

  //Codice della sede, per esempio per la sede di Pisa è "044000"
  @As(binder = NullStringBinder.class)
  public String code;

  //sedeId, serve per l'invio degli attestati, per esempio per la sede di Pisa è "223400"
  @Required
  @Unique
  @NotNull
  public String codeId;

  public String address;

  public LocalDate joiningDate;

  @ManyToOne(fetch = FetchType.LAZY)
  public Institute institute;

  public boolean headQuarter = false;

  @OneToMany(mappedBy = "owner", cascade = {CascadeType.REMOVE})
  public List<User> users = Lists.newArrayList();

  @OneToMany(mappedBy = "office", cascade = {CascadeType.REMOVE})
  public List<BadgeSystem> badgeSystems = Lists.newArrayList();

  @OneToMany(mappedBy = "office", cascade = {CascadeType.REMOVE})
  public List<Person> persons = Lists.newArrayList();

  @OneToMany(mappedBy = "office", cascade = {CascadeType.REMOVE})
  public List<Configuration> configurations = Lists.newArrayList();

  @OneToMany(mappedBy = "office", cascade = {CascadeType.REMOVE})
  public List<PersonReperibilityType> personReperibilityTypes = Lists.newArrayList();
  
  @OneToMany(mappedBy = "office", cascade = {CascadeType.REMOVE})
  public List<ShiftCategories> shiftCategories = Lists.newArrayList();

  
  @OneToMany(mappedBy = "office", cascade = {CascadeType.REMOVE})
  public List<UsersRolesOffices> usersRolesOffices = Lists.newArrayList();

  @OneToMany(mappedBy = "office", cascade = {CascadeType.REMOVE})
  public List<Group> groups = Lists.newArrayList();

  @NotAudited
  @OneToMany(mappedBy = "office")
  public List<WorkingTimeType> workingTimeType = Lists.newArrayList();
  
  @NotAudited
  @OneToMany(mappedBy = "office")
  public List<ShiftTimeTable> shiftTimeTable = Lists.newArrayList();

  @NotAudited
  @OneToMany(mappedBy = "office")
  public List<TotalOvertime> totalOvertimes = Lists.newArrayList();

  @NotAudited
  @OneToMany(mappedBy = "office")
  public List<Attachment> attachments = Lists.newArrayList();
  
  @NotAudited
  @OneToMany(mappedBy = "office")
  public List<MealTicket> tickets = Lists.newArrayList();

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
      if (configuration.epasParam.equals(epasParam)) {
        configurations.add(configuration);
      }
    }
    return configurations;
  }

  /**
   * @param param Parametro di configurazione da controllare.
   * @param value valore atteso
   * @return true se l'ufficio contiene il parametro di configurazione specificato con il valore
   *        indicato
   */
  public boolean checkConf(EpasParam param, String value) {
    return configurations.stream().anyMatch(conf -> conf.epasParam == param
        && conf.fieldValue.equals(value));
  }

}
