package manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.jpa.impl.JPAQueryFactory;

import models.ConfYear;
import models.Office;
import models.enumerate.Parameter;
import models.query.QConfYear;

import org.joda.time.LocalDate;

import play.cache.Cache;

import javax.inject.Inject;
import javax.persistence.EntityManager;

public class ConfYearManager {

  protected final JPQLQueryFactory queryFactory;

  /**
   * Questo manager utilizza direttamente JPQL perchè implementa un ulteriore strato di astrazione
   * sulle configurazioni (le configurazioni richieste non esistenti vengono create sulla base dei
   * dati di default o degli anni precedenti)
   */
  @Inject
  ConfYearManager(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    this.queryFactory = new JPAQueryFactory(emp);
  }

  /**
   * Produce la configurazione annuale per l'office. I parametri vengono creati a partire dalla
   * configurazione dell'anno precedente (se presente), altrimenti dai valori di default.
   *
   * Se overwrite è false mantiene senza sovrascrivere eventuali parametri generali preesitenti.
   */
  public void buildOfficeConfYear(Office office, Integer year, boolean overwrite) {

    for (Parameter param : Parameter.values()) {

      if (param.isYearly()) {

        Optional<ConfYear> confYear = getByFieldName(param.description, year, office);

        if (!confYear.isPresent() || overwrite) {

          Optional<ConfYear> previousConfYear = getByFieldName(param.description, year - 1, office);

          String newValue = null;

          if (previousConfYear.isPresent()) {
            newValue = previousConfYear.get().fieldValue;
          }

          saveConfYear(param, office, year, Optional.fromNullable(newValue));
        }
      }
    }
  }

  /**
   * Aggiorna il parametro di configurazione relativo all'office. Se value non è presente viene
   * persistito il valore dell'anno precedente. Se il valore dell'anno precedente non è presente
   * viene persistito il valore di default.
   *
   * Il valore preesistente se presente viene sovrascritto.
   */
  public Optional<ConfYear> saveConfYear(Parameter param, Office office, Integer year, Optional<String> value) {

    //Decido il nuovo valore

    String newValue = param.getDefaultValue();

    Optional<ConfYear> previousConfYear = getByFieldName(param.description, year - 1, office);

    if (previousConfYear.isPresent()) {
      newValue = previousConfYear.get().fieldValue;
    }

    if (value.isPresent()) {
      newValue = value.get();
    }

    //Prelevo quella esistente
    Optional<ConfYear> confYear = getByFieldName(param.description, year, office);

    if (confYear.isPresent()) {

      confYear.get().fieldValue = newValue;
      confYear.get().save();
      return confYear;
    }

    ConfYear newConfYear = new ConfYear(office, year, param.description, newValue);
    newConfYear.save();

    return Optional.fromNullable(newConfYear);

  }

  /**
   * Si recupera l'oggetto quando si vuole modificare il parametro.
   *
   * Se serve il valore utilizzare getFieldValue (utilizzo della cache).
   */
  public ConfYear getByField(Parameter param, Office office, Integer year) {

    Preconditions.checkState(param.isYearly());

    Optional<ConfYear> confYear = getByFieldName(param.description, year, office);

    if (!confYear.isPresent()) {

      confYear = saveConfYear(param, office, year, Optional.<String>absent());
    }

    return confYear.get();

  }

  /**
   * Preleva dalla cache il valore del campo di configurazione annuale. Se non presente lo crea a
   * partire da (1) eventuale valore definito per l'anno precedente (2) il valore di default.
   */
  public String getFieldValue(Parameter param, Office office, Integer year) {

    Preconditions.checkState(param.isYearly());

    String key = param.description + office.codeId;

    String value = (String) Cache.get(key);

    if (value == null) {

      Optional<ConfYear> conf = getByFieldName(param.description, year, office);

      if (!conf.isPresent()) {

        conf = saveConfYear(param, office, year, Optional.<String>absent());
      }

      value = conf.get().fieldValue;
      Cache.set(key, value);
    }

    return value;
  }

