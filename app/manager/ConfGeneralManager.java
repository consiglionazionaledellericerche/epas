package manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.jpa.impl.JPAQueryFactory;

import models.ConfGeneral;
import models.Office;
import models.enumerate.Parameter;
import models.query.QConfGeneral;

import org.joda.time.LocalDate;
import org.joda.time.MonthDay;

import play.cache.Cache;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

public class ConfGeneralManager {


  protected final JPQLQueryFactory queryFactory;

  @Inject
  ConfGeneralManager(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    this.queryFactory = new JPAQueryFactory(emp);
  }

  /**
   * @return l'oggetto confGeneral relativo all'id passato come parametro.
   */
  public Optional<ConfGeneral> getById(Long pk) {

    final QConfGeneral confGeneral = QConfGeneral.confGeneral;

    final JPQLQuery query = queryFactory.from(confGeneral)
            .where(confGeneral.id.eq(pk));

    return Optional.fromNullable(query.singleResult(confGeneral));
  }


  /**
   * @return il confGeneral relativo al campo param e all'ufficio office passati come parametro.
   */
  private Optional<ConfGeneral> getByField(Parameter param, Office office) {

    final QConfGeneral confGeneral = QConfGeneral.confGeneral;

    final JPQLQuery query = queryFactory.from(confGeneral)
            .where(confGeneral.field.eq(param.description).and(confGeneral.office.eq(office)));

    return Optional.fromNullable(query.singleResult(confGeneral));
  }

  /**
   * @return restituisce la lista di tutti i confGeneral che nel parametro field, contengono il
   *     valore value.
   */
  public List<ConfGeneral> containsValue(String field, String value) {

    final QConfGeneral confGeneral = QConfGeneral.confGeneral;

    return queryFactory.from(confGeneral).where(confGeneral.field.eq(field)
            .and(confGeneral.fieldValue.contains(value))).list(confGeneral);

  }

  /**
   * Produce la configurazione generale di default.
   * Se overwrite è false mantiene senza sovrascrivere eventuali parametri generali preesitenti.
   */
  public void buildOfficeConfGeneral(Office office, boolean overwrite) {

    for (Parameter param : Parameter.values()) {

      if (param.isGeneral()) {

        Optional<ConfGeneral> confGeneral = getByField(param, office);

        if (!confGeneral.isPresent() || overwrite) {
          saveConfGeneral(param, office, Optional.<String>absent());
        }

      }

    }
  }

  /**
   * Aggiorna il parametro di configurazione relativo all'office. Se value non è presente viene
   * persistito il valore di default.
   * Il valore precedente se presente viene sovrascritto.
   */
  public ConfGeneral saveConfGeneral(Parameter param, Office office, Optional<String> value) {
    // Il valore passato o in alternativa il valore di default
    String newValue = value.isPresent() ? value.get() : param.getDefaultValue();
    final String key = param.description + office.id;

    // Prelevo quella esistente se esiste, altrimenti ne creo una nuova
    ConfGeneral confGeneral = getByField(param, office)
            .or(new ConfGeneral(office, param.description, newValue));

    confGeneral.fieldValue = newValue;
    confGeneral.save();

    // Aggiorno la cache
    Cache.set(key, confGeneral, "30mn");

    return confGeneral;
  }


  /**
   * Questa funzione controlla effettua in un primo momento a prelevare il parametro dalla cache.
   * Nel caso non sia presente in cache lo preleva dal db e aggiorna la cache. Se non presente sul
   * db ne crea uno con i parametri di default e aggiorna la cache
   */
  public ConfGeneral getConfGeneral(Parameter param, Office office) {

    Preconditions.checkState(param.isGeneral());

    final String key = param.description + office.id;

    // 1. Provo a prelevarlo dalla cache
    ConfGeneral confGeneral = Cache.get(key, ConfGeneral.class);

    // Se non e' presente in cache, lo prelevo dal db
    if (confGeneral == null) {
      confGeneral = getByField(param, office).orNull();

      //Se non e' presente sul db ne creo uno con la configurazione di default
      if (confGeneral == null) {
        return saveConfGeneral(param, office, Optional.<String>absent());
      }
      // Aggiorno la cache se lo prelevo dal db
      Cache.set(key, confGeneral, "30mn");
    }

    return confGeneral;
  }

  public Integer getIntegerFieldValue(Parameter param, Office office) {
    return new Integer(getConfGeneral(param, office).fieldValue);
  }

  public Optional<LocalDate> getLocalDateFieldValue(Parameter param, Office office) {
    try {
      return Optional.fromNullable(new LocalDate(getConfGeneral(param, office).fieldValue));

    } catch (Exception e) {
      return Optional.<LocalDate>absent();
    }

  }

  public boolean getBooleanFieldValue(Parameter param, Office office) {
    return new Boolean(getConfGeneral(param, office).fieldValue);
  }

  public Optional<MonthDay> officePatron(Office office) {

    if (office == null) {
      return Optional.absent();
    }

    String monthOfPatron = getConfGeneral(Parameter.MONTH_OF_PATRON, office).fieldValue;
    String dayOfPatron = getConfGeneral(Parameter.DAY_OF_PATRON, office).fieldValue;
    return Optional.of(MonthDay.parse("--" + monthOfPatron + "-" + dayOfPatron));
  }

  public String getFieldValue(Parameter param, Office office) {
    return getConfGeneral(param, office).fieldValue;
  }

}
