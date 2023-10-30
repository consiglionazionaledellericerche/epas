package manager;

import com.google.common.collect.Maps;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.GroupOvertimeDao;
import lombok.extern.slf4j.Slf4j;
import models.Competence;
import models.CompetenceCode;
import models.GroupOvertime;
import models.Person;
import models.TotalOvertime;
import models.dto.PersonOvertimeInMonth;
import models.flows.Group;

@Slf4j
public class GroupOvertimeManager {
  
  private final CompetenceDao competenceDao;
  private final CompetenceManager competenceManager;
  private final GroupOvertimeDao groupOvertimeDao;
  private static CompetenceCodeDao competenceCodeDao;
  
  @Inject
  public GroupOvertimeManager(CompetenceDao competenceDao, CompetenceManager competenceManager, 
      GroupOvertimeDao groupOvertimeDao, CompetenceCodeDao competenceCodeDao) {
    this.competenceDao = competenceDao;
    this.competenceManager = competenceManager;
    this.groupOvertimeDao = groupOvertimeDao;
    this.competenceCodeDao = competenceCodeDao;
  }

  public boolean checkOvertimeAvailability(GroupOvertime groupOvertime, int year) {
    
    int totalGroupOvertimes = groupOvertime.getGroup().getGroupOvertimes().stream()
        .filter(go -> go.getYear().equals(LocalDate.now().getYear()))
        .mapToInt(go -> go.getNumberOfHours()).sum();
    
    List<TotalOvertime> totalList = competenceDao
        .getTotalOvertime(LocalDate.now().getYear(), groupOvertime.getGroup().getOffice());
    int totale = competenceManager.getTotalOvertime(totalList);
    
    List<Group> groupList = groupOvertime.getGroup().getOffice().getGroups().stream()
        .filter(g -> !g.getName().equals(groupOvertime.getGroup().getName()))
        .collect(Collectors.toList());
    int groupOvertimeSum = 0;
    for (Group otherGroup : groupList)  {
      List<GroupOvertime> groupOvertimeList = groupOvertimeDao
          .getByYearAndGroup(LocalDate.now().getYear(), otherGroup);
      groupOvertimeSum = groupOvertimeSum + groupOvertimeList.stream()
      .mapToInt(go -> go.getNumberOfHours()).sum();
    }
    int hoursAvailable = totale - groupOvertimeSum - totalGroupOvertimes;
    if (hoursAvailable - groupOvertime.getNumberOfHours() >= 0) {
      return true;
    }    
    return false;
  }
  
  public Map<Integer, List<PersonOvertimeInMonth>> groupOvertimeSituationInYear(List<Person> people, int year) {
    CompetenceCode code = competenceCodeDao.getCompetenceCodeByCode("S1");
    Map<Integer, List<PersonOvertimeInMonth>> releaseMap = Maps.newHashMap();
    Map<Person, List<Competence>> map = competenceDao.competencesInYear(people, year, code);
    
    for (Map.Entry<Person, List<Competence>> entry : map.entrySet()) {
      
    }
    
    return releaseMap;
  }
}
