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

package helpers;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import controllers.Security;
import dao.AbsenceRequestDao;
import dao.AbsenceTypeDao;
import dao.BadgeReaderDao;
import dao.BadgeSystemDao;
import dao.CategoryGroupAbsenceTypeDao;
import dao.CompetenceCodeDao;
import dao.CompetenceRequestDao;
import dao.ContractualReferenceDao;
import dao.GeneralSettingDao;
import dao.GroupDao;
import dao.InformationRequestDao;
import dao.MemoizedCollection;
import dao.MemoizedResults;
import dao.NotificationDao;
import dao.NotificationDao.NotificationFilter;
import dao.OfficeDao;
import dao.PersonDao;
import dao.QualificationDao;
import dao.RoleDao;
import dao.ShiftDao;
import dao.TimeSlotDao;
import dao.UserDao;
import dao.UsersRolesOfficesDao;
import dao.WorkingTimeTypeDao;
import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperFactory;
import helpers.jpa.ModelQuery;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import manager.SecureManager;
import manager.attestati.service.AttestatiApis;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import models.BadgeReader;
import models.BadgeSystem;
import models.CompetenceCode;
import models.CompetenceCodeGroup;
import models.Institute;
import models.Notification;
import models.Office;
import models.Person;
import models.Qualification;
import models.Role;
import models.TimeSlot;
import models.User;
import models.UsersRolesOffices;
import models.WorkingTimeType;
import models.absences.AbsenceType;
import models.absences.AmountType;
import models.absences.CategoryGroupAbsenceType;
import models.absences.GroupAbsenceType;
import models.base.InformationRequest;
import models.contractual.ContractualReference;
import models.enumerate.InformationType;
import models.enumerate.LimitType;
import models.enumerate.StampTypes;
import models.enumerate.TeleworkStampTypes;
import models.flows.AbsenceRequest;
import models.flows.CompetenceRequest;
import models.flows.Group;
import models.flows.enumerate.AbsenceRequestType;
import models.flows.enumerate.CompetenceRequestType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import play.Play;
import synch.diagnostic.SynchDiagnostic;

/**
 * Metodi usabili nel template.
 *
 * @author Alessandro Martelli
 */
public class TemplateUtility {
  
  private static final String WORKDAYS_REP = "207";
  private static final String HOLIDAYS_REP = "208";
  private static final String FLOWS_ACTIVE = "flows.active";

  private final SecureManager secureManager;
  private final OfficeDao officeDao;
  private final PersonDao personDao;
  private final UserDao userDao;
  private final QualificationDao qualificationDao;
  private final AbsenceTypeDao absenceTypeDao;
  private final RoleDao roleDao;
  private final BadgeReaderDao badgeReaderDao;
  private final WorkingTimeTypeDao workingTimeTypeDao;
  private final IWrapperFactory wrapperFactory;
  private final BadgeSystemDao badgeSystemDao;
  private final CategoryGroupAbsenceTypeDao categoryGroupAbsenceTypeDao;
  private final ContractualReferenceDao contractualReferenceDao;
  private final SynchDiagnostic synchDiagnostic;
  private final ConfigurationManager configurationManager;
  private final CompetenceCodeDao competenceCodeDao;
  private final MemoizedCollection<Notification> notifications;
  private final MemoizedCollection<Notification> archivedNotifications;
  private final AbsenceRequestDao absenceRequestDao;
  private final UsersRolesOfficesDao uroDao;
  private final GroupDao groupDao;
  private final TimeSlotDao timeSlotDao;
  private final CompetenceRequestDao competenceRequestDao;
  private final InformationRequestDao informationRequestDao;
  private final GeneralSettingDao generalSettingDao;
  
