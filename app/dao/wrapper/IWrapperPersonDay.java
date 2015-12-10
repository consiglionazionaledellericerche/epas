package dao.wrapper;

import com.google.common.base.Optional;

import models.Contract;
import models.PersonDay;
import models.Stamping;
import models.WorkingTimeTypeDay;

/**
 * @author alessandro
 */
public interface IWrapperPersonDay extends IWrapperModel<PersonDay> {

  /**
   * Il contratto cui appartiene il person day. Istanzia una variabile Lazy.
   *
   * @return Optional.absent() se non esiste contratto alla data.
   */
  Optional<Contract> getPersonDayContract();

  /**
   * True se il PersonDay cade in un tipo tirmbratura fixed. Istanzia una variabile Lazy.
   */
  boolean isFixedTimeAtWork();

  /**
   * Il tipo orario giornaliero del personDay. Istanzia una variabile Lazy.
   *
   * @return Optional.absent() in caso di mancanza contratto o di tipo orario.
   */
  Optional<WorkingTimeTypeDay> getWorkingTimeTypeDay();

  /**
   * Il personDay precedente solo se immediatamente consecutivo.
   *
   * @return Optiona.absent() in caso di giorno non consecutivo o primo giorno del contratto
   */
  Optional<PersonDay> getPreviousForNightStamp();

  void setPreviousForNightStamp(Optional<PersonDay> potentialOnlyPrevious);

  /**
   * Il personDay precedente per il calcolo del progressivo.
   */
  Optional<PersonDay> getPreviousForProgressive();

  void setPreviousForProgressive(Optional<PersonDay> potentialOnlyPrevious);

  /**
   * L'ultima timbratura in ordine di tempo nel giorno
   */
  Stamping getLastStamping();

}
