package manager.recaps.competence;

import com.google.common.base.Optional;
import com.google.gdata.util.common.base.Preconditions;

import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.wrapper.IWrapperFactory;

import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.ContractMonthRecap;
import models.Person;

import org.joda.time.YearMonth;

import play.data.validation.Valid;

/**
 * Riepilogo che popola la vista competenze del dipendente.
 */
public class PersonMonthCompetenceRecap {

  private final CompetenceCodeDao competenceCodeDao;
  private final CompetenceDao competenceDao;

  public Contract contract;
  public int year;
  public int month;

  public int holidaysAvailability = 0;
  public int weekDayAvailability = 0;
  public int daylightWorkingDaysOvertime = 0;
  public int daylightholidaysOvertime = 0;
  public int ordinaryShift = 0;
  public int nightShift = 0;
  public int progressivoFinalePositivoMese = 0;

  public PersonMonthCompetenceRecap(CompetenceCodeDao competenceCodeDao,
                                    CompetenceDao competenceDao, IWrapperFactory wrapperFactory,
                                    Contract contract, int month, int year) {

    this.competenceCodeDao = competenceCodeDao;
    this.competenceDao = competenceDao;

    Preconditions.checkNotNull(contract);

    this.contract = contract;
    this.year = year;
    this.month = month;

    //TODO implementare dei metodi un pò più generali (con enum come parametro)
    this.holidaysAvailability = getHolidaysAvailability(contract.person, year, month);
    this.weekDayAvailability = getWeekDayAvailability(contract.person, year, month);
    this.daylightWorkingDaysOvertime = getDaylightWorkingDaysOvertime(contract.person, year, month);
    this.daylightholidaysOvertime = getDaylightholidaysOvertime(contract.person, year, month);
    this.ordinaryShift = getOrdinaryShift(contract.person, year, month);
    this.nightShift = getNightShift(contract.person, year, month);

    Optional<ContractMonthRecap> recap =
            wrapperFactory.create(contract).getContractMonthRecap(new YearMonth(year, month));
    Preconditions.checkState(recap.isPresent());
    this.progressivoFinalePositivoMese = recap.get().getPositiveResidualInMonth();

  }


  /**
   * Ritorna il numero di giorni di indennità di reperibilità festiva per la persona nel mese.
   */
  private int getHolidaysAvailability(Person person, int year, int month) {
    int holidaysAvailability = 0;
    CompetenceCode cmpCode = competenceCodeDao.getCompetenceCodeByCode("208");

    Optional<Competence> competence = competenceDao.getCompetence(person, year, month, cmpCode);

    if (competence.isPresent())
      holidaysAvailability = competence.get().valueApproved;
    else
      holidaysAvailability = 0;
    return holidaysAvailability;
  }

  /**
   * Ritorna il numero di giorni di indennità di reperibilità feriale per la persona nel mese
   */
  private int getWeekDayAvailability(Person person, @Valid int year, @Valid int month) {
    int weekDayAvailability = 0;
    CompetenceCode cmpCode = competenceCodeDao.getCompetenceCodeByCode("207");

    Optional<Competence> competence = competenceDao.getCompetence(person, year, month, cmpCode);

    if (competence.isPresent())
      weekDayAvailability = competence.get().valueApproved;
    else
      weekDayAvailability = 0;
    return weekDayAvailability;
  }

  /**
   * Ritorna il numero di giorni di straordinario diurno nei giorni lavorativi per la persona nel
   * mese
   */
  private int getDaylightWorkingDaysOvertime(Person person, int year, int month) {
    int daylightWorkingDaysOvertime = 0;
    CompetenceCode cmpCode = competenceCodeDao.getCompetenceCodeByCode("S1");

    Optional<Competence> competence = competenceDao.getCompetence(person, year, month, cmpCode);

    if (competence.isPresent())
      daylightWorkingDaysOvertime = competence.get().valueApproved;
    else
      daylightWorkingDaysOvertime = 0;
    return daylightWorkingDaysOvertime;
  }

  /**
   * Ritorna il numero di giorni di straordinario diurno nei giorni festivi o notturno nei giorni
   * lavorativi per la persona nel mese
   */
  private int getDaylightholidaysOvertime(Person person, int year, int month) {
    int daylightholidaysOvertime = 0;
    CompetenceCode cmpCode = competenceCodeDao.getCompetenceCodeByCode("S2");

    Optional<Competence> competence = competenceDao.getCompetence(person, year, month, cmpCode);

    if (competence.isPresent())
      daylightholidaysOvertime = competence.get().valueApproved;
    else
      daylightholidaysOvertime = 0;
    return daylightholidaysOvertime;
  }

  /**
   * Ritorna il numero di giorni di turno ordinario per la persona nel mese
   */
  private int getOrdinaryShift(Person person, int year, int month) {
    int ordinaryShift = 0;
    CompetenceCode cmpCode = competenceCodeDao.getCompetenceCodeByCode("T1");

    Optional<Competence> competence = competenceDao.getCompetence(person, year, month, cmpCode);

    if (competence.isPresent())
      ordinaryShift = competence.get().valueApproved;
    else
      ordinaryShift = 0;
    return ordinaryShift;
  }

  /**
   * Ritorna il numero di giorni di turno notturno per la persona nel mese
   */
  private int getNightShift(Person person, int year, int month) {
    int nightShift = 0;
    CompetenceCode cmpCode = competenceCodeDao.getCompetenceCodeByCode("T2");

    if (cmpCode == null)
      return 0;
    Optional<Competence> competence = competenceDao.getCompetence(person, year, month, cmpCode);

    if (competence.isPresent())
      nightShift = competence.get().valueApproved;
    else
      nightShift = 0;
    return nightShift;
  }
}