  /**
   * Costruttotore di default per l'injection dei vari componenti.
   */
  @Inject
  public TemplateUtility(
      SecureManager secureManager, OfficeDao officeDao, PersonDao personDao,
      QualificationDao qualificationDao, AbsenceTypeDao absenceTypeDao,
      RoleDao roleDao, BadgeReaderDao badgeReaderDao, WorkingTimeTypeDao workingTimeTypeDao,
      IWrapperFactory wrapperFactory, BadgeSystemDao badgeSystemDao,
      SynchDiagnostic synchDiagnostic, ConfigurationManager configurationManager,
      CompetenceCodeDao competenceCodeDao, ShiftDao shiftDao, 
      AbsenceComponentDao absenceComponentDao,
      NotificationDao notificationDao, UserDao userDao,
      CategoryGroupAbsenceTypeDao categoryGroupAbsenceTypeDao,
      ContractualReferenceDao contractualReferenceDao, AbsenceRequestDao absenceRequestDao,
      UsersRolesOfficesDao uroDao, GroupDao groupDao, TimeSlotDao timeSlotDao,
      CompetenceRequestDao competenceRequestDao, InformationRequestDao informationRequestDao,
      GeneralSettingDao generalSettingDao) {

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
    this.configurationManager = configurationManager;
    this.competenceCodeDao = competenceCodeDao;
    this.userDao = userDao;
    this.categoryGroupAbsenceTypeDao = categoryGroupAbsenceTypeDao;
    this.contractualReferenceDao = contractualReferenceDao;
    this.absenceRequestDao = absenceRequestDao;
    this.uroDao = uroDao;
    this.groupDao = groupDao;
    this.timeSlotDao = timeSlotDao;
    this.competenceRequestDao = competenceRequestDao;
    this.informationRequestDao = informationRequestDao;
    this.generalSettingDao = generalSettingDao;
    
    notifications = MemoizedResults
        .memoize(new Supplier<ModelQuery.SimpleResults<Notification>>() {
          @Override
          public ModelQuery.SimpleResults<Notification> get() {
            return notificationDao.listFor(Security.getUser().get(), Optional.absent(),
                Optional.of(NotificationFilter.TO_READ), Optional.absent());
          }
        });

    archivedNotifications = MemoizedResults
        .memoize(new Supplier<ModelQuery.SimpleResults<Notification>>() {
          @Override
          public ModelQuery.SimpleResults<Notification> get() {
            return notificationDao.listFor(Security.getUser().get(), Optional.absent(), 
                Optional.of(NotificationFilter.ARCHIVED), Optional.absent());
          }
        });    
  }

  /**
   * Verifica se nella configurazione posso abilitare l'auto inserimento covid19.
   *
   * @return se nella configurazione generale ho abilitato il covid19 come parametro per 
   *        auto inserimento.
   */
  public boolean enableCovid() {
    return generalSettingDao.generalSetting().isEnableAutoconfigCovid19();
  }
  
  /**
   * Verifica se nella configurazione posso abilitare l'auto inserimento smartworking.
   *
   * @return se nella configurazione generale ho abilitato lo smartworking come parametro per 
   *        auto inserimento.
   */
  public boolean enableSmartworking() {
    return generalSettingDao.generalSetting().isEnableAutoconfigSmartworking();
  }

  /**
   * Verifica nella configurazione generale se il flusso per la richiesta malattia è attivo.
   */
  public boolean enableIllnessFlow() {
    return generalSettingDao.generalSetting().isEnableIllnessFlow();
  }

  /**
   * Verifica la presenza giornaliera.
   *
   * @return se è attiva la presenza giornaliera per il responsabile di gruppo.
   */
  public boolean enableDailyPresenceForManager() {
    return generalSettingDao.generalSetting().isEnableDailyPresenceForManager();
  }

  /**
   * Metodo di utilità per far comparire il badge con la quantità di richieste di riposi 
   * compensativi da approvare nel template.
   *
   * @return la quantità di riposi compensativi da approvare.
   */
  public final int compensatoryRestRequests() {
    User user = Security.getUser().get();
    if (user.isSystemUser()) {
      return 0;
    }
    List<UsersRolesOffices> roleList = uroDao.getUsersRolesOfficesByUser(user);
    List<Group> groups = 
        groupDao.groupsByOffice(
            user.getPerson().getOffice(), Optional.absent(), Optional.of(false));
    Set<AbsenceRequest> results = absenceRequestDao
        .toApproveResults(roleList, Optional.absent(), Optional.absent(), 
            AbsenceRequestType.COMPENSATORY_REST, groups, user.getPerson());
    return results.size();
  }
  
