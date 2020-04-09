package manager.service.contracts;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import com.google.common.base.Optional;
import dao.AbsenceDao;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.ContractManager;
import models.Person;
import models.absences.Absence;
import models.absences.AbsenceType;
import org.joda.time.LocalDate;

@Slf4j
public class ContractService {

  private final AbsenceDao absenceDao;
  private final ContractManager contractManager;

  @Inject
  public ContractService(AbsenceDao absenceDao, ContractManager contractManager) {
    this.absenceDao = absenceDao;
    this.contractManager = contractManager;
  }

  /**
   * La mappa con associazione data-lista tipi di assenza.
   * @param person la persona per cui si recuperano le assenze
   * @param from da quando recuperarle
   * @param to (opzionale) fino a quando recuperarle
   * @return la mappa contenente l'associazione data-lista di tipi assenza.
   */
  public final Map<LocalDate, List<AbsenceType>> getAbsencesInContract(Person person, 
      LocalDate from, Optional<LocalDate> to) {
    Map<LocalDate, List<AbsenceType>> map = Maps.newHashMap();
    List<Absence> absenceList = absenceDao.getAbsencesInPeriod(Optional.fromNullable(person), 
        from, to, false);
    List<AbsenceType> list = null;
    for (Absence abs : absenceList) {
      if (map.get(abs.personDay.date) == null) {
        list = Lists.newArrayList();
      } else {
        list = map.get(abs.personDay.date);
      }
      list.add(abs.absenceType);
      map.put(abs.personDay.date, list);
    }
    return map;
  }
  
  /**
   * Cancella le assenze per person da from a to.
   * @param person la persona per cui cancellare le assenze
   * @param from la data da cui cancellare le assenze
   * @param to (opzionale) la data fino a cui cancellare le assenze
   * @return la quantità di assenze cancellate.
   */
  public Long deleteAbsencesInPeriod(Person person, LocalDate from, Optional<LocalDate> to) {
    return absenceDao.deleteAbsencesInPeriod(person, from, to);
  }
  
  /**
   * Persiste le assenze precedentemente salvate sul nuovo contratto.
   * @param map la mappa con le assenze salvate in precedenza
   */
  public void saveAbsenceOnNewContract(Map<LocalDate, List<AbsenceType>> map) {
    
  }
}
