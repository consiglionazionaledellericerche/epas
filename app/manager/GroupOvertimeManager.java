/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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

package manager;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.event.ListSelectionEvent;
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

  /**
   * Controlla se è possibile assegnare lo straordinario richiesto.
   * 
   * @param groupOvertime la variazione di ore di straordinario
   * @param year l'anno di riferimento
   * @return true se possibile assegnare al gruppo la variazione di straordinario, 
   *    false altrimenti.
   */
  public boolean checkOvertimeAvailability(GroupOvertime groupOvertime, int year) {
    
    int totalGroupOvertimes = totalGroupOvertimes(groupOvertime.getGroup());
    
    List<TotalOvertime> totalList = competenceDao
        .getTotalOvertime(LocalDate.now().getYear(), groupOvertime.getGroup().getOffice());
    int totale = competenceManager.getTotalOvertime(totalList);
        
    int groupOvertimeSum = totalOtherGroupsOvertimes(groupOvertime);
    
    int hoursAvailable = totale - groupOvertimeSum - totalGroupOvertimes;
    if (hoursAvailable - groupOvertime.getNumberOfHours() >= 0) {
      return true;
    }    
    return false;
  }
  
  /**
   * Ritorna la mappa <mese,lista di competenze> da mostrare nella pagina di riepilogo 
   * delle ore di straordinario assegnate al gruppo.
   * 
   * @param people la lista di persone per cui recuperare gli straordinari assegnati nell'anno
   * @param year l'anno di riferimento
   * @return la mappa contenente, per ogni mese, la lista di persone con le quantità di straordinario assegnate.
   */
  public Map<Integer, List<PersonOvertimeInMonth>> groupOvertimeSituationInYear(List<Person> people, int year) {
    CompetenceCode code = competenceCodeDao.getCompetenceCodeByCode("S1");
    Map<Integer, List<PersonOvertimeInMonth>> releaseMap = Maps.newHashMap();
    Map<Person, List<Competence>> map = competenceDao.competencesInYear(people, year, code);
    for (Map.Entry<Person, List<Competence>> entry : map.entrySet()) {
      for (Competence competence : entry.getValue()) {
        List<PersonOvertimeInMonth> list = null;
        PersonOvertimeInMonth pom = new PersonOvertimeInMonth();
        pom.person = competence.getPerson();
        pom.quantity = competence.getValueApproved();
        if (!releaseMap.containsKey(competence.getMonth())) {
          list = Lists.newArrayList();                  
        } else {
          list = releaseMap.get(competence.getMonth());                   
        }
        list.add(pom); 
        releaseMap.put(competence.getMonth(), list);
      }
    }    
    return releaseMap;
  }
  
  /**
   * Ritorna la quantità di ore di straordinario assegnate al gruppo.
   * 
   * @param groupOvertime la nuova variazione di ore di straordinario per il gruppo
   * @return la quantità di ore di straordinario assegnate al gruppo.
   */
  public int totalGroupOvertimes(Group group) {
    return group.getGroupOvertimes().stream()
        .filter(go -> go.getYear().equals(LocalDate.now().getYear()))
        .mapToInt(go -> go.getNumberOfHours()).sum();
  }
  
  /**
   * Ritorna il totale delle ore di straordinario assegnate agli altri gruppi.
   * 
   * @param groupOvertime la variazione di ore di straordinario
   * @return il totale delle ore di straordinario assegnate agli altri gruppi.
   */
  public int totalOtherGroupsOvertimes(GroupOvertime groupOvertime) {
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
    return groupOvertimeSum;
  }
  
  /**
   * Ritorna la quantità totale di ore di straordinario assegnate ad un gruppo di persone.
   * 
   * @param map mappa contenente, per ogni mese, la lista di persone che hanno preso straordinario
   * e quante ore hanno preso
   * @return la quantità di ore di straordinario assegnate.
   */
  public int groupOvertimeAssignedInYear(Map<Integer, List<PersonOvertimeInMonth>> map) {
    return map.entrySet().stream()
    .flatMapToInt(pom -> pom.getValue().stream().mapToInt(p -> p.quantity)).sum();
  }
}