  /**
   * Metodo di utiiltà per far comparire il badge con la quantità di richieste ferie da approvare 
   * nel template.
   *
   * @return la quantità di richieste ferie da approvare.
   */
  public final int vacationRequests() {
    User user = Security.getUser().get();
    if (user.isSystemUser()) {
      return 0;
    }
    List<UsersRolesOffices> roleList = uroDao.getUsersRolesOfficesByUser(user);
    List<Group> groups = 
        groupDao.groupsByOffice(
            user.getPerson().getOffice(), Optional.absent(), Optional.of(false));
    Set<AbsenceRequest> results = absenceRequestDao
        .toApproveResults(roleList, Optional.absent(), Optional.absent(), 
            AbsenceRequestType.VACATION_REQUEST, groups, user.getPerson());

    return results.size();
  }
  
  /**
   * Metodo di utiiltà per far comparire il badge con la quantità di richieste di permesso personale
   * da approvare nel template.
   *
   * @return la quantità di richieste di permesso personale da approvare.
   */
  public final int personalPermissionRequests() {
    User user = Security.getUser().get();
    if (user.isSystemUser()) {
      return 0;
    }
    List<UsersRolesOffices> roleList = uroDao.getUsersRolesOfficesByUser(user);
    List<Group> groups = 
        groupDao.groupsByOffice(
            user.getPerson().getOffice(), Optional.absent(), Optional.of(false));
    Set<AbsenceRequest> results = absenceRequestDao
        .toApproveResults(roleList, Optional.absent(), Optional.absent(), 
            AbsenceRequestType.PERSONAL_PERMISSION, groups, user.getPerson());

    return results.size();
  }
  
  /**
   * Metodo di utiiltà per far comparire il badge con la quantità di richieste ferie anno passato
   * post deadline da approvare nel template.
   *
   * @return la quantità di richieste ferie anno passato post deadline da approvare.
   */
  public final int vacationPastYearAfterDeadlineRequests() {
    User user = Security.getUser().get();
    if (user.isSystemUser()) {
      return 0;
    }
    List<UsersRolesOffices> roleList = uroDao.getUsersRolesOfficesByUser(user);
    List<Group> groups = 
        groupDao.groupsByOffice(
            user.getPerson().getOffice(), Optional.absent(), Optional.of(false));
    Set<AbsenceRequest> results = absenceRequestDao
        .toApproveResults(roleList, Optional.absent(), Optional.absent(), 
            AbsenceRequestType.VACATION_PAST_YEAR_AFTER_DEADLINE_REQUEST, groups, user.getPerson());

    return results.size();
  }

  /**
   * Metodo di utilità per far comparire il badge con la quantità di richieste di cambio di 
   * reperibilità da approvare nel template.
   *
   * @return la quantità di richieste di cambio di reperibilità attive.
   */
  public final int changeReperibilityRequests() {
    User user = Security.getUser().get();
    if (user.isSystemUser()) {
      return 0;
    }
    List<UsersRolesOffices> roleList = uroDao.getUsersRolesOfficesByUser(user);

    List<CompetenceRequest> results = competenceRequestDao
        .toApproveResults(roleList, 
            LocalDateTime.now().minusMonths(1), 
            Optional.absent(), CompetenceRequestType.CHANGE_REPERIBILITY_REQUEST, 
            user.getPerson());
    return results.size();
  }
  
  /**
   * Metodo di utilità per conteggiare le richieste pendenti di approvazione telelavoro. 
   *
   * @return la quantità di richieste di telelavoro pendenti.
   */
  public final int teleworkRequests() {
    User user = Security.getUser().get();
    if (user.isSystemUser()) {
      return 0;
    }
    List<UsersRolesOffices> roleList = uroDao.getUsersRolesOfficesByUser(user);
    List<InformationRequest> results = informationRequestDao
        .toApproveResults(roleList, Optional.absent(), Optional.absent(), 
            InformationType.TELEWORK_INFORMATION, user.getPerson());

    return results.size();
  }
  
