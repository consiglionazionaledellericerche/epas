/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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

package dao;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.BadgeReader;
import models.ZoneToZones;
import models.query.QZoneToZones;


/**
 * DAO per le Zone.
 */
public class ZoneDao extends DaoBase {

  @Inject
  ZoneDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * La lista dei collegamenti tra zone che inviano timbrature allo stesso client.
   *
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
   * Il collegamento caratterizzato dall'id passato come parametro.
   *
   * @param id l'identificativo del collegamento
   * @return il collegamento, se esiste, caratterizzato dall'id passato come parametro.
   */
  public ZoneToZones getLinkById(long id) {
    final QZoneToZones zones = QZoneToZones.zoneToZones;
    return getQueryFactory().selectFrom(zones).where(zones.id.eq(id)).fetchOne();
  }

  /**
   * Il collegamento, se esiste, tra due zone di timbratura.
   *
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
