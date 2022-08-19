/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package controllers;

import common.security.SecurityRules;
import dao.BadgeReaderDao;
import dao.ZoneDao;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.BadgeReader;
import models.Zone;
import models.ZoneToZones;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la gestione delle zone di timbratura.
 */
@Slf4j
@With({Resecure.class})
public class Zones extends Controller {
  
  @Inject
  private static SecurityRules rules;
  @Inject
  private static BadgeReaderDao badgeReaderDao;
  @Inject
  private static ZoneDao zoneDao;

  /**
   * Form di inserimento di una nuova zona associata ad un sorgente timbratura.
   *
   * @param badgeReaderId l'id della sorgente timbratura a cui associare le zone.
   */
  public static void insertZone(long badgeReaderId) {
    BadgeReader reader = badgeReaderDao.byId(badgeReaderId);
    
    if (reader == null) {
      flash.error("Non esiste una sorgente timbratura con id: %s", badgeReaderId);
      render();
    }
    rules.checkIfPermitted(reader.user.owner);
    Zone zone = new Zone();
    render(zone, reader);
  }
  
  /**
   * Salvataggio di una nuova zona.
   *
   * @param zone la zona che si intende persistere.
   */
  public static void save(Zone zone, long readerId) {
    BadgeReader reader = badgeReaderDao.byId(readerId);
    notFoundIfNull(reader);
    notFoundIfNull(zone);
    rules.checkIfPermitted(reader.user.owner);
    zone.badgeReader = reader;
    zone.save();
    flash.success("Nuova zona %s - %s salvata con successo", zone.name, zone.description);
    BadgeReaders.edit(zone.badgeReader.id);
  }
  
  /**
   * ritorna la form per collegare le zone.
   *
   * @param badgeReaderId l'id del badgereader che preleva timbrature dalle zone.
   */
  public static void linkZones(long badgeReaderId, ZoneToZones link) {
    BadgeReader reader = badgeReaderDao.byId(badgeReaderId);
    notFoundIfNull(reader);
    rules.checkIfPermitted(reader.user.owner);
    List<Zone> zones = reader.zones;
    if (link == null) {
      link = new ZoneToZones();
    }
    log.debug("Link = {}", link);
    List<ZoneToZones> list = zoneDao.getZonesByBadgeReader(reader);
    render(zones, link, reader, list);
  }
  
  /**
   * Salva il collegamentro tra due zone di timbratura.
   *
   * @param link il collegamento tra zone da salvare.
   */
  public static void saveLinks(@Valid ZoneToZones link) {
    notFoundIfNull(link);
    rules.checkIfPermitted(link.zoneBase.badgeReader.user.owner);
    if (Validation.hasErrors()) {
      response.status = 400;
      List<Zone> zones = link.zoneBase.badgeReader.zones;
      List<ZoneToZones> list = zoneDao.getZonesByBadgeReader(link.zoneBase.badgeReader);
      BadgeReader reader = link.zoneBase.badgeReader;
      render("@linkZones", zones, list, link, reader);
    }
    //controllo che il link non sia tra le stesse zone
    if (link.zoneBase == link.zoneLinked) {
      flash.error("Il collegamento tra una zona e se stessa non è possibile.");
      List<Zone> zones = link.zoneBase.badgeReader.zones;      
      val reader = link.zoneBase.badgeReader;
      List<ZoneToZones> list = zoneDao.getZonesByBadgeReader(reader);      
      render("@linkZones", zones, link, reader, list);
      linkZones(link.zoneBase.badgeReader.id, link);
    }
    //controllo che non esista già il collegamento tra le zone in entrambi i sensi
    List<ZoneToZones> list = zoneDao.getZonesByBadgeReader(link.zoneBase.badgeReader);
    boolean found = list.stream()
        .anyMatch(l -> (l.zoneBase == link.zoneBase && l.zoneLinked == link.zoneLinked) 
            || (l.zoneBase == link.zoneLinked && l.zoneLinked == link.zoneBase));
    if (found) {
      flash.error("Esiste già un collegamento tra le zone selezionate!");
      linkZones(link.zoneBase.badgeReader.id, link);
    }
    
    link.save();
    flash.success("Collegamento salvato correttamente");
    linkZones(link.zoneBase.badgeReader.id, null);
  }
  
  /**
   * Il metodo che elimina un collegamento tra zone.
   *
   * @param linkId l'id del collegamento da eliminare
   * @param confirmed il booleano che mi identifica se devo confermare o meno la cancellazione
   */
  public static void deleteLink(long linkId, boolean confirmed) {
    ZoneToZones link = zoneDao.getLinkById(linkId);
    notFoundIfNull(link);
    rules.checkIfPermitted(link.zoneBase.badgeReader.user.owner);
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
    linkZones(link.zoneBase.badgeReader.id, null);
  }
}
