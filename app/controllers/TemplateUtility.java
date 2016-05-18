package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.AbsenceTypeDao;
import dao.BadgeReaderDao;
import dao.BadgeSystemDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.QualificationDao;
import dao.RoleDao;
import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperFactory;

import it.cnr.iit.epas.DateUtility;

import manager.SecureManager;

import models.AbsenceType;
import models.BadgeReader;
import models.BadgeSystem;
import models.Institute;
import models.Office;
import models.Person;
import models.Qualification;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.WorkingTimeType;
import models.enumerate.AbsenceTypeMapping;
import models.enumerate.VacationCode;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import synch.diagnostic.SynchDiagnostic;

/**
 * Metodi usabili nel template.
 *
 * @author alessandro
 */
public class TemplateUtility {


  private final SecureManager secureManager;
  private final OfficeDao officeDao;
  private final PersonDao personDao;
  private final QualificationDao qualificationDao;
  private final AbsenceTypeDao absenceTypeDao;
  private final RoleDao roleDao;
  private final BadgeReaderDao badgeReaderDao;
  private final WorkingTimeTypeDao workingTimeTypeDao;
  private final IWrapperFactory wrapperFactory;
  private final BadgeSystemDao badgeSystemDao;
  private final SynchDiagnostic synchDiagnostic;

  @Inject
  public TemplateUtility(
      SecureManager secureManager, OfficeDao officeDao, PersonDao personDao,
      QualificationDao qualificationDao, AbsenceTypeDao absenceTypeDao,
      RoleDao roleDao, BadgeReaderDao badgeReaderDao, WorkingTimeTypeDao workingTimeTypeDao,
      IWrapperFactory wrapperFactory, BadgeSystemDao badgeSystemDao, SynchDiagnostic synchDiagnostic) {

    this.secureManager = secureManager;
    this.officeDao = officeDao;
    this.personDao = personDao;
    this.qualificationDao = qualificationDao;
    this.absenceTypeDao = absenceTypeDao;
    this.roleDao = roleDao;
    this.badgeReaderDao = badgeReaderDao;
    this.workingTimeTypeDao = workingTimeTypeDao;
    this.wrapperFactory = wrapperFactory;
    this.badgeSystemDao = badgeSystemDao;
    this.synchDiagnostic = synchDiagnostic;
  }


  /**
   * @param month numero mese nel formato stringa (ex: "1").
   * @return il nome del mese.
   */
  public final String monthName(final String month) {

    return DateUtility.fromIntToStringMonth(Integer.parseInt(month));
  }

  /**
   * @param month numero mese formato integer (ex: 1).
   * @return il nome del mese.
   */
  public final String monthName(final Integer month) {

    return DateUtility.fromIntToStringMonth(month);
  }

  /**
   * @param month mese di partenza.
   * @return mese successivo a mese di partenza.
   */
  public final int computeNextMonth(final int month) {
    if (month == DateUtility.DECEMBER) {
      return DateUtility.JANUARY;
    }
    return month + 1;
  }

  /**
   * @param month mese di partenza.
   * @param year  anno di partenza.
   * @return anno successivo al mese/anno di partenza.
   */
  public final int computeNextYear(final int month, final int year) {
    if (month == DateUtility.DECEMBER) {
      return year + 1;
    }
    return year;
  }

  /**
   * @param month mese di partenza.
   * @return mese precedente a mese di partenza.
   */
  public final int computePreviousMonth(final int month) {
    if (month == DateUtility.JANUARY) {
      return DateUtility.DECEMBER;
    }
    return month - 1;
  }

  /**
   * @param month mese di partenza.
   * @param year  anno di partenza.
   * @return anno precedente al mese/anno di partenza.
   */
  public final int computePreviousYear(final int month, final int year) {
    if (month == DateUtility.JANUARY) {
      return year - 1;
    }
    return year;
  }

  // Liste di utilità per i template

  public Set<Office> officesAllowed() {
    return secureManager.officesWriteAllowed(Security.getUser().get());
  }

  public List<Qualification> getAllQualifications() {
    return qualificationDao.findAll();
  }

  public List<AbsenceType> getCertificateAbsenceTypes() {
    return absenceTypeDao.certificateTypes();
  }

  public ImmutableList<String> getAllDays() {
    return ImmutableList.of("lunedì", "martedì", "mercoledì", "giovedì", "venerdì", "sabato",
            "domenica");
  }

  public List<WorkingTimeType> getEnabledWorkingTimeTypeForOffice(Office office) {
    return workingTimeTypeDao.getEnabledWorkingTimeTypeForOffice(office);
  }
  
  public List<BadgeReader> getAllBadgeReader(Person person) {
    return badgeReaderDao.getBadgeReaderByOffice(person.office);
  }

