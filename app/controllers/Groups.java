package controllers;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import dao.GroupDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDao.PersonLite;
import lombok.extern.slf4j.Slf4j;
import models.Office;
import models.Person;
import models.User;
import models.flows.Group;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@Slf4j
@With({Resecure.class})
public class Groups extends Controller {

  @Inject
  private static SecurityRules rules;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static GroupDao groupDao;
  @Inject
  private static PersonDao personDao;
  
  public static void createGroup() {
    
  }
  
  public static void deleteGroup() {
    
  }
  
  public static void showGroups(Long officeId) {
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    
    rules.checkIfPermitted(office);
    List<Group> groups = groupDao.groupsByOffice(office);
    render(groups, office);
  }
  
  public static void edit(long groupId) {
    
  }
  
  public static void blank(long officeId) {
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    
    rules.checkIfPermitted(office);
    
    List<Person> people = personDao.list(Optional.<String>absent(), 
        Sets.newHashSet(office), false, LocalDate.now().dayOfMonth().withMinimumValue(), 
        LocalDate.now().dayOfMonth().withMaximumValue(), true).list();
    render(people);
  }
 }
