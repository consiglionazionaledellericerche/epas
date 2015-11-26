package it.cnr.iit.epas;

import dao.wrapper.IWrapperFactory;

import models.Absence;
import models.AbsenceType;
import models.AbsenceTypeGroup;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.PersonMonthRecap;
import models.Qualification;
import models.Stamping;
import models.VacationCode;

import org.joda.time.LocalDate;

import play.Logger;

import java.io.PrintWriter;
import java.util.List;

import javax.inject.Inject;

public class ExportToYaml {

  @Inject
  IWrapperFactory wrapperFactory;


  /**
   * Builder Yaml della tabella AbsenceType e AbsenceTypeGroup
   */
  public void buildAbsenceTypesAndQualifications(String fileName) {

    //Qualifiche relazionate anche ad absenceTypes
    String qualificationsYaml = "";
    List<Qualification> qualificationList = Qualification.findAll();
    for (Qualification qualification : qualificationList) {
      qualificationsYaml = qualificationsYaml + appendQualification(qualification);
    }

    String absencesYaml = "";

    //AbsenceType senza AbsenceTypeGroup
    List<AbsenceType> abtList1 = AbsenceType.find("Select abt from AbsenceType abt where abt.absenceTypeGroup is null").fetch();
    for (AbsenceType abt : abtList1) {
      absencesYaml = absencesYaml + appendAbsenceType(abt);
    }

    //AbsenceTypeGroup senza replacingAbsenceType
    List<AbsenceTypeGroup> abtgList1 = AbsenceTypeGroup.find("Select abtg from AbsenceTypeGroup abtg where abtg.replacingAbsenceType is null").fetch();
    for (AbsenceTypeGroup abtg : abtgList1) {
      absencesYaml = absencesYaml + appendAbsenceTypeGroup(abtg);
    }

    //AbsenceTypeGroup con replacingAbsenceType
    List<AbsenceTypeGroup> abtgList2 = AbsenceTypeGroup.find("Select abtg from AbsenceTypeGroup abtg where abtg.replacingAbsenceType is not null").fetch();
    for (AbsenceTypeGroup abtg : abtgList2) {
      try {
        absencesYaml = absencesYaml + appendAbsenceType(abtg.replacingAbsenceType);
      } catch (Exception e) {
        Logger.info("abtgList2: Scartata AbsenceType duplicata %s", abtg.replacingAbsenceType.code);
      }
      try {
        absencesYaml = absencesYaml + appendAbsenceTypeGroup(abtg);
      } catch (Exception e) {
        Logger.info("abtgList2: Scartato AbsenceTypeGroup duplicato %s", abtg.label);
      }

    }

    //AbsenceType con AbsenceTypeGroup
    List<AbsenceType> abtList2 = AbsenceType.find("Select abt from AbsenceType abt where abt.absenceTypeGroup is not null").fetch();
    for (AbsenceType abt : abtList2) {
      try {
        absencesYaml = absencesYaml + appendAbsenceType(abt);
      } catch (Exception e) {
        Logger.info("abtList2: Scartata AbsenceType duplicata %s", abt.code);
      }
    }


    writeToYamlFile(fileName, qualificationsYaml + absencesYaml);
  }

  /**
   *
   * @param fileName
   */
  public void buildQualifications(String fileName) {
    String qualificationsYaml = "";
    List<Qualification> qualificationList = Qualification.findAll();
    for (Qualification qualification : qualificationList) {
      qualificationsYaml = qualificationsYaml + appendQualification(qualification);
    }
    writeToYamlFile(fileName, qualificationsYaml);
  }

  /**
   * Builder Yaml della tabella CompetenceCode
   */
  public void buildCompetenceCodes(String fileName) {
    String competenceCodesYaml = "";
    List<CompetenceCode> compList = CompetenceCode.findAll();
    for (CompetenceCode comp : compList) {
      competenceCodesYaml = competenceCodesYaml + appendCompetenceCode(comp);
    }
    writeToYamlFile(fileName, competenceCodesYaml);
  }

  /**
   * Builder Yaml della tabella VacationCodes
   */
  public void buildVacationCodes(String fileName) {
    String vacationCodesYaml = "";
    List<VacationCode> vacCodeList = VacationCode.findAll();
    for (VacationCode vacCode : vacCodeList) {
      if (vacCode.description.equals("28+4") || vacCode.description.equals("26+4"))
        vacationCodesYaml = vacationCodesYaml + appendVacationCode(vacCode);
    }
    writeToYamlFile(fileName, vacationCodesYaml);

  }

