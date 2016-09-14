package manager.services.absences;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import models.absences.Absence;
import models.absences.AbsenceTrouble.AbsenceProblem;
import models.absences.AbsenceTrouble.AbsenceTypeProblem;
import models.absences.AbsenceTrouble.ImplementationProblem;
import models.absences.AbsenceTrouble.RequestProblem;
import models.absences.AbsenceType;

import org.joda.time.LocalDate;
import org.testng.collections.Maps;

import java.util.List;
import java.util.Map;

@Slf4j
@Setter @Getter
public class AbsencesReport {

  //List degli errori
  public List<ReportAbsenceProblem> absenceProblems = Lists.newArrayList();
  public List<ReportAbsenceTypeProblem> absenceTypeProblems = Lists.newArrayList();
  public List<ReportRequestProblem> requestProblems = Lists.newArrayList();
  public List<ReportImplementationProblem> implementationProblems = Lists.newArrayList();
  
  // Esiti degli inserimenti
  public List<InsertResultItem> insertResultItems = Lists.newArrayList();
  
  // Situazione Assenze
  public List<AbsenceStatus> absencesStatus = Lists.newArrayList();
  
  
  public boolean containsProblems() {
    //TODO: to implement
    return !absenceProblems.isEmpty() 
        || !absenceTypeProblems.isEmpty()
        || !requestProblems.isEmpty()
        || !implementationProblems.isEmpty();
  }

  /**
   * La mappa degli errori riportati per assenza.
   * @return
   */
  public Map<Absence, List<AbsenceProblem>> remainingProblemsMap() {
    Map<Absence, List<AbsenceProblem>> remainingProblemsMap = Maps.newHashMap();
    for (ReportAbsenceProblem absenceProblem : this.absenceProblems) {
      List<AbsenceProblem> remainingProblems = remainingProblemsMap.get(absenceProblem.absence);
      if (remainingProblems == null) {
        remainingProblems = Lists.newArrayList();
        remainingProblemsMap.put(absenceProblem.absence, remainingProblems);
      }
      remainingProblems.add(absenceProblem.absenceProblem);
    }
    return remainingProblemsMap;
  }
  
  public boolean absenceTypeHasProblem(AbsenceType absenceType) {
    //TODO: map
    for (ReportAbsenceTypeProblem reportAbsenceTypeProblem : this.absenceTypeProblems) {
      if (reportAbsenceTypeProblem.absenceType.equals(absenceType)) {
        return true;
      }
    }
    return false;
  }
  
  public void addAbsenceProblem(ReportAbsenceProblem reportAbsenceProblem) {
    this.absenceProblems.add(reportAbsenceProblem );
    log.debug("Aggiunto a report.absenceProblems: " + reportAbsenceProblem.toString());
  }
  
  public void addAbsenceTypeProblem(ReportAbsenceTypeProblem reportAbsenceTypeProblem) {
    this.absenceTypeProblems.add(reportAbsenceTypeProblem );
  }
  
  public void addRequestProblem(ReportRequestProblem reportRequestProblem) {
    this.requestProblems.add(reportRequestProblem);
  }
  
  public void addImplementationProblem(ReportImplementationProblem implementationProblem) {
    this.implementationProblems.add(implementationProblem);
  }
  
  public void addInsertResultItem(InsertResultItem insertResultItem) {
    log.debug("Aggiunto a report.insertResultItems: " + insertResultItem.toString());
    this.insertResultItems.add(insertResultItem);
  }

  @Builder
  public static class ReportAbsenceProblem {
    public AbsenceProblem absenceProblem;
    public Absence absence;
    public Absence conflictingAbsence;
    public AbsenceType correctType;
    
    public String toString() {
      return MoreObjects.toStringHelper(ReportAbsenceProblem.class)
      .add("date", absence.getAbsenceDate())
      .add("code", absence.absenceType.code)
      .add("problem", absenceProblem)
      .toString();
    }
  }
  
  @Builder
  public static class ReportAbsenceTypeProblem {
    public AbsenceTypeProblem absenceTypeProblem;
    public AbsenceType absenceType;
    public AbsenceType conflictingAbsenceType;
  }
  
  @Builder
  public static class ReportRequestProblem {
    public RequestProblem requestProblem;
    public LocalDate date;
    public Absence absence;
  }
  
  @Builder
  public static class ReportImplementationProblem {
    public ImplementationProblem implementationProblem;
    public LocalDate date;
  }
  

  
}