  /**
   * Metodo di utilità per conteggiare le richieste pendenti di approvazione di uscite di servizio.
   *
   * @return la quantità di richieste di uscite di servizio pendenti.
   */
  public final int serviceRequests() {
    User user = Security.getUser().get();
    if (user.isSystemUser()) {
      return 0;
    }
    List<UsersRolesOffices> roleList = uroDao.getUsersRolesOfficesByUser(user);
    List<InformationRequest> results = informationRequestDao
        .toApproveResults(roleList, Optional.absent(), Optional.absent(), 
            InformationType.SERVICE_INFORMATION, user.getPerson());

    return results.size();
  }
  
  /**
   * Metodo di utilità per conteggiare le richieste pendenti di informazione malattia. 
   *
   * @return la quantità di richieste di informazione di malattia pendenti.
   */
  public final int illnessRequests() {
    User user = Security.getUser().get();
    if (user.isSystemUser()) {
      return 0;
    }
    List<UsersRolesOffices> roleList = uroDao.getUsersRolesOfficesByUser(user);
    List<InformationRequest> results = informationRequestDao
        .toApproveResults(roleList, Optional.absent(), Optional.absent(), 
            InformationType.ILLNESS_INFORMATION, user.getPerson());

    return results.size();
  }
  
  /**
   * Metodo di utilità per conteggiare le richieste pendenti di approvazione telelavoro. 
   *
   * @return la quantità di richieste di telelavoro pendenti.
   */
  public final int parentalLeaveRequests() {
    User user = Security.getUser().get();
    if (user.isSystemUser()) {
      return 0;
    }
    List<UsersRolesOffices> roleList = uroDao.getUsersRolesOfficesByUser(user);
    List<InformationRequest> results = informationRequestDao
        .toApproveResults(roleList, Optional.absent(), Optional.absent(), 
            InformationType.PARENTAL_LEAVE_INFORMATION, user.getPerson());

    return results.size();
  }


  /**
   * Metodo di utilità per il nome del mese.
   *
   * @param month numero mese nel formato stringa (ex: "1").
   * @return il nome del mese.
   */
  public final String monthName(final String month) {

    return DateUtility.fromIntToStringMonth(Integer.parseInt(month));
  }

  /**
   * Metodo di utilità per il nome del mese.
   *
   * @param month numero mese formato integer (ex: 1).
   * @return il nome del mese.
   */
  public final String monthName(final Integer month) {

    return DateUtility.fromIntToStringMonth(month);
  }

