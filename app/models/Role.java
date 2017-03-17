package models;

import com.google.common.collect.Lists;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Entity
@Audited
@Table(name = "roles")
public class Role extends BaseModel {

  public static final String PERSONNEL_ADMIN = "personnelAdmin";
  public static final String PERSONNEL_ADMIN_MINI = "personnelAdminMini";
  public static final String EMPLOYEE = "employee";
  public static final String BADGE_READER = "badgeReader";
  public static final String REST_CLIENT = "restClient";
  public static final String TECHNICAL_ADMIN = "technicalAdmin";
  public static final String SHIFT_MANAGER = "shiftManager";
  public static final String REPERIBILITY_MANAGER = "reperibilityManager";
  private static final long serialVersionUID = 6717202212924325368L;
  public String name;

  @NotAudited
  @OneToMany(mappedBy = "role", cascade = {CascadeType.REMOVE}, orphanRemoval = true)
  public List<UsersRolesOffices> usersRolesOffices = Lists.newArrayList();

  @Override
  public String toString() {
    if (name.equals(Role.PERSONNEL_ADMIN)) {
      return "Amministratore Personale";
    }
    if (name.equals(Role.PERSONNEL_ADMIN_MINI)) {
      return "Amministratore Personale Sola lettura";
    }
    if (name.equals(Role.TECHNICAL_ADMIN)) {
      return "Amministratore Tecnico";
    }
    if (name.equals(Role.EMPLOYEE)) {
      return "Dipendente";
    }
    if (name.equals(Role.REPERIBILITY_MANAGER)) {
      return "Gestore reperibilità";
    }
    if (name.equals(Role.SHIFT_MANAGER)) {
      return "Gestore turni";
    }
    if (name.equals(Role.BADGE_READER)) {
      return "Lettore di badge";
    }
    if (name.equals(Role.REST_CLIENT)) {
      return "Client rest";
    }
    return this.name;
  }

}
