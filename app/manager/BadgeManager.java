package manager;

import java.util.Map;

import javax.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import dao.BadgeDao;
import dao.BadgeReaderDao;
import models.Badge;
import models.BadgeReader;

public class BadgeManager {

  @Inject
  public BadgeManager(BadgeDao badgeDao) {
    this.badgeDao = badgeDao;
  }

  private final BadgeDao badgeDao;

  private boolean associateBadge(BadgeReader badgeReader, Integer number) {
    String numero = number + "";
    Optional<Badge> badge = badgeDao.byCode(numero, Optional.fromNullable(badgeReader));
    if (!badge.isPresent()) {
      Badge newBadge = new Badge();
      newBadge.code = numero;
      newBadge.badgeReader = badgeReader;
      newBadge.save();
      return true;
    }
    return false;
  }

  /**
   * 
   * @param inizio il numero inziale di badge 
   * @param fine il numero finale di badge
   * @param badgeReader il lettore di badge
   * @return una mappa contenente come chiavi i numeri di badge e come valori il booleano che
   *     determina se tale badge è stato salvato oppure no.
   */
  public Map<Integer, Boolean> reportAssociateBadge(String inizio, String fine,
      BadgeReader badgeReader) {
    Map<Integer, Boolean> map = Maps.newHashMap();
    Integer begin = new Integer(inizio);
    Integer end = new Integer(fine);
    while (begin <= end) {
      if (associateBadge(badgeReader, begin)) {
        map.put(begin, new Boolean(true));
      } else {
        map.put(begin, new Boolean(false));
      }
      begin++;
    }
    return map;
  }
}
