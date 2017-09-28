package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import dao.BadgeReaderDao;
import dao.ZoneDao;

import java.util.List;

import javax.inject.Inject;

import models.BadgeReader;
import models.Person;
import models.Zone;
import models.ZoneToZones;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;


@With({Resecure.class})
public class Zones extends Controller {
  
  @Inject
  private static BadgeReaderDao badgeReaderDao;
  @Inject
  private static ZoneDao zoneDao;

  /**
   * 
   * @param badgeReaderId l'id della sorgente timbratura a cui associare le zone.
   */
  public static void insertZone(long badgeReaderId) {
    BadgeReader reader = badgeReaderDao.byId(badgeReaderId);
    if (reader == null) {
      flash.error("Non esiste una sorgente timbratura con id: %s", badgeReaderId);
      render();
    }
    Zone zone = new Zone();
    render(zone, reader);
  }
  
  /**
   * 
   * @param zone la zona che si intende persistere.
   */
  public static void save(Zone zone, long readerId) {
    BadgeReader reader = badgeReaderDao.byId(readerId);
    notFoundIfNull(reader);
    notFoundIfNull(zone);
    zone.badgeReader = reader;
    zone.save();
    flash.success("Nuova zona %s - %s salvata con successo", zone.name, zone.description);
    BadgeReaders.edit(zone.badgeReader.id);
  }
  
  /**
   * ritorna la form per collegare le zone.
   * @param badgeReaderId l'id del badgereader che preleva timbrature dalle zone.
   */
  public static void linkZones(long badgeReaderId) {
    BadgeReader reader = badgeReaderDao.byId(badgeReaderId);
    notFoundIfNull(reader);
    List<Zone> fromList = zoneDao.getByBadgeReader(reader);
    List<Zone> toList = zoneDao.getByBadgeReader(reader);
    ZoneToZones link = new ZoneToZones();
    List<ZoneToZones> list = zoneDao.getZonesByBadgeReader(reader);
    render(fromList, toList, link, reader, list);
  }
  
  /**
   * 
   * @param link il collegamento tra zone da salvare.
   */
  public static void saveLinks(@Valid ZoneToZones link) {
    notFoundIfNull(link);
    
    if (Validation.hasErrors()) {
      response.status = 400;
      List<Zone> fromList = zoneDao.getByBadgeReader(link.zoneBase.badgeReader);
      List<Zone> toList = zoneDao.getByBadgeReader(link.zoneBase.badgeReader);
      List<ZoneToZones> list = zoneDao.getZonesByBadgeReader(link.zoneBase.badgeReader);
      BadgeReader reader = link.zoneBase.badgeReader;
      render("@linkZones", fromList, toList, list, link, reader);
    }
    //controllo che il link non sia tra le stesse zone
    if (link.zoneBase == link.zoneLinked) {
      flash.error("Il collegamento tra una zona e se stessa non è possibile.");
      linkZones(link.zoneBase.badgeReader.id);
    }
    //controllo che non esista già il collegamento tra le zone in entrambi i sensi
    List<ZoneToZones> list = zoneDao.getZonesByBadgeReader(link.zoneBase.badgeReader);
    boolean found = list.stream()
        .anyMatch(l -> (l.zoneBase == link.zoneBase && l.zoneLinked == link.zoneLinked) 
            || (l.zoneBase == link.zoneLinked && l.zoneLinked == link.zoneBase));
    if (found) {
      flash.error("Esiste già un collegamento tra le zone selezionate!");
      linkZones(link.zoneBase.badgeReader.id);
    }
    
    link.save();
    flash.success("Collegamento salvato correttamente");
    linkZones(link.zoneBase.badgeReader.id);
  }
  
  /**
   * 
   * @param linkId
   * @param confirmed
   */
  public static void deleteLink(long linkId, boolean confirmed) {
    ZoneToZones link = zoneDao.getLinkById(linkId);
    notFoundIfNull(link);
    if (!confirmed) {
      confirmed = true;
      render("@deleteLink", link, confirmed);
    }
    if (Validation.hasErrors()) {
      response.status = 400;
      render("@deletePersonShiftShiftType", link, confirmed);
    }
    
    link.delete();

    flash.success("Eliminato collegamento tra %s e %s ", 
        link.zoneBase.name, link.zoneLinked.name);
    linkZones(link.zoneBase.badgeReader.id);
  }
}