  /**
   * Metodo di utilità per aggiornare il mese successivo.
   *
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
   * Metodo di utilità per aggiornare all'anno successivo.
   *
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
   * Metodo di utilità per aggiornare al mese precedente.
   *
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
   * Metodo di utilità per aggiornare all'anno precedente.
   *
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

  /**
   *Liste di utilità per i template.
   */
  public List<Office> officesAllowed() {
    return secureManager.officesWriteAllowed(Security.getUser().get())
        .stream()
          .sorted((o, o1) -> o.getName().compareTo(o1.getName()))
          .collect(Collectors.toList());
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

  public List<TimeSlot> getEnabledTimeSlotsForOffice(Office office) {
    return timeSlotDao.getEnabledTimeSlotsForOffice(office);
  }
  
  public List<BadgeReader> getAllBadgeReader(Person person) {
    return badgeReaderDao.getBadgeReaderByOffice(person.getOffice());
  }

  public List<CompetenceCode> allCodeList() {
    return competenceCodeDao.getAllCompetenceCode();
  }

  public List<CompetenceCode> allCodesWithoutGroup() {
    return competenceCodeDao.getCodeWithoutGroup();
  }

  public List<CompetenceCode> allCodesContainingGroupCodes(CompetenceCodeGroup group) {
    return competenceCodeDao.allCodesContainingGroupCodes(group);
  }
    
  public List<CompetenceCode> allOnMonthlyPresenceCodes() {
    return competenceCodeDao.getCompetenceCodeByLimitType(LimitType.onMonthlyPresence);
  }

  public List<CategoryGroupAbsenceType> allCategoryGroupAbsenceTypes() {
    return categoryGroupAbsenceTypeDao.all();
  }
  
  public List<ContractualReference> allContractualReferences() {
    return contractualReferenceDao.all(Optional.of(false));
  }
  
  /**
   * Controlla se i flussi sono attivi.
   *
   * @return true se sono attivi i flussi su ePAS, false altrimenti.
   * @throws NoSuchFieldException lancia eccezione se non esiste il campo in conf.
   */
  public boolean isFlowsActive() throws NoSuchFieldException {
    if ("true".equals(Play.configuration.getProperty(FLOWS_ACTIVE))) {
      return true;
    }
    return false;
  }
  
  /**
   * Gli user associati a tutte le persone appartenenti all'istituto.
   */
  public List<User> usersInInstitute(Institute institute) {

    Set<Office> offices = Sets.newHashSet();
    offices.addAll(institute.getSeats());

    //FIXME per pagani metto provvisoriamente che il develop vede tutti.
    // Per poter nominarlo amministratore tecnico di ogni sede di milano
    // Decidere una soluzione migliore e meno sbrigativa.

    if (Security.getUser().get().getUsername().equals("developer")) {
      offices = Sets.newHashSet(officeDao.allOffices().list());
    }

    List<Person> personList = personDao.listPerseo(Optional.<String>absent(), offices, false,
        LocalDate.now(), LocalDate.now(), false).list();

    List<User> users = Lists.newArrayList();
    for (Person person : personList) {
      users.add(person.getUser());
    }

    return users;
  }

  /**
   * Persone assegnabili ad un certo utente dall'operatore corrente.
   *
   * @return Una lista delle persone assegnabili ad un certo utente dall'operatore corrente.
   */
  public List<PersonDao.PersonLite> assignablePeople() {
    return personDao.peopleInOffices(secureManager
        .officesTechnicalAdminAllowed(Security.getUser().get()));
  }

  /**
   * Metodo che ritora la lista dei ruoli assegnabili.
   *
   * @param office la sede per cui si cerca la lista dei ruoli
   * @return la lista dei ruoli assegnabili.
   */
  public List<Role> rolesAssignable(Office office) {

    List<Role> roles = Lists.newArrayList();

    // TODO: i ruoli impostabili sull'office dipendono da chi esegue la richiesta...
    // e vanno spostati nel secureManager.
    Optional<User> user = Security.getUser();
    if (user.isPresent()) {
      roles.add(roleDao.getRoleByName(Role.SEAT_SUPERVISOR));
      roles.add(roleDao.getRoleByName(Role.TECHNICAL_ADMIN));
      roles.add(roleDao.getRoleByName(Role.PERSONNEL_ADMIN));
      roles.add(roleDao.getRoleByName(Role.MEAL_TICKET_MANAGER));
      roles.add(roleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI));
      roles.add(roleDao.getRoleByName(Role.REGISTRY_MANAGER));
      return roles;
    }
    return roles;
  }

  /**
   * Metodo che ritorna la lista dei ruoli di sistema.
   *
   * @return la lista dei ruoli di sistema.
   */
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

  /**
   * Metodo che ritorna la lista dei ruoli "fisici".
   *
   * @return la lista dei ruoli assegnabili a persone fisiche.
   */
  public List<Role> allPhysicalRoles() {
    List<Role> roles = Lists.newArrayList();
    Optional<User> user = Security.getUser();
    if (user.isPresent()) {
      roles = rolesAssignable(user.get().getPerson().getOffice());
      roles.add(roleDao.getRoleByName(Role.EMPLOYEE));
      return roles;
    }
    return roles;
  }

  /**
   * Ritorna tutti i ruoli presenti.
   *
   * @return tutti i ruoli presenti.
   */
  public List<Role> getRoles() {
    return roleDao.getAll();
  }

