package manager;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import dao.AbsenceDao;
import dao.ContractDao;
import dao.PersonDayDao;
import dao.UsersRolesOfficesDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPersonDay;
import it.cnr.iit.epas.DateUtility;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.persistence.Query;
import lombok.val;
import lombok.var;
import lombok.extern.slf4j.Slf4j;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.dto.AbsenceToRecoverDto;
import org.assertj.core.util.Lists;
import org.joda.time.LocalDate;
import play.db.jpa.JPA;

@Slf4j
public class PersonManager {

  private final ContractDao contractDao;
  private final PersonDayDao personDayDao;
  public final PersonDayManager personDayManager;
  private final IWrapperFactory wrapperFactory;
  private final AbsenceDao absenceDao;
  

  /**
   * Costrutture.
   *
   * @param contractDao      contractDao
   * @param personDayDao     personDayDao
   * @param absenceDao       absenceDao
   * @param personDayManager personDayManager
   * @param wrapperFactory   wrapperFactory
   */
  @Inject
  public PersonManager(ContractDao contractDao,
      PersonDayDao personDayDao,
      AbsenceDao absenceDao,
      PersonDayManager personDayManager,
      IWrapperFactory wrapperFactory,
      UsersRolesOfficesDao uroDao) {
    this.contractDao = contractDao;
    this.personDayDao = personDayDao;
    this.absenceDao = absenceDao;
    this.personDayManager = personDayManager;
    this.wrapperFactory = wrapperFactory;
    
  }

  /**
   * Calcola se la persona nel giorno non è nè in turno nè in reperibilità e quindi può prendere
   * l'assenza.
   *
   * @return esito
   */
  public boolean canPersonTakeAbsenceInShiftOrReperibility(Person person, LocalDate date) {
    Query queryReperibility =
        JPA.em().createQuery(
            "Select count(*) from PersonReperibilityDay prd where prd.date = :date "
                + "and prd.personReperibility.person = :person");
    queryReperibility.setParameter("date", date).setParameter("person", person);
    int prdCount = queryReperibility.getFirstResult();
    if (prdCount != 0) {
      return false;
    }
    Query queryShift =
        JPA.em().createQuery(
            "Select count(*) from PersonShiftDay psd where psd.date = :date "
                + "and psd.personShift.person = :person");
    queryShift.setParameter("date", date).setParameter("person", person);
    int psdCount = queryShift.getFirstResult();
    if (psdCount != 0) {
      return false;
    }

    return true;
  }

  /**
   * Conta i codici di assenza.
   *
   * @param personDays lista di PersonDay
   * @return La mappa dei codici di assenza utilizzati nei persondays specificati
   */
  public Map<AbsenceType, Integer> countAbsenceCodes(List<PersonDay> personDays) {

    final Map<AbsenceType, Integer> absenceCodeMap = Maps.newHashMap();

    personDays.stream().flatMap(personDay -> personDay.absences.stream()
        .<AbsenceType>map(absence -> absence.absenceType)).forEach(absenceType -> {
          Integer count = absenceCodeMap.get(absenceType);
          absenceCodeMap.put(absenceType, (count == null) ? 1 : count + 1);
        });

    return absenceCodeMap;
  }
  
  /**
   * Metodo utile per il calcolo dei codici di assenza presenti in un certo arco temporale
   *     derivante dalla lista dei personday.
   * @param personDays la lista dei personDay
   * @return la lista dei codici di assenza presenti nella lista dei personDay.
   */
  public List<Absence> listAbsenceCodes(List<PersonDay> personDays) {
    final List<Absence> list = Lists.newArrayList();
    personDays.stream().flatMap(personDay -> personDay.absences.stream()
        .<Absence>map(absence -> absence)).forEach(absence -> {          
          list.add(absence);
        });
        
    return list;
  }


  /**
   * Metodo che determina quanti giorni di lavoro in sede sono stati effettuati. Controlla anche
   *     che tra questi giorni non ci siano giorni di lavoro FUORI SEDE che vengono sottratti
   *     dal conteggio.
   * @param personDays la lista dei personDay
   * @param contracts la lista dei contratti
   * @param end la data di fine
   * @return la lista dei giorni di lavoro IN SEDE derivanti dai parametri passati.
   */
  public int basedWorkingDays(List<PersonDay> personDays,
      List<Contract> contracts, LocalDate end) {

    int basedDays = 0;

    for (PersonDay pd : personDays) {

      if (pd.isHoliday) {
        continue;
      }
      boolean find = false;
      for (Contract contract : contracts) {
        if (DateUtility.isDateIntoInterval(pd.date, contract.periodInterval())) {
          find = true;
        }
      }

      if (!find) {
        continue;
      }
      IWrapperPersonDay day = wrapperFactory.create(pd);
      boolean fixed = day.isFixedTimeAtWork();

      if (fixed && !personDayManager.isAllDayAbsences(pd)) {
        basedDays++;
      } else if (!fixed && pd.stampings.size() > 0 
          && !personDayManager.isAllDayAbsences(pd) 
          && pd.person.qualification.qualification < 4) {
        basedDays++;
      } else if (!fixed && pd.stampings.size() > 0
          && !personDayManager.isAllDayAbsences(pd) 
          && personDayManager.enoughTimeInSeat(pd.stampings, day)) {
        basedDays++;
      }

    }

    return basedDays;
  }