  /**
   * Builder Yaml della persona contente WorkingTimeType, WorkingTimeTypeDays, StampProfiles,
   * Contracts //TODO Person_CompetenceCode assegnabili
   */
  public void buildPerson(Person person, String fileName) {
    String personYaml = "";
    personYaml = personYaml + "";
    personYaml = personYaml + appendPerson(person);
    personYaml = personYaml + "";
    personYaml = personYaml + appendContracts(person);


    writeToYamlFile(fileName, personYaml);
  }

  /**
   * Builder Yaml della persona contenente Competences del mese, PersonMonth per il calcolo del
   * riepilogo mensile, PersonDays e Stampings //TODO cosa serve per testare aggiornaRiepiloghi??
   */
  public void buildPersonMonth(Person person, int year, int month, String fileName) {
    String personMonthYaml = "";
    personMonthYaml = personMonthYaml + appendPersonCompetences(person, year, month);
    personMonthYaml = personMonthYaml + appendPersonMonth(person, year, month);
    personMonthYaml = personMonthYaml + buildDays(person, year, month);

    writeToYamlFile(fileName, personMonthYaml);

  }

  public void buildYearlyAbsences(Person person, int year, String fileName) {
    LocalDate yearStart = new LocalDate(year, 1, 1);
    LocalDate yearEnd = new LocalDate(year, 12, 31);
    List<PersonDay> pdList = PersonDay.find(
            "Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date",
            person,
            yearStart,
            yearEnd).fetch();


    String yearlyAbsences = "";
    for (PersonDay pd : pdList) {
      yearlyAbsences = yearlyAbsences + appendPersonDay(pd);
      yearlyAbsences = yearlyAbsences + appendAbsences(pd);
    }
    writeToYamlFile(fileName, yearlyAbsences);
  }