  /**
   * Ritorna la lista degli uffici su cui l'utente ha ruolo di Technical Admin.
   *
   * @return tutti gli uffici sul quale l'utente corrente ha il ruolo di TECHNICAL_ADMIN.
   */
  public List<Office> getTechnicalAdminOffices() {
    return secureManager.officesTechnicalAdminAllowed(Security.getUser().get())
        .stream().sorted((o, o1) -> o.getName().compareTo(o1.getName()))
        .collect(Collectors.toList());
  }

  /**
   * Gli uffici che l'user può assegnare come owner ai BadgeReader. Il super admin può assegnarlo ad
   * ogni ufficio.
   */
  public List<Office> officeForBadgeReaders() {

    List<Office> offices = Lists.newArrayList();

    Optional<User> user = Security.getUser();

    // se admin tutti, altrimenti gli office di cui si ha technicalAdmin
    // TODO: spostare nel sucureManager
    if (!user.isPresent()) {
      return offices;
    }

    if (user.get().isSystemUser()) {
      return officeDao.getAllOffices();
    }

    for (UsersRolesOffices uro : user.get().getUsersRolesOffices()) {
      if (uro.getRole().getName().equals(Role.TECHNICAL_ADMIN)) {
        offices.add(uro.getOffice());
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
      for (UsersRolesOffices uro : user.getUsersRolesOffices()) {
        if (uro.getOffice().id.equals(office.id)) {
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


  /**
   * La lista dei gruppi badge per sede.
   *
   * @param office la sede di riferimento
   * @return la lista dei gruppi badge per sede.
   */
  public List<BadgeSystem> getConfiguredBadgeSystems(Office office) {
    List<BadgeSystem> configuredBadgeSystem = Lists.newArrayList();
    for (BadgeSystem badgeSystem : office.getBadgeSystems()) {
      if (!badgeSystem.getBadgeReaders().isEmpty()) {
        configuredBadgeSystem.add(badgeSystem);
      }
    }
    return configuredBadgeSystem;
  }

  
  public boolean hasAdminRole() {
    return userDao.hasAdminRoles(Security.getUser().get());
  }

  public List<StampTypes> getStampTypes() {
    return UserDao.getAllowedStampTypes(Security.getUser().get());
  }
  
  public List<TeleworkStampTypes> getTeleworkStampTypes() {
    return UserDao.getAllowedTeleworkStampTypes(Security.getUser().get());
  }

  /**
   * L'istanza del wrapperFactory disponibile nei template.
   *
   * @return wrapperFactory
   */
  public IWrapperFactory getWrapperFactory() {
    return this.wrapperFactory;
  }

  public SynchDiagnostic getSyncDiagnostic() {
    return this.synchDiagnostic;
  }

  public Object getObjectInConfiguration(EpasParam epasParam, String fieldValue) {
    return configurationManager.parseValue(epasParam, fieldValue);
  }

  public MemoizedCollection<Notification> getNotifications() {
    return notifications;
  }

  public MemoizedCollection<Notification> getArchivedNotifications() {
    return archivedNotifications;
  }


  /**
   * Verifica se un tipo di assenza è utilizzata come assenza di rimpiazzamento.
   */
  public boolean isReplacingCode(AbsenceType absenceType, GroupAbsenceType group) {
    if (group.getComplationAbsenceBehaviour() != null
        && group.getComplationAbsenceBehaviour().getReplacingCodes().contains(absenceType)) {
      return true;
    }
    return false;
  }

  /**
   * Verifica se un tipo di assenza è utilizzata come assenza di completamento.
   */
  public boolean isComplationCode(AbsenceType absenceType, GroupAbsenceType group) {
    if (group.getComplationAbsenceBehaviour() != null
        && group.getComplationAbsenceBehaviour().getComplationCodes().contains(absenceType)) {
      return true;
    }
    return false;
  }

  /**
   * Se l'assenza è prendibile, false altrimenti.
   *
   * @param absenceType il tipo di assenza
   * @param group il gruppo di tipi di assenza
   * @return se l'assenza è prendibile, false altrimenti
   */
  public boolean isTakableOnly(AbsenceType absenceType, GroupAbsenceType group) {
    if (group.getTakableAbsenceBehaviour() != null
        && group.getTakableAbsenceBehaviour().getTakableCodes().contains(absenceType)
        && !isComplationCode(absenceType, group)) {
      return true;
    }
    return false;
  }

  /**
   * Formatta l'ammontare nell'amountType fornito.
   *
   * @param amount     ammontare
   * @param amountType amountType
   * @return string
   */
  public String formatAmount(int amount, AmountType amountType) {
    if (amountType == null) {
      return "";
    }
    String format = "";
    if (amountType.equals(AmountType.units)) {
      if (amount == 0) {
        return "0 giorni"; // giorno lavorativo";
      }
      int units = amount / 100;
      int percent = amount % 100;
      String label = " giorni lavorativi";
      if (units == 1) {
        label = " giorno lavorativo";
      }
      if (units > 0 && percent > 0) {
        return units + label + " + " + percent + "% di un giorno lavorativo";
      } else if (units > 0) {
        return units + label;
      } else if (percent > 0) {
        return percent + "% di un giorno lavorativo";
      }
      return ((double) amount / 100) + " giorni";
    }
    if (amountType.equals(AmountType.minutes)) {
      if (amount == 0) {
        return "0 minuti";
      }
      int hours = amount / 60; //since both are ints, you get an int
      int minutes = amount % 60;

      if (hours > 0 && minutes > 0) {
        format = hours + " ore " + minutes + " minuti";
      } else if (hours > 0) {
        format = hours + " ore";
      } else if (minutes > 0) {
        format = minutes + " minuti";
      }
    }
    return format;
  }

  /**
   * Url del servizio Attestati.
   */
  public String getAttestatiUrl() {
    try {
      return AttestatiApis.getAttestatiBaseUrl();
    } catch (NoSuchFieldException e) {
      // Empty URL
      return "#";
    }
  }
  
  /**
   * Verifica se la persona è reperible in data odierna.
   *
   * @param person la persona di cui si intende sapere se è reperibile
   * @return se la persona è reperibile in data odierna.
   */
  public boolean isAvailable(Person person) {
    return person.getPersonCompetenceCodes().stream()
        .anyMatch(comp -> !comp.getBeginDate().isAfter(LocalDate.now()) 
            && (comp.getCompetenceCode().getCode().equalsIgnoreCase(WORKDAYS_REP) 
            || comp.getCompetenceCode().getCode().equalsIgnoreCase(HOLIDAYS_REP)));
    
  }
  
  /**
   * Lista di persone appartententi all'ufficio passato (in questo anno).
   *
   * @param office la sede per cui si ricercano le persone
   * @return la lista di persone della sede abili a far parte di un gruppo.
   */
  public List<Person> peopleForGroups(Office office) {
    List<Person> people = personDao.list(Optional.<String>absent(), 
        Sets.newHashSet(office), false, LocalDate.now().dayOfMonth().withMinimumValue(), 
        LocalDate.now().dayOfMonth().withMaximumValue(), true).list();
    return people;
  }

  /**
   * Sigla dell'ente/azienda che utilizza ePAS.
   */
  public String getCompanyCode() {
    return CompanyConfig.code();
  }

  /**
   * Nome dell'ente/azienda che utilizza ePAS.
   */
  public String getCompanyName() {
    return CompanyConfig.name();
  }

  /**
   * Indirizzo sito/web dell'ente/azienda che utilizza ePAS.
   */
  public String getCompanyUrl() {
    return CompanyConfig.url();
  }

  
  /**
   * Indica se è permessa la configurabilità delle richieste di assenza 
   * per i livelli I-III.
   */
  public boolean absenceRequestAuthorizationTopLevelEnabled() {
    return generalSettingDao.generalSetting().isEnableAbsenceTopLevelAuthorization();
  }
}