  /**
   *
   * @param param
   * @param office
   * @param year
   * @return
   */
  public Integer getIntegerFieldValue(Parameter param, Office office, Integer year) {
    return new Integer(getFieldValue(param, office, year));
  }

  /**
   *
   * @param param
   * @param office
   * @param year
   * @return
   */
  public LocalDate getLocalDateFieldValue(Parameter param, Office office, Integer year) {
    return new LocalDate(getFieldValue(param, office, year));
  }

  /**
   * L'enumerato associato a ConfYear.
   */
  public Parameter getParameter(ConfYear confYear) {

    Parameter parameter = null;

    for (Parameter param : Parameter.values()) {
      if (param.description.equals(confYear.field)) {
        parameter = param;
        break;
      }

    }

    Preconditions.checkNotNull(parameter);

    return parameter;

  }

  /**
   * @return il conf year di un certo ufficio in un certo anno rispondente al parametro field
   */
  private Optional<ConfYear> getByFieldName(String field, Integer year, Office office) {

    final QConfYear confYear = QConfYear.confYear;
    final JPQLQuery query = queryFactory.from(confYear);

    query.where(confYear.year.eq(year)
            .and(confYear.field.eq(field)).and(confYear.office.eq(office)));

    return Optional.fromNullable(query.singleResult(confYear));
  }

  /**
   * Validazione del valore di configurazione. Aggiorna la CACHE.
   */
  public MessageResult persistConfYear(ConfYear conf, String value) {

    Preconditions.checkNotNull(conf);

    Integer year = conf.year;

    if (conf.field.equals(Parameter.DAY_EXPIRY_VACATION_PAST_YEAR.description)) {

      Integer month = getIntegerFieldValue(Parameter.MONTH_EXPIRY_VACATION_PAST_YEAR,
              conf.office, year);
      try {
        new LocalDate(year, month, Integer.parseInt(value));
      } catch (Exception e) {

        return new MessageResult(false, Integer.parseInt(value) + "/" + month + "/" + year + " data non valida. Settare correttamente i parametri.");
      }
    }

    if (conf.field.equals(Parameter.MONTH_EXPIRY_VACATION_PAST_YEAR.description)) {

      Integer day = getIntegerFieldValue(Parameter.DAY_EXPIRY_VACATION_PAST_YEAR,
              conf.office, year);
      try {
        new LocalDate(year, Integer.parseInt(value), day);
      } catch (Exception e) {
        return new MessageResult(false, Integer.parseInt(value) + "/" + year + " data non valida. Settare correttamente i parametri.");
      }

    }

    if (conf.field.equals(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_13.description)) {
      if (Integer.parseInt(value) < 0 || Integer.parseInt(value) > 12) {
        return new MessageResult(false, "Bad request");
      }
    }

    if (conf.field.equals(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_49.description)) {
      if (Integer.parseInt(value) < 0 || Integer.parseInt(value) > 12) {
        return new MessageResult(false, "Bad request");
      }
    }

    if (conf.field.equals(Parameter.MAX_RECOVERY_DAYS_13.description)) {
      if (Integer.parseInt(value) < 0 || Integer.parseInt(value) > 31) {
        return new MessageResult(false, "Bad request");
      }
    }

    if (conf.field.equals(Parameter.MAX_RECOVERY_DAYS_49.description)) {
      if (Integer.parseInt(value) < 0 || Integer.parseInt(value) > 31) {
        return new MessageResult(false, "Bad request");
      }
    }

    saveConfYear(getParameter(conf), conf.office, conf.year,
            Optional.fromNullable(value));

    return new MessageResult(true, "parametro di configurazione correttamente inserito");
  }

  public static final class MessageResult {
    public boolean result;
    public String message;

    public MessageResult(boolean result, String message) {
      this.result = result;
      this.message = message;
    }
  }


}
