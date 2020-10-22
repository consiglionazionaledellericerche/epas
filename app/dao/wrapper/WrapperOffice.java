package dao.wrapper;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import dao.RoleDao;
import java.util.List;
import models.Office;
import models.Role;
import models.UsersRolesOffices;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

/**
 * Wrapper sede.
 *
 * @author alessandro
 */
public class WrapperOffice implements IWrapperOffice {

  private final Office value;
  private final RoleDao roleDao;

  @Inject
  WrapperOffice(@Assisted Office office,
      RoleDao roleDao) {
    value = office;
    this.roleDao = roleDao;
  }

  @Override
  public final Office getValue() {
    return value;
  }

  @Override
  public final LocalDate initDate() {
    return this.value.getBeginDate();
  }


  /**
   * I Responsabili Sede dell'office (di norma 1).
   */
  public List<UsersRolesOffices> getSeatSupervisor() {
    Role role = roleDao.getRoleByName(Role.SEAT_SUPERVISOR);
    return filterUros(role);
  }
  
  /**
   * Gli amministratori tecnici dell'office.
   */
  public List<UsersRolesOffices> getTechnicalAdmins() {
    Role role = roleDao.getRoleByName(Role.TECHNICAL_ADMIN);
    return filterUros(role);
  }

  /**
   * Gli amministratori dell'office.
   */
  public List<UsersRolesOffices> getPersonnelAdmins() {

    Role role = roleDao.getRoleByName(Role.PERSONNEL_ADMIN);
    return filterUros(role);
  }

  /**
   * I mini amministratori dell'office.
   */
  public List<UsersRolesOffices> getMiniAdmins() {

    Role role = roleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI);
    return filterUros(role);
  }
  
  /**
   * Lista di gestori buoni basto.
   * @return i gestori dei buoni pasto
   */
  public List<UsersRolesOffices> getMealTicketManagers() {
    Role role = roleDao.getRoleByName(Role.MEAL_TICKET_MANAGER);
    return filterUros(role);
  }
  
  /**
   * Lista di gestori anagrafica.
   * @return i gestori dell'anagrafica
   */
  public List<UsersRolesOffices> getRegistryManagers() {
    Role role = roleDao.getRoleByName(Role.REGISTRY_MANAGER);
    return filterUros(role);
  }

  /**
   * Il primo mese per cui è possibile effettuare l'upload degli attestati.
   *
   * @return yearMonth se esiste.
   */
  @Override
  public Optional<YearMonth> getFirstMonthUploadable() {

    LocalDate officeInitUse = this.value.getBeginDate();

    return Optional.fromNullable(new YearMonth(officeInitUse));

  }


  /**
   * La lista degli anni di cui è possibile effettuare l'invio degli attestati per la sede.
   *
   * @return lista degli anni
   */
  @Override
  public List<Integer> getYearUploadable() {

    List<Integer> years = Lists.newArrayList();

    Optional<YearMonth> firstMonthUploadable = getFirstMonthUploadable();
    if (!firstMonthUploadable.isPresent()) {
      return years;
    }

    int officeYearFrom = getFirstMonthUploadable().get().getYear();
    // anni in cui possono essere inviati gli attestati (installazione sede fino ad oggi)
    int officeYearTo = LocalDate.now().getYear();
    if (officeYearFrom > officeYearTo) {
      officeYearTo = officeYearFrom;
    }
    for (int yearFrom = officeYearFrom; yearFrom <= officeYearTo; yearFrom++) {
      years.add(yearFrom);
    }
    return years;
  }

  /**
   * La lista dei mesi di cui è possibile effettuare l'invio degli attestati.
   *
   * @return lista dei mesi
   */
  public List<Integer> getMonthUploadable() {
    return Lists.newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
  }

  /**
   * Il mese di cui presumubilmente occorre fare l'invio attestati. (Il precedente rispetto a
   * quello attuale se non è precedente al primo mese per attestati).
   *
   * @return prossimo mese da inviare
   */
  @Override
  public Optional<YearMonth> nextYearMonthToUpload() {

    // mese scorso preselezionato (se esiste) oppure installazione sede
    YearMonth previousYearMonth = new YearMonth(LocalDate.now().minusMonths(1));

    Optional<YearMonth> first = getFirstMonthUploadable();
    if (!first.isPresent()) {
      return Optional.<YearMonth>absent();
    }

    if (previousYearMonth.isBefore(first.get())) {
      return first;
    }
    return Optional.fromNullable(previousYearMonth);
  }

  /**
   * Se il mese passato come parametro è inviabile.
   *
   * @param yearMonth mese da verificare
   * @return esito
   */
  @Override
  public boolean isYearMonthUploadable(YearMonth yearMonth) {
    Optional<YearMonth> first = getFirstMonthUploadable();
    if (!first.isPresent()) {
      return false;
    }
    if (yearMonth.isBefore(first.get())) {
      return false;
    }
    return true;
  }
  
  private List<UsersRolesOffices> filterUros(Role role) {
    List<UsersRolesOffices> uroList = Lists.newArrayList();
    for (UsersRolesOffices uro : this.value.usersRolesOffices) {

      if (uro.office.id.equals(this.value.id) && uro.role.id.equals(role.id)
          && uro.user.person != null) {
        uroList.add(uro);
      }
    }
    return uroList;
  }

}
