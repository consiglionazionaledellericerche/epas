/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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

import com.google.common.base.Optional;
import com.google.inject.Inject;
import dao.AbsenceDao;
import dao.CompetenceDao;
import dao.PersonDao;
import dao.PersonMonthRecapDao;
import dao.wrapper.IWrapperContractMonthRecap;
import java.util.List;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import models.Competence;
import models.CompetenceCodeGroup;
import models.Office;
import models.Person;
import models.PersonMonthRecap;
import models.absences.Absence;
import play.Play;

/**
 * Manager per la generazione del file da caricare su Attestati.
 */
public class UploadSituationManager {

  
  public static final String FILE_PREFIX = "situazioneMensile";
  public static final String FILE_SUFFIX = ".txt";
  public static final String HEADING = "heading";
  private final PersonDao personDao;
  private AbsenceDao absenceDao;
  private CompetenceDao competenceDao;
  private PersonMonthRecapDao personMonthRecapDao;
  private PersonStampingRecapFactory stampingsRecapFactory;
  
  /**
   * Generatore del file da caricare su attestati con le assenze/compentenze del personale.
   */
  @Inject
  public UploadSituationManager(PersonDao personDao, AbsenceDao absenceDao, 
      CompetenceDao competenceDao, PersonMonthRecapDao personMonthRecapDao,
      PersonStampingRecapFactory stampingsRecapFactory) {
    this.personDao = personDao;
    this.absenceDao = absenceDao;
    this.competenceDao = competenceDao;
    this.personMonthRecapDao = personMonthRecapDao;
    this.stampingsRecapFactory = stampingsRecapFactory;
  }
  
  /**
   * Produce il contenuto del file per attestati circa mese e anno per tutti i dipendenti della
   * sede passata come paramemtro.
   *
   * @param office office
   * @param year anno 
   * @param month mese 
   * @return contenuto del file
   */
  public String createFile(Office office, Integer year, Integer month) {
    
    List<Person> personList = personDao.getPersonsWithNumber(Optional.of(office));
    
    String body = Play.configuration.getProperty(HEADING) + "\r\n" 
        + office.getCodeId() + " " + year + " " + month + "\r\n";
    
    for (Person person : personList) {
      // la parte delle assenze
      List<Absence> absenceList  = absenceDao.getAbsencesNotInternalUseInMonth(person, year, month);
      for (Absence abs : absenceList) {
        body = body + person.getNumber() + " A " + abs.getAbsenceType().getCode() + " " 
             + abs.getPersonDay().getDate().getDayOfMonth() 
             + " " + abs.getPersonDay().getDate().getDayOfMonth() + " \r\n"; 
      }
      //la parte delle competenze
      List<Competence> competenceList = competenceDao
          .getCompetenceInMonthForUploadSituation(person, year, month, 
              Optional.<CompetenceCodeGroup>absent());
      for (Competence comp : competenceList) {
        body = body + person.getNumber() + " C " + comp.getCompetenceCode().getCode() + " " 
            + comp.getValueApproved() + " \r\n";
      }
      List<PersonMonthRecap> pmrList = 
          personMonthRecapDao.getPersonMonthRecapInYearOrWithMoreDetails(
              person, year, Optional.fromNullable(month), Optional.<Boolean>absent());
      for (PersonMonthRecap pmr : pmrList) {
        body = body + person.getNumber() + " F " + pmr.getFromDate().getDayOfMonth() + " " 
            + pmr.getToDate().getDayOfMonth() + " " + pmr.getTrainingHours() + " \r\n";
      }
      PersonStampingRecap psDto = stampingsRecapFactory.create(person, year, month, false);
      for (IWrapperContractMonthRecap cmr : psDto.contractMonths) {
        body = body + person.getNumber() + " B " + cmr.getValue().getBuoniPastoUsatiNelMese() + " " 
            + cmr.getValue().getBuoniPastoUsatiNelMese() + " \r\n";
      }
    }
    
    return body;
  }

}