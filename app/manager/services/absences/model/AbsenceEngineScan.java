package manager.services.absences.model;

import com.google.common.collect.Maps;

import manager.services.absences.AbsenceEngineUtility;

import models.absences.Absence;
import models.absences.GroupAbsenceType;

import org.joda.time.LocalDate;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AbsenceEngineScan {
  
  public AbsenceEngineUtility absenceEngineUtility;
  
  public LocalDate scanFrom;
  public List<Absence> scanAbsences;
  //public Iterator<Absence> absencesIterator;
  public Absence currentAbsence;
  public GroupAbsenceType currentGroup;
  
  Map<Absence, Set<GroupAbsenceType>> absencesGroupsToScan = Maps.newHashMap();
  
  public void setGroupScanned(Absence absence, GroupAbsenceType groupAbsenceType) {
    Set<GroupAbsenceType> absenceGroupsToScan = absencesGroupsToScan.get(absence);
    if (absenceGroupsToScan == null) {
      return;
    }
    absenceGroupsToScan.remove(groupAbsenceType);
  }
  
  /**
   * Il prossimo gruppo da analizzare per la currentAbsence (se c'è).
   * @return
   */
  private GroupAbsenceType currentAbsenceNextGroup() {
    if (this.currentAbsence == null) {
      return null;
    }
    Set<GroupAbsenceType> groupsToScan = this.absencesGroupsToScan.get(this.currentAbsence);
    if (groupsToScan.isEmpty()) {
      return null;
    }
    GroupAbsenceType group = groupsToScan.iterator().next();
    setGroupScanned(this.currentAbsence, group);
    return group;
  }
  
  /**
   * Configura il prossimo gruppo da analizzare (se esiste).
   * @param absenceEngine
   * @return
   */
  public AbsenceEngineScan configureNextGroupToScan(Iterator<Absence> iterator) {
    
    // stessa assenza prossimo gruppo
    this.currentGroup = currentAbsenceNextGroup();
    if (this.currentGroup != null) {
      return this;
    }
    
    // prossima assenza primo gruppo
    while (iterator.hasNext()) {
      this.currentAbsence = iterator.next();
      this.currentGroup = currentAbsenceNextGroup();
      if (this.currentGroup != null) {
        return this;
      }
    }
    return this;
  }

}