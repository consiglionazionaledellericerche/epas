package models.dto;

import java.util.List;
import models.User;

public class SeatSituationDto {

  public List<User> technicalAdmins;
  public List<User> personnelAdmins;
  public List<User> seatSupervisors;
  public List<User> shiftSupervisors;
  public List<User> reperibilitySupervisors;
  public List<User> mealTicketsManager;
  public List<User> registryManagers;
  public List<User> groupManagers;
}
