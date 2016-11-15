package manager;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import models.Badge;

/**
 * TODO: In questo manager andrebbero spostati i metodi che gestiscono i controllers BadgeReaders
 * e BadgeSystems.
 * 
 * @author alessandro
 *
 */
@Slf4j
public class BadgeManager {

  @Inject
  public BadgeManager() {
    
  }
  
  /**
   * Rimuove gli zero davanti al codice se presenti e persiste il dato.
   * @param badge il badge da normalizzare
   * @param persist se si vuole persistere la normalizzazione
   */
  public void normalizeBadgeCode(Badge badge, boolean persist) {
    try {
      String code = badge.code;
      Integer number = Integer.parseInt(code);
      badge.code = number + "";
      if (!code.equals(number + "") ) {
        if (persist) {
          badge.save();
          log.info("Normalizzato e persistito badge.code: da {} a {}", code, number);
        } else {
          log.info("Normalizzato badge.code: da {} a {}", code, number);
        }
      }
    } catch (Exception ex) {
      //Tipo String
      //log.info("Impossibile {}", badge.code);
    }
  }
  
}
