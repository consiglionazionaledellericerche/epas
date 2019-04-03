package dao;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import models.BadgeReader;
import models.ZoneToZones;
import models.query.QZoneToZones;


public class ZoneDao extends DaoBase {

  @Inject
  ZoneDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * @param reader il client che recupera le timbrature
   * @return la lista dei collegamenti tra zone che inviano le timbrature allo stesso client.
   */
  public List<ZoneToZones> getZonesByBadgeReader(BadgeReader reader) {
    final QZoneToZones zones = QZoneToZones.zoneToZones;
    return getQueryFactory()
        .selectFrom(zones)
        .where(zones.zoneBase.badgeReader.eq(reader).and(zones.zoneLinked.badgeReader.eq(reader)))
        .fetch();
  }

  /**
   * @param id l'identificativo del collegamento
   * @return il collegamento, se esiste, caratterizzato dall'id passato come parametro.
   */
  public ZoneToZones getLinkById(long id) {
    final QZoneToZones zones = QZoneToZones.zoneToZones;
    return getQueryFactory().selectFrom(zones).where(zones.id.eq(id)).fetchOne();
  }

  /**
   * @param name1 il nome della prima zona
   * @param name2 il nome della seconda zona
   * @return il link, se esiste, tra le zone passate come parametro.
   */
  public Optional<ZoneToZones> getByLinkNames(String name1, String name2) {
    if (Strings.isNullOrEmpty(name1) || Strings.isNullOrEmpty(name2)) {
      return Optional.absent();
    }
    final QZoneToZones zones = QZoneToZones.zoneToZones;
    final ZoneToZones result = getQueryFactory()
        .selectFrom(zones)
        .where(zones.zoneBase.name.likeIgnoreCase(name1)
            .and(zones.zoneLinked.name.likeIgnoreCase(name2))
            .or(zones.zoneBase.name.likeIgnoreCase(name2)
                .and(zones.zoneLinked.name.likeIgnoreCase(name1)))).fetchOne();
    return Optional.fromNullable(result);
  }
}
