package models;

import com.google.common.collect.Lists;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Audited
@Table(name = "roles")
public class Role extends BaseModel {

  public static final String DEVELOPER = "developer";
  public static final String ADMIN = "admin";
  public static final String PERSONNEL_ADMIN = "personnelAdmin";
  public static final String PERSONNEL_ADMIN_MINI = "personnelAdminMini";
  public static final String EMPLOYEE = "employee";
  public static final String BADGE_READER = "badgeReader";
  public static final String REST_CLIENT = "restClient";
  public static final String TECNICAL_ADMIN = "tecnicalAdmin";
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
    if (name.equals(Role.TECNICAL_ADMIN)) {
      return "Amministratore Tecnico";
    }
    return this.name;
  }

}
