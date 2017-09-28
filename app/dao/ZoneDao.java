package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import dao.wrapper.IWrapperFactory;

import java.util.List;

import javax.persistence.EntityManager;

import models.BadgeReader;
import models.Zone;
import models.ZoneToZones;
import models.query.QZone;
import models.query.QZoneToZones;


public class ZoneDao extends DaoBase {
  
  private final IWrapperFactory factory;

  @Inject
  ZoneDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      IWrapperFactory factory) {
    super(queryFactory, emp);
    this.factory = factory;
  }
  
  /**
   * 
   * @param reader il lettore badge per cui si vogliono le zone associate
   * @return la lista di tutte le zone di timbratura associate al badge reader passato 
   *     come parametro.
   */
  public List<Zone> getByBadgeReader(BadgeReader reader) {
    final QZone zone = QZone.zone;
    JPQLQuery query = getQueryFactory().from(zone).where(zone.badgeReader.eq(reader));
    return query.list(zone);
  }
  
  /**
   * 
   * @param reader il client che recupera le timbrature
   * @return la lista dei collegamenti tra zone che inviano le timbrature allo stesso client.
   */
  public List<ZoneToZones> getZonesByBadgeReader(BadgeReader reader) {
    final QZoneToZones zones = QZoneToZones.zoneToZones;
    JPQLQuery query = getQueryFactory()
        .from(zones)
        .where(zones.zoneBase.badgeReader.eq(reader).and(zones.zoneLinked.badgeReader.eq(reader)));
    return query.list(zones);
  }
  
  /**
   * 
   * @param id l'identificativo del collegamento
   * @return il collegamento, se esiste, caratterizzato dall'id passato come parametro.
   */
  public ZoneToZones getLinkById(long id) {
    final QZoneToZones zones = QZoneToZones.zoneToZones;
    JPQLQuery query = getQueryFactory().from(zones).where(zones.id.eq(id));
    return query.singleResult(zones);
  }
}
