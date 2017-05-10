package jobs;

import dao.RoleDao;
import dao.UsersRolesOfficesDao;

import lombok.extern.slf4j.Slf4j;

import manager.ShiftManager;

import models.Role;

import play.jobs.Job;
import play.jobs.OnApplicationStart;

import javax.inject.Inject;

@Slf4j
@OnApplicationStart(async = true)
public class ShiftmanagerFix extends Job {

  @Inject
  static ShiftManager shiftManager;
  @Inject
  static UsersRolesOfficesDao uroDao;
  @Inject
  static RoleDao roleDao;
  
  public void doJob() {
    Role role = roleDao.getRoleByName(Role.SHIFT_SUPERVISOR);
    if (uroDao.countSupervisors(role) == 0) {
      log.info("Verifico che ci siano dei supervisori presenti tra i turni per assegnargli il ruolo...");
      shiftManager.linkSupervisorToRole();
      log.info("Procedura terminata.");
    }

  }

}
