/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
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

package cnr.sync.dto.v3;

import java.time.LocalDateTime;
import models.Stamping;
import models.Stamping.WayType;
import org.junit.Test;
import play.test.UnitTest;

/**
 * Verifica che i DTO REST delle timbrature non trasferiscano su Stamping i flag
 * markedByAdmin e markedByEmployee inviati dal client.
 */
public class StampingDtoTest extends UnitTest {

  @Test
  public void buildIgnoresProvenanceFlags() {
    StampingCreateDto createDto = new StampingCreateDto();
    createDto.setBadgeNumber("123");
    createDto.setDateTime(LocalDateTime.of(2026, 11, 12, 8, 30));
    createDto.setWayType(WayType.in);
    createDto.setNote("nota");
    createDto.setMarkedByAdmin(true);
    createDto.setMarkedByEmployee(true);
    Stamping stamping = StampingCreateDto.build(createDto);
    assertEquals(WayType.in, stamping.getWay());
    assertEquals("nota", stamping.getNote());
    assertFalse(stamping.isMarkedByAdmin());
    assertFalse(stamping.isMarkedByEmployee());

    StampingUpdateDto updateDto = new StampingUpdateDto();
    updateDto.setWayType(WayType.out);
    updateDto.setMarkedByAdmin(true);
    updateDto.setMarkedByEmployee(true);
    stamping = StampingUpdateDto.build(updateDto);
    assertEquals(WayType.out, stamping.getWay());
    assertFalse(stamping.isMarkedByAdmin());
    assertFalse(stamping.isMarkedByEmployee());
  }

  @Test
  public void updateResetsProvenanceFlags() {
    Stamping stamping = new Stamping(null, org.joda.time.LocalDateTime.now());
    stamping.setMarkedByAdmin(true);
    stamping.setMarkedByEmployee(true);
    StampingUpdateDto updateDto = new StampingUpdateDto();
    updateDto.setWayType(WayType.in);
    updateDto.setMarkedByAdmin(true);
    updateDto.update(stamping);
    assertFalse(stamping.isMarkedByAdmin());
    assertFalse(stamping.isMarkedByEmployee());
  }
}
