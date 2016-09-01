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
  public Iterator<Absence> absencesIterator;
  public Absence currentAbsence;
  public GroupAbsenceType currentGroup;
  
  Map<Absence, Set<GroupAbsenceType>> absencesGroupsToScan = Maps.newHashMap();
  
  public boolean isConfiguredForNextScan() {
    return this.currentGroup != null;
  }
  
  public boolean hasNextGroupToScan(Absence absence) {
    Set<GroupAbsenceType> groupsToScan = this.absencesGroupsToScan.get(absence);
    if (groupsToScan == null) {
      groupsToScan = absenceEngineUtility.involvedGroup(absence.absenceType); 
      this.absencesGroupsToScan.put(absence, groupsToScan);
    }
    return !absencesGroupsToScan.get(absence).isEmpty();
  }
  
  public GroupAbsenceType getNextGroupToScan(Absence absence) {
    Set<GroupAbsenceType> absenceGroupsToScan = absencesGroupsToScan.get(absence);
    if (absenceGroupsToScan.isEmpty()) { 
      return null;
    }
    GroupAbsenceType group = absenceGroupsToScan.iterator().next();
    setGroupScanned(absence, group);
    return group;
  }
  
  public void setGroupScanned(Absence absence, GroupAbsenceType groupAbsenceType) {
    Set<GroupAbsenceType> absenceGroupsToScan = absencesGroupsToScan.get(absence);
    if (absenceGroupsToScan == null) {
      return;
    }
    absenceGroupsToScan.remove(groupAbsenceType);
  }
  
  /**
   * Configura il prossimo gruppo da analizzare (se esiste).
   * @param absenceEngine
   * @return
   */
  public AbsenceEngineScan configureNextGroupToScan() {
    // stessa assenza prossimo gruppo
    if (this.currentAbsence != null && hasNextGroupToScan(this.currentAbsence)) {
      this.currentGroup = getNextGroupToScan(this.currentAbsence);
      return this;
    }
    
    // prossima assenza primo gruppo
    this.currentGroup = null;
    while (this.absencesIterator.hasNext()) {
      this.currentAbsence = this.absencesIterator.next();
      if (hasNextGroupToScan(this.currentAbsence)) {   
        this.currentGroup = getNextGroupToScan(this.currentAbsence);
        return this;
      }
    }
    return this;
  }

}