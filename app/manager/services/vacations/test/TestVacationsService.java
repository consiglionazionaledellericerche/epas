package manager.services.vacations.test;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import dao.AbsenceDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.ConfYearManager;
import manager.cache.AbsenceTypeManager;
import manager.services.vacations.IVacationsRecap;
import manager.services.vacations.IVacationsService;
import manager.services.vacations.impl.VacationsRecapImpl;

import models.Absence;
import models.AbsenceType;
import models.Contract;
import models.Office;
import models.Person;
import models.VacationPeriod;
import models.enumerate.AbsenceTypeMapping;

import org.joda.time.LocalDate;

import java.util.List;

import javax.inject.Inject;

/**
 * Implementazione di test del servizio ferie e permessi.
 * @author alessandro
 *
 */
public class TestVacationsService implements IVacationsService {

  /**
   * Costruttore del recap.
   */
  @Override
  public IVacationsRecap build(int year, Contract contract, List<Absence> absencesToConsider,
      LocalDate accruedDate, LocalDate dateExpireLastYear, boolean considerDateExpireLastYear,
      Optional<LocalDate> dateAsToday) {
    
    return new VacationsRecapImpl(year, contract, absencesToConsider, LocalDate.now(), 
        dateExpireLastYear, true, dateAsToday); 
  }

  @Override
  public Optional<IVacationsRecap> create(int year, Contract contract, LocalDate actualDate,
      boolean considerExpireLastYear, List<Absence> otherAbsences,
      Optional<LocalDate> dateAsToday) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Optional<IVacationsRecap> create(int year, Contract contract, LocalDate actualDate,
      boolean considerExpireLastYear) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Optional<IVacationsRecap> create(int year, Contract contract, LocalDate actualDate,
      boolean considerExpireLastYear, LocalDate dateAsToday) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public AbsenceType whichVacationCode(Person person, LocalDate date, List<Absence> otherAbsences) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean canTake32(Person person, LocalDate date, List<Absence> otherAbsences) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean canTake31(Person person, LocalDate date, List<Absence> otherAbsences) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean canTake94(Person person, LocalDate date, List<Absence> otherAbsences) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public LocalDate vacationsLastYearExpireDate(int year, Office office) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isVacationsLastYearExpired(int year, LocalDate expireDate) {
    // TODO Auto-generated method stub
    return false;
  }
  
}