  /**
   * Gli user associati a tutte le persone appartenenti all'istituto.
   */
  public List<User> usersInInstitute(Institute institute) {

    Set<Office> offices = Sets.newHashSet();
    offices.addAll(institute.seats);

    //FIXME per pagani metto provvisoriamente che il develop vede tutti.
    // Per poter nominarlo amministratore tecnico di ogni sede di milano
    // Decidere una soluzione migliore e meno sbrigativa.

    if (Security.getUser().get().username.equals("developer")){
      offices = Sets.newHashSet(officeDao.allOffices().list());
    }

    List<Person> personList = personDao.listPerseo(Optional.<String>absent(), offices, false,
            LocalDate.now(), LocalDate.now(), true).list();

    List<User> users = Lists.newArrayList();
    for (Person person : personList) {
      users.add(person.user);
    }

    return users;
  }

  public List<Role> rolesAssignable(Office office) {

    List<Role> roles = Lists.newArrayList();

    // TODO: i ruoli impostabili sull'office dipendono da chi esegue la richiesta...
    // e vanno spostati nel secureManager.
    Optional<User> user = Security.getUser();
    if (user.isPresent()) {
      roles.add(roleDao.getRoleByName(Role.TECNICAL_ADMIN));
      roles.add(roleDao.getRoleByName(Role.PERSONNEL_ADMIN));
      roles.add(roleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI));
      return roles;
    }
    return roles;
  }
  
  public List<Role> allSystemRoles() {
    List<Role> roles = Lists.newArrayList();
    Optional<User> user = Security.getUser();
    if (user.isPresent()) {
      roles.add(roleDao.getRoleByName(Role.REPERIBILITY_MANAGER));
      roles.add(roleDao.getRoleByName(Role.SHIFT_MANAGER));
      roles.add(roleDao.getRoleByName(Role.REST_CLIENT));
      return roles;
    }
    return roles;
  }
  
  public List<Role> allPhysicalRoles(){
    List<Role> roles = Lists.newArrayList();
    Optional<User> user = Security.getUser();
    if (user.isPresent()) {
      roles = rolesAssignable(user.get().person.office);
      roles.add(roleDao.getRoleByName(Role.EMPLOYEE));
      return roles;
    }
    return roles;
  }

  /**
   * Gli uffici che l'user può assegnare come owner ai BadgeReader. Il super admin può assegnarlo ad
   * ogni ufficio.
   */
  public List<Office> officeForBadgeReaders() {

    List<Office> offices = Lists.newArrayList();

    Optional<User> user = Security.getUser();

    // se admin tutti, altrimenti gli office di cui si ha tecnicalAdmin
    // TODO: spostare nel sucureManager
    if (!user.isPresent()) {
      return offices;
    }

    if (user.get().isSuperAdmin()) {
      return officeDao.getAllOffices();
    }

    for (UsersRolesOffices uro : user.get().usersRolesOffices) {
      if (uro.role.name.equals(Role.TECNICAL_ADMIN)) {
        offices.add(uro.office);
      }
    }
    return offices;
  }

  /**
   * Gli account di tutti i badgeReader non ancora assegnati ad office.
   */
  public List<User> badgeReaderUserForOffice(Office office) {

    List<User> users = Lists.newArrayList();

    List<User> badgeReaders = badgeReaderDao.usersBadgeReader();
    for (User user : badgeReaders) {
      boolean insert = true;
      for (UsersRolesOffices uro : user.usersRolesOffices) {
        if (uro.office.id.equals(office.id)) {
          insert = false;
          break;
        }
      }
      if (insert) {
        users.add(user);
      }
    }
    return users;
  }

  /**
   * Tutti i badge system.
   */
  public List<BadgeSystem> allBadgeSystem() {

    return badgeSystemDao.badgeSystems(Optional.<String>absent(),
        Optional.<BadgeReader>absent()).list();
  }

  public List<BadgeSystem> getConfiguredBadgeSystems(Office office) {
    List<BadgeSystem> configuredBadgeSystem = Lists.newArrayList();
    for (BadgeSystem badgeSystem : office.badgeSystems) {
      if (!badgeSystem.badgeReaders.isEmpty()) {
        configuredBadgeSystem.add(badgeSystem);
      }
    }
    return configuredBadgeSystem;
  }

  /**
   * I codici di assenza ordinati dai più utilizzati.
   */
  public List<AbsenceType> frequentAbsenceTypeList() {

    Optional<AbsenceType> ferCode = absenceTypeDao
            .getAbsenceTypeByCode(AbsenceTypeMapping.FERIE_FESTIVITA_SOPPRESSE_EPAS.getCode());
    Preconditions.checkState(ferCode.isPresent());

    return FluentIterable.from(Lists.newArrayList(ferCode.get()))
            .append(absenceTypeDao.getFrequentTypes()).toList();
  }

  /**
   * I codici di assenza attivi ordinati per codice.
   */
  public List<AbsenceType> allAbsenceCodes(LocalDate date) {
    return absenceTypeDao.getAbsenceTypeFromEffectiveDate(date);
  }


  /**
   * L'istanza del wrapperFactory disponibile nei template.
   * @return wrapperFactory
   */
  public IWrapperFactory getWrapperFactory() {
    return this.wrapperFactory;
  }
  
  public SynchDiagnostic getSyncDiagnostic() {
    return this.synchDiagnostic;
  }
}