  private String buildDays(Person person, int year, int month) {
    LocalDate date = new LocalDate(year, month, 1);
    List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date DESC",
            person,
            date,
            date.dayOfMonth().withMaximumValue())
            .fetch();
    String out = "";
    for (PersonDay pd : pdList) {
      out = out + appendPersonDay(pd);
      for (Stamping s : pd.stampings) {
        out = out + appendStamping(s);
      }
    }
    return out;
  }

  private String appendQualification(Qualification qual) {
    String out = "";
    out = out + getFormattedHeader("Qualification", "qual" + qual.id);
    out = out + getFormattedProperty("description", qual.description);
    out = out + getFormattedProperty("qualification", qual.qualification + "");
    return out;
  }

  private String appendCompetenceCode(CompetenceCode comp) {
    String out = "";
    out = out + getFormattedHeader("CompetenceCode", "compCode" + comp.id);
    out = out + getFormattedProperty("code", comp.code);
    out = out + getFormattedProperty("codeToPresence", comp.codeToPresence);
    out = out + getFormattedProperty("description", comp.description);
    //		out = out + getFormattedProperty("inactive", comp.inactive+"");
    return out;
  }

  private String appendVacationCode(VacationCode vacCode) {
    String out = "";
    out = out + getFormattedHeader("VacationCode", "vacCode" + vacCode.id);
    out = out + getFormattedProperty("description", vacCode.description);
    out = out + getFormattedProperty("vacationDays", vacCode.vacationDays + "");
    out = out + getFormattedProperty("permissionDays", vacCode.permissionDays + "");
    return out;
  }

  private String appendAbsenceTypeGroup(AbsenceTypeGroup abtg) {
    String out = "";
    out = out + getFormattedHeader("AbsenceTypeGroup", "abtg" + abtg.id);
    out = out + getFormattedProperty("accumulationType", abtg.accumulationType.description);
    out = out + getFormattedProperty("label", abtg.label);
    out = out + getFormattedProperty("limitInMinute", abtg.limitInMinute + "");
    out = out + getFormattedProperty("minutesExcess", abtg.minutesExcess + "");
    out = out + getFormattedProperty("accumulationBehaviour", abtg.accumulationBehaviour.name());
    out = out + getFormattedProperty("accumulationType", abtg.accumulationType.name());
    if (abtg.replacingAbsenceType != null)
      out = out + getFormattedProperty("replacingAbsenceType", "abt" + abtg.replacingAbsenceType.id);

    return out;
  }

  private String appendAbsenceType(AbsenceType abt) {

    String out = "";
    out = out + getFormattedHeader("AbsenceType", "abt" + abt.id);
    out = out + getFormattedProperty("certificateCode", abt.certificateCode);
    out = out + getFormattedProperty("code", abt.code);
    //		out = out + getFormattedProperty("compensatoryRest", abt.compensatoryRest+"");
    out = out + getFormattedProperty("consideredWeekEnd", abt.consideredWeekEnd + "");
    out = out + getFormattedProperty("description", abt.description);
    //		out = out + getFormattedProperty("ignoreStamping", abt.ignoreStamping+"");
    out = out + getFormattedProperty("internalUse", abt.internalUse + "");
    out = out + getFormattedProperty("justifiedTimeAtWork", abt.justifiedTimeAtWork.toString());
//		out = out + getFormattedProperty("mealTicketCalculation", abt.mealTicketCalculation+"");
//		out = out + getFormattedProperty("multipleUse", abt.multipleUse+"");

    if (abt.validFrom != null)
      out = out + getFormattedProperty("validFrom", "'" + abt.validFrom + "'");
    if (abt.validTo != null)
      out = out + getFormattedProperty("validTo", "'" + abt.validTo + "'");
    if (abt.absenceTypeGroup != null)
      out = out + getFormattedProperty("absenceTypeGroup", "abtg" + abt.absenceTypeGroup.id);

    String value = "[";
    for (Qualification qual : abt.qualifications) {
      value = value + "qual" + qual.id + ", ";
    }
    value = value.substring(0, value.length() - 2) + "]";
    out = out + getFormattedProperty("qualifications", value);
    return out;
  }

  private String appendAbsences(PersonDay pd) {
    String out = "";
    for (Absence ab : pd.absences) {
      out = out + getFormattedHeader("Absence", "ab" + ab.id);
      out = out + getFormattedProperty("personDay", "pd" + pd.id);
      out = out + getFormattedProperty("absenceType", "abt" + ab.absenceType.id);
    }
    return out;
  }

  private String appendStamping(Stamping s) {
    String out = "";
    out = out + getFormattedHeader("Stamping", "s" + s.id);
    out = out + getFormattedProperty("personDay", "pd" + s.personDay.id);
    out = out + getFormattedProperty("way", s.way.description);
    if (s.date != null)
      out = out + getFormattedProperty("date", "'" + s.date + "'");
    out = out + getFormattedProperty("markedByAdmin", s.markedByAdmin + "");
    out = out + getFormattedProperty("valid", s.valid + "");
    if (s.stampModificationType != null)
      out = out + getFormattedProperty("stampModificationType", s.stampModificationType.code);
    if (s.stampType != null && s.stampType.identifier.equals("s"))
      out = out + getFormattedProperty("stampType", "motiviDiServizio");
    return out;
  }

  private String appendPersonDay(PersonDay pd) {
    String out = "";
    out = out + getFormattedHeader("PersonDay", "pd" + pd.id);
    out = out + getFormattedProperty("person", "person" + pd.person.id);
    if (pd.date != null)
      out = out + getFormattedProperty("date", "'" + pd.date + "'");
    out = out + getFormattedProperty("isTicketForcedByAdmin", pd.isTicketForcedByAdmin + "");
    out = out + getFormattedProperty("isTicketAvailable", pd.isTicketAvailable + "");
    out = out + getFormattedProperty("isTimeAtWorkAutoCertificated", wrapperFactory.create(pd).isFixedTimeAtWork() + "");
    out = out + getFormattedProperty("isWorkingInAnotherPlace", pd.isWorkingInAnotherPlace + "");
    //out = out + getFormattedProperty("modificationType", pd.modificationType);
    out = out + getFormattedProperty("progressive", pd.progressive + "");
    out = out + getFormattedProperty("difference", pd.difference + "");
    out = out + getFormattedProperty("timeAtWork", pd.timeAtWork + "");
    out = out + appendAbsences(pd);

    //absenceList


    return out;
  }

  private String appendPerson(Person person) {
    String out = "";
    out = out + getFormattedHeader("Person", "person" + person.id);
    out = out + getFormattedProperty("name", person.name);
    out = out + getFormattedProperty("surname", person.surname);
    out = out + appendPersonCompetenceAdmitted(person);
    return out;
  }

  private String appendContracts(Person person) {
    String out = "";
    for (Contract c : person.contracts) {
      out = out + getFormattedHeader("Contract", "c" + c.id);
      out = out + getFormattedProperty("person", "person" + person.id);
      if (c.beginContract != null)
        out = out + getFormattedProperty("beginContract", "'" + c.beginContract + "'");
      if (c.endContract != null)
        out = out + getFormattedProperty("endContract", "'" + c.endContract + "'");
      if (c.expireContract != null)
        out = out + getFormattedProperty("expireContract", "'" + c.expireContract + "'");
      out = out + getFormattedProperty("onCertificate", c.onCertificate + "");
    }
    return out;
  }

  private String appendPersonCompetenceAdmitted(Person person) {
    String out = "";
    String value = "[";
    for (CompetenceCode compCode : person.competenceCode) {
      value = value + "compCode" + compCode.id + ", ";
    }
    value = value.substring(0, value.length() - 2) + "]";
    out = out + getFormattedProperty("competenceCode", value);
    return out;
  }

  private String appendPersonCompetences(Person person, int year, int month) {
    String out = "";
    //competenceCode ammessi ????? non so come modellarla (inserirle direttamente nel test)

    //competence assegnate
    for (Competence comp : person.competences) {
      if (comp.year == year && comp.month == month) {
        out = out + getFormattedHeader("Competence", "comp" + comp.id);
        out = out + getFormattedProperty("person", "person" + person.id);
        out = out + getFormattedProperty("competenceCode", "compCode" + comp.competenceCode.id);
        out = out + getFormattedProperty("month", comp.month + "");
        out = out + getFormattedProperty("reason", comp.reason);
        out = out + getFormattedProperty("valueApproved", comp.valueApproved + "");
        out = out + getFormattedProperty("valueRequest", comp.valueRequested + "");
        out = out + getFormattedProperty("year", comp.year + "");
      }
    }
    return out;

  }

  private String appendPersonMonth(Person person, int year, int month) {
    String out = "";
    LocalDate actualMonth = new LocalDate(year, month, 1);
    LocalDate previousMonth = actualMonth.minusMonths(1);
    for (PersonMonthRecap pm : person.personMonths) {
      LocalDate pmDate = new LocalDate(pm.year, pm.month, 1);
      if (pmDate.isEqual(previousMonth)) {
        out = out + getFormattedHeader("PersonMonth", "pm" + pm.id);
        out = out + getFormattedProperty("person", "person" + person.id);
        out = out + getFormattedProperty("year", pm.year + "");
        out = out + getFormattedProperty("month", pm.month + "");
        //				out = out + getFormattedProperty("compensatoryRestInMinutes", pm.compensatoryRestInMinutes+"");
        //				out = out + getFormattedProperty("recuperiOreDaAnnoPrecedente", pm.recuperiOreDaAnnoPrecedente+"");
        //				out = out + getFormattedProperty("remainingMinutesPastYearTaken", pm.remainingMinutesPastYearTaken+"");
        //				out = out + getFormattedProperty("residualPastYear", pm.residualPastYear+"");
        //				out = out + getFormattedProperty("riposiCompensativiDaAnnoCorrente", pm.riposiCompensativiDaAnnoCorrente+"");
        //				out = out + getFormattedProperty("riposiCompensativiDaAnnoPrecedente", pm.riposiCompensativiDaAnnoPrecedente+"");
        //				out = out + getFormattedProperty("riposiCompensativiDaInizializzazione", pm.riposiCompensativiDaInizializzazione+"");
        //				out = out + getFormattedProperty("straordinari", pm.straordinari+"");
        //				//out = out + getFormattedProperty("", pm.totalRemainingMinutes); deprecated
      }
    }
    return out;

  }

  private String getFormattedHeader(String type, String name) {
    return "\r\n" + type + "(" + name + "):\r\n";
  }

  private String getFormattedProperty(String name, String value) {
    if (value != null && value.contains(":"))
      value = value.replace(":", "-");
    return "    " + name + ": " + value + "\r\n";
  }


  public void writeToYamlFile(String fileName, String out) {
    try {
      PrintWriter writer = new PrintWriter(fileName, "UTF-8");
      writer.print(out);
      writer.close();
    } catch (Exception e) {

    }
  }

}