  /**
   * Il numero di riposi compensativi utilizzati tra 2 date (in linea di massima ha senso
   * dall'inizio dell'anno a una certa data).
   */
  public int numberOfCompensatoryRestUntilToday(Person person, LocalDate begin, LocalDate end) {

    List<Contract> contractsInPeriod = contractDao
        .getActiveContractsInPeriod(person, begin, Optional.of(end));

    Contract newerContract = contractsInPeriod.stream().filter(contract ->
        contract.sourceDateResidual != null).max(Comparator
        .comparing(Contract::getSourceDateResidual)).orElse(null);

    if (newerContract != null && newerContract.sourceDateRecoveryDay != null
        && !newerContract.sourceDateRecoveryDay.isBefore(begin)
        && !newerContract.sourceDateRecoveryDay.isAfter(end)) {
      return newerContract.sourceRecoveryDayUsed + absenceDao
          .absenceInPeriod(person, newerContract.sourceDateRecoveryDay, end, "91").size();
    }

    return absenceDao.absenceInPeriod(person, begin, end, "91").size();
  }

  /**
   * Il numero di riposi compensativi utilizzati nell'anno dalla persona.
   */
  public int numberOfCompensatoryRestUntilToday(Person person, int year, int month) {

    LocalDate begin = new LocalDate(year, 1, 1);
    LocalDate end = new LocalDate(year, month, 1).dayOfMonth().withMaximumValue();
    return numberOfCompensatoryRestUntilToday(person, begin, end);
  }

  /**
   * Minuti di presenza festiva non accettata.
   *
   * @param person persona
   * @param year   anno
   * @param month  mese
   * @return minuti
   */
  public int holidayWorkingTimeNotAccepted(Person person, Optional<Integer> year,
      Optional<Integer> month) {

    List<PersonDay> pdList = personDayDao
        .getHolidayWorkingTime(person, year, month);
    int value = 0;
    for (PersonDay pd : pdList) {
      value += pd.getOnHoliday() - pd.getApprovedOnHoliday();
    }
    return value;
  }

  /**
   * Minuti di presenza festiva accettata.
   *
   * @param person persona
   * @param year   anno
   * @param month  mese
   * @return minuti
   */
  public int holidayWorkingTimeAccepted(Person person, Optional<Integer> year,
      Optional<Integer> month) {

    List<PersonDay> pdList = personDayDao
        .getHolidayWorkingTime(person, year, month);
    int value = 0;
    for (PersonDay pd : pdList) {
      value += pd.getApprovedOnHoliday();
    }
    return value;
  }

  /**
   * Minuti di presenza festiva totali.
   *
   * @param person persona
   * @param year   anno
   * @param month  mese
   * @return minuti
   */
  public int holidayWorkingTimeTotal(
      Person person, Optional<Integer> year, Optional<Integer> month) {
    List<PersonDay> pdList = personDayDao
        .getHolidayWorkingTime(person, year, month);
    int value = 0;
    for (PersonDay pd : pdList) {
      value += pd.timeAtWork;
    }
    return value;
  }
  
  /**
   * Metodo che ritorna la lista delle assenze di tipo recover_time non ancora evase nell'arco 
   * temporale compreso tra from e to.
   * @param person la persona di cui si cercano le assenze
   * @param from la data da cui si cercano le assenze
   * @param to la data fino a cui si cercano le assenze
   * @param justifiedTypeName il tipo di giustificativo 
   * @return la lista di assenze di tipo recovery_time presenti nei parametri specificati
   */
  public List<Absence> absencesToRecover(Person person, LocalDate from, 
      LocalDate to, JustifiedTypeName justifiedTypeName) {
    List<Absence> absencesToRecover = Lists.newArrayList();
    List<Absence> absences = absenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(person), 
        Optional.absent(), from, to, Optional.fromNullable(justifiedTypeName), 
        false, false);
    for (Absence abs : absences) {
      int sum = abs.timeVariations.stream().mapToInt(o -> o.timeVariation).sum();
      if (sum < abs.timeToRecover) {
        absencesToRecover.add(abs);
      }
    }
        
    return absencesToRecover;
  }
  
  /**
   * Metodo di utilità per trasformare una lista di assenze in lista di dto per il template.
   * @param list lista di assenze a giustificazione recover_time
   * @return la lista di dto da ritornare alla vista.
   */
  public List<AbsenceToRecoverDto> dtoList(List<Absence> list) {

    List<AbsenceToRecoverDto> absencesToRecover =
        FluentIterable.from(list).transform(
            new Function<Absence, AbsenceToRecoverDto>() {
              @Override
              public AbsenceToRecoverDto apply(Absence absence) {
                return new AbsenceToRecoverDto(
                absence, absence.personDay.date, absence.expireRecoverDate,
                absence.timeToRecover,
                absence.timeVariations.stream().mapToInt(i -> i.timeVariation).sum(),
                Math.round(absence.timeVariations.stream().mapToInt(i -> i.timeVariation).sum() 
                / (float) absence.timeToRecover * 100)
                );
              }
            }
       ).toList();

    return absencesToRecover;
  }
  
  /**
   * eppn viene calcolato come username @ ultimi due livelli 
   * del nome a dominio dell'email.
   * Per esempio se l'username è giuseppe.verdi e l'mail è g.verdi@iit.cnr.it
   * il campo ePPN viene impostato a giuseppe.verdi@cnr.it
  */ 
  public static String eppn(String username, String email) {
    Verify.verifyNotNull(username);
    Verify.verifyNotNull(email);
    
    val emailParts = email.split("@");
    if (emailParts.length < 2) {
      log.warn("Impossibile calcolare il campo eppn per username = {} e email = {}. "
          + "Email non valida.", username, email);
      return null;
    }
    var domain = emailParts[1];
    if (domain.split("\\.").length > 2) {
      val domainTokens = domain.split("\\.");
      domain = String.format("%s.%s",
          domainTokens[domainTokens.length - 2], domainTokens[domainTokens.length - 1]);
    }
    return String.format("%s@%s", username, domain);
  }

}