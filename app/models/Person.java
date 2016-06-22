package models;


import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import it.cnr.iit.epas.NullStringBinder;

import manager.configurations.EpasParam;

import models.base.IPropertiesInPeriodOwner;
import models.base.IPropertyInPeriod;
import models.base.PeriodModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import play.data.binding.As;
import play.data.validation.Email;
import play.data.validation.Required;
import play.data.validation.Unique;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

/**
 * Entity per le persone.
 *
 * @author cristian
 */

/**
 * IMPORTANTE: relazione con user impostata a LAZY per non scaricare tutte le informazioni della
 * persona in fase di personDao.list. Necessaria comunque la join con le relazioni OneToOne.
 */
@Entity
@Audited
@Table(name = "persons")
public class Person extends PeriodModel implements IPropertiesInPeriodOwner {

  private static final long serialVersionUID = -2293369685203872207L;

  @Column(name = "perseo_id")
  public Long perseoId;

  @Version
  public Integer version;

  @Required
  public String name;

  @Required
  public String surname;

  @Column(name = "other_surnames")
  public String othersSurnames;

  @Column(name = "birthday")
  public LocalDate birthday;

  @Email
  @Unique
  @As(binder = NullStringBinder.class)
  @Required
  public String email;

  @OneToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  public User user;

  /**
   * Numero di matricola.
   */
  @Unique
  public Integer number;


  /**
   * id che questa persona aveva nel vecchio database.
   */
  public Long oldId;

  /**
   * Internal ID: server per l'identificazione univoca della persona nella sincronizzazione con
   * Perseo (Person.id di Perseo).
   */
  @Unique
  public Integer iId;

  /**
   * Campo da usarsi in caso di autenticazione via shibboleth.
   */
  @Unique
  @As(binder = NullStringBinder.class)
  public String eppn;

  public String telephone;

  public String fax;

  public String mobile;

  @Column(name = "want_email")
  public boolean wantEmail;

  /**
   * i successivi due campi servono per la nuova relazione tra Person e Person relativa ai
   * responsabili.
   */
  @OneToMany(mappedBy = "personInCharge")
  @OrderBy("surname")
  public List<Person> people = Lists.newArrayList();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "person_in_charge")
  @Nullable
  public Person personInCharge;

  /**
   * questo campo booleano serve a stabilire se una persona è un responsabile o no.
   */
  @Column(name = "is_person_in_charge")
  public boolean isPersonInCharge;

  /**
   * relazione con i turni.
   */
  @OneToMany(mappedBy = "supervisor")
  public List<ShiftCategories> shiftCategories = Lists.newArrayList();

  @NotAudited
  @OneToMany(mappedBy = "person", cascade = {CascadeType.REMOVE})
  public List<Contract> contracts = Lists.newArrayList();

  /**
   * relazione con la tabella dei figli del personale.
   */
  @OneToMany(mappedBy = "person", cascade = {CascadeType.REMOVE})
  public Set<PersonChildren> personChildren = Sets.newHashSet();

  /**
   * relazione con la nuova tabella dei person day.
   */
  @OneToMany(mappedBy = "person", cascade = {CascadeType.REMOVE})
  public List<PersonDay> personDays = Lists.newArrayList();

  @OneToMany(mappedBy = "person", cascade = {CascadeType.REMOVE})
  public List<CertificatedData> certificatedData = Lists.newArrayList();


  @OneToMany(mappedBy = "admin")
  public List<MealTicket> mealTicketsAdmin = Lists.newArrayList();

  /**
   * relazione con la nuova tabella dei person_month.
   */
  @NotAudited
  @OneToMany(mappedBy = "person", cascade = {CascadeType.REMOVE})
  public List<PersonMonthRecap> personMonths = Lists.newArrayList();

  /**
   * relazione con la tabella Competence.
   */
  @NotAudited
  @OneToMany(mappedBy = "person", cascade = {CascadeType.REMOVE})
  public List<Competence> competences = Lists.newArrayList();

  /**
   * relazione con la tabella dei codici competenza per stabilire se una persona ha diritto o meno a
   * una certa competenza.
   */
  @NotAudited
  @ManyToMany(cascade = {CascadeType.REFRESH})
  public List<CompetenceCode> competenceCode = Lists.newArrayList();

  @OneToOne(mappedBy = "person")
  public PersonHourForOvertime personHourForOvertime;

  @OneToOne(mappedBy = "person", cascade = {CascadeType.REMOVE})
  public PersonReperibility reperibility;

  @OneToOne(mappedBy = "person")
  public PersonShift personShift;

  @ManyToOne
  @JoinColumn(name = "qualification_id")
  @Required
  public Qualification qualification;

  @ManyToOne
  @JoinColumn(name = "office_id")
  @Required
  public Office office;

  /**
   * TODO: da rimuovere quando si userà lo storico per intercettare il cambio di sede per adesso è
   * popolato dal valore su perseo alla costruzione dell'oggetto.
   */
  @Transient
  public Long perseoOfficeId = null;

  /**
   * Rimuoverlo quando sarà stata effettuata la migrazione di tutti i badge alla tabella badges.
   */
  @Deprecated
  @As(binder = NullStringBinder.class)
  public String badgeNumber;

  /**
   * Sono stati implementati i metodi Equals e HashCode sulla classe Badge in modo che Se sono
   * presenti più badge per la persona che differiscono solo per il campo badgeReader venga
   * restituito un solo elemento (effettivamente per noi è lo stesso badge).Quindi person.badges non
   * restituisce i duplicati
   */
  @OneToMany(mappedBy = "person", cascade = {CascadeType.REMOVE})
  public Set<Badge> badges = Sets.newHashSet();
  
  /**
   * Le configurazioni della persona.
   */
  @OneToMany(mappedBy = "person", cascade = {CascadeType.REMOVE})
  public List<PersonConfiguration> personConfigurations = Lists.newArrayList();



  public String getName() {
    return this.name;
  }

  public String getSurname() {
    return this.surname;
  }

  public void setCompetenceCodes(List<CompetenceCode> competenceCode) {
    this.competenceCode = competenceCode;
  }

  /**
   * @return il nome completo.
   */
  public String getFullname() {
    return String.format("%s %s", surname, name);
  }


  public String fullName() {
    return getFullname();
  }

  @Override
  public String toString() {
    return getFullname();
  }

  @Override
  public Collection<IPropertyInPeriod> periods(Object type) {
    
    if (type.getClass().equals(EpasParam.class)) {
      return (Collection<IPropertyInPeriod>)filterConfigurations((EpasParam)type);
    }
    return null;
  }
  
  @Override
  public Collection<Object> types() {
    return Sets.newHashSet(Arrays.asList(EpasParam.values()));
  }
  
  /**
   * Filtra dalla lista di configurations le occorrenze del tipo epasParam.
   * @param epasPersonParam filtro
   * @return insieme filtrato
   */
  private Set<IPropertyInPeriod> filterConfigurations(EpasParam epasPersonParam) {
    Set<IPropertyInPeriod> configurations = Sets.newHashSet();
    for (PersonConfiguration configuration : this.personConfigurations) {
      if (configuration.epasParam.equals(epasPersonParam)) {
        configurations.add(configuration);
      }
    }
    return configurations;
  }

}
