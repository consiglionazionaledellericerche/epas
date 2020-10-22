package models;

import com.google.common.collect.Lists;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import models.base.BaseModel;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name = "roles")
public class Role extends BaseModel {

  public static final String SEAT_SUPERVISOR = "seatSupervisor";
  public static final String PERSONNEL_ADMIN = "personnelAdmin";
  public static final String PERSONNEL_ADMIN_MINI = "personnelAdminMini";
  public static final String EMPLOYEE = "employee";
  public static final String BADGE_READER = "badgeReader";
  public static final String REST_CLIENT = "restClient";
  public static final String TECHNICAL_ADMIN = "technicalAdmin";
  public static final String SHIFT_MANAGER = "shiftManager";
  public static final String REPERIBILITY_MANAGER = "reperibilityManager";
  public static final String GROUP_MANAGER = "groupManager";
  public static final String MEAL_TICKET_MANAGER = "mealTicketManager";
  public static final String REGISTRY_MANAGER = "registryManager";
  public static final String PERSON_DAY_READER = "personDayReader";
  private static final long serialVersionUID = 6717202212924325368L;
  public String name;

  
  @OneToMany(mappedBy = "role", cascade = CascadeType.REMOVE, orphanRemoval = true)
  public List<UsersRolesOffices> usersRolesOffices = Lists.newArrayList();

  @Override
  public String toString() {
    if (name.equals(SEAT_SUPERVISOR)) {
      return "Responsabile Sede";
    }
    if (name.equals(PERSONNEL_ADMIN)) {
      return "Amministratore Personale";
    }
    if (name.equals(PERSONNEL_ADMIN_MINI)) {
      return "Amministratore Personale Sola lettura";
    }
    if (name.equals(TECHNICAL_ADMIN)) {
      return "Amministratore Tecnico";
    }
    if (name.equals(EMPLOYEE)) {
      return "Dipendente";
    }
    if (name.equals(REPERIBILITY_MANAGER)) {
      return "Gestore reperibilità";
    }
    if (name.equals(SHIFT_MANAGER)) {
      return "Gestore turni";
    }
    if (name.equals(BADGE_READER)) {
      return "Lettore di badge";
    }
    if (name.equals(REST_CLIENT)) {
      return "Client rest";
    }
    if (name.equals(GROUP_MANAGER)) {
      return "Responsabile gruppo";
    }
    if (name.equals(MEAL_TICKET_MANAGER)) {
      return "Gestore buoni pasto";
    }
    if (name.equals(REGISTRY_MANAGER)) {
      return "Gestore anagrafica";
    }
    if (name.equals(PERSON_DAY_READER)) {
      return "Lettore informazioni";
    }
    return name;
  }

}
